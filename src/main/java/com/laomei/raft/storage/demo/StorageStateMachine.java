package com.laomei.raft.storage.demo;

import com.google.protobuf.InvalidProtocolBufferException;
import com.laomei.raft.storage.demo.data.StorageProto;
import com.laomei.raft.storage.demo.data.StorageProto.DeleteRequest;
import com.laomei.raft.storage.demo.data.StorageProto.ReadRequest;
import com.laomei.raft.storage.demo.data.StorageProto.StorageRequest;
import com.laomei.raft.storage.demo.data.StorageProto.UpdateRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.ratis.proto.RaftProtos;
import org.apache.ratis.protocol.Message;
import org.apache.ratis.protocol.RaftClientRequest;
import org.apache.ratis.protocol.RaftGroupId;
import org.apache.ratis.server.RaftServer;
import org.apache.ratis.server.storage.RaftStorage;
import org.apache.ratis.statemachine.StateMachineStorage;
import org.apache.ratis.statemachine.TransactionContext;
import org.apache.ratis.statemachine.impl.BaseStateMachine;
import org.apache.ratis.statemachine.impl.SimpleStateMachineStorage;
import org.apache.ratis.thirdparty.com.google.protobuf.ByteString;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * @author luobo.hwz on 2020/12/21 17:26
 */
@Slf4j
public class StorageStateMachine extends BaseStateMachine {
    private final SimpleStateMachineStorage machineStorage;

    private final ConfigMemoryStorage memoryStorage;

    private final ExecutorService executorService;

    public StorageStateMachine() {
        this.machineStorage = new SimpleStateMachineStorage();
        this.memoryStorage = new ConfigMemoryStorage();
        this.executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);
    }

    @Override
    public void initialize(RaftServer raftServer, RaftGroupId raftGroupId, RaftStorage storage) throws IOException {
        super.initialize(raftServer, raftGroupId, storage);
        machineStorage.init(storage);
    }

    @Override
    public StateMachineStorage getStateMachineStorage() {
        return machineStorage;
    }

    @Override
    public void close() throws IOException {
        super.close();
        memoryStorage.clear();
    }

    @Override
    public TransactionContext startTransaction(RaftClientRequest request) throws IOException {
        return TransactionContext.newBuilder()
                .setClientRequest(request)
                .setStateMachine(this)
                .setServerRole(RaftProtos.RaftPeerRole.LEADER)
                .setLogData(request.getMessage().getContent())
                .build();
    }

    /**
     * update configs
     * @param trx
     * @return
     */
    @Override
    public CompletableFuture<Message> applyTransaction(TransactionContext trx) {
        final ByteString messageContent = trx.getStateMachineLogEntry().getLogData();
        try {
            final StorageRequest storageRequest = StorageRequest.parseFrom(messageContent.toByteArray());
            if (validateRequest(StorageProto.Type.READ, storageRequest.getCmdType())) {
                return exceptionalFuture("request is READ when apply transaction");
            }
            CompletableFuture<Void> future = CompletableFuture.supplyAsync(() -> {
                switch (storageRequest.getCmdType()) {
                    case UPDATE:
                        updateConfigs(storageRequest.getUpdateRequest());
                        break;
                    case DELETE:
                        deleteConfigs(storageRequest.getDeleteRequest());
                        break;
                }
                return null;
            }, executorService);
            final CompletableFuture<Message> result = new CompletableFuture<>();
            future.whenComplete((v, t) -> {
                if (t != null) {
                    result.completeExceptionally(t);
                } else {
                    final String msg = storageRequest.getCmdType() == StorageProto.Type.UPDATE ?
                            "update success" : "delete success";
                    result.complete(Message.valueOf(
                            ByteString.copyFrom(
                                    StorageProto.StorageResponse
                                        .newBuilder()
                                        .setMsg(msg)
                                        .setData(
                                                StorageProto.ResponseData.newBuilder()
                                                        .setCmdType(StorageProto.Type.UPDATE)
                                                        .build()
                                        ).build()
                                        .toByteArray()
                            )
                    ));
                }
            });
            return result;
        } catch (com.google.protobuf.InvalidProtocolBufferException e) {
            final CompletableFuture<Message> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }

    @Override
    public CompletableFuture<Message> query(Message request) {
        final ByteString bytes = request.getContent();
        try {
            final StorageRequest storageRequest = StorageRequest.parseFrom(bytes.toByteArray());
            if (!validateRequest(StorageProto.Type.READ, storageRequest.getCmdType())) {
                return exceptionalFuture("request is not READ when query");
            }
            final CompletableFuture<Message> result = new CompletableFuture<>();
            final ReadRequest readRequest = storageRequest.getReadRequest();
            final Collection<String> keys = readRequest.getKeysList();
            final Map<String, Object> configs = keys.stream()
                    .filter(k -> Objects.nonNull(memoryStorage.get(k)))
                    .collect(Collectors.toMap(key -> key, memoryStorage::get));
            final String data = Util.json(configs);
            final Message message = Message.valueOf(
                    ByteString.copyFrom(
                        StorageProto.StorageResponse.newBuilder()
                            .setMsg("query success")
                            .setData(
                                    StorageProto.ResponseData.newBuilder()
                                            .setCmdType(StorageProto.Type.READ)
                                            .setReadResponse(
                                                    StorageProto.ReadResponse.newBuilder()
                                                            .setConfigs(data)
                                                            .build()
                                            )
                                            .build()
                            ).build().toByteArray()
                    )
            );
            result.complete(message);
            return result;
        } catch (InvalidProtocolBufferException e) {
            final CompletableFuture<Message> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }

    private void updateConfigs(final UpdateRequest updateRequest) {
        final String json = updateRequest.getDataJson();
        final Map<String, Object> datas = Util.json(json);
        if (datas == null) {
            throw new IllegalStateException("deserialize config json failed");
        }
        System.out.println("add configs: " + datas);
        datas.forEach(memoryStorage::add);
    }

    private void deleteConfigs(final DeleteRequest deleteRequest) {
        System.out.println("delete configs: " + deleteRequest.getKeysList());
        deleteRequest.getKeysList().forEach(memoryStorage::delete);
    }

    private CompletableFuture<Message> exceptionalFuture(final String msg) {
        final CompletableFuture<Message> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException(msg));
        return future;
    }

    private boolean validateRequest(final StorageProto.Type expected, final StorageProto.Type original) {
        return expected == original;
    }
}

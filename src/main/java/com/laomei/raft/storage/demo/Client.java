package com.laomei.raft.storage.demo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.laomei.raft.storage.demo.data.StorageProto;
import com.laomei.raft.storage.demo.data.StorageProto.DeleteRequest;
import com.laomei.raft.storage.demo.data.StorageProto.ReadRequest;
import com.laomei.raft.storage.demo.data.StorageProto.StorageRequest;
import com.laomei.raft.storage.demo.data.StorageProto.UpdateRequest;
import org.apache.ratis.client.RaftClient;
import org.apache.ratis.conf.Parameters;
import org.apache.ratis.conf.RaftProperties;
import org.apache.ratis.grpc.GrpcFactory;
import org.apache.ratis.protocol.ClientId;
import org.apache.ratis.protocol.Message;
import org.apache.ratis.protocol.RaftClientReply;
import org.apache.ratis.thirdparty.com.google.protobuf.ByteString;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;

import static com.laomei.raft.storage.demo.Config.RAFT_GROUP;

/**
 * @author luobo.hwz on 2021/01/06 20:49
 */
public class Client {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static void main(String[] args) throws InterruptedException, ExecutionException, IOException {
        final RaftClient raftClient = buildClient();
        final Map<String, Object> value = new HashMap<>();
        value.put("key1", "a");
        value.put("key2", 1);
        write(raftClient, value);
        System.out.println("==========================");
        read(raftClient, Arrays.asList("key1", "key2"));
        System.out.println("==========================");
        delete(raftClient, Collections.singletonList("key1"));
        System.out.println("==========================");
        read(raftClient, Arrays.asList("key1", "key2"));
    }

    private static void delete(final RaftClient raftClient, final Collection<String> keys) throws ExecutionException, InterruptedException {
        final DeleteRequest deleteRequest = DeleteRequest.newBuilder()
                .addAllKeys(keys)
                .build();
        final StorageRequest storageRequest = StorageRequest.newBuilder()
                .setCmdType(StorageProto.Type.DELETE)
                .setDeleteRequest(deleteRequest)
                .build();
        final RaftClientReply reply = raftClient.sendAsync(Message.valueOf(
                ByteString.copyFrom(storageRequest.toByteArray()))).get();
        final String result = reply.getMessage().getContent().toStringUtf8();
        System.out.println(result);
    }

    private static void read(final RaftClient raftClient, final Collection<String> keys) throws ExecutionException, InterruptedException, IOException {
        final ReadRequest readRequest = ReadRequest.newBuilder()
                .addAllKeys(keys)
                .build();
        final StorageRequest storageRequest = StorageRequest.newBuilder()
                .setCmdType(StorageProto.Type.READ)
                .setReadRequest(readRequest)
                .build();
        final RaftClientReply reply = raftClient.sendReadOnly(Message.valueOf(
                ByteString.copyFrom(storageRequest.toByteArray())));
        final String result = reply.getMessage().getContent().toStringUtf8();
        System.out.println(result);
    }

    private static void write(final RaftClient raftClient, final Map<String, Object> value) throws JsonProcessingException, ExecutionException, InterruptedException {
        final String data = OBJECT_MAPPER.writeValueAsString(value);
        final UpdateRequest updateRequest = UpdateRequest.newBuilder()
                .setDataJson(data)
                .build();
        final StorageRequest storageRequest = StorageRequest.newBuilder()
                .setCmdType(StorageProto.Type.UPDATE)
                .setUpdateRequest(updateRequest)
                .build();
        final RaftClientReply reply = raftClient.sendAsync(Message.valueOf(
                ByteString.copyFrom(storageRequest.toByteArray()))).get();
        final String result = reply.getMessage().getContent().toStringUtf8();
        System.out.println(result);
    }

    private static RaftClient buildClient() {
        RaftProperties raftProperties = new RaftProperties();
        RaftClient.Builder builder = RaftClient.newBuilder()
                .setProperties(raftProperties)
                .setRaftGroup(RAFT_GROUP)
                .setClientRpc(
                        new GrpcFactory(new Parameters())
                                .newRaftClientRpc(ClientId.randomId(), raftProperties));
        return builder.build();
    }
}

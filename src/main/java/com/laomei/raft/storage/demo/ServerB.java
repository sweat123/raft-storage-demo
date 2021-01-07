package com.laomei.raft.storage.demo;

import org.apache.ratis.conf.RaftProperties;
import org.apache.ratis.grpc.GrpcConfigKeys;
import org.apache.ratis.server.RaftServer;
import org.apache.ratis.server.RaftServerConfigKeys;
import org.apache.ratis.util.NetUtils;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Scanner;

import static com.laomei.raft.storage.demo.Config.RAFT_GROUP;
import static com.laomei.raft.storage.demo.Config.raftPeerB;

/**
 * @author luobo.hwz on 2021/01/06 20:39
 */
public class ServerB {

    public static void main(String[] args) throws IOException {
        final RaftProperties properties = new RaftProperties();
        File raftStorageDir = new File("./" + raftPeerB.getId().toString());
        RaftServerConfigKeys.setStorageDir(properties, Collections.singletonList(raftStorageDir));
        final int port = NetUtils.createSocketAddr(raftPeerB.getAddress()).getPort();
        GrpcConfigKeys.Server.setPort(properties, port);
        StorageStateMachine configStateMachine = new StorageStateMachine();
        RaftServer server = RaftServer.newBuilder()
                .setGroup(RAFT_GROUP)
                .setProperties(properties)
                .setServerId(raftPeerB.getId())
                .setStateMachine(configStateMachine)
                .build();
        server.start();

        //exit when any input entered
        Scanner scanner = new Scanner(System.in);
        scanner.nextLine();
        server.close();
    }
}

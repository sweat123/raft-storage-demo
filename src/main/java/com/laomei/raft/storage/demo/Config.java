package com.laomei.raft.storage.demo;

import org.apache.ratis.protocol.RaftGroup;
import org.apache.ratis.protocol.RaftGroupId;
import org.apache.ratis.protocol.RaftPeer;
import org.apache.ratis.protocol.RaftPeerId;

import java.net.InetSocketAddress;
import java.util.UUID;

/**
 * @author luobo.hwz on 2021/01/07 09:01
 */
public class Config {


    static final RaftPeer raftPeerA = new RaftPeer(RaftPeerId.getRaftPeerId("A"),
            new InetSocketAddress("127.0.0.1", 8081));

    static final RaftPeer raftPeerB = new RaftPeer(RaftPeerId.getRaftPeerId("B"),
            new InetSocketAddress("127.0.0.1", 8082));

    static final RaftGroup RAFT_GROUP = RaftGroup.valueOf(
            RaftGroupId.valueOf(UUID.fromString("02511d47-d67c-49a3-9011-abb3109a44c1")), raftPeerA, raftPeerB);
}

前期验证项目，用于验证基于`raft`构建配置中心的可行性。配置中心的配置存放在内存，使用`raft`做到强一致。

启动运行`com.laomei.raft.storage.demo.ServerA`和`com.laomei.raft.storage.demo.ServerB`，`2`个`server`数据使用`raft`做了一致性保证。

运行`com.laomei.raft.storage.demo.Client`，`Client`会写入、读取、删除测试数据。

***

Demo project build with raft as config center service. Config center data is stored in memory.

First Step:

run `com.laomei.raft.storage.demo.ServerA` and `com.laomei.raft.storage.demo.ServerB`。These servers work with ratis to ensure consistency;

Then:

run `com.laomei.raft.storage.demo.Client`, client will write, read, delete config data.


 
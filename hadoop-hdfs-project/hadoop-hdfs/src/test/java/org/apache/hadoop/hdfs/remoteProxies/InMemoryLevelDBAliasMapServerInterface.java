package org.apache.hadoop.hdfs.remoteProxies;

public interface InMemoryLevelDBAliasMapServerInterface {
    java.util.Optional<org.apache.hadoop.hdfs.protocol.ProvidedStorageLocation> read(BlockInterface arg0) throws java.io.IOException;
    java.lang.String getBlockPoolId();
    void write(BlockInterface arg0, ProvidedStorageLocationInterface arg1) throws java.io.IOException;
    IterationResultInterface list(java.util.Optional<org.apache.hadoop.hdfs.protocol.Block> arg0) throws java.io.IOException;
    InMemoryAliasMapInterface getAliasMap();
    void close();
    ConfigurationInterface getConf();
    void start() throws java.io.IOException;
    void setConf(ConfigurationInterface arg0);
}
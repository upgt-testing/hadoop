package org.apache.hadoop.hdfs.remoteProxies;

public interface InMemoryAliasMapInterface {
    java.util.Optional<org.apache.hadoop.hdfs.protocol.ProvidedStorageLocation> read(BlockInterface arg0) throws java.io.IOException;
    void transferForBootstrap(javax.servlet.http.HttpServletResponse arg0, ConfigurationInterface arg1, InMemoryAliasMapInterface arg2) throws java.io.IOException;
    ConfigurationInterface getConf();
    IterationResultInterface list(java.util.Optional<org.apache.hadoop.hdfs.protocol.Block> arg0) throws java.io.IOException;
    java.io.File getCompressedAliasMap(java.io.File arg0) throws java.io.IOException;
    java.io.File createSnapshot(InMemoryAliasMapInterface arg0) throws java.io.IOException;
    InMemoryAliasMapInterface init(ConfigurationInterface arg0, java.lang.String arg1) throws java.io.IOException;
    java.lang.String getBlockPoolId();
    ProvidedStorageLocationInterface fromProvidedStorageLocationBytes(byte[] arg0) throws org.apache.hadoop.thirdparty.protobuf.InvalidProtocolBufferException;
    void write(BlockInterface arg0, ProvidedStorageLocationInterface arg1) throws java.io.IOException;
    BlockInterface fromBlockBytes(byte[] arg0) throws org.apache.hadoop.thirdparty.protobuf.InvalidProtocolBufferException;
    void completeBootstrapTransfer(java.io.File arg0) throws java.io.IOException;
    void close() throws java.io.IOException;
    byte[] toProtoBufBytes(BlockInterface arg0) throws java.io.IOException;
    void addFileToTarGzRecursively(TarArchiveOutputStreamInterface arg0, java.io.File arg1, java.lang.String arg2, ConfigurationInterface arg3) throws java.io.IOException;
    byte[] toProtoBufBytes(ProvidedStorageLocationInterface arg0) throws java.io.IOException;
    void setConf(ConfigurationInterface arg0);
}
package org.apache.hadoop.hdfs.remoteProxies;

public interface BlockListAsLongsInterface {
    int getNumberOfBlocks();
    BlockListAsLongsInterface decodeBuffer(int arg0, ByteStringInterface arg1, int arg2);
//    java.util.Iterator<T> iterator();
    BuilderInterface builder();
    java.util.Iterator<org.apache.hadoop.hdfs.protocol.BlockListAsLongs.BlockReportReplica> iterator();
    BlockListAsLongsInterface decodeBuffers(int arg0, java.util.List<org.apache.hadoop.thirdparty.protobuf.ByteString> arg1);
    BlockListAsLongsInterface decodeLongs(java.util.List<java.lang.Long> arg0, int arg1);
    void writeTo(java.io.OutputStream arg0) throws java.io.IOException;
    BlockListAsLongsInterface decodeBuffers(int arg0, java.util.List<org.apache.hadoop.thirdparty.protobuf.ByteString> arg1, int arg2);
//    java.util.Spliterator<T> spliterator();
    long[] getBlockListAsLongs();
    ByteStringInterface getBlocksBuffer();
    BlockListAsLongsInterface decodeLongs(java.util.List<java.lang.Long> arg0);
//    void forEach(java.util.function.Consumer<? super T> arg0);
    BuilderInterface builder(int arg0);
    java.util.List<org.apache.hadoop.thirdparty.protobuf.ByteString> getBlocksBuffers();
    BlockListAsLongsInterface readFrom(java.io.InputStream arg0, int arg1) throws java.io.IOException;
    BlockListAsLongsInterface encode(java.util.Collection<? extends org.apache.hadoop.hdfs.server.datanode.Replica> arg0);
}
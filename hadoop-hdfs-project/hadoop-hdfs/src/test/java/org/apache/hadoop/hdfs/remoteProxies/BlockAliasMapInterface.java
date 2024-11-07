package org.apache.hadoop.hdfs.remoteProxies;

public interface BlockAliasMapInterface<T> {
    WriterInterface<T> getWriter(org.apache.hadoop.hdfs.server.common.blockaliasmap.BlockAliasMap.Writer.Options arg0, java.lang.String arg1) throws java.io.IOException;
    void refresh() throws java.io.IOException;
    ReaderInterface<T> getReader(org.apache.hadoop.hdfs.server.common.blockaliasmap.BlockAliasMap.Reader.Options arg0, java.lang.String arg1) throws java.io.IOException;
    void close() throws java.io.IOException;
}
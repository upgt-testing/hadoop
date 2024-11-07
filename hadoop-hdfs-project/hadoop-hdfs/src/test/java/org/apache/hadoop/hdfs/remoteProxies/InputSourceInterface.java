package org.apache.hadoop.hdfs.remoteProxies;

public interface InputSourceInterface {
    void setPublicId(java.lang.String arg0);
    java.lang.String getPublicId();
    java.lang.String getEncoding();
    boolean isStreamEmpty();
    java.io.InputStream getByteStream();
    java.io.Reader getCharacterStream();
    void setByteStream(java.io.InputStream arg0);
    void setEncoding(java.lang.String arg0);
    java.lang.String getSystemId();
    void setCharacterStream(java.io.Reader arg0);
    boolean isEmpty();
    void setSystemId(java.lang.String arg0);
}
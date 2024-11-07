package org.apache.hadoop.hdfs.remoteProxies;

public interface JsonLocationInterface {
    int hashCode();
    long getByteOffset();
    int getColumnNr();
    java.lang.String toString();
    java.lang.StringBuilder appendOffsetDescription(java.lang.StringBuilder arg0);
    java.lang.String offsetDescription();
    long getCharOffset();
    java.lang.String sourceDescription();
    ContentReferenceInterface _wrap(java.lang.Object arg0);
    java.lang.Object getSourceRef();
    boolean equals(java.lang.Object arg0);
    int getLineNr();
    ContentReferenceInterface contentReference();
}
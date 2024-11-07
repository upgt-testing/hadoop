package org.apache.hadoop.hdfs.remoteProxies;

public interface ReferenceMapInterface {
    WithCountInterface loadINodeReferenceWithCount(boolean arg0, java.io.DataInput arg1, LoaderInterface arg2) throws java.io.IOException;
    void writeINodeReferenceWithCount(WithCountInterface arg0, java.io.DataOutput arg1, boolean arg2) throws java.io.IOException;
    boolean toProcessSubtree(long arg0);
}
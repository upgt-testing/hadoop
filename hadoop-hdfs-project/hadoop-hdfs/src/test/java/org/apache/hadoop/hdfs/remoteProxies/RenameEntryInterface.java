package org.apache.hadoop.hdfs.remoteProxies;

public interface RenameEntryInterface {
    byte[][] getTargetPath();
    boolean isRename();
    void setSource(INodeInterface arg0, byte[][] arg1);
    void setTarget(INodeInterface arg0, byte[][] arg1);
    byte[][] getSourcePath();
    void setTarget(byte[][] arg0);
}
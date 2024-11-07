package org.apache.hadoop.hdfs.remoteProxies;

public interface NameNodeResourceCheckerInterface {
    void setMinimumReduntdantVolumes(int arg0);
    boolean hasAvailableDiskSpace();
//    void setVolumes(java.util.Map<java.lang.String, org.apache.hadoop.hdfs.server.namenode.NameNodeResourceChecker.CheckedVolume> arg0);
    void addDirToCheck(java.net.URI arg0, boolean arg1) throws java.io.IOException;
    java.util.Collection<java.lang.String> getVolumesLowOnSpace() throws java.io.IOException;
}
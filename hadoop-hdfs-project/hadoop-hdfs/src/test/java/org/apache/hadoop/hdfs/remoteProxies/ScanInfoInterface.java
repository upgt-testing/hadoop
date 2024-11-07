package org.apache.hadoop.hdfs.remoteProxies;

public interface ScanInfoInterface {
    long getGenStamp();
    int hashCode();
    java.io.File getBlockFile();
    boolean equals(java.lang.Object arg0);
    long getBlockLength();
    java.io.File getMetaFile();
    java.lang.String fullMetaFile();
    java.lang.String getSuffix(java.lang.String arg0, java.lang.String arg1);
    long getBlockId();
    org.apache.hadoop.hdfs.server.datanode.fsdataset.FsVolumeSpi getVolume();
    int compareTo(ScanInfoInterface arg0);
    FileRegionInterface getFileRegion();
}
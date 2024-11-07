package org.apache.hadoop.hdfs.remoteProxies;

public interface ChecksumOptInterface {
    ChecksumOptInterface processChecksumOpt(ChecksumOptInterface arg0, ChecksumOptInterface arg1, int arg2);
    ChecksumOptInterface processChecksumOpt(ChecksumOptInterface arg0, ChecksumOptInterface arg1);
    org.apache.hadoop.util.DataChecksum.Type getChecksumType();
    ChecksumOptInterface createDisabled();
    java.lang.String toString();
    int getBytesPerChecksum();
}
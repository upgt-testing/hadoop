package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;

import org.apache.hadoop.hdfs.server.datanode.FileIoProvider;
import org.apache.hadoop.hdfs.server.datanode.fsdataset.FsVolumeSpi;

public interface ProfilingFileIoEventsInterface {

    long beforeMetadataOp(FsVolumeSpi volume, FileIoProvider.OPERATION op);

    void afterMetadataOp(FsVolumeSpi volume, FileIoProvider.OPERATION op, long begin);

    long beforeFileIo(FsVolumeSpi volume, FileIoProvider.OPERATION op, long len);

    void afterFileIo(FsVolumeSpi volume, FileIoProvider.OPERATION op, long begin, long len);

    void onFailure(FsVolumeSpi volume, long begin);

    void setSampleRangeMax(int fileIOSamplingPercentage);

    boolean getDiskStatsEnabled();

    int getSampleRangeMax();
}

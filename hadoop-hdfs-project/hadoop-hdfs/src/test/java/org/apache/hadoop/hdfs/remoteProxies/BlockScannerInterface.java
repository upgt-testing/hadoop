package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;
import org.apache.hadoop.hdfs.server.datanode.fsdataset.FsVolumeSpi;

public interface BlockScannerInterface {

    boolean isEnabled();

    boolean hasAnyRegisteredScanner();

    void addVolumeScanner(FsVolumeReferenceInterface ref);

    void removeVolumeScanner(FsVolumeSpi volume);

    void removeAllVolumeScanners();

    long getJoinVolumeScannersTimeOutMs();

    void setJoinVolumeScannersTimeOutMs(long joinScannersTimeOutMs);
}

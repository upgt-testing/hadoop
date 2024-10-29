package org.apache.hadoop.hdfs.remoteProxies;

import org.apache.hadoop.hdfs.protocol.SnapshotDiffReport;

import java.util.*;
import java.io.*;

public interface SnapshotDiffReportInterface {

    String getSnapshotRoot();

    String getFromSnapshot();

    String getLaterSnapshotName();

    SnapshotDiffReport.DiffStats getStats();

    List<SnapshotDiffReport.DiffReportEntry> getDiffList();

    String toString();
}

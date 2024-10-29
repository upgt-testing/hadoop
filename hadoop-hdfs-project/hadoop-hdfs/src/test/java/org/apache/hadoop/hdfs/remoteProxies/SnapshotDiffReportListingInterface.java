package org.apache.hadoop.hdfs.remoteProxies;

import org.apache.hadoop.hdfs.protocol.SnapshotDiffReportListing;

import java.util.*;
import java.io.*;

public interface SnapshotDiffReportListingInterface {

    List<SnapshotDiffReportListing.DiffReportListingEntry> getModifyList();

    List<SnapshotDiffReportListing.DiffReportListingEntry> getCreateList();

    List<SnapshotDiffReportListing.DiffReportListingEntry> getDeleteList();

    byte[] getLastPath();

    int getLastIndex();

    boolean getIsFromEarlier();
}

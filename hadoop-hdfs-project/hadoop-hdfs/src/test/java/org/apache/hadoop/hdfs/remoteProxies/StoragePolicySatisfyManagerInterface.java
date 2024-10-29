package org.apache.hadoop.hdfs.remoteProxies;

import org.apache.hadoop.hdfs.protocol.HdfsConstants;

import java.util.*;
import java.io.*;

public interface StoragePolicySatisfyManagerInterface {

    void start();

    void stop();

    void changeModeEvent(HdfsConstants.StoragePolicySatisfierMode newMode);

    boolean isSatisfierRunning();

    Long getNextPathId();

    void verifyOutstandingPathQLimit();

    void removeAllPathIds();

    void addPathId(long id);

    boolean isEnabled();

    HdfsConstants.StoragePolicySatisfierMode getMode();
}

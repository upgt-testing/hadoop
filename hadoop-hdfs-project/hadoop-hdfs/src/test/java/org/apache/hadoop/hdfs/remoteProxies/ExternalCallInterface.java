package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;
import org.apache.hadoop.security.UserGroupInformation;

public interface ExternalCallInterface {

    String getDetailedMetricsName();

    UserGroupInformationInterface getRemoteUser();

    //T get();

    Void run();
}

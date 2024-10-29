package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;

public interface HAContextInterface {

    void setState(HAStateInterface state);

    HAStateInterface getState();

    void startActiveServices();

    void stopActiveServices();

    void startStandbyServices();

    void prepareToStopStandbyServices();

    void stopStandbyServices();
}

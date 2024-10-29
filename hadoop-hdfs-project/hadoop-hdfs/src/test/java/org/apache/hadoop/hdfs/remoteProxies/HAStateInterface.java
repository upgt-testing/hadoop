package org.apache.hadoop.hdfs.remoteProxies;

import org.apache.hadoop.hdfs.server.namenode.NameNode;
import org.apache.hadoop.hdfs.server.namenode.ha.HAContext;
import org.apache.hadoop.hdfs.server.namenode.ha.HAState;

import java.util.*;
import java.io.*;

public interface HAStateInterface {

    //HAServiceState getServiceState();

    long getLastHATransitionTime();

    void prepareToEnterState(HAContext context);

    void enterState(HAContext context);

    void prepareToExitState(HAContext context);

    void exitState(HAContext context);

    void setState(HAContext context, HAState s);

    void checkOperation(HAContext context, NameNode.OperationCategory op);

    boolean shouldPopulateReplQueues();

    String toString();
}

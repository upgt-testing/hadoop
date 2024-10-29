package org.apache.hadoop.hdfs.remoteProxies;

import org.apache.hadoop.hdfs.protocol.ErasureCodingPolicyState;

import java.util.*;
import java.io.*;

public interface ErasureCodingPolicyInfoInterface {

    ErasureCodingPolicyInterface getPolicy();

    ErasureCodingPolicyState getState();

    void setState(ErasureCodingPolicyState newState);

    boolean isEnabled();

    boolean isDisabled();

    boolean isRemoved();

    boolean equals(Object o);

    int hashCode();

    String toString();
}

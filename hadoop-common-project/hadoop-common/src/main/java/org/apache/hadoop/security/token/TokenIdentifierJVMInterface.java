package org.apache.hadoop.security.token;

import org.apache.hadoop.io.WritableJVMInterface;

public interface TokenIdentifierJVMInterface extends WritableJVMInterface {

    org.apache.hadoop.security.UserGroupInformationJVMInterface getUser();

    java.lang.String getTrackingId();

    org.apache.hadoop.io.TextJVMInterface getKind();

    byte[] getBytes();
}

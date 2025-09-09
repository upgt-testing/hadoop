package org.apache.hadoop.fs;

import org.apache.hadoop.io.WritableJVMInterface;

public interface FileChecksumJVMInterface extends WritableJVMInterface {

    int hashCode();

    int getLength();

    java.lang.String getAlgorithmName();

    boolean equals(java.lang.Object arg0);

    byte[] getBytes();

    java.lang.Object getChecksumOpt();
}

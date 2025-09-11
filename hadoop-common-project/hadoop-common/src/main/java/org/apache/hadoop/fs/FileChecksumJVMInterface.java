package org.apache.hadoop.fs;

import org.apache.hadoop.io.WritableJVMInterface;

public interface FileChecksumJVMInterface extends WritableJVMInterface {

    int hashCode();

    int getLength();

    boolean equals(java.lang.Object arg0);

    java.lang.String getAlgorithmName();

    byte[] getBytes();

    java.lang.Object getChecksumOpt();
}

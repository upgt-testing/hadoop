package org.apache.hadoop.io;

public interface BinaryComparableJVMInterface {

    int getLength();

    int hashCode();

    boolean equals(java.lang.Object arg0);

    int compareTo_bridge(org.apache.hadoop.io.BinaryComparableJVMInterface arg0);

    int compareTo(byte[] arg0, int arg1, int arg2);

    byte[] getBytes();
}

package org.apache.hadoop.io;

public interface TextJVMInterface extends BinaryComparableJVMInterface, WritableComparableJVMInterface<org.apache.hadoop.io.BinaryComparable> {

    int getLength();

    int hashCode();

    int find(java.lang.String arg0);

    boolean equals(java.lang.Object arg0);

    void clear();

    void write_bridge(java.lang.Object arg0, int arg1) throws java.io.IOException;

    java.lang.String toString();

    int charAt(int arg0);

    void append(byte[] arg0, int arg1, int arg2);

    void readFields_bridge(java.lang.Object arg0) throws java.io.IOException;

    void readWithKnownLength_bridge(java.lang.Object arg0, int arg1) throws java.io.IOException;

    byte[] copyBytes();

    int find(java.lang.String arg0, int arg1);

    void set_bridge(org.apache.hadoop.io.TextJVMInterface arg0);

    void set(byte[] arg0, int arg1, int arg2);

    void readFields_bridge(java.lang.Object arg0, int arg1) throws java.io.IOException;

    void set(java.lang.String arg0);

    void write_bridge(java.lang.Object arg0) throws java.io.IOException;

    void set(byte[] arg0);

    byte[] getBytes();
}

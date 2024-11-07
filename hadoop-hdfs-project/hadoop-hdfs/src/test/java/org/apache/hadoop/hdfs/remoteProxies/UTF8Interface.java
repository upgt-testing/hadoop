package org.apache.hadoop.hdfs.remoteProxies;

public interface UTF8Interface {
    void write(java.io.DataOutput arg0) throws java.io.IOException;
    java.lang.String toString();
    byte[] getBytes();
    java.lang.String fromBytes(byte[] arg0) throws java.io.IOException;
    byte[] getBytes(java.lang.String arg0);
    char highSurrogate(int arg0);
    java.lang.String readString(java.io.DataInput arg0) throws java.io.IOException;
    int compareTo(UTF8Interface arg0);
    void set(UTF8Interface arg0);
    boolean equals(java.lang.Object arg0);
    java.lang.String toStringChecked() throws java.io.IOException;
    int getLength();
    int utf8Length(java.lang.String arg0);
    int hashCode();
    int writeString(java.io.DataOutput arg0, java.lang.String arg1) throws java.io.IOException;
    void readFields(java.io.DataInput arg0) throws java.io.IOException;
    void skip(java.io.DataInput arg0) throws java.io.IOException;
    char lowSurrogate(int arg0);
    void set(java.lang.String arg0);
    void readChars(java.io.DataInput arg0, java.lang.StringBuilder arg1, int arg2) throws java.io.UTFDataFormatException, java.io.IOException;
    void writeChars(java.io.DataOutput arg0, java.lang.String arg1, int arg2, int arg3) throws java.io.IOException;
}
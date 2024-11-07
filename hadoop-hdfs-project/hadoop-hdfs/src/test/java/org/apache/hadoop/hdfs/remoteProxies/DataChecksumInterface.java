package org.apache.hadoop.hdfs.remoteProxies;

public interface DataChecksumInterface {
    void update(byte[] arg0);
    void reset();
    void update(int arg0);
    void verifyChunked(org.apache.hadoop.util.DataChecksum.Type arg0, java.util.zip.Checksum arg1, java.nio.ByteBuffer arg2, int arg3, java.nio.ByteBuffer arg4, java.lang.String arg5, long arg6) throws org.apache.hadoop.fs.ChecksumException;
    int getBytesPerChecksum();
    long getValue();
    int getChecksumSize();
    void calculateChunkedSums(java.nio.ByteBuffer arg0, java.nio.ByteBuffer arg1);
    int getCrcPolynomialForType(org.apache.hadoop.util.DataChecksum.Type arg0) throws java.io.IOException;
    int getChecksumSize(int arg0);
    void calculateChunkedSums(byte[] arg0, int arg1, int arg2, byte[] arg3, int arg4);
    void update(byte[] arg0, int arg1, int arg2);
    void throwChecksumException(org.apache.hadoop.util.DataChecksum.Type arg0, java.util.zip.Checksum arg1, java.lang.String arg2, long arg3, int arg4, int arg5) throws org.apache.hadoop.fs.ChecksumException;
    java.util.zip.Checksum newCrc32C();
    void verifyChunkedSums(java.nio.ByteBuffer arg0, java.nio.ByteBuffer arg1, java.lang.String arg2, long arg3) throws org.apache.hadoop.fs.ChecksumException;
    void update(java.nio.ByteBuffer arg0);
    org.apache.hadoop.util.DataChecksum.Type mapByteToChecksumType(int arg0) throws org.apache.hadoop.util.InvalidChecksumSizeException;
    org.apache.hadoop.util.DataChecksum.Type getChecksumType();
    DataChecksumInterface newDataChecksum(java.io.DataInputStream arg0) throws java.io.IOException;
    void writeHeader(java.io.DataOutputStream arg0) throws java.io.IOException;
    boolean compare(byte[] arg0, int arg1);
    java.lang.String toString();
    int hashCode();
    void verifyChunked(org.apache.hadoop.util.DataChecksum.Type arg0, java.util.zip.Checksum arg1, byte[] arg2, int arg3, int arg4, int arg5, byte[] arg6, int arg7, java.lang.String arg8, long arg9) throws org.apache.hadoop.fs.ChecksumException;
    java.util.zip.Checksum newCrc32();
    DataChecksumInterface newDataChecksum(org.apache.hadoop.util.DataChecksum.Type arg0, int arg1);
    int getNumBytesInSum();
    int writeValue(java.io.DataOutputStream arg0, boolean arg1) throws java.io.IOException;
    int getChecksumHeaderSize();
    int writeValue(byte[] arg0, int arg1, boolean arg2) throws java.io.IOException;
    DataChecksumInterface newDataChecksum(byte[] arg0, int arg1) throws java.io.IOException;
    byte[] getHeader();
    boolean equals(java.lang.Object arg0);
}
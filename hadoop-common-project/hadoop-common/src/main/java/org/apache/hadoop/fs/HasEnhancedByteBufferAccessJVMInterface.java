package org.apache.hadoop.fs;

public interface HasEnhancedByteBufferAccessJVMInterface {

    java.nio.ByteBuffer read_bridge(java.lang.Object arg0, int arg1, java.util.EnumSet<org.apache.hadoop.fs.ReadOption> arg2) throws java.io.IOException, java.lang.UnsupportedOperationException;

    void releaseBuffer(java.nio.ByteBuffer arg0);
}

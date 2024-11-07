package org.apache.hadoop.hdfs.remoteProxies;

public interface CodedInputStreamInterface {
    void readMessage(org.apache.hadoop.thirdparty.protobuf.MessageLite.Builder arg0, ExtensionRegistryLiteInterface arg1) throws java.io.IOException;
    long readUInt64() throws java.io.IOException;
    <T> T readMessage(org.apache.hadoop.thirdparty.protobuf.Parser<T> arg0, ExtensionRegistryLiteInterface arg1) throws java.io.IOException;
    int readEnum() throws java.io.IOException;
    long readSFixed64() throws java.io.IOException;
    int readFixed32() throws java.io.IOException;
    int readRawLittleEndian32() throws java.io.IOException;
    int readSFixed32() throws java.io.IOException;
    CodedInputStreamInterface newInstance(java.nio.ByteBuffer arg0, boolean arg1);
    void checkLastTagWas(int arg0) throws org.apache.hadoop.thirdparty.protobuf.InvalidProtocolBufferException;
    int readSInt32() throws java.io.IOException;
    boolean skipField(int arg0) throws java.io.IOException;
    void skipMessage() throws java.io.IOException;
    int setRecursionLimit(int arg0);
    int getTotalBytesRead();
    double readDouble() throws java.io.IOException;
    int decodeZigZag32(int arg0);
    CodedInputStreamInterface newInstance(java.lang.Iterable<java.nio.ByteBuffer> arg0, boolean arg1);
    long readSInt64() throws java.io.IOException;
    CodedInputStreamInterface newInstance(java.io.InputStream arg0);
    long readFixed64() throws java.io.IOException;
    void popLimit(int arg0);
    void unsetDiscardUnknownFields();
    <T> T readGroup(int arg0, org.apache.hadoop.thirdparty.protobuf.Parser<T> arg1, ExtensionRegistryLiteInterface arg2) throws java.io.IOException;
    long readRawVarint64() throws java.io.IOException;
    int readInt32() throws java.io.IOException;
    int readRawVarint32() throws java.io.IOException;
    int readRawVarint32(int arg0, java.io.InputStream arg1) throws java.io.IOException;
    CodedInputStreamInterface newInstance(java.io.InputStream arg0, int arg1);
    float readFloat() throws java.io.IOException;
    CodedInputStreamInterface newInstance(java.lang.Iterable<java.nio.ByteBuffer> arg0);
    int readUInt32() throws java.io.IOException;
    int readRawVarint32(java.io.InputStream arg0) throws java.io.IOException;
    CodedInputStreamInterface newInstance(byte[] arg0, int arg1, int arg2, boolean arg3);
    CodedInputStreamInterface newInstance(java.nio.ByteBuffer arg0);
    void readUnknownGroup(int arg0, org.apache.hadoop.thirdparty.protobuf.MessageLite.Builder arg1) throws java.io.IOException;
    void readGroup(int arg0, org.apache.hadoop.thirdparty.protobuf.MessageLite.Builder arg1, ExtensionRegistryLiteInterface arg2) throws java.io.IOException;
    boolean skipField(int arg0, CodedOutputStreamInterface arg1) throws java.io.IOException;
    CodedInputStreamInterface newInstance(byte[] arg0, int arg1, int arg2);
    java.lang.String readString() throws java.io.IOException;
    void enableAliasing(boolean arg0);
    CodedInputStreamInterface newInstance(byte[] arg0);
    long readInt64() throws java.io.IOException;
    int readTag() throws java.io.IOException;
    void skipRawBytes(int arg0) throws java.io.IOException;
    boolean shouldDiscardUnknownFields();
    long readRawVarint64SlowPath() throws java.io.IOException;
    int getBytesUntilLimit();
    ByteStringInterface readBytes() throws java.io.IOException;
    byte[] readByteArray() throws java.io.IOException;
    byte[] readRawBytes(int arg0) throws java.io.IOException;
    long readRawLittleEndian64() throws java.io.IOException;
    boolean isAtEnd() throws java.io.IOException;
    void resetSizeCounter();
    long decodeZigZag64(long arg0);
    int pushLimit(int arg0) throws org.apache.hadoop.thirdparty.protobuf.InvalidProtocolBufferException;
    int setSizeLimit(int arg0);
    boolean readBool() throws java.io.IOException;
    java.lang.String readStringRequireUtf8() throws java.io.IOException;
    java.nio.ByteBuffer readByteBuffer() throws java.io.IOException;
    void discardUnknownFields();
    void skipMessage(CodedOutputStreamInterface arg0) throws java.io.IOException;
    int getLastTag();
    byte readRawByte() throws java.io.IOException;
}
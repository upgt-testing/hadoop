package org.apache.hadoop.hdfs.remoteProxies;

public interface AbstractMessageLiteInterface<MessageType, BuilderType> {
    <T> void addAll(java.lang.Iterable<T> arg0, java.util.Collection<? super T> arg1);
    java.lang.String getSerializingExceptionMessage(java.lang.String arg0);
    void writeTo(java.io.OutputStream arg0) throws java.io.IOException;
    int getSerializedSize();
    void setMemoizedSerializedSize(int arg0);
    boolean isInitialized();
    byte[] toByteArray();
    org.apache.hadoop.thirdparty.protobuf.MessageLite.Builder toBuilder();
    UninitializedMessageExceptionInterface newUninitializedMessageException();
    org.apache.hadoop.thirdparty.protobuf.Parser<? extends org.apache.hadoop.thirdparty.protobuf.MessageLite> getParserForType();
    void writeDelimitedTo(java.io.OutputStream arg0) throws java.io.IOException;
    org.apache.hadoop.thirdparty.protobuf.MessageLite.Builder newBuilderForType();
    org.apache.hadoop.thirdparty.protobuf.MessageLite getDefaultInstanceForType();
    ByteStringInterface toByteString();
    <T> void addAll(java.lang.Iterable<T> arg0, java.util.List<? super T> arg1);
    void writeTo(CodedOutputStreamInterface arg0) throws java.io.IOException;
    int getMemoizedSerializedSize();
    void checkByteStringIsUtf8(ByteStringInterface arg0) throws java.lang.IllegalArgumentException;
}
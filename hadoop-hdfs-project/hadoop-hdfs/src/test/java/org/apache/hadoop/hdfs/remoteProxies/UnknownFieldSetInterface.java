package org.apache.hadoop.hdfs.remoteProxies;

public interface UnknownFieldSetInterface {
    java.lang.String toString();
    BuilderInterface newBuilderForType();
    void writeDelimitedTo(java.io.OutputStream arg0) throws java.io.IOException;
    int getSerializedSize();
    BuilderInterface newBuilder();
    BuilderInterface newBuilder(UnknownFieldSetInterface arg0);
    boolean hasField(int arg0);
    UnknownFieldSetInterface getDefaultInstanceForType();
    byte[] toByteArray();
    int hashCode();
    UnknownFieldSetInterface getDefaultInstance();
    UnknownFieldSetInterface parseFrom(ByteStringInterface arg0) throws org.apache.hadoop.thirdparty.protobuf.InvalidProtocolBufferException;
    void writeTo(java.io.OutputStream arg0) throws java.io.IOException;
    ByteStringInterface toByteString();
    BuilderInterface toBuilder();
//    org.apache.hadoop.thirdparty.protobuf.MessageLite.Builder toBuilder();
    int getSerializedSizeAsMessageSet();
//    org.apache.hadoop.thirdparty.protobuf.MessageLite getDefaultInstanceForType();
    void writeTo(CodedOutputStreamInterface arg0) throws java.io.IOException;
    java.util.Map<java.lang.Integer, org.apache.hadoop.thirdparty.protobuf.UnknownFieldSet.Field> asMap();
    UnknownFieldSetInterface parseFrom(java.io.InputStream arg0) throws java.io.IOException;
    void writeAsMessageSetTo(CodedOutputStreamInterface arg0) throws java.io.IOException;
//    org.apache.hadoop.thirdparty.protobuf.MessageLite.Builder newBuilderForType();
    boolean equals(java.lang.Object arg0);
    boolean isInitialized();
    FieldInterface getField(int arg0);
    UnknownFieldSetInterface parseFrom(byte[] arg0) throws org.apache.hadoop.thirdparty.protobuf.InvalidProtocolBufferException;
    org.apache.hadoop.thirdparty.protobuf.Parser<? extends org.apache.hadoop.thirdparty.protobuf.MessageLite> getParserForType();
//    ParserInterface getParserForType();
    UnknownFieldSetInterface parseFrom(CodedInputStreamInterface arg0) throws java.io.IOException;
}
package org.apache.hadoop.hdfs.remoteProxies;

public interface LazyFieldLiteInterface {
    org.apache.hadoop.thirdparty.protobuf.MessageLite setValue(org.apache.hadoop.thirdparty.protobuf.MessageLite arg0);
    void checkArguments(ExtensionRegistryLiteInterface arg0, ByteStringInterface arg1);
    LazyFieldLiteInterface fromValue(org.apache.hadoop.thirdparty.protobuf.MessageLite arg0);
    boolean containsDefaultInstance();
    org.apache.hadoop.thirdparty.protobuf.MessageLite getValue(org.apache.hadoop.thirdparty.protobuf.MessageLite arg0);
    void clear();
    void merge(LazyFieldLiteInterface arg0);
    void setByteString(ByteStringInterface arg0, ExtensionRegistryLiteInterface arg1);
    void mergeFrom(CodedInputStreamInterface arg0, ExtensionRegistryLiteInterface arg1) throws java.io.IOException;
    void set(LazyFieldLiteInterface arg0);
    boolean equals(java.lang.Object arg0);
    ByteStringInterface toByteString();
    int getSerializedSize();
    void ensureInitialized(org.apache.hadoop.thirdparty.protobuf.MessageLite arg0);
    int hashCode();
    org.apache.hadoop.thirdparty.protobuf.MessageLite mergeValueAndBytes(org.apache.hadoop.thirdparty.protobuf.MessageLite arg0, ByteStringInterface arg1, ExtensionRegistryLiteInterface arg2);
}
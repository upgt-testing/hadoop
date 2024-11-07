package org.apache.hadoop.hdfs.remoteProxies;

public interface FieldInterface {
    java.lang.Object[] getIdentityArray();
    void writeAsMessageSetExtensionTo(int arg0, CodedOutputStreamInterface arg1) throws java.io.IOException;
    ByteStringInterface toByteString(int arg0);
    void writeTo(int arg0, CodedOutputStreamInterface arg1) throws java.io.IOException;
    java.util.List<org.apache.hadoop.thirdparty.protobuf.UnknownFieldSet> getGroupList();
    int getSerializedSizeAsMessageSetExtension(int arg0);
    int getSerializedSize(int arg0);
    int hashCode();
    BuilderInterface newBuilder(FieldInterface arg0);
    FieldInterface getDefaultInstance();
    java.util.List<java.lang.Integer> getFixed32List();
    java.util.List<java.lang.Long> getVarintList();
    java.util.List<org.apache.hadoop.thirdparty.protobuf.ByteString> getLengthDelimitedList();
    boolean equals(java.lang.Object arg0);
    java.util.List<java.lang.Long> getFixed64List();
    BuilderInterface newBuilder();
}
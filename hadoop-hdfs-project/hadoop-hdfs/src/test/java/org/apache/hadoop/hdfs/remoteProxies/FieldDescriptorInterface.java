package org.apache.hadoop.hdfs.remoteProxies;

public interface FieldDescriptorInterface {
    EnumDescriptorInterface getEnumType();
    boolean isOptional();
    DescriptorInterface getMessageType();
    boolean isPackable();
    boolean isRepeated();
    boolean isExtension();
    FieldOptionsInterface getOptions();
    java.lang.String getFullName();
    DescriptorInterface getContainingType();
    void setProto(FieldDescriptorProtoInterface arg0);
//    org.apache.hadoop.thirdparty.protobuf.Internal.EnumLiteMap<?> getEnumType();
    org.apache.hadoop.thirdparty.protobuf.WireFormat.JavaType getLiteJavaType();
    int getNumber();
    boolean isRequired();
    org.apache.hadoop.thirdparty.protobuf.Descriptors.FieldDescriptor.Type getType();
    int compareTo(FieldDescriptorInterface arg0);
    org.apache.hadoop.thirdparty.protobuf.WireFormat.FieldType getLiteType();
    void crossLink() throws org.apache.hadoop.thirdparty.protobuf.Descriptors.DescriptorValidationException;
    boolean isPacked();
    FileDescriptorInterface getFile();
    java.lang.String getName();
    org.apache.hadoop.thirdparty.protobuf.Message toProto();
    org.apache.hadoop.thirdparty.protobuf.Descriptors.FieldDescriptor.JavaType getJavaType();
    boolean hasDefaultValue();
    OneofDescriptorInterface getContainingOneof();
//    FieldDescriptorProtoInterface toProto();
    java.lang.String getJsonName();
    boolean isMapField();
    int getIndex();
    java.lang.String fieldNameToJsonName(java.lang.String arg0);
    java.lang.String toString();
    org.apache.hadoop.thirdparty.protobuf.MessageLite.Builder internalMergeFrom(org.apache.hadoop.thirdparty.protobuf.MessageLite.Builder arg0, org.apache.hadoop.thirdparty.protobuf.MessageLite arg1);
    DescriptorInterface getExtensionScope();
    java.lang.Object getDefaultValue();
    boolean needsUtf8Check();
}
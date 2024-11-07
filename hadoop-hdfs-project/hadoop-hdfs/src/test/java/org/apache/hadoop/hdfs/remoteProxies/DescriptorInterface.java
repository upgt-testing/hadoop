package org.apache.hadoop.hdfs.remoteProxies;

public interface DescriptorInterface {
    java.util.List<org.apache.hadoop.thirdparty.protobuf.Descriptors.FieldDescriptor> getExtensions();
    java.lang.String getFullName();
    FileDescriptorInterface getFile();
    boolean isReservedName(java.lang.String arg0);
    org.apache.hadoop.thirdparty.protobuf.Message toProto();
    MessageOptionsInterface getOptions();
    DescriptorInterface getContainingType();
    DescriptorInterface findNestedTypeByName(java.lang.String arg0);
    void setProto(DescriptorProtoInterface arg0);
    boolean isReservedNumber(int arg0);
    java.util.List<org.apache.hadoop.thirdparty.protobuf.Descriptors.EnumDescriptor> getEnumTypes();
    boolean isExtensionNumber(int arg0);
    java.util.List<org.apache.hadoop.thirdparty.protobuf.Descriptors.Descriptor> getNestedTypes();
    boolean isExtendable();
    java.util.List<org.apache.hadoop.thirdparty.protobuf.Descriptors.OneofDescriptor> getOneofs();
    FieldDescriptorInterface findFieldByNumber(int arg0);
    void crossLink() throws org.apache.hadoop.thirdparty.protobuf.Descriptors.DescriptorValidationException;
    int getIndex();
    EnumDescriptorInterface findEnumTypeByName(java.lang.String arg0);
    FieldDescriptorInterface findFieldByName(java.lang.String arg0);
//    DescriptorProtoInterface toProto();
    java.lang.String getName();
    java.util.List<org.apache.hadoop.thirdparty.protobuf.Descriptors.FieldDescriptor> getFields();
}
package org.apache.hadoop.hdfs.remoteProxies;

public interface EnumDescriptorInterface {
    EnumValueDescriptorInterface findValueByNumberCreatingIfUnknown(int arg0);
    FileDescriptorInterface getFile();
    int getIndex();
    org.apache.hadoop.thirdparty.protobuf.Message toProto();
    java.util.List<org.apache.hadoop.thirdparty.protobuf.Descriptors.EnumValueDescriptor> getValues();
    int getUnknownEnumValueDescriptorCount();
    java.lang.String getName();
    java.lang.String getFullName();
    DescriptorInterface getContainingType();
    EnumOptionsInterface getOptions();
    EnumValueDescriptorInterface findValueByName(java.lang.String arg0);
//    EnumDescriptorProtoInterface toProto();
    EnumValueDescriptorInterface findValueByNumber(int arg0);
//    T findValueByNumber(int arg0);
    void setProto(EnumDescriptorProtoInterface arg0);
}
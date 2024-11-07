package org.apache.hadoop.hdfs.remoteProxies;

public interface OneofDescriptorInterface {
    void setProto(OneofDescriptorProtoInterface arg0);
    java.lang.String getFullName();
    int getIndex();
    DescriptorInterface getContainingType();
    FileDescriptorInterface getFile();
    java.lang.String getName();
    OneofOptionsInterface getOptions();
    int getFieldCount();
    java.util.List<org.apache.hadoop.thirdparty.protobuf.Descriptors.FieldDescriptor> getFields();
    FieldDescriptorInterface getField(int arg0);
}
package org.apache.hadoop.hdfs.remoteProxies;

public interface EnumValueDescriptorInterface {
    java.lang.String toString();
    int getNumber();
    FileDescriptorInterface getFile();
    EnumValueOptionsInterface getOptions();
    int getIndex();
    java.lang.String getName();
    EnumValueDescriptorProtoInterface toProto();
    java.lang.String getFullName();
    EnumDescriptorInterface getType();
//    org.apache.hadoop.thirdparty.protobuf.Message toProto();
    void setProto(EnumValueDescriptorProtoInterface arg0);
}
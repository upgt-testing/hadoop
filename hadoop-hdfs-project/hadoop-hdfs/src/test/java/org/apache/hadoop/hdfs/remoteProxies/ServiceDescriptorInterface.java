package org.apache.hadoop.hdfs.remoteProxies;

public interface ServiceDescriptorInterface {
    int getIndex();
    ServiceDescriptorProtoInterface toProto();
    FileDescriptorInterface getFile();
    ServiceOptionsInterface getOptions();
    MethodDescriptorInterface findMethodByName(java.lang.String arg0);
    java.lang.String getName();
    void crossLink() throws org.apache.hadoop.thirdparty.protobuf.Descriptors.DescriptorValidationException;
    java.util.List<org.apache.hadoop.thirdparty.protobuf.Descriptors.MethodDescriptor> getMethods();
//    org.apache.hadoop.thirdparty.protobuf.Message toProto();
    java.lang.String getFullName();
    void setProto(ServiceDescriptorProtoInterface arg0);
}
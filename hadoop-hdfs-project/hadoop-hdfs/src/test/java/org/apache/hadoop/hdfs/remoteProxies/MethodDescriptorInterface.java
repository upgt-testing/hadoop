package org.apache.hadoop.hdfs.remoteProxies;

public interface MethodDescriptorInterface {
    MethodOptionsInterface getOptions();
    void crossLink() throws org.apache.hadoop.thirdparty.protobuf.Descriptors.DescriptorValidationException;
    DescriptorInterface getOutputType();
    java.lang.String getName();
    int getIndex();
    FileDescriptorInterface getFile();
    ServiceDescriptorInterface getService();
    java.lang.String getFullName();
    DescriptorInterface getInputType();
    MethodDescriptorProtoInterface toProto();
    void setProto(MethodDescriptorProtoInterface arg0);
//    org.apache.hadoop.thirdparty.protobuf.Message toProto();
}
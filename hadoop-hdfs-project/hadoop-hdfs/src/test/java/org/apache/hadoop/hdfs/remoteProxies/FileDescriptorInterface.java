package org.apache.hadoop.hdfs.remoteProxies;

public interface FileDescriptorInterface {
    FileDescriptorInterface buildFrom(FileDescriptorProtoInterface arg0, FileDescriptorInterface[] arg1) throws org.apache.hadoop.thirdparty.protobuf.Descriptors.DescriptorValidationException;
    java.util.List<org.apache.hadoop.thirdparty.protobuf.Descriptors.Descriptor> getMessageTypes();
    org.apache.hadoop.thirdparty.protobuf.Message toProto();
    void crossLink() throws org.apache.hadoop.thirdparty.protobuf.Descriptors.DescriptorValidationException;
    java.util.List<org.apache.hadoop.thirdparty.protobuf.Descriptors.FileDescriptor> getDependencies();
    java.lang.String getPackage();
    java.util.List<org.apache.hadoop.thirdparty.protobuf.Descriptors.ServiceDescriptor> getServices();
    java.util.List<org.apache.hadoop.thirdparty.protobuf.Descriptors.EnumDescriptor> getEnumTypes();
    FileDescriptorInterface getFile();
    DescriptorInterface findMessageTypeByName(java.lang.String arg0);
//    FileDescriptorProtoInterface toProto();
    void internalBuildGeneratedFileFrom(java.lang.String[] arg0, java.lang.Class<?> arg1, java.lang.String[] arg2, java.lang.String[] arg3, org.apache.hadoop.thirdparty.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner arg4);
    org.apache.hadoop.thirdparty.protobuf.Descriptors.FileDescriptor.Syntax getSyntax();
    ServiceDescriptorInterface findServiceByName(java.lang.String arg0);
    java.lang.String getName();
    FileOptionsInterface getOptions();
    void setProto(FileDescriptorProtoInterface arg0);
    java.util.List<org.apache.hadoop.thirdparty.protobuf.Descriptors.FieldDescriptor> getExtensions();
    void internalBuildGeneratedFileFrom(java.lang.String[] arg0, FileDescriptorInterface[] arg1, org.apache.hadoop.thirdparty.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner arg2);
    boolean supportsUnknownEnumValue();
    EnumDescriptorInterface findEnumTypeByName(java.lang.String arg0);
    FieldDescriptorInterface findExtensionByName(java.lang.String arg0);
    FileDescriptorInterface buildFrom(FileDescriptorProtoInterface arg0, FileDescriptorInterface[] arg1, boolean arg2) throws org.apache.hadoop.thirdparty.protobuf.Descriptors.DescriptorValidationException;
    java.lang.String getFullName();
    void internalUpdateFileDescriptor(FileDescriptorInterface arg0, ExtensionRegistryInterface arg1);
    java.util.List<org.apache.hadoop.thirdparty.protobuf.Descriptors.FileDescriptor> getPublicDependencies();
}
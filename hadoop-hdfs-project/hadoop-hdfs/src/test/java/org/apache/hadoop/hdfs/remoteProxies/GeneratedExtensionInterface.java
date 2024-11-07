package org.apache.hadoop.hdfs.remoteProxies;

public interface GeneratedExtensionInterface<ContainingType, Type> {
    org.apache.hadoop.thirdparty.protobuf.MessageLite getMessageDefaultInstance();
    void internalInit(FieldDescriptorInterface arg0);
    java.lang.Object toReflectionType(java.lang.Object arg0);
//    org.apache.hadoop.thirdparty.protobuf.Extension.ExtensionType getExtensionType();
    boolean isLite();
    boolean isRepeated();
    org.apache.hadoop.thirdparty.protobuf.WireFormat.FieldType getLiteType();
    FieldDescriptorInterface getDescriptor();
//    org.apache.hadoop.thirdparty.protobuf.Message getMessageDefaultInstance();
    java.lang.Object singularToReflectionType(java.lang.Object arg0);
    int getNumber();
    java.lang.Object fromReflectionType(java.lang.Object arg0);
    java.lang.Object singularFromReflectionType(java.lang.Object arg0);
    Type getDefaultValue();
    org.apache.hadoop.thirdparty.protobuf.Extension.MessageType getMessageType();
}
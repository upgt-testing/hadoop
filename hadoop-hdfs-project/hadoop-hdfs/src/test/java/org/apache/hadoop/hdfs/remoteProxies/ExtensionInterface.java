package org.apache.hadoop.hdfs.remoteProxies;

public interface ExtensionInterface<ContainingType, Type> {
    java.lang.Object singularToReflectionType(java.lang.Object arg0);
    boolean isLite();
    java.lang.Object fromReflectionType(java.lang.Object arg0);
//    org.apache.hadoop.thirdparty.protobuf.Extension.ExtensionType getExtensionType();
    Type getDefaultValue();
    java.lang.Object singularFromReflectionType(java.lang.Object arg0);
    org.apache.hadoop.thirdparty.protobuf.WireFormat.FieldType getLiteType();
    org.apache.hadoop.thirdparty.protobuf.Extension.MessageType getMessageType();
    org.apache.hadoop.thirdparty.protobuf.MessageLite getMessageDefaultInstance();
    java.lang.Object toReflectionType(java.lang.Object arg0);
    FieldDescriptorInterface getDescriptor();
    boolean isRepeated();
    int getNumber();
}
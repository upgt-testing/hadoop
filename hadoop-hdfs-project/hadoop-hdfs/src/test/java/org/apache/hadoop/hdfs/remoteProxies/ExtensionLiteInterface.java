package org.apache.hadoop.hdfs.remoteProxies;

public interface ExtensionLiteInterface<ContainingType, Type> {
    Type getDefaultValue();
    org.apache.hadoop.thirdparty.protobuf.MessageLite getMessageDefaultInstance();
    org.apache.hadoop.thirdparty.protobuf.WireFormat.FieldType getLiteType();
    boolean isLite();
    boolean isRepeated();
    int getNumber();
}
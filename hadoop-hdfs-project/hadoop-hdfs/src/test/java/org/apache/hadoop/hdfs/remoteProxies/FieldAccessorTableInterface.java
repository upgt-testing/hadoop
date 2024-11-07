package org.apache.hadoop.hdfs.remoteProxies;

public interface FieldAccessorTableInterface {
    OneofAccessorInterface getOneof(OneofDescriptorInterface arg0);
//    org.apache.hadoop.thirdparty.protobuf.GeneratedMessageV3.FieldAccessorTable.FieldAccessor getField(FieldDescriptorInterface arg0);
    boolean supportFieldPresence(FileDescriptorInterface arg0);
    FieldAccessorTableInterface ensureFieldAccessorsInitialized(java.lang.Class<? extends org.apache.hadoop.thirdparty.protobuf.GeneratedMessageV3> arg0, java.lang.Class<? extends org.apache.hadoop.thirdparty.protobuf.GeneratedMessageV3.Builder> arg1);
}
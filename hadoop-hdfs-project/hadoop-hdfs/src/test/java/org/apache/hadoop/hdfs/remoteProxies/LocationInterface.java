package org.apache.hadoop.hdfs.remoteProxies;

public interface LocationInterface {
    <M> M parseDelimitedWithIOException(org.apache.hadoop.thirdparty.protobuf.Parser<M> arg0, java.io.InputStream arg1, ExtensionRegistryLiteInterface arg2) throws java.io.IOException;
    byte[] toByteArray();
    org.apache.hadoop.thirdparty.protobuf.Internal.IntList newIntList();
    int hashMapField(java.lang.Object arg0);
    int getMemoizedSerializedSize();
    java.lang.String getLeadingDetachedComments(int arg0);
    void writeDelimitedTo(java.io.OutputStream arg0) throws java.io.IOException;
    boolean equals(java.lang.Object arg0);
    boolean isInitialized();
    MapFieldInterface internalGetMapField(int arg0);
    void writeString(CodedOutputStreamInterface arg0, int arg1, java.lang.Object arg2) throws java.io.IOException;
    LocationInterface getDefaultInstance();
    org.apache.hadoop.thirdparty.protobuf.Internal.LongList mutableCopy(org.apache.hadoop.thirdparty.protobuf.Internal.LongList arg0);
    <K, V> void serializeMapTo(CodedOutputStreamInterface arg0, java.util.Map<K, V> arg1, MapEntryInterface<K, V> arg2, int arg3) throws java.io.IOException;
    LocationInterface parseFrom(CodedInputStreamInterface arg0, ExtensionRegistryLiteInterface arg1) throws java.io.IOException;
    LocationInterface parseDelimitedFrom(java.io.InputStream arg0, ExtensionRegistryLiteInterface arg1) throws java.io.IOException;
    java.util.Map<org.apache.hadoop.thirdparty.protobuf.Descriptors.FieldDescriptor, java.lang.Object> getAllFieldsRaw();
    DescriptorInterface getDescriptorForType();
    ByteStringInterface getLeadingDetachedCommentsBytes(int arg0);
    int getRepeatedFieldCount(FieldDescriptorInterface arg0);
    int getPathCount();
    <M> M parseWithIOException(org.apache.hadoop.thirdparty.protobuf.Parser<M> arg0, CodedInputStreamInterface arg1) throws java.io.IOException;
    boolean hasOneof(OneofDescriptorInterface arg0);
    java.lang.String getTrailingComments();
    java.lang.String getSerializingExceptionMessage(java.lang.String arg0);
    java.util.List<java.lang.String> getLeadingDetachedCommentsList();
    LocationInterface getDefaultInstanceForType();
    BuilderInterface newBuilderForType();
    boolean hasField(FieldDescriptorInterface arg0);
    <V> void maybeSerializeBooleanEntryTo(CodedOutputStreamInterface arg0, java.util.Map<java.lang.Boolean, V> arg1, MapEntryInterface<java.lang.Boolean, V> arg2, int arg3, boolean arg4) throws java.io.IOException;
    LocationInterface parseFrom(java.nio.ByteBuffer arg0) throws org.apache.hadoop.thirdparty.protobuf.InvalidProtocolBufferException;
    int getLeadingDetachedCommentsCount();
    UnknownFieldSetInterface getUnknownFields();
//    org.apache.hadoop.thirdparty.protobuf.Message.Builder newBuilderForType(org.apache.hadoop.thirdparty.protobuf.GeneratedMessageV3.BuilderParent arg0);
    java.lang.Object invokeOrDie(java.lang.reflect.Method arg0, java.lang.Object arg1, java.lang.Object... arg2);
    UninitializedMessageExceptionInterface newUninitializedMessageException();
    LocationInterface parseFrom(ByteStringInterface arg0) throws org.apache.hadoop.thirdparty.protobuf.InvalidProtocolBufferException;
    org.apache.hadoop.thirdparty.protobuf.Internal.DoubleList mutableCopy(org.apache.hadoop.thirdparty.protobuf.Internal.DoubleList arg0);
    boolean compareFields(java.util.Map<org.apache.hadoop.thirdparty.protobuf.Descriptors.FieldDescriptor, java.lang.Object> arg0, java.util.Map<org.apache.hadoop.thirdparty.protobuf.Descriptors.FieldDescriptor, java.lang.Object> arg1);
    org.apache.hadoop.thirdparty.protobuf.Internal.FloatList mutableCopy(org.apache.hadoop.thirdparty.protobuf.Internal.FloatList arg0);
    java.lang.Object getField(FieldDescriptorInterface arg0);
    java.util.Map convertMapEntryListToMap(java.util.List arg0);
    int hashLong(long arg0);
//    org.apache.hadoop.thirdparty.protobuf.MessageLite.Builder newBuilderForType();
//    BuilderInterface newBuilderForType(org.apache.hadoop.thirdparty.protobuf.GeneratedMessageV3.BuilderParent arg0);
    org.apache.hadoop.thirdparty.protobuf.Internal.DoubleList emptyDoubleList();
    DescriptorInterface getDescriptor();
    java.lang.Object writeReplace() throws java.io.ObjectStreamException;
    org.apache.hadoop.thirdparty.protobuf.Parser<org.apache.hadoop.thirdparty.protobuf.DescriptorProtos.SourceCodeInfo.Location> getParserForType();
//    org.apache.hadoop.thirdparty.protobuf.Parser<? extends org.apache.hadoop.thirdparty.protobuf.GeneratedMessageV3> getParserForType();
//    org.apache.hadoop.thirdparty.protobuf.Parser<? extends org.apache.hadoop.thirdparty.protobuf.MessageLite> getParserForType();
    void writeTo(CodedOutputStreamInterface arg0) throws java.io.IOException;
    boolean hasLeadingComments();
    java.util.Map<org.apache.hadoop.thirdparty.protobuf.Descriptors.FieldDescriptor, java.lang.Object> getAllFieldsMutable(boolean arg0);
    java.lang.String toString();
    LocationInterface parseFrom(byte[] arg0, ExtensionRegistryLiteInterface arg1) throws org.apache.hadoop.thirdparty.protobuf.InvalidProtocolBufferException;
    org.apache.hadoop.thirdparty.protobuf.Internal.LongList emptyLongList();
    void writeTo(java.io.OutputStream arg0) throws java.io.IOException;
    boolean compareMapField(java.lang.Object arg0, java.lang.Object arg1);
    void enableAlwaysUseFieldBuildersForTesting();
    <MessageType, T> ExtensionInterface<MessageType, T> checkNotLite(ExtensionLiteInterface<MessageType, T> arg0);
    int hashFields(int arg0, java.util.Map<org.apache.hadoop.thirdparty.protobuf.Descriptors.FieldDescriptor, java.lang.Object> arg1);
    boolean hasTrailingComments();
    ByteStringInterface getTrailingCommentsBytes();
    org.apache.hadoop.thirdparty.protobuf.MessageLite.Builder toBuilder();
    java.util.List<java.lang.Integer> getPathList();
    BuilderInterface newBuilder();
    java.lang.reflect.Method getMethodOrDie(java.lang.Class arg0, java.lang.String arg1, java.lang.Class... arg2);
    int getPath(int arg0);
//    org.apache.hadoop.thirdparty.protobuf.Message.Builder newBuilderForType();
    int hashEnum(org.apache.hadoop.thirdparty.protobuf.Internal.EnumLite arg0);
//    org.apache.hadoop.thirdparty.protobuf.Message.Builder newBuilderForType(org.apache.hadoop.thirdparty.protobuf.AbstractMessage.BuilderParent arg0);
    int computeStringSizeNoTag(java.lang.Object arg0);
    java.util.List<java.lang.String> findInitializationErrors();
    LocationInterface parseFrom(java.nio.ByteBuffer arg0, ExtensionRegistryLiteInterface arg1) throws org.apache.hadoop.thirdparty.protobuf.InvalidProtocolBufferException;
    LocationInterface parseFrom(java.io.InputStream arg0) throws java.io.IOException;
    LocationInterface parseFrom(CodedInputStreamInterface arg0) throws java.io.IOException;
    int getSpanCount();
    org.apache.hadoop.thirdparty.protobuf.Internal.DoubleList newDoubleList();
    void checkByteStringIsUtf8(ByteStringInterface arg0) throws java.lang.IllegalArgumentException;
    FieldAccessorTableInterface internalGetFieldAccessorTable();
    <V> void serializeIntegerMapTo(CodedOutputStreamInterface arg0, MapFieldInterface<java.lang.Integer, V> arg1, MapEntryInterface<java.lang.Integer, V> arg2, int arg3) throws java.io.IOException;
    ByteStringInterface toByteString(java.lang.Object arg0);
    boolean compareBytes(java.lang.Object arg0, java.lang.Object arg1);
    java.util.List<java.lang.Integer> getSpanList();
    void writeStringNoTag(CodedOutputStreamInterface arg0, java.lang.Object arg1) throws java.io.IOException;
//    org.apache.hadoop.thirdparty.protobuf.MessageLite getDefaultInstanceForType();
    org.apache.hadoop.thirdparty.protobuf.Internal.LongList newLongList();
    int computeStringSize(int arg0, java.lang.Object arg1);
    ByteStringInterface getLeadingCommentsBytes();
    void setMemoizedSerializedSize(int arg0);
    <M> M parseWithIOException(org.apache.hadoop.thirdparty.protobuf.Parser<M> arg0, java.io.InputStream arg1, ExtensionRegistryLiteInterface arg2) throws java.io.IOException;
//    org.apache.hadoop.thirdparty.protobuf.ProtocolStringList getLeadingDetachedCommentsList();
    java.lang.Object getFieldRaw(FieldDescriptorInterface arg0);
//    org.apache.hadoop.thirdparty.protobuf.Message.Builder toBuilder();
    boolean parseUnknownFieldProto3(CodedInputStreamInterface arg0, BuilderInterface arg1, ExtensionRegistryLiteInterface arg2, int arg3) throws java.io.IOException;
    boolean parseUnknownField(CodedInputStreamInterface arg0, BuilderInterface arg1, ExtensionRegistryLiteInterface arg2, int arg3) throws java.io.IOException;
    int hashCode();
    java.util.Map<org.apache.hadoop.thirdparty.protobuf.Descriptors.FieldDescriptor, java.lang.Object> getAllFields();
    <V> void serializeBooleanMapTo(CodedOutputStreamInterface arg0, MapFieldInterface<java.lang.Boolean, V> arg1, MapEntryInterface<java.lang.Boolean, V> arg2, int arg3) throws java.io.IOException;
    LocationInterface parseFrom(java.io.InputStream arg0, ExtensionRegistryLiteInterface arg1) throws java.io.IOException;
    int getSpan(int arg0);
    <V> void serializeStringMapTo(CodedOutputStreamInterface arg0, MapFieldInterface<java.lang.String, V> arg1, MapEntryInterface<java.lang.String, V> arg2, int arg3) throws java.io.IOException;
    java.lang.String getInitializationErrorString();
//    org.apache.hadoop.thirdparty.protobuf.Parser<? extends org.apache.hadoop.thirdparty.protobuf.Message> getParserForType();
    org.apache.hadoop.thirdparty.protobuf.Internal.IntList mutableCopy(org.apache.hadoop.thirdparty.protobuf.Internal.IntList arg0);
    LocationInterface parseFrom(ByteStringInterface arg0, ExtensionRegistryLiteInterface arg1) throws org.apache.hadoop.thirdparty.protobuf.InvalidProtocolBufferException;
    LocationInterface parseDelimitedFrom(java.io.InputStream arg0) throws java.io.IOException;
    <M> M parseWithIOException(org.apache.hadoop.thirdparty.protobuf.Parser<M> arg0, CodedInputStreamInterface arg1, ExtensionRegistryLiteInterface arg2) throws java.io.IOException;
    void makeExtensionsImmutable();
    org.apache.hadoop.thirdparty.protobuf.Internal.IntList emptyIntList();
    boolean canUseUnsafe();
    org.apache.hadoop.thirdparty.protobuf.Internal.BooleanList emptyBooleanList();
//    org.apache.hadoop.thirdparty.protobuf.Message getDefaultInstanceForType();
    <V> void serializeLongMapTo(CodedOutputStreamInterface arg0, MapFieldInterface<java.lang.Long, V> arg1, MapEntryInterface<java.lang.Long, V> arg2, int arg3) throws java.io.IOException;
    org.apache.hadoop.thirdparty.protobuf.Parser<org.apache.hadoop.thirdparty.protobuf.DescriptorProtos.SourceCodeInfo.Location> parser();
    int hashBoolean(boolean arg0);
    org.apache.hadoop.thirdparty.protobuf.Internal.BooleanList mutableCopy(org.apache.hadoop.thirdparty.protobuf.Internal.BooleanList arg0);
    int hashEnumList(java.util.List<? extends org.apache.hadoop.thirdparty.protobuf.Internal.EnumLite> arg0);
    ByteStringInterface toByteString();
    int getSerializedSize();
    java.lang.Object getRepeatedField(FieldDescriptorInterface arg0, int arg1);
    org.apache.hadoop.thirdparty.protobuf.Internal.FloatList newFloatList();
    FieldDescriptorInterface getOneofFieldDescriptor(OneofDescriptorInterface arg0);
    org.apache.hadoop.thirdparty.protobuf.Internal.BooleanList newBooleanList();
    BuilderInterface newBuilder(LocationInterface arg0);
    LocationInterface parseFrom(byte[] arg0) throws org.apache.hadoop.thirdparty.protobuf.InvalidProtocolBufferException;
    <T> void addAll(java.lang.Iterable<T> arg0, java.util.List<? super T> arg1);
    org.apache.hadoop.thirdparty.protobuf.Internal.FloatList emptyFloatList();
    java.lang.String getLeadingComments();
//    BuilderInterface toBuilder();
    <M> M parseDelimitedWithIOException(org.apache.hadoop.thirdparty.protobuf.Parser<M> arg0, java.io.InputStream arg1) throws java.io.IOException;
    <M> M parseWithIOException(org.apache.hadoop.thirdparty.protobuf.Parser<M> arg0, java.io.InputStream arg1) throws java.io.IOException;
    <T> void addAll(java.lang.Iterable<T> arg0, java.util.Collection<? super T> arg1);
}
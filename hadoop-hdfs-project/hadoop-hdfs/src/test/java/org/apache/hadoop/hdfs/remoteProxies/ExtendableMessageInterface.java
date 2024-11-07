package org.apache.hadoop.hdfs.remoteProxies;

public interface ExtendableMessageInterface<MessageType> {
    org.apache.hadoop.thirdparty.protobuf.Internal.LongList emptyLongList();
    int computeStringSizeNoTag(java.lang.Object arg0);
    org.apache.hadoop.thirdparty.protobuf.Internal.LongList mutableCopy(org.apache.hadoop.thirdparty.protobuf.Internal.LongList arg0);
    void verifyExtensionContainingType(ExtensionInterface<MessageType, ?> arg0);
    org.apache.hadoop.thirdparty.protobuf.Internal.IntList emptyIntList();
    void makeExtensionsImmutable();
    void verifyContainingType(FieldDescriptorInterface arg0);
    DescriptorInterface getDescriptorForType();
    <Type> boolean hasExtension(ExtensionInterface<MessageType, Type> arg0);
    void enableAlwaysUseFieldBuildersForTesting();
    java.lang.Object getRepeatedField(FieldDescriptorInterface arg0, int arg1);
    <Type> int getExtensionCount(ExtensionInterface<MessageType, java.util.List<Type>> arg0);
    java.lang.Object writeReplace() throws java.io.ObjectStreamException;
    int extensionsSerializedSize();
    <V> void serializeLongMapTo(CodedOutputStreamInterface arg0, MapFieldInterface<java.lang.Long, V> arg1, MapEntryInterface<java.lang.Long, V> arg2, int arg3) throws java.io.IOException;
    <MessageType, T> ExtensionInterface<MessageType, T> checkNotLite(ExtensionLiteInterface<MessageType, T> arg0);
    UnknownFieldSetInterface getUnknownFields();
    void writeString(CodedOutputStreamInterface arg0, int arg1, java.lang.Object arg2) throws java.io.IOException;
    void checkByteStringIsUtf8(ByteStringInterface arg0) throws java.lang.IllegalArgumentException;
    org.apache.hadoop.thirdparty.protobuf.Internal.IntList mutableCopy(org.apache.hadoop.thirdparty.protobuf.Internal.IntList arg0);
    void writeTo(CodedOutputStreamInterface arg0) throws java.io.IOException;
    boolean hasField(FieldDescriptorInterface arg0);
    MapFieldInterface internalGetMapField(int arg0);
    org.apache.hadoop.thirdparty.protobuf.MessageLite getDefaultInstanceForType();
    <M> M parseDelimitedWithIOException(org.apache.hadoop.thirdparty.protobuf.Parser<M> arg0, java.io.InputStream arg1) throws java.io.IOException;
    org.apache.hadoop.thirdparty.protobuf.Internal.IntList newIntList();
    org.apache.hadoop.thirdparty.protobuf.Internal.BooleanList mutableCopy(org.apache.hadoop.thirdparty.protobuf.Internal.BooleanList arg0);
    ExtensionWriterInterface newExtensionWriter();
    org.apache.hadoop.thirdparty.protobuf.Message.Builder toBuilder();
    int computeStringSize(int arg0, java.lang.Object arg1);
    java.lang.String getSerializingExceptionMessage(java.lang.String arg0);
    org.apache.hadoop.thirdparty.protobuf.Parser<? extends org.apache.hadoop.thirdparty.protobuf.Message> getParserForType();
    <K, V> void serializeMapTo(CodedOutputStreamInterface arg0, java.util.Map<K, V> arg1, MapEntryInterface<K, V> arg2, int arg3) throws java.io.IOException;
    boolean compareBytes(java.lang.Object arg0, java.lang.Object arg1);
    int hashCode();
    int hashFields(int arg0, java.util.Map<org.apache.hadoop.thirdparty.protobuf.Descriptors.FieldDescriptor, java.lang.Object> arg1);
    boolean hasOneof(OneofDescriptorInterface arg0);
    <M> M parseWithIOException(org.apache.hadoop.thirdparty.protobuf.Parser<M> arg0, CodedInputStreamInterface arg1, ExtensionRegistryLiteInterface arg2) throws java.io.IOException;
    int hashEnum(org.apache.hadoop.thirdparty.protobuf.Internal.EnumLite arg0);
    <V> void serializeIntegerMapTo(CodedOutputStreamInterface arg0, MapFieldInterface<java.lang.Integer, V> arg1, MapEntryInterface<java.lang.Integer, V> arg2, int arg3) throws java.io.IOException;
    FieldAccessorTableInterface internalGetFieldAccessorTable();
    byte[] toByteArray();
//    org.apache.hadoop.thirdparty.protobuf.Message.Builder newBuilderForType(org.apache.hadoop.thirdparty.protobuf.AbstractMessage.BuilderParent arg0);
    <M> M parseWithIOException(org.apache.hadoop.thirdparty.protobuf.Parser<M> arg0, java.io.InputStream arg1, ExtensionRegistryLiteInterface arg2) throws java.io.IOException;
    <M> M parseDelimitedWithIOException(org.apache.hadoop.thirdparty.protobuf.Parser<M> arg0, java.io.InputStream arg1, ExtensionRegistryLiteInterface arg2) throws java.io.IOException;
    <Type> boolean hasExtension(GeneratedExtensionInterface<MessageType, Type> arg0);
    int hashLong(long arg0);
    org.apache.hadoop.thirdparty.protobuf.Internal.LongList newLongList();
    boolean canUseUnsafe();
    java.util.Map<org.apache.hadoop.thirdparty.protobuf.Descriptors.FieldDescriptor, java.lang.Object> getAllFieldsMutable(boolean arg0);
    org.apache.hadoop.thirdparty.protobuf.Internal.FloatList mutableCopy(org.apache.hadoop.thirdparty.protobuf.Internal.FloatList arg0);
    <M> M parseWithIOException(org.apache.hadoop.thirdparty.protobuf.Parser<M> arg0, java.io.InputStream arg1) throws java.io.IOException;
    int hashBoolean(boolean arg0);
    boolean parseUnknownField(CodedInputStreamInterface arg0, BuilderInterface arg1, ExtensionRegistryLiteInterface arg2, int arg3) throws java.io.IOException;
    void setMemoizedSerializedSize(int arg0);
//    org.apache.hadoop.thirdparty.protobuf.Parser<? extends org.apache.hadoop.thirdparty.protobuf.MessageLite> getParserForType();
    java.lang.Object getFieldRaw(FieldDescriptorInterface arg0);
    java.lang.reflect.Method getMethodOrDie(java.lang.Class arg0, java.lang.String arg1, java.lang.Class... arg2);
    org.apache.hadoop.thirdparty.protobuf.Internal.BooleanList emptyBooleanList();
    int hashMapField(java.lang.Object arg0);
    ByteStringInterface toByteString(java.lang.Object arg0);
    <Type> Type getExtension(ExtensionLiteInterface<MessageType, Type> arg0);
    <T> void addAll(java.lang.Iterable<T> arg0, java.util.List<? super T> arg1);
    <Type> Type getExtension(ExtensionInterface<MessageType, java.util.List<Type>> arg0, int arg1);
    java.lang.String toString();
    ExtensionWriterInterface newMessageSetExtensionWriter();
//    org.apache.hadoop.thirdparty.protobuf.Message getDefaultInstanceForType();
    int extensionsSerializedSizeAsMessageSet();
//    org.apache.hadoop.thirdparty.protobuf.MessageLite.Builder toBuilder();
    boolean isInitialized();
    int hashEnumList(java.util.List<? extends org.apache.hadoop.thirdparty.protobuf.Internal.EnumLite> arg0);
    <V> void serializeBooleanMapTo(CodedOutputStreamInterface arg0, MapFieldInterface<java.lang.Boolean, V> arg1, MapEntryInterface<java.lang.Boolean, V> arg2, int arg3) throws java.io.IOException;
    org.apache.hadoop.thirdparty.protobuf.Message.Builder newBuilderForType();
    boolean extensionsAreInitialized();
    java.util.Map<org.apache.hadoop.thirdparty.protobuf.Descriptors.FieldDescriptor, java.lang.Object> getExtensionFields();
    boolean parseUnknownFieldProto3(CodedInputStreamInterface arg0, BuilderInterface arg1, ExtensionRegistryLiteInterface arg2, int arg3) throws java.io.IOException;
    <Type> Type getExtension(GeneratedExtensionInterface<MessageType, Type> arg0);
    org.apache.hadoop.thirdparty.protobuf.Internal.DoubleList emptyDoubleList();
    boolean compareFields(java.util.Map<org.apache.hadoop.thirdparty.protobuf.Descriptors.FieldDescriptor, java.lang.Object> arg0, java.util.Map<org.apache.hadoop.thirdparty.protobuf.Descriptors.FieldDescriptor, java.lang.Object> arg1);
    java.util.List<java.lang.String> findInitializationErrors();
    FieldDescriptorInterface getOneofFieldDescriptor(OneofDescriptorInterface arg0);
    java.util.Map<org.apache.hadoop.thirdparty.protobuf.Descriptors.FieldDescriptor, java.lang.Object> getAllFieldsRaw();
    <V> void maybeSerializeBooleanEntryTo(CodedOutputStreamInterface arg0, java.util.Map<java.lang.Boolean, V> arg1, MapEntryInterface<java.lang.Boolean, V> arg2, int arg3, boolean arg4) throws java.io.IOException;
//    org.apache.hadoop.thirdparty.protobuf.Message.Builder newBuilderForType(org.apache.hadoop.thirdparty.protobuf.GeneratedMessageV3.BuilderParent arg0);
    <Type> Type getExtension(ExtensionLiteInterface<MessageType, java.util.List<Type>> arg0, int arg1);
    java.util.Map<org.apache.hadoop.thirdparty.protobuf.Descriptors.FieldDescriptor, java.lang.Object> getAllFields();
    boolean equals(java.lang.Object arg0);
    org.apache.hadoop.thirdparty.protobuf.Internal.FloatList emptyFloatList();
    void writeStringNoTag(CodedOutputStreamInterface arg0, java.lang.Object arg1) throws java.io.IOException;
    java.util.Map convertMapEntryListToMap(java.util.List arg0);
    <Type> int getExtensionCount(ExtensionLiteInterface<MessageType, java.util.List<Type>> arg0);
    java.lang.Object invokeOrDie(java.lang.reflect.Method arg0, java.lang.Object arg1, java.lang.Object... arg2);
//    org.apache.hadoop.thirdparty.protobuf.MessageLite.Builder newBuilderForType();
//    org.apache.hadoop.thirdparty.protobuf.Parser<? extends org.apache.hadoop.thirdparty.protobuf.GeneratedMessageV3> getParserForType();
    org.apache.hadoop.thirdparty.protobuf.Internal.BooleanList newBooleanList();
    java.lang.String getInitializationErrorString();
    int getRepeatedFieldCount(FieldDescriptorInterface arg0);
    int getMemoizedSerializedSize();
    void writeTo(java.io.OutputStream arg0) throws java.io.IOException;
    <M> M parseWithIOException(org.apache.hadoop.thirdparty.protobuf.Parser<M> arg0, CodedInputStreamInterface arg1) throws java.io.IOException;
    <Type> boolean hasExtension(ExtensionLiteInterface<MessageType, Type> arg0);
    <V> void serializeStringMapTo(CodedOutputStreamInterface arg0, MapFieldInterface<java.lang.String, V> arg1, MapEntryInterface<java.lang.String, V> arg2, int arg3) throws java.io.IOException;
    org.apache.hadoop.thirdparty.protobuf.Internal.DoubleList newDoubleList();
    <Type> Type getExtension(ExtensionInterface<MessageType, Type> arg0);
    <Type> Type getExtension(GeneratedExtensionInterface<MessageType, java.util.List<Type>> arg0, int arg1);
    ByteStringInterface toByteString();
    int getSerializedSize();
    <Type> int getExtensionCount(GeneratedExtensionInterface<MessageType, java.util.List<Type>> arg0);
    org.apache.hadoop.thirdparty.protobuf.Internal.FloatList newFloatList();
    boolean compareMapField(java.lang.Object arg0, java.lang.Object arg1);
    java.lang.Object getField(FieldDescriptorInterface arg0);
    org.apache.hadoop.thirdparty.protobuf.Internal.DoubleList mutableCopy(org.apache.hadoop.thirdparty.protobuf.Internal.DoubleList arg0);
    UninitializedMessageExceptionInterface newUninitializedMessageException();
    <T> void addAll(java.lang.Iterable<T> arg0, java.util.Collection<? super T> arg1);
    void writeDelimitedTo(java.io.OutputStream arg0) throws java.io.IOException;
}
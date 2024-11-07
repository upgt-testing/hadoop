package org.apache.hadoop.hdfs.remoteProxies;

public interface ByteStringInterface {
    java.util.List<java.nio.ByteBuffer> asReadOnlyByteBufferList();
    CodedInputStreamInterface newCodedInput();
    boolean isBalanced();
    ByteStringInterface copyFrom(byte[] arg0, int arg1, int arg2);
    ByteStringInterface copyFrom(java.nio.ByteBuffer arg0);
    int partialHash(int arg0, int arg1, int arg2);
    ByteStringInterface balancedConcat(java.util.Iterator<org.apache.hadoop.thirdparty.protobuf.ByteString> arg0, int arg1);
    java.nio.ByteBuffer asReadOnlyByteBuffer();
    int hashCode();
    int toInt(byte arg0);
    void writeTo(ByteOutputInterface arg0) throws java.io.IOException;
    OutputInterface newOutput();
    java.lang.String toStringInternal(java.nio.charset.Charset arg0);
    ByteStringInterface readFrom(java.io.InputStream arg0, int arg1, int arg2) throws java.io.IOException;
    ByteStringInterface readChunk(java.io.InputStream arg0, int arg1) throws java.io.IOException;
//    java.util.Spliterator<T> spliterator();
    void copyTo(byte[] arg0, int arg1, int arg2, int arg3);
    java.lang.String toString(java.lang.String arg0) throws java.io.UnsupportedEncodingException;
    byte[] toByteArray();
    ByteStringInterface copyFromUtf8(java.lang.String arg0);
    ByteStringInterface copyFrom(byte[] arg0);
    boolean isValidUtf8();
    void copyTo(java.nio.ByteBuffer arg0);
    java.io.InputStream newInput();
    ByteStringInterface wrap(byte[] arg0, int arg1, int arg2);
    void writeTo(java.io.OutputStream arg0) throws java.io.IOException;
    ByteStringInterface copyFrom(java.lang.String arg0, java.nio.charset.Charset arg1);
    boolean equals(java.lang.Object arg0);
    boolean endsWith(ByteStringInterface arg0);
    ByteStringInterface copyFrom(java.lang.String arg0, java.lang.String arg1) throws java.io.UnsupportedEncodingException;
    int checkRange(int arg0, int arg1, int arg2);
    boolean isEmpty();
    ByteStringInterface copyFrom(java.lang.Iterable<org.apache.hadoop.thirdparty.protobuf.ByteString> arg0);
    java.lang.String toString(java.nio.charset.Charset arg0);
    byte internalByteAt(int arg0);
    ByteStringInterface concat(ByteStringInterface arg0);
    boolean startsWith(ByteStringInterface arg0);
    java.lang.String toString();
    ByteStringInterface wrap(byte[] arg0);
    ByteStringInterface readFrom(java.io.InputStream arg0, int arg1) throws java.io.IOException;
    void writeToInternal(java.io.OutputStream arg0, int arg1, int arg2) throws java.io.IOException;
    int partialIsValidUtf8(int arg0, int arg1, int arg2);
    org.apache.hadoop.thirdparty.protobuf.ByteString.ByteIterator iterator();
    java.util.Comparator<org.apache.hadoop.thirdparty.protobuf.ByteString> unsignedLexicographicalComparator();
    void writeTo(java.io.OutputStream arg0, int arg1, int arg2) throws java.io.IOException;
    CodedBuilderInterface newCodedBuilder(int arg0);
    int size();
    ByteStringInterface substring(int arg0);
//    java.util.Iterator<T> iterator();
    int getTreeDepth();
    java.lang.String toStringUtf8();
    void checkIndex(int arg0, int arg1);
    int peekCachedHashCode();
    ByteStringInterface substring(int arg0, int arg1);
    ByteStringInterface copyFrom(java.nio.ByteBuffer arg0, int arg1);
    byte byteAt(int arg0);
    void copyToInternal(byte[] arg0, int arg1, int arg2, int arg3);
    ByteStringInterface readFrom(java.io.InputStream arg0) throws java.io.IOException;
    void copyTo(byte[] arg0, int arg1);
//    void forEach(java.util.function.Consumer<? super T> arg0);
    ByteStringInterface wrap(java.nio.ByteBuffer arg0);
    OutputInterface newOutput(int arg0);
}
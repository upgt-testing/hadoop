package org.apache.hadoop.hdfs.remoteProxies;

public interface CodedBuilderInterface {
    ByteStringInterface build();
    CodedOutputStreamInterface getCodedOutput();
}
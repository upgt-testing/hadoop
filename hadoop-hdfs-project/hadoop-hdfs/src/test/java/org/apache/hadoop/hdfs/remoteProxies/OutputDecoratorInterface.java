package org.apache.hadoop.hdfs.remoteProxies;

public interface OutputDecoratorInterface {
    java.io.Writer decorate(IOContextInterface arg0, java.io.Writer arg1) throws java.io.IOException;
    java.io.OutputStream decorate(IOContextInterface arg0, java.io.OutputStream arg1) throws java.io.IOException;
}
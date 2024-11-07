package org.apache.hadoop.hdfs.remoteProxies;

public interface InputDecoratorInterface {
    java.io.DataInput decorate(IOContextInterface arg0, java.io.DataInput arg1) throws java.io.IOException;
    java.io.Reader decorate(IOContextInterface arg0, java.io.Reader arg1) throws java.io.IOException;
    java.io.InputStream decorate(IOContextInterface arg0, java.io.InputStream arg1) throws java.io.IOException;
    java.io.InputStream decorate(IOContextInterface arg0, byte[] arg1, int arg2, int arg3) throws java.io.IOException;
}
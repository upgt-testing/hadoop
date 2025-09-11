package org.apache.hadoop.fs;

public interface HasFileDescriptorJVMInterface {

    java.io.FileDescriptor getFileDescriptor() throws java.io.IOException;
}

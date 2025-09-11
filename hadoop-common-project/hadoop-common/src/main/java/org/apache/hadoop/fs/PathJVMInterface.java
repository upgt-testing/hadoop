package org.apache.hadoop.fs;

public interface PathJVMInterface {

    int hashCode();

    org.apache.hadoop.fs.PathJVMInterface getParent();

    int compareTo(java.lang.Object arg0);

    boolean equals(java.lang.Object arg0);

    java.lang.String toString();

    java.net.URI toUri();

    org.apache.hadoop.fs.PathJVMInterface suffix(java.lang.String arg0);

    boolean isAbsoluteAndSchemeAuthorityNull();

    org.apache.hadoop.fs.PathJVMInterface makeQualified_bridge(java.net.URI arg0, org.apache.hadoop.fs.PathJVMInterface arg1);

    boolean isUriPathAbsolute();

    void validateObject() throws java.io.InvalidObjectException;

    boolean isRoot();

    int depth();

    java.lang.String getName();

    org.apache.hadoop.fs.FileSystemJVMInterface getFileSystem_bridge(org.apache.hadoop.conf.ConfigurationJVMInterface arg0) throws java.io.IOException;

    boolean isAbsolute();

    org.apache.hadoop.fs.PathJVMInterface makeQualified_bridge(org.apache.hadoop.fs.FileSystemJVMInterface arg0);
}

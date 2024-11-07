package org.apache.hadoop.hdfs.remoteProxies;

public interface PathInterface {
    void checkPathArg(java.lang.String arg0) throws java.lang.IllegalArgumentException;
    java.lang.String toString();
    PathInterface getPathWithoutSchemeAndAuthority(PathInterface arg0);
    PathInterface suffix(java.lang.String arg0);
    boolean isAbsoluteAndSchemeAuthorityNull();
    boolean isAbsolute();
    PathInterface makeQualified(FileSystemInterface arg0);
    java.net.URI toUri();
    java.lang.String normalizePath(java.lang.String arg0, java.lang.String arg1);
    java.lang.String getName();
    void initialize(java.lang.String arg0, java.lang.String arg1, java.lang.String arg2, java.lang.String arg3);
    int compareTo(PathInterface arg0);
    void validateObject() throws java.io.InvalidObjectException;
    boolean isUriPathAbsolute();
    PathInterface getParent();
    PathInterface makeQualified(java.net.URI arg0, PathInterface arg1);
    boolean isWindowsAbsolutePath(java.lang.String arg0, boolean arg1);
    boolean equals(java.lang.Object arg0);
    boolean isRoot();
    void checkNotSchemeWithRelative();
    int depth();
    int hashCode();
    int startPositionWithoutWindowsDrive(java.lang.String arg0);
    FileSystemInterface getFileSystem(ConfigurationInterface arg0) throws java.io.IOException;
    PathInterface mergePaths(PathInterface arg0, PathInterface arg1);
    void checkNotRelative();
    boolean hasWindowsDrive(java.lang.String arg0);
}
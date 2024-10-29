package org.apache.hadoop.hdfs.remoteProxies;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import java.net.URI;
import java.util.*;
import java.io.*;

public interface PathInterface {

    URI toUri();

    FileSystemInterface getFileSystem(Configuration conf);

    boolean isAbsoluteAndSchemeAuthorityNull();

    boolean isUriPathAbsolute();

    boolean isAbsolute();

    boolean isRoot();

    String getName();

    PathInterface getParent();

    PathInterface suffix(String suffix);

    String toString();

    boolean equals(Object o);

    int hashCode();

    int compareTo(Path o);

    int depth();

    PathInterface makeQualified(FileSystem fs);

    PathInterface makeQualified(URI defaultUri, Path workingDir);

    void validateObject();
}

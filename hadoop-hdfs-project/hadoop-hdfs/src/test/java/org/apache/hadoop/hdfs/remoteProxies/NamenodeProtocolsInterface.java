package org.apache.hadoop.hdfs.remoteProxies;

import org.apache.hadoop.fs.permission.FsPermission;

import java.util.*;
import java.io.*;

public interface NamenodeProtocolsInterface {
    boolean mkdirs(String src, FsPermission masked, boolean createParent)
            throws IOException;
}

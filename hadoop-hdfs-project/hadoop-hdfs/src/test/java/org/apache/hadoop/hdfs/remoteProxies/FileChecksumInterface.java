package org.apache.hadoop.hdfs.remoteProxies;

import org.apache.hadoop.fs.Options;

import java.util.*;
import java.io.*;

public interface FileChecksumInterface {

    String getAlgorithmName();

    int getLength();

    byte[] getBytes();

    Options.ChecksumOpt getChecksumOpt();

    boolean equals(Object other);

    int hashCode();
}

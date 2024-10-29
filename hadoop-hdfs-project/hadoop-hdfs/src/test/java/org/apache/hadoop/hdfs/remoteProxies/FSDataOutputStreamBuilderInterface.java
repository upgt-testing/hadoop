package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.util.Progressable;

public interface FSDataOutputStreamBuilderInterface {
    /**

    B getThisBuilder();

    B permission(FsPermission perm);

    B bufferSize(int bufSize);

    B replication(short replica);

    B blockSize(long blkSize);

    B recursive();

    B progress(Progressable prog);

    B create();

    B overwrite(boolean overwrite);

    B append();

    B checksumOpt(ChecksumOpt chksumOpt);

    S build();
     **/
}

package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;

public interface ExportedBlockKeysInterface {

    boolean isBlockTokenEnabled();

    long getKeyUpdateInterval();

    long getTokenLifetime();

    BlockKeyInterface getCurrentKey();

    //BlockKey[] getAllKeys();

    void write(DataOutput out);

    void readFields(DataInput in);
}

package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;

public interface BlockLocalPathInfoInterface {

    String getBlockPath();

    ExtendedBlockInterface getBlock();

    String getMetaPath();

    long getNumBytes();
}

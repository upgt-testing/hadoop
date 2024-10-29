package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;
import org.apache.hadoop.util.Options;

public interface BlockAliasMapInterface {

    //Reader<T> getReader(OptionsInterface opts, String blockPoolID);

    //Writer<T> getWriter(Writer.Options opts, String blockPoolID);

    void refresh();

    void close();
}

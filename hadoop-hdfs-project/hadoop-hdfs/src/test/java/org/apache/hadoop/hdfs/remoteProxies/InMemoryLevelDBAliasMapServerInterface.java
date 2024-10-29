package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hdfs.protocol.Block;
import org.apache.hadoop.hdfs.protocol.ProvidedStorageLocation;
import org.apache.hadoop.hdfs.server.aliasmap.InMemoryAliasMap;

public interface InMemoryLevelDBAliasMapServerInterface {

    void start();

    InMemoryAliasMap.IterationResult list(Optional<Block> marker);

    Optional<ProvidedStorageLocation> read(Block block);

    void write(Block block, ProvidedStorageLocationInterface providedStorageLocation);

    String getBlockPoolId();

    void setConf(Configuration conf);

    Configuration getConf();

    InMemoryAliasMapInterface getAliasMap();

    void close();
}

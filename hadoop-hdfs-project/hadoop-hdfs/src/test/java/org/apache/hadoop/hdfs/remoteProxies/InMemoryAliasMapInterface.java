package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hdfs.protocol.Block;
import org.apache.hadoop.hdfs.protocol.ProvidedStorageLocation;
import org.apache.hadoop.hdfs.server.aliasmap.InMemoryAliasMapProtocol;

public interface InMemoryAliasMapInterface {

    void setConf(Configuration conf);

    Configuration getConf();

    InMemoryAliasMapProtocol.IterationResult list(Optional<Block> marker);

    Optional<ProvidedStorageLocation> read(Block block);

    void write(Block block, ProvidedStorageLocation providedStorageLocation);

    String getBlockPoolId();

    void close();
}

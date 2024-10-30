package org.apache.hadoop.hdfs.remoteProxies;

import org.apache.hadoop.conf.Configuration;

import java.net.InetAddress;
import java.util.*;
import java.io.*;

public interface TrustedChannelResolverInterface {

    void setConf(Configuration conf);

    Configuration getConf();

    boolean isTrusted();

    boolean isTrusted(InetAddress peerAddress);
}

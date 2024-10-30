package org.apache.hadoop.hdfs.remoteProxies;

import org.apache.hadoop.conf.Configuration;

import java.net.InetAddress;
import java.util.*;
import java.io.*;

public interface SaslPropertiesResolverInterface {

    void setConf(Configuration conf);

    Configuration getConf();

    Map<String, String> getDefaultProperties();

    Map<String, String> getServerProperties(InetAddress clientAddress);

    Map<String, String> getServerProperties(InetAddress clientAddress, int ingressPort);

    Map<String, String> getClientProperties(InetAddress serverAddress);

    Map<String, String> getClientProperties(InetAddress serverAddress, int ingressPort);
}

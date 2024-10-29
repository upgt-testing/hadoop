package org.apache.hadoop.hdfs.remoteProxies;

import java.net.InetSocketAddress;
import java.util.*;
import java.io.*;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hdfs.server.common.HdfsServerConstants;
import org.apache.hadoop.security.token.delegation.web.DelegationTokenIdentifier;

public interface NameNodeInterface {

    long getProtocolVersion(String protocol, long clientVersion);

    FSNamesystemInterface getNamesystem();

    NamenodeProtocolsInterface getRpcServer();

    HttpServer2Interface getHttpServer();

    void queueExternalCall(ExternalCallInterface extCall);

    String getTokenServiceName();

    String getClientNamenodeAddress();

    HdfsServerConstants.NamenodeRole getRole();

    void verifyToken(DelegationTokenIdentifier id, byte[] password);

    InMemoryLevelDBAliasMapServerInterface getAliasMapServer();

    void join();

    void stop();

    boolean isInSafeMode();

    FSImageInterface getFSImage();

    InetSocketAddress getNameNodeAddress();

    Set<InetSocketAddress> getAuxiliaryNameNodeAddresses();

    String getNameNodeAddressHostPortString();

    String getNNAuxiliaryRpcAddress();

    InetSocketAddress getServiceRpcAddress();

    InetSocketAddress getHttpAddress();

    InetSocketAddress getHttpsAddress();

    void joinHttpServer();

    boolean testRMIPrint(String message);

    void testRMIConf(Configuration conf);

    String getNNRole();

    String getState();

    String getHostAndPort();

    boolean isSecurityEnabled();

    long getLastHATransitionTime();

    long getBytesWithFutureGenerationStamps();

    String getSlowPeersReport();

    String getSlowDisksReport();

    boolean isStandbyState();

    boolean isActiveState();

    boolean isObserverState();

    Collection<String> getReconfigurableProperties();
}

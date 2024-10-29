package org.apache.hadoop.hdfs.remoteProxies;

import org.apache.hadoop.hdfs.protocol.DatanodeID;
import org.apache.hadoop.thirdparty.protobuf.ByteString;

import java.util.*;
import java.io.*;

public interface DatanodeIDInterface {

    void setIpAddr(String ipAddr);

    void setPeerHostName(String peerHostName);

    String getDatanodeUuid();

    ByteString getDatanodeUuidBytes();

    String getIpAddr();

    ByteString getIpAddrBytes();

    String getHostName();

    ByteString getHostNameBytes();

    String getPeerHostName();

    String getXferAddr();

    String getInfoAddr();

    String getInfoSecureAddr();

    String getXferAddrWithHostname();

    String getXferAddr(boolean useHostname);

    String getIpcAddr(boolean useHostname);

    int getXferPort();

    int getInfoPort();

    int getInfoSecurePort();

    int getIpcPort();

    boolean equals(Object to);

    int hashCode();

    String toString();

    void updateRegInfo(DatanodeID nodeReg);

    int compareTo(DatanodeID that);

    //InetSocketAddress getResolvedAddress();
}

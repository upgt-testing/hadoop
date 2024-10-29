package org.apache.hadoop.hdfs.remoteProxies;

import java.nio.ByteBuffer;
import java.util.*;
import java.io.*;

public interface TokenInterface {

    ByteBuffer getIdentifier();

    void setIdentifier(ByteBuffer identifier);

    ByteBuffer getPassword();

    void setPassword(ByteBuffer password);

    String getKind();

    void setKind(String kind);

    String getService();

    void setService(String service);
}

package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.security.UserGroupInformation;

public interface TokenIdentifierInterface {

    Text getKind();

    UserGroupInformation getUser();

    byte[] getBytes();

    String getTrackingId();
}

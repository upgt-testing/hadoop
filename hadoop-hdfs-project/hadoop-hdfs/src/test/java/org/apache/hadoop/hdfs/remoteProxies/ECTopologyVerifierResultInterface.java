package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;

public interface ECTopologyVerifierResultInterface {

    String getResultMessage();

    boolean isSupported();
}

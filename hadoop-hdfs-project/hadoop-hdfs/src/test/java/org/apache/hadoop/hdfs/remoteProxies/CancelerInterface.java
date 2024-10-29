package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;

public interface CancelerInterface {

    void cancel(String reason);

    boolean isCancelled();

    String getCancellationReason();
}

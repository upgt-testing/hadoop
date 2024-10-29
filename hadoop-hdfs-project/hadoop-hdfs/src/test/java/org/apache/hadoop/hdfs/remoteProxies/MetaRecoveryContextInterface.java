package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;

public interface MetaRecoveryContextInterface {

    String ask(String prompt, String firstChoice, String choices);

    void quit();

    int getForce();

    void setForce(int force);
}

package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;

public interface ResourceInterface {

    ResourceInterface profile(String profile);

    String getProfile();

    void setProfile(String profile);

    ResourceInterface cpus(Integer cpus);

    Integer getCpus();

    void setCpus(Integer cpus);

    ResourceInterface memory(String memory);

    String getMemory();

    void setMemory(String memory);

    long calcMemoryMB();

    //ResourceInterface setResourceInformations(Map<String, ResourceInformation> resourceInformations);

    //ResourceInterface resourceInformations(Map<String, ResourceInformation> resourceInformations);

    //Map<String, ResourceInformation> getAdditional();

    boolean equals(java.lang.Object o);

    int hashCode();

    String toString();

    Object clone();
}

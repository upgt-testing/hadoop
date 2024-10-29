package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;

public interface ArtifactInterface {

    //Artifact id(String id);

    String getId();

    void setId(String id);

    //Artifact type(TypeEnum type);

    //TypeEnum getType();

    //void setType(TypeEnum type);

    //Artifact uri(String uri);

    String getUri();

    void setUri(String uri);

    boolean equals(java.lang.Object o);

    int hashCode();

    String toString();
}

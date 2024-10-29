package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;

public interface XAttrFeatureInterface {

    List<XAttrInterface> getXAttrs();

    boolean equals(Object o);

    int hashCode();

    XAttrInterface getXAttr(String prefixedName);
}

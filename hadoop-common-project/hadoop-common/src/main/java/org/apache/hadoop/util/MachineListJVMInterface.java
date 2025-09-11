package org.apache.hadoop.util;

public interface MachineListJVMInterface {

    boolean includes(java.net.InetAddress arg0);

    java.util.Collection getCollection();

    boolean includes(java.lang.String arg0);
}

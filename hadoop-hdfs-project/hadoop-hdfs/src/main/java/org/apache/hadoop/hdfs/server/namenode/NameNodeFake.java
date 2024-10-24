package org.apache.hadoop.hdfs.server.namenode;

public class NameNodeFake {
    public NameNodeFake() {
        System.out.println("NameNodeFake created");
    }

    public boolean testRMIPrint(String message) {
        System.out.println("RMI print: " + message);
        return true;
    }


}

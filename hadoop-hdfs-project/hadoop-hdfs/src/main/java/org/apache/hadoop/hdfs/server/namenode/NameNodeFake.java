package org.apache.hadoop.hdfs.server.namenode;

public class NameNodeFake {
    private int ID;

    public NameNodeFake() {
        this.ID = 0;
        System.out.println("NameNodeFake created with ID: " + ID);
    }

    public NameNodeFake(int ID) {
        this.ID = ID;
        System.out.println("NameNodeFake created with ID: " + ID);
    }

    public boolean testRMIPrint(String message) {
        System.out.println("RMI print: " + message);
        return true;
    }

    public int getID() {
        return ID;
    }
}

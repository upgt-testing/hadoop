package org.apache.hadoop.hdfs.rmi;

import java.util.UUID;

public class MyUUID implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    public final UUID uuid;

    public MyUUID(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID get() {
        return uuid;
    }
}

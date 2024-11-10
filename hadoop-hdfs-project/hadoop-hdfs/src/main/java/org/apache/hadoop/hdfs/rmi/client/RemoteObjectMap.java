package org.apache.hadoop.hdfs.rmi.client;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RemoteObjectMap {
    private static final Map<UUID, Object> rmiObjectMap = new HashMap<>();

    public static <T> T get(UUID id, Class<T> clazz) {
        return (T) rmiObjectMap.get(id);
    }

    public static void put(UUID id, Object obj) {
        rmiObjectMap.put(id, obj);
    }

    public static void remove(UUID id) {
        rmiObjectMap.remove(id);
    }

    public static boolean containsRmiObject(UUID id) {
        return rmiObjectMap.containsKey(id);
    }
}

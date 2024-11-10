package org.apache.hadoop.hdfs.rmi.server;

import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ActualObjectMap {
    private final static Logger LOG = org.slf4j.LoggerFactory.getLogger(ActualObjectMap.class);

    // The object can be "object" or "RemoteObjectImpl"
    private static final Map<UUID, RemoteObjectImpl> idToRemoteObjImplMap = new HashMap<>();

    public static void put(UUID uuid, RemoteObjectImpl obj) {
        if (obj == null) {
            LOG.info("Skip putting null object into the ActualObjectMap");
            return;
        }
        idToRemoteObjImplMap.put(uuid, obj);
    }

    private static Object getActualObjectFromUUID(UUID uuid) {
        return idToRemoteObjImplMap.get(uuid).getActualObject();
    }

    public static Object getActualObject(UUID uuid) {
        if (!idToRemoteObjImplMap.containsKey(uuid)) {
            LOG.info("Object with UUID {} is not found in the map", uuid);
            return null;
        }
        return getActualObjectFromUUID(uuid);
    }

    public static Object getRemoteObject(UUID uuid) {
        return idToRemoteObjImplMap.get(uuid);
    }

    public static boolean containsActualObject(Object actualObj) throws InterruptedException {
        LOG.info("Checking if the actual object is in the map");
        LOG.info("Actual object: {}", actualObj);
        LOG.info("Map: {}", idToRemoteObjImplMap);
        //Thread.sleep(30000);
        for (RemoteObjectImpl value : idToRemoteObjImplMap.values()) {
            if (value.getActualObject().equals(actualObj)) {
                return true;
            }
        }
        return false;
    }

    public static Object getRemoteObjectFromActualObject(Object actualObj) {
        for (RemoteObjectImpl value : idToRemoteObjImplMap.values()) {
            if (value.getActualObject().equals(actualObj)) {
                return value;
            }
        }
        return null;
    }
}

package org.apache.hadoop.hdfs.rmi;

import org.apache.hadoop.hdfs.rmi.server.RemoteObject;
import org.slf4j.Logger;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class RmiUtils {

    private final static Logger LOG = org.slf4j.LoggerFactory.getLogger(RmiUtils.class);
    /**
     * Check if a registry is already running on the given port
     * @param port the port number
     * @return true if a registry is already running on the given port, false otherwise
     */
    public static boolean isRegistryExist(int port) {
        Registry registry = null;
        try {
            registry = LocateRegistry.getRegistry(port);
            registry.list();
            return true;
        } catch (RemoteException e) {
            LOG.info("Registry does not exist on port {}", port);
            return false;
        }
    }

    /**
     * Create a registry on the given port
     * @param port the port number
     */
    public static void createRegistry(int port) {
        try {
            if (isRegistryExist(port)) {
                return;
            }
            LocateRegistry.createRegistry(port);
        } catch (RemoteException e) {
            LOG.error("Failed to create registry", e);
            throw new RuntimeException("Failed to create registry", e);
        }
    }

    /**
     * Register an object with the given name in the registry
     * @param name the name of the object
     * @param obj the object to be registered
     * @return true if the object is successfully registered, false otherwise
     */
    public static boolean registerRmiObject(String name, RemoteObject obj, int port) {
        try {
            System.out.println(InetAddress.getLocalHost().getHostAddress());
            LOG.info("Local host address: {}", InetAddress.getLocalHost().getHostAddress());
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        if (!isRegistryExist(port)) {
            LOG.warn("Registry does not exist on port {}, creating one", port);
            createRegistry(port);
        }
        try {
            Registry registry = LocateRegistry.getRegistry("localhost", port);
            registry.rebind(name, obj);
            LOG.info("Object with name {} is registered in registry with port {}", name, port);
            return true;
        } catch (RemoteException e) {
            LOG.error("Failed to register object with name {} in registry", name, e);
            return false;
        }
    }
}

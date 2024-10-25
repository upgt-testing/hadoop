package org.apache.hadoop.hdfs.rmi;

import org.apache.hadoop.hdfs.rmi.server.RemoteObject;
import org.apache.hadoop.hdfs.rmi.server.RemoteObjectImpl;
import org.slf4j.Logger;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class RmiUtils {
    private final static String RMI_CONNECTION_PORT_PROPERTY_KEY = "rmi.connection.port";
    private final static String RMI_OBJECT_PORT_PROPERTY_KEY = "rmi.object.port";

    private final static Logger LOG = org.slf4j.LoggerFactory.getLogger(RmiUtils.class);

    private static int rmiConnectionPort = -1;
    private static int rmiObjectPort = -1;

    public static int getRmiConnectionPort() {
        if (rmiConnectionPort == -1) {
            rmiConnectionPort = Integer.parseInt(System.getProperty(RMI_CONNECTION_PORT_PROPERTY_KEY));
            LOG.info("RMI connection port: {}", rmiConnectionPort);
        }
        return rmiConnectionPort;
    }

    public static int getRmiObjectPort() {
        if (rmiObjectPort == -1) {
            rmiObjectPort = Integer.parseInt(System.getProperty(RMI_OBJECT_PORT_PROPERTY_KEY));
            LOG.info("RMI object port: {}", rmiObjectPort);
        }
        return rmiObjectPort;
    }

    /**
     * Check if a registry is already running on the given port
     * @return true if a registry is already running on the given port, false otherwise
     */
    public static boolean isRegistryExist() {
        Registry registry = null;
        try {
            registry = LocateRegistry.getRegistry(getRmiConnectionPort());
            registry.list();
            return true;
        } catch (RemoteException e) {
            LOG.info("Registry does not exist on port {}", getRmiConnectionPort());
            return false;
        }
    }

    /**
     * Create a registry on the given port
     */
    public static void createRegistry() {
        try {
            if (isRegistryExist()) {
                return;
            }
            LocateRegistry.createRegistry(getRmiConnectionPort());
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
        if (!isRegistryExist()) {
            LOG.warn("Registry does not exist on port {}, creating one", getRmiConnectionPort());
            createRegistry();
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

    public static void registerCurrentRmiObject(String className, Object actualObj) {
        try {
            System.setProperty("java.rmi.server.hostname", "localhost");
            // Shuai: Register RMI
            RemoteObject nnRemote = new RemoteObjectImpl(actualObj, RmiUtils.getRmiObjectPort());
            RmiUtils.registerRmiObject(className, nnRemote, RmiUtils.getRmiConnectionPort());
        } catch (java.rmi.RemoteException e) {
            throw new RuntimeException("Failed to register RMI object", e);
        }
    }
}

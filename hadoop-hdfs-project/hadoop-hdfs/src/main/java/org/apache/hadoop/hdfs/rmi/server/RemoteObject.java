package org.apache.hadoop.hdfs.rmi.server;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.UUID;

public interface RemoteObject extends Remote {
    Object invoke(String methodName, Class<?>[] paramTypes, Object[] args) throws RemoteException;
    Object getActualObject() throws RemoteException;
    UUID getObjectUniqueID() throws RemoteException;
}

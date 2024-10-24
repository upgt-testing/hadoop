package org.apache.hadoop.hdfs.rmi.server;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteObject extends Remote {
    Object invoke(String methodName, Class<?>[] paramTypes, Object[] args) throws RemoteException;
}

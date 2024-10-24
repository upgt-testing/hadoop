package org.apache.hadoop.hdfs.rmi.client;

import org.apache.hadoop.hdfs.rmi.server.RemoteObject;
import java.lang.reflect.Proxy;

public class DynamicProxyFactory {
    public static Object createProxy(RemoteObject remoteObject, Class<?> interfaceClass) {
        return Proxy.newProxyInstance(
                interfaceClass.getClassLoader(),
                new Class<?>[]{interfaceClass},
                new RemoteObjectProxy(remoteObject)
        );
    }
}


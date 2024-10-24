package org.apache.hadoop.hdfs.rmi.client;


import org.apache.hadoop.hdfs.rmi.server.RemoteObject;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.rmi.RemoteException;

public class RemoteObjectProxy implements InvocationHandler {
    private RemoteObject remoteObject;

    public RemoteObjectProxy(RemoteObject remoteObject) {
        this.remoteObject = remoteObject;
    }

    @SuppressWarnings("unchecked")
    public static <T> T newInstance(RemoteObject remoteObject, Class<T> clazz) {
        return (T) Proxy.newProxyInstance(
                clazz.getClassLoader(),
                new Class<?>[]{clazz},
                new RemoteObjectProxy(remoteObject)
        );
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        try {
            // Get parameter types
            Class<?>[] paramTypes = method.getParameterTypes();

            // Invoke the method on the remote object
            Object result = remoteObject.invoke(method.getName(), paramTypes, args);

            // If the result is a RemoteObject, create a new proxy for it
            if (result instanceof RemoteObject) {
                Class<?> returnType = method.getReturnType();
                return newInstance((RemoteObject) result, returnType);
            } else {
                return result;
            }
        } catch (RemoteException e) {
            throw e.getCause();
        }
    }
}

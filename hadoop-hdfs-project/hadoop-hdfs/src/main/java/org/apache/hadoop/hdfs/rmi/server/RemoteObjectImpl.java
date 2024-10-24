package org.apache.hadoop.hdfs.rmi.server;


import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class RemoteObjectImpl extends UnicastRemoteObject implements RemoteObject {
    private Object actualObject;

    public RemoteObjectImpl(Object actualObject) throws RemoteException {
        super(1100);
        this.actualObject = actualObject;
    }

    @Override
    public Object invoke(String methodName, Class<?>[] paramTypes, Object[] args) throws RemoteException {
        try {
            // Find the method
            Method method = actualObject.getClass().getMethod(methodName, paramTypes);

            // Invoke the method
            Object result = method.invoke(actualObject, args);

            // If the result is a complex object, wrap it in a RemoteObjectImpl
            if (result != null && !isPrimitiveOrWrapper(result.getClass())) {
                return new RemoteObjectImpl(result);
            } else {
                return result;
            }
        } catch (Exception e) {
            throw new RemoteException("Invocation error", e);
        }
    }

    private boolean isPrimitiveOrWrapper(Class<?> type) {
        return type.isPrimitive() || type == String.class
                || type == Integer.class || type == Boolean.class
                || type == Long.class || type == Double.class
                || type == Float.class || type == Character.class
                || type == Byte.class || type == Short.class;
    }
}


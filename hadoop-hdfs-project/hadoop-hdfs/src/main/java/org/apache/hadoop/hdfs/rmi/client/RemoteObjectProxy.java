package org.apache.hadoop.hdfs.rmi.client;


import org.apache.hadoop.hdfs.rmi.MyUUID;
import org.apache.hadoop.hdfs.rmi.server.RemoteObject;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.rmi.RemoteException;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public class RemoteObjectProxy implements InvocationHandler {
    private static final String REMOTE_PROXIES_PACKAGE = "org.apache.hadoop.hdfs.remoteProxies";
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

    /**
     * The invoke method may contains RMI interfaces, the override invoke method will replace the RMI interfaces with UUID
     * The remote part will get the actual class and object from the UUID
     * For example, given method signature: public void setConf(ConfigurationInterface conf)
     * The method signature will be changed to: public void setConf(MyUUID uuid) and the remote part will get the actual class and object from the UUID
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        try {
            // Get parameter types
            Class<?>[] paramTypes = method.getParameterTypes();
            for (int i = 0; i < paramTypes.length; i++) {
                Class<?> paramType = paramTypes[i];
                // step1 : check whether paramTypes have any class that is (1) from remoteProxies package and (2) name ends with "Interface"
                if (paramType.getName().contains(REMOTE_PROXIES_PACKAGE) && paramType.getName().endsWith("Interface")) {
                    // step2: if so we change the param type to UUID and rely the remote part to get the actual class and object
                    paramTypes[i] = MyUUID.class;
                    UUID uuid = ((RemoteObjectProxy) Proxy.getInvocationHandler(args[i])).getObjectUniqueID();
                    args[i] = new MyUUID(uuid);
                }
            }

            Object result = remoteObject.invoke(method.getName(), paramTypes, args);

            /*
            boolean isTarget = false;
            // Invoke the method on the remote object
            if (method.getName().contains("setConf")) {
                isTarget = true;
            }
            Object result = null;
            if (isTarget) {
                //result = remoteObject.invoke(method.getName(), paramTypes, new Object[]{});
                UUID resultObjectID = ((RemoteObjectProxy)Proxy.getInvocationHandler(args[0])).getObjectUniqueID();
                args = new Object[]{null, resultObjectID};
                result = remoteObject.invoke(method.getName(), new Class<?>[]{Configuration.class, UUID.class}, args);
            } else {
                result = remoteObject.invoke(method.getName(), newParamTypes, args);
            } */

            // If the result is a RemoteObject, create a new proxy for it
            if (result instanceof RemoteObject) {
                UUID resultObjectID = ((RemoteObject) result).getObjectUniqueID();
                Class<?> returnType = method.getReturnType();
                System.out.println("Get UUID: " + resultObjectID);
                System.out.println("Return type: " + returnType.getName());
                if (RemoteObjectMap.containsRmiObject(resultObjectID)) {
                    return RemoteObjectMap.get(resultObjectID, returnType);
                }
                return newInstance((RemoteObject) result, returnType);
            } else {
                return result;
            }
        } catch (RemoteException e) {
            throw e.getCause();
        }
    }

    public RemoteObject getRemoteObject() {
        return remoteObject;
    }

    public UUID getObjectUniqueID() {
        try {
            return remoteObject.getObjectUniqueID();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }
}

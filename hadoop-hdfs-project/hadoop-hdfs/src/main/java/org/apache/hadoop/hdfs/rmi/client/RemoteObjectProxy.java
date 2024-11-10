package org.apache.hadoop.hdfs.rmi.client;


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

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        try {
            // Get parameter types
            Class<?>[] paramTypes = method.getParameterTypes();
            Class<?>[] newParamTypes = new Class<?>[paramTypes.length];
            List<Integer> remoteProxyParamIndexes = new LinkedList<>();
            for (int i = 0; i < paramTypes.length; i++) {
                Class<?> paramType = paramTypes[i];
                newParamTypes[i] = paramType;
                // step1 : check whether paramTypes have any class that is (1) from remoteProxies package and (2) name ends with "Interface"
                if (paramType.getName().contains(REMOTE_PROXIES_PACKAGE) && paramType.getName().endsWith("Interface")) {
                    // step2: if step1 is true, then change the paramTypes to the actual class, we can do it through the "clz" field in the interface
                    try {
                        Class<?> clz = Class.forName(paramType.getField("clz").get(null).toString());
                        newParamTypes[i] = clz;
                        remoteProxyParamIndexes.add(i);
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException("Failed to get class from remoteProxies package for " + paramType.getName(), e);
                    }
                }
            }

            Object result;
            // step3: we need to reconstruct both the paramTypes and args
            // For paramTypes, each element at (index+1) in remoteProxyParamIndexes need to be added as UUID.class
            // For args, each element at index in remoteProxyParamIndexes need to be replaced with the UUID of the remote object
            if (paramTypes.length > 0 && !remoteProxyParamIndexes.isEmpty()) {
                Class<?>[] finalParamTypes = new Class<?>[paramTypes.length + remoteProxyParamIndexes.size()];
                Object[] finalArgs = new Object[args.length + remoteProxyParamIndexes.size()];
                int j = 0;
                for (int i = 0; i < paramTypes.length; i++) {
                    finalParamTypes[j] = newParamTypes[i];
                    finalArgs[j] = args[i];
                    if (remoteProxyParamIndexes.contains(i)) {
                        // set object to null to avoid NotSerializableException
                        // the actual object will be extracted remotely through UUID
                        finalArgs[j] = null;
                        finalParamTypes[j + 1] = UUID.class;
                        finalArgs[j + 1] = ((RemoteObjectProxy) Proxy.getInvocationHandler(args[i])).getObjectUniqueID();
                        j++;
                    }
                    j++;
                }
                result = remoteObject.invoke(method.getName(), finalParamTypes, finalArgs);
            } else {
                result = remoteObject.invoke(method.getName(), newParamTypes, args);
            }

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

package org.apache.hadoop.hdfs.rmi.server;


import org.apache.hadoop.hdfs.rmi.MyUUID;
import org.apache.hadoop.hdfs.rmi.RmiUtils;

import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.UUID;

public class RemoteObjectImpl extends UnicastRemoteObject implements RemoteObject {
    /** The actual object that this RemoteObjectImpl wraps */
    private Object actualObject;
    /** The unique ID of the actual object */
    private UUID objectUniqueID;
    private Class<?> actualObjectClass;

    public RemoteObjectImpl(Object actualObject, UUID id, int port) throws RemoteException {
        super(port);
        this.actualObject = actualObject;
        this.objectUniqueID = id;
        this.actualObjectClass = actualObject.getClass();
    }

    /**
     * The invoke paramTypes may contains RMI interfaces, and the corresponding args may contains MyUUID
     * For example, given method signature: public void setConf(ConfigurationInterface conf) -> public void setConf(MyUUID uuid)
     * The paramTypes will be MyUUID.class, and the args will be the actual MyUUID object.
     * This override method will replace the MyUUID with the actual object 
     * and replace the MyUUID.class with the actual class get from the actual object
     */
    @Override
    public Object invoke(String methodName, Class<?>[] paramTypes, Object[] args) throws RemoteException {
        try {

            Class<?>[] finalParamTypes;
            Object[] finalArgs;

            // Iterate through the paramTypes, if type is a MyUUID, then replace it with the actual object
            for (int i = 0; i < paramTypes.length; i++) {
                Class<?> currentType = paramTypes[i];
                if (currentType == MyUUID.class) {
                    UUID uuid = ((MyUUID) args[i]).get();
                    args[i] = ActualObjectMap.getActualObject(uuid);
                    paramTypes[i] = args[i].getClass();
                }
            }

            Method method = actualObject.getClass().getMethod(methodName, paramTypes);
            Object result = method.invoke(actualObject, args);
            if (result == null || isPrimitiveOrWrapper(result.getClass())) {
                return result;
            } else {
                if (ActualObjectMap.containsActualObject(result)) {
                    return ActualObjectMap.getRemoteObjectFromActualObject(result);
                } else {
                    UUID resultObjectID = UUID.randomUUID();
                    // If the result is a complex object, wrap it in a RemoteObjectImpl
                    Object returnObject = new RemoteObjectImpl(result, resultObjectID, RmiUtils.getRmiObjectPort());
                    ActualObjectMap.put(resultObjectID, (RemoteObjectImpl) returnObject);
                    return returnObject;
                }
            }

            /*
            if (methodName.contains("setConf")) {
                // remove the last element from the paramTypes
                Class<?>[] newParamTypes = new Class<?>[paramTypes.length - 1];
                for (int i = 0; i < newParamTypes.length; i++) {
                    newParamTypes[i] = paramTypes[i];
                }

                method = actualObject.getClass().getMethod(methodName, newParamTypes);
                // Invoke the method
                // Here we need a strategy to get the correct object
                Object[] newArgs = new Object[1];
                newArgs[0] = RmiUtils.getRmiObjectFromMap((UUID) args[args.length - 1]);
                result = method.invoke(actualObject, newArgs);
            } else {
                // Find the method
                method = actualObject.getClass().getMethod(methodName, paramTypes);
                // Invoke the method
                result = method.invoke(actualObject, args);
            }*/


            /*
            UUID resultObjectID = UUID.randomUUID();
            RmiUtils.storeRmiObjectToMap(resultObjectID, result);
            // If the result is a complex object, wrap it in a RemoteObjectImpl
            if (result != null && !isPrimitiveOrWrapper(result.getClass())) {
                return new RemoteObjectImpl(result, resultObjectID, RmiUtils.getRmiObjectPort());
            } else {
                return result;
            } */
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

    public Object getActualObject() {
        return actualObject;
    }

    public UUID getObjectUniqueID() {
        return objectUniqueID;
    }
}


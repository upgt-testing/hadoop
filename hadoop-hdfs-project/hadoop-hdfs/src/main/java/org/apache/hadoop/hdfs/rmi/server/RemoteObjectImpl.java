package org.apache.hadoop.hdfs.rmi.server;


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

    public RemoteObjectImpl(Object actualObject, UUID id, int port) throws RemoteException {
        super(port);
        this.actualObject = actualObject;
        this.objectUniqueID = id;
    }

    @Override
    public Object invoke(String methodName, Class<?>[] paramTypes, Object[] args) throws RemoteException {
        try {

            Class<?>[] finalParamTypes;
            Object[] finalArgs;

            // If length of paramTypes is 0 or 1, then there must be no UUID in the args
            if (paramTypes.length <= 1) {
                finalParamTypes = paramTypes;
                finalArgs = args;
            } else {
                int numUUIDs = 0;
                for (Class<?> paramType : paramTypes) {
                    if (paramType == UUID.class) {
                        numUUIDs++;
                    }
                }

                finalParamTypes = new Class<?>[paramTypes.length - numUUIDs];
                finalArgs = new Object[args.length - numUUIDs];
                int index = 0;
                // iterate through the paramTypes, if type is a UUID, then replace it with the actual object
                for (int i = 0; i < paramTypes.length; i++) {
                    Class<?> currentType = paramTypes[i];
                    // If currentType is UUID, then the previous element should be the actual object
                    if (currentType == UUID.class) {
                        // i = 0 will never be a UUID so we can safely access i - 1
                        finalArgs[index - 1] = ActualObjectMap.getActualObject((UUID) args[i]);
                    } else {
                        finalParamTypes[index] = currentType;
                        finalArgs[index] = args[i];
                        index += 1;
                    }
                }
            }

            Method method = actualObject.getClass().getMethod(methodName, finalParamTypes);
            Object result = method.invoke(actualObject, finalArgs);
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


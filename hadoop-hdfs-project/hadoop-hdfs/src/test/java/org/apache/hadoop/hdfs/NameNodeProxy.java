package org.apache.hadoop.hdfs;

import org.apache.hadoop.hdfs.server.namenode.NameNodeJVMInterface;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class NameNodeProxy implements InvocationHandler {
    private final Object target;

    public NameNodeProxy(Object target) {
        this.target = target;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // Delegate method calls to the target NameNode
        return method.invoke(target, args);
    }

    // Factory method to create the proxy
    public static NameNodeJVMInterface createProxy(Object target, ClassLoader appClassLoader) {
        return (NameNodeJVMInterface) Proxy.newProxyInstance(
                appClassLoader, // Use the app class loader to create the proxy
                new Class<?>[]{NameNodeJVMInterface.class}, // Interface to implement
                new NameNodeProxy(target) // Invocation handler
        );
    }
}

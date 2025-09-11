package org.apache.hadoop.yarn.event;

public interface EventHandlerJVMInterface<T> {

    void handle(T arg0);
}

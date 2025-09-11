package org.apache.hadoop.yarn.event;

import org.apache.hadoop.service.AbstractServiceJVMInterface;

public interface EventDispatcherJVMInterface<T> extends AbstractServiceJVMInterface, EventHandlerJVMInterface<T> {

    void handle(T arg0);

    void disableExitOnError();
}

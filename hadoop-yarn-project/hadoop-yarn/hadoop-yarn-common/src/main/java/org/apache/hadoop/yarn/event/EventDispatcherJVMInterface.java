package org.apache.hadoop.yarn.event;

import org.apache.hadoop.service.AbstractServiceJVMInterface;

public interface EventDispatcherJVMInterface<T> extends AbstractServiceJVMInterface, EventHandlerJVMInterface<T> {

    void setMetrics_bridge(java.lang.Object arg0);

    void handle(T arg0);

    void disableExitOnError();
}

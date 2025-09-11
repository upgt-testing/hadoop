package org.apache.hadoop.yarn.event;

public interface AbstractEventJVMInterface<TYPE> extends EventJVMInterface<TYPE> {

    java.lang.String toString();

    TYPE getType();

    long getTimestamp();
}

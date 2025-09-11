package org.apache.hadoop.yarn.event;

public interface EventJVMInterface<TYPE> {

    java.lang.String toString();

    TYPE getType();

    long getTimestamp();
}

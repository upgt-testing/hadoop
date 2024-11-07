package org.apache.hadoop.hdfs.remoteProxies;

public interface TypeReferenceInterface<T> {
    int compareTo(TypeReferenceInterface<T> arg0);
    java.lang.reflect.Type getType();
}
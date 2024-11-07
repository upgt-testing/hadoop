package org.apache.hadoop.hdfs.remoteProxies;

public interface HttpFieldInterface {
    org.eclipse.jetty.http.HttpHeader getHeader();
    java.lang.String toString();
    boolean contains(java.lang.String arg0);
    java.lang.String getValue();
    int getIntValue();
    java.lang.String getName();
    boolean is(java.lang.String arg0);
    int nameHashCode();
    int hashCode();
    boolean equals(java.lang.Object arg0);
    boolean isSameName(HttpFieldInterface arg0);
    java.lang.String[] getValues();
    java.lang.String getLowerCaseName();
    long getLongValue();
}
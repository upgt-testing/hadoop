package org.apache.hadoop.hdfs.remoteProxies;

public interface ServletContextEventInterface {
    javax.servlet.ServletContext getServletContext();
    java.lang.Object getSource();
    java.lang.String toString();
}
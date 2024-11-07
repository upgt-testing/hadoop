package org.apache.hadoop.hdfs.remoteProxies;

public interface CookieInterface {
    void setMaxAge(int arg0);
    java.lang.String getDomain();
    void setHttpOnly(boolean arg0);
    java.lang.String getValue();
    void setDomain(java.lang.String arg0);
    void setValue(java.lang.String arg0);
    java.lang.String getName();
    boolean isHttpOnly();
    void setPath(java.lang.String arg0);
    void setComment(java.lang.String arg0);
    int getVersion();
    java.lang.String getPath();
    void setSecure(boolean arg0);
    boolean getSecure();
    boolean isToken(java.lang.String arg0);
    java.lang.String getComment();
    void setVersion(int arg0);
    java.lang.Object clone();
    int getMaxAge();
}
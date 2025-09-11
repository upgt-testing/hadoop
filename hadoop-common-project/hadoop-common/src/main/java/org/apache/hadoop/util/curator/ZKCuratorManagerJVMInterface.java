package org.apache.hadoop.util.curator;

public interface ZKCuratorManagerJVMInterface {

    void createRootDirRecursively(java.lang.String arg0, java.util.List<org.apache.zookeeper.data.ACL> arg1) throws java.lang.Exception;

    boolean create(java.lang.String arg0) throws java.lang.Exception;

    void setData(java.lang.String arg0, byte[] arg1, int arg2) throws java.lang.Exception;

    java.util.List getACL(java.lang.String arg0) throws java.lang.Exception;

    void setData(java.lang.String arg0, java.lang.String arg1, int arg2) throws java.lang.Exception;

    void start() throws java.io.IOException;

    byte[] getData(java.lang.String arg0) throws java.lang.Exception;

    void start(java.util.List<org.apache.curator.framework.AuthInfo> arg0) throws java.io.IOException;

    void safeSetData(java.lang.String arg0, byte[] arg1, int arg2, java.util.List<org.apache.zookeeper.data.ACL> arg3, java.lang.String arg4) throws java.lang.Exception;

    boolean delete(java.lang.String arg0) throws java.lang.Exception;

    java.lang.String getStringData(java.lang.String arg0, org.apache.zookeeper.data.Stat arg1) throws java.lang.Exception;

    void safeCreate_bridge(java.lang.String arg0, byte[] arg1, java.util.List<org.apache.zookeeper.data.ACL> arg2, java.lang.Object arg3, java.util.List<org.apache.zookeeper.data.ACL> arg4, java.lang.String arg5) throws java.lang.Exception;

    void safeDelete(java.lang.String arg0, java.util.List<org.apache.zookeeper.data.ACL> arg1, java.lang.String arg2) throws java.lang.Exception;

    boolean create(java.lang.String arg0, java.util.List<org.apache.zookeeper.data.ACL> arg1) throws java.lang.Exception;

    void createRootDirRecursively(java.lang.String arg0) throws java.lang.Exception;

    java.lang.Object createTransaction(java.util.List<org.apache.zookeeper.data.ACL> arg0, java.lang.String arg1) throws java.lang.Exception;

    java.util.List getChildren(java.lang.String arg0) throws java.lang.Exception;

    java.lang.String getStringData(java.lang.String arg0) throws java.lang.Exception;

    byte[] getData(java.lang.String arg0, org.apache.zookeeper.data.Stat arg1) throws java.lang.Exception;

    java.lang.Object getCurator();

    boolean exists(java.lang.String arg0) throws java.lang.Exception;

    void close();
}

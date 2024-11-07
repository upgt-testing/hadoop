package org.apache.hadoop.hdfs.remoteProxies;

public interface ServletMappingInterface {
    boolean isDefault();
    SourceInterface getSource();
    java.lang.String getServletName();
    void setServletName(java.lang.String arg0);
    void setPathSpec(java.lang.String arg0);
    void setPathSpecs(java.lang.String[] arg0);
    void setDefault(boolean arg0);
    void dump(java.lang.Appendable arg0, java.lang.String arg1) throws java.io.IOException;
    java.lang.String toString();
    boolean containsPathSpec(java.lang.String arg0);
    java.lang.String[] getPathSpecs();
}
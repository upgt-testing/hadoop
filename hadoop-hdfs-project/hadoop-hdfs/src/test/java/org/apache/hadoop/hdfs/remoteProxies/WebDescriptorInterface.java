package org.apache.hadoop.hdfs.remoteProxies;

public interface WebDescriptorInterface {
    void processOrdering();
    boolean isOrdered();
    void setDistributable(boolean arg0);
    int getMajorVersion();
    ResourceInterface getResource();
    java.util.List<java.lang.String> getOrdering();
    java.util.ArrayList<java.lang.String> getClassNames();
    boolean isDistributable();
    XmlParserInterface newParser(boolean arg0) throws java.lang.ClassNotFoundException;
    void addClassName(java.lang.String arg0);
    java.lang.String toString();
    boolean isValidating();
    XmlParserInterface ensureParser() throws java.lang.ClassNotFoundException;
    void processVersion();
    void setValidating(boolean arg0);
    NodeInterface getRoot();
    int getMinorVersion();
    org.eclipse.jetty.webapp.MetaDataComplete getMetaDataComplete();
    void parse() throws java.lang.Exception;
}
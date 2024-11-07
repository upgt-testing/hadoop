package org.apache.hadoop.hdfs.remoteProxies;

public interface FragmentDescriptorInterface {
    java.util.List<java.lang.String> getBefores();
    ResourceInterface getResource();
    void processOrdering();
    java.util.List<java.lang.String> getAfters();
    org.eclipse.jetty.webapp.MetaDataComplete getMetaDataComplete();
    java.lang.String getName();
    boolean isValidating();
    XmlParserInterface ensureParser() throws java.lang.ClassNotFoundException;
    java.util.ArrayList<java.lang.String> getClassNames();
    XmlParserInterface newParser(boolean arg0) throws java.lang.ClassNotFoundException;
    void setValidating(boolean arg0);
    int getMajorVersion();
    NodeInterface getRoot();
    boolean isDistributable();
    void processVersion();
    void addClassName(java.lang.String arg0);
    void setDistributable(boolean arg0);
    void processBefores(NodeInterface arg0);
    void processAfters(NodeInterface arg0);
    void processName();
    int getMinorVersion();
    java.util.List<java.lang.String> getOrdering();
    org.eclipse.jetty.webapp.FragmentDescriptor.OtherType getOtherType();
    java.lang.String toString();
    void parse() throws java.lang.Exception;
    boolean isOrdered();
}
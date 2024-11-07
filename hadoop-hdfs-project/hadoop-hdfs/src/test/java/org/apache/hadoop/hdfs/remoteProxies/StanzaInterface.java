package org.apache.hadoop.hdfs.remoteProxies;

public interface StanzaInterface {
    void addChild(java.lang.String arg0, StanzaInterface arg1);
    boolean hasChildren(java.lang.String arg0);
    java.lang.String getValueOrNull(java.lang.String arg0) throws org.apache.hadoop.hdfs.util.XMLUtils.InvalidXmlException;
    java.lang.String getValue(java.lang.String arg0) throws org.apache.hadoop.hdfs.util.XMLUtils.InvalidXmlException;
    java.util.List<org.apache.hadoop.hdfs.util.XMLUtils.Stanza> getChildren(java.lang.String arg0) throws org.apache.hadoop.hdfs.util.XMLUtils.InvalidXmlException;
    void setValue(java.lang.String arg0);
    java.lang.String getValue();
    java.lang.String toString();
}
package org.apache.hadoop.hdfs.remoteProxies;

public interface XmlParserInterface {
    java.lang.String getXpath();
    java.lang.String getDTD();
    void setValidating(boolean arg0);
    void redirectEntity(java.lang.String arg0, java.net.URL arg1);
    boolean isValidating();
    NodeInterface parse(InputSourceInterface arg0) throws java.io.IOException, org.xml.sax.SAXException;
    NodeInterface parse(java.lang.String arg0) throws java.io.IOException, org.xml.sax.SAXException;
    NodeInterface parse(java.io.File arg0) throws java.io.IOException, org.xml.sax.SAXException;
    void setXpath(java.lang.String arg0);
    InputSourceInterface resolveEntity(java.lang.String arg0, java.lang.String arg1);
    void addContentHandler(java.lang.String arg0, org.xml.sax.ContentHandler arg1);
    NodeInterface parse(java.io.InputStream arg0) throws java.io.IOException, org.xml.sax.SAXException;
}
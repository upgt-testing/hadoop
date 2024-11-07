package org.apache.hadoop.hdfs.remoteProxies;

public interface JsonStreamContextInterface {
    java.lang.String typeDesc();
    java.lang.String toString();
    java.lang.Object getCurrentValue();
    java.lang.String getCurrentName();
    int getEntryCount();
    boolean inArray();
    boolean hasCurrentIndex();
    boolean inRoot();
    JsonLocationInterface startLocation(ContentReferenceInterface arg0);
    JsonStreamContextInterface getParent();
    void setCurrentValue(java.lang.Object arg0);
    boolean hasPathSegment();
    JsonPointerInterface pathAsPointer(boolean arg0);
    boolean inObject();
    java.lang.String getTypeDesc();
    JsonLocationInterface getStartLocation(java.lang.Object arg0);
    int getCurrentIndex();
    JsonPointerInterface pathAsPointer();
    boolean hasCurrentName();
}
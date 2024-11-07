package org.apache.hadoop.hdfs.remoteProxies;

public interface JsonPointerInterface {
    boolean mayMatchProperty();
    JsonPointerInterface appendIndex(int arg0);
    JsonPointerInterface _parseTail(java.lang.String arg0);
    java.lang.String getMatchingProperty();
    int getMatchingIndex();
    java.lang.Object writeReplace();
    boolean equals(java.lang.Object arg0);
    int _parseIndex(java.lang.String arg0);
    boolean _compare(java.lang.String arg0, int arg1, java.lang.String arg2, int arg3);
    int hashCode();
    JsonPointerInterface empty();
    boolean mayMatchElement();
    boolean matchesElement(int arg0);
    JsonPointerInterface tail();
    JsonPointerInterface forPath(JsonStreamContextInterface arg0, boolean arg1);
    void _appendEscaped(java.lang.StringBuilder arg0, java.lang.String arg1);
    JsonPointerInterface last();
    JsonPointerInterface _constructHead();
    java.lang.String toString();
    JsonPointerInterface matchProperty(java.lang.String arg0);
    JsonPointerInterface append(JsonPointerInterface arg0);
    int length();
    int _extractEscapedSegment(java.lang.String arg0, int arg1, int arg2, java.lang.StringBuilder arg3);
    JsonPointerInterface appendProperty(java.lang.String arg0);
    JsonPointerInterface compile(java.lang.String arg0) throws java.lang.IllegalArgumentException;
    boolean matchesProperty(java.lang.String arg0);
    JsonPointerInterface _constructHead(int arg0, JsonPointerInterface arg1);
    void _appendEscape(java.lang.StringBuilder arg0, char arg1);
    boolean matches();
    JsonPointerInterface head();
    JsonPointerInterface valueOf(java.lang.String arg0);
    JsonPointerInterface _buildPath(java.lang.String arg0, int arg1, java.lang.String arg2, PointerParentInterface arg3);
    JsonPointerInterface matchElement(int arg0);
}
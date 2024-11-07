package org.apache.hadoop.hdfs.remoteProxies;

public interface ContentReferenceInterface {
    int _append(java.lang.StringBuilder arg0, java.lang.String arg1);
    java.lang.String buildSourceDescription();
    ContentReferenceInterface construct(boolean arg0, java.lang.Object arg1, int arg2, int arg3);
    ContentReferenceInterface unknown();
    java.lang.Object getRawContent();
    ContentReferenceInterface rawReference(boolean arg0, java.lang.Object arg1);
    void _truncateOffsets(int[] arg0, int arg1);
    int maxContentSnippetLength();
    java.lang.StringBuilder appendSourceDescription(java.lang.StringBuilder arg0);
    boolean hasTextualContent();
    void writeObject(java.io.ObjectOutputStream arg0) throws java.io.IOException;
    int contentLength();
    java.lang.Object readResolve();
    java.lang.String _truncate(char[] arg0, int[] arg1, int arg2);
    boolean equals(java.lang.Object arg0);
    java.lang.String _truncate(byte[] arg0, int[] arg1, int arg2);
    void readObject(java.io.ObjectInputStream arg0) throws java.io.IOException;
    int contentOffset();
    int hashCode();
    ContentReferenceInterface rawReference(java.lang.Object arg0);
    java.lang.String _truncate(java.lang.CharSequence arg0, int[] arg1, int arg2);
    boolean _appendEscaped(java.lang.StringBuilder arg0, int arg1);
    ContentReferenceInterface construct(boolean arg0, java.lang.Object arg1);
}
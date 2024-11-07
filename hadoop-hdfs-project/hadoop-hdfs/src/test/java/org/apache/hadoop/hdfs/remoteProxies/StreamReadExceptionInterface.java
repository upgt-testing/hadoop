package org.apache.hadoop.hdfs.remoteProxies;

public interface StreamReadExceptionInterface {
    java.lang.Throwable fillInStackTrace();
    java.lang.Object getProcessor();
    void writeObject(java.io.ObjectOutputStream arg0) throws java.io.IOException;
//    void printStackTrace(java.io.PrintWriter arg0);
    RequestPayloadInterface getRequestPayload();
    void printStackTrace();
    void setStackTrace(java.lang.StackTraceElement[] arg0);
    java.lang.Throwable fillInStackTrace(int arg0);
    void readObject(java.io.ObjectInputStream arg0) throws java.io.IOException, java.lang.ClassNotFoundException;
//    void printEnclosedStackTrace(java.lang.Throwable.PrintStreamOrWriter arg0, java.lang.StackTraceElement[] arg1, java.lang.String arg2, java.lang.String arg3, java.util.Set<java.lang.Throwable> arg4);
    java.lang.StackTraceElement[] getOurStackTrace();
    java.lang.String getLocalizedMessage();
//    JsonParserInterface getProcessor();
    JsonLocationInterface getLocation();
    java.lang.Throwable[] getSuppressed();
    java.lang.String getOriginalMessage();
    java.lang.String getMessageSuffix();
    java.lang.String toString();
    java.lang.Throwable initCause(java.lang.Throwable arg0);
    void addSuppressed(java.lang.Throwable arg0);
    int validateSuppressedExceptionsList(java.util.List<java.lang.Throwable> arg0) throws java.io.IOException;
    void clearLocation();
    StreamReadExceptionInterface withParser(JsonParserInterface arg0);
    java.lang.Throwable getCause();
    StreamReadExceptionInterface withRequestPayload(RequestPayloadInterface arg0);
    java.lang.String getRequestPayloadAsString();
    void printStackTrace(java.io.PrintStream arg0);
//    void printStackTrace(java.lang.Throwable.PrintStreamOrWriter arg0);
    java.lang.String getMessage();
    java.lang.StackTraceElement[] getStackTrace();
}
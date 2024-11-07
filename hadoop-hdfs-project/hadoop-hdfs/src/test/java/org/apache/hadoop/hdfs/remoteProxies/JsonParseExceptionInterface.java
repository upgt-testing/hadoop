package org.apache.hadoop.hdfs.remoteProxies;

public interface JsonParseExceptionInterface {
    java.lang.String toString();
//    void printStackTrace(java.io.PrintWriter arg0);
    java.lang.StackTraceElement[] getOurStackTrace();
    RequestPayloadInterface getRequestPayload();
//    void printEnclosedStackTrace(java.lang.Throwable.PrintStreamOrWriter arg0, java.lang.StackTraceElement[] arg1, java.lang.String arg2, java.lang.String arg3, java.util.Set<java.lang.Throwable> arg4);
    void printStackTrace(java.io.PrintStream arg0);
    void writeObject(java.io.ObjectOutputStream arg0) throws java.io.IOException;
    java.lang.Throwable[] getSuppressed();
    JsonParseExceptionInterface withParser(JsonParserInterface arg0);
    java.lang.String getRequestPayloadAsString();
    java.lang.Throwable initCause(java.lang.Throwable arg0);
    java.lang.Throwable fillInStackTrace();
    java.lang.String getLocalizedMessage();
//    void printStackTrace(java.lang.Throwable.PrintStreamOrWriter arg0);
    void addSuppressed(java.lang.Throwable arg0);
    void readObject(java.io.ObjectInputStream arg0) throws java.io.IOException, java.lang.ClassNotFoundException;
    void printStackTrace();
    void clearLocation();
//    StreamReadExceptionInterface withParser(JsonParserInterface arg0);
    java.lang.Object getProcessor();
    void setStackTrace(java.lang.StackTraceElement[] arg0);
    JsonParseExceptionInterface withRequestPayload(RequestPayloadInterface arg0);
    java.lang.String getOriginalMessage();
    java.lang.Throwable getCause();
//    JsonParserInterface getProcessor();
    JsonLocationInterface getLocation();
    int validateSuppressedExceptionsList(java.util.List<java.lang.Throwable> arg0) throws java.io.IOException;
    java.lang.String getMessageSuffix();
    java.lang.Throwable fillInStackTrace(int arg0);
    java.lang.StackTraceElement[] getStackTrace();
    java.lang.String getMessage();
//    StreamReadExceptionInterface withRequestPayload(RequestPayloadInterface arg0);
}
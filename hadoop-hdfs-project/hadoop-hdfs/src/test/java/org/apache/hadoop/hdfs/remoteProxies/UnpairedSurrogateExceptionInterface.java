package org.apache.hadoop.hdfs.remoteProxies;

public interface UnpairedSurrogateExceptionInterface {
    java.lang.String getLocalizedMessage();
    void printStackTrace();
    java.lang.String getMessage();
//    void printStackTrace(java.io.PrintWriter arg0);
    int validateSuppressedExceptionsList(java.util.List<java.lang.Throwable> arg0) throws java.io.IOException;
    java.lang.StackTraceElement[] getStackTrace();
    void printStackTrace(java.io.PrintStream arg0);
    java.lang.Throwable[] getSuppressed();
    java.lang.Throwable getCause();
    java.lang.Throwable initCause(java.lang.Throwable arg0);
//    void printEnclosedStackTrace(java.lang.Throwable.PrintStreamOrWriter arg0, java.lang.StackTraceElement[] arg1, java.lang.String arg2, java.lang.String arg3, java.util.Set<java.lang.Throwable> arg4);
    void readObject(java.io.ObjectInputStream arg0) throws java.io.IOException, java.lang.ClassNotFoundException;
    void addSuppressed(java.lang.Throwable arg0);
    void writeObject(java.io.ObjectOutputStream arg0) throws java.io.IOException;
    java.lang.Throwable fillInStackTrace(int arg0);
    java.lang.String toString();
//    void printStackTrace(java.lang.Throwable.PrintStreamOrWriter arg0);
    void setStackTrace(java.lang.StackTraceElement[] arg0);
    java.lang.StackTraceElement[] getOurStackTrace();
    java.lang.Throwable fillInStackTrace();
}
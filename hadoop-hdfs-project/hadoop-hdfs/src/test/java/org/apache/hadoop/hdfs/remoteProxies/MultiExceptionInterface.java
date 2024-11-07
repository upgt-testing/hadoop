package org.apache.hadoop.hdfs.remoteProxies;

public interface MultiExceptionInterface {
    void writeObject(java.io.ObjectOutputStream arg0) throws java.io.IOException;
    java.lang.Throwable getThrowable(int arg0);
    java.lang.Throwable fillInStackTrace(int arg0);
    java.lang.String getLocalizedMessage();
    java.lang.StackTraceElement[] getStackTrace();
    void ifExceptionThrowRuntime() throws java.lang.Error;
//    void printStackTrace(java.io.PrintStream arg0);
    void add(java.lang.Throwable arg0);
    void printStackTrace(java.io.PrintWriter arg0);
    void addSuppressed(java.lang.Throwable arg0);
//    void printStackTrace(java.lang.Throwable.PrintStreamOrWriter arg0);
//    void printEnclosedStackTrace(java.lang.Throwable.PrintStreamOrWriter arg0, java.lang.StackTraceElement[] arg1, java.lang.String arg2, java.lang.String arg3, java.util.Set<java.lang.Throwable> arg4);
    java.lang.Throwable getCause();
    java.lang.Throwable fillInStackTrace();
    java.lang.Throwable initCause(java.lang.Throwable arg0);
    java.lang.String getMessage();
    void ifExceptionThrowSuppressed() throws java.lang.Exception;
    int size();
    void setStackTrace(java.lang.StackTraceElement[] arg0);
    java.lang.StackTraceElement[] getOurStackTrace();
    void readObject(java.io.ObjectInputStream arg0) throws java.io.IOException, java.lang.ClassNotFoundException;
    int validateSuppressedExceptionsList(java.util.List<java.lang.Throwable> arg0) throws java.io.IOException;
    java.lang.String toString();
    void ifExceptionThrowMulti() throws org.eclipse.jetty.util.MultiException;
    void ifExceptionThrow() throws java.lang.Exception;
    java.lang.Throwable[] getSuppressed();
    void printStackTrace();
    java.util.List<java.lang.Throwable> getThrowables();
}
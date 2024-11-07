package org.apache.hadoop.hdfs.remoteProxies;

public interface UnavailableExceptionInterface {
    java.lang.Throwable fillInStackTrace();
    void writeObject(java.io.ObjectOutputStream arg0) throws java.io.IOException;
    javax.servlet.Servlet getServlet();
    java.lang.Throwable initCause(java.lang.Throwable arg0);
//    void printEnclosedStackTrace(java.lang.Throwable.PrintStreamOrWriter arg0, java.lang.StackTraceElement[] arg1, java.lang.String arg2, java.lang.String arg3, java.util.Set<java.lang.Throwable> arg4);
//    void printStackTrace(java.io.PrintStream arg0);
    void printStackTrace(java.io.PrintWriter arg0);
    java.lang.Throwable getRootCause();
    java.lang.String getLocalizedMessage();
    java.lang.String getMessage();
    java.lang.StackTraceElement[] getOurStackTrace();
//    void printStackTrace(java.lang.Throwable.PrintStreamOrWriter arg0);
    void addSuppressed(java.lang.Throwable arg0);
    int getUnavailableSeconds();
    java.lang.Throwable fillInStackTrace(int arg0);
    void printStackTrace();
    boolean isPermanent();
    int validateSuppressedExceptionsList(java.util.List<java.lang.Throwable> arg0) throws java.io.IOException;
    java.lang.Throwable getCause();
    java.lang.String toString();
    void readObject(java.io.ObjectInputStream arg0) throws java.io.IOException, java.lang.ClassNotFoundException;
    java.lang.Throwable[] getSuppressed();
    java.lang.StackTraceElement[] getStackTrace();
    void setStackTrace(java.lang.StackTraceElement[] arg0);
}
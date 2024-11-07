package org.apache.hadoop.hdfs.remoteProxies;

public interface AddBlockPoolExceptionInterface {
    void setStackTrace(java.lang.StackTraceElement[] arg0);
    void printStackTrace(java.io.PrintWriter arg0);
//    void printStackTrace(java.lang.Throwable.PrintStreamOrWriter arg0);
    void addSuppressed(java.lang.Throwable arg0);
//    void printEnclosedStackTrace(java.lang.Throwable.PrintStreamOrWriter arg0, java.lang.StackTraceElement[] arg1, java.lang.String arg2, java.lang.String arg3, java.util.Set<java.lang.Throwable> arg4);
    java.lang.String getLocalizedMessage();
    java.lang.Throwable fillInStackTrace();
    java.lang.String toString();
    java.lang.StackTraceElement[] getStackTrace();
    java.lang.StackTraceElement[] getOurStackTrace();
    void printStackTrace();
    java.lang.Throwable fillInStackTrace(int arg0);
    void writeObject(java.io.ObjectOutputStream arg0) throws java.io.IOException;
    boolean hasExceptions();
//    void printStackTrace(java.io.PrintStream arg0);
    int validateSuppressedExceptionsList(java.util.List<java.lang.Throwable> arg0) throws java.io.IOException;
    java.lang.String getMessage();
    java.lang.Throwable initCause(java.lang.Throwable arg0);
    void readObject(java.io.ObjectInputStream arg0) throws java.io.IOException, java.lang.ClassNotFoundException;
    void mergeException(AddBlockPoolExceptionInterface arg0);
    java.lang.Throwable[] getSuppressed();
    java.util.Map<org.apache.hadoop.hdfs.server.datanode.fsdataset.FsVolumeSpi, java.io.IOException> getFailingVolumes();
    java.lang.Throwable getCause();
}
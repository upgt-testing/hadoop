package org.apache.hadoop.hdfs.remoteProxies;

public interface FileIoProviderInterface {
    java.io.FileOutputStream getFileOutputStream(org.apache.hadoop.hdfs.server.datanode.fsdataset.FsVolumeSpi arg0, java.io.File arg1, boolean arg2) throws java.io.FileNotFoundException;
    void sync(org.apache.hadoop.hdfs.server.datanode.fsdataset.FsVolumeSpi arg0, java.io.FileOutputStream arg1) throws java.io.IOException;
    boolean exists(org.apache.hadoop.hdfs.server.datanode.fsdataset.FsVolumeSpi arg0, java.io.File arg1);
    boolean createFile(org.apache.hadoop.hdfs.server.datanode.fsdataset.FsVolumeSpi arg0, java.io.File arg1) throws java.io.IOException;
    void dirSync(org.apache.hadoop.hdfs.server.datanode.fsdataset.FsVolumeSpi arg0, java.io.File arg1) throws java.io.IOException;
    void onFailure(org.apache.hadoop.hdfs.server.datanode.fsdataset.FsVolumeSpi arg0, long arg1);
    boolean mkdirs(org.apache.hadoop.hdfs.server.datanode.fsdataset.FsVolumeSpi arg0, java.io.File arg1) throws java.io.IOException;
    java.lang.String[] list(org.apache.hadoop.hdfs.server.datanode.fsdataset.FsVolumeSpi arg0, java.io.File arg1) throws java.io.IOException;
    void nativeCopyFileUnbuffered(org.apache.hadoop.hdfs.server.datanode.fsdataset.FsVolumeSpi arg0, java.io.File arg1, java.io.File arg2, boolean arg3) throws java.io.IOException;
    boolean fullyDelete(org.apache.hadoop.hdfs.server.datanode.fsdataset.FsVolumeSpi arg0, java.io.File arg1);
    void mkdirsWithExistsCheck(org.apache.hadoop.hdfs.server.datanode.fsdataset.FsVolumeSpi arg0, java.io.File arg1) throws java.io.IOException;
    void transferToSocketFully(org.apache.hadoop.hdfs.server.datanode.fsdataset.FsVolumeSpi arg0, SocketOutputStreamInterface arg1, java.nio.channels.FileChannel arg2, long arg3, int arg4, LongWritableInterface arg5, LongWritableInterface arg6) throws java.io.IOException;
    void flush(org.apache.hadoop.hdfs.server.datanode.fsdataset.FsVolumeSpi arg0, java.io.Flushable arg1) throws java.io.IOException;
    java.io.File[] listFiles(org.apache.hadoop.hdfs.server.datanode.fsdataset.FsVolumeSpi arg0, java.io.File arg1) throws java.io.IOException;
    void posixFadvise(org.apache.hadoop.hdfs.server.datanode.fsdataset.FsVolumeSpi arg0, java.lang.String arg1, java.io.FileDescriptor arg2, long arg3, long arg4, int arg5) throws org.apache.hadoop.io.nativeio.NativeIOException;
    void replaceFile(org.apache.hadoop.hdfs.server.datanode.fsdataset.FsVolumeSpi arg0, java.io.File arg1, java.io.File arg2) throws java.io.IOException;
    ProfilingFileIoEventsInterface getProfilingEventHook();
    void move(org.apache.hadoop.hdfs.server.datanode.fsdataset.FsVolumeSpi arg0, java.nio.file.Path arg1, java.nio.file.Path arg2, java.nio.file.CopyOption... arg3) throws java.io.IOException;
    boolean deleteWithExistsCheck(org.apache.hadoop.hdfs.server.datanode.fsdataset.FsVolumeSpi arg0, java.io.File arg1);
    int getHardLinkCount(org.apache.hadoop.hdfs.server.datanode.fsdataset.FsVolumeSpi arg0, java.io.File arg1) throws java.io.IOException;
    java.util.List<java.lang.String> listDirectory(org.apache.hadoop.hdfs.server.datanode.fsdataset.FsVolumeSpi arg0, java.io.File arg1, java.io.FilenameFilter arg2) throws java.io.IOException;
    java.io.FileOutputStream getFileOutputStream(org.apache.hadoop.hdfs.server.datanode.fsdataset.FsVolumeSpi arg0, java.io.File arg1) throws java.io.FileNotFoundException;
    void rename(org.apache.hadoop.hdfs.server.datanode.fsdataset.FsVolumeSpi arg0, java.io.File arg1, java.io.File arg2) throws java.io.IOException;
    void moveFile(org.apache.hadoop.hdfs.server.datanode.fsdataset.FsVolumeSpi arg0, java.io.File arg1, java.io.File arg2) throws java.io.IOException;
    java.io.FileInputStream getShareDeleteFileInputStream(org.apache.hadoop.hdfs.server.datanode.fsdataset.FsVolumeSpi arg0, java.io.File arg1, long arg2) throws java.io.IOException;
    boolean delete(org.apache.hadoop.hdfs.server.datanode.fsdataset.FsVolumeSpi arg0, java.io.File arg1);
    java.io.FileInputStream getFileInputStream(org.apache.hadoop.hdfs.server.datanode.fsdataset.FsVolumeSpi arg0, java.io.File arg1) throws java.io.FileNotFoundException;
    java.io.FileInputStream openAndSeek(org.apache.hadoop.hdfs.server.datanode.fsdataset.FsVolumeSpi arg0, java.io.File arg1, long arg2) throws java.io.IOException;
    void syncFileRange(org.apache.hadoop.hdfs.server.datanode.fsdataset.FsVolumeSpi arg0, java.io.FileDescriptor arg1, long arg2, long arg3, int arg4) throws org.apache.hadoop.io.nativeio.NativeIOException;
    java.io.FileOutputStream getFileOutputStream(org.apache.hadoop.hdfs.server.datanode.fsdataset.FsVolumeSpi arg0, java.io.FileDescriptor arg1);
    java.io.RandomAccessFile getRandomAccessFile(org.apache.hadoop.hdfs.server.datanode.fsdataset.FsVolumeSpi arg0, java.io.File arg1, java.lang.String arg2) throws java.io.FileNotFoundException;
}
package org.apache.hadoop.hdfs.remoteProxies;

import java.nio.channels.FileChannel;
import java.nio.file.CopyOption;
import java.util.*;
import java.io.*;
import org.apache.hadoop.hdfs.server.datanode.fsdataset.FsVolumeSpi;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.fs.Path;

public interface FileIoProviderInterface {

    void flush(FsVolumeSpiInterface volume, Flushable f);

    void sync(FsVolumeSpi volume, FileOutputStream fos);

    void dirSync(FsVolumeSpi volume, File dir);

    void syncFileRange(FsVolumeSpi volume, FileDescriptor outFd, long offset, long numBytes, int flags);

    void posixFadvise(FsVolumeSpi volume, String identifier, FileDescriptor outFd, long offset, long length, int flags);

    boolean delete(FsVolumeSpi volume, File f);

    boolean deleteWithExistsCheck(FsVolumeSpi volume, File f);

    void transferToSocketFully(FsVolumeSpi volume, SocketOutputStreamInterface sockOut, FileChannel fileCh, long position, int count, LongWritable waitTime, LongWritable transferTime);

    boolean createFile(FsVolumeSpi volume, File f);

    FileInputStream getFileInputStream(FsVolumeSpi volume, File f);

    FileOutputStream getFileOutputStream(FsVolumeSpi volume, File f, boolean append);

    FileOutputStream getFileOutputStream(FsVolumeSpi volume, File f);

    FileOutputStream getFileOutputStream(FsVolumeSpi volume, FileDescriptor fd);

    FileInputStream getShareDeleteFileInputStream(FsVolumeSpi volume, File f, long offset);

    FileInputStream openAndSeek(FsVolumeSpi volume, File f, long offset);

    RandomAccessFile getRandomAccessFile(FsVolumeSpi volume, File f, String mode);

    boolean fullyDelete(FsVolumeSpi volume, File dir);

    void replaceFile(FsVolumeSpi volume, File src, File target);

    void rename(FsVolumeSpi volume, File src, File target);

    void moveFile(FsVolumeSpi volume, File src, File target);

    void move(FsVolumeSpi volume, PathInterface src, Path target, CopyOption options);

    void nativeCopyFileUnbuffered(FsVolumeSpi volume, File src, File target, boolean preserveFileDate);

    boolean mkdirs(FsVolumeSpi volume, File dir);

    void mkdirsWithExistsCheck(FsVolumeSpi volume, File dir);

    File[] listFiles(FsVolumeSpi volume, File dir);

    String[] list(FsVolumeSpi volume, File dir);

    List<String> listDirectory(FsVolumeSpi volume, File dir, FilenameFilter filter);

    int getHardLinkCount(FsVolumeSpi volume, File f);

    boolean exists(FsVolumeSpi volume, File f);

    ProfilingFileIoEventsInterface getProfilingEventHook();
}

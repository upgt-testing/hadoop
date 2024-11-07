package org.apache.hadoop.hdfs.remoteProxies;

public interface FSDataInputStreamBuilderInterface {
//    B opt(java.lang.String arg0, int arg1);
    PathInterface getPath();
    FileStatusInterface getStatus();
    void rejectUnknownMandatoryKeys(java.util.Collection<java.lang.String> arg0, java.lang.String arg1) throws java.lang.IllegalArgumentException;
//    B opt(java.lang.String arg0, float arg1);
//    B opt(java.lang.String arg0, java.lang.String... arg1);
    java.util.Set<java.lang.String> getOptionalKeys();
    int getBufferSize();
    void rejectUnknownMandatoryKeys(java.util.Set<java.lang.String> arg0, java.util.Collection<java.lang.String> arg1, java.lang.String arg2) throws java.lang.IllegalArgumentException;
    void initFromFS();
//    B must(java.lang.String arg0, int arg1);
    java.lang.Object build() throws java.lang.IllegalArgumentException, java.lang.UnsupportedOperationException, java.io.IOException;
//    B opt(java.lang.String arg0, long arg1);
    ConfigurationInterface getOptions();
    org.apache.hadoop.fs.FutureDataInputStreamBuilder builder();
//    B must(java.lang.String arg0, double arg1);
    org.apache.hadoop.fs.FutureDataInputStreamBuilder bufferSize(int arg0);
//    S build() throws java.lang.IllegalArgumentException, java.lang.UnsupportedOperationException, java.io.IOException;
//    B must(java.lang.String arg0, long arg1);
//    B must(java.lang.String arg0, boolean arg1);
    java.util.Optional<org.apache.hadoop.fs.PathHandle> getOptionalPathHandle();
    FileSystemInterface getFS();
//    B must(java.lang.String arg0, java.lang.String... arg1);
    java.util.Optional<org.apache.hadoop.fs.Path> getOptionalPath();
//    java.util.concurrent.CompletableFuture<org.apache.hadoop.fs.FSDataInputStream> build() throws java.io.IOException;
//    B must(java.lang.String arg0, float arg1);
//    B getThisBuilder();
//    B opt(java.lang.String arg0, boolean arg1);
    org.apache.hadoop.fs.FutureDataInputStreamBuilder withFileStatus(FileStatusInterface arg0);
    java.util.Set<java.lang.String> getMandatoryKeys();
    org.apache.hadoop.fs.FutureDataInputStreamBuilder getThisBuilder();
//    B must(java.lang.String arg0, java.lang.String arg1);
//    B opt(java.lang.String arg0, java.lang.String arg1);
    org.apache.hadoop.fs.PathHandle getPathHandle();
//    B opt(java.lang.String arg0, double arg1);
}
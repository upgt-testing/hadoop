package org.apache.hadoop.fs;

public interface FSDataOutputStreamBuilderJVMInterface<S, B> {

    B recursive();

    B must(java.lang.String arg0, int arg1);

    B permission_bridge(org.apache.hadoop.fs.permission.FsPermissionJVMInterface arg0);

    B must(java.lang.String arg0, java.lang.String arg1);

    B must(java.lang.String arg0, boolean arg1);

    B opt(java.lang.String arg0, int arg1);

    B create();

    S build() throws java.lang.IllegalArgumentException, java.io.IOException;

    B opt(java.lang.String arg0, float arg1);

    B opt(java.lang.String arg0, boolean arg1);

    B replication(short arg0);

    B progress_bridge(java.lang.Object arg0);

    B opt(java.lang.String arg0, double arg1);

    B must(java.lang.String arg0, double arg1);

    B must(java.lang.String arg0, java.lang.String[] arg1);

    B opt(java.lang.String arg0, java.lang.String arg1);

    B bufferSize(int arg0);

    B opt(java.lang.String arg0, java.lang.String[] arg1);

    B checksumOpt_bridge(java.lang.Object arg0);

    B overwrite(boolean arg0);

    B blockSize(long arg0);

    B append();

    B must(java.lang.String arg0, float arg1);
}

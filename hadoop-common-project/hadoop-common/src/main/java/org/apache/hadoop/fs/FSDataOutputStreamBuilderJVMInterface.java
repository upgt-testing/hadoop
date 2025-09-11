package org.apache.hadoop.fs;

public interface FSDataOutputStreamBuilderJVMInterface<S, B> {

    B recursive();

    B permission_bridge(org.apache.hadoop.fs.permission.FsPermissionJVMInterface arg0);

    B bufferSize(int arg0);

    B create();

    B checksumOpt_bridge(java.lang.Object arg0);

    S build() throws java.io.IOException;

    B overwrite(boolean arg0);

    B blockSize(long arg0);

    B replication(short arg0);

    B progress_bridge(java.lang.Object arg0);

    B append();
}

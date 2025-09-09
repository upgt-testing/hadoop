package org.apache.hadoop.fs;

import org.apache.hadoop.fs.impl.AbstractFSBuilderImplJVMInterface;

public interface FSDataOutputStreamBuilderJVMInterface<S, B> extends AbstractFSBuilderImplJVMInterface<S, B> {

    B recursive();

    B getThisBuilder();

    B permission_bridge(org.apache.hadoop.fs.permission.FsPermissionJVMInterface arg0);

    B bufferSize(int arg0);

    B create();

    B checksumOpt_bridge(java.lang.Object arg0);

    B overwrite(boolean arg0);

    S build() throws java.lang.IllegalArgumentException, java.io.IOException;

    B blockSize(long arg0);

    B replication(short arg0);

    B append();

    B progress_bridge(java.lang.Object arg0);
}

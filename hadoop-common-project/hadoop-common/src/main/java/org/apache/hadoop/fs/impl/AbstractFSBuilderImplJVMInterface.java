package org.apache.hadoop.fs.impl;

import org.apache.hadoop.fs.FSBuilderJVMInterface;

public interface AbstractFSBuilderImplJVMInterface<S, B> extends FSBuilderJVMInterface<S, B> {

    B must(java.lang.String arg0, int arg1);

    B must(java.lang.String arg0, long arg1);

    B must(java.lang.String arg0, java.lang.String arg1);

    B must(java.lang.String arg0, boolean arg1);

    java.lang.Object getPathHandle();

    B opt(java.lang.String arg0, int arg1);

    B opt(java.lang.String arg0, long arg1);

    java.util.Set getMandatoryKeys();

    B opt(java.lang.String arg0, boolean arg1);

    B opt(java.lang.String arg0, float arg1);

    B opt(java.lang.String arg0, double arg1);

    B getThisBuilder();

    B must(java.lang.String arg0, double arg1);

    B must(java.lang.String arg0, java.lang.String[] arg1);

    org.apache.hadoop.fs.PathJVMInterface getPath();

    java.util.Optional getOptionalPath();

    B opt(java.lang.String arg0, java.lang.String arg1);

    java.util.Set getOptionalKeys();

    B opt(java.lang.String arg0, java.lang.String[] arg1);

    java.util.Optional getOptionalPathHandle();

    org.apache.hadoop.conf.ConfigurationJVMInterface getOptions();

    B must(java.lang.String arg0, float arg1);
}

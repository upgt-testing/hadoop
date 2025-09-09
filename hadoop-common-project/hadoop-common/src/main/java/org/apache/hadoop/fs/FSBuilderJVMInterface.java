package org.apache.hadoop.fs;

public interface FSBuilderJVMInterface<S, B> {

    B must(java.lang.String arg0, int arg1);

    B must(java.lang.String arg0, long arg1);

    B must(java.lang.String arg0, java.lang.String arg1);

    B must(java.lang.String arg0, boolean arg1);

    B opt(java.lang.String arg0, int arg1);

    B opt(java.lang.String arg0, long arg1);

    S build() throws java.lang.IllegalArgumentException, java.lang.UnsupportedOperationException, java.io.IOException;

    B opt(java.lang.String arg0, boolean arg1);

    B opt(java.lang.String arg0, float arg1);

    B opt(java.lang.String arg0, double arg1);

    B must(java.lang.String arg0, double arg1);

    B must(java.lang.String arg0, java.lang.String[] arg1);

    B opt(java.lang.String arg0, java.lang.String arg1);

    B opt(java.lang.String arg0, java.lang.String[] arg1);

    B must(java.lang.String arg0, float arg1);
}

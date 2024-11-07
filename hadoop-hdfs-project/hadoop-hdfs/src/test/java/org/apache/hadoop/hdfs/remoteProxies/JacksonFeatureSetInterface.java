package org.apache.hadoop.hdfs.remoteProxies;

public interface JacksonFeatureSetInterface<F> {
    <F> JacksonFeatureSetInterface<F> fromDefaults(F[] arg0);
    <F> JacksonFeatureSetInterface<F> fromBitmask(int arg0);
    JacksonFeatureSetInterface<F> without(F arg0);
    int asBitmask();
    JacksonFeatureSetInterface<F> with(F arg0);
    boolean isEnabled(F arg0);
}
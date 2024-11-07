package org.apache.hadoop.hdfs.remoteProxies;

public interface OpenFileParametersInterface {
    OpenFileParametersInterface withOptionalKeys(java.util.Set<java.lang.String> arg0);
    OpenFileParametersInterface withMandatoryKeys(java.util.Set<java.lang.String> arg0);
    int getBufferSize();
    java.util.Set<java.lang.String> getMandatoryKeys();
    java.util.Set<java.lang.String> getOptionalKeys();
    OpenFileParametersInterface withOptions(ConfigurationInterface arg0);
    ConfigurationInterface getOptions();
    FileStatusInterface getStatus();
    OpenFileParametersInterface withBufferSize(int arg0);
    OpenFileParametersInterface withStatus(FileStatusInterface arg0);
}
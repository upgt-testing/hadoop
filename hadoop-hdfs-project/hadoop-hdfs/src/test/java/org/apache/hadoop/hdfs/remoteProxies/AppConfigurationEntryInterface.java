package org.apache.hadoop.hdfs.remoteProxies;

public interface AppConfigurationEntryInterface {
    java.util.Map<java.lang.String, ?> getOptions();
    LoginModuleControlFlagInterface getControlFlag();
    java.lang.String getLoginModuleName();
}
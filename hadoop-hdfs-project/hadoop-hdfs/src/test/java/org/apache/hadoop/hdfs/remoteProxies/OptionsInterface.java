package org.apache.hadoop.hdfs.remoteProxies;

public interface OptionsInterface {
    java.util.Map<java.lang.String, java.lang.String> getAttributes();
    OptionsInterface setCipher(java.lang.String arg0);
    OptionsInterface setAttributes(java.util.Map<java.lang.String, java.lang.String> arg0);
    java.lang.String toString();
    OptionsInterface setBitLength(int arg0);
    int getBitLength();
    java.lang.String getCipher();
    java.lang.String getDescription();
    OptionsInterface setDescription(java.lang.String arg0);
}
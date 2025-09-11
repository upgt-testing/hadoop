package org.apache.hadoop.conf;

import org.apache.hadoop.io.WritableJVMInterface;

public interface ConfigurationJVMInterface extends WritableJVMInterface {

    java.lang.Class getClassByNameOrNull(java.lang.String arg0);

    void setTimeDuration(java.lang.String arg0, long arg1, java.util.concurrent.TimeUnit arg2);

    void clear();

    void addResource(java.lang.String arg0);

    long[] getTimeDurations(java.lang.String arg0, java.util.concurrent.TimeUnit arg1);

    java.lang.Class getClassByName(java.lang.String arg0) throws java.lang.ClassNotFoundException;

    org.apache.hadoop.fs.PathJVMInterface getLocalPath(java.lang.String arg0, java.lang.String arg1) throws java.io.IOException;

    void setRestrictSystemProperties(boolean arg0);

    void addResource(java.net.URL arg0, boolean arg1);

    java.lang.Object getRange(java.lang.String arg0, java.lang.String arg1);

    char[] getPassword(java.lang.String arg0) throws java.io.IOException;

    char[] getPasswordFromCredentialProviders(java.lang.String arg0) throws java.io.IOException;

    void addResource(java.lang.String arg0, boolean arg1);

    java.util.Iterator iterator();

    java.lang.String[] getStrings(java.lang.String arg0, java.lang.String[] arg1);

    <T extends java.lang.Enum<T>> T getEnum(java.lang.String arg0, T arg1);

    void unset(java.lang.String arg0);

    java.lang.String getTrimmed(java.lang.String arg0);

    float getFloat(java.lang.String arg0, float arg1);

    int getInt(java.lang.String arg0, int arg1);

    void write_bridge(java.lang.Object arg0) throws java.io.IOException;

    <T extends java.lang.Enum<T>> void setEnum(java.lang.String arg0, T arg1);

    java.util.Map getValByRegex(java.lang.String arg0);

    java.lang.String[] getTrimmedStrings(java.lang.String arg0, java.lang.String[] arg1);

    void setStrings(java.lang.String arg0, java.lang.String[] arg1);

    void setPattern(java.lang.String arg0, java.util.regex.Pattern arg1);

    java.net.InetSocketAddress getSocketAddr(java.lang.String arg0, java.lang.String arg1, int arg2);

    void setDouble(java.lang.String arg0, double arg1);

    java.io.InputStream getConfResourceAsInputStream(java.lang.String arg0);

    void setClass(java.lang.String arg0, java.lang.Class<?> arg1, java.lang.Class<?> arg2);

    void setSocketAddr(java.lang.String arg0, java.net.InetSocketAddress arg1);

    java.lang.Class getClass(java.lang.String arg0, java.lang.Class<?> arg1);

    void addResource_bridge(org.apache.hadoop.fs.PathJVMInterface arg0, boolean arg1);

    void setBoolean(java.lang.String arg0, boolean arg1);

    void setClassLoader(java.lang.ClassLoader arg0);

    void setDeprecatedProperties();

    java.net.URL getResource(java.lang.String arg0);

    java.io.Reader getConfResourceAsReader(java.lang.String arg0);

    java.net.InetSocketAddress getSocketAddr(java.lang.String arg0, java.lang.String arg1, java.lang.String arg2, int arg3);

    void addResource(java.io.InputStream arg0, java.lang.String arg1);

    java.lang.String get(java.lang.String arg0);

    java.io.File getFile(java.lang.String arg0, java.lang.String arg1) throws java.io.IOException;

    void set(java.lang.String arg0, java.lang.String arg1, java.lang.String arg2);

    java.util.Map getPropsWithPrefix(java.lang.String arg0);

    long getLongBytes(java.lang.String arg0, long arg1);

    <U> java.util.List getInstances(java.lang.String arg0, java.lang.Class<U> arg1);

    void readFields_bridge(java.lang.Object arg0) throws java.io.IOException;

    java.lang.String[] getTrimmedStrings(java.lang.String arg0);

    void setInt(java.lang.String arg0, int arg1);

    java.lang.ClassLoader getClassLoader();

    <U> java.lang.Class getClass(java.lang.String arg0, java.lang.Class<? extends U> arg1, java.lang.Class<U> arg2);

    void setAllowNullValueProperties(boolean arg0);

    void writeXml(java.io.Writer arg0) throws java.io.IOException;

    java.lang.String getRaw(java.lang.String arg0);

    boolean onlyKeyExists(java.lang.String arg0);

    void addResource(java.io.InputStream arg0, java.lang.String arg1, boolean arg2);

    java.util.Collection getStringCollection(java.lang.String arg0);

    void setIfUnset(java.lang.String arg0, java.lang.String arg1);

    java.util.Set getFinalParameters();

    void addResource(java.io.InputStream arg0, boolean arg1);

    java.net.InetSocketAddress updateConnectAddr(java.lang.String arg0, java.net.InetSocketAddress arg1);

    java.lang.String[] getPropertySources(java.lang.String arg0);

    void setBooleanIfUnset(java.lang.String arg0, boolean arg1);

    java.lang.String getTrimmed(java.lang.String arg0, java.lang.String arg1);

    java.net.InetSocketAddress updateConnectAddr(java.lang.String arg0, java.lang.String arg1, java.lang.String arg2, java.net.InetSocketAddress arg3);

    double getDouble(java.lang.String arg0, double arg1);

    boolean getBoolean(java.lang.String arg0, boolean arg1);

    java.lang.Class<?>[] getClasses(java.lang.String arg0, java.lang.Class<?>[] arg1);

    java.lang.String toString();

    void setFloat(java.lang.String arg0, float arg1);

    int size();

    void writeXml(java.io.OutputStream arg0) throws java.io.IOException;

    void addResource_bridge(org.apache.hadoop.conf.ConfigurationJVMInterface arg0);

    void addResource(java.net.URL arg0);

    long getTimeDuration(java.lang.String arg0, long arg1, java.util.concurrent.TimeUnit arg2);

    void setRestrictSystemProps(boolean arg0);

    java.lang.String[] getStrings(java.lang.String arg0);

    void setQuietMode(boolean arg0);

    java.util.regex.Pattern getPattern(java.lang.String arg0, java.util.regex.Pattern arg1);

    void setLong(java.lang.String arg0, long arg1);

    java.util.Collection getTrimmedStringCollection(java.lang.String arg0);

    long getLong(java.lang.String arg0, long arg1);

    void addResource_bridge(org.apache.hadoop.fs.PathJVMInterface arg0);

    void addResource(java.io.InputStream arg0);

    java.lang.String get(java.lang.String arg0, java.lang.String arg1);

    void writeXml(java.lang.String arg0, java.io.Writer arg1) throws java.io.IOException, java.lang.IllegalArgumentException;

    void set(java.lang.String arg0, java.lang.String arg1);

    int[] getInts(java.lang.String arg0);

    void reloadConfiguration();
}

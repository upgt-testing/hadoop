package org.apache.hadoop.hdfs.remoteProxies;

public interface FilterMappingInterface {
    javax.servlet.DispatcherType dispatch(int arg0);
    void lambda$named$0(java.lang.String arg0, java.lang.Object arg1, java.lang.Appendable arg2, java.lang.String arg3) throws java.io.IOException;
    boolean appliesTo(javax.servlet.DispatcherType arg0);
    void setServletNames(java.lang.String[] arg0);
    void setServletName(java.lang.String arg0);
    java.util.EnumSet<javax.servlet.DispatcherType> getDispatcherTypes();
    void setFilterHolder(FilterHolderInterface arg0);
    void setPathSpec(java.lang.String arg0);
    java.lang.String[] getPathSpecs();
    boolean appliesTo(int arg0);
    void dumpIterable(java.lang.Appendable arg0, java.lang.String arg1, java.lang.Iterable<?> arg2, boolean arg3) throws java.io.IOException;
    org.eclipse.jetty.util.component.Dumpable named(java.lang.String arg0, java.lang.Object arg1);
    boolean appliesTo(java.lang.String arg0, int arg1);
    java.lang.String toString();
    void dump(java.lang.Appendable arg0, java.lang.String arg1) throws java.io.IOException;
    void setFilterName(java.lang.String arg0);
    java.lang.String dump(org.eclipse.jetty.util.component.Dumpable arg0);
    void setDispatches(int arg0);
    FilterHolderInterface getFilterHolder();
    java.lang.String dump();
    void dumpMapEntries(java.lang.Appendable arg0, java.lang.String arg1, java.util.Map<?, ?> arg2, boolean arg3) throws java.io.IOException;
    void setPathSpecs(java.lang.String[] arg0);
    void dumpObject(java.lang.Appendable arg0, java.lang.Object arg1) throws java.io.IOException;
    void dumpObjects(java.lang.Appendable arg0, java.lang.String arg1, java.lang.Object arg2, java.lang.Object... arg3) throws java.io.IOException;
    java.lang.String dumpSelf();
    java.lang.String[] getServletNames();
    void dumpContainer(java.lang.Appendable arg0, java.lang.String arg1, org.eclipse.jetty.util.component.Container arg2, boolean arg3) throws java.io.IOException;
    javax.servlet.DispatcherType dispatch(java.lang.String arg0);
    boolean isDefaultDispatches();
    java.lang.String getFilterName();
    void setDispatcherTypes(java.util.EnumSet<javax.servlet.DispatcherType> arg0);
    int dispatch(javax.servlet.DispatcherType arg0);
}
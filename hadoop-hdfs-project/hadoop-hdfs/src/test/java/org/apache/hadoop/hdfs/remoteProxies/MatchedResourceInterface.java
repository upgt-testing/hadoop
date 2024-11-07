package org.apache.hadoop.hdfs.remoteProxies;

public interface MatchedResourceInterface<E> {
    <E> MatchedResourceInterface<E> of(java.util.Map.Entry<org.eclipse.jetty.http.pathmap.PathSpec, E> arg0, org.eclipse.jetty.http.pathmap.MatchedPath arg1);
    E getResource();
    java.lang.String getPathMatch();
    org.eclipse.jetty.http.pathmap.PathSpec getPathSpec();
    java.lang.String getPathInfo();
}
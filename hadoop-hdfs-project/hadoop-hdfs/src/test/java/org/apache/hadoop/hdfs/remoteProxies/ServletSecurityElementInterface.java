package org.apache.hadoop.hdfs.remoteProxies;

public interface ServletSecurityElementInterface {
    javax.servlet.annotation.ServletSecurity.EmptyRoleSemantic getEmptyRoleSemantic();
    java.util.Collection<java.lang.String> getMethodNames();
    java.lang.String[] getRolesAllowed();
    java.util.Collection<java.lang.String> checkMethodNames(java.util.Collection<javax.servlet.HttpMethodConstraintElement> arg0);
    java.lang.String[] copyStrings(java.lang.String[] arg0);
    java.util.Collection<javax.servlet.HttpMethodConstraintElement> getHttpMethodConstraints();
    javax.servlet.annotation.ServletSecurity.TransportGuarantee getTransportGuarantee();
}
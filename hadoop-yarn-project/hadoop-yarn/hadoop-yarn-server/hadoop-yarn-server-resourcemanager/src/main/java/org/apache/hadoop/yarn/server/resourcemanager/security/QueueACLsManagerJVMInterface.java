package org.apache.hadoop.yarn.server.resourcemanager.security;

public interface QueueACLsManagerJVMInterface {

    boolean checkAccess_bridge(org.apache.hadoop.security.UserGroupInformationJVMInterface arg0, java.lang.Object arg1, java.lang.Object arg2, java.lang.String arg3, java.util.List<java.lang.String> arg4);

    boolean checkAccess_bridge(org.apache.hadoop.security.UserGroupInformationJVMInterface arg0, java.lang.Object arg1, java.lang.Object arg2, java.lang.String arg3, java.util.List<java.lang.String> arg4, java.lang.String arg5);
}

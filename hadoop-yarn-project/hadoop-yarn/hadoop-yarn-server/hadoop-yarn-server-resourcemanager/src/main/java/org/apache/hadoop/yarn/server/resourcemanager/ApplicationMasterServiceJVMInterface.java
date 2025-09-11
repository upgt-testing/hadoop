package org.apache.hadoop.yarn.server.resourcemanager;

import org.apache.hadoop.service.AbstractServiceJVMInterface;
import org.apache.hadoop.yarn.api.ApplicationMasterProtocolJVMInterface;

public interface ApplicationMasterServiceJVMInterface extends AbstractServiceJVMInterface, ApplicationMasterProtocolJVMInterface {

    java.net.InetSocketAddress getBindAddress();

    org.apache.hadoop.yarn.api.protocolrecords.AllocateResponseJVMInterface allocate_bridge(org.apache.hadoop.yarn.api.protocolrecords.AllocateRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException, java.io.IOException;

    void registerAppAttempt_bridge(org.apache.hadoop.yarn.api.records.ApplicationAttemptIdJVMInterface arg0);

    boolean hasApplicationMasterRegistered_bridge(org.apache.hadoop.yarn.api.records.ApplicationAttemptIdJVMInterface arg0);

    org.apache.hadoop.yarn.api.protocolrecords.RegisterApplicationMasterResponseJVMInterface registerApplicationMaster_bridge(org.apache.hadoop.yarn.api.protocolrecords.RegisterApplicationMasterRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException, java.io.IOException;

    void unregisterAttempt_bridge(org.apache.hadoop.yarn.api.records.ApplicationAttemptIdJVMInterface arg0);

    org.apache.hadoop.ipc.ServerJVMInterface getServer();

    void refreshServiceAcls_bridge(org.apache.hadoop.conf.ConfigurationJVMInterface arg0, org.apache.hadoop.security.authorize.PolicyProviderJVMInterface arg1);

    org.apache.hadoop.yarn.api.protocolrecords.FinishApplicationMasterResponseJVMInterface finishApplicationMaster_bridge(org.apache.hadoop.yarn.api.protocolrecords.FinishApplicationMasterRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException, java.io.IOException;
}

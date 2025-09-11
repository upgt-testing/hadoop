package org.apache.hadoop.yarn.api;

public interface ApplicationMasterProtocolJVMInterface {

    org.apache.hadoop.yarn.api.protocolrecords.AllocateResponseJVMInterface allocate_bridge(org.apache.hadoop.yarn.api.protocolrecords.AllocateRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException, java.io.IOException;

    org.apache.hadoop.yarn.api.protocolrecords.RegisterApplicationMasterResponseJVMInterface registerApplicationMaster_bridge(org.apache.hadoop.yarn.api.protocolrecords.RegisterApplicationMasterRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException, java.io.IOException;

    org.apache.hadoop.yarn.api.protocolrecords.FinishApplicationMasterResponseJVMInterface finishApplicationMaster_bridge(org.apache.hadoop.yarn.api.protocolrecords.FinishApplicationMasterRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException, java.io.IOException;
}

package org.apache.hadoop.yarn.api;

public interface ApplicationBaseProtocolJVMInterface {

    org.apache.hadoop.yarn.api.protocolrecords.GetApplicationReportResponseJVMInterface getApplicationReport_bridge(org.apache.hadoop.yarn.api.protocolrecords.GetApplicationReportRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException, java.io.IOException;

    org.apache.hadoop.yarn.api.protocolrecords.CancelDelegationTokenResponseJVMInterface cancelDelegationToken_bridge(org.apache.hadoop.yarn.api.protocolrecords.CancelDelegationTokenRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException, java.io.IOException;

    org.apache.hadoop.yarn.api.protocolrecords.GetApplicationAttemptReportResponseJVMInterface getApplicationAttemptReport_bridge(org.apache.hadoop.yarn.api.protocolrecords.GetApplicationAttemptReportRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException, java.io.IOException;

    org.apache.hadoop.yarn.api.protocolrecords.GetApplicationsResponseJVMInterface getApplications_bridge(org.apache.hadoop.yarn.api.protocolrecords.GetApplicationsRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException, java.io.IOException;

    org.apache.hadoop.yarn.api.protocolrecords.GetContainersResponseJVMInterface getContainers_bridge(org.apache.hadoop.yarn.api.protocolrecords.GetContainersRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException, java.io.IOException;

    org.apache.hadoop.yarn.api.protocolrecords.RenewDelegationTokenResponseJVMInterface renewDelegationToken_bridge(org.apache.hadoop.yarn.api.protocolrecords.RenewDelegationTokenRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException, java.io.IOException;

    org.apache.hadoop.yarn.api.protocolrecords.GetDelegationTokenResponseJVMInterface getDelegationToken_bridge(org.apache.hadoop.yarn.api.protocolrecords.GetDelegationTokenRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException, java.io.IOException;

    org.apache.hadoop.yarn.api.protocolrecords.GetContainerReportResponseJVMInterface getContainerReport_bridge(org.apache.hadoop.yarn.api.protocolrecords.GetContainerReportRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException, java.io.IOException;

    org.apache.hadoop.yarn.api.protocolrecords.GetApplicationAttemptsResponseJVMInterface getApplicationAttempts_bridge(org.apache.hadoop.yarn.api.protocolrecords.GetApplicationAttemptsRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException, java.io.IOException;
}

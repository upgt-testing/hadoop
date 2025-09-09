package org.apache.hadoop.yarn.server.resourcemanager;

import org.apache.hadoop.service.AbstractServiceJVMInterface;
import org.apache.hadoop.yarn.api.ApplicationClientProtocolJVMInterface;

public interface ClientRMServiceJVMInterface extends AbstractServiceJVMInterface, ApplicationClientProtocolJVMInterface {

    org.apache.hadoop.yarn.api.protocolrecords.GetClusterNodeLabelsResponseJVMInterface getClusterNodeLabels_bridge(org.apache.hadoop.yarn.api.protocolrecords.GetClusterNodeLabelsRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException, java.io.IOException;

    org.apache.hadoop.yarn.api.protocolrecords.ReservationSubmissionResponseJVMInterface submitReservation_bridge(org.apache.hadoop.yarn.api.protocolrecords.ReservationSubmissionRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException, java.io.IOException;

    org.apache.hadoop.yarn.api.protocolrecords.UpdateApplicationPriorityResponseJVMInterface updateApplicationPriority_bridge(org.apache.hadoop.yarn.api.protocolrecords.UpdateApplicationPriorityRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException, java.io.IOException;

    org.apache.hadoop.yarn.api.protocolrecords.GetLabelsToNodesResponseJVMInterface getLabelsToNodes_bridge(org.apache.hadoop.yarn.api.protocolrecords.GetLabelsToNodesRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException, java.io.IOException;

    org.apache.hadoop.yarn.api.protocolrecords.GetAttributesToNodesResponseJVMInterface getAttributesToNodes_bridge(org.apache.hadoop.yarn.api.protocolrecords.GetAttributesToNodesRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException, java.io.IOException;

    org.apache.hadoop.yarn.api.protocolrecords.SignalContainerResponseJVMInterface signalToContainer_bridge(org.apache.hadoop.yarn.api.protocolrecords.SignalContainerRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException, java.io.IOException;

    org.apache.hadoop.yarn.api.protocolrecords.GetApplicationReportResponseJVMInterface getApplicationReport_bridge(org.apache.hadoop.yarn.api.protocolrecords.GetApplicationReportRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException;

    org.apache.hadoop.yarn.api.protocolrecords.ReservationListResponseJVMInterface listReservations_bridge(org.apache.hadoop.yarn.api.protocolrecords.ReservationListRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException, java.io.IOException;

    org.apache.hadoop.yarn.api.protocolrecords.GetAllResourceProfilesResponseJVMInterface getResourceProfiles_bridge(org.apache.hadoop.yarn.api.protocolrecords.GetAllResourceProfilesRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException, java.io.IOException;

    org.apache.hadoop.yarn.api.protocolrecords.GetQueueUserAclsInfoResponseJVMInterface getQueueUserAcls_bridge(org.apache.hadoop.yarn.api.protocolrecords.GetQueueUserAclsInfoRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException;

    org.apache.hadoop.yarn.api.protocolrecords.GetClusterNodesResponseJVMInterface getClusterNodes_bridge(org.apache.hadoop.yarn.api.protocolrecords.GetClusterNodesRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException;

    org.apache.hadoop.yarn.api.protocolrecords.GetQueueInfoResponseJVMInterface getQueueInfo_bridge(org.apache.hadoop.yarn.api.protocolrecords.GetQueueInfoRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException;

    org.apache.hadoop.yarn.api.protocolrecords.GetAllResourceTypeInfoResponseJVMInterface getResourceTypeInfo_bridge(org.apache.hadoop.yarn.api.protocolrecords.GetAllResourceTypeInfoRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException, java.io.IOException;

    org.apache.hadoop.yarn.api.protocolrecords.GetClusterNodeAttributesResponseJVMInterface getClusterNodeAttributes_bridge(org.apache.hadoop.yarn.api.protocolrecords.GetClusterNodeAttributesRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException, java.io.IOException;

    org.apache.hadoop.yarn.api.protocolrecords.GetApplicationAttemptReportResponseJVMInterface getApplicationAttemptReport_bridge(org.apache.hadoop.yarn.api.protocolrecords.GetApplicationAttemptReportRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException, java.io.IOException;

    void setDisplayPerUserApps(boolean arg0);

    org.apache.hadoop.yarn.api.protocolrecords.GetContainersResponseJVMInterface getContainers_bridge(org.apache.hadoop.yarn.api.protocolrecords.GetContainersRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException, java.io.IOException;

    org.apache.hadoop.yarn.api.protocolrecords.RenewDelegationTokenResponseJVMInterface renewDelegationToken_bridge(org.apache.hadoop.yarn.api.protocolrecords.RenewDelegationTokenRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException;

    org.apache.hadoop.yarn.api.protocolrecords.GetNodesToLabelsResponseJVMInterface getNodeToLabels_bridge(org.apache.hadoop.yarn.api.protocolrecords.GetNodesToLabelsRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException, java.io.IOException;

    org.apache.hadoop.yarn.api.protocolrecords.GetClusterMetricsResponseJVMInterface getClusterMetrics_bridge(org.apache.hadoop.yarn.api.protocolrecords.GetClusterMetricsRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException;

    org.apache.hadoop.yarn.api.protocolrecords.MoveApplicationAcrossQueuesResponseJVMInterface moveApplicationAcrossQueues_bridge(org.apache.hadoop.yarn.api.protocolrecords.MoveApplicationAcrossQueuesRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException;

    java.net.InetSocketAddress getBindAddress();

    org.apache.hadoop.yarn.api.protocolrecords.GetNewApplicationResponseJVMInterface getNewApplication_bridge(org.apache.hadoop.yarn.api.protocolrecords.GetNewApplicationRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException;

    org.apache.hadoop.yarn.api.protocolrecords.SubmitApplicationResponseJVMInterface submitApplication_bridge(org.apache.hadoop.yarn.api.protocolrecords.SubmitApplicationRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException, java.io.IOException;

    org.apache.hadoop.yarn.api.protocolrecords.GetNodesToAttributesResponseJVMInterface getNodesToAttributes_bridge(org.apache.hadoop.yarn.api.protocolrecords.GetNodesToAttributesRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException, java.io.IOException;

    org.apache.hadoop.yarn.api.protocolrecords.GetApplicationsResponseJVMInterface getApplications_bridge(org.apache.hadoop.yarn.api.protocolrecords.GetApplicationsRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException;

    org.apache.hadoop.yarn.api.protocolrecords.GetNewReservationResponseJVMInterface getNewReservation_bridge(org.apache.hadoop.yarn.api.protocolrecords.GetNewReservationRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException, java.io.IOException;

    org.apache.hadoop.yarn.api.protocolrecords.FailApplicationAttemptResponseJVMInterface failApplicationAttempt_bridge(org.apache.hadoop.yarn.api.protocolrecords.FailApplicationAttemptRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException;

    org.apache.hadoop.yarn.api.protocolrecords.GetDelegationTokenResponseJVMInterface getDelegationToken_bridge(org.apache.hadoop.yarn.api.protocolrecords.GetDelegationTokenRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException;

    org.apache.hadoop.yarn.api.protocolrecords.GetResourceProfileResponseJVMInterface getResourceProfile_bridge(org.apache.hadoop.yarn.api.protocolrecords.GetResourceProfileRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException, java.io.IOException;

    org.apache.hadoop.yarn.api.protocolrecords.KillApplicationResponseJVMInterface forceKillApplication_bridge(org.apache.hadoop.yarn.api.protocolrecords.KillApplicationRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException;

    org.apache.hadoop.yarn.api.protocolrecords.ReservationDeleteResponseJVMInterface deleteReservation_bridge(org.apache.hadoop.yarn.api.protocolrecords.ReservationDeleteRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException, java.io.IOException;

    org.apache.hadoop.yarn.api.protocolrecords.ReservationUpdateResponseJVMInterface updateReservation_bridge(org.apache.hadoop.yarn.api.protocolrecords.ReservationUpdateRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException, java.io.IOException;

    org.apache.hadoop.yarn.api.protocolrecords.CancelDelegationTokenResponseJVMInterface cancelDelegationToken_bridge(org.apache.hadoop.yarn.api.protocolrecords.CancelDelegationTokenRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException;

    org.apache.hadoop.ipc.ServerJVMInterface getServer();

    org.apache.hadoop.yarn.api.protocolrecords.UpdateApplicationTimeoutsResponseJVMInterface updateApplicationTimeouts_bridge(org.apache.hadoop.yarn.api.protocolrecords.UpdateApplicationTimeoutsRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException, java.io.IOException;

    org.apache.hadoop.yarn.api.protocolrecords.GetApplicationAttemptsResponseJVMInterface getApplicationAttempts_bridge(org.apache.hadoop.yarn.api.protocolrecords.GetApplicationAttemptsRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException, java.io.IOException;

    org.apache.hadoop.yarn.api.protocolrecords.GetContainerReportResponseJVMInterface getContainerReport_bridge(org.apache.hadoop.yarn.api.protocolrecords.GetContainerReportRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException, java.io.IOException;
}

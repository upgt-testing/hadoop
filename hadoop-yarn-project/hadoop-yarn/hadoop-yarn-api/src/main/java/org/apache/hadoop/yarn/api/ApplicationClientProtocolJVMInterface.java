package org.apache.hadoop.yarn.api;

public interface ApplicationClientProtocolJVMInterface extends ApplicationBaseProtocolJVMInterface {

    org.apache.hadoop.yarn.api.protocolrecords.GetClusterNodeLabelsResponseJVMInterface getClusterNodeLabels_bridge(org.apache.hadoop.yarn.api.protocolrecords.GetClusterNodeLabelsRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException, java.io.IOException;

    org.apache.hadoop.yarn.api.protocolrecords.GetNewApplicationResponseJVMInterface getNewApplication_bridge(org.apache.hadoop.yarn.api.protocolrecords.GetNewApplicationRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException, java.io.IOException;

    org.apache.hadoop.yarn.api.protocolrecords.ReservationSubmissionResponseJVMInterface submitReservation_bridge(org.apache.hadoop.yarn.api.protocolrecords.ReservationSubmissionRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException, java.io.IOException;

    org.apache.hadoop.yarn.api.protocolrecords.SubmitApplicationResponseJVMInterface submitApplication_bridge(org.apache.hadoop.yarn.api.protocolrecords.SubmitApplicationRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException, java.io.IOException;

    org.apache.hadoop.yarn.api.protocolrecords.UpdateApplicationPriorityResponseJVMInterface updateApplicationPriority_bridge(org.apache.hadoop.yarn.api.protocolrecords.UpdateApplicationPriorityRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException, java.io.IOException;

    org.apache.hadoop.yarn.api.protocolrecords.GetLabelsToNodesResponseJVMInterface getLabelsToNodes_bridge(org.apache.hadoop.yarn.api.protocolrecords.GetLabelsToNodesRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException, java.io.IOException;

    org.apache.hadoop.yarn.api.protocolrecords.GetNewReservationResponseJVMInterface getNewReservation_bridge(org.apache.hadoop.yarn.api.protocolrecords.GetNewReservationRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException, java.io.IOException;

    org.apache.hadoop.yarn.api.protocolrecords.FailApplicationAttemptResponseJVMInterface failApplicationAttempt_bridge(org.apache.hadoop.yarn.api.protocolrecords.FailApplicationAttemptRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException, java.io.IOException;

    org.apache.hadoop.yarn.api.protocolrecords.SignalContainerResponseJVMInterface signalToContainer_bridge(org.apache.hadoop.yarn.api.protocolrecords.SignalContainerRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException, java.io.IOException;

    org.apache.hadoop.yarn.api.protocolrecords.GetResourceProfileResponseJVMInterface getResourceProfile_bridge(org.apache.hadoop.yarn.api.protocolrecords.GetResourceProfileRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException, java.io.IOException;

    org.apache.hadoop.yarn.api.protocolrecords.KillApplicationResponseJVMInterface forceKillApplication_bridge(org.apache.hadoop.yarn.api.protocolrecords.KillApplicationRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException, java.io.IOException;

    org.apache.hadoop.yarn.api.protocolrecords.ReservationDeleteResponseJVMInterface deleteReservation_bridge(org.apache.hadoop.yarn.api.protocolrecords.ReservationDeleteRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException, java.io.IOException;

    org.apache.hadoop.yarn.api.protocolrecords.ReservationUpdateResponseJVMInterface updateReservation_bridge(org.apache.hadoop.yarn.api.protocolrecords.ReservationUpdateRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException, java.io.IOException;

    org.apache.hadoop.yarn.api.protocolrecords.GetQueueUserAclsInfoResponseJVMInterface getQueueUserAcls_bridge(org.apache.hadoop.yarn.api.protocolrecords.GetQueueUserAclsInfoRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException, java.io.IOException;

    org.apache.hadoop.yarn.api.protocolrecords.GetAllResourceProfilesResponseJVMInterface getResourceProfiles_bridge(org.apache.hadoop.yarn.api.protocolrecords.GetAllResourceProfilesRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException, java.io.IOException;

    org.apache.hadoop.yarn.api.protocolrecords.ReservationListResponseJVMInterface listReservations_bridge(org.apache.hadoop.yarn.api.protocolrecords.ReservationListRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException, java.io.IOException;

    org.apache.hadoop.yarn.api.protocolrecords.GetClusterNodesResponseJVMInterface getClusterNodes_bridge(org.apache.hadoop.yarn.api.protocolrecords.GetClusterNodesRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException, java.io.IOException;

    org.apache.hadoop.yarn.api.protocolrecords.GetQueueInfoResponseJVMInterface getQueueInfo_bridge(org.apache.hadoop.yarn.api.protocolrecords.GetQueueInfoRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException, java.io.IOException;

    org.apache.hadoop.yarn.api.protocolrecords.GetAllResourceTypeInfoResponseJVMInterface getResourceTypeInfo_bridge(org.apache.hadoop.yarn.api.protocolrecords.GetAllResourceTypeInfoRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException, java.io.IOException;

    org.apache.hadoop.yarn.api.protocolrecords.UpdateApplicationTimeoutsResponseJVMInterface updateApplicationTimeouts_bridge(org.apache.hadoop.yarn.api.protocolrecords.UpdateApplicationTimeoutsRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException, java.io.IOException;

    org.apache.hadoop.yarn.api.protocolrecords.GetClusterMetricsResponseJVMInterface getClusterMetrics_bridge(org.apache.hadoop.yarn.api.protocolrecords.GetClusterMetricsRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException, java.io.IOException;

    org.apache.hadoop.yarn.api.protocolrecords.GetNodesToLabelsResponseJVMInterface getNodeToLabels_bridge(org.apache.hadoop.yarn.api.protocolrecords.GetNodesToLabelsRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException, java.io.IOException;

    org.apache.hadoop.yarn.api.protocolrecords.MoveApplicationAcrossQueuesResponseJVMInterface moveApplicationAcrossQueues_bridge(org.apache.hadoop.yarn.api.protocolrecords.MoveApplicationAcrossQueuesRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException, java.io.IOException;
}

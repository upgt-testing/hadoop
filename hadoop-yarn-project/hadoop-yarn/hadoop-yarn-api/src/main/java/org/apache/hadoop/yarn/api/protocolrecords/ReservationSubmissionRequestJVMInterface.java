package org.apache.hadoop.yarn.api.protocolrecords;

public interface ReservationSubmissionRequestJVMInterface {

    org.apache.hadoop.yarn.api.records.ReservationDefinitionJVMInterface getReservationDefinition();

    org.apache.hadoop.yarn.api.records.ReservationIdJVMInterface getReservationId();

    void setQueue(java.lang.String arg0);

    void setReservationId_bridge(org.apache.hadoop.yarn.api.records.ReservationIdJVMInterface arg0);

    java.lang.String getQueue();

    void setReservationDefinition_bridge(org.apache.hadoop.yarn.api.records.ReservationDefinitionJVMInterface arg0);
}

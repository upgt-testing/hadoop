package org.apache.hadoop.yarn.api.protocolrecords;

public interface ReservationUpdateRequestJVMInterface {

    org.apache.hadoop.yarn.api.records.ReservationDefinitionJVMInterface getReservationDefinition();

    org.apache.hadoop.yarn.api.records.ReservationIdJVMInterface getReservationId();

    void setReservationId_bridge(org.apache.hadoop.yarn.api.records.ReservationIdJVMInterface arg0);

    void setReservationDefinition_bridge(org.apache.hadoop.yarn.api.records.ReservationDefinitionJVMInterface arg0);
}

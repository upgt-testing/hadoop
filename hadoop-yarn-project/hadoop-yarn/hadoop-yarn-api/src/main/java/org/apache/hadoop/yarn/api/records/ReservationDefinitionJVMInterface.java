package org.apache.hadoop.yarn.api.records;

public interface ReservationDefinitionJVMInterface {

    void setArrival(long arg0);

    void setPriority_bridge(org.apache.hadoop.yarn.api.records.PriorityJVMInterface arg0);

    void setReservationName(java.lang.String arg0);

    void setRecurrenceExpression(java.lang.String arg0);

    long getArrival();

    long getDeadline();

    java.lang.String getRecurrenceExpression();

    void setDeadline(long arg0);

    java.lang.String getReservationName();

    org.apache.hadoop.yarn.api.records.PriorityJVMInterface getPriority();

    void setReservationRequests_bridge(org.apache.hadoop.yarn.api.records.ReservationRequestsJVMInterface arg0);

    org.apache.hadoop.yarn.api.records.ReservationRequestsJVMInterface getReservationRequests();
}

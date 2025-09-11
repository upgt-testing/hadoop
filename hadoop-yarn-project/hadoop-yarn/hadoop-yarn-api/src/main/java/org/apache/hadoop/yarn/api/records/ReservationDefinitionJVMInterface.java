package org.apache.hadoop.yarn.api.records;

public interface ReservationDefinitionJVMInterface {

    void setPriority_bridge(org.apache.hadoop.yarn.api.records.PriorityJVMInterface arg0);

    void setArrival(long arg0);

    void setReservationName(java.lang.String arg0);

    long getArrival();

    void setRecurrenceExpression(java.lang.String arg0);

    long getDeadline();

    void setDeadline(long arg0);

    java.lang.String getRecurrenceExpression();

    java.lang.String getReservationName();

    org.apache.hadoop.yarn.api.records.PriorityJVMInterface getPriority();

    void setReservationRequests_bridge(org.apache.hadoop.yarn.api.records.ReservationRequestsJVMInterface arg0);

    org.apache.hadoop.yarn.api.records.ReservationRequestsJVMInterface getReservationRequests();
}

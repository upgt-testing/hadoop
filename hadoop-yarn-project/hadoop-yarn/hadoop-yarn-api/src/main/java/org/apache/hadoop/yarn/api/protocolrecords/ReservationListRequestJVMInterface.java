package org.apache.hadoop.yarn.api.protocolrecords;

public interface ReservationListRequestJVMInterface {

    boolean getIncludeResourceAllocations();

    java.lang.String getReservationId();

    long getEndTime();

    long getStartTime();

    void setQueue(java.lang.String arg0);

    void setIncludeResourceAllocations(boolean arg0);

    void setEndTime(long arg0);

    void setReservationId(java.lang.String arg0);

    java.lang.String getQueue();

    void setStartTime(long arg0);
}

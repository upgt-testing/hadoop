package org.apache.hadoop.yarn.api.protocolrecords;

public interface ReservationListRequestJVMInterface {

    boolean getIncludeResourceAllocations();

    long getEndTime();

    java.lang.String getReservationId();

    long getStartTime();

    void setIncludeResourceAllocations(boolean arg0);

    void setQueue(java.lang.String arg0);

    void setEndTime(long arg0);

    void setReservationId(java.lang.String arg0);

    void setStartTime(long arg0);

    java.lang.String getQueue();
}

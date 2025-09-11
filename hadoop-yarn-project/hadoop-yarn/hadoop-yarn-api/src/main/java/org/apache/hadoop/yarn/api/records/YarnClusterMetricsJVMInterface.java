package org.apache.hadoop.yarn.api.records;

public interface YarnClusterMetricsJVMInterface {

    int getNumLostNodeManagers();

    void setNumNodeManagers(int arg0);

    void setNumActiveNodeManagers(int arg0);

    void setNumLostNodeManagers(int arg0);

    void setNumDecommissionedNodeManagers(int arg0);

    int getNumRebootedNodeManagers();

    int getNumUnhealthyNodeManagers();

    int getNumNodeManagers();

    int getNumActiveNodeManagers();

    void setNumUnhealthyNodeManagers(int arg0);

    void setNumRebootedNodeManagers(int arg0);

    int getNumDecommissionedNodeManagers();
}

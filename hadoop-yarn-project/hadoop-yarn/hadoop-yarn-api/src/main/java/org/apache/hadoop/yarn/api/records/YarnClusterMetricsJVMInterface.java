package org.apache.hadoop.yarn.api.records;

public interface YarnClusterMetricsJVMInterface {

    void setNumNodeManagers(int arg0);

    int getNumLostNodeManagers();

    void setNumActiveNodeManagers(int arg0);

    void setNumLostNodeManagers(int arg0);

    void setNumDecommissionedNodeManagers(int arg0);

    int getNumRebootedNodeManagers();

    int getNumUnhealthyNodeManagers();

    int getNumActiveNodeManagers();

    void setNumUnhealthyNodeManagers(int arg0);

    int getNumNodeManagers();

    void setNumRebootedNodeManagers(int arg0);

    int getNumDecommissionedNodeManagers();
}

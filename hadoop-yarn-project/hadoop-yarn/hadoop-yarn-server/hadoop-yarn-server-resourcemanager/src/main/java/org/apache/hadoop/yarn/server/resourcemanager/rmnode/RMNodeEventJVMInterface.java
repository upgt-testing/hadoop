package org.apache.hadoop.yarn.server.resourcemanager.rmnode;

import org.apache.hadoop.yarn.event.AbstractEventJVMInterface;

public interface RMNodeEventJVMInterface extends AbstractEventJVMInterface<org.apache.hadoop.yarn.server.resourcemanager.rmnode.RMNodeEventType> {

    org.apache.hadoop.yarn.api.records.NodeIdJVMInterface getNodeId();
}

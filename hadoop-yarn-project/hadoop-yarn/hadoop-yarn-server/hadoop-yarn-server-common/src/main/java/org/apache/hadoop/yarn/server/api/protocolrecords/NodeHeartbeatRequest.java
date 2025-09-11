/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.yarn.server.api.protocolrecords;

import java.util.List;
import java.util.Set;

import org.apache.hadoop.yarn.api.records.NodeLabel;
import org.apache.hadoop.yarn.server.api.records.MasterKey;
import org.apache.hadoop.yarn.server.api.records.NodeStatus;
import org.apache.hadoop.yarn.util.Records;

public abstract class NodeHeartbeatRequest implements NodeHeartbeatRequestJVMInterface {
  
  public static NodeHeartbeatRequest newInstance(NodeStatus nodeStatus,
      MasterKey lastKnownContainerTokenMasterKey,
      MasterKey lastKnownNMTokenMasterKey, Set<NodeLabel> nodeLabels) {
    NodeHeartbeatRequest nodeHeartbeatRequest =
        Records.newRecord(NodeHeartbeatRequest.class);
    nodeHeartbeatRequest.setNodeStatus(nodeStatus);
    nodeHeartbeatRequest
        .setLastKnownContainerTokenMasterKey(lastKnownContainerTokenMasterKey);
    nodeHeartbeatRequest
        .setLastKnownNMTokenMasterKey(lastKnownNMTokenMasterKey);
    nodeHeartbeatRequest.setNodeLabels(nodeLabels);
    return nodeHeartbeatRequest;
  }

  public abstract NodeStatus getNodeStatus();
  public abstract void setNodeStatus(NodeStatus status);

  public abstract MasterKey getLastKnownContainerTokenMasterKey();
  public abstract void setLastKnownContainerTokenMasterKey(MasterKey secretKey);
  
  public abstract MasterKey getLastKnownNMTokenMasterKey();
  public abstract void setLastKnownNMTokenMasterKey(MasterKey secretKey);
  
  public abstract Set<NodeLabel> getNodeLabels();
  public abstract void setNodeLabels(Set<NodeLabel> nodeLabels);

  public abstract List<LogAggregationReport>
      getLogAggregationReportsForApps();

  public abstract void setLogAggregationReportsForApps(
      List<LogAggregationReport> logAggregationReportsForApps);
  
  public void setNodeStatus_bridge(org.apache.hadoop.yarn.server.api.records.NodeStatusJVMInterface arg0) {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[1];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("setNodeStatus", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("setNodeStatus"))
                      continue;
                  if (m.getParameterCount() != 1)
                      continue;
                  if (m.getReturnType().getName().equals("void"))
                      continue;
                  target = m;
                  break;
              }
          }
          if (target == null)
              throw new RuntimeException("No matching target method found: setNodeStatus");
          target.setAccessible(true);
          target.invoke(this, arg0);
          return;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public void setLastKnownContainerTokenMasterKey_bridge(java.lang.Object arg0) {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[1];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("setLastKnownContainerTokenMasterKey", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("setLastKnownContainerTokenMasterKey"))
                      continue;
                  if (m.getParameterCount() != 1)
                      continue;
                  if (m.getReturnType().getName().equals("void"))
                      continue;
                  target = m;
                  break;
              }
          }
          if (target == null)
              throw new RuntimeException("No matching target method found: setLastKnownContainerTokenMasterKey");
          target.setAccessible(true);
          target.invoke(this, arg0);
          return;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public void setLastKnownNMTokenMasterKey_bridge(java.lang.Object arg0) {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[1];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("setLastKnownNMTokenMasterKey", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("setLastKnownNMTokenMasterKey"))
                      continue;
                  if (m.getParameterCount() != 1)
                      continue;
                  if (m.getReturnType().getName().equals("void"))
                      continue;
                  target = m;
                  break;
              }
          }
          if (target == null)
              throw new RuntimeException("No matching target method found: setLastKnownNMTokenMasterKey");
          target.setAccessible(true);
          target.invoke(this, arg0);
          return;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
}

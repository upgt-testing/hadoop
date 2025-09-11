/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership.  The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.apache.hadoop.yarn.server.federation.store.records;

import org.apache.hadoop.classification.InterfaceAudience.Private;
import org.apache.hadoop.classification.InterfaceAudience.Public;
import org.apache.hadoop.classification.InterfaceStability.Unstable;
import org.apache.hadoop.yarn.util.Records;

/**
 * <p>
 * SubClusterRegisterRequest is a request by a sub-cluster
 * {@code ResourceManager} to participate in federation.
 *
 * <p>
 * It includes information such as:
 * <ul>
 * <li>{@link SubClusterId}</li>
 * <li>The URL of the subcluster</li>
 * <li>The timestamp representing the last start time of the subCluster</li>
 * <li>{@code FederationsubClusterState}</li>
 * <li>The current capacity and utilization of the subCluster</li>
 * </ul>
 */
@Private
@Unstable
public abstract class SubClusterRegisterRequest implements SubClusterRegisterRequestJVMInterface {

  @Private
  @Unstable
  public static SubClusterRegisterRequest newInstance(
      SubClusterInfo subClusterInfo) {
    SubClusterRegisterRequest registerSubClusterRequest =
        Records.newRecord(SubClusterRegisterRequest.class);
    registerSubClusterRequest.setSubClusterInfo(subClusterInfo);
    return registerSubClusterRequest;
  }

  /**
   * Get the {@link SubClusterInfo} encapsulating the information about the
   * sub-cluster.
   *
   * @return the information pertaining to the sub-cluster
   */
  @Public
  @Unstable
  public abstract SubClusterInfo getSubClusterInfo();

  /**
   * Set the {@link SubClusterInfo} encapsulating the information about the
   * sub-cluster.
   *
   * @param subClusterInfo the information pertaining to the sub-cluster
   */
  @Public
  @Unstable
  public abstract void setSubClusterInfo(SubClusterInfo subClusterInfo);
  
  public void setSubClusterInfo_bridge(org.apache.hadoop.yarn.server.federation.store.records.SubClusterInfoJVMInterface arg0) {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[1];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("setSubClusterInfo", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("setSubClusterInfo"))
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
              throw new RuntimeException("No matching target method found: setSubClusterInfo");
          target.setAccessible(true);
          target.invoke(this, arg0);
          return;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }

}

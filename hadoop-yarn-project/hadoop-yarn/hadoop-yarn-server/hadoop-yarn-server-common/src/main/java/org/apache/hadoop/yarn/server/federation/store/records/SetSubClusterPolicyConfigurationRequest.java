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
 * SetSubClusterPolicyConfigurationRequest is a request to the
 * {@code FederationPolicyStore} to set the policy configuration corresponding
 * to a queue.
 */
@Private
@Unstable
public abstract class SetSubClusterPolicyConfigurationRequest implements SetSubClusterPolicyConfigurationRequestJVMInterface {
  @Private
  @Unstable
  public static SetSubClusterPolicyConfigurationRequest newInstance(
      SubClusterPolicyConfiguration policy) {
    SetSubClusterPolicyConfigurationRequest request =
        Records.newRecord(SetSubClusterPolicyConfigurationRequest.class);
    request.setPolicyConfiguration(policy);
    return request;
  }

  /**
   * Get the policy configuration assigned to the queue.
   *
   * @return the policy for the specified queue
   */
  @Public
  @Unstable
  public abstract SubClusterPolicyConfiguration getPolicyConfiguration();

  /**
   * Set the policyConfiguration configuration for the queue.
   *
   * @param policyConfiguration the policyConfiguration for the specified queue
   */
  @Private
  @Unstable
  public abstract void setPolicyConfiguration(
      SubClusterPolicyConfiguration policyConfiguration);
  
  public void setPolicyConfiguration_bridge(org.apache.hadoop.yarn.server.federation.store.records.SubClusterPolicyConfigurationJVMInterface arg0) {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[1];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("setPolicyConfiguration", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("setPolicyConfiguration"))
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
              throw new RuntimeException("No matching target method found: setPolicyConfiguration");
          target.setAccessible(true);
          target.invoke(this, arg0);
          return;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
}

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

package org.apache.hadoop.yarn.api.protocolrecords;

import org.apache.hadoop.classification.InterfaceAudience.Public;
import org.apache.hadoop.classification.InterfaceStability.Unstable;
import org.apache.hadoop.yarn.api.records.ContainerId;
import org.apache.hadoop.yarn.api.records.ContainerReport;
import org.apache.hadoop.yarn.util.Records;

/**
 * <p>
 * The request sent by a client to the <code>ResourceManager</code> to get an
 * {@link ContainerReport} for a container.
 * </p>
 */
@Public
@Unstable
public abstract class GetContainerReportRequest implements GetContainerReportRequestJVMInterface {

  @Public
  @Unstable
  public static GetContainerReportRequest newInstance(ContainerId containerId) {
    GetContainerReportRequest request =
        Records.newRecord(GetContainerReportRequest.class);
    request.setContainerId(containerId);
    return request;
  }

  /**
   * Get the <code>ContainerId</code> of the Container.
   * 
   * @return <code>ContainerId</code> of the Container
   */
  @Public
  @Unstable
  public abstract ContainerId getContainerId();

  /**
   * Set the <code>ContainerId</code> of the container
   * 
   * @param containerId
   *          <code>ContainerId</code> of the container
   */
  @Public
  @Unstable
  public abstract void setContainerId(ContainerId containerId);
  
  public void setContainerId_bridge(org.apache.hadoop.yarn.api.records.ContainerIdJVMInterface arg0) {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[1];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("setContainerId", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("setContainerId"))
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
              throw new RuntimeException("No matching target method found: setContainerId");
          target.setAccessible(true);
          target.invoke(this, arg0);
          return;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
}

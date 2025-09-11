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
import org.apache.hadoop.classification.InterfaceStability.Stable;
import org.apache.hadoop.yarn.api.ContainerManagementProtocol;
import org.apache.hadoop.yarn.api.records.ContainerLaunchContext;
import org.apache.hadoop.yarn.api.records.NMToken;
import org.apache.hadoop.yarn.api.records.Token;
import org.apache.hadoop.yarn.util.Records;

/**
 * <p>The request sent by the <code>ApplicationMaster</code> to the
 * <code>NodeManager</code> to <em>start</em> a container.</p>
 * 
 * <p>The <code>ApplicationMaster</code> has to provide details such as
 * allocated resource capability, security tokens (if enabled), command
 * to be executed to start the container, environment for the process, 
 * necessary binaries/jar/shared-objects etc. via the 
 * {@link ContainerLaunchContext}.</p>
 *
 * @see ContainerManagementProtocol#startContainers(StartContainersRequest)
 */
@Public
@Stable
public abstract class StartContainerRequest implements StartContainerRequestJVMInterface {
  @Public
  @Stable
  public static StartContainerRequest newInstance(
      ContainerLaunchContext context, Token container) {
    StartContainerRequest request =
        Records.newRecord(StartContainerRequest.class);
    request.setContainerLaunchContext(context);
    request.setContainerToken(container);
    return request;
  }

  /**
   * Get the <code>ContainerLaunchContext</code> for the container to be started
   * by the <code>NodeManager</code>.
   * 
   * @return <code>ContainerLaunchContext</code> for the container to be started
   *         by the <code>NodeManager</code>
   */
  @Public
  @Stable
  public abstract ContainerLaunchContext getContainerLaunchContext();
  
  /**
   * Set the <code>ContainerLaunchContext</code> for the container to be started
   * by the <code>NodeManager</code>
   * @param context <code>ContainerLaunchContext</code> for the container to be 
   *                started by the <code>NodeManager</code>
   */
  @Public
  @Stable
  public abstract void setContainerLaunchContext(ContainerLaunchContext context);

  /**
   * Get the container token to be used for authorization during starting
   * container.
   * <p>
   * Note: {@link NMToken} will be used for authenticating communication with
   * {@code NodeManager}.
   * @return the container token to be used for authorization during starting
   * container.
   * @see NMToken
   * @see ContainerManagementProtocol#startContainers(StartContainersRequest)
   */
  @Public
  @Stable
  public abstract Token getContainerToken();

  @Public
  @Stable
  public abstract void setContainerToken(Token container);
  
  public void setContainerToken_bridge(org.apache.hadoop.yarn.api.records.TokenJVMInterface arg0) {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[1];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("setContainerToken", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("setContainerToken"))
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
              throw new RuntimeException("No matching target method found: setContainerToken");
          target.setAccessible(true);
          target.invoke(this, arg0);
          return;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public void setContainerLaunchContext_bridge(org.apache.hadoop.yarn.api.records.ContainerLaunchContextJVMInterface arg0) {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[1];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("setContainerLaunchContext", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("setContainerLaunchContext"))
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
              throw new RuntimeException("No matching target method found: setContainerLaunchContext");
          target.setAccessible(true);
          target.invoke(this, arg0);
          return;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
}

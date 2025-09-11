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
import org.apache.hadoop.yarn.api.ApplicationClientProtocol;
import org.apache.hadoop.yarn.api.records.Priority;
import org.apache.hadoop.yarn.util.Records;

/**
 * <p>
 * The response sent by the <code>ResourceManager</code> to the client on update
 * the application priority.
 * </p>
 * <p>
 * A response without exception means that the move has completed successfully.
 * </p>
 * 
 * @see ApplicationClientProtocol#updateApplicationPriority(UpdateApplicationPriorityRequest)
 */

@Public
@Unstable
public abstract class UpdateApplicationPriorityResponse implements UpdateApplicationPriorityResponseJVMInterface {

  public static UpdateApplicationPriorityResponse newInstance(
      Priority priority) {
    UpdateApplicationPriorityResponse response =
        Records.newRecord(UpdateApplicationPriorityResponse.class);
    response.setApplicationPriority(priority);
    return response;
  }

  /**
   * Get the <code>Priority</code> of the application to be set.
   * @return Updated <code>Priority</code> of the application.
   */
  public abstract Priority getApplicationPriority();

  /**
   * Set the <code>Priority</code> of the application.
   *
   * @param priority <code>Priority</code> of the application
   */
  public abstract void setApplicationPriority(Priority priority);
  
  public void setApplicationPriority_bridge(org.apache.hadoop.yarn.api.records.PriorityJVMInterface arg0) {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[1];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("setApplicationPriority", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("setApplicationPriority"))
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
              throw new RuntimeException("No matching target method found: setApplicationPriority");
          target.setAccessible(true);
          target.invoke(this, arg0);
          return;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
}

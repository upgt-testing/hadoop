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
import org.apache.hadoop.yarn.api.ApplicationHistoryProtocol;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.util.Records;

/**
 * <p>
 * The request from clients to get a list of application attempt reports of an
 * application from the <code>ResourceManager</code>.
 * </p>
 * 
 * @see ApplicationHistoryProtocol#getApplicationAttempts(GetApplicationAttemptsRequest)
 */
@Public
@Unstable
public abstract class GetApplicationAttemptsRequest implements GetApplicationAttemptsRequestJVMInterface {

  @Public
  @Unstable
  public static GetApplicationAttemptsRequest newInstance(
      ApplicationId applicationId) {
    GetApplicationAttemptsRequest request =
        Records.newRecord(GetApplicationAttemptsRequest.class);
    request.setApplicationId(applicationId);
    return request;
  }

  /**
   * Get the <code>ApplicationId</code> of an application
   * 
   * @return <code>ApplicationId</code> of an application
   */
  @Public
  @Unstable
  public abstract ApplicationId getApplicationId();

  /**
   * Set the <code>ApplicationId</code> of an application
   * 
   * @param applicationId
   *          <code>ApplicationId</code> of an application
   */
  @Public
  @Unstable
  public abstract void setApplicationId(ApplicationId applicationId);
  
  public void setApplicationId_bridge(org.apache.hadoop.yarn.api.records.ApplicationIdJVMInterface arg0) {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[1];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("setApplicationId", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("setApplicationId"))
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
              throw new RuntimeException("No matching target method found: setApplicationId");
          target.setAccessible(true);
          target.invoke(this, arg0);
          return;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
}

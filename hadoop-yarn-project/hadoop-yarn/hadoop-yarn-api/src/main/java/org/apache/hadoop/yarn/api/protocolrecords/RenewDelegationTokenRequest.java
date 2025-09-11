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

import org.apache.hadoop.classification.InterfaceAudience.Private;
import org.apache.hadoop.classification.InterfaceStability.Unstable;
import org.apache.hadoop.yarn.api.records.Token;
import org.apache.hadoop.yarn.util.Records;

/**
 * The request issued by the client to renew a delegation token from
 * the {@code ResourceManager}.
 */
@Private
@Unstable
public abstract class RenewDelegationTokenRequest implements RenewDelegationTokenRequestJVMInterface {
  @Private
  @Unstable
  public static RenewDelegationTokenRequest newInstance(Token dToken) {
    RenewDelegationTokenRequest request =
        Records.newRecord(RenewDelegationTokenRequest.class);
    request.setDelegationToken(dToken);
    return request;
  }

  /**
   * Get the delegation token requested to be renewed by the client.
   * @return the delegation token requested to be renewed by the client.
   */
  @Private
  @Unstable
  public abstract Token getDelegationToken();

  @Private
  @Unstable
  public abstract void setDelegationToken(Token dToken);
  
  public void setDelegationToken_bridge(org.apache.hadoop.yarn.api.records.TokenJVMInterface arg0) {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[1];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("setDelegationToken", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("setDelegationToken"))
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
              throw new RuntimeException("No matching target method found: setDelegationToken");
          target.setAccessible(true);
          target.invoke(this, arg0);
          return;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
}

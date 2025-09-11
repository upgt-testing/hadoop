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
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.util.Records;

/**
 * <p>The request sent by the client to the <code>ResourceManager</code>
 * to move a submitted application to a different queue.</p>
 * 
 * <p>The request includes the {@link ApplicationId} of the application to be
 * moved and the queue to place it in.</p>
 * 
 * @see ApplicationClientProtocol#moveApplicationAcrossQueues(MoveApplicationAcrossQueuesRequest)
 */
@Public
@Unstable
public abstract class MoveApplicationAcrossQueuesRequest implements MoveApplicationAcrossQueuesRequestJVMInterface {
  public static MoveApplicationAcrossQueuesRequest newInstance(ApplicationId appId, String queue) {
    MoveApplicationAcrossQueuesRequest request =
        Records.newRecord(MoveApplicationAcrossQueuesRequest.class);
    request.setApplicationId(appId);
    request.setTargetQueue(queue);
    return request;
  }
  
  /**
   * Get the <code>ApplicationId</code> of the application to be moved.
   * @return <code>ApplicationId</code> of the application to be moved
   */
  public abstract ApplicationId getApplicationId();
  
  /**
   * Set the <code>ApplicationId</code> of the application to be moved.
   * @param appId <code>ApplicationId</code> of the application to be moved
   */
  public abstract void setApplicationId(ApplicationId appId);
  
  /**
   * Get the queue to place the application in.
   * @return the name of the queue to place the application in
   */
  public abstract String getTargetQueue();

  /**
   * Get the queue to place the application in.
   * @param queue the name of the queue to place the application in
   */
  public abstract void setTargetQueue(String queue);
  
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

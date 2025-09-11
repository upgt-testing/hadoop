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
import org.apache.hadoop.classification.InterfaceAudience.Public;
import org.apache.hadoop.classification.InterfaceStability.Stable;
import org.apache.hadoop.classification.InterfaceStability.Unstable;
import org.apache.hadoop.yarn.api.ApplicationClientProtocol;
import org.apache.hadoop.yarn.api.records.QueueInfo;
import org.apache.hadoop.yarn.util.Records;

/**
 * The response sent by the {@code ResourceManager} to a client
 * requesting information about queues in the system.
 * <p>
 * The response includes a {@link QueueInfo} which has details such as
 * queue name, used/total capacities, running applications, child queues etc.
 * 
 * @see QueueInfo
 * @see ApplicationClientProtocol#getQueueInfo(GetQueueInfoRequest)
 */
@Public
@Stable
public abstract class GetQueueInfoResponse implements GetQueueInfoResponseJVMInterface {

  @Private
  @Unstable
  public static GetQueueInfoResponse newInstance(QueueInfo queueInfo) {
    GetQueueInfoResponse response = Records.newRecord(GetQueueInfoResponse.class);
    response.setQueueInfo(queueInfo);
    return response;
  }

  /**
   * Get the <code>QueueInfo</code> for the specified queue.
   * @return <code>QueueInfo</code> for the specified queue
   */
  @Public
  @Stable
  public abstract QueueInfo getQueueInfo();
  
  @Private
  @Unstable
  public abstract void setQueueInfo(QueueInfo queueInfo);
  
  public void setQueueInfo_bridge(org.apache.hadoop.yarn.api.records.QueueInfoJVMInterface arg0) {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[1];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("setQueueInfo", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("setQueueInfo"))
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
              throw new RuntimeException("No matching target method found: setQueueInfo");
          target.setAccessible(true);
          target.invoke(this, arg0);
          return;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
}

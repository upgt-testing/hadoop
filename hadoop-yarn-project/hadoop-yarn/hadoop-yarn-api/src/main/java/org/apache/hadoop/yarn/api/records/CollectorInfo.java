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

package org.apache.hadoop.yarn.api.records;

import org.apache.hadoop.classification.InterfaceStability.Evolving;
import org.apache.hadoop.classification.InterfaceAudience.Public;
import org.apache.hadoop.yarn.util.Records;

/**
 * Collector info containing collector address and collector token passed from
 * RM to AM in Allocate Response.
 */
@Public
@Evolving
public abstract class CollectorInfo implements CollectorInfoJVMInterface {

  protected static final long DEFAULT_TIMESTAMP_VALUE = -1;

  public static CollectorInfo newInstance(String collectorAddr) {
    return newInstance(collectorAddr, null);
  }

  public static CollectorInfo newInstance(String collectorAddr, Token token) {
    CollectorInfo amCollectorInfo =
        Records.newRecord(CollectorInfo.class);
    amCollectorInfo.setCollectorAddr(collectorAddr);
    amCollectorInfo.setCollectorToken(token);
    return amCollectorInfo;
  }

  public abstract String getCollectorAddr();

  public abstract void setCollectorAddr(String addr);

  /**
   * Get delegation token for app collector which AM will use to publish
   * entities.
   * @return the delegation token for app collector.
   */
  public abstract Token getCollectorToken();

  public abstract void setCollectorToken(Token token);
  
  public void setCollectorToken_bridge(org.apache.hadoop.yarn.api.records.TokenJVMInterface arg0) {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[1];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("setCollectorToken", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("setCollectorToken"))
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
              throw new RuntimeException("No matching target method found: setCollectorToken");
          target.setAccessible(true);
          target.invoke(this, arg0);
          return;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
}

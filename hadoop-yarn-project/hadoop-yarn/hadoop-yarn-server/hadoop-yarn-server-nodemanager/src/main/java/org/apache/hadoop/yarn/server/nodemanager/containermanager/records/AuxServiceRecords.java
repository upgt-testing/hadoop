/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.yarn.server.nodemanager.containermanager.records;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.hadoop.classification.InterfaceAudience;
import org.apache.hadoop.classification.InterfaceStability;

import java.util.ArrayList;
import java.util.List;

/**
 * A list of Services.
 **/
@InterfaceAudience.Public
@InterfaceStability.Unstable
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuxServiceRecords implements AuxServiceRecordsJVMInterface {
  private List<AuxServiceRecord> services = new ArrayList<>();

  public AuxServiceRecords serviceList(AuxServiceRecord... serviceList) {
    for (AuxServiceRecord service : serviceList) {
      this.services.add(service);
    }
    return this;
  }

  public List<AuxServiceRecord> getServices() {
    return services;
  }
  
  public org.apache.hadoop.yarn.server.nodemanager.containermanager.records.AuxServiceRecordsJVMInterface serviceList_bridge(org.apache.hadoop.yarn.server.nodemanager.containermanager.records.AuxServiceRecordJVMInterface[] arg0) {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[1];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("serviceList", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("serviceList"))
                      continue;
                  if (m.getParameterCount() != 1)
                      continue;
                  if (m.getReturnType().getName().equals("org.apache.hadoop.yarn.server.nodemanager.containermanager.records.AuxServiceRecordsJVMInterface"))
                      continue;
                  target = m;
                  break;
              }
          }
          if (target == null)
              throw new RuntimeException("No matching target method found: serviceList");
          target.setAccessible(true);
          org.apache.hadoop.yarn.server.nodemanager.containermanager.records.AuxServiceRecordsJVMInterface __result = (org.apache.hadoop.yarn.server.nodemanager.containermanager.records.AuxServiceRecordsJVMInterface) target.invoke(this, arg0);
          return __result;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
}

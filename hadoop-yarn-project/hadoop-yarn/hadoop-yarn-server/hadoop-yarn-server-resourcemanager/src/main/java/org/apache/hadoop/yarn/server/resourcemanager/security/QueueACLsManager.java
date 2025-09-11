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

package org.apache.hadoop.yarn.server.resourcemanager.security;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.yarn.api.records.QueueACL;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.server.resourcemanager.scheduler.ResourceScheduler;

import com.google.common.annotations.VisibleForTesting;

public class QueueACLsManager implements QueueACLsManagerJVMInterface {
  private ResourceScheduler scheduler;
  private boolean isACLsEnable;
  
  @VisibleForTesting
  public QueueACLsManager() {
    this(null, new Configuration());
  }

  public QueueACLsManager(ResourceScheduler scheduler, Configuration conf) {
    this.scheduler = scheduler;
    this.isACLsEnable = conf.getBoolean(YarnConfiguration.YARN_ACL_ENABLE,
        YarnConfiguration.DEFAULT_YARN_ACL_ENABLE);
  }

  public boolean checkAccess(UserGroupInformation callerUGI,
      QueueACL acl, String queueName) {
    if (!isACLsEnable) {
      return true;
    }
    return scheduler.checkAccess(callerUGI, acl, queueName);
  }
  
  public boolean checkAccess_bridge(org.apache.hadoop.security.UserGroupInformationJVMInterface arg0, java.lang.Object arg1, java.lang.String arg2) {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[3];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              __types[1] = (arg1 != null ? arg1.getClass() : Object.class);
              __types[2] = (arg2 != null ? arg2.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("checkAccess", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("checkAccess"))
                      continue;
                  if (m.getParameterCount() != 3)
                      continue;
                  if (m.getReturnType().getName().equals("boolean"))
                      continue;
                  target = m;
                  break;
              }
          }
          if (target == null)
              throw new RuntimeException("No matching target method found: checkAccess");
          target.setAccessible(true);
          boolean __result = (boolean) target.invoke(this, arg0, arg1, arg2);
          return __result;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
}

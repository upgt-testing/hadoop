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
package org.apache.hadoop.yarn.server.security;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.classification.InterfaceAudience;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.security.AccessControlException;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.security.authorize.AccessControlList;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.api.records.ApplicationAccessType;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.security.AdminACLsManager;

import com.google.common.annotations.VisibleForTesting;

@InterfaceAudience.Private
public class ApplicationACLsManager implements ApplicationACLsManagerJVMInterface {

  private static final Log LOG = LogFactory
      .getLog(ApplicationACLsManager.class);

  private static AccessControlList DEFAULT_YARN_APP_ACL 
    = new AccessControlList(YarnConfiguration.DEFAULT_YARN_APP_ACL);
  private final Configuration conf;
  private final AdminACLsManager adminAclsManager;
  private final ConcurrentMap<ApplicationId, Map<ApplicationAccessType, AccessControlList>> applicationACLS
    = new ConcurrentHashMap<ApplicationId, Map<ApplicationAccessType, AccessControlList>>();

  @VisibleForTesting
  public ApplicationACLsManager() {
    this(new Configuration());
  }
  
  public ApplicationACLsManager(Configuration conf) {
    this.conf = conf;
    this.adminAclsManager = new AdminACLsManager(this.conf);
  }

  public boolean areACLsEnabled() {
    return adminAclsManager.areACLsEnabled();
  }

  public void addApplication(ApplicationId appId,
      Map<ApplicationAccessType, String> acls) {
    Map<ApplicationAccessType, AccessControlList> finalMap
        = new HashMap<ApplicationAccessType, AccessControlList>(acls.size());
    for (Entry<ApplicationAccessType, String> acl : acls.entrySet()) {
      finalMap.put(acl.getKey(), new AccessControlList(acl.getValue()));
    }
    this.applicationACLS.put(appId, finalMap);
  }

  public void removeApplication(ApplicationId appId) {
    this.applicationACLS.remove(appId);
  }

  /**
   * If authorization is enabled, checks whether the user (in the callerUGI) is
   * authorized to perform the access specified by 'applicationAccessType' on
   * the application by checking if the user is applicationOwner or part of
   * application ACL for the specific access-type.
   * <ul>
   * <li>The owner of the application can have all access-types on the
   * application</li>
   * <li>For all other users/groups application-acls are checked</li>
   * </ul>
   * 
   * @param callerUGI
   * @param applicationAccessType
   * @param applicationOwner
   * @param applicationId
   */
  public boolean checkAccess(UserGroupInformation callerUGI,
      ApplicationAccessType applicationAccessType, String applicationOwner,
      ApplicationId applicationId) {

    if (LOG.isDebugEnabled()) {
      LOG.debug("Verifying access-type " + applicationAccessType + " for "
          + callerUGI + " on application " + applicationId + " owned by "
          + applicationOwner);
    }

    String user = callerUGI.getShortUserName();
    if (!areACLsEnabled()) {
      return true;
    }
    AccessControlList applicationACL = DEFAULT_YARN_APP_ACL;
    Map<ApplicationAccessType, AccessControlList> acls = this.applicationACLS
        .get(applicationId);
    if (acls == null) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("ACL not found for application "
            + applicationId + " owned by "
            + applicationOwner + ". Using default ["
            + YarnConfiguration.DEFAULT_YARN_APP_ACL + "]");
      }
    } else {
      AccessControlList applicationACLInMap = acls.get(applicationAccessType);
      if (applicationACLInMap != null) {
        applicationACL = applicationACLInMap;
      } else if (LOG.isDebugEnabled()) {
        LOG.debug("ACL not found for access-type " + applicationAccessType
            + " for application " + applicationId + " owned by "
            + applicationOwner + ". Using default ["
            + YarnConfiguration.DEFAULT_YARN_APP_ACL + "]");
      }
    }

    // Allow application-owner for any type of access on the application
    if (this.adminAclsManager.isAdmin(callerUGI)
        || user.equals(applicationOwner)
        || applicationACL.isUserAllowed(callerUGI)) {
      return true;
    }
    return false;
  }

  /**
   * Check if the given user in an admin.
   *
   * @param calledUGI
   *          UserGroupInformation for the user
   * @return true if the user is an admin, false otherwise
   */
  public final boolean isAdmin(final UserGroupInformation calledUGI) {
    return this.adminAclsManager.isAdmin(calledUGI);
  }
  
  public void addApplication_bridge(org.apache.hadoop.yarn.api.records.ApplicationIdJVMInterface arg0, java.util.Map<org.apache.hadoop.yarn.api.records.ApplicationAccessType, java.lang.String> arg1) {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[2];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              __types[1] = (arg1 != null ? arg1.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("addApplication", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("addApplication"))
                      continue;
                  if (m.getParameterCount() != 2)
                      continue;
                  if (m.getReturnType().getName().equals("void"))
                      continue;
                  target = m;
                  break;
              }
          }
          if (target == null)
              throw new RuntimeException("No matching target method found: addApplication");
          target.setAccessible(true);
          target.invoke(this, arg0, arg1);
          return;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public boolean isAdmin_bridge(org.apache.hadoop.security.UserGroupInformationJVMInterface arg0) {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[1];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("isAdmin", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("isAdmin"))
                      continue;
                  if (m.getParameterCount() != 1)
                      continue;
                  if (m.getReturnType().getName().equals("boolean"))
                      continue;
                  target = m;
                  break;
              }
          }
          if (target == null)
              throw new RuntimeException("No matching target method found: isAdmin");
          target.setAccessible(true);
          boolean __result = (boolean) target.invoke(this, arg0);
          return __result;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public void removeApplication_bridge(org.apache.hadoop.yarn.api.records.ApplicationIdJVMInterface arg0) {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[1];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("removeApplication", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("removeApplication"))
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
              throw new RuntimeException("No matching target method found: removeApplication");
          target.setAccessible(true);
          target.invoke(this, arg0);
          return;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public boolean checkAccess_bridge(org.apache.hadoop.security.UserGroupInformationJVMInterface arg0, java.lang.Object arg1, java.lang.String arg2, org.apache.hadoop.yarn.api.records.ApplicationIdJVMInterface arg3) {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[4];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              __types[1] = (arg1 != null ? arg1.getClass() : Object.class);
              __types[2] = (arg2 != null ? arg2.getClass() : Object.class);
              __types[3] = (arg3 != null ? arg3.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("checkAccess", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("checkAccess"))
                      continue;
                  if (m.getParameterCount() != 4)
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
          boolean __result = (boolean) target.invoke(this, arg0, arg1, arg2, arg3);
          return __result;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
}

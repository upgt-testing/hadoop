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

package org.apache.hadoop.yarn.server.resourcemanager.federation;

import java.net.InetSocketAddress;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.retry.RetryPolicy;
import org.apache.hadoop.net.NetUtils;
import org.apache.hadoop.service.AbstractService;
import org.apache.hadoop.util.concurrent.HadoopExecutors;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.apache.hadoop.yarn.exceptions.YarnRuntimeException;
import org.apache.hadoop.yarn.server.federation.store.FederationStateStore;
import org.apache.hadoop.yarn.server.federation.store.records.AddApplicationHomeSubClusterRequest;
import org.apache.hadoop.yarn.server.federation.store.records.AddApplicationHomeSubClusterResponse;
import org.apache.hadoop.yarn.server.federation.store.records.DeleteApplicationHomeSubClusterRequest;
import org.apache.hadoop.yarn.server.federation.store.records.DeleteApplicationHomeSubClusterResponse;
import org.apache.hadoop.yarn.server.federation.store.records.GetApplicationHomeSubClusterRequest;
import org.apache.hadoop.yarn.server.federation.store.records.GetApplicationHomeSubClusterResponse;
import org.apache.hadoop.yarn.server.federation.store.records.GetApplicationsHomeSubClusterRequest;
import org.apache.hadoop.yarn.server.federation.store.records.GetApplicationsHomeSubClusterResponse;
import org.apache.hadoop.yarn.server.federation.store.records.GetSubClusterInfoRequest;
import org.apache.hadoop.yarn.server.federation.store.records.GetSubClusterInfoResponse;
import org.apache.hadoop.yarn.server.federation.store.records.GetSubClusterPoliciesConfigurationsRequest;
import org.apache.hadoop.yarn.server.federation.store.records.GetSubClusterPoliciesConfigurationsResponse;
import org.apache.hadoop.yarn.server.federation.store.records.GetSubClusterPolicyConfigurationRequest;
import org.apache.hadoop.yarn.server.federation.store.records.GetSubClusterPolicyConfigurationResponse;
import org.apache.hadoop.yarn.server.federation.store.records.GetSubClustersInfoRequest;
import org.apache.hadoop.yarn.server.federation.store.records.GetSubClustersInfoResponse;
import org.apache.hadoop.yarn.server.federation.store.records.SetSubClusterPolicyConfigurationRequest;
import org.apache.hadoop.yarn.server.federation.store.records.SetSubClusterPolicyConfigurationResponse;
import org.apache.hadoop.yarn.server.federation.store.records.SubClusterDeregisterRequest;
import org.apache.hadoop.yarn.server.federation.store.records.SubClusterDeregisterResponse;
import org.apache.hadoop.yarn.server.federation.store.records.SubClusterHeartbeatRequest;
import org.apache.hadoop.yarn.server.federation.store.records.SubClusterHeartbeatResponse;
import org.apache.hadoop.yarn.server.federation.store.records.SubClusterId;
import org.apache.hadoop.yarn.server.federation.store.records.SubClusterInfo;
import org.apache.hadoop.yarn.server.federation.store.records.SubClusterRegisterRequest;
import org.apache.hadoop.yarn.server.federation.store.records.SubClusterRegisterResponse;
import org.apache.hadoop.yarn.server.federation.store.records.SubClusterState;
import org.apache.hadoop.yarn.server.federation.store.records.UpdateApplicationHomeSubClusterRequest;
import org.apache.hadoop.yarn.server.federation.store.records.UpdateApplicationHomeSubClusterResponse;
import org.apache.hadoop.yarn.server.federation.utils.FederationStateStoreFacade;
import org.apache.hadoop.yarn.server.records.Version;
import org.apache.hadoop.yarn.server.resourcemanager.RMContext;
import org.apache.hadoop.yarn.server.resourcemanager.ResourceManager;
import org.apache.hadoop.yarn.webapp.util.WebAppUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;

/**
 * Implements {@link FederationStateStore} and provides a service for
 * participating in the federation membership.
 */
public class FederationStateStoreService extends AbstractService
    implements FederationStateStore, FederationStateStoreServiceJVMInterface {

  public static final Logger LOG =
      LoggerFactory.getLogger(FederationStateStoreService.class);

  private Configuration config;
  private ScheduledExecutorService scheduledExecutorService;
  private FederationStateStoreHeartbeat stateStoreHeartbeat;
  private FederationStateStore stateStoreClient = null;
  private SubClusterId subClusterId;
  private long heartbeatInterval;
  private RMContext rmContext;

  public FederationStateStoreService(RMContext rmContext) {
    super(FederationStateStoreService.class.getName());
    LOG.info("FederationStateStoreService initialized");
    this.rmContext = rmContext;
  }

  @Override
  protected void serviceInit(Configuration conf) throws Exception {

    this.config = conf;

    RetryPolicy retryPolicy =
        FederationStateStoreFacade.createRetryPolicy(conf);

    this.stateStoreClient =
        (FederationStateStore) FederationStateStoreFacade.createRetryInstance(
            conf, YarnConfiguration.FEDERATION_STATESTORE_CLIENT_CLASS,
            YarnConfiguration.DEFAULT_FEDERATION_STATESTORE_CLIENT_CLASS,
            FederationStateStore.class, retryPolicy);
    this.stateStoreClient.init(conf);
    LOG.info("Initialized state store client class");

    this.subClusterId =
        SubClusterId.newInstance(YarnConfiguration.getClusterId(conf));

    heartbeatInterval = conf.getLong(
        YarnConfiguration.FEDERATION_STATESTORE_HEARTBEAT_INTERVAL_SECS,
        YarnConfiguration.DEFAULT_FEDERATION_STATESTORE_HEARTBEAT_INTERVAL_SECS);
    if (heartbeatInterval <= 0) {
      heartbeatInterval =
          YarnConfiguration.DEFAULT_FEDERATION_STATESTORE_HEARTBEAT_INTERVAL_SECS;
    }
    LOG.info("Initialized federation membership service.");

    super.serviceInit(conf);
  }

  @Override
  protected void serviceStart() throws Exception {

    registerAndInitializeHeartbeat();

    super.serviceStart();
  }

  @Override
  protected void serviceStop() throws Exception {
    Exception ex = null;
    try {
      if (this.scheduledExecutorService != null
          && !this.scheduledExecutorService.isShutdown()) {
        this.scheduledExecutorService.shutdown();
        LOG.info("Stopped federation membership heartbeat");
      }
    } catch (Exception e) {
      LOG.error("Failed to shutdown ScheduledExecutorService", e);
      ex = e;
    }

    if (this.stateStoreClient != null) {
      try {
        deregisterSubCluster(SubClusterDeregisterRequest
            .newInstance(subClusterId, SubClusterState.SC_UNREGISTERED));
      } finally {
        this.stateStoreClient.close();
      }
    }

    if (ex != null) {
      throw ex;
    }
  }

  // Return a client accessible string representation of the service address.
  private String getServiceAddress(InetSocketAddress address) {
    InetSocketAddress socketAddress = NetUtils.getConnectAddress(address);
    return socketAddress.getAddress().getHostAddress() + ":"
        + socketAddress.getPort();
  }

  private void registerAndInitializeHeartbeat() {
    String clientRMAddress =
        getServiceAddress(rmContext.getClientRMService().getBindAddress());
    String amRMAddress = getServiceAddress(
        rmContext.getApplicationMasterService().getBindAddress());
    String rmAdminAddress = getServiceAddress(
        config.getSocketAddr(YarnConfiguration.RM_ADMIN_ADDRESS,
            YarnConfiguration.DEFAULT_RM_ADMIN_ADDRESS,
            YarnConfiguration.DEFAULT_RM_ADMIN_PORT));
    String webAppAddress = getServiceAddress(NetUtils
        .createSocketAddr(WebAppUtils.getRMWebAppURLWithScheme(config)));

    SubClusterInfo subClusterInfo = SubClusterInfo.newInstance(subClusterId,
        amRMAddress, clientRMAddress, rmAdminAddress, webAppAddress,
        SubClusterState.SC_NEW, ResourceManager.getClusterTimeStamp(), "");
    try {
      registerSubCluster(SubClusterRegisterRequest.newInstance(subClusterInfo));
      LOG.info("Successfully registered for federation subcluster: {}",
          subClusterInfo);
    } catch (Exception e) {
      throw new YarnRuntimeException(
          "Failed to register Federation membership with the StateStore", e);
    }
    stateStoreHeartbeat = new FederationStateStoreHeartbeat(subClusterId,
        stateStoreClient, rmContext.getScheduler());
    scheduledExecutorService =
        HadoopExecutors.newSingleThreadScheduledExecutor();
    scheduledExecutorService.scheduleWithFixedDelay(stateStoreHeartbeat,
        heartbeatInterval, heartbeatInterval, TimeUnit.SECONDS);
    LOG.info("Started federation membership heartbeat with interval: {}",
        heartbeatInterval);
  }

  @VisibleForTesting
  public FederationStateStore getStateStoreClient() {
    return stateStoreClient;
  }

  @VisibleForTesting
  public FederationStateStoreHeartbeat getStateStoreHeartbeatThread() {
    return stateStoreHeartbeat;
  }

  @Override
  public Version getCurrentVersion() {
    return stateStoreClient.getCurrentVersion();
  }

  @Override
  public Version loadVersion() {
    return stateStoreClient.getCurrentVersion();
  }

  @Override
  public GetSubClusterPolicyConfigurationResponse getPolicyConfiguration(
      GetSubClusterPolicyConfigurationRequest request) throws YarnException {
    return stateStoreClient.getPolicyConfiguration(request);
  }

  @Override
  public SetSubClusterPolicyConfigurationResponse setPolicyConfiguration(
      SetSubClusterPolicyConfigurationRequest request) throws YarnException {
    return stateStoreClient.setPolicyConfiguration(request);
  }

  @Override
  public GetSubClusterPoliciesConfigurationsResponse getPoliciesConfigurations(
      GetSubClusterPoliciesConfigurationsRequest request) throws YarnException {
    return stateStoreClient.getPoliciesConfigurations(request);
  }

  @Override
  public SubClusterRegisterResponse registerSubCluster(
      SubClusterRegisterRequest registerSubClusterRequest)
      throws YarnException {
    return stateStoreClient.registerSubCluster(registerSubClusterRequest);
  }

  @Override
  public SubClusterDeregisterResponse deregisterSubCluster(
      SubClusterDeregisterRequest subClusterDeregisterRequest)
      throws YarnException {
    return stateStoreClient.deregisterSubCluster(subClusterDeregisterRequest);
  }

  @Override
  public SubClusterHeartbeatResponse subClusterHeartbeat(
      SubClusterHeartbeatRequest subClusterHeartbeatRequest)
      throws YarnException {
    return stateStoreClient.subClusterHeartbeat(subClusterHeartbeatRequest);
  }

  @Override
  public GetSubClusterInfoResponse getSubCluster(
      GetSubClusterInfoRequest subClusterRequest) throws YarnException {
    return stateStoreClient.getSubCluster(subClusterRequest);
  }

  @Override
  public GetSubClustersInfoResponse getSubClusters(
      GetSubClustersInfoRequest subClustersRequest) throws YarnException {
    return stateStoreClient.getSubClusters(subClustersRequest);
  }

  @Override
  public AddApplicationHomeSubClusterResponse addApplicationHomeSubCluster(
      AddApplicationHomeSubClusterRequest request) throws YarnException {
    return stateStoreClient.addApplicationHomeSubCluster(request);
  }

  @Override
  public UpdateApplicationHomeSubClusterResponse updateApplicationHomeSubCluster(
      UpdateApplicationHomeSubClusterRequest request) throws YarnException {
    return stateStoreClient.updateApplicationHomeSubCluster(request);
  }

  @Override
  public GetApplicationHomeSubClusterResponse getApplicationHomeSubCluster(
      GetApplicationHomeSubClusterRequest request) throws YarnException {
    return stateStoreClient.getApplicationHomeSubCluster(request);
  }

  @Override
  public GetApplicationsHomeSubClusterResponse getApplicationsHomeSubCluster(
      GetApplicationsHomeSubClusterRequest request) throws YarnException {
    return stateStoreClient.getApplicationsHomeSubCluster(request);
  }

  @Override
  public DeleteApplicationHomeSubClusterResponse deleteApplicationHomeSubCluster(
      DeleteApplicationHomeSubClusterRequest request) throws YarnException {
    return stateStoreClient.deleteApplicationHomeSubCluster(request);
  }
  
  public org.apache.hadoop.yarn.server.federation.store.records.GetSubClusterInfoResponseJVMInterface getSubCluster_bridge(org.apache.hadoop.yarn.server.federation.store.records.GetSubClusterInfoRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[1];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("getSubCluster", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("getSubCluster"))
                      continue;
                  if (m.getParameterCount() != 1)
                      continue;
                  if (m.getReturnType().getName().equals("org.apache.hadoop.yarn.server.federation.store.records.GetSubClusterInfoResponseJVMInterface"))
                      continue;
                  target = m;
                  break;
              }
          }
          if (target == null)
              throw new RuntimeException("No matching target method found: getSubCluster");
          target.setAccessible(true);
          org.apache.hadoop.yarn.server.federation.store.records.GetSubClusterInfoResponseJVMInterface __result = (org.apache.hadoop.yarn.server.federation.store.records.GetSubClusterInfoResponseJVMInterface) target.invoke(this, arg0);
          return __result;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public org.apache.hadoop.yarn.server.federation.store.records.GetSubClusterPoliciesConfigurationsResponseJVMInterface getPoliciesConfigurations_bridge(org.apache.hadoop.yarn.server.federation.store.records.GetSubClusterPoliciesConfigurationsRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[1];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("getPoliciesConfigurations", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("getPoliciesConfigurations"))
                      continue;
                  if (m.getParameterCount() != 1)
                      continue;
                  if (m.getReturnType().getName().equals("org.apache.hadoop.yarn.server.federation.store.records.GetSubClusterPoliciesConfigurationsResponseJVMInterface"))
                      continue;
                  target = m;
                  break;
              }
          }
          if (target == null)
              throw new RuntimeException("No matching target method found: getPoliciesConfigurations");
          target.setAccessible(true);
          org.apache.hadoop.yarn.server.federation.store.records.GetSubClusterPoliciesConfigurationsResponseJVMInterface __result = (org.apache.hadoop.yarn.server.federation.store.records.GetSubClusterPoliciesConfigurationsResponseJVMInterface) target.invoke(this, arg0);
          return __result;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public org.apache.hadoop.yarn.server.federation.store.records.SubClusterRegisterResponseJVMInterface registerSubCluster_bridge(org.apache.hadoop.yarn.server.federation.store.records.SubClusterRegisterRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[1];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("registerSubCluster", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("registerSubCluster"))
                      continue;
                  if (m.getParameterCount() != 1)
                      continue;
                  if (m.getReturnType().getName().equals("org.apache.hadoop.yarn.server.federation.store.records.SubClusterRegisterResponseJVMInterface"))
                      continue;
                  target = m;
                  break;
              }
          }
          if (target == null)
              throw new RuntimeException("No matching target method found: registerSubCluster");
          target.setAccessible(true);
          org.apache.hadoop.yarn.server.federation.store.records.SubClusterRegisterResponseJVMInterface __result = (org.apache.hadoop.yarn.server.federation.store.records.SubClusterRegisterResponseJVMInterface) target.invoke(this, arg0);
          return __result;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public org.apache.hadoop.yarn.server.federation.store.records.GetSubClustersInfoResponseJVMInterface getSubClusters_bridge(org.apache.hadoop.yarn.server.federation.store.records.GetSubClustersInfoRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[1];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("getSubClusters", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("getSubClusters"))
                      continue;
                  if (m.getParameterCount() != 1)
                      continue;
                  if (m.getReturnType().getName().equals("org.apache.hadoop.yarn.server.federation.store.records.GetSubClustersInfoResponseJVMInterface"))
                      continue;
                  target = m;
                  break;
              }
          }
          if (target == null)
              throw new RuntimeException("No matching target method found: getSubClusters");
          target.setAccessible(true);
          org.apache.hadoop.yarn.server.federation.store.records.GetSubClustersInfoResponseJVMInterface __result = (org.apache.hadoop.yarn.server.federation.store.records.GetSubClustersInfoResponseJVMInterface) target.invoke(this, arg0);
          return __result;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public org.apache.hadoop.yarn.server.federation.store.records.GetApplicationHomeSubClusterResponseJVMInterface getApplicationHomeSubCluster_bridge(org.apache.hadoop.yarn.server.federation.store.records.GetApplicationHomeSubClusterRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[1];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("getApplicationHomeSubCluster", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("getApplicationHomeSubCluster"))
                      continue;
                  if (m.getParameterCount() != 1)
                      continue;
                  if (m.getReturnType().getName().equals("org.apache.hadoop.yarn.server.federation.store.records.GetApplicationHomeSubClusterResponseJVMInterface"))
                      continue;
                  target = m;
                  break;
              }
          }
          if (target == null)
              throw new RuntimeException("No matching target method found: getApplicationHomeSubCluster");
          target.setAccessible(true);
          org.apache.hadoop.yarn.server.federation.store.records.GetApplicationHomeSubClusterResponseJVMInterface __result = (org.apache.hadoop.yarn.server.federation.store.records.GetApplicationHomeSubClusterResponseJVMInterface) target.invoke(this, arg0);
          return __result;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public org.apache.hadoop.yarn.server.federation.store.records.DeleteApplicationHomeSubClusterResponseJVMInterface deleteApplicationHomeSubCluster_bridge(org.apache.hadoop.yarn.server.federation.store.records.DeleteApplicationHomeSubClusterRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[1];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("deleteApplicationHomeSubCluster", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("deleteApplicationHomeSubCluster"))
                      continue;
                  if (m.getParameterCount() != 1)
                      continue;
                  if (m.getReturnType().getName().equals("org.apache.hadoop.yarn.server.federation.store.records.DeleteApplicationHomeSubClusterResponseJVMInterface"))
                      continue;
                  target = m;
                  break;
              }
          }
          if (target == null)
              throw new RuntimeException("No matching target method found: deleteApplicationHomeSubCluster");
          target.setAccessible(true);
          org.apache.hadoop.yarn.server.federation.store.records.DeleteApplicationHomeSubClusterResponseJVMInterface __result = (org.apache.hadoop.yarn.server.federation.store.records.DeleteApplicationHomeSubClusterResponseJVMInterface) target.invoke(this, arg0);
          return __result;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public org.apache.hadoop.yarn.server.federation.store.records.SubClusterDeregisterResponseJVMInterface deregisterSubCluster_bridge(org.apache.hadoop.yarn.server.federation.store.records.SubClusterDeregisterRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[1];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("deregisterSubCluster", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("deregisterSubCluster"))
                      continue;
                  if (m.getParameterCount() != 1)
                      continue;
                  if (m.getReturnType().getName().equals("org.apache.hadoop.yarn.server.federation.store.records.SubClusterDeregisterResponseJVMInterface"))
                      continue;
                  target = m;
                  break;
              }
          }
          if (target == null)
              throw new RuntimeException("No matching target method found: deregisterSubCluster");
          target.setAccessible(true);
          org.apache.hadoop.yarn.server.federation.store.records.SubClusterDeregisterResponseJVMInterface __result = (org.apache.hadoop.yarn.server.federation.store.records.SubClusterDeregisterResponseJVMInterface) target.invoke(this, arg0);
          return __result;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public org.apache.hadoop.yarn.server.federation.store.records.GetApplicationsHomeSubClusterResponseJVMInterface getApplicationsHomeSubCluster_bridge(org.apache.hadoop.yarn.server.federation.store.records.GetApplicationsHomeSubClusterRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[1];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("getApplicationsHomeSubCluster", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("getApplicationsHomeSubCluster"))
                      continue;
                  if (m.getParameterCount() != 1)
                      continue;
                  if (m.getReturnType().getName().equals("org.apache.hadoop.yarn.server.federation.store.records.GetApplicationsHomeSubClusterResponseJVMInterface"))
                      continue;
                  target = m;
                  break;
              }
          }
          if (target == null)
              throw new RuntimeException("No matching target method found: getApplicationsHomeSubCluster");
          target.setAccessible(true);
          org.apache.hadoop.yarn.server.federation.store.records.GetApplicationsHomeSubClusterResponseJVMInterface __result = (org.apache.hadoop.yarn.server.federation.store.records.GetApplicationsHomeSubClusterResponseJVMInterface) target.invoke(this, arg0);
          return __result;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public org.apache.hadoop.yarn.server.federation.store.records.SetSubClusterPolicyConfigurationResponseJVMInterface setPolicyConfiguration_bridge(org.apache.hadoop.yarn.server.federation.store.records.SetSubClusterPolicyConfigurationRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[1];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("setPolicyConfiguration", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("setPolicyConfiguration"))
                      continue;
                  if (m.getParameterCount() != 1)
                      continue;
                  if (m.getReturnType().getName().equals("org.apache.hadoop.yarn.server.federation.store.records.SetSubClusterPolicyConfigurationResponseJVMInterface"))
                      continue;
                  target = m;
                  break;
              }
          }
          if (target == null)
              throw new RuntimeException("No matching target method found: setPolicyConfiguration");
          target.setAccessible(true);
          org.apache.hadoop.yarn.server.federation.store.records.SetSubClusterPolicyConfigurationResponseJVMInterface __result = (org.apache.hadoop.yarn.server.federation.store.records.SetSubClusterPolicyConfigurationResponseJVMInterface) target.invoke(this, arg0);
          return __result;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public org.apache.hadoop.yarn.server.federation.store.records.GetSubClusterPolicyConfigurationResponseJVMInterface getPolicyConfiguration_bridge(org.apache.hadoop.yarn.server.federation.store.records.GetSubClusterPolicyConfigurationRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[1];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("getPolicyConfiguration", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("getPolicyConfiguration"))
                      continue;
                  if (m.getParameterCount() != 1)
                      continue;
                  if (m.getReturnType().getName().equals("org.apache.hadoop.yarn.server.federation.store.records.GetSubClusterPolicyConfigurationResponseJVMInterface"))
                      continue;
                  target = m;
                  break;
              }
          }
          if (target == null)
              throw new RuntimeException("No matching target method found: getPolicyConfiguration");
          target.setAccessible(true);
          org.apache.hadoop.yarn.server.federation.store.records.GetSubClusterPolicyConfigurationResponseJVMInterface __result = (org.apache.hadoop.yarn.server.federation.store.records.GetSubClusterPolicyConfigurationResponseJVMInterface) target.invoke(this, arg0);
          return __result;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public org.apache.hadoop.yarn.server.federation.store.records.UpdateApplicationHomeSubClusterResponseJVMInterface updateApplicationHomeSubCluster_bridge(org.apache.hadoop.yarn.server.federation.store.records.UpdateApplicationHomeSubClusterRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[1];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("updateApplicationHomeSubCluster", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("updateApplicationHomeSubCluster"))
                      continue;
                  if (m.getParameterCount() != 1)
                      continue;
                  if (m.getReturnType().getName().equals("org.apache.hadoop.yarn.server.federation.store.records.UpdateApplicationHomeSubClusterResponseJVMInterface"))
                      continue;
                  target = m;
                  break;
              }
          }
          if (target == null)
              throw new RuntimeException("No matching target method found: updateApplicationHomeSubCluster");
          target.setAccessible(true);
          org.apache.hadoop.yarn.server.federation.store.records.UpdateApplicationHomeSubClusterResponseJVMInterface __result = (org.apache.hadoop.yarn.server.federation.store.records.UpdateApplicationHomeSubClusterResponseJVMInterface) target.invoke(this, arg0);
          return __result;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public org.apache.hadoop.yarn.server.federation.store.records.AddApplicationHomeSubClusterResponseJVMInterface addApplicationHomeSubCluster_bridge(org.apache.hadoop.yarn.server.federation.store.records.AddApplicationHomeSubClusterRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[1];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("addApplicationHomeSubCluster", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("addApplicationHomeSubCluster"))
                      continue;
                  if (m.getParameterCount() != 1)
                      continue;
                  if (m.getReturnType().getName().equals("org.apache.hadoop.yarn.server.federation.store.records.AddApplicationHomeSubClusterResponseJVMInterface"))
                      continue;
                  target = m;
                  break;
              }
          }
          if (target == null)
              throw new RuntimeException("No matching target method found: addApplicationHomeSubCluster");
          target.setAccessible(true);
          org.apache.hadoop.yarn.server.federation.store.records.AddApplicationHomeSubClusterResponseJVMInterface __result = (org.apache.hadoop.yarn.server.federation.store.records.AddApplicationHomeSubClusterResponseJVMInterface) target.invoke(this, arg0);
          return __result;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public org.apache.hadoop.yarn.server.federation.store.records.SubClusterHeartbeatResponseJVMInterface subClusterHeartbeat_bridge(org.apache.hadoop.yarn.server.federation.store.records.SubClusterHeartbeatRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[1];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("subClusterHeartbeat", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("subClusterHeartbeat"))
                      continue;
                  if (m.getParameterCount() != 1)
                      continue;
                  if (m.getReturnType().getName().equals("org.apache.hadoop.yarn.server.federation.store.records.SubClusterHeartbeatResponseJVMInterface"))
                      continue;
                  target = m;
                  break;
              }
          }
          if (target == null)
              throw new RuntimeException("No matching target method found: subClusterHeartbeat");
          target.setAccessible(true);
          org.apache.hadoop.yarn.server.federation.store.records.SubClusterHeartbeatResponseJVMInterface __result = (org.apache.hadoop.yarn.server.federation.store.records.SubClusterHeartbeatResponseJVMInterface) target.invoke(this, arg0);
          return __result;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
}

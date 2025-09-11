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

package org.apache.hadoop.yarn.server.nodemanager.recovery;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.classification.InterfaceAudience.Private;
import org.apache.hadoop.classification.InterfaceStability.Unstable;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.service.AbstractService;
import org.apache.hadoop.yarn.api.protocolrecords.StartContainerRequest;
import org.apache.hadoop.yarn.api.records.ApplicationAttemptId;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.api.records.ContainerExitStatus;
import org.apache.hadoop.yarn.api.records.ContainerId;
import org.apache.hadoop.yarn.api.records.ContainerRetryContext;
import org.apache.hadoop.yarn.api.records.Resource;
import org.apache.hadoop.yarn.proto.YarnProtos.LocalResourceProto;
import org.apache.hadoop.yarn.proto.YarnServerNodemanagerRecoveryProtos.ContainerManagerApplicationProto;
import org.apache.hadoop.yarn.proto.YarnServerNodemanagerRecoveryProtos.DeletionServiceDeleteTaskProto;
import org.apache.hadoop.yarn.proto.YarnServerNodemanagerRecoveryProtos.LocalizedResourceProto;
import org.apache.hadoop.yarn.proto.YarnServerNodemanagerRecoveryProtos.LogDeleterProto;
import org.apache.hadoop.yarn.security.ContainerTokenIdentifier;
import org.apache.hadoop.yarn.server.api.records.MasterKey;

@Private
@Unstable
public abstract class NMStateStoreService extends AbstractService implements NMStateStoreServiceJVMInterface {

  public NMStateStoreService(String name) {
    super(name);
  }

  public static class RecoveredApplicationsState {
    List<ContainerManagerApplicationProto> applications;

    public List<ContainerManagerApplicationProto> getApplications() {
      return applications;
    }

  }

  /**
   * Type of post recovery action.
   */
  public enum RecoveredContainerType {
    KILL, RECOVER
  }

  public enum RecoveredContainerStatus {
    REQUESTED,
    QUEUED,
    LAUNCHED,
    COMPLETED,
    PAUSED
  }

  public static class RecoveredContainerState {
    RecoveredContainerStatus status;
    int exitCode = ContainerExitStatus.INVALID;
    boolean killed = false;
    String diagnostics = "";
    StartContainerRequest startRequest;
    Resource capability;
    private int remainingRetryAttempts = ContainerRetryContext.RETRY_INVALID;
    private String workDir;
    private String logDir;
    int version;
    private RecoveredContainerType recoveryType =
        RecoveredContainerType.RECOVER;
    private long startTime;

    public RecoveredContainerStatus getStatus() {
      return status;
    }

    public int getExitCode() {
      return exitCode;
    }

    public boolean getKilled() {
      return killed;
    }

    public String getDiagnostics() {
      return diagnostics;
    }

    public int getVersion() {
      return version;
    }

    public long getStartTime() {
      return startTime;
    }

    public void setStartTime(long ts) {
      startTime = ts;
    }

    public StartContainerRequest getStartRequest() {
      return startRequest;
    }

    public Resource getCapability() {
      return capability;
    }

    public int getRemainingRetryAttempts() {
      return remainingRetryAttempts;
    }

    public void setRemainingRetryAttempts(int retryAttempts) {
      this.remainingRetryAttempts = retryAttempts;
    }

    public String getWorkDir() {
      return workDir;
    }

    public void setWorkDir(String workDir) {
      this.workDir = workDir;
    }

    public String getLogDir() {
      return logDir;
    }

    public void setLogDir(String logDir) {
      this.logDir = logDir;
    }

    @Override
    public String toString() {
      return new StringBuffer("Status: ").append(getStatus())
          .append(", Exit code: ").append(exitCode)
          .append(", Version: ").append(version)
          .append(", Start Time: ").append(startTime)
          .append(", Killed: ").append(getKilled())
          .append(", Diagnostics: ").append(getDiagnostics())
          .append(", Capability: ").append(getCapability())
          .append(", StartRequest: ").append(getStartRequest())
          .append(", RemainingRetryAttempts: ").append(remainingRetryAttempts)
          .append(", WorkDir: ").append(workDir)
          .append(", LogDir: ").append(logDir)
          .toString();
    }

    public RecoveredContainerType getRecoveryType() {
      return recoveryType;
    }

    public void setRecoveryType(RecoveredContainerType recoveryType) {
      this.recoveryType = recoveryType;
    }
  }

  public static class LocalResourceTrackerState {
    List<LocalizedResourceProto> localizedResources =
        new ArrayList<LocalizedResourceProto>();
    Map<LocalResourceProto, Path> inProgressResources =
        new HashMap<LocalResourceProto, Path>();

    public List<LocalizedResourceProto> getLocalizedResources() {
      return localizedResources;
    }

    public Map<LocalResourceProto, Path> getInProgressResources() {
      return inProgressResources;
    }

    public boolean isEmpty() {
      return localizedResources.isEmpty() && inProgressResources.isEmpty();
    }
  }

  public static class RecoveredUserResources {
    LocalResourceTrackerState privateTrackerState =
        new LocalResourceTrackerState();
    Map<ApplicationId, LocalResourceTrackerState> appTrackerStates =
        new HashMap<ApplicationId, LocalResourceTrackerState>();

    public LocalResourceTrackerState getPrivateTrackerState() {
      return privateTrackerState;
    }

    public Map<ApplicationId, LocalResourceTrackerState>
    getAppTrackerStates() {
      return appTrackerStates;
    }
  }

  public static class RecoveredLocalizationState {
    LocalResourceTrackerState publicTrackerState =
        new LocalResourceTrackerState();
    Map<String, RecoveredUserResources> userResources =
        new HashMap<String, RecoveredUserResources>();

    public LocalResourceTrackerState getPublicTrackerState() {
      return publicTrackerState;
    }

    public Map<String, RecoveredUserResources> getUserResources() {
      return userResources;
    }
  }

  public static class RecoveredDeletionServiceState {
    List<DeletionServiceDeleteTaskProto> tasks;

    public List<DeletionServiceDeleteTaskProto> getTasks() {
      return tasks;
    }
  }

  public static class RecoveredNMTokensState {
    MasterKey currentMasterKey;
    MasterKey previousMasterKey;
    Map<ApplicationAttemptId, MasterKey> applicationMasterKeys;

    public MasterKey getCurrentMasterKey() {
      return currentMasterKey;
    }

    public MasterKey getPreviousMasterKey() {
      return previousMasterKey;
    }

    public Map<ApplicationAttemptId, MasterKey> getApplicationMasterKeys() {
      return applicationMasterKeys;
    }
  }

  public static class RecoveredContainerTokensState {
    MasterKey currentMasterKey;
    MasterKey previousMasterKey;
    Map<ContainerId, Long> activeTokens;

    public MasterKey getCurrentMasterKey() {
      return currentMasterKey;
    }

    public MasterKey getPreviousMasterKey() {
      return previousMasterKey;
    }

    public Map<ContainerId, Long> getActiveTokens() {
      return activeTokens;
    }
  }

  public static class RecoveredLogDeleterState {
    Map<ApplicationId, LogDeleterProto> logDeleterMap;

    public Map<ApplicationId, LogDeleterProto> getLogDeleterMap() {
      return logDeleterMap;
    }
  }

  /**
   * Recovered states for AMRMProxy.
   */
  public static class RecoveredAMRMProxyState {
    private MasterKey currentMasterKey;
    private MasterKey nextMasterKey;
    // For each app, stores amrmToken, user name, as well as various AMRMProxy
    // intercepter states
    private Map<ApplicationAttemptId, Map<String, byte[]>> appContexts;

    public RecoveredAMRMProxyState() {
      appContexts = new HashMap<>();
    }

    public MasterKey getCurrentMasterKey() {
      return currentMasterKey;
    }

    public MasterKey getNextMasterKey() {
      return nextMasterKey;
    }

    public Map<ApplicationAttemptId, Map<String, byte[]>> getAppContexts() {
      return appContexts;
    }

    public void setCurrentMasterKey(MasterKey currentKey) {
      currentMasterKey = currentKey;
    }

    public void setNextMasterKey(MasterKey nextKey) {
      nextMasterKey = nextKey;
    }
  }

  /** Initialize the state storage */
  @Override
  public void serviceInit(Configuration conf) throws IOException {
    initStorage(conf);
  }

  /** Start the state storage for use */
  @Override
  public void serviceStart() throws IOException {
    startStorage();
  }

  /** Shutdown the state storage. */
  @Override
  public void serviceStop() throws IOException {
    closeStorage();
  }

  public boolean canRecover() {
    return true;
  }

  public boolean isNewlyCreated() {
    return false;
  }

  /**
   * Load the state of applications.
   * @return recovered state for applications.
   * @throws IOException IO Exception.
   */
  public abstract RecoveredApplicationsState loadApplicationsState()
      throws IOException;

  /**
   * Record the start of an application
   * @param appId the application ID
   * @param p state to store for the application
   * @throws IOException
   */
  public abstract void storeApplication(ApplicationId appId,
      ContainerManagerApplicationProto p) throws IOException;

  /**
   * Remove records corresponding to an application
   * @param appId the application ID
   * @throws IOException
   */
  public abstract void removeApplication(ApplicationId appId)
      throws IOException;


  /**
   * Load the state of containers
   * @return recovered state for containers
   * @throws IOException
   */
  public abstract List<RecoveredContainerState> loadContainersState()
      throws IOException;

  /**
   * Record a container start request
   * @param containerId the container ID
   * @param containerVersion the container Version
   * @param startTime container start time
   * @param startRequest the container start request
   * @throws IOException
   */
  public abstract void storeContainer(ContainerId containerId,
      int containerVersion, long startTime, StartContainerRequest startRequest)
      throws IOException;

  /**
   * Record that a container has been queued at the NM
   * @param containerId the container ID
   * @throws IOException
   */
  public abstract void storeContainerQueued(ContainerId containerId)
      throws IOException;

  /**
   * Record that a container has been paused at the NM.
   * @param containerId the container ID.
   * @throws IOException IO Exception.
   */
  public abstract void storeContainerPaused(ContainerId containerId)
      throws IOException;

  /**
   * Record that a container has been resumed at the NM by removing the
   * fact that it has be paused.
   * @param containerId the container ID.
   * @throws IOException IO Exception.
   */
  public abstract void removeContainerPaused(ContainerId containerId)
      throws IOException;

  /**
   * Record that a container has been launched
   * @param containerId the container ID
   * @throws IOException
   */
  public abstract void storeContainerLaunched(ContainerId containerId)
      throws IOException;

  /**
   * Record that a container has been updated
   * @param containerId the container ID
   * @param containerTokenIdentifier container token identifier
   * @throws IOException
   */
  public abstract void storeContainerUpdateToken(ContainerId containerId,
      ContainerTokenIdentifier containerTokenIdentifier) throws IOException;

  /**
   * Record that a container has completed
   * @param containerId the container ID
   * @param exitCode the exit code from the container
   * @throws IOException
   */
  public abstract void storeContainerCompleted(ContainerId containerId,
      int exitCode) throws IOException;

  /**
   * Record a request to kill a container
   * @param containerId the container ID
   * @throws IOException
   */
  public abstract void storeContainerKilled(ContainerId containerId)
      throws IOException;

  /**
   * Record diagnostics for a container
   * @param containerId the container ID
   * @param diagnostics the container diagnostics
   * @throws IOException
   */
  public abstract void storeContainerDiagnostics(ContainerId containerId,
      StringBuilder diagnostics) throws IOException;

  /**
   * Record remaining retry attempts for a container.
   * @param containerId the container ID
   * @param remainingRetryAttempts the remain retry times when container
   *                               fails to run
   * @throws IOException
   */
  public abstract void storeContainerRemainingRetryAttempts(
      ContainerId containerId, int remainingRetryAttempts) throws IOException;

  /**
   * Record working directory for a container.
   * @param containerId the container ID
   * @param workDir the working directory
   * @throws IOException
   */
  public abstract void storeContainerWorkDir(
      ContainerId containerId, String workDir) throws IOException;

  /**
   * Record log directory for a container.
   * @param containerId the container ID
   * @param logDir the log directory
   * @throws IOException
   */
  public abstract void storeContainerLogDir(
      ContainerId containerId, String logDir) throws IOException;

  /**
   * Remove records corresponding to a container
   * @param containerId the container ID
   * @throws IOException
   */
  public abstract void removeContainer(ContainerId containerId)
      throws IOException;


  /**
   * Load the state of localized resources
   * @return recovered localized resource state
   * @throws IOException
   */
  public abstract RecoveredLocalizationState loadLocalizationState()
      throws IOException;

  /**
   * Record the start of localization for a resource
   * @param user the username or null if the resource is public
   * @param appId the application ID if the resource is app-specific or null
   * @param proto the resource request
   * @param localPath local filesystem path where the resource will be stored
   * @throws IOException
   */
  public abstract void startResourceLocalization(String user,
      ApplicationId appId, LocalResourceProto proto, Path localPath)
          throws IOException;

  /**
   * Record the completion of a resource localization
   * @param user the username or null if the resource is public
   * @param appId the application ID if the resource is app-specific or null
   * @param proto the serialized localized resource
   * @throws IOException
   */
  public abstract void finishResourceLocalization(String user,
      ApplicationId appId, LocalizedResourceProto proto) throws IOException;

  /**
   * Remove records related to a resource localization
   * @param user the username or null if the resource is public
   * @param appId the application ID if the resource is app-specific or null
   * @param localPath local filesystem path where the resource will be stored
   * @throws IOException
   */
  public abstract void removeLocalizedResource(String user,
      ApplicationId appId, Path localPath) throws IOException;


  /**
   * Load the state of the deletion service
   * @return recovered deletion service state
   * @throws IOException
   */
  public abstract RecoveredDeletionServiceState loadDeletionServiceState()
      throws IOException;

  /**
   * Record a deletion task
   * @param taskId the deletion task ID
   * @param taskProto the deletion task protobuf
   * @throws IOException
   */
  public abstract void storeDeletionTask(int taskId,
      DeletionServiceDeleteTaskProto taskProto) throws IOException;

  /**
   * Remove records corresponding to a deletion task
   * @param taskId the deletion task ID
   * @throws IOException
   */
  public abstract void removeDeletionTask(int taskId) throws IOException;


  /**
   * Load the state of NM tokens
   * @return recovered state of NM tokens
   * @throws IOException
   */
  public abstract RecoveredNMTokensState loadNMTokensState()
      throws IOException;

  /**
   * Record the current NM token master key
   * @param key the master key
   * @throws IOException
   */
  public abstract void storeNMTokenCurrentMasterKey(MasterKey key)
      throws IOException;

  /**
   * Record the previous NM token master key
   * @param key the previous master key
   * @throws IOException
   */
  public abstract void storeNMTokenPreviousMasterKey(MasterKey key)
      throws IOException;

  /**
   * Record a master key corresponding to an application
   * @param attempt the application attempt ID
   * @param key the master key
   * @throws IOException
   */
  public abstract void storeNMTokenApplicationMasterKey(
      ApplicationAttemptId attempt, MasterKey key) throws IOException;

  /**
   * Remove a master key corresponding to an application
   * @param attempt the application attempt ID
   * @throws IOException
   */
  public abstract void removeNMTokenApplicationMasterKey(
      ApplicationAttemptId attempt) throws IOException;


  /**
   * Load the state of container tokens
   * @return recovered state of container tokens
   * @throws IOException
   */
  public abstract RecoveredContainerTokensState loadContainerTokensState()
      throws IOException;

  /**
   * Record the current container token master key
   * @param key the master key
   * @throws IOException
   */
  public abstract void storeContainerTokenCurrentMasterKey(MasterKey key)
      throws IOException;

  /**
   * Record the previous container token master key
   * @param key the previous master key
   * @throws IOException
   */
  public abstract void storeContainerTokenPreviousMasterKey(MasterKey key)
      throws IOException;

  /**
   * Record the expiration time for a container token
   * @param containerId the container ID
   * @param expirationTime the container token expiration time
   * @throws IOException
   */
  public abstract void storeContainerToken(ContainerId containerId,
      Long expirationTime) throws IOException;

  /**
   * Remove records for a container token
   * @param containerId the container ID
   * @throws IOException
   */
  public abstract void removeContainerToken(ContainerId containerId)
      throws IOException;


  /**
   * Load the state of log deleters
   * @return recovered log deleter state
   * @throws IOException
   */
  public abstract RecoveredLogDeleterState loadLogDeleterState()
      throws IOException;

  /**
   * Store the state of a log deleter
   * @param appId the application ID for the log deleter
   * @param proto the serialized state of the log deleter
   * @throws IOException
   */
  public abstract void storeLogDeleter(ApplicationId appId,
      LogDeleterProto proto) throws IOException;

  /**
   * Remove the state of a log deleter
   * @param appId the application ID for the log deleter
   * @throws IOException
   */
  public abstract void removeLogDeleter(ApplicationId appId)
      throws IOException;

  /**
   * Load the state of AMRMProxy.
   * @return recovered state of AMRMProxy
   * @throws IOException if fails
   */
  public abstract RecoveredAMRMProxyState loadAMRMProxyState()
      throws IOException;

  /**
   * Record the current AMRMProxyTokenSecretManager master key.
   * @param key the current master key
   * @throws IOException if fails
   */
  public abstract void storeAMRMProxyCurrentMasterKey(MasterKey key)
      throws IOException;

  /**
   * Record the next AMRMProxyTokenSecretManager master key.
   * @param key the next master key
   * @throws IOException if fails
   */
  public abstract void storeAMRMProxyNextMasterKey(MasterKey key)
      throws IOException;

  /**
   * Add a context entry for an application attempt in AMRMProxyService.
   * @param attempt app attempt ID
   * @param key key string
   * @param data state data to store
   * @throws IOException if fails
   */
  public abstract void storeAMRMProxyAppContextEntry(
      ApplicationAttemptId attempt, String key, byte[] data) throws IOException;

  /**
   * Remove a context entry for an application attempt in AMRMProxyService.
   * @param attempt attempt ID
   * @param key key string
   * @throws IOException if fails
   */
  public abstract void removeAMRMProxyAppContextEntry(
      ApplicationAttemptId attempt, String key) throws IOException;

  /**
   * Remove the entire context map for an application attempt in
   * AMRMProxyService.
   * @param attempt attempt ID
   * @throws IOException if fails
   */
  public abstract void removeAMRMProxyAppContext(ApplicationAttemptId attempt)
      throws IOException;

  protected abstract void initStorage(Configuration conf) throws IOException;

  protected abstract void startStorage() throws IOException;

  protected abstract void closeStorage() throws IOException;
  
  public void finishResourceLocalization_bridge(java.lang.String arg0, org.apache.hadoop.yarn.api.records.ApplicationIdJVMInterface arg1, java.lang.Object arg2) throws java.io.IOException {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[3];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              __types[1] = (arg1 != null ? arg1.getClass() : Object.class);
              __types[2] = (arg2 != null ? arg2.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("finishResourceLocalization", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("finishResourceLocalization"))
                      continue;
                  if (m.getParameterCount() != 3)
                      continue;
                  if (m.getReturnType().getName().equals("void"))
                      continue;
                  target = m;
                  break;
              }
          }
          if (target == null)
              throw new RuntimeException("No matching target method found: finishResourceLocalization");
          target.setAccessible(true);
          target.invoke(this, arg0, arg1, arg2);
          return;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public void storeNMTokenPreviousMasterKey_bridge(java.lang.Object arg0) throws java.io.IOException {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[1];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("storeNMTokenPreviousMasterKey", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("storeNMTokenPreviousMasterKey"))
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
              throw new RuntimeException("No matching target method found: storeNMTokenPreviousMasterKey");
          target.setAccessible(true);
          target.invoke(this, arg0);
          return;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public void storeContainerTokenPreviousMasterKey_bridge(java.lang.Object arg0) throws java.io.IOException {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[1];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("storeContainerTokenPreviousMasterKey", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("storeContainerTokenPreviousMasterKey"))
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
              throw new RuntimeException("No matching target method found: storeContainerTokenPreviousMasterKey");
          target.setAccessible(true);
          target.invoke(this, arg0);
          return;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public void storeContainerWorkDir_bridge(org.apache.hadoop.yarn.api.records.ContainerIdJVMInterface arg0, java.lang.String arg1) throws java.io.IOException {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[2];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              __types[1] = (arg1 != null ? arg1.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("storeContainerWorkDir", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("storeContainerWorkDir"))
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
              throw new RuntimeException("No matching target method found: storeContainerWorkDir");
          target.setAccessible(true);
          target.invoke(this, arg0, arg1);
          return;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public void storeNMTokenApplicationMasterKey_bridge(org.apache.hadoop.yarn.api.records.ApplicationAttemptIdJVMInterface arg0, java.lang.Object arg1) throws java.io.IOException {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[2];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              __types[1] = (arg1 != null ? arg1.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("storeNMTokenApplicationMasterKey", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("storeNMTokenApplicationMasterKey"))
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
              throw new RuntimeException("No matching target method found: storeNMTokenApplicationMasterKey");
          target.setAccessible(true);
          target.invoke(this, arg0, arg1);
          return;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public void storeContainerLaunched_bridge(org.apache.hadoop.yarn.api.records.ContainerIdJVMInterface arg0) throws java.io.IOException {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[1];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("storeContainerLaunched", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("storeContainerLaunched"))
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
              throw new RuntimeException("No matching target method found: storeContainerLaunched");
          target.setAccessible(true);
          target.invoke(this, arg0);
          return;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public void storeContainerPaused_bridge(org.apache.hadoop.yarn.api.records.ContainerIdJVMInterface arg0) throws java.io.IOException {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[1];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("storeContainerPaused", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("storeContainerPaused"))
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
              throw new RuntimeException("No matching target method found: storeContainerPaused");
          target.setAccessible(true);
          target.invoke(this, arg0);
          return;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public void storeAMRMProxyCurrentMasterKey_bridge(java.lang.Object arg0) throws java.io.IOException {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[1];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("storeAMRMProxyCurrentMasterKey", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("storeAMRMProxyCurrentMasterKey"))
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
              throw new RuntimeException("No matching target method found: storeAMRMProxyCurrentMasterKey");
          target.setAccessible(true);
          target.invoke(this, arg0);
          return;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public void removeContainer_bridge(org.apache.hadoop.yarn.api.records.ContainerIdJVMInterface arg0) throws java.io.IOException {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[1];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("removeContainer", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("removeContainer"))
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
              throw new RuntimeException("No matching target method found: removeContainer");
          target.setAccessible(true);
          target.invoke(this, arg0);
          return;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public void storeContainerRemainingRetryAttempts_bridge(org.apache.hadoop.yarn.api.records.ContainerIdJVMInterface arg0, int arg1) throws java.io.IOException {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[2];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              __types[1] = int.class;
              try {
                  target = this.getClass().getMethod("storeContainerRemainingRetryAttempts", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("storeContainerRemainingRetryAttempts"))
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
              throw new RuntimeException("No matching target method found: storeContainerRemainingRetryAttempts");
          target.setAccessible(true);
          target.invoke(this, arg0, arg1);
          return;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public void removeContainerToken_bridge(org.apache.hadoop.yarn.api.records.ContainerIdJVMInterface arg0) throws java.io.IOException {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[1];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("removeContainerToken", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("removeContainerToken"))
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
              throw new RuntimeException("No matching target method found: removeContainerToken");
          target.setAccessible(true);
          target.invoke(this, arg0);
          return;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public void removeAMRMProxyAppContextEntry_bridge(org.apache.hadoop.yarn.api.records.ApplicationAttemptIdJVMInterface arg0, java.lang.String arg1) throws java.io.IOException {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[2];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              __types[1] = (arg1 != null ? arg1.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("removeAMRMProxyAppContextEntry", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("removeAMRMProxyAppContextEntry"))
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
              throw new RuntimeException("No matching target method found: removeAMRMProxyAppContextEntry");
          target.setAccessible(true);
          target.invoke(this, arg0, arg1);
          return;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public void serviceInit_bridge(org.apache.hadoop.conf.ConfigurationJVMInterface arg0) throws java.io.IOException {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[1];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("serviceInit", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("serviceInit"))
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
              throw new RuntimeException("No matching target method found: serviceInit");
          target.setAccessible(true);
          target.invoke(this, arg0);
          return;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public void storeContainerDiagnostics_bridge(org.apache.hadoop.yarn.api.records.ContainerIdJVMInterface arg0, java.lang.StringBuilder arg1) throws java.io.IOException {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[2];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              __types[1] = (arg1 != null ? arg1.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("storeContainerDiagnostics", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("storeContainerDiagnostics"))
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
              throw new RuntimeException("No matching target method found: storeContainerDiagnostics");
          target.setAccessible(true);
          target.invoke(this, arg0, arg1);
          return;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public void storeContainerQueued_bridge(org.apache.hadoop.yarn.api.records.ContainerIdJVMInterface arg0) throws java.io.IOException {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[1];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("storeContainerQueued", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("storeContainerQueued"))
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
              throw new RuntimeException("No matching target method found: storeContainerQueued");
          target.setAccessible(true);
          target.invoke(this, arg0);
          return;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public void removeAMRMProxyAppContext_bridge(org.apache.hadoop.yarn.api.records.ApplicationAttemptIdJVMInterface arg0) throws java.io.IOException {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[1];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("removeAMRMProxyAppContext", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("removeAMRMProxyAppContext"))
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
              throw new RuntimeException("No matching target method found: removeAMRMProxyAppContext");
          target.setAccessible(true);
          target.invoke(this, arg0);
          return;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public void storeContainerKilled_bridge(org.apache.hadoop.yarn.api.records.ContainerIdJVMInterface arg0) throws java.io.IOException {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[1];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("storeContainerKilled", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("storeContainerKilled"))
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
              throw new RuntimeException("No matching target method found: storeContainerKilled");
          target.setAccessible(true);
          target.invoke(this, arg0);
          return;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public void storeContainerLogDir_bridge(org.apache.hadoop.yarn.api.records.ContainerIdJVMInterface arg0, java.lang.String arg1) throws java.io.IOException {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[2];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              __types[1] = (arg1 != null ? arg1.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("storeContainerLogDir", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("storeContainerLogDir"))
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
              throw new RuntimeException("No matching target method found: storeContainerLogDir");
          target.setAccessible(true);
          target.invoke(this, arg0, arg1);
          return;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public void removeNMTokenApplicationMasterKey_bridge(org.apache.hadoop.yarn.api.records.ApplicationAttemptIdJVMInterface arg0) throws java.io.IOException {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[1];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("removeNMTokenApplicationMasterKey", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("removeNMTokenApplicationMasterKey"))
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
              throw new RuntimeException("No matching target method found: removeNMTokenApplicationMasterKey");
          target.setAccessible(true);
          target.invoke(this, arg0);
          return;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public void storeDeletionTask_bridge(int arg0, java.lang.Object arg1) throws java.io.IOException {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[2];
              __types[0] = int.class;
              __types[1] = (arg1 != null ? arg1.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("storeDeletionTask", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("storeDeletionTask"))
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
              throw new RuntimeException("No matching target method found: storeDeletionTask");
          target.setAccessible(true);
          target.invoke(this, arg0, arg1);
          return;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public void removeApplication_bridge(org.apache.hadoop.yarn.api.records.ApplicationIdJVMInterface arg0) throws java.io.IOException {
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
  
  public void storeApplication_bridge(org.apache.hadoop.yarn.api.records.ApplicationIdJVMInterface arg0, java.lang.Object arg1) throws java.io.IOException {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[2];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              __types[1] = (arg1 != null ? arg1.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("storeApplication", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("storeApplication"))
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
              throw new RuntimeException("No matching target method found: storeApplication");
          target.setAccessible(true);
          target.invoke(this, arg0, arg1);
          return;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public void storeNMTokenCurrentMasterKey_bridge(java.lang.Object arg0) throws java.io.IOException {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[1];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("storeNMTokenCurrentMasterKey", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("storeNMTokenCurrentMasterKey"))
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
              throw new RuntimeException("No matching target method found: storeNMTokenCurrentMasterKey");
          target.setAccessible(true);
          target.invoke(this, arg0);
          return;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public void storeContainerToken_bridge(org.apache.hadoop.yarn.api.records.ContainerIdJVMInterface arg0, java.lang.Long arg1) throws java.io.IOException {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[2];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              __types[1] = (arg1 != null ? arg1.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("storeContainerToken", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("storeContainerToken"))
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
              throw new RuntimeException("No matching target method found: storeContainerToken");
          target.setAccessible(true);
          target.invoke(this, arg0, arg1);
          return;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public void storeLogDeleter_bridge(org.apache.hadoop.yarn.api.records.ApplicationIdJVMInterface arg0, java.lang.Object arg1) throws java.io.IOException {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[2];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              __types[1] = (arg1 != null ? arg1.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("storeLogDeleter", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("storeLogDeleter"))
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
              throw new RuntimeException("No matching target method found: storeLogDeleter");
          target.setAccessible(true);
          target.invoke(this, arg0, arg1);
          return;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public void storeContainerCompleted_bridge(org.apache.hadoop.yarn.api.records.ContainerIdJVMInterface arg0, int arg1) throws java.io.IOException {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[2];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              __types[1] = int.class;
              try {
                  target = this.getClass().getMethod("storeContainerCompleted", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("storeContainerCompleted"))
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
              throw new RuntimeException("No matching target method found: storeContainerCompleted");
          target.setAccessible(true);
          target.invoke(this, arg0, arg1);
          return;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public void removeLogDeleter_bridge(org.apache.hadoop.yarn.api.records.ApplicationIdJVMInterface arg0) throws java.io.IOException {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[1];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("removeLogDeleter", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("removeLogDeleter"))
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
              throw new RuntimeException("No matching target method found: removeLogDeleter");
          target.setAccessible(true);
          target.invoke(this, arg0);
          return;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public void startResourceLocalization_bridge(java.lang.String arg0, org.apache.hadoop.yarn.api.records.ApplicationIdJVMInterface arg1, java.lang.Object arg2, org.apache.hadoop.fs.PathJVMInterface arg3) throws java.io.IOException {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[4];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              __types[1] = (arg1 != null ? arg1.getClass() : Object.class);
              __types[2] = (arg2 != null ? arg2.getClass() : Object.class);
              __types[3] = (arg3 != null ? arg3.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("startResourceLocalization", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("startResourceLocalization"))
                      continue;
                  if (m.getParameterCount() != 4)
                      continue;
                  if (m.getReturnType().getName().equals("void"))
                      continue;
                  target = m;
                  break;
              }
          }
          if (target == null)
              throw new RuntimeException("No matching target method found: startResourceLocalization");
          target.setAccessible(true);
          target.invoke(this, arg0, arg1, arg2, arg3);
          return;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public void storeAMRMProxyNextMasterKey_bridge(java.lang.Object arg0) throws java.io.IOException {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[1];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("storeAMRMProxyNextMasterKey", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("storeAMRMProxyNextMasterKey"))
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
              throw new RuntimeException("No matching target method found: storeAMRMProxyNextMasterKey");
          target.setAccessible(true);
          target.invoke(this, arg0);
          return;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public void removeContainerPaused_bridge(org.apache.hadoop.yarn.api.records.ContainerIdJVMInterface arg0) throws java.io.IOException {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[1];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("removeContainerPaused", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("removeContainerPaused"))
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
              throw new RuntimeException("No matching target method found: removeContainerPaused");
          target.setAccessible(true);
          target.invoke(this, arg0);
          return;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public void removeLocalizedResource_bridge(java.lang.String arg0, org.apache.hadoop.yarn.api.records.ApplicationIdJVMInterface arg1, org.apache.hadoop.fs.PathJVMInterface arg2) throws java.io.IOException {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[3];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              __types[1] = (arg1 != null ? arg1.getClass() : Object.class);
              __types[2] = (arg2 != null ? arg2.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("removeLocalizedResource", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("removeLocalizedResource"))
                      continue;
                  if (m.getParameterCount() != 3)
                      continue;
                  if (m.getReturnType().getName().equals("void"))
                      continue;
                  target = m;
                  break;
              }
          }
          if (target == null)
              throw new RuntimeException("No matching target method found: removeLocalizedResource");
          target.setAccessible(true);
          target.invoke(this, arg0, arg1, arg2);
          return;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public void storeContainer_bridge(org.apache.hadoop.yarn.api.records.ContainerIdJVMInterface arg0, int arg1, long arg2, org.apache.hadoop.yarn.api.protocolrecords.StartContainerRequestJVMInterface arg3) throws java.io.IOException {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[4];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              __types[1] = int.class;
              __types[2] = long.class;
              __types[3] = (arg3 != null ? arg3.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("storeContainer", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("storeContainer"))
                      continue;
                  if (m.getParameterCount() != 4)
                      continue;
                  if (m.getReturnType().getName().equals("void"))
                      continue;
                  target = m;
                  break;
              }
          }
          if (target == null)
              throw new RuntimeException("No matching target method found: storeContainer");
          target.setAccessible(true);
          target.invoke(this, arg0, arg1, arg2, arg3);
          return;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public void storeContainerTokenCurrentMasterKey_bridge(java.lang.Object arg0) throws java.io.IOException {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[1];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("storeContainerTokenCurrentMasterKey", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("storeContainerTokenCurrentMasterKey"))
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
              throw new RuntimeException("No matching target method found: storeContainerTokenCurrentMasterKey");
          target.setAccessible(true);
          target.invoke(this, arg0);
          return;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public void storeContainerUpdateToken_bridge(org.apache.hadoop.yarn.api.records.ContainerIdJVMInterface arg0, org.apache.hadoop.yarn.security.ContainerTokenIdentifierJVMInterface arg1) throws java.io.IOException {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[2];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              __types[1] = (arg1 != null ? arg1.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("storeContainerUpdateToken", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("storeContainerUpdateToken"))
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
              throw new RuntimeException("No matching target method found: storeContainerUpdateToken");
          target.setAccessible(true);
          target.invoke(this, arg0, arg1);
          return;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public void storeAMRMProxyAppContextEntry_bridge(org.apache.hadoop.yarn.api.records.ApplicationAttemptIdJVMInterface arg0, java.lang.String arg1, byte[] arg2) throws java.io.IOException {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[3];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              __types[1] = (arg1 != null ? arg1.getClass() : Object.class);
              __types[2] = (arg2 != null ? arg2.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("storeAMRMProxyAppContextEntry", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("storeAMRMProxyAppContextEntry"))
                      continue;
                  if (m.getParameterCount() != 3)
                      continue;
                  if (m.getReturnType().getName().equals("void"))
                      continue;
                  target = m;
                  break;
              }
          }
          if (target == null)
              throw new RuntimeException("No matching target method found: storeAMRMProxyAppContextEntry");
          target.setAccessible(true);
          target.invoke(this, arg0, arg1, arg2);
          return;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
}

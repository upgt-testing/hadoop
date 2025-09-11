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

package org.apache.hadoop.yarn.server.nodemanager.security;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.yarn.server.nodemanager.recovery.RecoveryIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.hadoop.classification.InterfaceAudience.Private;
import org.apache.hadoop.yarn.api.records.ApplicationAttemptId;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.api.records.Container;
import org.apache.hadoop.yarn.api.records.NMToken;
import org.apache.hadoop.yarn.api.records.NodeId;
import org.apache.hadoop.yarn.api.records.Token;
import org.apache.hadoop.yarn.security.NMTokenIdentifier;
import org.apache.hadoop.yarn.server.api.records.MasterKey;
import org.apache.hadoop.yarn.server.nodemanager.recovery.NMNullStateStoreService;
import org.apache.hadoop.yarn.server.nodemanager.recovery.NMStateStoreService;
import org.apache.hadoop.yarn.server.nodemanager.recovery.NMStateStoreService.RecoveredNMTokensState;
import org.apache.hadoop.yarn.server.security.BaseNMTokenSecretManager;
import org.apache.hadoop.yarn.server.security.MasterKeyData;

import com.google.common.annotations.VisibleForTesting;

public class NMTokenSecretManagerInNM extends BaseNMTokenSecretManager implements NMTokenSecretManagerInNMJVMInterface {

  private static final Logger LOG =
       LoggerFactory.getLogger(NMTokenSecretManagerInNM.class);
  
  private MasterKeyData previousMasterKey;
  
  private final Map<ApplicationAttemptId, MasterKeyData> oldMasterKeys;
  private final Map<ApplicationId, List<ApplicationAttemptId>> appToAppAttemptMap;
  private final NMStateStoreService stateStore;
  private NodeId nodeId;
  
  public NMTokenSecretManagerInNM() {
    this(new NMNullStateStoreService());
  }

  public NMTokenSecretManagerInNM(NMStateStoreService stateStore) {
    this.oldMasterKeys =
        new HashMap<ApplicationAttemptId, MasterKeyData>();
    appToAppAttemptMap =         
        new HashMap<ApplicationId, List<ApplicationAttemptId>>();
    this.stateStore = stateStore;
  }
  
  public synchronized void recover()
      throws IOException {
    RecoveredNMTokensState state = stateStore.loadNMTokensState();
    MasterKey key = state.getCurrentMasterKey();
    if (key != null) {
      super.currentMasterKey =
          new MasterKeyData(key, createSecretKey(key.getBytes().array()));
    }

    key = state.getPreviousMasterKey();
    if (key != null) {
      previousMasterKey =
          new MasterKeyData(key, createSecretKey(key.getBytes().array()));
    }

    // restore the serial number from the current master key
    if (super.currentMasterKey != null) {
      super.serialNo = super.currentMasterKey.getMasterKey().getKeyId() + 1;
    }

    try (RecoveryIterator<Map.Entry<ApplicationAttemptId, MasterKey>> it =
             state.getIterator()) {
      while (it.hasNext()) {
        Map.Entry<ApplicationAttemptId, MasterKey> entry = it.next();
        key = entry.getValue();
        oldMasterKeys.put(entry.getKey(),
            new MasterKeyData(key, createSecretKey(key.getBytes().array())));
      }
    }

    // reconstruct app to app attempts map
    appToAppAttemptMap.clear();
    for (ApplicationAttemptId attempt : oldMasterKeys.keySet()) {
      ApplicationId app = attempt.getApplicationId();
      List<ApplicationAttemptId> attempts = appToAppAttemptMap.get(app);
      if (attempts == null) {
        attempts = new ArrayList<ApplicationAttemptId>();
        appToAppAttemptMap.put(app, attempts);
      }
      attempts.add(attempt);
    }
  }

  private void updateCurrentMasterKey(MasterKeyData key) {
    super.currentMasterKey = key;
    try {
      stateStore.storeNMTokenCurrentMasterKey(key.getMasterKey());
    } catch (IOException e) {
      LOG.error("Unable to update current master key in state store", e);
    }
  }

  private void updatePreviousMasterKey(MasterKeyData key) {
    previousMasterKey = key;
    try {
      stateStore.storeNMTokenPreviousMasterKey(key.getMasterKey());
    } catch (IOException e) {
      LOG.error("Unable to update previous master key in state store", e);
    }
  }

  /**
   * Used by NodeManagers to create a token-secret-manager with the key
   * obtained from the RM. This can happen during registration or when the RM
   * rolls the master-key and signal the NM.
   */
  @Private
  public synchronized void setMasterKey(MasterKey masterKey) {
    // Update keys only if the key has changed.
    if (super.currentMasterKey == null || super.currentMasterKey.getMasterKey()
          .getKeyId() != masterKey.getKeyId()) {
      LOG.info("Rolling master-key for container-tokens, got key with id "
          + masterKey.getKeyId());
      if (super.currentMasterKey != null) {
        updatePreviousMasterKey(super.currentMasterKey);
      }
      updateCurrentMasterKey(new MasterKeyData(masterKey,
          createSecretKey(masterKey.getBytes().array())));
    }
  }

  /**
   * This method will be used to verify NMTokens generated by different master
   * keys.
   */
  @Override
  public synchronized byte[] retrievePassword(NMTokenIdentifier identifier)
      throws InvalidToken {
    int keyId = identifier.getKeyId();
    ApplicationAttemptId appAttemptId = identifier.getApplicationAttemptId();

    /*
     * MasterKey used for retrieving password will be as follows. 1) By default
     * older saved master key will be used. 2) If identifier's master key id
     * matches that of previous master key id then previous key will be used. 3)
     * If identifier's master key id matches that of current master key id then
     * current key will be used.
     */
    MasterKeyData oldMasterKey = oldMasterKeys.get(appAttemptId);
    MasterKeyData masterKeyToUse = oldMasterKey;
    if (previousMasterKey != null
        && keyId == previousMasterKey.getMasterKey().getKeyId()) {
      masterKeyToUse = previousMasterKey;
    } else if (keyId == currentMasterKey.getMasterKey().getKeyId()) {
      masterKeyToUse = currentMasterKey;
    }
    
    if (nodeId != null && !identifier.getNodeId().equals(nodeId)) {
      throw new InvalidToken("Given NMToken for application : "
          + appAttemptId.toString() + " is not valid for current node manager."
          + "expected : " + nodeId.toString() + " found : "
          + identifier.getNodeId().toString());
    }
    
    if (masterKeyToUse != null) {
      byte[] password = retrivePasswordInternal(identifier, masterKeyToUse);
      LOG.debug("NMToken password retrieved successfully!!");
      return password;
    }

    throw new InvalidToken("Given NMToken for application : "
        + appAttemptId.toString() + " seems to have been generated illegally.");
  }

  public synchronized void appFinished(ApplicationId appId) {
    List<ApplicationAttemptId> appAttemptList = appToAppAttemptMap.get(appId);
    if (appAttemptList != null) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Removing application attempts NMToken keys for application "
            + appId);
      }
      for (ApplicationAttemptId appAttemptId : appAttemptList) {
        removeAppAttemptKey(appAttemptId);
      }
      appToAppAttemptMap.remove(appId);
    } else {
      LOG.error("No application Attempt for application : " + appId
          + " started on this NM.");
    }
  }

  /**
   * This will be called by startContainer. It will add the master key into
   * the cache used for starting this container. This should be called before
   * validating the startContainer request.
   */
  public synchronized void appAttemptStartContainer(
      NMTokenIdentifier identifier)
      throws org.apache.hadoop.security.token.SecretManager.InvalidToken {
    ApplicationAttemptId appAttemptId = identifier.getApplicationAttemptId();
    if (!appToAppAttemptMap.containsKey(appAttemptId.getApplicationId())) {
      // First application attempt for the given application
      appToAppAttemptMap.put(appAttemptId.getApplicationId(),
        new ArrayList<ApplicationAttemptId>());
    }
    MasterKeyData oldKey = oldMasterKeys.get(appAttemptId);

    if (oldKey == null) {
      // This is a new application attempt.
      appToAppAttemptMap.get(appAttemptId.getApplicationId()).add(appAttemptId);
    }
    if (oldKey == null
        || oldKey.getMasterKey().getKeyId() != identifier.getKeyId()) {
      // Update key only if it is modified.
      if (LOG.isDebugEnabled()) {
        LOG.debug("NMToken key updated for application attempt : "
            + identifier.getApplicationAttemptId().toString());
      }
      if (identifier.getKeyId() == currentMasterKey.getMasterKey()
        .getKeyId()) {
        updateAppAttemptKey(appAttemptId, currentMasterKey);
      } else if (previousMasterKey != null
          && identifier.getKeyId() == previousMasterKey.getMasterKey()
            .getKeyId()) {
        updateAppAttemptKey(appAttemptId, previousMasterKey);
      } else {
        throw new InvalidToken(
          "Older NMToken should not be used while starting the container.");
      }
    }
  }
  
  public synchronized void setNodeId(NodeId nodeId) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("updating nodeId : " + nodeId);
    }
    this.nodeId = nodeId;
  }
  
  @Private
  @VisibleForTesting
  public synchronized boolean
      isAppAttemptNMTokenKeyPresent(ApplicationAttemptId appAttemptId) {
    return oldMasterKeys.containsKey(appAttemptId);
  }
  
  @Private
  @VisibleForTesting
  public synchronized NodeId getNodeId() {
    return this.nodeId;
  }

  private void updateAppAttemptKey(ApplicationAttemptId attempt,
      MasterKeyData key) {
    this.oldMasterKeys.put(attempt, key);
    try {
      stateStore.storeNMTokenApplicationMasterKey(attempt,
          key.getMasterKey());
    } catch (IOException e) {
      LOG.error("Unable to store master key for application " + attempt, e);
    }
  }

  private void removeAppAttemptKey(ApplicationAttemptId attempt) {
    this.oldMasterKeys.remove(attempt);
    try {
      stateStore.removeNMTokenApplicationMasterKey(attempt);
    } catch (IOException e) {
      LOG.error("Unable to remove master key for application " + attempt, e);
    }
  }

  /**
   * Used by the Distributed Scheduler framework to generate NMTokens
   * @param applicationSubmitter
   * @param container
   * @return NMToken
   */
  public NMToken generateNMToken(
      String applicationSubmitter, Container container) {
    this.readLock.lock();
    try {
      Token token =
          createNMToken(container.getId().getApplicationAttemptId(),
              container.getNodeId(), applicationSubmitter);
      return NMToken.newInstance(container.getNodeId(), token);
    } finally {
      this.readLock.unlock();
    }
  }
  
  public void appAttemptStartContainer_bridge(org.apache.hadoop.yarn.security.NMTokenIdentifierJVMInterface arg0) throws org.apache.hadoop.security.token.SecretManager.InvalidToken {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[1];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("appAttemptStartContainer", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("appAttemptStartContainer"))
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
              throw new RuntimeException("No matching target method found: appAttemptStartContainer");
          target.setAccessible(true);
          target.invoke(this, arg0);
          return;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public byte[] retrievePassword_bridge(org.apache.hadoop.yarn.security.NMTokenIdentifierJVMInterface arg0) throws org.apache.hadoop.security.token.SecretManager.InvalidToken {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[1];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("retrievePassword", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("retrievePassword"))
                      continue;
                  if (m.getParameterCount() != 1)
                      continue;
                  if (m.getReturnType().getName().equals("byte[]"))
                      continue;
                  target = m;
                  break;
              }
          }
          if (target == null)
              throw new RuntimeException("No matching target method found: retrievePassword");
          target.setAccessible(true);
          byte[] __result = (byte[]) target.invoke(this, arg0);
          return __result;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public org.apache.hadoop.yarn.api.records.NMTokenJVMInterface generateNMToken_bridge(java.lang.String arg0, org.apache.hadoop.yarn.api.records.ContainerJVMInterface arg1) {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[2];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              __types[1] = (arg1 != null ? arg1.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("generateNMToken", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("generateNMToken"))
                      continue;
                  if (m.getParameterCount() != 2)
                      continue;
                  if (m.getReturnType().getName().equals("org.apache.hadoop.yarn.api.records.NMTokenJVMInterface"))
                      continue;
                  target = m;
                  break;
              }
          }
          if (target == null)
              throw new RuntimeException("No matching target method found: generateNMToken");
          target.setAccessible(true);
          org.apache.hadoop.yarn.api.records.NMTokenJVMInterface __result = (org.apache.hadoop.yarn.api.records.NMTokenJVMInterface) target.invoke(this, arg0, arg1);
          return __result;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public boolean isAppAttemptNMTokenKeyPresent_bridge(org.apache.hadoop.yarn.api.records.ApplicationAttemptIdJVMInterface arg0) {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[1];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("isAppAttemptNMTokenKeyPresent", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("isAppAttemptNMTokenKeyPresent"))
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
              throw new RuntimeException("No matching target method found: isAppAttemptNMTokenKeyPresent");
          target.setAccessible(true);
          boolean __result = (boolean) target.invoke(this, arg0);
          return __result;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public void setNodeId_bridge(org.apache.hadoop.yarn.api.records.NodeIdJVMInterface arg0) {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[1];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("setNodeId", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("setNodeId"))
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
              throw new RuntimeException("No matching target method found: setNodeId");
          target.setAccessible(true);
          target.invoke(this, arg0);
          return;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public void setMasterKey_bridge(java.lang.Object arg0) {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[1];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("setMasterKey", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("setMasterKey"))
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
              throw new RuntimeException("No matching target method found: setMasterKey");
          target.setAccessible(true);
          target.invoke(this, arg0);
          return;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public void appFinished_bridge(org.apache.hadoop.yarn.api.records.ApplicationIdJVMInterface arg0) {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[1];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("appFinished", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("appFinished"))
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
              throw new RuntimeException("No matching target method found: appFinished");
          target.setAccessible(true);
          target.invoke(this, arg0);
          return;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
}

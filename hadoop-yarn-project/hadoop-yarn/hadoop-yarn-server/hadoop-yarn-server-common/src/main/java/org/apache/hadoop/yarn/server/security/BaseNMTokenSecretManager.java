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

import java.net.InetSocketAddress;
import java.security.SecureRandom;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.hadoop.classification.InterfaceAudience.Private;
import org.apache.hadoop.net.NetUtils;
import org.apache.hadoop.security.SecurityUtil;
import org.apache.hadoop.security.token.SecretManager;
import org.apache.hadoop.yarn.api.records.ApplicationAttemptId;
import org.apache.hadoop.yarn.api.records.NodeId;
import org.apache.hadoop.yarn.api.records.Token;
import org.apache.hadoop.yarn.security.NMTokenIdentifier;
import org.apache.hadoop.yarn.server.api.records.MasterKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BaseNMTokenSecretManager extends
    SecretManager<NMTokenIdentifier> implements BaseNMTokenSecretManagerJVMInterface {

  private static final Logger LOG =
      LoggerFactory.getLogger(BaseNMTokenSecretManager.class);

  protected int serialNo = new SecureRandom().nextInt();

  protected final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
  protected final Lock readLock = readWriteLock.readLock();
  protected final Lock writeLock = readWriteLock.writeLock();

  protected MasterKeyData currentMasterKey;
  
  protected MasterKeyData createNewMasterKey() {
    this.writeLock.lock();
    try {
      return new MasterKeyData(serialNo++, generateSecret());
    } finally {
      this.writeLock.unlock();
    }
  }

  @Private
  public MasterKey getCurrentKey() {
    this.readLock.lock();
    try {
      return this.currentMasterKey.getMasterKey();
    } finally {
      this.readLock.unlock();
    }
  }

  @Override
  protected byte[] createPassword(NMTokenIdentifier identifier) {
    LOG.debug("creating password for {} for user {} to run on NM {}",
        identifier.getApplicationAttemptId(),
        identifier.getApplicationSubmitter(), identifier.getNodeId());
    readLock.lock();
    try {
      return createPassword(identifier.getBytes(),
          currentMasterKey.getSecretKey());
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public byte[] retrievePassword(NMTokenIdentifier identifier)
      throws org.apache.hadoop.security.token.SecretManager.InvalidToken {
    readLock.lock();
    try {
      return retrivePasswordInternal(identifier, currentMasterKey);
    } finally {
      readLock.unlock();
    }
  }

  protected byte[] retrivePasswordInternal(NMTokenIdentifier identifier,
      MasterKeyData masterKey) {
    LOG.debug("retriving password for {} for user {} to run on NM {}",
        identifier.getApplicationAttemptId(),
        identifier.getApplicationSubmitter(), identifier.getNodeId());
    return createPassword(identifier.getBytes(), masterKey.getSecretKey());
  }
  /**
   * It is required for RPC
   */
  @Override
  public NMTokenIdentifier createIdentifier() {
    return new NMTokenIdentifier();
  }
  
  /**
   * Helper function for creating NMTokens.
   */
  public Token createNMToken(ApplicationAttemptId applicationAttemptId,
      NodeId nodeId, String applicationSubmitter) {
    byte[] password;
    NMTokenIdentifier identifier;
    
    this.readLock.lock();
    try {
      identifier =
          new NMTokenIdentifier(applicationAttemptId, nodeId,
              applicationSubmitter, this.currentMasterKey.getMasterKey()
                  .getKeyId());
      password = this.createPassword(identifier);
    } finally {
      this.readLock.unlock();
    }
    return newInstance(password, identifier);
  }
  
  public static Token newInstance(byte[] password,
      NMTokenIdentifier identifier) {
    NodeId nodeId = identifier.getNodeId();
    // RPC layer client expects ip:port as service for tokens
    InetSocketAddress addr =
        NetUtils.createSocketAddrForHost(nodeId.getHost(), nodeId.getPort());
    Token nmToken =
        Token.newInstance(identifier.getBytes(),
          NMTokenIdentifier.KIND.toString(), password, SecurityUtil
            .buildTokenService(addr).toString());
    return nmToken;
  }
  
  public org.apache.hadoop.yarn.api.records.TokenJVMInterface createNMToken_bridge(org.apache.hadoop.yarn.api.records.ApplicationAttemptIdJVMInterface arg0, org.apache.hadoop.yarn.api.records.NodeIdJVMInterface arg1, java.lang.String arg2) {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[3];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              __types[1] = (arg1 != null ? arg1.getClass() : Object.class);
              __types[2] = (arg2 != null ? arg2.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("createNMToken", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("createNMToken"))
                      continue;
                  if (m.getParameterCount() != 3)
                      continue;
                  if (m.getReturnType().getName().equals("org.apache.hadoop.yarn.api.records.TokenJVMInterface"))
                      continue;
                  target = m;
                  break;
              }
          }
          if (target == null)
              throw new RuntimeException("No matching target method found: createNMToken");
          target.setAccessible(true);
          org.apache.hadoop.yarn.api.records.TokenJVMInterface __result = (org.apache.hadoop.yarn.api.records.TokenJVMInterface) target.invoke(this, arg0, arg1, arg2);
          return __result;
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
}

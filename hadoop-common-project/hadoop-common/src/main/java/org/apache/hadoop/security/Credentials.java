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

package org.apache.hadoop.security;

import com.google.protobuf.ByteString;

import java.io.BufferedInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.classification.InterfaceAudience;
import org.apache.hadoop.classification.InterfaceStability;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableUtils;
import org.apache.hadoop.security.token.Token;
import org.apache.hadoop.security.token.TokenIdentifier;
import org.apache.hadoop.security.proto.SecurityProtos.CredentialsKVProto;
import org.apache.hadoop.security.proto.SecurityProtos.CredentialsProto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A class that provides the facilities of reading and writing
 * secret keys and Tokens.
 */
@InterfaceAudience.Public
@InterfaceStability.Evolving
public class Credentials implements Writable, CredentialsJVMInterface {
  public enum SerializedFormat {
    WRITABLE((byte) 0x00),
    PROTOBUF((byte) 0x01);

    // Caching to avoid reconstructing the array each time.
    private static final SerializedFormat[] FORMATS = values();

    final byte value;

    SerializedFormat(byte val) {
      this.value = val;
    }

    public static SerializedFormat valueOf(int val) {
      try {
        return FORMATS[val];
      } catch (ArrayIndexOutOfBoundsException e) {
        throw new IllegalArgumentException("Unknown credential format: " + val);
      }
    }
  }

  private static final Logger LOG = LoggerFactory.getLogger(Credentials.class);

  private  Map<Text, byte[]> secretKeysMap = new HashMap<Text, byte[]>();
  private  Map<Text, Token<? extends TokenIdentifier>> tokenMap =
      new HashMap<Text, Token<? extends TokenIdentifier>>();

  /**
   * Create an empty credentials instance.
   */
  public Credentials() {
  }

  /**
   * Create a copy of the given credentials.
   * @param credentials to copy
   */
  public Credentials(Credentials credentials) {
    this.addAll(credentials);
  }

  /**
   * Returns the Token object for the alias.
   * @param alias the alias for the Token
   * @return token for this alias
   */
  public Token<? extends TokenIdentifier> getToken(Text alias) {
    return tokenMap.get(alias);
  }

  /**
   * Add a token in the storage (in memory).
   * @param alias the alias for the key
   * @param t the token object
   */
  public void addToken(Text alias, Token<? extends TokenIdentifier> t) {
    if (t == null) {
      LOG.warn("Null token ignored for " + alias);
    } else if (tokenMap.put(alias, t) != null) {
      // Update private tokens
      Map<Text, Token<? extends TokenIdentifier>> tokensToAdd =
          new HashMap<>();
      for (Map.Entry<Text, Token<? extends TokenIdentifier>> e :
          tokenMap.entrySet()) {
        Token<? extends TokenIdentifier> token = e.getValue();
        if (token.isPrivateCloneOf(alias)) {
          tokensToAdd.put(e.getKey(), t.privateClone(token.getService()));
        }
      }
      tokenMap.putAll(tokensToAdd);
    }
  }

  /**
   * Return all the tokens in the in-memory map.
   */
  public Collection<Token<? extends TokenIdentifier>> getAllTokens() {
    return tokenMap.values();
  }

  /**
   * @return number of Tokens in the in-memory map
   */
  public int numberOfTokens() {
    return tokenMap.size();
  }

  /**
   * Returns the key bytes for the alias.
   * @param alias the alias for the key
   * @return key for this alias
   */
  public byte[] getSecretKey(Text alias) {
    return secretKeysMap.get(alias);
  }

  /**
   * @return number of keys in the in-memory map
   */
  public int numberOfSecretKeys() {
    return secretKeysMap.size();
  }

  /**
   * Set the key for an alias.
   * @param alias the alias for the key
   * @param key the key bytes
   */
  public void addSecretKey(Text alias, byte[] key) {
    secretKeysMap.put(alias, key);
  }

  /**
   * Remove the key for a given alias.
   * @param alias the alias for the key
   */
  public void removeSecretKey(Text alias) {
    secretKeysMap.remove(alias);
  }

  /**
   * Return all the secret key entries in the in-memory map.
   */
  public List<Text> getAllSecretKeys() {
    List<Text> list = new java.util.ArrayList<Text>();
    list.addAll(secretKeysMap.keySet());

    return list;
  }

  /**
   * Convenience method for reading a token storage file and loading its Tokens.
   * @param filename
   * @param conf
   * @throws IOException
   */
  public static Credentials readTokenStorageFile(Path filename,
                                                 Configuration conf)
  throws IOException {
    FSDataInputStream in = null;
    Credentials credentials = new Credentials();
    try {
      in = filename.getFileSystem(conf).open(filename);
      credentials.readTokenStorageStream(in);
      in.close();
      return credentials;
    } catch(IOException ioe) {
      throw new IOException("Exception reading " + filename, ioe);
    } finally {
      IOUtils.cleanupWithLogger(LOG, in);
    }
  }

  /**
   * Convenience method for reading a token storage file and loading its Tokens.
   * @param filename
   * @param conf
   * @throws IOException
   */
  public static Credentials readTokenStorageFile(File filename,
                                                 Configuration conf)
      throws IOException {
    DataInputStream in = null;
    Credentials credentials = new Credentials();
    try {
      in = new DataInputStream(new BufferedInputStream(
          new FileInputStream(filename)));
      credentials.readTokenStorageStream(in);
      return credentials;
    } catch(IOException ioe) {
      throw new IOException("Exception reading " + filename, ioe);
    } finally {
      IOUtils.cleanupWithLogger(LOG, in);
    }
  }

  /**
   * Convenience method for reading a token from a DataInputStream.
   */
  public void readTokenStorageStream(DataInputStream in) throws IOException {
    byte[] magic = new byte[TOKEN_STORAGE_MAGIC.length];
    in.readFully(magic);
    if (!Arrays.equals(magic, TOKEN_STORAGE_MAGIC)) {
      throw new IOException("Bad header found in token storage.");
    }
    SerializedFormat format;
    try {
      format = SerializedFormat.valueOf(in.readByte());
    } catch (IllegalArgumentException e) {
      throw new IOException(e);
    }
    switch (format) {
    case WRITABLE:
      readFields(in);
      break;
    case PROTOBUF:
      readProto(in);
      break;
    default:
      throw new IOException("Unsupported format " + format);
    }
  }

  private static final byte[] TOKEN_STORAGE_MAGIC =
      "HDTS".getBytes(StandardCharsets.UTF_8);

  public void writeTokenStorageToStream(DataOutputStream os)
      throws IOException {
    // by default store in the oldest supported format for compatibility
    writeTokenStorageToStream(os, SerializedFormat.WRITABLE);
  }

  public void writeTokenStorageToStream(DataOutputStream os,
      SerializedFormat format) throws IOException {
    switch (format) {
    case WRITABLE:
      writeWritableOutputStream(os);
      break;
    case PROTOBUF:
      writeProtobufOutputStream(os);
      break;
    default:
      throw new IllegalArgumentException("Unsupported serialized format: "
          + format);
    }
  }

  private void writeWritableOutputStream(DataOutputStream os)
      throws IOException {
    os.write(TOKEN_STORAGE_MAGIC);
    os.write(SerializedFormat.WRITABLE.value);
    write(os);
  }

  private void writeProtobufOutputStream(DataOutputStream os)
      throws IOException {
    os.write(TOKEN_STORAGE_MAGIC);
    os.write(SerializedFormat.PROTOBUF.value);
    writeProto(os);
  }

  public void writeTokenStorageFile(Path filename,
                                    Configuration conf) throws IOException {
    // by default store in the oldest supported format for compatibility
    writeTokenStorageFile(filename, conf, SerializedFormat.WRITABLE);
  }

  public void writeTokenStorageFile(Path filename, Configuration conf,
      SerializedFormat format) throws IOException {
    try (FSDataOutputStream os =
             filename.getFileSystem(conf).create(filename)) {
      writeTokenStorageToStream(os, format);
    }
  }

  /**
   * Stores all the keys to DataOutput.
   * @param out
   * @throws IOException
   */
  @Override
  public void write(DataOutput out) throws IOException {
    // write out tokens first
    WritableUtils.writeVInt(out, tokenMap.size());
    for(Map.Entry<Text,
            Token<? extends TokenIdentifier>> e: tokenMap.entrySet()) {
      e.getKey().write(out);
      e.getValue().write(out);
    }

    // now write out secret keys
    WritableUtils.writeVInt(out, secretKeysMap.size());
    for(Map.Entry<Text, byte[]> e : secretKeysMap.entrySet()) {
      e.getKey().write(out);
      WritableUtils.writeVInt(out, e.getValue().length);
      out.write(e.getValue());
    }
  }

  /**
   * Write contents of this instance as CredentialsProto message to DataOutput.
   * @param out
   * @throws IOException
   */
  void writeProto(DataOutput out) throws IOException {
    CredentialsProto.Builder storage = CredentialsProto.newBuilder();
    for (Map.Entry<Text, Token<? extends TokenIdentifier>> e :
                                                         tokenMap.entrySet()) {
      CredentialsKVProto.Builder kv = CredentialsKVProto.newBuilder().
          setAliasBytes(ByteString.copyFrom(
              e.getKey().getBytes(), 0, e.getKey().getLength())).
          setToken(e.getValue().toTokenProto());
      storage.addTokens(kv.build());
    }

    for(Map.Entry<Text, byte[]> e : secretKeysMap.entrySet()) {
      CredentialsKVProto.Builder kv = CredentialsKVProto.newBuilder().
          setAliasBytes(ByteString.copyFrom(
              e.getKey().getBytes(), 0, e.getKey().getLength())).
          setSecret(ByteString.copyFrom(e.getValue()));
      storage.addSecrets(kv.build());
    }
    storage.build().writeDelimitedTo((DataOutputStream)out);
  }

  /**
   * Populates keys/values from proto buffer storage.
   * @param in - stream ready to read a serialized proto buffer message
   */
  void readProto(DataInput in) throws IOException {
    CredentialsProto storage = CredentialsProto.parseDelimitedFrom((DataInputStream)in);
    for (CredentialsKVProto kv : storage.getTokensList()) {
      addToken(new Text(kv.getAliasBytes().toByteArray()),
               (Token<? extends TokenIdentifier>) new Token(kv.getToken()));
    }
    for (CredentialsKVProto kv : storage.getSecretsList()) {
      addSecretKey(new Text(kv.getAliasBytes().toByteArray()),
                   kv.getSecret().toByteArray());
    }
  }

  /**
   * Loads all the keys.
   * @param in
   * @throws IOException
   */
  @Override
  public void readFields(DataInput in) throws IOException {
    secretKeysMap.clear();
    tokenMap.clear();

    int size = WritableUtils.readVInt(in);
    for(int i=0; i<size; i++) {
      Text alias = new Text();
      alias.readFields(in);
      Token<? extends TokenIdentifier> t = new Token<TokenIdentifier>();
      t.readFields(in);
      tokenMap.put(alias, t);
    }

    size = WritableUtils.readVInt(in);
    for(int i=0; i<size; i++) {
      Text alias = new Text();
      alias.readFields(in);
      int len = WritableUtils.readVInt(in);
      byte[] value = new byte[len];
      in.readFully(value);
      secretKeysMap.put(alias, value);
    }
  }

  /**
   * Copy all of the credentials from one credential object into another.
   * Existing secrets and tokens are overwritten.
   * @param other the credentials to copy
   */
  public void addAll(Credentials other) {
    addAll(other, true);
  }

  /**
   * Copy all of the credentials from one credential object into another.
   * Existing secrets and tokens are not overwritten.
   * @param other the credentials to copy
   */
  public void mergeAll(Credentials other) {
    addAll(other, false);
  }

  private void addAll(Credentials other, boolean overwrite) {
    for(Map.Entry<Text, byte[]> secret: other.secretKeysMap.entrySet()) {
      Text key = secret.getKey();
      if (!secretKeysMap.containsKey(key) || overwrite) {
        secretKeysMap.put(key, secret.getValue());
      }
    }
    for(Map.Entry<Text, Token<?>> token: other.tokenMap.entrySet()){
      Text key = token.getKey();
      if (!tokenMap.containsKey(key) || overwrite) {
        addToken(key, token.getValue());
      }
    }
  }
  
  public void addSecretKey_bridge(org.apache.hadoop.io.TextJVMInterface arg0, byte[] arg1) {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[2];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              __types[1] = (arg1 != null ? arg1.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("addSecretKey", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("addSecretKey"))
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
              throw new RuntimeException("No matching target method found: addSecretKey");
          target.setAccessible(true);
          target.invoke(this, arg0, arg1);
          return;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public void addToken_bridge(org.apache.hadoop.io.TextJVMInterface arg0, org.apache.hadoop.security.token.TokenJVMInterface arg1) {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[2];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              __types[1] = (arg1 != null ? arg1.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("addToken", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("addToken"))
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
              throw new RuntimeException("No matching target method found: addToken");
          target.setAccessible(true);
          target.invoke(this, arg0, arg1);
          return;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public void removeSecretKey_bridge(org.apache.hadoop.io.TextJVMInterface arg0) {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[1];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("removeSecretKey", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("removeSecretKey"))
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
              throw new RuntimeException("No matching target method found: removeSecretKey");
          target.setAccessible(true);
          target.invoke(this, arg0);
          return;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public void writeTokenStorageFile_bridge(org.apache.hadoop.fs.PathJVMInterface arg0, org.apache.hadoop.conf.ConfigurationJVMInterface arg1) throws java.io.IOException {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[2];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              __types[1] = (arg1 != null ? arg1.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("writeTokenStorageFile", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("writeTokenStorageFile"))
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
              throw new RuntimeException("No matching target method found: writeTokenStorageFile");
          target.setAccessible(true);
          target.invoke(this, arg0, arg1);
          return;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public void readFields_bridge(java.lang.Object arg0) throws java.io.IOException {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[1];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("readFields", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("readFields"))
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
              throw new RuntimeException("No matching target method found: readFields");
          target.setAccessible(true);
          target.invoke(this, arg0);
          return;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public org.apache.hadoop.security.token.TokenJVMInterface getToken_bridge(org.apache.hadoop.io.TextJVMInterface arg0) {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[1];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("getToken", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("getToken"))
                      continue;
                  if (m.getParameterCount() != 1)
                      continue;
                  if (m.getReturnType().getName().equals("org.apache.hadoop.security.token.TokenJVMInterface"))
                      continue;
                  target = m;
                  break;
              }
          }
          if (target == null)
              throw new RuntimeException("No matching target method found: getToken");
          target.setAccessible(true);
          org.apache.hadoop.security.token.TokenJVMInterface __result = (org.apache.hadoop.security.token.TokenJVMInterface) target.invoke(this, arg0);
          return __result;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public void writeTokenStorageFile_bridge(org.apache.hadoop.fs.PathJVMInterface arg0, org.apache.hadoop.conf.ConfigurationJVMInterface arg1, java.lang.Object arg2) throws java.io.IOException {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[3];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              __types[1] = (arg1 != null ? arg1.getClass() : Object.class);
              __types[2] = (arg2 != null ? arg2.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("writeTokenStorageFile", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("writeTokenStorageFile"))
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
              throw new RuntimeException("No matching target method found: writeTokenStorageFile");
          target.setAccessible(true);
          target.invoke(this, arg0, arg1, arg2);
          return;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public void mergeAll_bridge(org.apache.hadoop.security.CredentialsJVMInterface arg0) {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[1];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("mergeAll", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("mergeAll"))
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
              throw new RuntimeException("No matching target method found: mergeAll");
          target.setAccessible(true);
          target.invoke(this, arg0);
          return;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public void write_bridge(java.lang.Object arg0) throws java.io.IOException {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[1];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("write", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("write"))
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
              throw new RuntimeException("No matching target method found: write");
          target.setAccessible(true);
          target.invoke(this, arg0);
          return;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public byte[] getSecretKey_bridge(org.apache.hadoop.io.TextJVMInterface arg0) {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[1];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("getSecretKey", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("getSecretKey"))
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
              throw new RuntimeException("No matching target method found: getSecretKey");
          target.setAccessible(true);
          byte[] __result = (byte[]) target.invoke(this, arg0);
          return __result;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public void addAll_bridge(org.apache.hadoop.security.CredentialsJVMInterface arg0) {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[1];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("addAll", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("addAll"))
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
              throw new RuntimeException("No matching target method found: addAll");
          target.setAccessible(true);
          target.invoke(this, arg0);
          return;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public void writeTokenStorageToStream_bridge(java.io.DataOutputStream arg0, java.lang.Object arg1) throws java.io.IOException {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[2];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              __types[1] = (arg1 != null ? arg1.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("writeTokenStorageToStream", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("writeTokenStorageToStream"))
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
              throw new RuntimeException("No matching target method found: writeTokenStorageToStream");
          target.setAccessible(true);
          target.invoke(this, arg0, arg1);
          return;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
}

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
package org.apache.hadoop.fs;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.classification.InterfaceAudience;
import org.apache.hadoop.classification.InterfaceStability;
import org.apache.hadoop.io.Writable;

/** This class is used to represent the capacity, free and used space on a
  * {@link FileSystem}.
  */
@InterfaceAudience.Public
@InterfaceStability.Stable
public class FsStatus implements Writable, FsStatusJVMInterface {
  private long capacity;
  private long used;
  private long remaining;

  /** Construct a FsStatus object, using the specified statistics */
  public FsStatus(long capacity, long used, long remaining) {
    this.capacity = capacity;
    this.used = used;
    this.remaining = remaining;
  }

  /** Return the capacity in bytes of the file system */
  public long getCapacity() {
    return capacity;
  }

  /** Return the number of bytes used on the file system */
  public long getUsed() {
    return used;
  }

  /** Return the number of remaining bytes on the file system */
  public long getRemaining() {
    return remaining;
  }

  //////////////////////////////////////////////////
  // Writable
  //////////////////////////////////////////////////
  @Override
  public void write(DataOutput out) throws IOException {
    out.writeLong(capacity);
    out.writeLong(used);
    out.writeLong(remaining);
  }

  @Override
  public void readFields(DataInput in) throws IOException {
    capacity = in.readLong();
    used = in.readLong();
    remaining = in.readLong();
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
}

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

package org.apache.hadoop.metrics2.lib;

import org.apache.hadoop.classification.InterfaceAudience;
import org.apache.hadoop.classification.InterfaceStability;
import org.apache.hadoop.metrics2.MetricsRecordBuilder;

/**
 * The mutable metric interface
 */
@InterfaceAudience.Public
@InterfaceStability.Evolving
public abstract class MutableMetric implements MutableMetricJVMInterface {
  private volatile boolean changed = true;

  /**
   * Get a snapshot of the metric
   * @param builder the metrics record builder
   * @param all if true, snapshot unchanged metrics as well
   */
  public abstract void snapshot(MetricsRecordBuilder builder, boolean all);

  /**
   * Get a snapshot of metric if changed
   * @param builder the metrics record builder
   */
  public void snapshot(MetricsRecordBuilder builder) {
    snapshot(builder, false);
  }

  /**
   * Set the changed flag in mutable operations
   */
  protected void setChanged() { changed = true; }

  /**
   * Clear the changed flag in the snapshot operations
   */
  protected void clearChanged() { changed = false; }

  /**
   * @return  true if metric is changed since last snapshot/snapshot
   */
  public boolean changed() { return changed; }
  
  public void snapshot_bridge(org.apache.hadoop.metrics2.MetricsRecordBuilderJVMInterface arg0, boolean arg1) {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[2];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              __types[1] = boolean.class;
              try {
                  target = this.getClass().getMethod("snapshot", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("snapshot"))
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
              throw new RuntimeException("No matching target method found: snapshot");
          target.setAccessible(true);
          target.invoke(this, arg0, arg1);
          return;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public void snapshot_bridge(org.apache.hadoop.metrics2.MetricsRecordBuilderJVMInterface arg0) {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[1];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("snapshot", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("snapshot"))
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
              throw new RuntimeException("No matching target method found: snapshot");
          target.setAccessible(true);
          target.invoke(this, arg0);
          return;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
}

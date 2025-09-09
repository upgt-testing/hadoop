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

package org.apache.hadoop.metrics2;

import org.apache.hadoop.classification.InterfaceAudience;
import org.apache.hadoop.classification.InterfaceStability;

/**
 * The metrics record builder interface
 */
@InterfaceAudience.Public
@InterfaceStability.Evolving
public abstract class MetricsRecordBuilder implements MetricsRecordBuilderJVMInterface {
  /**
   * Add a metrics value with metrics information
   * @param info  metadata of the tag
   * @param value of the tag
   * @return self
   */
  public abstract MetricsRecordBuilder tag(MetricsInfo info, String value);

  /**
   * Add an immutable metrics tag object
   * @param tag a pre-made tag object (potentially save an object construction)
   * @return self
   */
  public abstract MetricsRecordBuilder add(MetricsTag tag);

  /**
   * Add a pre-made immutable metric object
   * @param metric  the pre-made metric to save an object construction
   * @return self
   */
  public abstract MetricsRecordBuilder add(AbstractMetric metric);

  /**
   * Set the context tag
   * @param value of the context
   * @return self
   */
  public abstract MetricsRecordBuilder setContext(String value);

  /**
   * Add an integer metric
   * @param info  metadata of the metric
   * @param value of the metric
   * @return self
   */
  public abstract MetricsRecordBuilder addCounter(MetricsInfo info, int value);

  /**
   * Add an long metric
   * @param info  metadata of the metric
   * @param value of the metric
   * @return self
   */
  public abstract MetricsRecordBuilder addCounter(MetricsInfo info, long value);

  /**
   * Add a integer gauge metric
   * @param info  metadata of the metric
   * @param value of the metric
   * @return self
   */
  public abstract MetricsRecordBuilder addGauge(MetricsInfo info, int value);

  /**
   * Add a long gauge metric
   * @param info  metadata of the metric
   * @param value of the metric
   * @return self
   */
  public abstract MetricsRecordBuilder addGauge(MetricsInfo info, long value);

  /**
   * Add a float gauge metric
   * @param info  metadata of the metric
   * @param value of the metric
   * @return self
   */
  public abstract MetricsRecordBuilder addGauge(MetricsInfo info, float value);

  /**
   * Add a double gauge metric
   * @param info  metadata of the metric
   * @param value of the metric
   * @return self
   */
  public abstract MetricsRecordBuilder addGauge(MetricsInfo info, double value);

  /**
   * @return the parent metrics collector object
   */
  public abstract MetricsCollector parent();

  /**
   * Syntactic sugar to add multiple records in a collector in a one liner.
   * @return the parent metrics collector object
   */
  public MetricsCollector endRecord() { return parent(); }
  
  public org.apache.hadoop.metrics2.MetricsRecordBuilderJVMInterface addGauge_bridge(java.lang.Object arg0, int arg1) {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[2];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              __types[1] = int.class;
              try {
                  target = this.getClass().getMethod("addGauge", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("addGauge"))
                      continue;
                  if (m.getParameterCount() != 2)
                      continue;
                  if (m.getReturnType().getName().equals("org.apache.hadoop.metrics2.MetricsRecordBuilderJVMInterface"))
                      continue;
                  target = m;
                  break;
              }
          }
          if (target == null)
              throw new RuntimeException("No matching target method found: addGauge");
          target.setAccessible(true);
          org.apache.hadoop.metrics2.MetricsRecordBuilderJVMInterface __result = (org.apache.hadoop.metrics2.MetricsRecordBuilderJVMInterface) target.invoke(this, arg0, arg1);
          return __result;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public org.apache.hadoop.metrics2.MetricsRecordBuilderJVMInterface add_bridge(org.apache.hadoop.metrics2.AbstractMetricJVMInterface arg0) {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[1];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("add", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("add"))
                      continue;
                  if (m.getParameterCount() != 1)
                      continue;
                  if (m.getReturnType().getName().equals("org.apache.hadoop.metrics2.MetricsRecordBuilderJVMInterface"))
                      continue;
                  target = m;
                  break;
              }
          }
          if (target == null)
              throw new RuntimeException("No matching target method found: add");
          target.setAccessible(true);
          org.apache.hadoop.metrics2.MetricsRecordBuilderJVMInterface __result = (org.apache.hadoop.metrics2.MetricsRecordBuilderJVMInterface) target.invoke(this, arg0);
          return __result;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public org.apache.hadoop.metrics2.MetricsRecordBuilderJVMInterface addGauge_bridge(java.lang.Object arg0, long arg1) {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[2];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              __types[1] = long.class;
              try {
                  target = this.getClass().getMethod("addGauge", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("addGauge"))
                      continue;
                  if (m.getParameterCount() != 2)
                      continue;
                  if (m.getReturnType().getName().equals("org.apache.hadoop.metrics2.MetricsRecordBuilderJVMInterface"))
                      continue;
                  target = m;
                  break;
              }
          }
          if (target == null)
              throw new RuntimeException("No matching target method found: addGauge");
          target.setAccessible(true);
          org.apache.hadoop.metrics2.MetricsRecordBuilderJVMInterface __result = (org.apache.hadoop.metrics2.MetricsRecordBuilderJVMInterface) target.invoke(this, arg0, arg1);
          return __result;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public org.apache.hadoop.metrics2.MetricsRecordBuilderJVMInterface addGauge_bridge(java.lang.Object arg0, double arg1) {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[2];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              __types[1] = double.class;
              try {
                  target = this.getClass().getMethod("addGauge", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("addGauge"))
                      continue;
                  if (m.getParameterCount() != 2)
                      continue;
                  if (m.getReturnType().getName().equals("org.apache.hadoop.metrics2.MetricsRecordBuilderJVMInterface"))
                      continue;
                  target = m;
                  break;
              }
          }
          if (target == null)
              throw new RuntimeException("No matching target method found: addGauge");
          target.setAccessible(true);
          org.apache.hadoop.metrics2.MetricsRecordBuilderJVMInterface __result = (org.apache.hadoop.metrics2.MetricsRecordBuilderJVMInterface) target.invoke(this, arg0, arg1);
          return __result;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public org.apache.hadoop.metrics2.MetricsRecordBuilderJVMInterface tag_bridge(java.lang.Object arg0, java.lang.String arg1) {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[2];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              __types[1] = (arg1 != null ? arg1.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("tag", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("tag"))
                      continue;
                  if (m.getParameterCount() != 2)
                      continue;
                  if (m.getReturnType().getName().equals("org.apache.hadoop.metrics2.MetricsRecordBuilderJVMInterface"))
                      continue;
                  target = m;
                  break;
              }
          }
          if (target == null)
              throw new RuntimeException("No matching target method found: tag");
          target.setAccessible(true);
          org.apache.hadoop.metrics2.MetricsRecordBuilderJVMInterface __result = (org.apache.hadoop.metrics2.MetricsRecordBuilderJVMInterface) target.invoke(this, arg0, arg1);
          return __result;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public org.apache.hadoop.metrics2.MetricsRecordBuilderJVMInterface add_bridge(org.apache.hadoop.metrics2.MetricsTagJVMInterface arg0) {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[1];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("add", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("add"))
                      continue;
                  if (m.getParameterCount() != 1)
                      continue;
                  if (m.getReturnType().getName().equals("org.apache.hadoop.metrics2.MetricsRecordBuilderJVMInterface"))
                      continue;
                  target = m;
                  break;
              }
          }
          if (target == null)
              throw new RuntimeException("No matching target method found: add");
          target.setAccessible(true);
          org.apache.hadoop.metrics2.MetricsRecordBuilderJVMInterface __result = (org.apache.hadoop.metrics2.MetricsRecordBuilderJVMInterface) target.invoke(this, arg0);
          return __result;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public org.apache.hadoop.metrics2.MetricsRecordBuilderJVMInterface addGauge_bridge(java.lang.Object arg0, float arg1) {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[2];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              __types[1] = float.class;
              try {
                  target = this.getClass().getMethod("addGauge", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("addGauge"))
                      continue;
                  if (m.getParameterCount() != 2)
                      continue;
                  if (m.getReturnType().getName().equals("org.apache.hadoop.metrics2.MetricsRecordBuilderJVMInterface"))
                      continue;
                  target = m;
                  break;
              }
          }
          if (target == null)
              throw new RuntimeException("No matching target method found: addGauge");
          target.setAccessible(true);
          org.apache.hadoop.metrics2.MetricsRecordBuilderJVMInterface __result = (org.apache.hadoop.metrics2.MetricsRecordBuilderJVMInterface) target.invoke(this, arg0, arg1);
          return __result;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public org.apache.hadoop.metrics2.MetricsRecordBuilderJVMInterface addCounter_bridge(java.lang.Object arg0, int arg1) {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[2];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              __types[1] = int.class;
              try {
                  target = this.getClass().getMethod("addCounter", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("addCounter"))
                      continue;
                  if (m.getParameterCount() != 2)
                      continue;
                  if (m.getReturnType().getName().equals("org.apache.hadoop.metrics2.MetricsRecordBuilderJVMInterface"))
                      continue;
                  target = m;
                  break;
              }
          }
          if (target == null)
              throw new RuntimeException("No matching target method found: addCounter");
          target.setAccessible(true);
          org.apache.hadoop.metrics2.MetricsRecordBuilderJVMInterface __result = (org.apache.hadoop.metrics2.MetricsRecordBuilderJVMInterface) target.invoke(this, arg0, arg1);
          return __result;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public org.apache.hadoop.metrics2.MetricsRecordBuilderJVMInterface addCounter_bridge(java.lang.Object arg0, long arg1) {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[2];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              __types[1] = long.class;
              try {
                  target = this.getClass().getMethod("addCounter", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("addCounter"))
                      continue;
                  if (m.getParameterCount() != 2)
                      continue;
                  if (m.getReturnType().getName().equals("org.apache.hadoop.metrics2.MetricsRecordBuilderJVMInterface"))
                      continue;
                  target = m;
                  break;
              }
          }
          if (target == null)
              throw new RuntimeException("No matching target method found: addCounter");
          target.setAccessible(true);
          org.apache.hadoop.metrics2.MetricsRecordBuilderJVMInterface __result = (org.apache.hadoop.metrics2.MetricsRecordBuilderJVMInterface) target.invoke(this, arg0, arg1);
          return __result;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
}

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
package org.apache.hadoop.tracing;

/**
 * No-Op Tracer (for now) to remove HTrace without changing too many files.
 */
public class Tracer implements TracerJVMInterface {
  // Singleton
  private static final Tracer globalTracer = null;
  private final NullTraceScope nullTraceScope;
  private final String name;

  public final static String SPAN_RECEIVER_CLASSES_KEY =
      "span.receiver.classes";

  public Tracer(String name) {
    this.name = name;
    nullTraceScope = NullTraceScope.INSTANCE;
  }

  // Keeping this function at the moment for HTrace compatiblity,
  // in fact all threads share a single global tracer for OpenTracing.
  public static Tracer curThreadTracer() {
    return globalTracer;
  }

  /***
   * Return active span.
   * @return org.apache.hadoop.tracing.Span
   */
  public static Span getCurrentSpan() {
    return null;
  }

  public TraceScope newScope(String description) {
    return nullTraceScope;
  }

  public Span newSpan(String description, SpanContext spanCtx) {
    return new Span();
  }

  public TraceScope newScope(String description, SpanContext spanCtx) {
    return nullTraceScope;
  }

  public TraceScope newScope(String description, SpanContext spanCtx,
      boolean finishSpanOnClose) {
    return nullTraceScope;
  }

  public TraceScope activateSpan(Span span) {
    return nullTraceScope;
  }

  public void close() {
  }

  public String getName() {
    return name;
  }

  public static class Builder {
    static Tracer globalTracer;
    private String name;

    public Builder(final String name) {
      this.name = name;
    }

    public Builder conf(TraceConfiguration conf) {
      return this;
    }

    public Tracer build() {
      if (globalTracer == null) {
        globalTracer = new Tracer(name);
      }
      return globalTracer;
    }
  }
  
  public org.apache.hadoop.tracing.TraceScopeJVMInterface newScope_bridge(java.lang.String arg0, org.apache.hadoop.tracing.SpanContextJVMInterface arg1) {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[2];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              __types[1] = (arg1 != null ? arg1.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("newScope", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("newScope"))
                      continue;
                  if (m.getParameterCount() != 2)
                      continue;
                  if (m.getReturnType().getName().equals("org.apache.hadoop.tracing.TraceScopeJVMInterface"))
                      continue;
                  target = m;
                  break;
              }
          }
          if (target == null)
              throw new RuntimeException("No matching target method found: newScope");
          target.setAccessible(true);
          org.apache.hadoop.tracing.TraceScopeJVMInterface __result = (org.apache.hadoop.tracing.TraceScopeJVMInterface) target.invoke(this, arg0, arg1);
          return __result;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public org.apache.hadoop.tracing.TraceScopeJVMInterface activateSpan_bridge(org.apache.hadoop.tracing.SpanJVMInterface arg0) {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[1];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("activateSpan", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("activateSpan"))
                      continue;
                  if (m.getParameterCount() != 1)
                      continue;
                  if (m.getReturnType().getName().equals("org.apache.hadoop.tracing.TraceScopeJVMInterface"))
                      continue;
                  target = m;
                  break;
              }
          }
          if (target == null)
              throw new RuntimeException("No matching target method found: activateSpan");
          target.setAccessible(true);
          org.apache.hadoop.tracing.TraceScopeJVMInterface __result = (org.apache.hadoop.tracing.TraceScopeJVMInterface) target.invoke(this, arg0);
          return __result;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public org.apache.hadoop.tracing.SpanJVMInterface newSpan_bridge(java.lang.String arg0, org.apache.hadoop.tracing.SpanContextJVMInterface arg1) {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[2];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              __types[1] = (arg1 != null ? arg1.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("newSpan", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("newSpan"))
                      continue;
                  if (m.getParameterCount() != 2)
                      continue;
                  if (m.getReturnType().getName().equals("org.apache.hadoop.tracing.SpanJVMInterface"))
                      continue;
                  target = m;
                  break;
              }
          }
          if (target == null)
              throw new RuntimeException("No matching target method found: newSpan");
          target.setAccessible(true);
          org.apache.hadoop.tracing.SpanJVMInterface __result = (org.apache.hadoop.tracing.SpanJVMInterface) target.invoke(this, arg0, arg1);
          return __result;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public org.apache.hadoop.tracing.TraceScopeJVMInterface newScope_bridge(java.lang.String arg0, org.apache.hadoop.tracing.SpanContextJVMInterface arg1, boolean arg2) {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[3];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              __types[1] = (arg1 != null ? arg1.getClass() : Object.class);
              __types[2] = boolean.class;
              try {
                  target = this.getClass().getMethod("newScope", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("newScope"))
                      continue;
                  if (m.getParameterCount() != 3)
                      continue;
                  if (m.getReturnType().getName().equals("org.apache.hadoop.tracing.TraceScopeJVMInterface"))
                      continue;
                  target = m;
                  break;
              }
          }
          if (target == null)
              throw new RuntimeException("No matching target method found: newScope");
          target.setAccessible(true);
          org.apache.hadoop.tracing.TraceScopeJVMInterface __result = (org.apache.hadoop.tracing.TraceScopeJVMInterface) target.invoke(this, arg0, arg1, arg2);
          return __result;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
}

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

package org.apache.hadoop.service;

import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.classification.InterfaceAudience.Public;
import org.apache.hadoop.classification.InterfaceStability.Evolving;
import org.apache.hadoop.conf.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Composition of services.
 */
@Public
@Evolving
public class CompositeService extends AbstractService implements CompositeServiceJVMInterface {

  private static final Logger LOG =
      LoggerFactory.getLogger(CompositeService.class);

  /**
   * Policy on shutdown: attempt to close everything (purest) or
   * only try to close started services (which assumes
   * that the service implementations may not handle the stop() operation
   * except when started.
   * Irrespective of this policy, if a child service fails during
   * its init() or start() operations, it will have stop() called on it.
   */
  protected static final boolean STOP_ONLY_STARTED_SERVICES = false;

  private final List<Service> serviceList = new ArrayList<Service>();

  public CompositeService(String name) {
    super(name);
  }

  /**
   * Get a cloned list of services
   * @return a list of child services at the time of invocation -
   * added services will not be picked up.
   */
  public List<Service> getServices() {
    synchronized (serviceList) {
      return new ArrayList<Service>(serviceList);
    }
  }

  /**
   * Add the passed {@link Service} to the list of services managed by this
   * {@link CompositeService}
   * @param service the {@link Service} to be added
   */
  protected void addService(Service service) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Adding service " + service.getName());
    }
    synchronized (serviceList) {
      serviceList.add(service);
    }
  }

  /**
   * If the passed object is an instance of {@link Service},
   * add it to the list of services managed by this {@link CompositeService}
   * @param object
   * @return true if a service is added, false otherwise.
   */
  protected boolean addIfService(Object object) {
    if (object instanceof Service) {
      addService((Service) object);
      return true;
    } else {
      return false;
    }
  }

  protected synchronized boolean removeService(Service service) {
    synchronized (serviceList) {
      return serviceList.remove(service);
    }
  }

  protected void serviceInit(Configuration conf) throws Exception {
    List<Service> services = getServices();
    if (LOG.isDebugEnabled()) {
      LOG.debug(getName() + ": initing services, size=" + services.size());
    }
    for (Service service : services) {
      service.init(conf);
    }
    super.serviceInit(conf);
  }

  protected void serviceStart() throws Exception {
    List<Service> services = getServices();
    if (LOG.isDebugEnabled()) {
      LOG.debug(getName() + ": starting services, size=" + services.size());
    }
    for (Service service : services) {
      // start the service. If this fails that service
      // will be stopped and an exception raised
      service.start();
    }
    super.serviceStart();
  }

  protected void serviceStop() throws Exception {
    //stop all services that were started
    int numOfServicesToStop = serviceList.size();
    if (LOG.isDebugEnabled()) {
      LOG.debug(getName() + ": stopping services, size=" + numOfServicesToStop);
    }
    stop(numOfServicesToStop, STOP_ONLY_STARTED_SERVICES);
    super.serviceStop();
  }

  /**
   * Stop the services in reverse order
   *
   * @param numOfServicesStarted index from where the stop should work
   * @param stopOnlyStartedServices flag to say "only start services that are
   * started, not those that are NOTINITED or INITED.
   * @throws RuntimeException the first exception raised during the
   * stop process -<i>after all services are stopped</i>
   */
  private void stop(int numOfServicesStarted, boolean stopOnlyStartedServices) {
    // stop in reverse order of start
    Exception firstException = null;
    List<Service> services = getServices();
    for (int i = numOfServicesStarted - 1; i >= 0; i--) {
      Service service = services.get(i);
      if (LOG.isDebugEnabled()) {
        LOG.debug("Stopping service #" + i + ": " + service);
      }
      STATE state = service.getServiceState();
      //depending on the stop police
      if (state == STATE.STARTED 
         || (!stopOnlyStartedServices && state == STATE.INITED)) {
        Exception ex = ServiceOperations.stopQuietly(LOG, service);
        if (ex != null && firstException == null) {
          firstException = ex;
        }
      }
    }
    //after stopping all services, rethrow the first exception raised
    if (firstException != null) {
      throw ServiceStateException.convert(firstException);
    }
  }

  /**
   * JVM Shutdown hook for CompositeService which will stop the give
   * CompositeService gracefully in case of JVM shutdown.
   */
  public static class CompositeServiceShutdownHook implements Runnable {

    private CompositeService compositeService;

    public CompositeServiceShutdownHook(CompositeService compositeService) {
      this.compositeService = compositeService;
    }

    @Override
    public void run() {
      ServiceOperations.stopQuietly(compositeService);
    }
  }
  
  public java.lang.Object getServiceState_bridge() {
      try {
          java.lang.reflect.Method target = null;
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("getServiceState"))
                      continue;
                  if (m.getParameterCount() != 0)
                      continue;
                  if (m.getReturnType().getName().equals("java.lang.Object"))
                      continue;
                  target = m;
                  break;
              }
          }
          if (target == null)
              throw new RuntimeException("No matching target method found: getServiceState");
          target.setAccessible(true);
          java.lang.Object __result = (java.lang.Object) target.invoke(this);
          return __result;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public java.lang.Object getFailureState_bridge() {
      try {
          java.lang.reflect.Method target = null;
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("getFailureState"))
                      continue;
                  if (m.getParameterCount() != 0)
                      continue;
                  if (m.getReturnType().getName().equals("java.lang.Object"))
                      continue;
                  target = m;
                  break;
              }
          }
          if (target == null)
              throw new RuntimeException("No matching target method found: getFailureState");
          target.setAccessible(true);
          java.lang.Object __result = (java.lang.Object) target.invoke(this);
          return __result;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public java.util.Map getBlockers_bridge() {
      try {
          java.lang.reflect.Method target = null;
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("getBlockers"))
                      continue;
                  if (m.getParameterCount() != 0)
                      continue;
                  if (m.getReturnType().getName().equals("java.util.Map"))
                      continue;
                  target = m;
                  break;
              }
          }
          if (target == null)
              throw new RuntimeException("No matching target method found: getBlockers");
          target.setAccessible(true);
          java.util.Map __result = (java.util.Map) target.invoke(this);
          return __result;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public java.util.List getLifecycleHistory_bridge() {
      try {
          java.lang.reflect.Method target = null;
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("getLifecycleHistory"))
                      continue;
                  if (m.getParameterCount() != 0)
                      continue;
                  if (m.getReturnType().getName().equals("java.util.List"))
                      continue;
                  target = m;
                  break;
              }
          }
          if (target == null)
              throw new RuntimeException("No matching target method found: getLifecycleHistory");
          target.setAccessible(true);
          java.util.List __result = (java.util.List) target.invoke(this);
          return __result;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public void stop_bridge() {
      try {
          java.lang.reflect.Method target = null;
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("stop"))
                      continue;
                  if (m.getParameterCount() != 0)
                      continue;
                  if (m.getReturnType().getName().equals("void"))
                      continue;
                  target = m;
                  break;
              }
          }
          if (target == null)
              throw new RuntimeException("No matching target method found: stop");
          target.setAccessible(true);
          target.invoke(this);
          return;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public void start_bridge() {
      try {
          java.lang.reflect.Method target = null;
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("start"))
                      continue;
                  if (m.getParameterCount() != 0)
                      continue;
                  if (m.getReturnType().getName().equals("void"))
                      continue;
                  target = m;
                  break;
              }
          }
          if (target == null)
              throw new RuntimeException("No matching target method found: start");
          target.setAccessible(true);
          target.invoke(this);
          return;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public void unregisterServiceListener_bridge(java.lang.Object arg0) {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[1];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("unregisterServiceListener", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("unregisterServiceListener"))
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
              throw new RuntimeException("No matching target method found: unregisterServiceListener");
          target.setAccessible(true);
          target.invoke(this, arg0);
          return;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public void registerServiceListener_bridge(java.lang.Object arg0) {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[1];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("registerServiceListener", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("registerServiceListener"))
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
              throw new RuntimeException("No matching target method found: registerServiceListener");
          target.setAccessible(true);
          target.invoke(this, arg0);
          return;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public boolean waitForServiceToStop_bridge(long arg0) {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[1];
              __types[0] = long.class;
              try {
                  target = this.getClass().getMethod("waitForServiceToStop", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("waitForServiceToStop"))
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
              throw new RuntimeException("No matching target method found: waitForServiceToStop");
          target.setAccessible(true);
          boolean __result = (boolean) target.invoke(this, arg0);
          return __result;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public org.apache.hadoop.conf.ConfigurationJVMInterface getConfig_bridge() {
      try {
          java.lang.reflect.Method target = null;
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("getConfig"))
                      continue;
                  if (m.getParameterCount() != 0)
                      continue;
                  if (m.getReturnType().getName().equals("org.apache.hadoop.conf.ConfigurationJVMInterface"))
                      continue;
                  target = m;
                  break;
              }
          }
          if (target == null)
              throw new RuntimeException("No matching target method found: getConfig");
          target.setAccessible(true);
          org.apache.hadoop.conf.ConfigurationJVMInterface __result = (org.apache.hadoop.conf.ConfigurationJVMInterface) target.invoke(this);
          return __result;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public long getStartTime_bridge() {
      try {
          java.lang.reflect.Method target = null;
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("getStartTime"))
                      continue;
                  if (m.getParameterCount() != 0)
                      continue;
                  if (m.getReturnType().getName().equals("long"))
                      continue;
                  target = m;
                  break;
              }
          }
          if (target == null)
              throw new RuntimeException("No matching target method found: getStartTime");
          target.setAccessible(true);
          long __result = (long) target.invoke(this);
          return __result;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public void init_bridge(org.apache.hadoop.conf.ConfigurationJVMInterface arg0) {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[1];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("init", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("init"))
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
              throw new RuntimeException("No matching target method found: init");
          target.setAccessible(true);
          target.invoke(this, arg0);
          return;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public java.lang.String getName_bridge() {
      try {
          java.lang.reflect.Method target = null;
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("getName"))
                      continue;
                  if (m.getParameterCount() != 0)
                      continue;
                  if (m.getReturnType().getName().equals("java.lang.String"))
                      continue;
                  target = m;
                  break;
              }
          }
          if (target == null)
              throw new RuntimeException("No matching target method found: getName");
          target.setAccessible(true);
          java.lang.String __result = (java.lang.String) target.invoke(this);
          return __result;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public boolean isInState_bridge(java.lang.Object arg0) {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[1];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("isInState", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("isInState"))
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
              throw new RuntimeException("No matching target method found: isInState");
          target.setAccessible(true);
          boolean __result = (boolean) target.invoke(this, arg0);
          return __result;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public java.lang.Throwable getFailureCause_bridge() {
      try {
          java.lang.reflect.Method target = null;
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("getFailureCause"))
                      continue;
                  if (m.getParameterCount() != 0)
                      continue;
                  if (m.getReturnType().getName().equals("java.lang.Throwable"))
                      continue;
                  target = m;
                  break;
              }
          }
          if (target == null)
              throw new RuntimeException("No matching target method found: getFailureCause");
          target.setAccessible(true);
          java.lang.Throwable __result = (java.lang.Throwable) target.invoke(this);
          return __result;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public void close_bridge() throws java.io.IOException {
      try {
          java.lang.reflect.Method target = null;
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("close"))
                      continue;
                  if (m.getParameterCount() != 0)
                      continue;
                  if (m.getReturnType().getName().equals("void"))
                      continue;
                  target = m;
                  break;
              }
          }
          if (target == null)
              throw new RuntimeException("No matching target method found: close");
          target.setAccessible(true);
          target.invoke(this);
          return;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }

}

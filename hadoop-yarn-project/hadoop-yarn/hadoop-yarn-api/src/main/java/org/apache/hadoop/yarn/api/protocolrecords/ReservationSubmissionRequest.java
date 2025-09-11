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

package org.apache.hadoop.yarn.api.protocolrecords;

import org.apache.hadoop.classification.InterfaceAudience.Public;
import org.apache.hadoop.classification.InterfaceStability.Unstable;
import org.apache.hadoop.yarn.api.records.QueueInfo;
import org.apache.hadoop.yarn.api.records.ReservationDefinition;
import org.apache.hadoop.yarn.api.records.ReservationId;
import org.apache.hadoop.yarn.util.Records;

/**
 * {@link ReservationSubmissionRequest} captures the set of requirements the
 * user has to create a reservation.
 * 
 * @see ReservationDefinition
 * 
 */
@Public
@Unstable
public abstract class ReservationSubmissionRequest implements ReservationSubmissionRequestJVMInterface {

  @Public
  @Unstable
  public static ReservationSubmissionRequest newInstance(
      ReservationDefinition reservationDefinition, String queueName,
      ReservationId reservationId) {
    ReservationSubmissionRequest request =
        Records.newRecord(ReservationSubmissionRequest.class);
    request.setReservationDefinition(reservationDefinition);
    request.setQueue(queueName);
    request.setReservationId(reservationId);
    return request;
  }

  /**
   * Get the {@link ReservationDefinition} representing the user constraints for
   * this reservation
   * 
   * @return the reservation definition representing user constraints
   */
  @Public
  @Unstable
  public abstract ReservationDefinition getReservationDefinition();

  /**
   * Set the {@link ReservationDefinition} representing the user constraints for
   * this reservation
   * 
   * @param reservationDefinition the reservation request representing the
   *          reservation
   */
  @Public
  @Unstable
  public abstract void setReservationDefinition(
      ReservationDefinition reservationDefinition);

  /**
   * Get the name of the {@code Plan} that corresponds to the name of the
   * {@link QueueInfo} in the scheduler to which the reservation will be
   * submitted to.
   * 
   * @return the name of the {@code Plan} that corresponds to the name of the
   *         {@link QueueInfo} in the scheduler to which the reservation will be
   *         submitted to
   */
  @Public
  @Unstable
  public abstract String getQueue();

  /**
   * Set the name of the {@code Plan} that corresponds to the name of the
   * {@link QueueInfo} in the scheduler to which the reservation will be
   * submitted to
   * 
   * @param queueName the name of the parent {@code Plan} that corresponds to
   *          the name of the {@link QueueInfo} in the scheduler to which the
   *          reservation will be submitted to
   */
  @Public
  @Unstable
  public abstract void setQueue(String queueName);

  /**
   * Get the reservation id that corresponds to the reservation submission.
   *
   * @return reservation id that will be used to identify the reservation
   * submission.
   */
  @Public
  @Unstable
  public abstract ReservationId getReservationId();

  /**
   * Set the reservation id that corresponds to the reservation submission.
   *
   * @param reservationId reservation id that will be used to identify the
   *                      reservation submission.
   */
  @Public
  @Unstable
  public abstract void setReservationId(ReservationId reservationId);
  
  public void setReservationId_bridge(org.apache.hadoop.yarn.api.records.ReservationIdJVMInterface arg0) {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[1];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("setReservationId", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("setReservationId"))
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
              throw new RuntimeException("No matching target method found: setReservationId");
          target.setAccessible(true);
          target.invoke(this, arg0);
          return;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
  
  public void setReservationDefinition_bridge(org.apache.hadoop.yarn.api.records.ReservationDefinitionJVMInterface arg0) {
      try {
          java.lang.reflect.Method target = null;
          {
              Class<?>[] __types = new Class<?>[1];
              __types[0] = (arg0 != null ? arg0.getClass() : Object.class);
              try {
                  target = this.getClass().getMethod("setReservationDefinition", __types);
              } catch (NoSuchMethodException e) {
              }
          }
          if (target == null) {
              for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                  if (!m.getName().equals("setReservationDefinition"))
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
              throw new RuntimeException("No matching target method found: setReservationDefinition");
          target.setAccessible(true);
          target.invoke(this, arg0);
          return;
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }

}

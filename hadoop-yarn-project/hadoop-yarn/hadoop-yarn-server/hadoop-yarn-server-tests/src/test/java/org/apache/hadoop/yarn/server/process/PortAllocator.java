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

package org.apache.hadoop.yarn.server.process;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashSet;
import java.util.Set;

import org.apache.hadoop.net.ServerSocketUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Allocates available network ports for YARN node processes. This class
 * dynamically finds and allocates free ports to avoid conflicts when running
 * multiple YARN nodes on the same machine.
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Dynamic port allocation from configurable range</li>
 *   <li>Port availability checking before allocation</li>
 *   <li>Tracks allocated ports to prevent duplicates</li>
 *   <li>Batch allocation for multiple ports</li>
 *   <li>Thread-safe port allocation</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>
 * PortAllocator allocator = new PortAllocator();
 *
 * // Allocate single port
 * int rmPort = allocator.allocatePort();
 *
 * // Allocate multiple ports
 * int[] ports = allocator.allocatePorts(5); // RM needs 5 ports
 *
 * // Release port when done
 * allocator.releasePort(rmPort);
 * </pre>
 *
 * @see ProcessConfigurationGenerator
 */
public class PortAllocator {

  private static final Logger LOG =
      LoggerFactory.getLogger(PortAllocator.class);

  /** Default starting port for allocation */
  public static final int DEFAULT_START_PORT = 50000;

  /** Default ending port for allocation */
  public static final int DEFAULT_END_PORT = 59999;

  /** Maximum number of allocation attempts before giving up */
  private static final int MAX_ALLOCATION_ATTEMPTS = 100;

  /** Starting port for allocation range */
  private final int startPort;

  /** Ending port for allocation range */
  private final int endPort;

  /** Next port to try allocating */
  private int nextPort;

  /** Set of ports that have been allocated */
  private final Set<Integer> allocatedPorts;

  /**
   * Constructs a PortAllocator with default port range (50000-59999).
   */
  public PortAllocator() {
    this(DEFAULT_START_PORT, DEFAULT_END_PORT);
  }

  /**
   * Constructs a PortAllocator with custom port range.
   *
   * @param startPort Starting port (inclusive)
   * @param endPort Ending port (inclusive)
   */
  public PortAllocator(int startPort, int endPort) {
    if (startPort < 1024) {
      throw new IllegalArgumentException(
          "Start port must be >= 1024 (got: " + startPort + ")");
    }

    if (endPort > 65535) {
      throw new IllegalArgumentException(
          "End port must be <= 65535 (got: " + endPort + ")");
    }

    if (startPort >= endPort) {
      throw new IllegalArgumentException(
          "Start port must be < end port (got: " + startPort +
          " >= " + endPort + ")");
    }

    this.startPort = startPort;
    this.endPort = endPort;
    this.nextPort = startPort;
    this.allocatedPorts = new HashSet<>();

    LOG.info("Created PortAllocator with range: {}-{}", startPort, endPort);
  }

  /**
   * Allocates a single available port.
   *
   * @return An available port number
   * @throws IOException if no ports are available after max attempts
   */
  public synchronized int allocatePort() throws IOException {
    int attempts = 0;

    while (attempts < MAX_ALLOCATION_ATTEMPTS) {
      int candidatePort = nextPort;

      // Advance next port (with wraparound)
      nextPort++;
      if (nextPort > endPort) {
        nextPort = startPort;
      }

      // Check if already allocated
      if (allocatedPorts.contains(candidatePort)) {
        attempts++;
        continue;
      }

      // Check if port is actually available
      if (isPortAvailable(candidatePort)) {
        allocatedPorts.add(candidatePort);
        LOG.debug("Allocated port: {}", candidatePort);
        return candidatePort;
      }

      attempts++;
    }

    throw new IOException(
        "Failed to allocate port after " + MAX_ALLOCATION_ATTEMPTS +
        " attempts. Range: " + startPort + "-" + endPort +
        ", allocated: " + allocatedPorts.size());
  }

  /**
   * Allocates multiple ports at once.
   *
   * @param count Number of ports to allocate
   * @return Array of allocated port numbers
   * @throws IOException if unable to allocate all requested ports
   */
  public synchronized int[] allocatePorts(int count) throws IOException {
    if (count <= 0) {
      throw new IllegalArgumentException(
          "Port count must be positive (got: " + count + ")");
    }

    int[] ports = new int[count];

    try {
      for (int i = 0; i < count; i++) {
        ports[i] = allocatePort();
      }
      return ports;
    } catch (IOException e) {
      // Allocation failed - release any ports we did allocate
      for (int i = 0; i < count; i++) {
        if (ports[i] != 0) {
          releasePort(ports[i]);
        }
      }
      throw new IOException(
          "Failed to allocate " + count + " ports", e);
    }
  }

  /**
   * Checks if a port is available by attempting to bind to it.
   *
   * @param port Port to check
   * @return true if the port is available, false otherwise
   */
  private boolean isPortAvailable(int port) {
    try (ServerSocket socket = new ServerSocket(port)) {
      socket.setReuseAddress(true);
      return true;
    } catch (IOException e) {
      LOG.trace("Port {} is not available: {}", port, e.getMessage());
      return false;
    }
  }

  /**
   * Releases a previously allocated port, making it available for reuse.
   *
   * @param port Port to release
   * @return true if the port was released, false if it wasn't allocated
   */
  public synchronized boolean releasePort(int port) {
    boolean removed = allocatedPorts.remove(port);
    if (removed) {
      LOG.debug("Released port: {}", port);
    } else {
      LOG.warn("Attempted to release unallocated port: {}", port);
    }
    return removed;
  }

  /**
   * Releases multiple ports at once.
   *
   * @param ports Ports to release
   */
  public synchronized void releasePorts(int... ports) {
    if (ports == null) {
      return;
    }

    for (int port : ports) {
      releasePort(port);
    }
  }

  /**
   * Releases all allocated ports.
   */
  public synchronized void releaseAll() {
    int count = allocatedPorts.size();
    allocatedPorts.clear();
    LOG.info("Released all {} allocated ports", count);
  }

  /**
   * Gets the number of currently allocated ports.
   *
   * @return Number of allocated ports
   */
  public synchronized int getAllocatedCount() {
    return allocatedPorts.size();
  }

  /**
   * Gets the number of available ports in the range.
   * Note: This is an estimate and doesn't account for system-allocated ports.
   *
   * @return Estimated number of available ports
   */
  public synchronized int getAvailableCount() {
    int totalRange = endPort - startPort + 1;
    return totalRange - allocatedPorts.size();
  }

  /**
   * Gets a set of currently allocated ports (read-only view).
   *
   * @return Set of allocated port numbers
   */
  public synchronized Set<Integer> getAllocatedPorts() {
    return new HashSet<>(allocatedPorts);
  }

  /**
   * Checks if a specific port is allocated by this allocator.
   *
   * @param port Port to check
   * @return true if allocated, false otherwise
   */
  public synchronized boolean isAllocated(int port) {
    return allocatedPorts.contains(port);
  }

  /**
   * Gets the start of the port range.
   *
   * @return Start port
   */
  public int getStartPort() {
    return startPort;
  }

  /**
   * Gets the end of the port range.
   *
   * @return End port
   */
  public int getEndPort() {
    return endPort;
  }

  /**
   * Resets the allocator, releasing all ports and resetting the next port
   * to the start of the range.
   */
  public synchronized void reset() {
    allocatedPorts.clear();
    nextPort = startPort;
    LOG.info("PortAllocator reset");
  }

  @Override
  public synchronized String toString() {
    return "PortAllocator{" +
        "range=" + startPort + "-" + endPort +
        ", allocated=" + allocatedPorts.size() +
        ", next=" + nextPort +
        '}';
  }
}

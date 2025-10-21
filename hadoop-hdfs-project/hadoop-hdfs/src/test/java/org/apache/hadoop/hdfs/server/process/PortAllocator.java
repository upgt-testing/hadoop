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
package org.apache.hadoop.hdfs.server.process;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashSet;
import java.util.Set;

/**
 * Allocates and tracks network ports for ProcessBasedMiniDFSCluster nodes.
 *
 * <p>This class provides dynamic port allocation to avoid conflicts when
 * running multiple cluster instances or when ports are already in use.
 * Allocated ports are tracked to ensure no port is assigned twice within
 * the same allocator instance.
 *
 * <p>The allocator uses a configurable port range and checks port availability
 * by attempting to bind to each port before allocation.
 *
 * <p>Thread-safe for concurrent port allocation requests.
 */
public class PortAllocator {
  private static final Logger LOG = LoggerFactory.getLogger(PortAllocator.class);

  // Default port range for mini cluster nodes
  private static final int DEFAULT_MIN_PORT = 50000;
  private static final int DEFAULT_MAX_PORT = 59999;

  private final int minPort;
  private final int maxPort;
  private final Set<Integer> allocatedPorts;
  private int nextPort;

  /**
   * Creates a PortAllocator with the default port range (50000-59999).
   */
  public PortAllocator() {
    this(DEFAULT_MIN_PORT, DEFAULT_MAX_PORT);
  }

  /**
   * Creates a PortAllocator with a custom port range.
   *
   * @param minPort minimum port number (inclusive)
   * @param maxPort maximum port number (inclusive)
   * @throws IllegalArgumentException if port range is invalid
   */
  public PortAllocator(int minPort, int maxPort) {
    if (minPort < 1024 || minPort > 65535) {
      throw new IllegalArgumentException(
          "minPort must be between 1024 and 65535: " + minPort);
    }
    if (maxPort < 1024 || maxPort > 65535) {
      throw new IllegalArgumentException(
          "maxPort must be between 1024 and 65535: " + maxPort);
    }
    if (minPort > maxPort) {
      throw new IllegalArgumentException(
          "minPort must be <= maxPort: " + minPort + " > " + maxPort);
    }

    this.minPort = minPort;
    this.maxPort = maxPort;
    this.nextPort = minPort;
    this.allocatedPorts = new HashSet<>();
  }

  /**
   * Allocates an available port from the configured range.
   *
   * <p>This method scans ports sequentially starting from the last allocated
   * port, wrapping around to minPort if necessary. Each candidate port is
   * tested for availability by attempting to bind a ServerSocket to it.
   *
   * @return an available port number
   * @throws IOException if no available port can be found in the range
   */
  public synchronized int allocatePort() throws IOException {
    int startPort = nextPort;
    int attemptsRemaining = (maxPort - minPort + 1);

    while (attemptsRemaining > 0) {
      int candidatePort = nextPort;

      // Advance to next port for next allocation
      nextPort++;
      if (nextPort > maxPort) {
        nextPort = minPort;
      }

      // Skip if already allocated in this allocator instance
      if (allocatedPorts.contains(candidatePort)) {
        attemptsRemaining--;
        continue;
      }

      // Check if port is actually available
      if (isPortAvailable(candidatePort)) {
        allocatedPorts.add(candidatePort);
        LOG.debug("Allocated port: {}", candidatePort);
        return candidatePort;
      }

      attemptsRemaining--;
    }

    throw new IOException(
        "No available ports in range [" + minPort + ", " + maxPort + "]. " +
        "Already allocated: " + allocatedPorts.size() + " ports.");
  }

  /**
   * Allocates multiple ports at once.
   *
   * @param count number of ports to allocate
   * @return array of allocated port numbers
   * @throws IOException if unable to allocate the requested number of ports
   * @throws IllegalArgumentException if count is not positive
   */
  public synchronized int[] allocatePorts(int count) throws IOException {
    if (count <= 0) {
      throw new IllegalArgumentException("count must be positive: " + count);
    }

    int[] ports = new int[count];
    try {
      for (int i = 0; i < count; i++) {
        ports[i] = allocatePort();
      }
      return ports;
    } catch (IOException e) {
      // If we fail partway through, release any ports we did allocate
      for (int i = 0; i < count; i++) {
        if (ports[i] != 0) {
          releasePort(ports[i]);
        }
      }
      throw e;
    }
  }

  /**
   * Releases a previously allocated port, making it available for reuse.
   *
   * @param port the port to release
   * @return true if the port was allocated and has been released, false otherwise
   */
  public synchronized boolean releasePort(int port) {
    boolean removed = allocatedPorts.remove(port);
    if (removed) {
      LOG.debug("Released port: {}", port);
    }
    return removed;
  }

  /**
   * Releases multiple ports at once.
   *
   * @param ports array of ports to release
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
   * Checks if a port is available by attempting to bind to it.
   *
   * @param port the port to check
   * @return true if the port is available, false otherwise
   */
  private boolean isPortAvailable(int port) {
    try (ServerSocket socket = new ServerSocket(port)) {
      socket.setReuseAddress(true);
      return true;
    } catch (IOException e) {
      // Port is not available
      return false;
    }
  }

  /**
   * Gets the set of currently allocated ports.
   *
   * @return a copy of the allocated ports set
   */
  public synchronized Set<Integer> getAllocatedPorts() {
    return new HashSet<>(allocatedPorts);
  }

  /**
   * Gets the number of currently allocated ports.
   *
   * @return count of allocated ports
   */
  public synchronized int getAllocatedPortCount() {
    return allocatedPorts.size();
  }

  /**
   * Releases all allocated ports.
   */
  public synchronized void releaseAll() {
    LOG.debug("Releasing all {} allocated ports", allocatedPorts.size());
    allocatedPorts.clear();
  }

  /**
   * Gets the minimum port in the allocation range.
   *
   * @return minimum port number
   */
  public int getMinPort() {
    return minPort;
  }

  /**
   * Gets the maximum port in the allocation range.
   *
   * @return maximum port number
   */
  public int getMaxPort() {
    return maxPort;
  }
}

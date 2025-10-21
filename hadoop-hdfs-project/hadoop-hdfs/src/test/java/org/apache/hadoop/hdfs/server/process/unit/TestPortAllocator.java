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
package org.apache.hadoop.hdfs.server.process.unit;

import org.apache.hadoop.hdfs.server.process.PortAllocator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Unit tests for PortAllocator.
 */
public class TestPortAllocator {

  private PortAllocator allocator;

  @Before
  public void setUp() {
    allocator = new PortAllocator();
  }

  @After
  public void tearDown() {
    if (allocator != null) {
      allocator.releaseAll();
    }
  }

  @Test
  public void testAllocateSinglePort() throws IOException {
    int port = allocator.allocatePort();
    assertTrue("Port should be in valid range", port >= 50000 && port <= 59999);
  }

  @Test
  public void testAllocateMultiplePorts() throws IOException {
    int port1 = allocator.allocatePort();
    int port2 = allocator.allocatePort();
    int port3 = allocator.allocatePort();

    assertNotEquals("Ports should be different", port1, port2);
    assertNotEquals("Ports should be different", port2, port3);
    assertNotEquals("Ports should be different", port1, port3);
  }

  @Test
  public void testAllocatePortsArray() throws IOException {
    int count = 5;
    int[] ports = allocator.allocatePorts(count);

    assertEquals("Should allocate correct number of ports", count, ports.length);

    // Check all ports are unique
    Set<Integer> portSet = new HashSet<>();
    for (int port : ports) {
      assertTrue("Port should be in valid range", port >= 50000 && port <= 59999);
      assertTrue("Ports should be unique", portSet.add(port));
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAllocatePortsWithZeroCount() throws IOException {
    allocator.allocatePorts(0);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAllocatePortsWithNegativeCount() throws IOException {
    allocator.allocatePorts(-1);
  }

  @Test
  public void testReleasePort() throws IOException {
    int port = allocator.allocatePort();
    assertTrue("Port should be in allocated set",
        allocator.getAllocatedPorts().contains(port));

    boolean released = allocator.releasePort(port);
    assertTrue("Release should return true", released);
    assertFalse("Port should not be in allocated set after release",
        allocator.getAllocatedPorts().contains(port));
  }

  @Test
  public void testReleaseUnallocatedPort() {
    boolean released = allocator.releasePort(12345);
    assertFalse("Release of unallocated port should return false", released);
  }

  @Test
  public void testReleasePorts() throws IOException {
    int[] ports = allocator.allocatePorts(3);

    allocator.releasePorts(ports);

    for (int port : ports) {
      assertFalse("Port should not be in allocated set after release",
          allocator.getAllocatedPorts().contains(port));
    }
  }

  @Test
  public void testReleasePortsWithNull() {
    // Should not throw exception
    allocator.releasePorts(null);
  }

  @Test
  public void testGetAllocatedPorts() throws IOException {
    Set<Integer> portsBefore = allocator.getAllocatedPorts();
    assertEquals("Should start with no allocated ports", 0, portsBefore.size());

    int port1 = allocator.allocatePort();
    int port2 = allocator.allocatePort();

    Set<Integer> portsAfter = allocator.getAllocatedPorts();
    assertEquals("Should have 2 allocated ports", 2, portsAfter.size());
    assertTrue("Should contain port1", portsAfter.contains(port1));
    assertTrue("Should contain port2", portsAfter.contains(port2));
  }

  @Test
  public void testGetAllocatedPortCount() throws IOException {
    assertEquals("Should start with 0", 0, allocator.getAllocatedPortCount());

    allocator.allocatePort();
    assertEquals("Should have 1", 1, allocator.getAllocatedPortCount());

    allocator.allocatePort();
    assertEquals("Should have 2", 2, allocator.getAllocatedPortCount());
  }

  @Test
  public void testReleaseAll() throws IOException {
    allocator.allocatePorts(5);
    assertEquals("Should have 5 allocated ports", 5, allocator.getAllocatedPortCount());

    allocator.releaseAll();
    assertEquals("Should have no allocated ports", 0, allocator.getAllocatedPortCount());
  }

  @Test
  public void testCustomPortRange() throws IOException {
    PortAllocator customAllocator = new PortAllocator(60000, 60100);

    int port = customAllocator.allocatePort();
    assertTrue("Port should be in custom range", port >= 60000 && port <= 60100);

    customAllocator.releaseAll();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidPortRangeMinTooLow() {
    new PortAllocator(100, 60000);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidPortRangeMaxTooHigh() {
    new PortAllocator(50000, 70000);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidPortRangeMinGreaterThanMax() {
    new PortAllocator(60000, 50000);
  }

  @Test
  public void testGetMinMaxPort() {
    assertEquals("Min port should be 50000", 50000, allocator.getMinPort());
    assertEquals("Max port should be 59999", 59999, allocator.getMaxPort());

    PortAllocator custom = new PortAllocator(40000, 40100);
    assertEquals("Custom min port", 40000, custom.getMinPort());
    assertEquals("Custom max port", 40100, custom.getMaxPort());
  }

  @Test
  public void testPortNotReusedWithinAllocator() throws IOException {
    Set<Integer> allocatedPorts = new HashSet<>();

    // Allocate many ports
    for (int i = 0; i < 100; i++) {
      int port = allocator.allocatePort();
      assertTrue("Port should not be reused", allocatedPorts.add(port));
    }
  }

  @Test
  public void testPortCanBeReusedAfterRelease() throws IOException {
    // Allocate all ports in a small range
    PortAllocator smallAllocator = new PortAllocator(55000, 55002);

    int port1 = smallAllocator.allocatePort();
    int port2 = smallAllocator.allocatePort();
    int port3 = smallAllocator.allocatePort();

    // Release one port
    smallAllocator.releasePort(port1);

    // Should be able to allocate again (will get the released port)
    int port4 = smallAllocator.allocatePort();
    assertTrue("Should allocate successfully", port4 >= 55000 && port4 <= 55002);

    smallAllocator.releaseAll();
  }

  @Test
  public void testSkipOccupiedPort() throws IOException {
    // Occupy a port manually
    ServerSocket socket = null;
    try {
      socket = new ServerSocket(55000);

      // Allocator should skip the occupied port
      PortAllocator testAllocator = new PortAllocator(54999, 55002);
      int port1 = testAllocator.allocatePort();
      int port2 = testAllocator.allocatePort();
      int port3 = testAllocator.allocatePort();

      // None of the allocated ports should be 55000
      assertNotEquals("Should skip occupied port", 55000, port1);
      assertNotEquals("Should skip occupied port", 55000, port2);
      assertNotEquals("Should skip occupied port", 55000, port3);

      testAllocator.releaseAll();
    } finally {
      if (socket != null) {
        socket.close();
      }
    }
  }

  @Test(expected = IOException.class)
  public void testAllocationFailsWhenNoPortsAvailable() throws IOException {
    // Create allocator with very small range
    PortAllocator tinyAllocator = new PortAllocator(55000, 55001);

    // Allocate all available ports
    tinyAllocator.allocatePort();
    tinyAllocator.allocatePort();

    // This should fail
    tinyAllocator.allocatePort();
  }

  @Test
  public void testConcurrentAllocation() throws Exception {
    final int numThreads = 10;
    final int portsPerThread = 5;
    Thread[] threads = new Thread[numThreads];
    final Set<Integer> allPorts = new HashSet<>();
    final Object lock = new Object();

    for (int i = 0; i < numThreads; i++) {
      threads[i] = new Thread(() -> {
        try {
          for (int j = 0; j < portsPerThread; j++) {
            int port = allocator.allocatePort();
            synchronized (lock) {
              assertTrue("Port should be unique across threads", allPorts.add(port));
            }
          }
        } catch (IOException e) {
          fail("Allocation failed: " + e.getMessage());
        }
      });
    }

    // Start all threads
    for (Thread thread : threads) {
      thread.start();
    }

    // Wait for all threads to complete
    for (Thread thread : threads) {
      thread.join();
    }

    assertEquals("Should have allocated correct total number of ports",
        numThreads * portsPerThread, allPorts.size());
  }
}

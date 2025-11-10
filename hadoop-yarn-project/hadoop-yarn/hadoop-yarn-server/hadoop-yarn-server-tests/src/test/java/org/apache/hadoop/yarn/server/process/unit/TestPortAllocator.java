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

package org.apache.hadoop.yarn.server.process.unit;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.hadoop.yarn.server.process.PortAllocator;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

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
  public void testAllocatePort() throws IOException {
    int port = allocator.allocatePort();

    Assert.assertTrue("Port should be within valid range",
        port >= 1024 && port <= 65535);
    Assert.assertTrue("Port should be tracked as allocated",
        allocator.isAllocated(port));
    Assert.assertEquals("Should have 1 allocated port",
        1, allocator.getAllocatedCount());
  }

  @Test
  public void testAllocateMultiplePorts() throws IOException {
    int port1 = allocator.allocatePort();
    int port2 = allocator.allocatePort();
    int port3 = allocator.allocatePort();

    Assert.assertNotEquals("Ports should be unique", port1, port2);
    Assert.assertNotEquals("Ports should be unique", port1, port3);
    Assert.assertNotEquals("Ports should be unique", port2, port3);

    Assert.assertEquals("Should have 3 allocated ports",
        3, allocator.getAllocatedCount());
  }

  @Test
  public void testAllocatePortsArray() throws IOException {
    int count = 5;
    int[] ports = allocator.allocatePorts(count);

    Assert.assertEquals("Should allocate requested count",
        count, ports.length);

    // Verify all ports are unique
    Set<Integer> portSet = new HashSet<>();
    for (int port : ports) {
      Assert.assertTrue("Duplicate port allocated: " + port,
          portSet.add(port));
      Assert.assertTrue("Port should be allocated",
          allocator.isAllocated(port));
    }

    Assert.assertEquals("Should have correct allocated count",
        count, allocator.getAllocatedCount());
  }

  @Test
  public void testReleasePort() throws IOException {
    int port = allocator.allocatePort();
    Assert.assertTrue("Port should be allocated",
        allocator.isAllocated(port));

    boolean released = allocator.releasePort(port);
    Assert.assertTrue("Release should return true", released);
    Assert.assertFalse("Port should not be allocated after release",
        allocator.isAllocated(port));
    Assert.assertEquals("Should have 0 allocated ports",
        0, allocator.getAllocatedCount());
  }

  @Test
  public void testReleaseUnallocatedPort() {
    int port = 12345;
    boolean released = allocator.releasePort(port);
    Assert.assertFalse("Releasing unallocated port should return false",
        released);
  }

  @Test
  public void testReleasePorts() throws IOException {
    int[] ports = allocator.allocatePorts(3);
    allocator.releasePorts(ports);

    for (int port : ports) {
      Assert.assertFalse("Port should be released",
          allocator.isAllocated(port));
    }
    Assert.assertEquals("Should have 0 allocated ports",
        0, allocator.getAllocatedCount());
  }

  @Test
  public void testReleaseAll() throws IOException {
    allocator.allocatePorts(10);
    Assert.assertEquals("Should have 10 allocated ports",
        10, allocator.getAllocatedCount());

    allocator.releaseAll();
    Assert.assertEquals("Should have 0 allocated ports after releaseAll",
        0, allocator.getAllocatedCount());
  }

  @Test
  public void testReset() throws IOException {
    allocator.allocatePorts(5);
    Assert.assertEquals("Should have 5 allocated ports",
        5, allocator.getAllocatedCount());

    allocator.reset();
    Assert.assertEquals("Should have 0 allocated ports after reset",
        0, allocator.getAllocatedCount());
  }

  @Test
  public void testCustomPortRange() throws IOException {
    int startPort = 55000;
    int endPort = 55100;
    PortAllocator customAllocator = new PortAllocator(startPort, endPort);

    try {
      int port = customAllocator.allocatePort();
      Assert.assertTrue("Port should be in custom range",
          port >= startPort && port <= endPort);
    } finally {
      customAllocator.releaseAll();
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidPortRange_StartTooLow() {
    new PortAllocator(100, 60000);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidPortRange_EndTooHigh() {
    new PortAllocator(50000, 70000);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidPortRange_StartGreaterThanEnd() {
    new PortAllocator(60000, 50000);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAllocatePortsInvalidCount() throws IOException {
    allocator.allocatePorts(0);
  }

  @Test
  public void testGetAllocatedPorts() throws IOException {
    int port1 = allocator.allocatePort();
    int port2 = allocator.allocatePort();

    Set<Integer> allocated = allocator.getAllocatedPorts();
    Assert.assertEquals("Should return correct set size",
        2, allocated.size());
    Assert.assertTrue("Should contain allocated ports",
        allocated.contains(port1));
    Assert.assertTrue("Should contain allocated ports",
        allocated.contains(port2));
  }

  @Test
  public void testGetAvailableCount() throws IOException {
    int startPort = 50000;
    int endPort = 50010;
    PortAllocator customAllocator = new PortAllocator(startPort, endPort);

    try {
      int totalRange = endPort - startPort + 1;
      Assert.assertEquals("Should have full range available initially",
          totalRange, customAllocator.getAvailableCount());

      customAllocator.allocatePorts(3);
      Assert.assertEquals("Should have reduced available count",
          totalRange - 3, customAllocator.getAvailableCount());
    } finally {
      customAllocator.releaseAll();
    }
  }

  @Test
  public void testGetPortRange() {
    Assert.assertEquals("Should return default start port",
        PortAllocator.DEFAULT_START_PORT, allocator.getStartPort());
    Assert.assertEquals("Should return default end port",
        PortAllocator.DEFAULT_END_PORT, allocator.getEndPort());

    PortAllocator customAllocator = new PortAllocator(55000, 56000);
    Assert.assertEquals("Should return custom start port",
        55000, customAllocator.getStartPort());
    Assert.assertEquals("Should return custom end port",
        56000, customAllocator.getEndPort());
  }

  @Test
  public void testToString() throws IOException {
    String str = allocator.toString();
    Assert.assertTrue("toString should contain class name",
        str.contains("PortAllocator"));
    Assert.assertTrue("toString should contain range info",
        str.contains("range="));
  }

  @Test
  public void testConcurrentAllocation() throws Exception {
    // Test basic thread safety
    final int threadsCount = 3;
    final int portsPerThread = 5;
    Thread[] threads = new Thread[threadsCount];
    final Set<Integer>[] allocatedPorts = new Set[threadsCount];

    for (int i = 0; i < threadsCount; i++) {
      final int threadIndex = i;
      allocatedPorts[i] = new HashSet<>();
      threads[i] = new Thread(() -> {
        try {
          for (int j = 0; j < portsPerThread; j++) {
            int port = allocator.allocatePort();
            synchronized (allocatedPorts[threadIndex]) {
              allocatedPorts[threadIndex].add(port);
            }
          }
        } catch (IOException e) {
          Assert.fail("Thread " + threadIndex + " failed: " + e.getMessage());
        }
      });
    }

    // Start all threads
    for (Thread thread : threads) {
      thread.start();
    }

    // Wait for all threads
    for (Thread thread : threads) {
      thread.join();
    }

    // Verify all ports are unique across threads
    Set<Integer> allPorts = new HashSet<>();
    for (Set<Integer> threadPorts : allocatedPorts) {
      for (Integer port : threadPorts) {
        Assert.assertTrue("Port " + port + " allocated by multiple threads",
            allPorts.add(port));
      }
    }

    Assert.assertEquals("Should have allocated correct total count",
        threadsCount * portsPerThread, allocator.getAllocatedCount());
    Assert.assertEquals("All ports should be unique",
        threadsCount * portsPerThread, allPorts.size());
  }
}

/*
 * Copyright 2017 Agilx, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.apptuit.metrics.client;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Rajiv Shivane
 */

public class XCollectorForwarderTest {

  private static final int UDP_PORT = 8953;
  private static MockServer mockServer;
  private TagEncodedMetricName tagEncodedMetricName;
  private HashMap<String, String> globalTags;

  @BeforeClass
  public static void setUpClass() throws Exception {
    mockServer = new MockServer();
    mockServer.start();
  }

  @AfterClass
  public static void tearDownClass() throws Exception {
    if (mockServer != null) {
      mockServer.stop();
    }
  }

  @Before
  public void setUp() throws Exception {
    tagEncodedMetricName = TagEncodedMetricName.decode("proc.stat.cpu")
            .withTags("type", "idle");

    globalTags = new HashMap<>();
    globalTags.put("host", "rajiv");
    globalTags.put("env", "dev");
    globalTags.put("dev", "rajiv");
  }

  @After
  public void tearDown() throws Exception {
    mockServer.clearReceivedDPs();
  }

  @Test
  public void testSinglePacket() throws Exception {
    testForward(10,Sanitizer.NO_OP_SANITIZER);
  }

  @Test
  public void testMultiPacketDefaultSanitizer() throws Exception {
    testForward(250, null);
  }

  @Test
  public void testMultiPacket() throws Exception {
    testForward(250, Sanitizer.NO_OP_SANITIZER);
  }

  private void testForward(int numDataPoints, Sanitizer sanitizer) throws SocketException {
    ArrayList<DataPoint> dataPoints = createDataPoints(numDataPoints);

    XCollectorForwarder forwarder = new XCollectorForwarder(globalTags,
            new InetSocketAddress("127.0.0.1", UDP_PORT));
    if (sanitizer != null) {
      forwarder.forward(dataPoints, sanitizer);
    } else {
      forwarder.forward(dataPoints);
    }

    await().atMost(5, TimeUnit.SECONDS).until(() -> mockServer.countReceivedDPs() == numDataPoints);

    DataPoint[] receivedDPs = mockServer.getReceivedDPs();
    assertEquals(numDataPoints, receivedDPs.length);
    for (int i = 0; i < numDataPoints; i++) {
      assertEquals(getExpectedDataPoint(dataPoints.get(i), globalTags, sanitizer), receivedDPs[i]);
    }
  }

  private ArrayList<DataPoint> createDataPoints(int numDataPoints) {
    ArrayList<DataPoint> dataPoints = new ArrayList<>(numDataPoints);
    for (int i = 0; i < numDataPoints; i++) {
      long value = 99 + i;
      long epoch = System.currentTimeMillis() / 1000 - value;
      DataPoint dataPoint = new DataPoint(tagEncodedMetricName.getMetricName(), epoch, value,
              tagEncodedMetricName.getTags());
      dataPoints.add(dataPoint);
    }
    return dataPoints;
  }

  private DataPoint getExpectedDataPoint(DataPoint dataPoint, HashMap<String, String> globalTags, Sanitizer sanitizer) {
    Map<String, String> tags = new HashMap<>(dataPoint.getTags());
    tags.putAll(globalTags);
    if (sanitizer == null) {
      sanitizer = Sanitizer.DEFAULT_SANITIZER;
    }
    return new DataPoint(sanitizer.sanitizer(dataPoint.getMetric()), dataPoint.getTimestamp(), dataPoint.getValue(),
            tags);
  }

  private static class MockServer {

    private final DatagramSocket socket;
    private final byte[] buf = new byte[8192];
    private final Thread thread;
    private final List<DataPoint> receivedDPs = new ArrayList<>();
    private boolean running = true;

    public MockServer() throws SocketException {
      socket = new DatagramSocket(UDP_PORT);
      thread = new Thread(new RequestProcessor());
      thread.setDaemon(true);
    }

    public void start() {
      thread.start();
    }

    public void stop() {
      running = false;
      thread.interrupt();
    }

    public int countReceivedDPs() {
      return receivedDPs.size();
    }

    public DataPoint[] getReceivedDPs() {
      return receivedDPs.toArray(new DataPoint[receivedDPs.size()]);
    }

    public void clearReceivedDPs() {
      receivedDPs.clear();
    }

    private class RequestProcessor implements Runnable {

      @Override
      public void run() {
        while (running) {
          DatagramPacket packet = new DatagramPacket(buf, buf.length);
          try {
            socket.receive(packet);
            String data = new String(packet.getData(), 0, packet.getLength());
            //System.err.printf("Got packet of [%d] bytes.\n", data.length());
            Scanner lines = new Scanner(data).useDelimiter("\n");
            lines.forEachRemaining(line -> receivedDPs.add(toDataPoint(line)));
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      }

      private DataPoint toDataPoint(String line) {
        Scanner fields = new Scanner(line).useDelimiter(" ");
        return new DataPoint(fields.next(), fields.nextLong(),
                fields.nextLong(), getTags(fields));
      }

      private Map<String, String> getTags(Scanner fields) {
        Map<String, String> retVal = new HashMap<>();
        fields.forEachRemaining(field -> {
          String[] kv = field.split("=");
          retVal.put(kv[0], kv[1]);
        });
        return retVal;
      }
    }
  }

}
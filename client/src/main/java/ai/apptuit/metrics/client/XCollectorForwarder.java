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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Rajiv Shivane
 */
public class XCollectorForwarder {

  private static final Logger LOGGER = Logger.getLogger(XCollectorForwarder.class.getName());

  private static final String DEFAULT_HOST = "127.0.0.1";
  private static final int DEFAULT_PORT = 8953;

  private static final int KB = 1024;
  private static final int PACKET_SIZE = 8 * KB;
  private static final int BUFFER_SIZE = 16 * KB;

  private final Map<String, String> globalTags;
  private final SocketAddress xcollectorAddress;
  private DatagramSocket socket = null;

  public XCollectorForwarder(Map<String, String> globalTags) {
    this(globalTags, new InetSocketAddress(DEFAULT_HOST, DEFAULT_PORT));
  }

  XCollectorForwarder(Map<String, String> globalTags, SocketAddress xcollectorAddress) {
    this.globalTags = globalTags;
    this.xcollectorAddress = xcollectorAddress;
  }

  public void forward(Collection<DataPoint> dataPoints) {
    forward(dataPoints, Sanitizer.DEFAULT_SANITIZER);
  }

  public void forward(Collection<DataPoint> dataPoints, Sanitizer sanitizer) {

    if (socket == null) {
      try {
        socket = new DatagramSocket();
      } catch (SocketException e) {
        LOGGER.log(Level.SEVERE, "Error creating UDP socket", e);
        return;
      }
    }

    ByteArrayOutputStream baos = new ByteArrayOutputStream(BUFFER_SIZE);

    int idx = 0;
    for (DataPoint dp : dataPoints) {
      dp.toTextLine(baos, globalTags, sanitizer);
      int size = baos.size();
      if (size >= PACKET_SIZE) {
        sendPacket(baos, idx);
        idx = baos.size();
      } else {
        idx = size;
      }
    }
    sendPacket(baos, idx);
  }

  private void sendPacket(ByteArrayOutputStream outputStream, int idx) {
    if (idx <= 0) {
      return;
    }

    int size = outputStream.size();
    byte[] bytes = outputStream.toByteArray();
    DatagramPacket packet = new DatagramPacket(bytes, 0, idx, xcollectorAddress);
    try {
      socket.send(packet);
      LOGGER.info(" Forwarded [" + idx + "] bytes.");
    } catch (IOException e) {
      LOGGER.log(Level.SEVERE, "Error sending packet", e);
    }
    outputStream.reset();
    int newSize = size - idx;
    if (newSize > 0) {
      outputStream.write(bytes, idx, newSize);
    }
  }
}

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

package ai.apptuit.metrics.dropwizard;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;

import ai.apptuit.metrics.dropwizard.ApptuitReporter.ReportingMode;
import ai.apptuit.metrics.dropwizard.BaseMockClient.DataListener;
import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * @author Rajiv Shivane
 */
@PrepareForTest({ApptuitReporter.class})
@PowerMockIgnore({"org.jboss.byteman.*", "javax.net.ssl.*",
    "com.sun.management.*", "javax.management.*"})
@RunWith(PowerMockRunner.class)
public class ApptuitReporterTest {

  private MetricRegistry registry;
  private int period = 5;

  @Before
  public void setUp() throws Exception {
    registry = new MetricRegistry();
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public void testPutReporter() throws Exception {
    MockApptuitPutClient putClient = MockApptuitPutClient.getInstance();

    try (ScheduledReporter ignored = createReporter()) {

      UUID uuid = UUID.randomUUID();
      String metricName = "ApptuitReporterTest.testPutReporter." + uuid.toString();
      int expectedCount = 2;

      AtomicBoolean foundMetric = new AtomicBoolean(false);
      AtomicInteger lastSeenCount = new AtomicInteger(-1);
      DataListener listener = dataPoints -> {
        dataPoints.forEach(dataPoint -> {
          if (!metricName.equals(dataPoint.getMetric())) {
            return;
          }
          int i = dataPoint.getValue().intValue();
          lastSeenCount.set(i);
          if (i != 2) {
            return;
          }
          foundMetric.set(true);
        });
      };
      putClient.addPutListener(listener);

      Counter counter = registry.counter(metricName);
      counter.inc();
      counter.inc();

      await().atMost(period * 3, TimeUnit.SECONDS).untilTrue(foundMetric);
      putClient.removePutListener(listener);

      assertEquals(expectedCount, lastSeenCount.intValue());
    }
  }

  @Test
  public void testXCollectorReporter() throws Exception {
    MockXCollectorForwarder forwarder = MockXCollectorForwarder.getInstance();

    try (ScheduledReporter ignored = createReporter(ReportingMode.XCOLLECTOR)) {

      UUID uuid = UUID.randomUUID();
      String metricName = "ApptuitReporterTest.testXCollectorReporter." + uuid.toString();
      int expectedCount = 2;

      AtomicBoolean foundMetric = new AtomicBoolean(false);
      AtomicInteger lastSeenCount = new AtomicInteger(-1);
      DataListener listener = dataPoints -> {
        dataPoints.forEach(dataPoint -> {
          if (!metricName.equals(dataPoint.getMetric())) {
            return;
          }
          int i = dataPoint.getValue().intValue();
          lastSeenCount.set(i);
          if (i != 2) {
            return;
          }
          foundMetric.set(true);
        });
      };
      forwarder.addPutListener(listener);

      Counter counter = registry.counter(metricName);
      counter.inc();
      counter.inc();

      await().atMost(period * 3, TimeUnit.SECONDS).untilTrue(foundMetric);
      forwarder.removePutListener(listener);

      assertEquals(expectedCount, lastSeenCount.intValue());
    }
  }

  private ScheduledReporter createReporter() {
    return createReporter(ReportingMode.API_PUT);
  }

  private ScheduledReporter createReporter(ReportingMode mode) {
    ApptuitReporterFactory factory = new ApptuitReporterFactory();
    factory.setRateUnit(TimeUnit.SECONDS);
    factory.setDurationUnit(TimeUnit.MILLISECONDS);
    factory.addGlobalTag("globalTag1", "globalValue1");
    factory.setApiKey("dummy");

    factory.setReportingMode(mode);

    ScheduledReporter reporter;
    reporter = factory.build(registry);
    reporter.start(period, TimeUnit.SECONDS);
    return reporter;
  }
}

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

import ai.apptuit.metrics.client.DataPoint;
import ai.apptuit.metrics.dropwizard.ApptuitReporter.ReportingMode;
import ai.apptuit.metrics.dropwizard.BaseMockClient.DataListener;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
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
  private int period = 1;

  @Before
  public void setUp() throws Exception {
    registry = new MetricRegistry();
  }

  @After
  public void tearDown() throws Exception {
  }

  //TODO add test cases for Hist, Meter, Timer

  @Test
  public void testCounterPut() throws Exception {
    testCounter(ReportingMode.API_PUT,
        "testCounterPut." + UUID.randomUUID().toString());
  }

  @Test
  public void testCounterXCollector() throws Exception {
    testCounter(ReportingMode.XCOLLECTOR,
        "testCounterXCollector." + UUID.randomUUID().toString());
  }

  private void testCounter(ReportingMode reportingMode, String metricName) throws Exception {

    long expectedCount = 2;

    List<DataPoint> reportedPoints = new ArrayList<>();

    testMetric(reportingMode, () -> {
      Counter counter = registry.counter(metricName);
      for (int i = 0; i < expectedCount; i++) {
        counter.inc();
      }
    }, dataPoints -> {
      reportedPoints.clear();
      dataPoints.forEach(dataPoint -> {
        if (!metricName.equals(dataPoint.getMetric())) {
          return;
        }
        reportedPoints.add(dataPoint);
      });
    }, () -> reportedPoints.size() > 0);
    assertEquals(1, reportedPoints.size());
    assertEquals(expectedCount, reportedPoints.get(0).getValue());
  }

  @Test
  public void testGaugeDoublePut() throws Exception {
    testGaugeDouble(ReportingMode.API_PUT,
        "testGaugeDoublePut." + UUID.randomUUID().toString());
  }

  @Test
  public void testGaugeDoubleXCollector() throws Exception {
    testGaugeDouble(ReportingMode.XCOLLECTOR,
        "testGaugeDoubleXCollector." + UUID.randomUUID().toString());
  }

  private void testGaugeDouble(ReportingMode reportingMode, String metricName) throws Exception {
    double expectedValue = 2;
    testGauge(reportingMode, metricName, expectedValue);
  }


  @Test
  public void testGaugeBigDecimalPut() throws Exception {
    testGaugeBigDecimal(ReportingMode.API_PUT,
        "testGaugeBigDecimalPut." + UUID.randomUUID().toString());
  }

  @Test
  public void testGaugeBigDecimalXCollector() throws Exception {
    testGaugeBigDecimal(ReportingMode.XCOLLECTOR,
        "testGaugeBigDecimalXCollector." + UUID.randomUUID().toString());
  }

  private void testGaugeBigDecimal(ReportingMode reportingMode, String metricName)
      throws Exception {
    BigDecimal expectedValue = new BigDecimal(2);
    testGauge(reportingMode, metricName, expectedValue);
  }


  @Test
  public void testGaugeBigIntegerPut() throws Exception {
    testGaugeBigInteger(ReportingMode.API_PUT,
        "testGaugeBigIntegerPut." + UUID.randomUUID().toString());
  }

  @Test
  public void testGaugeBigIntegerXCollector() throws Exception {
    testGaugeBigInteger(ReportingMode.XCOLLECTOR,
        "testGaugeBigIntegerXCollector." + UUID.randomUUID().toString());
  }

  private void testGaugeBigInteger(ReportingMode reportingMode, String metricName)
      throws Exception {
    BigInteger expectedValue = new BigInteger("2");
    testGauge(reportingMode, metricName, expectedValue);
  }


  @Test
  public void testGaugeIntegerPut() throws Exception {
    testGaugeInteger(ReportingMode.API_PUT,
        "testGaugeIntegerPut." + UUID.randomUUID().toString());
  }

  @Test
  public void testGaugeIntegerXCollector() throws Exception {
    testGaugeInteger(ReportingMode.XCOLLECTOR,
        "testGaugeIntegerXCollector." + UUID.randomUUID().toString());
  }

  private void testGaugeInteger(ReportingMode reportingMode, String metricName)
      throws Exception {
    Integer expectedValue = new Integer("2");
    testGauge(reportingMode, metricName, expectedValue);
  }

  private void testGauge(ReportingMode reportingMode, String metricName, Number expectedValue)
      throws Exception {
    List<DataPoint> reportedPoints = new ArrayList<>();

    testMetric(reportingMode, () -> {
      registry.register(metricName, (Gauge<Number>) () -> expectedValue);
    }, dataPoints -> {
      reportedPoints.clear();
      dataPoints.forEach(dataPoint -> {
        if (!metricName.equals(dataPoint.getMetric())) {
          return;
        }
        reportedPoints.add(dataPoint);
      });
    }, () -> reportedPoints.size() > 0);
    assertEquals(1, reportedPoints.size());
    //ApptuitReporter reports all Gauges as double ... even if the gauge value is int
    assertEquals(expectedValue.doubleValue(), reportedPoints.get(0).getValue());
  }

  private void testMetric(ReportingMode reportingMode, Runnable metricUpdate, DataListener listener,
      Callable<Boolean> awaitUntil) throws Exception {

    BaseMockClient mockClient =
        reportingMode == ReportingMode.API_PUT ? MockApptuitPutClient.getInstance()
            : MockXCollectorForwarder.getInstance();

    try (ScheduledReporter ignored = createReporter(reportingMode)) {
      mockClient.addPutListener(listener);
      metricUpdate.run();
      await().atMost(period * 3, TimeUnit.SECONDS).until(awaitUntil);
      mockClient.removePutListener(listener);
    }
  }

  private ScheduledReporter createReporter(ReportingMode mode) {
    ApptuitReporterFactory factory = new ApptuitReporterFactory();
    factory.setRateUnit(TimeUnit.SECONDS);
    factory.setDurationUnit(TimeUnit.MILLISECONDS);
    factory.addGlobalTag("globalTag1", "globalValue1");
    if (mode == ReportingMode.API_PUT) {
      factory.setApiKey("dummy");
    }

    factory.setReportingMode(mode);

    ScheduledReporter reporter = factory.build(registry);
    reporter.start(period, TimeUnit.SECONDS);
    return reporter;
  }
}

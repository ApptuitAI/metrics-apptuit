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

import ai.apptuit.metrics.client.DataPoint;
import ai.apptuit.metrics.client.TagEncodedMetricName;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

/**
 * @author Rajiv Shivane
 */
public abstract class BaseReporterTest {
  protected MetricRegistry registry;

  @Test
  public void testCounterPut() throws Exception {
    testCounter(ApptuitReporter.ReportingMode.API_PUT,
            "testCounterPut." + UUID.randomUUID().toString());
  }

  //TODO add test cases for Hist, Meter, Timer
  @Test
  public void testCounterXCollector() throws Exception {
    testCounter(ApptuitReporter.ReportingMode.XCOLLECTOR,
            "testCounterXCollector." + UUID.randomUUID().toString());
  }

  private void testCounter(ApptuitReporter.ReportingMode reportingMode, String metricName) throws Exception {

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
    testGaugeDouble(ApptuitReporter.ReportingMode.API_PUT,
            "testGaugeDoublePut." + UUID.randomUUID().toString());
  }

  @Test
  public void testGaugeDoubleXCollector() throws Exception {
    testGaugeDouble(ApptuitReporter.ReportingMode.XCOLLECTOR,
            "testGaugeDoubleXCollector." + UUID.randomUUID().toString());
  }

  private void testGaugeDouble(ApptuitReporter.ReportingMode reportingMode, String metricName) throws Exception {
    double expectedValue = 2;
    testGauge(reportingMode, metricName, expectedValue);
  }

  @Test
  public void testGaugeBigDecimalPut() throws Exception {
    testGaugeBigDecimal(ApptuitReporter.ReportingMode.API_PUT,
            "testGaugeBigDecimalPut." + UUID.randomUUID().toString());
  }

  @Test
  public void testGaugeBigDecimalXCollector() throws Exception {
    testGaugeBigDecimal(ApptuitReporter.ReportingMode.XCOLLECTOR,
            "testGaugeBigDecimalXCollector." + UUID.randomUUID().toString());
  }

  private void testGaugeBigDecimal(ApptuitReporter.ReportingMode reportingMode, String metricName)
          throws Exception {
    BigDecimal expectedValue = new BigDecimal(2);
    testGauge(reportingMode, metricName, expectedValue);
  }

  @Test
  public void testGaugeBigIntegerPut() throws Exception {
    testGaugeBigInteger(ApptuitReporter.ReportingMode.API_PUT,
            "testGaugeBigIntegerPut." + UUID.randomUUID().toString());
  }

  @Test
  public void testGaugeBigIntegerXCollector() throws Exception {
    testGaugeBigInteger(ApptuitReporter.ReportingMode.XCOLLECTOR,
            "testGaugeBigIntegerXCollector." + UUID.randomUUID().toString());
  }

  private void testGaugeBigInteger(ApptuitReporter.ReportingMode reportingMode, String metricName)
          throws Exception {
    BigInteger expectedValue = new BigInteger("2");
    testGauge(reportingMode, metricName, expectedValue);
  }

  @Test
  public void testGaugeIntegerPut() throws Exception {
    testGaugeInteger(ApptuitReporter.ReportingMode.API_PUT,
            "testGaugeIntegerPut." + UUID.randomUUID().toString());
  }

  @Test
  public void testGaugeIntegerXCollector() throws Exception {
    testGaugeInteger(ApptuitReporter.ReportingMode.XCOLLECTOR,
            "testGaugeIntegerXCollector." + UUID.randomUUID().toString());
  }

  private void testGaugeInteger(ApptuitReporter.ReportingMode reportingMode, String metricName)
          throws Exception {
    Integer expectedValue = new Integer("2");
    testGauge(reportingMode, metricName, expectedValue);
  }

  private void testGauge(ApptuitReporter.ReportingMode reportingMode, String metricName, Number expectedValue)
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

  @Test
  public void testTimer() throws Exception {
    String metricName = "testTimer";
    String countMetric = metricName + ".count";

    List<DataPoint> reportedPoints = new LinkedList<>();
    Map<TagEncodedMetricName, DataPoint> reportedMetrics = new TreeMap<>();
    Set<TagEncodedMetricName> expectedMetrics = getExpectedTimers(metricName);

    testMetric(ApptuitReporter.ReportingMode.API_PUT, () -> {
      Timer timer = registry.timer(metricName);
      timer.update(250L, TimeUnit.MILLISECONDS);
    }, dataPoints -> {
      dataPoints.forEach(dataPoint -> {
        String reportedMetric = dataPoint.getMetric();
        if (!reportedMetric.startsWith(metricName + ".")) {
          return;
        }
        Map<String, String> filteredTags = new HashMap<>();
        dataPoint.getTags().forEach((k, v) -> {
          if (k.equals("quantile") || k.equals("window")) filteredTags.put(k, v);
        });
        reportedMetrics.put(TagEncodedMetricName.decode(reportedMetric).withTags(filteredTags), dataPoint);
        reportedPoints.add(dataPoint);
      });
    }, () -> reportedPoints.size() >= expectedMetrics.size() + 2);
    assertEquals(expectedMetrics, reportedMetrics.keySet());
    assertEquals(1L, reportedMetrics.get(TagEncodedMetricName.decode(countMetric)).getValue());

    List<DataPoint> countMetrics = new LinkedList<>();
    reportedPoints.forEach(dataPoint -> {
      if (dataPoint.getMetric().equals(countMetric)) countMetrics.add(dataPoint);
    });
    assertEquals(reportedPoints.size(), reportedMetrics.size() - 1 + countMetrics.size());
  }

  private Set<TagEncodedMetricName> getExpectedTimers(String metricName) {
    Set<TagEncodedMetricName> expectedMetrics = new TreeSet<>();
    TagEncodedMetricName root = TagEncodedMetricName.decode(metricName);
    expectedMetrics.add(root.submetric("count"));

    TagEncodedMetricName duration = root.submetric("duration");
    expectedMetrics.add(duration.submetric("min"));
    expectedMetrics.add(duration.submetric("max"));
    expectedMetrics.add(duration.submetric("mean"));
    expectedMetrics.add(duration.submetric("stddev"));
    expectedMetrics.add(duration.withTags("quantile", "0.5"));
    expectedMetrics.add(duration.withTags("quantile", "0.75"));
    expectedMetrics.add(duration.withTags("quantile", "0.95"));
    expectedMetrics.add(duration.withTags("quantile", "0.98"));
    expectedMetrics.add(duration.withTags("quantile", "0.99"));
    expectedMetrics.add(duration.withTags("quantile", "0.999"));

    TagEncodedMetricName rate = root.submetric("rate");
    expectedMetrics.add(rate.withTags("window", "1m"));
    expectedMetrics.add(rate.withTags("window", "5m"));
    expectedMetrics.add(rate.withTags("window", "15m"));

    return expectedMetrics;
  }

  protected abstract void testMetric(ApptuitReporter.ReportingMode reportingMode, Runnable metricUpdate, BaseMockClient.DataListener listener,
                                     Callable<Boolean> awaitUntil) throws Exception;
}

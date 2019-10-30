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
import com.codahale.metrics.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Rajiv Shivane
 */
public class DataPointCollector {
  private static final String QUANTILE_TAG_NAME = "quantile";
  private static final String WINDOW_TAG_NAME = "window";
  private static final String RATE_SUBMETRIC = "rate";

  private final long epoch;
  private final ApptuitReporter apptuitReporter;
  private final List<DataPoint> dataPoints;

  DataPointCollector(long epoch, ApptuitReporter apptuitReporter) {
    this.epoch = epoch;
    this.apptuitReporter = apptuitReporter;
    this.dataPoints = new LinkedList<>();
  }

  public void collectGauge(String name, Gauge gauge) {
    Object value = gauge.getValue();
    if (value instanceof BigDecimal) {
      addDataPoint(name, ((BigDecimal) value).doubleValue());
    } else if (value instanceof BigInteger) {
      addDataPoint(name, ((BigInteger) value).doubleValue());
    } else if (value != null && value.getClass().isAssignableFrom(Double.class)) {
      if (!Double.isNaN((Double) value) && Double.isFinite((Double) value)) {
        addDataPoint(name, (Double) value);
      }
    } else if (value instanceof Number) {
      addDataPoint(name, ((Number) value).doubleValue());
    }
  }

  public void collectCounter(String name, Counter counter) {
    addDataPoint(name, counter.getCount());
  }


  public void collectHistogram(String name, Histogram histogram) {
    TagEncodedMetricName rootMetric = TagEncodedMetricName.decode(name);
    collectCounting(rootMetric.submetric("count"), histogram, () -> reportSnapshot(rootMetric, histogram.getSnapshot()));
  }

  public void collectMeter(String name, Meter meter) {
    TagEncodedMetricName rootMetric = TagEncodedMetricName.decode(name);
    collectCounting(rootMetric.submetric("total"), meter, () -> reportMetered(rootMetric, meter));
  }

  public void collectTimer(String name, final Timer timer) {
    TagEncodedMetricName rootMetric = TagEncodedMetricName.decode(name);
    collectCounting(rootMetric.submetric("count"), timer, () -> {
      reportSnapshot(rootMetric.submetric("duration"), timer.getSnapshot());
      reportMetered(rootMetric, timer)
      ;
    });
  }

  public List<DataPoint> getDataPoints() {
    return dataPoints;
  }

  private <T extends Counting> void collectCounting(TagEncodedMetricName countMetric, T metric,
                                                    Runnable reportSubmetrics) {
    long currentCount = metric.getCount();
    addDataPoint(countMetric, currentCount);
    Long lastCount = apptuitReporter.lastReportedCount.put(countMetric, currentCount);
    if (lastCount == null || lastCount != currentCount) {
      reportSubmetrics.run();
    }
  }

  private void reportSnapshot(TagEncodedMetricName metric, Snapshot snapshot) {
    addDataPoint(metric.submetric("min"), convertDuration(snapshot.getMin()));
    addDataPoint(metric.submetric("max"), convertDuration(snapshot.getMax()));
    addDataPoint(metric.submetric("mean"), convertDuration(snapshot.getMean()));
    addDataPoint(metric.submetric("stddev"), convertDuration(snapshot.getStdDev()));
    addDataPoint(metric.withTags(QUANTILE_TAG_NAME, "0.5"), convertDuration(snapshot.getMedian()));
    addDataPoint(metric.withTags(QUANTILE_TAG_NAME, "0.75"), convertDuration(snapshot.get75thPercentile()));
    addDataPoint(metric.withTags(QUANTILE_TAG_NAME, "0.95"), convertDuration(snapshot.get95thPercentile()));
    addDataPoint(metric.withTags(QUANTILE_TAG_NAME, "0.98"), convertDuration(snapshot.get98thPercentile()));
    addDataPoint(metric.withTags(QUANTILE_TAG_NAME, "0.99"), convertDuration(snapshot.get99thPercentile()));
    addDataPoint(metric.withTags(QUANTILE_TAG_NAME, "0.999"), convertDuration(snapshot.get999thPercentile()));
  }

  private void reportMetered(TagEncodedMetricName metric, Metered meter) {
    addDataPoint(metric.submetric(RATE_SUBMETRIC).withTags(WINDOW_TAG_NAME, "1m"),
        convertRate(meter.getOneMinuteRate()));
    addDataPoint(metric.submetric(RATE_SUBMETRIC).withTags(WINDOW_TAG_NAME, "5m"),
        convertRate(meter.getFiveMinuteRate()));
    addDataPoint(metric.submetric(RATE_SUBMETRIC).withTags(WINDOW_TAG_NAME, "15m"),
        convertRate(meter.getFifteenMinuteRate()));
    //addDataPoint(rootMetric.submetric("rate", "window", "all"), epoch, meter.getMeanRate());
  }

  private double convertRate(double rate) {
    return apptuitReporter.convertRate(rate);
  }

  private double convertDuration(double duration) {
    return apptuitReporter.convertDuration(duration);
  }

  private void addDataPoint(String name, double value) {
    addDataPoint(TagEncodedMetricName.decode(name), value);
  }

  private void addDataPoint(String name, long value) {
    addDataPoint(TagEncodedMetricName.decode(name), value);
  }

  private void addDataPoint(TagEncodedMetricName name, Number value) {
    /*
    //TODO support disabled metric attributes
    if(getDisabledMetricAttributes().contains(type)) {
        return;
    }
    */

    DataPoint dataPoint = new DataPoint(name.getMetricName(), epoch, value, name.getTags());
    dataPoints.add(dataPoint);
    ApptuitReporter.debug(dataPoint);
  }
}

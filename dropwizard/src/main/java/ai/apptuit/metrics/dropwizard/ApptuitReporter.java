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

import ai.apptuit.metrics.client.*;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.codahale.metrics.Timer;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Rajiv Shivane
 */
public class ApptuitReporter extends ScheduledReporter {

  private static final Logger LOGGER = Logger.getLogger(ApptuitReporter.class.getName());
  private static final boolean DEBUG = false;
  private static final ReportingMode DEFAULT_REPORTING_MODE = ReportingMode.API_PUT;
  private static final String REPORTER_NAME = "apptuit-reporter";

  private final Timer buildReportTimer;
  private final Timer sendReportTimer;
  private final Counter metricsSentCounter;
  private final Counter pointsSentCounter;
  private final DataPointsSender dataPointsSender;
  final Map<TagEncodedMetricName, Long> lastReportedCount = new HashMap<>();

  protected ApptuitReporter(MetricRegistry registry, MetricFilter filter, TimeUnit rateUnit,
                            TimeUnit durationUnit, Map<String, String> globalTags,
                            String key, URL apiUrl,
                            ReportingMode reportingMode, Sanitizer sanitizer,
                            SendErrorHandler errorHandler) {
    this(registry, filter, rateUnit, durationUnit,
        getDataPointSender(globalTags, key, apiUrl, reportingMode, sanitizer, errorHandler));
  }

  protected ApptuitReporter(MetricRegistry registry, MetricFilter filter, TimeUnit rateUnit,
                            TimeUnit durationUnit, DataPointsSender sender) {
    super(registry, REPORTER_NAME, filter, rateUnit, durationUnit);

    this.buildReportTimer = registry.timer("apptuit.reporter.report.build");
    this.sendReportTimer = registry.timer("apptuit.reporter.report.send");
    this.metricsSentCounter = registry.counter("apptuit.reporter.metrics.sent.count");
    this.pointsSentCounter = registry.counter("apptuit.reporter.points.sent.count");
    this.dataPointsSender = sender;
  }

  private static DataPointsSender getDataPointSender(Map<String, String> globalTags, String key, URL apiUrl,
                                                     ReportingMode reportingMode, Sanitizer sanitizer,
                                                     SendErrorHandler errorHandler) {
    if (reportingMode == null) {
      reportingMode = DEFAULT_REPORTING_MODE;
    }

    switch (reportingMode) {
      case NO_OP:
        return dataPoints -> {
        };
      case SYS_OUT:
        return dataPoints -> {
          dataPoints.forEach(dp -> dp.toTextLine(System.out, globalTags, sanitizer));
        };
      case XCOLLECTOR:
        XCollectorForwarder forwarder = new XCollectorForwarder(globalTags);
        return dataPoints -> forwarder.forward(dataPoints, sanitizer);
      case API_PUT:
      default:
        ApptuitPutClient putClient = new ApptuitPutClient(key, globalTags, apiUrl);
        return dataPoints -> {
          try {
            putClient.send(dataPoints, sanitizer);
          } catch (IOException e) {
            if (errorHandler != null) {
              errorHandler.handle(e);
            } else {
              LOGGER.log(Level.SEVERE, "Error Sending Datapoints", e);
            }
          }
        };
    }
  }

  @Override
  public void report(SortedMap<String, Gauge> gauges, SortedMap<String, Counter> counters,
                     SortedMap<String, Histogram> histograms, SortedMap<String, Meter> meters,
                     SortedMap<String, Timer> timers) {

    DataPointCollector collector = new DataPointCollector(System.currentTimeMillis() / 1000, this);
    try {
      long t0 = System.currentTimeMillis();
      debug("################");
      debug(">>>>>>>> Guages <<<<<<<<<");
      gauges.forEach(collector::collectGauge);
      debug(">>>>>>>> Counters <<<<<<<<<");
      counters.forEach(collector::collectCounter);
      debug(">>>>>>>> Histograms <<<<<<<<<");
      histograms.forEach(collector::collectHistogram);
      debug(">>>>>>>> Meters <<<<<<<<<");
      meters.forEach(collector::collectMeter);
      debug(">>>>>>>> Timers <<<<<<<<<");
      timers.forEach(collector::collectTimer);

      debug("################");
      int numMetrics = gauges.size() + counters.size() + histograms.size() + meters.size() + timers.size();
      metricsSentCounter.inc(numMetrics);
      pointsSentCounter.inc(collector.getDataPoints().size());

      buildReportTimer.update(System.currentTimeMillis() - t0, TimeUnit.MILLISECONDS);
    } catch (Exception | Error e) {
      LOGGER.log(Level.SEVERE, "Error building metrics.", e);
    }

    try {
      long t1 = System.currentTimeMillis();
      Collection<DataPoint> dataPoints = collector.getDataPoints();
      dataPointsSender.send(dataPoints);
      //dataPoints.forEach(System.out::println);
      sendReportTimer.update(System.currentTimeMillis() - t1, TimeUnit.MILLISECONDS);
    } catch (Exception | Error e) {
      LOGGER.log(Level.SEVERE, "Error reporting metrics.", e);
    }

  }

  @Override
  protected double convertDuration(double duration) {
    return super.convertDuration(duration);
  }

  @Override
  protected double convertRate(double rate) {
    return super.convertRate(rate);
  }

  static void debug(Object s) {
    if (DEBUG) {
      System.out.println(s);
    }
  }

  public enum ReportingMode {
    NO_OP, SYS_OUT, XCOLLECTOR, API_PUT
  }

  public interface DataPointsSender {

    void send(Collection<DataPoint> dataPoints);
  }

}

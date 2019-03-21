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

package ai.apptuit.metrics.micrometer_registry_apptuit;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.Random;
import java.util.concurrent.ThreadFactory;

import io.micrometer.core.instrument.util.NamedThreadFactory;
import org.junit.Test;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.Measurement;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MockClock;
import io.micrometer.core.instrument.Statistic;
import io.micrometer.core.instrument.Timer;

public class ApptuitMeterRegistryTest {
	private MockClock clock = new MockClock();
    private ApptuitConfig config = new ApptuitConfig() {
        @Override
        public String get(String key) {
            return null;
        }

        @Override
        public boolean enabled() {
            return true;
        }
    };
    private static final ThreadFactory DEFAULT_THREAD_FACTORY = new NamedThreadFactory("apptuit-metrics-publisher");
    private ApptuitMeterRegistry registry = ApptuitMeterRegistry.builder(config).clock(clock).build();
    private ApptuitMeterRegistry.DataPointCollector collector = registry.new DataPointCollector(System.currentTimeMillis() / 1000);
    private Random r = new Random();
    private int rangeMin = 1;
    private int rangeMax = 1000;

    @Test
    public void objectCreationUsingBuilder(){
        ApptuitMeterRegistry register = ApptuitMeterRegistry.builder(config).clock(clock).threadFactory(DEFAULT_THREAD_FACTORY).build();
        ApptuitMeterRegistry.DataPointCollector collect = register.new DataPointCollector(System.currentTimeMillis() / 1000);
        Counter count = Counter.builder("demo.counter").register(register);
        collect.collectCounter(count.getId().getName(),count);
        assertEquals(0.0,collect.dataPoints.get(0).getValue());
        assertEquals("demo.counter.total",collect.dataPoints.get(0).getMetric());
        collect.dataPoints.clear();
    }
     
    @Test
    public void testCounter() {
    	Counter counter = Counter.builder("my.Counter").baseUnit("seconds").tag("type","testing").register(registry);
        counter.increment();
        counter.increment();
        //FunctionTimer t = FunctionTimer.builder("my.FunctionTimer").register(registry);
        clock.add(config.step());
        collector.collectCounter(counter.getId().getName(),counter);
        registry.publish();
        assertEquals(1,collector.dataPoints.size());
        assertEquals(2.0,collector.dataPoints.get(0).getValue());
        assertEquals("my.Counter.total",collector.dataPoints.get(0).getMetric());
        assertEquals(1,collector.dataPoints.get(0).getTags().size());
        collector.dataPoints.clear();
        Counter counter1 = Counter.builder("my.Counter").register(registry);
        collector.collectCounter(counter1.getId().getName(),counter1);
        assertEquals(1,collector.dataPoints.size());
        assertEquals(0.0,collector.dataPoints.get(0).getValue());
        assertEquals("my.Counter.total",collector.dataPoints.get(0).getMetric());
        collector.dataPoints.clear();
    }
    
    @Test
    public void testGuage() {
    	double randomValue = rangeMin + (rangeMax - rangeMin) * r.nextDouble();
        Gauge gauge = Gauge.builder("my.Gauge", randomValue, Number::doubleValue).baseUnit("bytes").tag("type","testing").register(registry);
    	collector.collectGauge(gauge.getId().getName(),gauge);
        registry.publish();
    	assertEquals(1,collector.dataPoints.size());
    	assertEquals(randomValue,collector.dataPoints.get(0).getValue());
    	assertEquals("my.Gauge.bytes",collector.dataPoints.get(0).getMetric());
        assertEquals(1,collector.dataPoints.get(0).getTags().size());
    	collector.dataPoints.clear();
        Gauge gauge1 = Gauge.builder("my.Gauge", randomValue, Number::doubleValue).register(registry);
        collector.collectGauge(gauge1.getId().getName(),gauge1);
        assertEquals(1,collector.dataPoints.size());
        assertEquals(randomValue,collector.dataPoints.get(0).getValue());
        assertEquals("my.Gauge",collector.dataPoints.get(0).getMetric());
        collector.dataPoints.clear();
    }
    
    @Test
    public void testFunctionCounter() {
    	FunctionCounter counter = FunctionCounter.builder("my.Function.Counter",0.0, Number::doubleValue).tag("type","testing").register(registry);
    	collector.collectFunctionCounter(counter.getId().getName(), counter);
        registry.publish();
    	assertEquals(1,collector.dataPoints.size());
    	assertEquals(0.0,collector.dataPoints.get(0).getValue());
    	assertEquals("my.Function.Counter.total",collector.dataPoints.get(0).getMetric());
        assertEquals(1,collector.dataPoints.get(0).getTags().size());
    	collector.dataPoints.clear();
    }
    
    @Test
    public void testHistogram() {
    	DistributionSummary histogram = DistributionSummary.builder("my.Histogram").baseUnit("seconds").tag("type","testing").publishPercentiles(0.95).register(registry);
    	collector.collectHistogram(histogram.getId().getName(), histogram);
        registry.publish();
    	assertEquals(4,collector.dataPoints.size());
    	assertEquals(0.0,collector.dataPoints.get(0).getValue());
    	assertEquals("my.Histogram."+registry.getBaseTimeUnit().toString().toLowerCase()+".duration.max",collector.dataPoints.get(0).getMetric());
    	assertEquals("my.Histogram."+registry.getBaseTimeUnit().toString().toLowerCase()+".duration.mean",collector.dataPoints.get(1).getMetric());
    	assertEquals("my.Histogram."+registry.getBaseTimeUnit().toString().toLowerCase()+".duration.count",collector.dataPoints.get(2).getMetric());
        assertEquals(1,collector.dataPoints.get(0).getTags().size());
    	collector.dataPoints.clear();
    }
    
    @Test
    public void testTimer() {
    	Timer timer = Timer.builder("my.Timer").tag("type","testing").register(registry);
    	collector.collectTimer(timer.getId().getName(), timer);
        registry.publish();
    	assertEquals(3,collector.dataPoints.size());
    	assertEquals(0.0,collector.dataPoints.get(0).getValue());
    	assertEquals("my.Timer."+registry.getBaseTimeUnit().toString().toLowerCase()+".duration.max",collector.dataPoints.get(0).getMetric());
    	assertEquals("my.Timer."+registry.getBaseTimeUnit().toString().toLowerCase()+".duration.mean",collector.dataPoints.get(1).getMetric());
    	assertEquals("my.Timer."+registry.getBaseTimeUnit().toString().toLowerCase()+".duration.count",collector.dataPoints.get(2).getMetric());
        assertEquals(1,collector.dataPoints.get(0).getTags().size());
    	collector.dataPoints.clear();
    }
    
    @Test
    public void testLongTaskTimer() {
    	LongTaskTimer timer = LongTaskTimer.builder("my.LongTaskTimer").tag("type","testing").register(registry);
    	collector.collectLongTaskTimer(timer.getId().getName(), timer);
        registry.publish();
    	assertEquals(2,collector.dataPoints.size());
    	assertEquals("my.LongTaskTimer.activeTasks",collector.dataPoints.get(0).getMetric());
    	assertEquals("my.LongTaskTimer."+registry.getBaseTimeUnit().toString().toLowerCase()+".duration",collector.dataPoints.get(1).getMetric());
        assertEquals(1,collector.dataPoints.get(0).getTags().size());
    	collector.dataPoints.clear();
    }
    
    @Test
    public void testMeter() {
    	Meter meter = Meter.builder("my.Meter",Meter.Type.COUNTER,Collections.singletonList(new Measurement(() -> 1.0, Statistic.COUNT))).tag("type","testing").register(registry);
    	collector.collectMeter(meter.getId().getName(),meter);
        registry.publish();
    	assertEquals(1,collector.dataPoints.size());
    	assertEquals("my.Meter.count",collector.dataPoints.get(0).getMetric());
    	assertEquals(1.0,collector.dataPoints.get(0).getValue());
        assertEquals(1,collector.dataPoints.get(0).getTags().size());
    	collector.dataPoints.clear();
    }
    
    
}

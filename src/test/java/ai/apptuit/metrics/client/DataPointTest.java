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

import ai.apptuit.metrics.dropwizard.TagEncodedMetricName;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * @author Rajiv Shivane
 */

public class DataPointTest {

  private TagEncodedMetricName tagEncodedMetricName;

  @Before
  public void setUp() throws Exception {
    tagEncodedMetricName = TagEncodedMetricName.decode("proc.stat.cpu")
            .withTags("host", "myhost", "type", "idle");
  }

  @Test
  public void testNotEqualsNull() throws Exception {
    long epoch = System.currentTimeMillis();
    long value = 1515;
    DataPoint dp = new DataPoint(tagEncodedMetricName.getMetricName(), epoch, value,
            Collections.emptyMap());
    assertNotEquals(dp, null);
  }

  @Test
  public void testNotEqualsString() throws Exception {
    long epoch = System.currentTimeMillis();
    long value = 1515;
    DataPoint dp = new DataPoint(tagEncodedMetricName.getMetricName(), epoch, value,
            Collections.emptyMap());
    assertNotEquals(dp, "Text");
  }

  @Test
  public void testEqualsSelf() throws Exception {
    long epoch = System.currentTimeMillis();
    long value = 1515;
    DataPoint dp = new DataPoint(tagEncodedMetricName.getMetricName(), epoch, value,
            Collections.emptyMap());
    assertEquals(dp, dp);
  }

  @Test
  public void testEqualsNoTags() throws Exception {
    long epoch = System.currentTimeMillis();
    long value = 1515;
    DataPoint dp1 = new DataPoint(tagEncodedMetricName.getMetricName(), epoch, value,
            Collections.emptyMap());
    DataPoint dp2 = new DataPoint(tagEncodedMetricName.getMetricName(), epoch, value,
            Collections.emptyMap());
    assertEquals(dp1, dp2);
    assertEquals(dp1.hashCode(), dp2.hashCode());
  }


  @Test
  public void testEqualsWithTags() throws Exception {
    long epoch = System.currentTimeMillis();
    long value = 1515;
    DataPoint dp1 = new DataPoint(tagEncodedMetricName.getMetricName(), epoch, value,
            tagEncodedMetricName.getTags());
    DataPoint dp2 = new DataPoint(tagEncodedMetricName.getMetricName(), epoch, value,
            tagEncodedMetricName.getTags());
    assertEquals(dp1, dp2);
    assertEquals(dp1.hashCode(), dp2.hashCode());
  }

  @Test
  public void testNotEqualsName() throws Exception {
    long epoch = System.currentTimeMillis();
    long value = 1515;
    DataPoint dp1 = new DataPoint(tagEncodedMetricName.getMetricName(), epoch, value,
            tagEncodedMetricName.getTags());
    DataPoint dp2 = new DataPoint(tagEncodedMetricName.getMetricName() + "x", epoch, value,
            tagEncodedMetricName.getTags());
    assertNotEquals(dp1, dp2);
    assertNotEquals(dp1.hashCode(), dp2.hashCode());
  }

  @Test
  public void testNotEqualsTime() throws Exception {
    long epoch = System.currentTimeMillis();
    long value = 1515;
    DataPoint dp1 = new DataPoint(tagEncodedMetricName.getMetricName(), epoch, value,
            tagEncodedMetricName.getTags());
    DataPoint dp2 = new DataPoint(tagEncodedMetricName.getMetricName(), epoch + 1, value,
            tagEncodedMetricName.getTags());
    assertNotEquals(dp1, dp2);
    assertNotEquals(dp1.hashCode(), dp2.hashCode());
  }

  @Test
  public void testNotEqualsValue() throws Exception {
    long epoch = System.currentTimeMillis();
    long value = 1515;
    DataPoint dp1 = new DataPoint(tagEncodedMetricName.getMetricName(), epoch, value,
            tagEncodedMetricName.getTags());
    DataPoint dp2 = new DataPoint(tagEncodedMetricName.getMetricName(), epoch, value + 1,
            tagEncodedMetricName.getTags());
    assertNotEquals(dp1, dp2);
    assertNotEquals(dp1.hashCode(), dp2.hashCode());
  }

  @Test
  public void testNotEqualsTags() throws Exception {
    long epoch = System.currentTimeMillis();
    long value = 1515;
    Map<String, String> tags = tagEncodedMetricName.getTags();
    DataPoint dp1 = new DataPoint(tagEncodedMetricName.getMetricName(), epoch, value, tags);
    Map<String, String> newtags = new HashMap<>(tags);
    newtags.put("key", "value");
    DataPoint dp2 = new DataPoint(tagEncodedMetricName.getMetricName(), epoch, value, newtags);
    assertNotEquals(dp1, dp2);
    assertNotEquals(dp1.hashCode(), dp2.hashCode());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNullTags() throws Exception {
    new DataPoint(tagEncodedMetricName.getMetricName(), System.currentTimeMillis(), 1515, null);
  }

  @Test
  public void testToStringNoTags() throws Exception {
    long epoch = System.currentTimeMillis();
    long value = 1515;
    DataPoint dataPoint = new DataPoint(tagEncodedMetricName.getMetricName(), epoch, value,
            Collections.emptyMap());

    assertEquals("proc.stat.cpu " + epoch + " " + value, dataPoint.toString());
  }

  @Test
  public void testToStringWithTags() throws Exception {
    long epoch = System.currentTimeMillis();
    long value = 1515;
    DataPoint dataPoint = new DataPoint(tagEncodedMetricName.getMetricName(),
            epoch, value, tagEncodedMetricName.getTags());

    assertEquals("proc.stat.cpu " + epoch + " " + value + " host=myhost type=idle",
            dataPoint.toString());
  }

  @Test
  public void testToTextNoTags() throws Exception {
    long epoch = System.currentTimeMillis();
    long value = 1515;
    DataPoint dataPoint = new DataPoint(tagEncodedMetricName.getMetricName(), epoch, value,
            Collections.emptyMap());

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    dataPoint.toTextLine(out, null, Sanitizer.NO_OP_SANITZER);

    assertEquals("proc.stat.cpu " + epoch + " " + value + "\n", out.toString());
  }

  @Test
  public void testToTextWithTags() throws Exception {
    long epoch = System.currentTimeMillis();
    long value = 1515;
    DataPoint dataPoint = new DataPoint(tagEncodedMetricName.getMetricName(),
            epoch, value, tagEncodedMetricName.getTags());

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    dataPoint.toTextLine(out, null, Sanitizer.NO_OP_SANITZER);

    assertEquals("proc.stat.cpu " + epoch + " " + value + " host=myhost type=idle\n",
            out.toString());
  }

  @Test
  public void testToTextWithGlobalTags() throws Exception {
    long epoch = System.currentTimeMillis();
    long value = 1515;
    DataPoint dataPoint = new DataPoint(tagEncodedMetricName.getMetricName(),
            epoch, value, Collections.emptyMap());

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    dataPoint.toTextLine(out, tagEncodedMetricName.getTags(), Sanitizer.NO_OP_SANITZER);

    assertEquals("proc.stat.cpu " + epoch + " " + value + " host=myhost type=idle\n",
            out.toString());
  }

  @Test
  public void testToJsonNoTags() throws Exception {
    long epoch = System.currentTimeMillis();
    long value = 1515;
    DataPoint dataPoint = new DataPoint(tagEncodedMetricName.getMetricName(), epoch, value,
            Collections.emptyMap());

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    dataPoint.toJson(new PrintStream(out), null, Sanitizer.NO_OP_SANITZER);
    String jsonTxt = out.toString();

    DataPoint dp = Util.jsonToDataPoint(jsonTxt);
    assertEquals(dataPoint, dp);
  }

  @Test
  public void testToJsonWithTags() throws Exception {
    long epoch = System.currentTimeMillis();
    long value = 1515;
    DataPoint dataPoint = new DataPoint(tagEncodedMetricName.getMetricName(),
            epoch, value, tagEncodedMetricName.getTags());

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    dataPoint.toJson(new PrintStream(out), null, Sanitizer.NO_OP_SANITZER);
    String jsonTxt = out.toString();

    DataPoint dp = Util.jsonToDataPoint(jsonTxt);
    assertEquals(dataPoint, dp);
  }

  @Test
  public void testToJsonWithGlobalTags() throws Exception {
    long epoch = System.currentTimeMillis();
    long value = 1515;
    DataPoint dataPoint = new DataPoint(tagEncodedMetricName.getMetricName(),
            epoch, value, Collections.emptyMap());

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    dataPoint.toJson(new PrintStream(out), tagEncodedMetricName.getTags(), Sanitizer.NO_OP_SANITZER);
    String jsonTxt = out.toString();

    DataPoint dp = Util.jsonToDataPoint(jsonTxt);
    DataPoint expectedDataPoint = new DataPoint(tagEncodedMetricName.getMetricName(),
            epoch, value, tagEncodedMetricName.getTags());
    assertEquals(expectedDataPoint, dp);
  }

  @Test
  public void testToPromSanitization() throws Exception {
    long epoch = System.currentTimeMillis();
    long value = 1515;
    tagEncodedMetricName = TagEncodedMetricName.decode("1proc.stat$cpu")
            .withTags("3host", "2myhost", "type__4", "idle");
    DataPoint dataPoint1 = new DataPoint(tagEncodedMetricName.getMetricName(),
            epoch, value, Collections.emptyMap());

    ByteArrayOutputStream out1 = new ByteArrayOutputStream();
    dataPoint1.toTextLine(out1, tagEncodedMetricName.getTags(), Sanitizer.PROMETHEUS_SANITZER);

    assertEquals("_1proc_stat_cpu " + epoch + " " + value + " _3host=2myhost type_4=idle\n",
            out1.toString());

    tagEncodedMetricName = TagEncodedMetricName.decode("1proc.stat_わcpu")
            .withTags("3host", "2myhost_わ", "type__4", "idle");
    DataPoint dataPoint2 = new DataPoint(tagEncodedMetricName.getMetricName(),
            epoch, value, Collections.emptyMap());

    ByteArrayOutputStream out2 = new ByteArrayOutputStream();
    dataPoint2.toTextLine(out2, tagEncodedMetricName.getTags(), Sanitizer.PROMETHEUS_SANITZER);

    assertEquals("_1proc_stat_cpu " + epoch + " " + value + " _3host=2myhost_わ type_4=idle\n",
            out2.toString());
  }

  @Test
  public void testToApptuitSanitization() throws Exception {
    long epoch = System.currentTimeMillis();
    long value = 1515;
    tagEncodedMetricName = TagEncodedMetricName.decode("1proc.stat$cpu_わ")
            .withTags("3host", "2-myhost", "type__4", "idle");
    DataPoint dataPoint1 = new DataPoint(tagEncodedMetricName.getMetricName(),
            epoch, value, Collections.emptyMap());

    ByteArrayOutputStream out1 = new ByteArrayOutputStream();
    dataPoint1.toTextLine(out1, tagEncodedMetricName.getTags(), Sanitizer.APPTUIT_SANITZER);

    assertEquals("1proc.stat_cpu_わ " + epoch + " " + value + " 3host=2-myhost type_4=idle\n",
            out1.toString());


    tagEncodedMetricName = TagEncodedMetricName.decode("1proc.stat$cpu")
            .withTags("3.host", "2-myhost", "type_/4", "idle");
    DataPoint dataPoint2 = new DataPoint(tagEncodedMetricName.getMetricName(),
            epoch, value, Collections.emptyMap());

    ByteArrayOutputStream out2 = new ByteArrayOutputStream();
    dataPoint2.toTextLine(out2, tagEncodedMetricName.getTags(), Sanitizer.APPTUIT_SANITZER);

    assertEquals("1proc.stat_cpu " + epoch + " " + value + " 3.host=2-myhost type_/4=idle\n",
            out2.toString());
  }

  @Test
  public void testToNoOpSanitization() throws Exception {
    long epoch = System.currentTimeMillis();
    long value = 1515;
    tagEncodedMetricName = TagEncodedMetricName.decode("1proc.stat$cpu_わ")
            .withTags("3host", "2-myhost", "type__4", "idle");
    DataPoint dataPoint1 = new DataPoint(tagEncodedMetricName.getMetricName(),
            epoch, value, Collections.emptyMap());

    ByteArrayOutputStream out1 = new ByteArrayOutputStream();
    dataPoint1.toTextLine(out1, tagEncodedMetricName.getTags(), Sanitizer.NO_OP_SANITZER);

    assertEquals("1proc.stat$cpu_わ " + epoch + " " + value + " 3host=2-myhost type__4=idle\n",
            out1.toString());


    tagEncodedMetricName = TagEncodedMetricName.decode("1proc.stat$cpu")
            .withTags("3.host", "2-myhost", "type_/4", "idle");
    DataPoint dataPoint2 = new DataPoint(tagEncodedMetricName.getMetricName(),
            epoch, value, Collections.emptyMap());

    ByteArrayOutputStream out2 = new ByteArrayOutputStream();
    dataPoint2.toTextLine(out2, tagEncodedMetricName.getTags(), Sanitizer.NO_OP_SANITZER);

    assertEquals("1proc.stat$cpu " + epoch + " " + value + " 3.host=2-myhost type_/4=idle\n",
            out2.toString());
  }

}

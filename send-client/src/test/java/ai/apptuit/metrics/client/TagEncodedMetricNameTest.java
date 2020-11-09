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

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * @author Rajiv Shivane
 */

public class TagEncodedMetricNameTest {

  private TagEncodedMetricName encodedMetricName;

  @Before
  public void setUp() throws Exception {
    encodedMetricName = TagEncodedMetricName.decode("asdf");
  }

  @Test
  public void testParseMultipleTags() throws Exception {
    TagEncodedMetricName metric = TagEncodedMetricName.decode("asdf[k:0, k2:7]");
    assertEquals("asdf", metric.getMetricName());
    Map<String, String> expectedTags = new HashMap<>();
    expectedTags.put("k", "0");
    expectedTags.put("k2", "7");
    assertEquals(expectedTags, metric.getTags());
  }

  @Test
  public void testParseSingleTag() throws Exception {
    TagEncodedMetricName metric;
    metric = TagEncodedMetricName.decode("asdf[k:0]");
    assertEquals("asdf", metric.getMetricName());
    Map<String, String> expectedTags = new HashMap<>();
    expectedTags.put("k", "0");
    assertEquals(expectedTags, metric.getTags());
  }

  @Test
  public void testParseNameOnlyNoTags() throws Exception {
    TagEncodedMetricName metric;
    metric = encodedMetricName;
    assertEquals("asdf", metric.getMetricName());
    Map<String, String> expectedTags = new HashMap<>();
    assertEquals(expectedTags, metric.getTags());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testMissingColonInTagFails() throws Exception {
    TagEncodedMetricName.decode("asdf[k:0, k27]");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testMissingTagValueFails() throws Exception {
    TagEncodedMetricName.decode("asdf[k:0, k27:]");
  }

  @Test
  public void testTrailingCommaIsOk() throws Exception {
    TagEncodedMetricName t = TagEncodedMetricName.decode("asdf[k:0, ]");
    TagEncodedMetricName decoded = TagEncodedMetricName.decode("asdf").withTags("k", "0");
    assertEquals(t, decoded);
  }


  @Test(expected = IllegalArgumentException.class)
  public void testMissingTagValueQuoteFails() throws Exception {
    TagEncodedMetricName.decode("asdf[k:0, k27:\"]");
  }


  @Test(expected = IllegalArgumentException.class)
  public void testEmptyQuotedTagValueFails() throws Exception {
    TagEncodedMetricName.decode("asdf[k:0, k27:\"\"]");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testTagNoValueFails() throws Exception {
    encodedMetricName.withTags("key");
  }

  @Test
  public void testEqualsNoTags() throws Exception {
    assertEquals(encodedMetricName, encodedMetricName);
  }

  @Test
  public void testEqualsSubmetric() throws Exception {
    TagEncodedMetricName m1 = encodedMetricName.submetric("pqr");
    TagEncodedMetricName m2 = TagEncodedMetricName.decode("asdf.pqr");
    assertEquals(m1, m2);
    assertEquals(m1.hashCode(), m2.hashCode());
  }

  @Test
  public void testEqualsSingleTag() throws Exception {
    TagEncodedMetricName m1 = encodedMetricName.submetric("pqr").withTags("k", "v");
    TagEncodedMetricName m2 = TagEncodedMetricName.decode("asdf.pqr[k:v]");
    assertEquals(m1, m2);
    assertEquals(m1.hashCode(), m2.hashCode());
  }

  @Test
  public void testEqualsMultipleTags() throws Exception {
    TagEncodedMetricName m1 = encodedMetricName
        .submetric("pqr").withTags("k1", "v1", "k2", "v2");
    TagEncodedMetricName m2 = TagEncodedMetricName.decode("asdf.pqr[k1:v1,k2:v2]");
    assertEquals(m1, m2);
    assertEquals(m1.hashCode(), m2.hashCode());
  }

  @Test
  public void testEqualsSelf() throws Exception {
    assertTrue(encodedMetricName.equals(encodedMetricName));
  }

  @Test
  public void testUnequal() throws Exception {
    assertFalse(encodedMetricName.equals(null));
    assertFalse(encodedMetricName.equals(new Object()));
    assertFalse(encodedMetricName.equals(TagEncodedMetricName.decode("asdf.pqr")));
    assertFalse(encodedMetricName.equals(TagEncodedMetricName.decode("asdf[k:v]")));
  }


  @Test
  public void testSubmetricForMetricWithTags() throws Exception {
    assertEquals(encodedMetricName.withTags("k1", "v1").submetric("pqr").withTags("k2", "v2"),
        encodedMetricName.submetric("pqr").withTags("k1", "v1").withTags("k2", "v2"));
  }

  @Test
  public void testSubmetricForMetricWithTagMap() throws Exception {
    Map<String, String> map = new HashMap<>();
    map.put("k2", "v2");
    assertEquals(encodedMetricName.withTags("k1", "v1").submetric("pqr").withTags(map),
        encodedMetricName.submetric("pqr").withTags("k1", "v1").withTags("k2", "v2"));
  }

  @Test
  public void testIgnoreTagOrder() throws Exception {
    TagEncodedMetricName t1 = encodedMetricName
        .withTags("k1", "v1")
        .withTags("k2", "v2")
        .withTags("a", "b");
    TagEncodedMetricName t2 = encodedMetricName
        .withTags("a", "b")
        .withTags("k2", "v2")
        .withTags("k1", "v1");
    assertEquals(t1.toString(), t2.toString());
  }

  @Test
  public void testQuotesInTagValue() throws Exception {
    String val = "Quoted\"Value\"";
    String decodedVal = encodeAndDecodeTagValue(val);
    assertEquals(val, decodedVal);
  }

  @Test
  public void testCommaInTagValue() throws Exception {
    String val = "Comma, separated, value";
    String decodedVal = encodeAndDecodeTagValue(val);
    assertEquals(val, decodedVal);
  }

  @Test
  public void testColonInTagValue() throws Exception {
    String val = "colon:value";
    String decodedVal = encodeAndDecodeTagValue(val);
    assertEquals(val, decodedVal);
  }

  private String encodeAndDecodeTagValue(String val) {
    String key = "key";
    TagEncodedMetricName t = encodedMetricName.withTags(key, val);
    TagEncodedMetricName decoded = TagEncodedMetricName.decode(t.toString());
    return decoded.getTags().get(key);
  }

  @Test
  public void testQuotedUnquotedValues() {
    TagEncodedMetricName t = encodedMetricName.withTags("a", "b").withTags("q", "r,s,t");
    TagEncodedMetricName decoded = TagEncodedMetricName.decode(t.toString());
    assertEquals(t, decoded);
  }

}

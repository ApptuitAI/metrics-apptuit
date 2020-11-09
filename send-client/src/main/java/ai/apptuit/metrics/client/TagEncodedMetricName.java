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

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Rajiv Shivane
 */
public class TagEncodedMetricName implements Comparable<TagEncodedMetricName> {

  private static final Pattern TAG_ENCODED_METRICNAME_PATTERN = Pattern.compile("([^\\[]+)\\[(.*)\\]");
  private static final char TAG_VALUE_SEPARATOR = ':';

  private final Map<String, String> tags;
  private final String metricName;
  private String toString;

  private TagEncodedMetricName(String metricName, Map<String, String> tags) {
    if (metricName == null) {
      throw new IllegalArgumentException("metricName cannot be null");
    }
    this.metricName = metricName;
    if (tags == null) {
      this.tags = Collections.emptyMap();
    } else {
      this.tags = Collections.unmodifiableMap(tags);
    }

  }

  public static TagEncodedMetricName decode(String encodedTagName) {
    String metricName;
    Map<String, String> tags = null;

    Matcher matcher = TAG_ENCODED_METRICNAME_PATTERN.matcher(encodedTagName);
    if (matcher.find() && matcher.groupCount() == 2) {
      metricName = matcher.group(1);
      tags = parseTags(matcher.group(2));
    } else {
      metricName = encodedTagName;
    }

    checkEmpty(metricName, "metricName");
    return new TagEncodedMetricName(metricName, tags);
  }

  private static Map<String, String> parseTags(String tv) {
    char[] tagValues = tv.toCharArray();
    Map<String, String> tags = new TreeMap<>();
    StringBuilder tagValueBuffer = new StringBuilder();

    int last = 0;
    parsing:
    while (last < tagValues.length) {
      int cur = last;

      //Consume leading whitespace in front of tag
      while (tagValues[cur] == ' ' || tagValues[cur] == '\t') {
        cur++;
        if (cur >= tagValues.length) {
          break parsing;
        }
      }

      //Extract Tag
      while (tagValues[cur] != TAG_VALUE_SEPARATOR) {
        cur++;
        if (cur >= tagValues.length) {
          throw new IllegalArgumentException("Could not parse tags {" + tv + "}. Last:" + last + ". Index: " + cur);
        }
      }
      String k = new String(tagValues, last, cur - last).trim();
      checkEmpty(k, "tag");

      //Consume the TAG_VALUE_SEPARATOR
      if (tagValues[cur] == TAG_VALUE_SEPARATOR) {
        cur++;
      } else {
        throw new IllegalArgumentException("Could not parse tags {" + tv + "}. Last:" + last + ". Index: " + cur);
      }

      //Consume leading whitespace in front of value
      while (cur < tagValues.length && (tagValues[cur] == ' ' || tagValues[cur] == '\t')) {
        cur++;
      }
      if (cur >= tagValues.length) {
        throw new IllegalArgumentException("Could not parse tags {" + tv + "}. Last:" + last + ". Index: " + cur);
      }

      last = cur;
      String v;
      if (tagValues[cur] == '"') {
        cur++;
        boolean foundEndQuotedString = false;
        while (cur < tagValues.length) {
          char c = tagValues[cur];
          cur++;
          if (c == '"') {
            if (cur < tagValues.length && tagValues[cur] == '"') {
              //consume escaped quote
              cur++;
            } else {
              foundEndQuotedString = true;
              break;
            }
          }
          tagValueBuffer.append(c);
        }
        if (!foundEndQuotedString) {
          throw new IllegalArgumentException("Could not parse tags {" + tv + "}. Last:" + last + ". Index: " + cur);
        }
        v = tagValueBuffer.toString();
        tagValueBuffer.setLength(0);

        //Consume trailing whitespace
        while (cur < tagValues.length && (tagValues[cur] == ' ' || tagValues[cur] == '\t')) {
          cur++;
        }
      } else {
        while (tagValues[cur] != ',') {
          cur++;
          if (cur >= tagValues.length) {
            break;
          }
        }
        v = new String(tagValues, last, cur - last).trim();
      }
      checkEmpty(v, "tag value");
      tags.put(k, v);

      if (cur < tagValues.length) {
        if (tagValues[cur] == ',') {
          cur++;
        } else {
          throw new IllegalArgumentException("Could not parse tags {" + tv + "}. Last:" + last + ". Index: " + cur);
        }
      }
      last = cur;
    }
    return tags;
  }

  private static void checkEmpty(String s, String field) {
    if (s == null || "".equals(s.trim())) {
      throw new IllegalArgumentException(field + " must be defined");
    }
  }

  public String getMetricName() {
    return metricName;
  }

  public Map<String, String> getTags() {
    return tags;
  }

  public TagEncodedMetricName submetric(String suffix) {
    StringBuilder sb = new StringBuilder();
    append(sb, metricName);
    append(sb, suffix);
    return new TagEncodedMetricName(sb.toString(), tags);
  }

  private static void append(StringBuilder builder, String part) {
    if (part != null && !part.isEmpty()) {
      if (builder.length() > 0) {
        builder.append('.');
      }
      builder.append(part);
    }
  }

  public TagEncodedMetricName withTags(String... additionalTags) {
    Map<String, String> t = tags;
    if (additionalTags != null && additionalTags.length > 0) {
      if (additionalTags.length % 2 != 0) {
        throw new IllegalArgumentException("Additional Tags has to even in count");
      }
      t = new TreeMap<>(t);
      for (int i = 0; i < additionalTags.length; i += 2) {
        t.put(additionalTags[i], additionalTags[i + 1]);
      }
    }
    return new TagEncodedMetricName(metricName, t);
  }


  public TagEncodedMetricName withTags(Map<String, String> additionalTags) {
    Map<String, String> t = tags;
    if (additionalTags != null && additionalTags.size() > 0) {
      t = new TreeMap<>(t);
      t.putAll(additionalTags);
    }
    return new TagEncodedMetricName(metricName, t);
  }

  public String toString() {
    if (tags.isEmpty()) {
      return this.metricName;
    } else {
      if (toString != null) {
        return toString;
      }
      toString = createStringRep();
      return toString;
    }
  }

  @Override
  public int compareTo(TagEncodedMetricName tagEncodedMetricName) {
    return toString().compareTo(tagEncodedMetricName.toString());
  }

  private String createStringRep() {
    StringBuilder sb = new StringBuilder(this.metricName);
    //sb.append("\n{\n");
    sb.append("[");
    String prefix = "";

    for (Entry<String, String> tagValuePair : tags.entrySet()) {
      sb.append(prefix);
      sb.append(tagValuePair.getKey());
      sb.append(TAG_VALUE_SEPARATOR);
      sb.append(escapeTagValue(tagValuePair.getValue()));
      //prefix = ",\n";
      prefix = ",";
    }
    //sb.append("}\n");
    sb.append("]");
    return sb.toString();
  }

  private String escapeTagValue(String unescapedTagValue) {
    char[] chars = unescapedTagValue.toCharArray();
    boolean needsEscaping = false;
    for (char c : chars) {
      switch (c) {
        case '"':
        case ' ':
        case '\t':
        case ',':
        case ':':
          needsEscaping = true;
          break;
        default:
      }
    }
    if (!needsEscaping) {
      return unescapedTagValue;
    }
    return "\"" + unescapedTagValue.replace("\"", "\"\"") + "\"";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    TagEncodedMetricName that = (TagEncodedMetricName) o;

    return metricName.equals(that.metricName) && tags.equals(that.tags);
  }

  @Override
  public int hashCode() {
    int result = metricName.hashCode();
    result = 31 * result + tags.hashCode();
    return result;
  }
}

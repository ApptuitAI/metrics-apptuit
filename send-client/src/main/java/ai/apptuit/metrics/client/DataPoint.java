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

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author Rajiv Shivane
 */
public class DataPoint {

  private final String metric;
  private final long timestamp;
  private final Number value;
  private final Map<String, String> tags;

  public DataPoint(String metricName, long epoch, Number value, Map<String, String> tags) {
    if (metricName == null) {
      throw new IllegalArgumentException("metricName cannot be null");
    }
    this.metric = metricName;
    this.timestamp = epoch;
    if (value == null) {
      throw new IllegalArgumentException("Value cannot be null");
    }
    this.value = value;
    if (tags == null) {
      throw new IllegalArgumentException("Tags cannot be null");
    }
    this.tags = tags;
  }


  public String getMetric() {
    return metric;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public Number getValue() {
    return value;
  }

  public Map<String, String> getTags() {
    return tags;
  }

  @Override
  public String toString() {
    StringWriter out = new StringWriter();
    toTextPlain(new PrintWriter(out), null, Sanitizer.NO_OP_SANITIZER);
    return out.toString();
  }

  public void toJson(PrintStream ps, Map<String, String> globalTags, Sanitizer sanitizer) {
    ps.append("{");
    {
      ps.append("\n\"metric\":\"").append(sanitizer.sanitizer(getMetric())).append("\",")
              .append("\n\"timestamp\":").append(Long.toString(getTimestamp())).append(",")
              .append("\n\"value\":").append(String.valueOf(getValue()));
      ps.append(",\n\"tags\": {");

      Map<String, String> tagsToMarshall = new LinkedHashMap<>(getTags());
      if (globalTags != null) {
        tagsToMarshall.putAll(globalTags);
      }
      Iterator<Entry<String, String>> iterator = tagsToMarshall.entrySet().iterator();
      while (iterator.hasNext()) {
        Entry<String, String> tag = iterator.next();
        ps.append("\n\"").append(sanitizer.sanitizer(tag.getKey())).append("\":\"")
                .append(tag.getValue().replace("\"", "\\\"")).append("\"");
        if (iterator.hasNext()) {
          ps.append(",");
        }
      }
      ps.append("}\n");
    }
    ps.append("}");
  }

  public void toTextLine(OutputStream out, Map<String, String> globalTags, Sanitizer sanitizer) {
    try {
      PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, "UTF-8"));
      toTextPlain(writer, globalTags, sanitizer);
      writer.append('\n');
      writer.flush();
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  private void toTextPlain(PrintWriter ps, Map<String, String> globalTags, Sanitizer sanitizer) {
    ps.append(sanitizer.sanitizer(getMetric())).append(" ")
            .append(Long.toString(getTimestamp())).append(" ")
            .append(String.valueOf(getValue()));

    Map<String, String> tagsToMarshall = getTags();
    if (globalTags != null) {
      LinkedHashMap<String, String> t = new LinkedHashMap<>(tagsToMarshall);
      t.putAll(globalTags);
      tagsToMarshall = t;
    }
    tagsToMarshall.forEach((key, val) -> ps.append(" ")
            .append(sanitizer.sanitizer(key)).append("=").append(val));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    DataPoint dataPoint = (DataPoint) o;

    return timestamp == dataPoint.timestamp
            && metric.equals(dataPoint.metric)
            && value.equals(dataPoint.value)
            && tags.equals(dataPoint.tags);
  }

  @Override
  public int hashCode() {
    int result = metric.hashCode();
    result = 31 * result + (int) (timestamp ^ (timestamp >>> 32));
    result = 31 * result + value.hashCode();
    result = 31 * result + tags.hashCode();
    return result;
  }

}

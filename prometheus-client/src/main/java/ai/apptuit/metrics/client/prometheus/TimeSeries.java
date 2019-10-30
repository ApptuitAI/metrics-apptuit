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

package ai.apptuit.metrics.client.prometheus;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.annotations.JsonAdapter;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author Rajiv Shivane
 */
public class TimeSeries {

  @SuppressFBWarnings(value = "UWF_UNWRITTEN_FIELD", justification = "Initialized by Gson")
  private Map<String, String> metric;

  @SuppressFBWarnings(value = "UWF_UNWRITTEN_FIELD", justification = "Initialized by Gson")
  private List<Tuple> values;

  public Map<String, String> getLabels() {
    return metric == null ? Collections.emptyMap() : Collections.unmodifiableMap(metric);
  }

  public List<Tuple> getValues() {
    return values == null ? Collections.emptyList() : Collections.unmodifiableList(values);
  }

  @JsonAdapter(Tuple.TupleDeserializer.class)
  public static class Tuple {
    private long timestamp;
    private String value;

    private Tuple(long ts, String val) {
      this.timestamp = ts * 1000;
      this.value = val;
    }

    public long getTimestamp() {
      return timestamp;
    }

    public String getValue() {
      return value;
    }

    public int getValueAsInteger() {
      return Integer.parseInt(value);
    }

    public long getValueAsLong() {
      return Long.parseLong(value);
    }


    public float getValueAsFloat() {
      return Float.parseFloat(value);
    }


    public double getValueAsDouble() {
      return Double.parseDouble(value);
    }

    public static class TupleDeserializer implements JsonDeserializer<Tuple>, JsonSerializer<Tuple> {
      @Override
      public Tuple deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonArray tupleValues = json.getAsJsonArray();
        return new Tuple(tupleValues.get(0).getAsLong(), tupleValues.get(1).getAsString());
      }

      @Override
      public JsonElement serialize(Tuple tuple, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonArray retVal = new JsonArray();
        retVal.add(tuple.getTimestamp() / 1000);
        retVal.add(tuple.getValue());
        return retVal;
      }
    }
  }
}

package ai.apptuit.metrics.client.prometheus;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class TimeSeries {
  private Map<String, String> metric;
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
      return Integer.valueOf(value);
    }

    public long getValueAsLong() {
      return Long.valueOf(value);
    }


    public float getValueAsFloat() {
      return Float.valueOf(value);
    }


    public double getValueAsDouble() {
      return Double.valueOf(value);
    }

    public static class TupleDeserializer implements JsonDeserializer<Tuple>, JsonSerializer<Tuple>  {
      @Override
      public Tuple deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonArray tupleValues = json.getAsJsonArray();
        return new Tuple(tupleValues.get(0).getAsLong(), tupleValues.get(1).getAsString());
      }

      @Override
      public JsonElement serialize(Tuple tuple, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonArray retVal = new JsonArray();
        retVal.add(tuple.getTimestamp()/1000);
        retVal.add(tuple.getValue());
        return retVal;
      }
    }
  }
}

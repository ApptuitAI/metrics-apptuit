package ai.apptuit.metrics.client.prometheus;

import java.util.Collections;
import java.util.List;

public class MatrixResult extends QueryResult {
  private List<TimeSeries> series;

  MatrixResult(List<TimeSeries> series) {
    super(TYPE.matrix);
    this.series = series;
  }

  public List<TimeSeries> getSeries() {
    return series == null ? Collections.emptyList() : Collections.unmodifiableList(series);
  }
}

package ai.apptuit.metrics.client.prometheus;

public abstract class QueryResult {
  public enum TYPE {matrix, vector, scalar, string}

  private TYPE type;

  protected QueryResult(TYPE type) {
    this.type = type;
  }

  public TYPE getType() {
    return type;
  }
}

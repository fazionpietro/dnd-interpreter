package it.univr.dndlang;

public class ReturnException extends RuntimeException {
  private final Object value;
  private final String type;

  public ReturnException(Object value, String type) {
    super(null, null, false, false);
    this.value = value;
    this.type = type;
  }

  public Object getValue() {
    return value;
  }

  public String getType() {
    return type;
  }
}

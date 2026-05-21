package it.univr.dndlang;

public class VariableSymbol extends Symbol {
  private Object value;

  public VariableSymbol(String type, Object value) {
    super(type);
    this.value = value;
  }

  public Object getValue() {
    return value;
  }

  public void setValue(Object value) {
    this.value = value;
  }
}

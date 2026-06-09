package it.univr.dndlang;

/** Simbolo di variabile: contiene tipo e valore corrente. */
public class VariableSymbol extends Symbol {
  private Object value;

  /** Crea un simbolo di variabile con tipo e valore iniziale. */
  public VariableSymbol(String type, Object value) {
    super(type);
    this.value = value;
  }

  /** Restituisce il valore corrente della variabile. */
  public Object getValue() {
    return value;
  }

  /** Aggiorna il valore della variabile. */
  public void setValue(Object value) {
    this.value = value;
  }
}

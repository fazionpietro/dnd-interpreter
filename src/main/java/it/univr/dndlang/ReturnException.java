package it.univr.dndlang;

/** Eccezione usata per implementare il return: trasporta valore e tipo di ritorno. */
public class ReturnException extends RuntimeException {
  private final Object value;
  private final String type;

  /** Crea l'eccezione con il valore da restituire e il tipo atteso. */
  public ReturnException(Object value, String type) {
    super(null, null, false, false);
    this.value = value;
    this.type = type;
  }

  /** Restituisce il valore di ritorno della funzione. */
  public Object getValue() {
    return value;
  }

  /** Restituisce il tipo di ritorno dichiarato. */
  public String getType() {
    return type;
  }
}

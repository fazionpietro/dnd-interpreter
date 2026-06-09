package it.univr.dndlang;

/** Classe base astratta per i simboli nella tabella dei simboli. */
public abstract class Symbol {
  private final String type;

  /** Crea un simbolo con il tipo specificato. */
  protected Symbol(String type) {
    this.type = type;
  }

  /** Restituisce il tipo del simbolo. */
  public String getType() {
    return type;
  }
}

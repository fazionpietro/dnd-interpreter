package it.univr.dndlang;

public abstract class Symbol {
  private final String type;

  protected Symbol(String type) {
    this.type = type;
  }

  public String getType() {
    return type;
  }
}

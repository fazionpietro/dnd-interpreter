package it.univr.dndlang;

/*
 * DESIGN
 * ------
 *
 * Base class for our symbol table hierarchy. Both variables and functions 
 * share the concept of having a declared type, which is centralized here.
 */

/** 
 * Abstract base class for symbols in the symbol table. 
 */
public abstract class Symbol {
  private final String type;

  /** 
   * Creates a symbol with the specified type. 
   */
  protected Symbol(String type) {
    this.type = type;
  }

  /** 
   * Retrieves the type of the symbol. 
   */
  public String getType() {
    return type;
  }
}

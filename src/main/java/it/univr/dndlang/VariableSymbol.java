package it.univr.dndlang;

/** 
 * Variable symbol: contains its type and current runtime value. 
 */
public class VariableSymbol extends Symbol {
  private Object value;

  /** 
   * Creates a variable symbol with an initial type and value. 
   */
  public VariableSymbol(String type, Object value) {
    super(type);
    this.value = value;
  }

  /** 
   * Retrieves the current value of the variable. 
   */
  public Object getValue() {
    return value;
  }

  /** 
   * Updates the value of the variable. 
   */
  public void setValue(Object value) {
    this.value = value;
  }
}

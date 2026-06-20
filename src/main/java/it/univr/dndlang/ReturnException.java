package it.univr.dndlang;

/*
 * DESIGN
 * ------
 *
 * This class implements the control flow for the 'return' statement in our 
 * language interpreter. Because we are using the Visitor pattern to evaluate 
 * the AST, deep call stacks can form during execution. 
 *
 * To immediately interrupt execution within nested statements (like loops 
 * or conditionals) and return a value back to the function caller, we 
 * throw a ReturnException. This exception is then caught by the 
 * function invocation logic, effectively simulating a stack unwinding 
 * return jump.
 */

/** 
 * Exception used to transport the return value and type back to the caller. 
 */
public class ReturnException extends RuntimeException {
  private final Object value;
  private final String type;

  /** 
   * Creates the exception with the return value and its declared type. 
   */
  public ReturnException(Object value, String type) {
    /* 
     * Disable stack trace generation to improve performance, since this 
     * exception is used purely for control flow and not for actual errors. 
     */
    super(null, null, false, false);
    this.value = value;
    this.type = type;
  }

  /** 
   * Retrieves the return value of the function. 
   */
  public Object getValue() {
    return value;
  }

  /** 
   * Retrieves the declared return type. 
   */
  public String getType() {
    return type;
  }
}

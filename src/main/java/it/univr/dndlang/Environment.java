package it.univr.dndlang;

import java.util.HashMap;
import java.util.Map;

/*
 * DESIGN
 * ------
 *
 * The Environment class implements an execution environment with nested 
 * scopes for both variables and functions. It uses a linked list of Scope 
 * objects, where each scope contains a reference to its enclosing (parent) 
 * scope. 
 *
 * This design allows for variable and function shadowing natively. When 
 * resolving a symbol, we simply traverse the scope chain from the innermost 
 * scope outwards until the symbol is found.
 */

/** 
 * Represents the execution environment, managing nested scopes 
 * for variables and functions. 
 */
public class Environment {

  /* Single scope level containing variables and functions, with a link to the parent scope. */
  private static class Scope {
    final Scope enclosing;

    final Map<String, VariableSymbol> variables = new HashMap<>();
    final Map<String, FunctionSymbol> functions = new HashMap<>();

    Scope(Scope enclosing) {
      this.enclosing = enclosing;
    }
  }

  private Scope current;

  /** 
   * Initializes the environment with an empty global scope. 
   */
  public Environment() {
    this.current = new Scope(null);
  }

  /** 
   * Enters a new code block, creating a new child scope. 
   */
  public void enterBlock() {
    this.current = new Scope(this.current);
  }

  /** 
   * Exits the current block, reverting to the parent scope. 
   */
  public void exitBlock() {
    if (this.current != null)
      this.current = this.current.enclosing;
  }

  /** 
   * Declares a new variable within the current scope. 
   */
  public void declare(String name, Object value, String type) {
    this.current.variables.put(name, new VariableSymbol(type, value));
  }

  /** 
   * Registers a function in the current scope. 
   * Throws an error if a function with the same name is already declared locally. 
   */
  public void declareFunction(
      String name, String returnType, DnDLangParser.FunctionDeclContext ctx) {
    if (this.current.functions.containsKey(name)) {
      throw new DnDLangError(
          "Error: function '" + name + "' already declared.", ctx.getStart().getLine());
    }
    this.current.functions.put(name, new FunctionSymbol(returnType, ctx));
  }

  /** 
   * Searches for a function by traversing the scope chain upwards. 
   */
  public FunctionSymbol lookupFunction(String name, int line) {
    Scope scope = current;
    /* Traverse the scope chain until the function is found */
    while (scope != null) {
      if (scope.functions.containsKey(name)) {
        return scope.functions.get(name);
      }
      scope = scope.enclosing;
    }
    throw new DnDLangError("Runtime error: undeclared function '" + name + "'", line);
  }

  /** 
   * Assigns a new value to an existing variable by traversing the scope chain. 
   */
  public void assign(String name, Object value) {
    Scope scope = this.current;
    /* Traverse the scope chain upwards to find where the variable was defined */
    while (scope != null) {
      if (scope.variables.containsKey(name)) {
        scope.variables.get(name).setValue(value);
        return;
      }
      scope = scope.enclosing;
    }
    throw new DnDLangError("Runtime error: undeclared variable '" + name + "'", -1);
  }

  /** 
   * Retrieves the value of a variable by traversing the scope chain. 
   */
  public Object lookup(String name) {
    Scope scope = current;
    /* Traverse the scope chain upwards to find where the variable was defined */
    while (scope != null) {
      if (scope.variables.containsKey(name)) {
        return scope.variables.get(name).getValue();
      }
      scope = scope.enclosing;
    }
    throw new DnDLangError("Runtime error: undeclared variable '" + name + "'", -1);
  }

  /** 
   * Retrieves the declared type of a variable. 
   */
  public String getType(String name) {
    Scope scope = current;
    /* Traverse the scope chain upwards to find where the variable was defined */
    while (scope != null) {
      if (scope.variables.containsKey(name)) {
        return scope.variables.get(name).getType();
      }
      scope = scope.enclosing;
    }
    throw new DnDLangError("Runtime error: undeclared variable '" + name + "'", -1);
  }

  /** 
   * Checks if a variable exists in any of the currently active scopes. 
   */
  public boolean contains(String name) {
    Scope scope = current;
    /* Traverse the scope chain upwards to find where the variable was defined */
    while (scope != null) {
      if (scope.variables.containsKey(name)) {
        return true;
      }
      scope = scope.enclosing;
    }
    return false;
  }

  /** 
   * Checks if a variable exists strictly in the current local scope. 
   */
  public boolean containsLocal(String name) {
    return current.variables.containsKey(name);
  }

  /** 
   * Assigns a value only if the variable has already been declared. 
   */
  public void setFallback(String name, Object value) {
    if (contains(name))
      assign(name, value);
  }
}

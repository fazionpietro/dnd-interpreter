package it.univr.dndlang;

import java.util.HashMap;
import java.util.Map;

public class Environment {

  private static class Scope {
    final Scope enclosing;

    final Map<String, VariableSymbol> variables = new HashMap<>();
    final Map<String, FunctionSymbol> functions = new HashMap<>();

    Scope(Scope enclosing) {
      this.enclosing = enclosing;
    }
  }

  private Scope current;

  public Environment() {
    this.current = new Scope(null);
  }

  public void enterBlock() {
    this.current = new Scope(this.current);
  }

  public void exitBlock() {
    if (this.current != null) this.current = this.current.enclosing;
  }

  public void declare(String name, Object value, String type) {
    this.current.variables.put(name, new VariableSymbol(type, value));
  }

  public void declareFunction(
      String name, String returnType, DnDLangParser.FunctionDeclContext ctx) {
    if (this.current.functions.containsKey(name)) {
      throw new DnDLangError(
          "Errore: funzione '" + name + "' gia' dichiarata.", ctx.getStart().getLine());
    }
    this.current.functions.put(name, new FunctionSymbol(returnType, ctx));
  }

  public FunctionSymbol lookupFunction(String name, int line) {
    Scope scope = current;
    while (scope != null) {
      if (scope.functions.containsKey(name)) {
        return scope.functions.get(name);
      }
      scope = scope.enclosing;
    }
    throw new DnDLangError("Errore runtime: funzione non dichiarata '" + name + "'", line);
  }

  public void assign(String name, Object value) {
    Scope scope = this.current;
    while (scope != null) {
      if (scope.variables.containsKey(name)) {
        scope.variables.get(name).setValue(value);
        return;
      }
      scope = scope.enclosing;
    }
    throw new DnDLangError("Errore runtime: variabile non dichiarata '" + name + "'", -1);
  }

  public Object lookup(String name) {
    Scope scope = current;
    while (scope != null) {
      if (scope.variables.containsKey(name)) {
        return scope.variables.get(name).getValue();
      }
      scope = scope.enclosing;
    }
    throw new DnDLangError("Errore runtime: variabile non dichiarata '" + name + "'", -1);
  }

  public String getType(String name) {
    Scope scope = current;
    while (scope != null) {
      if (scope.variables.containsKey(name)) {
        return scope.variables.get(name).getType();
      }
      scope = scope.enclosing;
    }
    throw new DnDLangError("Errore runtime: variabile non dichiarata '" + name + "'", -1);
  }

  public boolean contains(String name) {
    Scope scope = current;
    while (scope != null) {
      if (scope.variables.containsKey(name)) {
        return true;
      }
      scope = scope.enclosing;
    }
    return false;
  }

  public boolean containsLocal(String name) {
    return current.variables.containsKey(name);
  }

  public void setFallback(String name, Object value) {
    if (contains(name)) assign(name, value);
  }
}

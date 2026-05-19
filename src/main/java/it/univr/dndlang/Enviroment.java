package it.univr.dndlang;

import java.util.HashMap;
import java.util.Map;

public class Enviroment {

  private static class VarData {
    Object value;
    String type;

    VarData(Object value, String type) {
      this.value = value;
      this.type = type;
    }
  }

  private static class Scope {
    final Scope enclosing;
    final Map<String, VarData> variables = new HashMap<>();

    Scope(Scope enclosing) {
      this.enclosing = enclosing;
    }
  }

  private Scope current;

  public Enviroment() {
    this.current = new Scope(null);
  }

  public void enterBlock() {
    this.current = new Scope(this.current);
  }

  public void exitBlock() {
    if (this.current != null) this.current = this.current.enclosing;
  }

  public void declare(String name, Object value, String type) {
    this.current.variables.put(name, new VarData(value, type));
  }

  public void assign(String name, Object value) {
    Scope scope = this.current;
    while (scope != null) {
      if (scope.variables.containsKey(name)) {
        scope.variables.get(name).value = value;
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
        return scope.variables.get(name).value;
      }
      scope = scope.enclosing;
    }
    throw new DnDLangError("Errore runtime: variabile non dichiarata '" + name + "'", -1);
  }

  public String getType(String name) {
    Scope scope = current;
    while (scope != null) {
      if (scope.variables.containsKey(name)) {
        return scope.variables.get(name).type;
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

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("Environment State:\n");

    Scope scope = current;
    int depth = 0;

    // Risaliamo la catena degli scope dal più locale al globale
    while (scope != null) {
      sb.append("  Scope Level ").append(depth).append(": ");

      if (scope.variables.isEmpty()) {
        sb.append("{ vuoto }\n");
      } else {
        sb.append("{ ");
        for (Map.Entry<String, VarData> entry : scope.variables.entrySet()) {
          sb.append(entry.getKey())
              .append("[")
              .append(entry.getValue().type)
              .append("]: ")
              .append(entry.getValue().value)
              .append(", ");
        }
        // Rimuove l'ultima virgola e spazio per pulizia visiva
        sb.setLength(sb.length() - 2);
        sb.append(" }\n");
      }

      scope = scope.enclosing; // Passa al padre
      depth++;
    }
    return sb.toString();
  }
}

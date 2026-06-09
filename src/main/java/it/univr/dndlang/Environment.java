package it.univr.dndlang;

import java.util.HashMap;
import java.util.Map;

/** Ambiente di esecuzione con scope annidati per variabili e funzioni. */
public class Environment {

  // Singolo livello di scope con riferimento allo scope padre
  private static class Scope {
    final Scope enclosing;

    final Map<String, VariableSymbol> variables = new HashMap<>();
    final Map<String, FunctionSymbol> functions = new HashMap<>();

    Scope(Scope enclosing) {
      this.enclosing = enclosing;
    }
  }

  private Scope current;

  /** Inizializza l'ambiente con uno scope globale vuoto. */
  public Environment() {
    this.current = new Scope(null);
  }

  /** Entra in un nuovo blocco, creando uno scope figlio. */
  public void enterBlock() {
    this.current = new Scope(this.current);
  }

  /** Esce dal blocco corrente, tornando allo scope padre. */
  public void exitBlock() {
    if (this.current != null) this.current = this.current.enclosing;
  }

  /** Dichiara una nuova variabile nello scope corrente. */
  public void declare(String name, Object value, String type) {
    this.current.variables.put(name, new VariableSymbol(type, value));
  }

  /** Registra una funzione nello scope corrente, errore se già dichiarata. */
  public void declareFunction(
      String name, String returnType, DnDLangParser.FunctionDeclContext ctx) {
    if (this.current.functions.containsKey(name)) {
      throw new DnDLangError(
          "Errore: funzione '" + name + "' gia' dichiarata.", ctx.getStart().getLine());
    }
    this.current.functions.put(name, new FunctionSymbol(returnType, ctx));
  }

  /** Cerca una funzione risalendo la catena degli scope. */
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

  /** Assegna un nuovo valore a una variabile esistente, risalendo gli scope. */
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

  /** Restituisce il valore di una variabile, risalendo gli scope. */
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

  /** Restituisce il tipo dichiarato di una variabile. */
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

  /** Verifica se una variabile esiste in uno qualsiasi degli scope. */
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

  /** Verifica se una variabile esiste solo nello scope corrente. */
  public boolean containsLocal(String name) {
    return current.variables.containsKey(name);
  }

  /** Assegna un valore solo se la variabile esiste già. */
  public void setFallback(String name, Object value) {
    if (contains(name)) assign(name, value);
  }
}

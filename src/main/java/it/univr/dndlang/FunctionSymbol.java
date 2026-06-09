package it.univr.dndlang;

/** Simbolo di funzione: contiene tipo di ritorno e nodo AST della dichiarazione. */
public class FunctionSymbol extends Symbol {
  private final DnDLangParser.FunctionDeclContext declarationNode;

  /** Crea un simbolo di funzione con tipo di ritorno e nodo della dichiarazione. */
  public FunctionSymbol(String returnType, DnDLangParser.FunctionDeclContext declarationNode) {
    super(returnType);
    this.declarationNode = declarationNode;
  }

  /** Restituisce il nodo AST della dichiarazione della funzione. */
  public DnDLangParser.FunctionDeclContext getDeclarationNode() {
    return declarationNode;
  }
}

package it.univr.dndlang;

public class FunctionSymbol extends Symbol {
  private final DnDLangParser.FunctionDeclContext declarationNode;

  public FunctionSymbol(String returnType, DnDLangParser.FunctionDeclContext declarationNode) {
    super(returnType);
    this.declarationNode = declarationNode;
  }

  public DnDLangParser.FunctionDeclContext getDeclarationNode() {
    return declarationNode;
  }
}

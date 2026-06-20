package it.univr.dndlang;

/** 
 * Function symbol: contains its return type and the AST node of its declaration. 
 */
public class FunctionSymbol extends Symbol {
  private final DnDLangParser.FunctionDeclContext declarationNode;

  /** 
   * Creates a function symbol with a return type and its declaration node. 
   */
  public FunctionSymbol(String returnType, DnDLangParser.FunctionDeclContext declarationNode) {
    super(returnType);
    this.declarationNode = declarationNode;
  }

  /** 
   * Retrieves the AST node of the function declaration. 
   */
  public DnDLangParser.FunctionDeclContext getDeclarationNode() {
    return declarationNode;
  }
}

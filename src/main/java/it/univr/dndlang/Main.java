package it.univr.dndlang;

import java.io.IOException;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;

/*
 * DESIGN
 * ------
 *
 * This file contains the entry point for the DnDLang interpreter. 
 * The design follows a standard compiler front-end pipeline:
 * 1. Read the source code file.
 * 2. Lexical analysis (Lexer) to produce tokens.
 * 3. Syntax analysis (Parser) to produce an Abstract Syntax Tree (AST).
 * 4. Semantic execution (Interpreter) using the Visitor pattern over the AST.
 */

/** 
 * Initializes the interpretation pipeline, executing the lexer and parser, 
 * and subsequently launching the AST visitor. 
 */
public class Main {

  /** 
   * Reads the specified source file, parses it into an AST, and executes it.
   */
  public static void main(String[] args) {
    if (args.length == 0) {
      System.err.println("Error: No input file specified.");
      System.err.println(
          "Correct usage: mvn exec:java -Dexec.mainClass=\"it.univr.dndlang.Main\""
              + " -Dexec.args=\"programs/filename.dnd\"");
      System.exit(1);
    }

    String filePath = args[0];

    try {
      /* Pipeline: source -> lexer -> tokens -> parser -> AST */
      CharStream input = CharStreams.fromFileName(filePath);
      DnDLangLexer lexer = new DnDLangLexer(input);
      CommonTokenStream tokens = new CommonTokenStream(lexer);

      DnDLangParser parser = new DnDLangParser(tokens);

      ParseTree tree = parser.program();

      /* 
       * We intercept syntax errors before visiting the AST. 
       * This is needed because evaluating an incomplete or malformed AST 
       * could lead to obscure runtime crashes instead of clear syntax errors. 
       */
      if (parser.getNumberOfSyntaxErrors() > 0) {
        System.err.println("\n[Parser]: Execution aborted due to syntax errors.");
        System.exit(1);
      }

      /* Visit the AST using the interpreter visitor */
      DnDInterpreter interpreter = new DnDInterpreter();
      interpreter.visit(tree);

    } catch (IOException e) {
      System.err.println(
          "Fatal error: Unable to read the source file '"
              + filePath
              + "'. Verify the path.");
      System.exit(1);
    } catch (DnDLangError e) {
      System.err.println(
          "Unhandled runtime error at line " + e.getLine() + ": " + e.getMessage());
      System.exit(1);
    } catch (Exception e) {
      /* 
       * We catch generic exceptions here as a last resort to prevent 
       * raw stack traces from reaching the user in case of bugs in the 
       * simulator itself. 
       */
      System.err.println("Unexpected critical simulator error: " + e.getMessage());
      System.exit(1);
    }
  }
}

package it.univr.dndlang;

import java.io.IOException;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;

/** Entry point: legge un file .dnd, esegue lexer/parser e avvia l'interprete. */
public class Main {

  /** Legge il file sorgente passato come argomento, lo analizza e lo interpreta. */
  public static void main(String[] args) {
    if (args.length == 0) {
      System.err.println("Errore: Nessun file di input specificato.");
      System.err.println(
          "Uso corretto: mvn exec:java -Dexec.mainClass=\"it.univr.dndlang.Main\""
              + " -Dexec.args=\"programs/nome_file.dnd\"");
      System.exit(1);
    }

    String filePath = args[0];

    try {
      // Pipeline: sorgente -> lexer -> token -> parser -> AST
      CharStream input = CharStreams.fromFileName(filePath);
      DnDLangLexer lexer = new DnDLangLexer(input);
      CommonTokenStream tokens = new CommonTokenStream(lexer);

      DnDLangParser parser = new DnDLangParser(tokens);

      ParseTree tree = parser.program();

      if (parser.getNumberOfSyntaxErrors() > 0) {
        System.err.println("\n[Parser]: Esecuzione interrotta a causa di errori sintattici.");
        System.exit(1);
      }

      // Visita l'AST tramite l'interprete
      DnDInterpreter interpreter = new DnDInterpreter();
      interpreter.visit(tree);

    } catch (IOException e) {
      System.err.println(
          "Errore fatale: Impossibile leggere il file sorgente '"
              + filePath
              + "'. Verificare il percorso.");
      System.exit(1);
    } catch (DnDLangError e) {
      System.err.println(
          "Errore runtime non gestito alla riga " + e.getLine() + ": " + e.getMessage());
      System.exit(1);
    } catch (Exception e) {
      // Fallback per qualsiasi altra eccezione imprevista di Java
      System.err.println("Errore critico imprevisto del simulatore: " + e.getMessage());
      System.exit(1);
    }
  }
}


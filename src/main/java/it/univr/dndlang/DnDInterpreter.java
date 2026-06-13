package it.univr.dndlang;

import it.univr.dndlang.DnDLangParser.AddSubExprContext;
import it.univr.dndlang.DnDLangParser.AndExprContext;
import it.univr.dndlang.DnDLangParser.BoolExprContext;
import it.univr.dndlang.DnDLangParser.BreakStmtContext;
import it.univr.dndlang.DnDLangParser.DiceAdvDisExprContext;
import it.univr.dndlang.DnDLangParser.DiceOnlyContext;
import it.univr.dndlang.DnDLangParser.EqualityExprContext;
import it.univr.dndlang.DnDLangParser.ExprStmtContext;
import it.univr.dndlang.DnDLangParser.FloatExprContext;
import it.univr.dndlang.DnDLangParser.IdExprContext;
import it.univr.dndlang.DnDLangParser.IfStmtContext;
import it.univr.dndlang.DnDLangParser.IntExprContext;
import it.univr.dndlang.DnDLangParser.MulDivExprContext;
import it.univr.dndlang.DnDLangParser.OrExprContext;
import it.univr.dndlang.DnDLangParser.ParenExprContext;
import it.univr.dndlang.DnDLangParser.PreIncExprContext;
import it.univr.dndlang.DnDLangParser.RelationalExprContext;
import it.univr.dndlang.DnDLangParser.SaveVsExprContext;
import it.univr.dndlang.DnDLangParser.StringExprContext;
import it.univr.dndlang.DnDLangParser.SwitchStmtContext;
import it.univr.dndlang.DnDLangParser.TernaryExprContext;
import it.univr.dndlang.DnDLangParser.UnaryExprContext;
import it.univr.dndlang.DnDLangParser.WhileStmtContext;
import org.antlr.v4.runtime.ParserRuleContext;

/** Visitor che interpreta l'AST generato dal parser DnDLang. */
public class DnDInterpreter extends DnDLangBaseVisitor<Object> {
  // tabella dei simboli
  private final Environment env = new Environment();
  // prefisso hero./foe. attivo
  private String currentStructPrefix = "";
  // flag break attivo
  private boolean isBreaking = false;
  // tipo di ritorno della funzione corrente
  private String currentReturnType = "Void";

  // --- Struttura del programma (program, hero, foe, quest) ---

  /** Visita il programma */
  @Override
  public Object visitProgram(DnDLangParser.ProgramContext ctx) {
    // Early return di sicurezza
    if (ctx.children == null) return null;

    int heroCount = 0;
    int foeCount = 0;

    for (var child : ctx.children) {

      if (child instanceof DnDLangParser.HeroSectionContext heroCtx) {
        if (++heroCount > 1) {
          throw new DnDLangError("Sezione 'hero' duplicata.", heroCtx.getStart().getLine());
        }
        visit(heroCtx);
      } else if (child instanceof DnDLangParser.FoeSectionContext foeCtx) {
        if (++foeCount > 1) {
          throw new DnDLangError("Sezione 'foe' duplicata.", foeCtx.getStart().getLine());
        }
        visit(foeCtx);
      } else if (child instanceof DnDLangParser.FunctionDeclContext funcCtx) {
        String funcName = funcCtx.ID().getText();
        String returnType = funcCtx.getChild(1).getText();
        env.declareFunction(funcName, returnType, funcCtx);

      } else if (child instanceof DnDLangParser.QuestSectionContext questCtx) {
        visit(questCtx);
      }
    }

    return null;
  }

  /** Imposta il prefisso "hero." e dichiara le variabili della sezione hero. */
  @Override
  public Object visitHeroSection(DnDLangParser.HeroSectionContext ctx) {
    currentStructPrefix = "hero.";
    visitDeclarativeBlock(ctx.block());
    currentStructPrefix = "";
    return null;
  }

  /** Imposta il prefisso "foe." e dichiara le variabili della sezione foe. */
  @Override
  public Object visitFoeSection(DnDLangParser.FoeSectionContext ctx) {
    currentStructPrefix = "foe.";
    visitDeclarativeBlock(ctx.block());
    currentStructPrefix = "";
    return null;
  }

  /** Esegue il blocco della sezione quest (corpo principale del programma). */
  @Override
  public Object visitQuestSection(DnDLangParser.QuestSectionContext ctx) {
    return visit(ctx.block());
  }

  // --- Blocchi e istruzioni ---

  /** Apre un nuovo scope, esegue tutte le istruzioni e chiude lo scope. */
  @Override
  public Object visitBlock(DnDLangParser.BlockContext ctx) {
    env.enterBlock();
    if (ctx.statement() != null) {
      for (var stmt : ctx.statement()) {
        visit(stmt);
        // se un break è stato attivato, interrompe l'esecuzione del blocco
        if (isBreaking) {
          break;
        }
      }
    }
    env.exitBlock();
    return null;
  }

  /**
   * Dichiara una variabile: determina il tipo, valuta l'espressione e la registra nell'ambiente.
   */
  @Override
  public Object visitDecl(DnDLangParser.DeclContext ctx) {
    String declaredType;
    if (ctx.TYPE_HP() != null) declaredType = "HP";
    else if (ctx.TYPE_AC() != null) declaredType = "AC";
    else if (ctx.TYPE_GOLD() != null) declaredType = "Gold";
    else if (ctx.TYPE_INT() != null) declaredType = "Int";
    else if (ctx.TYPE_FLOAT() != null) declaredType = "Float";
    else if (ctx.TYPE_BOOL() != null) declaredType = "Bool";
    else if (ctx.TYPE_STRING() != null) declaredType = "String";
    else throw new DnDLangError("Tipo sconosciuto", ctx.getStart().getLine());

    String rawId = ctx.ID().getText();
    // antepone "hero." o "foe." se siamo dentro una sezione struct
    String id = currentStructPrefix + rawId;

    Object value;

    if (ctx.expr() != null) {

      value = visit(ctx.expr());

    } else {
      value = getDefaultValueForType(declaredType, ctx);
    }

    value = coerceToDeclaredType(value, declaredType);
    env.declare(id, value, declaredType);
    return null;
  }

  // --- Assegnamento e incremento/decremento ---

  /** Gestisce l'assegnamento semplice e composto (=, +=, -=, *=, /=). */
  @Override
  public Object visitAssign(DnDLangParser.AssignContext ctx) {
    String rawId = ctx.ID().getText();
    String id = resolveId(rawId, ctx.getStart().getLine());

    if (!env.contains(id)) {
      throw new DnDLangError(
          "impossibile assegnare, variabile non dichiarata '" + id + "'", ctx.getStart().getLine());
    }
    String declaredType = env.getType(id);
    Object rightValue;

    rightValue = visit(ctx.expr());

    Object leftValue = null;
    Object finalValue = null;

    // il secondo figlio del nodo AST è l'operatore di assegnamento
    switch (ctx.getChild(1).getText()) {
      case "=":
        finalValue = rightValue;
        break;

      case "+=":
        leftValue = env.lookup(id);
        double sum = asDouble(leftValue, ctx) + asDouble(rightValue, ctx);
        // preserva il tipo intero solo se entrambi gli operandi sono interi
        finalValue = areBothIntegers(leftValue, rightValue) ? (int) sum : sum;
        break;

      case "-=":
        leftValue = env.lookup(id);
        double diff = asDouble(leftValue, ctx) - asDouble(rightValue, ctx);
        finalValue = areBothIntegers(leftValue, rightValue) ? (int) diff : diff;
        break;

      case "*=":
        leftValue = env.lookup(id);
        double prod = asDouble(leftValue, ctx) * asDouble(rightValue, ctx);
        finalValue = areBothIntegers(leftValue, rightValue) ? (int) prod : prod;
        break;

      case "/=":
        leftValue = env.lookup(id);
        double divRight = asDouble(rightValue, ctx);
        if (divRight == 0) {
          throw new DnDLangError("divisione per zero", ctx.getStart().getLine());
        }
        double quot = asDouble(leftValue, ctx) / divRight;
        finalValue = areBothIntegers(leftValue, rightValue) ? (int) quot : quot;
        break;

      default:
        throw new DnDLangError(
            "Errore runtime: assgnamento non riconosciuto", ctx.getStart().getLine());
    }
    // adatta il risultato al tipo dichiarato prima di salvarlo
    finalValue = coerceToDeclaredType(finalValue, declaredType);
    env.assign(id, finalValue);

    return null;
  }

  /** Valuta il post-incremento/decremento (x++, x--) e restituisce il valore precedente. */
  @Override
  public Object visitPostIncExpr(DnDLangParser.PostIncExprContext ctx) {
    String rawId = ctx.ID().getText();
    String id = resolveId(rawId, ctx.getStart().getLine());
    String declaredType = env.getType(id);

    Object oldValue = env.lookup(id);

    double calcValue = asDouble(oldValue, ctx);
    if (ctx.PLUS_PLUS() != null) {
      calcValue += 1.0;
    } else if (ctx.MINUS_MINUS() != null) {
      calcValue -= 1.0;
    } else {
      throw new DnDLangError(
          "Runtime error: operatore di incremento non riconosciuto", ctx.getStart().getLine());
    }

    // mantiene il tipo originale (Integer o Double)
    Object newValue = (oldValue instanceof Integer) ? (int) calcValue : calcValue;

    newValue = coerceToDeclaredType(newValue, declaredType);
    env.assign(id, newValue);

    // post-incremento: restituisce il valore prima della modifica
    return oldValue;
  }

  /** Valuta il pre-incremento/decremento (++x, --x) e restituisce il nuovo valore. */
  @Override
  public Object visitPreIncExpr(PreIncExprContext ctx) {
    String rawId = ctx.ID().getText();
    String id = resolveId(rawId, ctx.getStart().getLine());
    String declaredType = env.getType(id);

    Object oldValue = env.lookup(id);

    double calcValue = asDouble(oldValue, ctx);
    if (ctx.PLUS_PLUS() != null) {
      calcValue += 1.0;
    } else if (ctx.MINUS_MINUS() != null) {
      calcValue -= 1.0;
    } else {
      throw new DnDLangError(
          "Runtime error: operatore di incremento non riconosciuto", ctx.getStart().getLine());
    }

    Object newValue = (oldValue instanceof Integer) ? (int) calcValue : calcValue;

    newValue = coerceToDeclaredType(newValue, declaredType);
    env.assign(id, newValue);

    return newValue;
  }

  /** Valuta l'operatore ternario: condizione ? valore_vero : valore_falso. */
  @Override
  public Object visitTernaryExpr(TernaryExprContext ctx) {
    Object condition = visit(ctx.expr(0));
    if (!(condition instanceof Boolean)) {
      throw new DnDLangError(
          "La condizione dell'operatore ternario deve essere un booleano",
          ctx.getStart().getLine());
    }

    if ((Boolean) condition) {
      return visit(ctx.expr(1));

    } else {
      return visit(ctx.expr(2));
    }
  }

  // --- Print e interpolazione stringhe ---

  /** Stampa il valore di un'espressione o di una stringa interpolata. */
  @Override
  public Object visitPrintStmt(DnDLangParser.PrintStmtContext ctx) {
    Object value = visit(ctx.expr() != null ? ctx.expr() : ctx.ISTRING());
    System.out.println(value);
    return null;
  }

  // --- Letterali ---

  /** Restituisce il valore intero del letterale. */
  @Override
  public Object visitIntExpr(IntExprContext ctx) {
    return Integer.parseInt(ctx.INT().getText());
  }

  /** Restituisce il valore decimale del letterale. */
  @Override
  public Object visitFloatExpr(FloatExprContext ctx) {
    return Double.parseDouble(ctx.FLOAT().getText());
  }

  /** Restituisce il valore booleano del letterale. */
  @Override
  public Object visitBoolExpr(BoolExprContext ctx) {
    return Boolean.parseBoolean(ctx.BOOL().getText());
  }

  /** Restituisce il contenuto della stringa, gestendo le sequenze di escape. */
  @Override
  public Object visitStringExpr(StringExprContext ctx) {
    String str = ctx.STRING().getText();
    String content = str.substring(1, str.length() - 1);

    return content.replace("\\n", "\n").replace("\\t", "\t").replace("\\\"", "\"");
  }

  /** Interpola una stringa i"...", sostituendo ${expr} con il valore valutato. */
  @Override
  public Object visitIStringExpr(DnDLangParser.IStringExprContext ctx) {
    String rawString = ctx.ISTRING().getText();
    // rimuove il prefisso i" e le virgolette finali
    String content = rawString.substring(2, rawString.length() - 1);
    content = content.replace("\\n", "\n").replace("\\t", "\t").replace("\\\"", "\"");

    // cerca tutte le occorrenze di ${...} nella stringa
    java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\$\\{([^}]+)\\}");
    java.util.regex.Matcher matcher = pattern.matcher(content);
    StringBuilder result = new StringBuilder();

    while (matcher.find()) {
      String exprText = matcher.group(1).trim();
      String replacement;

      try {
        // crea un mini-parser per valutare l'espressione dentro ${...}
        org.antlr.v4.runtime.CharStream input =
            org.antlr.v4.runtime.CharStreams.fromString(exprText);
        DnDLangLexer lexer = new DnDLangLexer(input);
        org.antlr.v4.runtime.CommonTokenStream tokens =
            new org.antlr.v4.runtime.CommonTokenStream(lexer);
        DnDLangParser parser = new DnDLangParser(tokens);

        parser.removeErrorListeners();

        DnDLangParser.ExprContext exprCtx = parser.expr();

        if (parser.getNumberOfSyntaxErrors() > 0) {
          replacement = "[ERRORE SINTASSI IN: " + exprText + "]";
        } else {
          Object val = visit(exprCtx);

          // cerca la variabile per determinare se aggiungere un suffisso di tipo (HP, AC,
          // gp)
          String resolvedIdForType = null;
          if (!currentStructPrefix.isEmpty() && env.contains(currentStructPrefix + exprText)) {
            resolvedIdForType = currentStructPrefix + exprText;
          } else if (env.contains(exprText)) {
            resolvedIdForType = exprText;
          }

          if (resolvedIdForType != null) {
            String type = env.getType(resolvedIdForType);
            replacement =
                switch (type) {
                  case "HP" -> val + " HP";
                  case "AC" -> val + " AC";
                  case "Gold" -> val + " gp";
                  default -> val.toString();
                };
          } else {
            replacement = val.toString();
          }
        }
      } catch (Exception e) {
        replacement = "[ERRORE RUNTIME: " + exprText + "]";
      }

      matcher.appendReplacement(result, java.util.regex.Matcher.quoteReplacement(replacement));
    }
    matcher.appendTail(result);

    return result.toString();
  }

  // --- Dadi e meccaniche D&D (d20, adv/dis, save/vs) ---

  @Override
  public Object visitD20Expr(DnDLangParser.D20ExprContext ctx) {
    return rollDice(20);
  }

  @Override
  public Object visitD12Expr(DnDLangParser.D12ExprContext ctx) {
    return rollDice(12);
  }

  @Override
  public Object visitD10Expr(DnDLangParser.D10ExprContext ctx) {
    return rollDice(10);
  }

  @Override
  public Object visitD8Expr(DnDLangParser.D8ExprContext ctx) {
    return rollDice(8);
  }

  @Override
  public Object visitD6Expr(DnDLangParser.D6ExprContext ctx) {
    return rollDice(6);
  }

  @Override
  public Object visitD4Expr(DnDLangParser.D4ExprContext ctx) {
    return rollDice(4);
  }

  @Override
  public Object visitD3Expr(DnDLangParser.D3ExprContext ctx) {
    return rollDice(3);
  }

  /** Lancia due dadi e tiene il migliore (adv) o il peggiore (dis). */
  @Override
  public Object visitDiceAdvDisExpr(DiceAdvDisExprContext ctx) {

    int r1 = (int) visit(ctx.diceOnly());
    int r2 = (int) visit(ctx.diceOnly());

    boolean isAdv = ctx.ADV() != null;

    int res = isAdv ? Math.max(r1, r2) : Math.min(r1, r2);

    String tipo = isAdv ? "Vantaggio" : "Svantaggio";
    System.out.println(" > [" + tipo + "] Lanciati " + r1 + " e " + r2 + " -> Tengo " + res);

    return res;
  }

  /** Valuta un tiro salvezza (save) o una prova contrapposta (vs), restituisce un booleano. */
  @Override
  public Object visitSaveVsExpr(SaveVsExprContext ctx) {
    Object left = visit(ctx.expr(0));
    Object right = visit(ctx.expr(1));
    double l = asDouble(left, ctx);
    double r = asDouble(right, ctx);

    boolean success;

    if (ctx.SAVE() != null) {
      success = l >= r;
      System.out.println(
          "  > [ Tiro Salvezza ] Totale: "
              + (int) l
              + " vs CD: "
              + (int) r
              + " -> "
              + (success ? "SUCCESSO" : "FALLIMENTO"));
      return success;
    } else if (ctx.VS() != null) {
      success = l > r;
      System.out.println(
          "  > [ Prova Contrapposta ] Attivo: "
              + (int) l
              + " vs Avversario: "
              + (int) r
              + " -> "
              + (success ? "VITTORIA" : "SCONFITTA"));
      return success;
    } else {
      throw new DnDLangError("", ctx.getStart().getLine());
    }
  }

  // --- Espressioni aritmetiche, relazionali e logiche ---

  /** Restituisce il valore di una variabile, risolvendo il prefisso struct. */
  @Override
  public Object visitIdExpr(IdExprContext ctx) {
    String rawId = ctx.ID().getText();
    String id = resolveId(rawId, ctx.getStart().getLine());
    return env.lookup(id);
  }

  /** Valuta l'espressione tra parentesi. */
  @Override
  public Object visitParenExpr(ParenExprContext ctx) {
    return visit(ctx.expr());
  }

  /** Valuta addizione o sottrazione, preservando il tipo intero se possibile. */
  @Override
  public Object visitAddSubExpr(AddSubExprContext ctx) {
    Object left = visit(ctx.expr(0));
    Object right = visit(ctx.expr(1));

    double l = asDouble(left, ctx);
    double r = asDouble(right, ctx);
    double result = 0;

    if (ctx.PLUS() != null) {
      result = l + r;
    } else if (ctx.MINUS() != null) {
      result = l - r;
    }

    if (areBothIntegers(left, right)) {
      return (int) result;
    }
    return result;
  }

  /** Valuta moltiplicazione, divisione o modulo con controllo divisione per zero. */
  @Override
  public Object visitMulDivExpr(MulDivExprContext ctx) {
    Object left = visit(ctx.expr(0));
    Object right = visit(ctx.expr(1));

    double l = asDouble(left, ctx);
    double r = asDouble(right, ctx);
    double result = 0;

    if (ctx.STAR() != null) {
      result = l * r;
    } else if (ctx.SLASH() != null) {
      if (r == 0) throw new DnDLangError("divisione per zero", ctx.getStart().getLine());
      result = l / r;
    } else if (ctx.PERCENT() != null) {
      if (r == 0) throw new DnDLangError("modulo per zero", ctx.getStart().getLine());
      result = l % r;
    }

    if (areBothIntegers(left, right)) {
      return (int) result;
    }
    return result;
  }

  /** Valuta un confronto relazionale ({@literal <, <=, >, >=}). */
  @Override
  public Object visitRelationalExpr(RelationalExprContext ctx) {
    Object left = visit(ctx.expr(0));
    Object right = visit(ctx.expr(1));

    double l = asDouble(left, ctx);
    double r = asDouble(right, ctx);

    if (ctx.LT() != null) return l < r;
    else if (ctx.LE() != null) return l <= r;
    else if (ctx.GT() != null) return l > r;
    else if (ctx.GE() != null) return l >= r;

    throw new DnDLangError("Operatore relazionale non riconosciuto", ctx.getStart().getLine());
  }

  /** Valuta uguaglianza (==) o disuguaglianza (!=). */
  @Override
  public Object visitEqualityExpr(EqualityExprContext ctx) {
    Object left = visit(ctx.expr(0));
    Object right = visit(ctx.expr(1));

    boolean isEqual;

    if (left instanceof Number && right instanceof Number) {
      isEqual = asDouble(left, ctx) == asDouble(right, ctx);
    } else {
      isEqual = left.equals(right);
    }

    if (ctx.EQ() != null) return isEqual;
    else if (ctx.NEQ() != null) return !isEqual;

    throw new DnDLangError("Operatore di confronto non riconosciuto", ctx.getStart().getLine());
  }

  // --- Chiamata di funzione ---

  /** Invoca una funzione: valuta gli argomenti, apre uno scope locale e gestisce il return. */
  @Override
  public Object visitFunctionCallExpr(DnDLangParser.FunctionCallExprContext ctx) {
    String funcName = ctx.ID().getText();
    int line = ctx.getStart().getLine();

    // recupera il simbolo e il nodo AST della dichiarazione originale
    FunctionSymbol function = env.lookupFunction(funcName, line);
    DnDLangParser.FunctionDeclContext delCtx = function.getDeclarationNode();

    // valuta tutti gli argomenti del chiamante prima di entrare nello scope della
    // funzione
    java.util.ArrayList<Object> evalArguments = new java.util.ArrayList<>();
    if (ctx.expr() != null) {
      for (DnDLangParser.ExprContext argCtx : ctx.expr()) {
        evalArguments.add(visit(argCtx));
      }
    }

    // recupera i parametri formali "richiesti"
    DnDLangParser.ParamListContext paramListCtx = delCtx.paramList();
    java.util.List<DnDLangParser.ParamDeclContext> formalParameter = new java.util.ArrayList<>();
    if (paramListCtx != null) {
      formalParameter = paramListCtx.paramDecl();
    }

    // controlla l'arità
    if (evalArguments.size() != formalParameter.size()) {
      throw new DnDLangError(
          "Errore: la funzione '"
              + funcName
              + "' si aspetta "
              + formalParameter.size()
              + " parametri, ma ne sono stati passati "
              + evalArguments.size()
              + ".",
          line);
    }

    // salva e aggiorna il tipo di ritorno per supportare chiamate
    // ricorsive/annidate
    String previousReturnType = currentReturnType;
    currentReturnType = function.getType();

    env.enterBlock();

    // il return è implementato lanciando una ReturnException catturata qui sotto
    try {

      // dichiara parametri nello scope locale - se i tipi non combaciano lancia un
      // errore
      for (int i = 0; i < formalParameter.size(); i++) {
        DnDLangParser.ParamDeclContext paramCtx = formalParameter.get(i);
        String paramType = paramCtx.getChild(0).getText();
        String paramName = paramCtx.ID().getText();

        Object argValue = evalArguments.get(i);
        Object coercedValue;

        try {
          coercedValue = coerceToDeclaredType(argValue, paramType);
        } catch (Exception e) {
          throw new DnDLangError(
              "Errore di tipo per il parametro '"
                  + paramName
                  + "'. "
                  + "Atteso: "
                  + paramType
                  + ", Ricevuto: valore incompatibile.",
              line);
        }

        env.declare(paramName, coercedValue, paramType);
      }

      // esegue il corpo della funzione; un return lancerà ReturnException
      visit(delCtx.block());

      // se arriviamo qui senza ReturnException, la funzione non-Void è in errore
      if (!"Void".equals(currentReturnType)) {
        throw new DnDLangError(
            "La funzione '"
                + funcName
                + "' deve restituire un valore di tipo "
                + currentReturnType
                + " ma non ha un'istruzione return.",
            line);
      }
      return null;

    } catch (ReturnException e) {
      // il return è stato eseguito: estrae il valore dalla eccezione
      Object retVal = e.getValue();

      if ("Void".equals(currentReturnType)) {
        if (retVal != null) {
          throw new DnDLangError(
              "La funzione '" + funcName + "' è Void e non può restituire un valore.", line);
        }
        return null;
      }

      try {
        return coerceToDeclaredType(retVal, currentReturnType);
      } catch (IllegalArgumentException ex) {
        throw new DnDLangError(
            "La funzione '"
                + funcName
                + "' deve restituire "
                + currentReturnType
                + " ma ha restituito un tipo incompatibile.",
            line);
      }

    } finally {
      // ripristina lo scope e il tipo di ritorno del chiamante
      env.exitBlock();
      currentReturnType = previousReturnType;
    }
  }

  /** Valuta un'espressione usata come istruzione (scarta il risultato). */
  @Override
  public Object visitExprStmt(ExprStmtContext ctx) {
    visit(ctx.expr());
    return null;
  }

  /** Lancia il dado specificato dalla regola diceOnly. */
  @Override
  public Object visitDiceOnly(DiceOnlyContext ctx) {
    if (ctx.D20() != null) return rollDice(20);
    if (ctx.D12() != null) return rollDice(12);
    if (ctx.D10() != null) return rollDice(10);
    if (ctx.D8() != null) return rollDice(8);
    if (ctx.D6() != null) return rollDice(6);
    if (ctx.D4() != null) return rollDice(4);
    if (ctx.D3() != null) return rollDice(3);
    throw new DnDLangError("Dado non riconosciuto", ctx.getStart().getLine());
  }

  /** Valuta l'AND logico tra due espressioni booleane. */
  @Override
  public Object visitAndExpr(AndExprContext ctx) {
    Object left = visit(ctx.expr(0));
    Object right = visit(ctx.expr(1));

    if (!(left instanceof Boolean) || !(right instanceof Boolean)) {
      throw new DnDLangError(
          "l'operatore '&&' richiede due valori booleani", ctx.getStart().getLine());
    }
    return (Boolean) left && (Boolean) right;
  }

  /** Valuta l'OR logico tra due espressioni booleane. */
  @Override
  public Object visitOrExpr(OrExprContext ctx) {
    Object left = visit(ctx.expr(0));
    Object right = visit(ctx.expr(1));

    if (!(left instanceof Boolean) || !(right instanceof Boolean)) {
      throw new DnDLangError(
          "l'operatore '||' richiede due valori booleani", ctx.getStart().getLine());
    }
    return (Boolean) left || (Boolean) right;
  }

  /** Valuta un operatore unario: negazione logica (!) o aritmetica (-). */
  @Override
  public Object visitUnaryExpr(UnaryExprContext ctx) {
    Object value = visit(ctx.expr());

    if (ctx.NOT() != null) {
      if (!(value instanceof Boolean)) {
        throw new DnDLangError(
            "l'operatore '!' richiede un valore booleano", ctx.getStart().getLine());
      }
      return !(Boolean) value;
    } else if (ctx.MINUS() != null) {
      double num = asDouble(value, ctx);
      if (value instanceof Integer) {
        return (int) -num;
      }
      return -num;
    }
    throw new DnDLangError("Operatore unario non riconosciuto", ctx.getStart().getLine());
  }

  // --- Return e dichiarazione di funzione ---

  /** Lancia una ReturnException per interrompere l'esecuzione e restituire il valore. */
  @Override
  public Object visitReturnStmt(DnDLangParser.ReturnStmtContext ctx) {
    Object value = (ctx.expr() != null) ? visit(ctx.expr()) : null;
    throw new ReturnException(value, currentReturnType); // usa il tipo corrente
  }

  /** Registra una funzione nell'ambiente con nome, tipo di ritorno e nodo AST. */
  @Override
  public Object visitFunctionDecl(DnDLangParser.FunctionDeclContext ctx) {
    String name = ctx.ID().getText();

    String returnType = ctx.getChild(1).getText();

    env.declareFunction(name, returnType, ctx);
    return null;
  }

  // --- Controllo di flusso (if, while, switch, break) ---

  /** Valuta la condizione ed esegue il blocco then o il blocco else. */
  @Override
  public Object visitIfStmt(IfStmtContext ctx) {
    Object condition = visit(ctx.expr());

    if (!(condition instanceof Boolean)) {
      throw new DnDLangError(
          "la condizione del costrutto 'if' deve essere un booleano", ctx.getStart().getLine());
    }
    if ((Boolean) condition) {
      visit(ctx.block(0));
    } else if (ctx.block().size() > 1) {
      visit(ctx.block(1));
    }
    return null;
  }

  /** Esegue il corpo del ciclo finché la condizione è vera o si incontra un break. */
  @Override
  public Object visitWhileStmt(WhileStmtContext ctx) {
    while (true) {
      Object condition = visit(ctx.expr());

      if (!(condition instanceof Boolean)) {
        throw new DnDLangError(
            "la condizione del costrutto 'while' deve essere un booleano",
            ctx.getStart().getLine());
      }

      if (!(Boolean) condition) {
        break;
      }

      visit(ctx.block());

      if (isBreaking) {
        isBreaking = false;
        break;
      }
    }
    return null;
  }

  /** Confronta il valore dello switch con ogni case ed esegue il blocco corrispondente. */
  @Override
  public Object visitSwitchStmt(SwitchStmtContext ctx) {
    Object switchValue = visit(ctx.expr());

    for (DnDLangParser.CaseBlockContext caseCtx : ctx.caseBlock()) {
      Object caseValue = visit(caseCtx.expr());
      boolean isMatch = false;

      if (switchValue instanceof Number && caseValue instanceof Number) {
        isMatch = asDouble(switchValue, ctx) == asDouble(caseValue, ctx);
      } else {
        isMatch = switchValue.equals(caseValue);
      }

      if (isMatch) {
        visit(caseCtx.block());
        // resetta il flag break dopo l'esecuzione del case corrispondente
        isBreaking = false;
        return null;
      }
    }
    if (ctx.defaultBlock() != null) {
      visit(ctx.defaultBlock().block());
      isBreaking = false;
    }
    return null;
  }

  /** Attiva il flag di break per interrompere il ciclo corrente. */
  @Override
  public Object visitBreakStmt(BreakStmtContext ctx) {
    isBreaking = true;
    return null;
  }

  // --- Metodi di utilità ---

  /** Restituisce il valore di default per il tipo dato (0, false, ""). */
  private Object getDefaultValueForType(String type, ParserRuleContext ctx) {
    return switch (type) {
      case "HP", "AC", "Int" -> 0;
      case "Gold" -> 0.0;
      case "Bool" -> false;
      case "String" -> "";
      default -> throw new DnDLangError("Tipo sconosciuto: " + type, ctx.getStart().getLine());
    };
  }

  /** Converte un valore al tipo dichiarato, se compatibile (es. Int → Gold diventa Double). */
  private Object coerceToDeclaredType(Object value, String declaredType) {
    // Gold è sempre Double: converte un Integer in Double
    if ("Gold".equals(declaredType) && value instanceof Integer i) {
      return i.doubleValue();
    }
    // HP: intero, se scende sotto 0 viene bloccato a 0
    if ("HP".equals(declaredType)) {
      int intVal;
      if (value instanceof Double d) intVal = (int) d.doubleValue();
      else if (value instanceof Integer i) intVal = i;
      else throw new IllegalArgumentException("Tipo incompatibile con " + declaredType);

      return Math.max(intVal, 0);
    }
    // AC: intero, non può essere negativo
    if ("AC".equals(declaredType)) {
      int intVal;
      if (value instanceof Double d) intVal = (int) d.doubleValue();
      else if (value instanceof Integer i) intVal = i;
      else throw new IllegalArgumentException("Tipo incompatibile con " + declaredType);

      if (intVal < 0)
        throw new IllegalArgumentException(
            declaredType + " non può essere negativo (valore: " + intVal + ")");
      return intVal;
    }

    // Int è ovviamente un intero: tronca un Double
    if ("Int".equals(declaredType) && value instanceof Double d) {
      return (int) d.doubleValue();
    }

    // se non è necessaria una conversione, verifica la compatibilità del tipo
    boolean typeMatch =
        switch (declaredType) {
          case "Int", "HP", "AC" -> value instanceof Integer;
          case "Float", "Gold" -> value instanceof Double;
          case "Bool" -> value instanceof Boolean;
          case "String" -> value instanceof String;
          case "Void" -> value == null;
          default -> false;
        };
    if (!typeMatch) {

      throw new IllegalArgumentException(
          "Tipo incompatibile: atteso "
              + declaredType
              + ", ricevuto "
              + value.getClass().getSimpleName());
    }

    return value;
  }

  /** Esegue un blocco dichiarativo (hero/foe) senza creare un nuovo scope. */
  private Object visitDeclarativeBlock(DnDLangParser.BlockContext ctx) {
    if (ctx.statement() != null) {
      for (var stmt : ctx.statement()) {
        visit(stmt);
        if (isBreaking) {
          throw new DnDLangError("'break' usato fuori da un ciclo", stmt.getStart().getLine());
        }
      }
    }
    return null;
  }

  /** Converte un valore numerico in double, errore se non numerico. */
  private double asDouble(Object value, ParserRuleContext ctx) {
    if (value instanceof Number num) {
      return num.doubleValue();
    }
    throw new DnDLangError(
        "Operazione matematica su un tipo non numerico: " + value, ctx.getStart().getLine());
  }

  /** Verifica se entrambi gli operandi sono interi. */
  private boolean areBothIntegers(Object left, Object right) {
    return left instanceof Integer && right instanceof Integer;
  }

  /** Genera un numero casuale tra 1 e il numero di facce del dado. */
  private int rollDice(int sides) {
    return new java.util.Random().nextInt(sides) + 1;
  }

  /** Risolve un identificatore applicando il prefisso struct (hero./foe.) se presente. */
  private String resolveId(String rawId, int line) {
    // dentro hero/foe, prova prima con il prefisso (es. "hero.hp")
    if (!currentStructPrefix.isEmpty()) {
      String prefixedId = currentStructPrefix + rawId;
      if (env.contains(prefixedId)) {
        return prefixedId;
      }
    }

    // se non trovato con prefisso, cerca l'identificatore globale
    if (env.contains(rawId)) {
      return rawId;
    }

    throw new DnDLangError("variabile non dichiarata '" + rawId + "'", line);
  }
}

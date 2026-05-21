package it.univr.dndlang;

import it.univr.dndlang.DnDLangParser.AddSubExprContext;
import it.univr.dndlang.DnDLangParser.AndExprContext;
import it.univr.dndlang.DnDLangParser.BoolExprContext;
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

public class DnDInterpreter extends DnDLangBaseVisitor<Object> {
  private final Environment env = new Environment();
  private String currentStructPrefix = "";

  @Override
  public Object visitProgram(DnDLangParser.ProgramContext ctx) {
    if (ctx.functionSection() != null) visit(ctx.functionSection());
    if (ctx.heroSection() != null) visit(ctx.heroSection());
    if (ctx.foeSection() != null) visit(ctx.foeSection());
    if (ctx.questSection() != null) visit(ctx.questSection());
    return null;
  }

  @Override
  public Object visitHeroSection(DnDLangParser.HeroSectionContext ctx) {
    currentStructPrefix = "hero.";
    visitDeclarativeBlock(ctx.block());
    currentStructPrefix = "";
    return null;
  }

  @Override
  public Object visitFoeSection(DnDLangParser.FoeSectionContext ctx) {
    currentStructPrefix = "foe.";
    visitDeclarativeBlock(ctx.block());
    currentStructPrefix = "";
    return null;
  }

  @Override
  public Object visitQuestSection(DnDLangParser.QuestSectionContext ctx) {
    return visit(ctx.block());
  }

  @Override
  public Object visitBlock(DnDLangParser.BlockContext ctx) {
    env.enterBlock();
    if (ctx.statement() != null) {
      for (var stmt : ctx.statement()) {
        visit(stmt);
      }
    }
    env.exitBlock();
    return null;
  }

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

  @Override
  public Object visitAssign(DnDLangParser.AssignContext ctx) {
    String id = ctx.ID().getText();

    if (!env.contains(id)) {
      throw new DnDLangError(
          "impossibile assegnare, variabile non dichiarata '" + id + "'", ctx.getStart().getLine());
    }
    String declaredType = env.getType(id);
    Object rightValue;

    rightValue = visit(ctx.expr());

    Object leftValue = null;
    Object finalValue = null;

    switch (ctx.getChild(1).getText()) {
      case "=":
        finalValue = rightValue;
        break;

      case "+=":
        leftValue = env.lookup(id);
        double sum = asDouble(leftValue, ctx) + asDouble(rightValue, ctx);
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
    finalValue = coerceToDeclaredType(finalValue, declaredType);
    env.assign(id, finalValue);

    return null;
  }

  @Override
  public Object visitPostIncExpr(DnDLangParser.PostIncExprContext ctx) {
    String id = ctx.ID().getText();
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

    return oldValue;
  }

  @Override
  public Object visitPreIncExpr(PreIncExprContext ctx) {
    String id = ctx.ID().getText();
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

  @Override
  public Object visitPrintStmt(DnDLangParser.PrintStmtContext ctx) {
    Object value = visit(ctx.expr() != null ? ctx.expr() : ctx.ISTRING());
    System.out.println(value);
    return null;
  }

  @Override
  public Object visitIntExpr(IntExprContext ctx) {
    return Integer.parseInt(ctx.INT().getText());
  }

  @Override
  public Object visitFloatExpr(FloatExprContext ctx) {
    return Double.parseDouble(ctx.FLOAT().getText());
  }

  @Override
  public Object visitBoolExpr(BoolExprContext ctx) {
    return Boolean.parseBoolean(ctx.BOOL().getText());
  }

  @Override
  public Object visitStringExpr(StringExprContext ctx) {
    String str = ctx.STRING().getText();
    String content = str.substring(1, str.length() - 1);

    return content.replace("\\n", "\n").replace("\\t", "\t").replace("\\\"", "\"");
  }

  @Override
  public Object visitIStringExpr(DnDLangParser.IStringExprContext ctx) {
    String rawString = ctx.ISTRING().getText();
    String content = rawString.substring(2, rawString.length() - 1);
    content = content.replace("\\n", "\n").replace("\\t", "\t").replace("\\\"", "\"");

    java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\$\\{([^}]+)\\}");
    java.util.regex.Matcher matcher = pattern.matcher(content);
    StringBuilder result = new StringBuilder();

    while (matcher.find()) {
      String exprText = matcher.group(1).trim();
      String replacement;

      try {
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

          if (env.contains(exprText)) {
            String type = env.getType(exprText);
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

  @Override
  public Object visitIdExpr(IdExprContext ctx) {
    String id = ctx.ID().getText();
    if (!env.contains(id)) {
      throw new DnDLangError("variabile non dichiarata '" + id + "'", ctx.getStart().getLine());
    }
    return env.lookup(id);
  }

  @Override
  public Object visitParenExpr(ParenExprContext ctx) {
    return visit(ctx.expr());
  }

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

  @Override
  public Object visitFunctionCallExpr(DnDLangParser.FunctionCallExprContext ctx) {
    String funcName = ctx.ID().getText();
    int line = ctx.getStart().getLine();

    FunctionSymbol function = env.lookupFunction(funcName, line);
    DnDLangParser.FunctionDeclContext delCtx = function.getDeclarationNode();

    java.util.ArrayList<Object> evalArguments = new java.util.ArrayList<>();
    if (ctx.expr() != null) {
      for (DnDLangParser.ExprContext argCtx : ctx.expr()) {
        evalArguments.add(visit(argCtx));
      }
    }

    DnDLangParser.ParamListContext paramListCtx = delCtx.paramList();
    java.util.List<DnDLangParser.ParamDeclContext> formalParameter = new java.util.ArrayList<>();
    if (paramListCtx != null) {
      formalParameter = paramListCtx.paramDecl();
    }

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

    env.enterBlock();

    try {
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

      visit(delCtx.block());

      return null;

    } catch (ReturnException e) {

      return e.getValue();

    } finally {
      env.exitBlock();
    }
  }

  @Override
  public Object visitExprStmt(ExprStmtContext ctx) {
    visit(ctx.expr());
    return null;
  }

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

  @Override
  public Object visitReturnStmt(DnDLangParser.ReturnStmtContext ctx) {
    Object value = null;
    String type = "Void";

    if (ctx.expr() != null) {
      value = visit(ctx.expr());
    }

    throw new ReturnException(value, type);
  }

  @Override
  public Object visitFunctionDecl(DnDLangParser.FunctionDeclContext ctx) {
    String name = ctx.ID().getText();

    String returnType = ctx.getChild(1).getText();

    env.declareFunction(name, returnType, ctx);
    return null;
  }

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
    }
    return null;
  }

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
        return null;
      }
    }
    if (ctx.defaultBlock() != null) {
      visit(ctx.defaultBlock().block());
    }
    return null;
  }

  // --- Utility Methods ---

  private Object getDefaultValueForType(String type, ParserRuleContext ctx) {
    return switch (type) {
      case "HP", "AC", "Int" -> 0;
      case "Gold" -> 0.0;
      case "Bool" -> false;
      case "String" -> "";
      default -> throw new DnDLangError("Tipo sconosciuto: " + type, ctx.getStart().getLine());
    };
  }

  private Object coerceToDeclaredType(Object value, String declaredType) {
    if ("Gold".equals(declaredType) && value instanceof Integer i) {
      return i.doubleValue();
    }
    if (("HP".equals(declaredType) || "AC".equals(declaredType) || "Int".equals(declaredType))
        && value instanceof Double d) {
      return (int) d.doubleValue();
    }

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

  private Object visitDeclarativeBlock(DnDLangParser.BlockContext ctx) {
    if (ctx.statement() != null) {
      for (var stmt : ctx.statement()) {
        visit(stmt);
      }
    }
    return null;
  }

  private double asDouble(Object value, ParserRuleContext ctx) {
    if (value instanceof Number num) {
      return num.doubleValue();
    }
    throw new DnDLangError(
        "Operazione matematica su un tipo non numerico: " + value, ctx.getStart().getLine());
  }

  private boolean areBothIntegers(Object left, Object right) {
    return left instanceof Integer && right instanceof Integer;
  }

  private int rollDice(int sides) {
    return new java.util.Random().nextInt(sides) + 1;
  }
}

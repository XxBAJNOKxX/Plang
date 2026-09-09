package hu.ppke.itk.plang.prog;

import hu.ppke.itk.plang.gui.ExprNode;
import hu.ppke.itk.plang.gui.ProgramLine;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

abstract class Expression {
   private Type type;

   static Expression parseExpression(Lexer lex, Environment env) {
      ExprList lst = new ExprList(parseRelExpr(lex, env));

      while(true) {
         while(!lex.isKeyword("es")) {
            if (!lex.isKeyword("vagy")) {
               return lst.merge();
            }

            lex.next();
            lst.add(BinaryOperator.OR, parseRelExpr(lex, env));
         }

         lex.next();
         lst.add(BinaryOperator.AND, parseRelExpr(lex, env));
      }
   }

   private static Expression parseRelExpr(Lexer lex, Environment env) {
      ExprList lst = new ExprList(parseAddExpr(lex, env));

      while(true) {
         while(!lex.isKeyword("=")) {
            if (lex.isKeyword("/=")) {
               lex.next();
               lst.add(BinaryOperator.SLASHEQ, parseAddExpr(lex, env));
            } else if (lex.isKeyword("<=")) {
               lex.next();
               lst.add(BinaryOperator.LESSEQ, parseAddExpr(lex, env));
            } else if (lex.isKeyword(">=")) {
               lex.next();
               lst.add(BinaryOperator.GREATEQ, parseAddExpr(lex, env));
            } else if (lex.isKeyword("<")) {
               lex.next();
               lst.add(BinaryOperator.LESS, parseAddExpr(lex, env));
            } else {
               if (!lex.isKeyword(">")) {
                  return lst.merge();
               }

               lex.next();
               lst.add(BinaryOperator.GREATER, parseAddExpr(lex, env));
            }
         }

         lex.next();
         lst.add(BinaryOperator.EQUALS, parseAddExpr(lex, env));
      }
   }

   private static Expression parseAddExpr(Lexer lex, Environment env) {
      ExprList lst = new ExprList(parseMulExpr(lex, env));

      while(true) {
         while(!lex.isKeyword("+")) {
            if (!lex.isKeyword("-")) {
               return lst.merge();
            }

            lex.next();
            lst.add(BinaryOperator.MINUS, parseMulExpr(lex, env));
         }

         lex.next();
         lst.add(BinaryOperator.PLUS, parseMulExpr(lex, env));
      }
   }

   private static Expression parseMulExpr(Lexer lex, Environment env) {
      ExprList lst = new ExprList(parsePowExpr(lex, env));

      while(true) {
         while(!lex.isKeyword("*")) {
            if (lex.isKeyword("/")) {
               lex.next();
               lst.add(BinaryOperator.SLASH, parsePowExpr(lex, env));
            } else if (lex.isKeyword("div")) {
               lex.next();
               lst.add(BinaryOperator.DIV, parsePowExpr(lex, env));
            } else {
               if (!lex.isKeyword("mod")) {
                  return lst.merge();
               }

               lex.next();
               lst.add(BinaryOperator.MOD, parsePowExpr(lex, env));
            }
         }

         lex.next();
         lst.add(BinaryOperator.STAR, parsePowExpr(lex, env));
      }
   }

   private static Expression parsePowExpr(Lexer lex, Environment env) {
      ExprList lst = new ExprList(parsePrefixExpr(lex, env));

      while(true) {
         while(!lex.isKeyword("^")) {
            if (!lex.isKeyword("@")) {
               return lst.merge();
            }

            lex.next();
            lst.add(BinaryOperator.AT, parsePrefixExpr(lex, env));
         }

         lex.next();
         lst.add(BinaryOperator.CIRCFLX, parsePrefixExpr(lex, env));
      }
   }

   private static Expression parsePrefixExpr(Lexer lex, Environment env) {
      UnOpKwd[] var5;
      for(UnOpKwd ok : var5 = Expression.UnOpKwd.values()) {
         if (lex.isKeyword(ok.kwd)) {
            lex.next();
            return UnOpExpression.createUnOpExpr(parsePrefixExpr(lex, env), ok.op);
         }
      }

      if (lex.isKeyword("rnd")) {
         lex.next();
         return RandomExpression.createRandomExpr(parsePrefixExpr(lex, env));
      } else if (lex.isKeyword("vege")) {
         lex.next();
         return EofExpression.createEofExpr(lex, env);
      } else {
         return parseBasicExpr(lex, env);
      }
   }

   private static Expression parseBasicExpr(Lexer lex, Environment env) {
      if (lex.isData()) {
         if (lex.getType() == Lexer.Token.NUMBER) {
            if (lex.getNumber() == (double)((int)lex.getNumber())) {
               int n = (int)lex.getNumber();
               lex.next();
               return parsePostfixExpr(lex, env, new ConstExpression((double)n, BasicType.INTEGER));
            }

            double d = lex.getNumber();
            lex.next();
            return parsePostfixExpr(lex, env, new ConstExpression(d, BasicType.REAL));
         }

         if (lex.getType() == Lexer.Token.CHAR) {
            String s = lex.getString();
            lex.next();
            return parsePostfixExpr(lex, env, new ConstExpression(s, BasicType.CHARACTER));
         }

         if (lex.getType() == Lexer.Token.STRING) {
            String s = lex.getString();
            lex.next();
            return parsePostfixExpr(lex, env, new ConstExpression(s, BasicType.STRING));
         }
      }

      if (lex.isKeyword("igaz")) {
         lex.next();
         return parsePostfixExpr(lex, env, new ConstExpression("IGAZ", BasicType.BOOLEAN));
      } else if (lex.isKeyword("hamis")) {
         lex.next();
         return parsePostfixExpr(lex, env, new ConstExpression("HAMIS", BasicType.BOOLEAN));
      } else if (lex.isKeyword("sv")) {
         lex.next();
         return parsePostfixExpr(lex, env, new ConstExpression("\n", BasicType.CHARACTER));
      } else if (lex.isIdent()) {
         String var = lex.getString();
         lex.next();
         return (Expression)(!lex.isKeyword("(") ? parsePostfixExpr(lex, env, VarExpression.parseVarExpr(var, lex, env)) : new UnparsedExpression(var, lex));
      } else if (lex.isKeyword("|")) {
         lex.next();
         Expression expr = parseExpression(lex, env);
         if (lex.isKeyword("|")) {
            lex.next();
            return parsePostfixExpr(lex, env, UnOpExpression.createUnOpExpr(expr, UnaryOperator.PIPE));
         } else {
            return new UnparsedExpression(expr.toString(), lex);
         }
      } else if (lex.isKeyword("(")) {
         lex.next();
         Expression parExpr = parseExpression(lex, env);
         if (lex.isKeyword(")")) {
            lex.next();
            return parsePostfixExpr(lex, env, new ParenExpression(parExpr));
         } else {
            return new UnparsedExpression(parExpr.toString(), lex);
         }
      } else {
         return new UnparsedExpression("", lex);
      }
   }

   private static Expression parsePostfixExpr(Lexer lex, Environment env, Expression prefix) {
      while(lex.isKeyword("[")) {
         lex.next();
         Expression expr = parseExpression(lex, env);
         if (!lex.isKeyword("]")) {
            if (lex.isKeyword(":")) {
               lex.next();
               Expression expr2 = parseExpression(lex, env);
               if (!lex.isKeyword("]")) {
                  return new UnparsedExpression(prefix.toString() + "[" + expr.toString() + ":" + expr2.toString() + " ??? ", lex);
               }

               lex.next();
               return SubstringExpression.createSubstringExpr(prefix, expr, expr2);
            }

            return new UnparsedExpression(prefix.toString() + "[" + expr.toString() + " ??? ", lex);
         }

         lex.next();
         prefix = BinOpExpression.createBinOpExpr(prefix, expr, BinaryOperator.BRACKET);
      }

      return prefix;
   }

   protected Expression(Type t) {
      this.type = t;
   }

   public abstract String getError();

   public Type getType() {
      return this.type;
   }

   abstract String render();

   abstract Object getValue(State var1);

   abstract ExprNode[] getChildren(State var1);

   ExprNode getTree(State state) {
      if (state == null) {
         return new ExprNode((String)null, this.render(), this.getChildren(state));
      } else {
         Object val = this.getValue(state);
         return val instanceof BadValue ? new ExprNode("<font color=\"red\">" + val, this.render(), this.getChildren(state)) : new ExprNode(this.type.render(val), this.render(), this.getChildren(state));
      }
   }

   private static class UnparsedExpression extends Expression {
      String prefix;
      String rest;
      private static Set<String> termExpr = new HashSet(Arrays.asList(",", "akkor", "ha_vege", "kulonben", "ciklus_vege", "amig", "program_vege"));

      UnparsedExpression(String prefix, Lexer lex) {
         super((Type)null);
         this.prefix = prefix;
         if (prefix.length() > 0) {
            prefix = prefix + " ";
         }

         this.rest = lex.skip(termExpr);
      }

      String render() {
         return this.prefix + ProgramLine.bad(this.rest);
      }

      public String toString() {
         return this.prefix + this.rest;
      }

      Object getValue(State state) {
         return null;
      }

      ExprNode getTree(State state) {
         return new ExprNode((String)null, this.prefix + this.rest, (ExprNode[])null);
      }

      public String getError() {
         return "Nem sikerült értelmezni a kifejezést.";
      }

      ExprNode[] getChildren(State state) {
         return null;
      }
   }

   private static class ExprList {
      private List<Expression> exprs = new LinkedList();
      private List<BinaryOperator> ops;

      ExprList(Expression e) {
         this.exprs.add(e);
         this.ops = new LinkedList();
      }

      void add(BinaryOperator op, Expression e) {
         this.exprs.add(e);
         this.ops.add(op);
      }

      Expression merge() {
         Iterator<BinaryOperator> op = this.ops.iterator();
         Iterator<Expression> exp = this.exprs.iterator();

         Expression e;
         for(e = (Expression)exp.next(); exp.hasNext(); e = BinOpExpression.createBinOpExpr(e, (Expression)exp.next(), (BinaryOperator)op.next())) {
         }

         return e;
      }
   }

   private static enum UnOpKwd {
      MINUS("-", UnaryOperator.MINUS),
      NOT("nem", UnaryOperator.NOT),
      SIN("sin", UnaryOperator.SIN),
      COS("cos", UnaryOperator.COS),
      TAN("tan", UnaryOperator.TAN),
      ARCSIN("arcsin", UnaryOperator.ARCSIN),
      ARCCOS("arccos", UnaryOperator.ARCCOS),
      ARCTAN("arctan", UnaryOperator.ARCTAN),
      LOG("log", UnaryOperator.LOG),
      EXP("exp", UnaryOperator.EXP),
      TRUNC("egesz", UnaryOperator.TRUNC),
      ROUND("kerek", UnaryOperator.ROUND),
      REAL("valos", UnaryOperator.REAL),
      LOWER("kis", UnaryOperator.LOWER),
      UPPER("nagy", UnaryOperator.UPPER),
      ISNUM("szam", UnaryOperator.ISNUM),
      ISALPHA("betu", UnaryOperator.ISALPHA);

      final String kwd;
      final UnaryOperator op;

      private UnOpKwd(String kwd, UnaryOperator op) {
         this.kwd = kwd;
         this.op = op;
      }
   }
}

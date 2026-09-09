package hu.ppke.itk.plang.prog;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

abstract class BasicType extends Type {
   static final BasicType INTEGER;
   static final BasicType REAL;
   static final BasicType STRING;
   static final BasicType CHARACTER;
   static final BasicType BOOLEAN;
   private static Map<BT, Type> typeObj;
   private String name;
   private BT btype;
   Map<UnaryOperator, UnFun> unFuns;
   Map<BinaryOperator, Map<BT, BinFun>> binFuns;

   static {
      INTEGER = new BasicType("EGÉSZ", BasicType.BT.INTEGER) {
         Object constValue(String val) {
            return "???";
         }

         Object constValue(double val) {
            return new Integer((int)val);
         }

         Integer readData(StreamData strm, StreamState sst) throws StreamData.DataError {
            return strm.readInteger(sst);
         }
      };
      REAL = new BasicType("VALÓS", BasicType.BT.REAL) {
         private DecimalFormat form;

         {
            this.form = new DecimalFormat("#0.0#########", new DecimalFormatSymbols(Locale.US));
         }

         boolean canCopy(Type type) {
            return type == this || type == INTEGER;
         }

         Object copy(Object val) {
            return val instanceof Integer ? (double)(Integer)val : val;
         }

         public String toString(Object ob) {
            return this.form.format(ob);
         }

         Object constValue(String val) {
            return "???";
         }

         Object constValue(double val) {
            return new Double(val);
         }

         void printData(StreamData str, StreamState sst, Object val) {
            str.append(sst, this.form.format(val));
         }

         Double readData(StreamData strm, StreamState sst) throws StreamData.DataError {
            return strm.readReal(sst);
         }
      };
      STRING = new BasicType("SZÖVEG", BasicType.BT.STRING) {
         Object constValue(String val) {
            return val;
         }

         Object constValue(double val) {
            return "" + val;
         }

         public String toString(Object obj) {
            return "\"" + obj + "\"";
         }

         String readData(StreamData strm, StreamState sst) throws StreamData.DataError {
            return strm.readString(sst);
         }

         boolean hasAccessor(BinaryOperator op, Type rhs) {
            return op == BinaryOperator.BRACKET && rhs == INTEGER;
         }

         Object access(BinaryOperator op, Object oldVal, Object rhs, Object newVal) {
            if (newVal instanceof BadValue) {
               return newVal;
            } else {
               char[] s = ((String)oldVal).toCharArray();
               s[(Integer)rhs] = (Character)newVal;
               return new String(s);
            }
         }
      };
      CHARACTER = new BasicType("KARAKTER", BasicType.BT.CHARACTER) {
         Object constValue(String val) {
            return new Character(val.charAt(0));
         }

         Object constValue(double val) {
            return new Character('?');
         }

         public String toString(Object obj) {
            return (Character)obj == '\n' ? "SV" : "'" + obj + "'";
         }

         Object readData(StreamData strm, StreamState sst) throws StreamData.DataError {
            return strm.readChar(sst);
         }
      };
      BOOLEAN = new BasicType("LOGIKAI", BasicType.BT.BOOLEAN) {
         Object constValue(String val) {
            return val.equals("IGAZ") ? new Boolean(true) : new Boolean(false);
         }

         Object constValue(double val) {
            return new Boolean(false);
         }

         void printData(StreamData str, StreamState sst, Object val) {
            str.append(sst, (Boolean)val ? "igaz" : "hamis");
         }

         public String toString(Object obj) {
            return (Boolean)obj ? "igaz" : "hamis";
         }

         public String render(Object obj) {
            return (Boolean)obj ? "IGAZ" : "HAMIS";
         }

         Object readData(StreamData strm, StreamState sst) throws StreamData.DataError {
            return strm.readBoolean(sst);
         }
      };
      typeObj = new EnumMap(BT.class);
      typeObj.put(BasicType.BT.INTEGER, INTEGER);
      typeObj.put(BasicType.BT.REAL, REAL);
      typeObj.put(BasicType.BT.STRING, STRING);
      typeObj.put(BasicType.BT.CHARACTER, CHARACTER);
      typeObj.put(BasicType.BT.BOOLEAN, BOOLEAN);
   }

   private BasicType(String name, BT btype) {
      this.name = name;
      this.btype = btype;
      this.binFuns = new EnumMap(BinaryOperator.class);

      BinFun[] var6;
      for(BinFun bf : var6 = BasicType.BinFun.values()) {
         if (bf.leftType == btype) {
            if (!this.binFuns.containsKey(bf.op)) {
               this.binFuns.put(bf.op, new EnumMap(BT.class));
            }

            ((Map)this.binFuns.get(bf.op)).put(bf.rightType, bf);
         }
      }

      this.unFuns = new EnumMap(UnaryOperator.class);

      for(UnFun uf : BasicType.UnFun.values()) {
         if (uf.parType == btype) {
            this.unFuns.put(uf.op, uf);
         }
      }

   }

   public boolean equals(Object val) {
      return val instanceof BasicType && ((BasicType)val).name.equals(this.name);
   }

   public int hashCode() {
      return this.name.hashCode();
   }

   String render() {
      return this.name;
   }

   public String toString() {
      return this.name;
   }

   boolean canCopy(Type type) {
      return type == this;
   }

   Object copy(Object val) {
      return val;
   }

   final Object initVal() {
      return null;
   }

   abstract Object constValue(String var1);

   abstract Object constValue(double var1);

   Type operatorType(UnaryOperator op) {
      UnFun f = (UnFun)this.unFuns.get(op);
      return f != null ? (Type)typeObj.get(f.retType) : null;
   }

   Object apply(UnaryOperator op, Object val) {
      return ((UnFun)this.unFuns.get(op)).fun(val);
   }

   Type operatorType(BinaryOperator op, Type rhs) {
      if (rhs instanceof BasicType && this.binFuns.get(op) != null) {
         BinFun f = (BinFun)((Map)this.binFuns.get(op)).get(((BasicType)rhs).btype);
         return f != null ? (Type)typeObj.get(f.retType) : null;
      } else {
         return null;
      }
   }

   Object apply(BinaryOperator op, Type rt, Object left, Object right) {
      return ((BinFun)((Map)this.binFuns.get(op)).get(((BasicType)rt).btype)).fun(left, right);
   }

   boolean hasAccessor(UnaryOperator op) {
      return false;
   }

   Object access(UnaryOperator op, Object oldVal, Object newVal) {
      return null;
   }

   boolean hasAccessor(BinaryOperator op, Type rhs) {
      return false;
   }

   Object access(BinaryOperator op, Object oldVal, Object rhs, Object newVal) {
      return null;
   }

   // $FF: synthetic method
   BasicType(String var1, BT var2, BasicType var3) {
      this(var1, var2);
   }

   private static enum BT {
      INTEGER,
      REAL,
      STRING,
      CHARACTER,
      BOOLEAN;
   }

   private static enum BinFun {
      IntIntPlus(BasicType.BT.INTEGER, BasicType.BT.INTEGER, BasicType.BT.INTEGER, BinaryOperator.PLUS) {
         Object fun(Object l, Object r) {
            return (Integer)l + (Integer)r;
         }
      },
      IntIntMinus(BasicType.BT.INTEGER, BasicType.BT.INTEGER, BasicType.BT.INTEGER, BinaryOperator.MINUS) {
         Object fun(Object l, Object r) {
            return (Integer)l - (Integer)r;
         }
      },
      IntIntTimes(BasicType.BT.INTEGER, BasicType.BT.INTEGER, BasicType.BT.INTEGER, BinaryOperator.STAR) {
         Object fun(Object l, Object r) {
            return (Integer)l * (Integer)r;
         }
      },
      IntIntDivide(BasicType.BT.INTEGER, BasicType.BT.INTEGER, BasicType.BT.INTEGER, BinaryOperator.DIV) {
         Object fun(Object l, Object r) {
            return (Integer)r == 0 ? new BadValue("Osztás nullával") : (Integer)l / (Integer)r;
         }
      },
      IntIntModulo(BasicType.BT.INTEGER, BasicType.BT.INTEGER, BasicType.BT.INTEGER, BinaryOperator.MOD) {
         Object fun(Object l, Object r) {
            return (Integer)r == 0 ? new BadValue("Osztás nullával") : (Integer)l % (Integer)r;
         }
      },
      IntIntPow(BasicType.BT.INTEGER, BasicType.BT.INTEGER, BasicType.BT.INTEGER, BinaryOperator.CIRCFLX) {
         Object fun(Object l, Object r) {
            return (int)Math.pow((double)(Integer)l, (double)(Integer)r);
         }
      },
      IntIntEquals(BasicType.BT.INTEGER, BasicType.BT.INTEGER, BasicType.BT.BOOLEAN, BinaryOperator.EQUALS) {
         Object fun(Object l, Object r) {
            return l.equals(r);
         }
      },
      IntIntNotEq(BasicType.BT.INTEGER, BasicType.BT.INTEGER, BasicType.BT.BOOLEAN, BinaryOperator.SLASHEQ) {
         Object fun(Object l, Object r) {
            return !l.equals(r);
         }
      },
      IntIntLess(BasicType.BT.INTEGER, BasicType.BT.INTEGER, BasicType.BT.BOOLEAN, BinaryOperator.LESS) {
         Object fun(Object l, Object r) {
            return (Integer)l < (Integer)r ? true : false;
         }
      },
      IntIntGreater(BasicType.BT.INTEGER, BasicType.BT.INTEGER, BasicType.BT.BOOLEAN, BinaryOperator.GREATER) {
         Object fun(Object l, Object r) {
            return (Integer)l > (Integer)r ? true : false;
         }
      },
      IntIntLessEq(BasicType.BT.INTEGER, BasicType.BT.INTEGER, BasicType.BT.BOOLEAN, BinaryOperator.LESSEQ) {
         Object fun(Object l, Object r) {
            return (Integer)l <= (Integer)r ? true : false;
         }
      },
      IntIntGreaterEq(BasicType.BT.INTEGER, BasicType.BT.INTEGER, BasicType.BT.BOOLEAN, BinaryOperator.GREATEQ) {
         Object fun(Object l, Object r) {
            return (Integer)l >= (Integer)r ? true : false;
         }
      },
      IntRealPlus(BasicType.BT.INTEGER, BasicType.BT.REAL, BasicType.BT.REAL, BinaryOperator.PLUS) {
         Object fun(Object l, Object r) {
            return (double)(Integer)l + (Double)r;
         }
      },
      IntRealMinus(BasicType.BT.INTEGER, BasicType.BT.REAL, BasicType.BT.REAL, BinaryOperator.MINUS) {
         Object fun(Object l, Object r) {
            return (double)(Integer)l - (Double)r;
         }
      },
      IntRealTimes(BasicType.BT.INTEGER, BasicType.BT.REAL, BasicType.BT.REAL, BinaryOperator.STAR) {
         Object fun(Object l, Object r) {
            return (double)(Integer)l * (Double)r;
         }
      },
      IntRealDivide(BasicType.BT.INTEGER, BasicType.BT.REAL, BasicType.BT.REAL, BinaryOperator.SLASH) {
         Object fun(Object l, Object r) {
            return (double)(Integer)l / (Double)r;
         }
      },
      IntRealPow(BasicType.BT.INTEGER, BasicType.BT.REAL, BasicType.BT.REAL, BinaryOperator.CIRCFLX) {
         Object fun(Object l, Object r) {
            return Math.pow((double)(Integer)l, (Double)r);
         }
      },
      IntRealEquals(BasicType.BT.INTEGER, BasicType.BT.REAL, BasicType.BT.BOOLEAN, BinaryOperator.EQUALS) {
         Object fun(Object l, Object r) {
            return (double)(Integer)l == (Double)r ? true : false;
         }
      },
      IntRealNotEq(BasicType.BT.INTEGER, BasicType.BT.REAL, BasicType.BT.BOOLEAN, BinaryOperator.SLASHEQ) {
         Object fun(Object l, Object r) {
            return (double)(Integer)l != (Double)r ? true : false;
         }
      },
      IntRealLess(BasicType.BT.INTEGER, BasicType.BT.REAL, BasicType.BT.BOOLEAN, BinaryOperator.LESS) {
         Object fun(Object l, Object r) {
            return (double)(Integer)l < (Double)r ? true : false;
         }
      },
      IntRealGreater(BasicType.BT.INTEGER, BasicType.BT.REAL, BasicType.BT.BOOLEAN, BinaryOperator.GREATER) {
         Object fun(Object l, Object r) {
            return (double)(Integer)l > (Double)r ? true : false;
         }
      },
      IntRealLessEq(BasicType.BT.INTEGER, BasicType.BT.REAL, BasicType.BT.BOOLEAN, BinaryOperator.LESSEQ) {
         Object fun(Object l, Object r) {
            return (double)(Integer)l <= (Double)r ? true : false;
         }
      },
      IntRealGreaterEq(BasicType.BT.INTEGER, BasicType.BT.REAL, BasicType.BT.BOOLEAN, BinaryOperator.GREATEQ) {
         Object fun(Object l, Object r) {
            return (double)(Integer)l >= (Double)r ? true : false;
         }
      },
      RealIntPlus(BasicType.BT.REAL, BasicType.BT.INTEGER, BasicType.BT.REAL, BinaryOperator.PLUS) {
         Object fun(Object l, Object r) {
            return (Double)l + (double)(Integer)r;
         }
      },
      RealIntMinus(BasicType.BT.REAL, BasicType.BT.INTEGER, BasicType.BT.REAL, BinaryOperator.MINUS) {
         Object fun(Object l, Object r) {
            return (Double)l - (double)(Integer)r;
         }
      },
      RealIntTimes(BasicType.BT.REAL, BasicType.BT.INTEGER, BasicType.BT.REAL, BinaryOperator.STAR) {
         Object fun(Object l, Object r) {
            return (Double)l * (double)(Integer)r;
         }
      },
      RealIntPow(BasicType.BT.REAL, BasicType.BT.INTEGER, BasicType.BT.REAL, BinaryOperator.CIRCFLX) {
         Object fun(Object l, Object r) {
            return Math.pow((Double)l, (double)(Integer)r);
         }
      },
      RealIntDivide(BasicType.BT.REAL, BasicType.BT.INTEGER, BasicType.BT.REAL, BinaryOperator.SLASH) {
         Object fun(Object l, Object r) {
            return (Double)l / (double)(Integer)r;
         }
      },
      RealIntEquals(BasicType.BT.REAL, BasicType.BT.INTEGER, BasicType.BT.BOOLEAN, BinaryOperator.EQUALS) {
         Object fun(Object l, Object r) {
            return (Double)l == (double)(Integer)r ? true : false;
         }
      },
      RealIntNotEq(BasicType.BT.REAL, BasicType.BT.INTEGER, BasicType.BT.BOOLEAN, BinaryOperator.SLASHEQ) {
         Object fun(Object l, Object r) {
            return (Double)l != (double)(Integer)r ? true : false;
         }
      },
      RealIntLess(BasicType.BT.REAL, BasicType.BT.INTEGER, BasicType.BT.BOOLEAN, BinaryOperator.LESS) {
         Object fun(Object l, Object r) {
            return (Double)l < (double)(Integer)r ? true : false;
         }
      },
      RealIntGreater(BasicType.BT.REAL, BasicType.BT.INTEGER, BasicType.BT.BOOLEAN, BinaryOperator.GREATER) {
         Object fun(Object l, Object r) {
            return (Double)l > (double)(Integer)r ? true : false;
         }
      },
      RealIntLessEq(BasicType.BT.REAL, BasicType.BT.INTEGER, BasicType.BT.BOOLEAN, BinaryOperator.LESSEQ) {
         Object fun(Object l, Object r) {
            return (Double)l <= (double)(Integer)r ? true : false;
         }
      },
      RealIntGreaterEq(BasicType.BT.REAL, BasicType.BT.INTEGER, BasicType.BT.BOOLEAN, BinaryOperator.GREATEQ) {
         Object fun(Object l, Object r) {
            return (Double)l >= (double)(Integer)r ? true : false;
         }
      },
      RealRealPlus(BasicType.BT.REAL, BasicType.BT.REAL, BasicType.BT.REAL, BinaryOperator.PLUS) {
         Object fun(Object l, Object r) {
            return (Double)l + (Double)r;
         }
      },
      RealRealMinus(BasicType.BT.REAL, BasicType.BT.REAL, BasicType.BT.REAL, BinaryOperator.MINUS) {
         Object fun(Object l, Object r) {
            return (Double)l - (Double)r;
         }
      },
      RealRealTimes(BasicType.BT.REAL, BasicType.BT.REAL, BasicType.BT.REAL, BinaryOperator.STAR) {
         Object fun(Object l, Object r) {
            return (Double)l * (Double)r;
         }
      },
      RealRealPow(BasicType.BT.REAL, BasicType.BT.REAL, BasicType.BT.REAL, BinaryOperator.CIRCFLX) {
         Object fun(Object l, Object r) {
            return Math.pow((Double)l, (Double)r);
         }
      },
      RealRealDivide(BasicType.BT.REAL, BasicType.BT.REAL, BasicType.BT.REAL, BinaryOperator.SLASH) {
         Object fun(Object l, Object r) {
            return (Double)l / (Double)r;
         }
      },
      RealRealEquals(BasicType.BT.REAL, BasicType.BT.REAL, BasicType.BT.BOOLEAN, BinaryOperator.EQUALS) {
         Object fun(Object l, Object r) {
            return ((Double)l).equals(r);
         }
      },
      RealRealNotEq(BasicType.BT.REAL, BasicType.BT.REAL, BasicType.BT.BOOLEAN, BinaryOperator.SLASHEQ) {
         Object fun(Object l, Object r) {
            return !((Double)l).equals(r);
         }
      },
      RealRealLess(BasicType.BT.REAL, BasicType.BT.REAL, BasicType.BT.BOOLEAN, BinaryOperator.LESS) {
         Object fun(Object l, Object r) {
            return (Double)l < (Double)r ? true : false;
         }
      },
      RealRealGreater(BasicType.BT.REAL, BasicType.BT.REAL, BasicType.BT.BOOLEAN, BinaryOperator.GREATER) {
         Object fun(Object l, Object r) {
            return (Double)l > (Double)r ? true : false;
         }
      },
      RealRealLessEq(BasicType.BT.REAL, BasicType.BT.REAL, BasicType.BT.BOOLEAN, BinaryOperator.LESSEQ) {
         Object fun(Object l, Object r) {
            return (Double)l <= (Double)r ? true : false;
         }
      },
      RealRealGreaterEq(BasicType.BT.REAL, BasicType.BT.REAL, BasicType.BT.BOOLEAN, BinaryOperator.GREATEQ) {
         Object fun(Object l, Object r) {
            return (Double)l >= (Double)r ? true : false;
         }
      },
      BooleanEquals(BasicType.BT.BOOLEAN, BasicType.BT.BOOLEAN, BasicType.BT.BOOLEAN, BinaryOperator.EQUALS) {
         Object fun(Object l, Object r) {
            return ((Boolean)l).equals((Boolean)r);
         }
      },
      BooleanNotEq(BasicType.BT.BOOLEAN, BasicType.BT.BOOLEAN, BasicType.BT.BOOLEAN, BinaryOperator.SLASHEQ) {
         Object fun(Object l, Object r) {
            return !((Boolean)l).equals((Boolean)r);
         }
      },
      CharEquals(BasicType.BT.CHARACTER, BasicType.BT.CHARACTER, BasicType.BT.BOOLEAN, BinaryOperator.EQUALS) {
         Object fun(Object l, Object r) {
            return ((Character)l).equals((Character)r);
         }
      },
      CharNotEq(BasicType.BT.CHARACTER, BasicType.BT.CHARACTER, BasicType.BT.BOOLEAN, BinaryOperator.SLASHEQ) {
         Object fun(Object l, Object r) {
            return !((Character)l).equals((Character)r);
         }
      },
      CharLess(BasicType.BT.CHARACTER, BasicType.BT.CHARACTER, BasicType.BT.BOOLEAN, BinaryOperator.LESS) {
         Object fun(Object l, Object r) {
            return ((Character)l).compareTo((Character)r) < 0 ? true : false;
         }
      },
      CharGreater(BasicType.BT.CHARACTER, BasicType.BT.CHARACTER, BasicType.BT.BOOLEAN, BinaryOperator.GREATER) {
         Object fun(Object l, Object r) {
            return ((Character)l).compareTo((Character)r) > 0 ? true : false;
         }
      },
      CharLessEq(BasicType.BT.CHARACTER, BasicType.BT.CHARACTER, BasicType.BT.BOOLEAN, BinaryOperator.LESSEQ) {
         Object fun(Object l, Object r) {
            return ((Character)l).compareTo((Character)r) <= 0 ? true : false;
         }
      },
      CharGreaterEq(BasicType.BT.CHARACTER, BasicType.BT.CHARACTER, BasicType.BT.BOOLEAN, BinaryOperator.GREATEQ) {
         Object fun(Object l, Object r) {
            return ((Character)l).compareTo((Character)r) >= 0 ? true : false;
         }
      },
      StringConcat(BasicType.BT.STRING, BasicType.BT.STRING, BasicType.BT.STRING, BinaryOperator.PLUS) {
         Object fun(Object l, Object r) {
            return (String)l + (String)r;
         }
      },
      StringCharPlus(BasicType.BT.STRING, BasicType.BT.CHARACTER, BasicType.BT.STRING, BinaryOperator.PLUS) {
         Object fun(Object l, Object r) {
            return (String)l + (Character)r;
         }
      },
      CharStringPlus(BasicType.BT.CHARACTER, BasicType.BT.STRING, BasicType.BT.STRING, BinaryOperator.PLUS) {
         Object fun(Object l, Object r) {
            return (Character)l + (String)r;
         }
      },
      StringEquals(BasicType.BT.STRING, BasicType.BT.STRING, BasicType.BT.BOOLEAN, BinaryOperator.EQUALS) {
         Object fun(Object l, Object r) {
            return ((String)l).equals((String)r);
         }
      },
      StringNotEq(BasicType.BT.STRING, BasicType.BT.STRING, BasicType.BT.BOOLEAN, BinaryOperator.SLASHEQ) {
         Object fun(Object l, Object r) {
            return !((String)l).equals((String)r);
         }
      },
      StringLess(BasicType.BT.STRING, BasicType.BT.STRING, BasicType.BT.BOOLEAN, BinaryOperator.LESS) {
         Object fun(Object l, Object r) {
            return ((String)l).compareTo((String)r) < 0 ? true : false;
         }
      },
      StringGreater(BasicType.BT.STRING, BasicType.BT.STRING, BasicType.BT.BOOLEAN, BinaryOperator.GREATER) {
         Object fun(Object l, Object r) {
            return ((String)l).compareTo((String)r) > 0 ? true : false;
         }
      },
      StringLessEq(BasicType.BT.STRING, BasicType.BT.STRING, BasicType.BT.BOOLEAN, BinaryOperator.LESSEQ) {
         Object fun(Object l, Object r) {
            return ((String)l).compareTo((String)r) <= 0 ? true : false;
         }
      },
      StringGreaterEq(BasicType.BT.STRING, BasicType.BT.STRING, BasicType.BT.BOOLEAN, BinaryOperator.GREATEQ) {
         Object fun(Object l, Object r) {
            return ((String)l).compareTo((String)r) >= 0 ? true : false;
         }
      },
      StringFindChar(BasicType.BT.STRING, BasicType.BT.CHARACTER, BasicType.BT.INTEGER, BinaryOperator.AT) {
         Object fun(Object l, Object r) {
            int i = ((String)l).indexOf((Character)r);
            return i < 0 ? ((String)l).length() : i;
         }
      },
      StringFindString(BasicType.BT.STRING, BasicType.BT.STRING, BasicType.BT.INTEGER, BinaryOperator.AT) {
         Object fun(Object l, Object r) {
            int i = ((String)l).indexOf((String)r);
            return i < 0 ? ((String)l).length() : i;
         }
      },
      StringIndex(BasicType.BT.STRING, BasicType.BT.INTEGER, BasicType.BT.CHARACTER, BinaryOperator.BRACKET) {
         Object fun(Object l, Object r) {
            return (Integer)r >= 0 && ((String)l).length() > (Integer)r ? ((String)l).charAt((Integer)r) : new BadValue("Hibás szövegindex");
         }
      };

      final BT leftType;
      final BT rightType;
      final BT retType;
      final BinaryOperator op;

      private BinFun(BT leftType, BT rightType, BT retType, BinaryOperator op) {
         this.leftType = leftType;
         this.rightType = rightType;
         this.retType = retType;
         this.op = op;
      }

      abstract Object fun(Object var1, Object var2);

      // $FF: synthetic method
      BinFun(BT var3, BT var4, BT var5, BinaryOperator var6, BinFun var7) {
         this(var3, var4, var5, var6);
      }
   }

   private static enum UnFun {
      IntMinus(BasicType.BT.INTEGER, BasicType.BT.INTEGER, UnaryOperator.MINUS) {
         Object fun(Object par) {
            return -(Integer)par;
         }
      },
      IntAbs(BasicType.BT.INTEGER, BasicType.BT.INTEGER, UnaryOperator.PIPE) {
         Object fun(Object par) {
            return Math.abs((Integer)par);
         }
      },
      IntReal(BasicType.BT.INTEGER, BasicType.BT.REAL, UnaryOperator.REAL) {
         Object fun(Object par) {
            return ((Integer)par).doubleValue();
         }
      },
      RealMinus(BasicType.BT.REAL, BasicType.BT.REAL, UnaryOperator.MINUS) {
         Object fun(Object par) {
            return -(Double)par;
         }
      },
      RealAbs(BasicType.BT.REAL, BasicType.BT.REAL, UnaryOperator.PIPE) {
         Object fun(Object par) {
            return Math.abs((Double)par);
         }
      },
      Trunc(BasicType.BT.REAL, BasicType.BT.INTEGER, UnaryOperator.TRUNC) {
         Object fun(Object par) {
            return (int)(double)(Double)par;
         }
      },
      Round(BasicType.BT.REAL, BasicType.BT.INTEGER, UnaryOperator.ROUND) {
         Object fun(Object par) {
            return (int)Math.round((Double)par);
         }
      },
      Sine(BasicType.BT.REAL, BasicType.BT.REAL, UnaryOperator.SIN) {
         Object fun(Object par) {
            return Math.sin((Double)par);
         }
      },
      Cosine(BasicType.BT.REAL, BasicType.BT.REAL, UnaryOperator.COS) {
         Object fun(Object par) {
            return Math.cos((Double)par);
         }
      },
      Tangent(BasicType.BT.REAL, BasicType.BT.REAL, UnaryOperator.TAN) {
         Object fun(Object par) {
            return Math.tan((Double)par);
         }
      },
      ArcSine(BasicType.BT.REAL, BasicType.BT.REAL, UnaryOperator.ARCSIN) {
         Object fun(Object par) {
            return Math.asin((Double)par);
         }
      },
      ArcCosine(BasicType.BT.REAL, BasicType.BT.REAL, UnaryOperator.ARCCOS) {
         Object fun(Object par) {
            return Math.acos((Double)par);
         }
      },
      ArcTangent(BasicType.BT.REAL, BasicType.BT.REAL, UnaryOperator.ARCTAN) {
         Object fun(Object par) {
            return Math.atan((Double)par);
         }
      },
      Logarithm(BasicType.BT.REAL, BasicType.BT.REAL, UnaryOperator.LOG) {
         Object fun(Object par) {
            return Math.log((Double)par);
         }
      },
      Exponent(BasicType.BT.REAL, BasicType.BT.REAL, UnaryOperator.EXP) {
         Object fun(Object par) {
            return Math.exp((Double)par);
         }
      },
      BoolNot(BasicType.BT.BOOLEAN, BasicType.BT.BOOLEAN, UnaryOperator.NOT) {
         Object fun(Object par) {
            return !(Boolean)par;
         }
      },
      CharLower(BasicType.BT.CHARACTER, BasicType.BT.CHARACTER, UnaryOperator.LOWER) {
         Object fun(Object par) {
            return Character.toLowerCase((Character)par);
         }
      },
      CharUpper(BasicType.BT.CHARACTER, BasicType.BT.CHARACTER, UnaryOperator.UPPER) {
         Object fun(Object par) {
            return Character.toUpperCase((Character)par);
         }
      },
      CharIsNum(BasicType.BT.CHARACTER, BasicType.BT.BOOLEAN, UnaryOperator.ISNUM) {
         Object fun(Object par) {
            return Character.isDigit((Character)par);
         }
      },
      CharIsAlpha(BasicType.BT.CHARACTER, BasicType.BT.BOOLEAN, UnaryOperator.ISALPHA) {
         Object fun(Object par) {
            return Character.isLetter((Character)par);
         }
      },
      StringLength(BasicType.BT.STRING, BasicType.BT.INTEGER, UnaryOperator.PIPE) {
         Object fun(Object par) {
            return ((String)par).length();
         }
      };

      final BT parType;
      final BT retType;
      final UnaryOperator op;

      private UnFun(BT parType, BT retType, UnaryOperator op) {
         this.parType = parType;
         this.retType = retType;
         this.op = op;
      }

      abstract Object fun(Object var1);

      // $FF: synthetic method
      UnFun(BT var3, BT var4, UnaryOperator var5, UnFun var6) {
         this(var3, var4, var5);
      }
   }
}

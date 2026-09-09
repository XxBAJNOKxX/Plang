package hu.ppke.itk.plang.prog;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

class Environment {
   private Map<String, SubProgram> subProgs = new HashMap();
   private Map<String, Type> vars = new HashMap();
   private Map<StreamKind, Set<String>> streamNames;

   public Environment() {
      this.streamNames = new EnumMap(StreamKind.class);
      this.streamNames.put(StreamKind.INPUT, new TreeSet());
      ((Set)this.streamNames.get(StreamKind.INPUT)).add("BEMENET");
      this.streamNames.put(StreamKind.OUTPUT, new TreeSet());
      ((Set)this.streamNames.get(StreamKind.OUTPUT)).add("KIMENET");
   }

   Environment(Environment parent) {
      this.streamNames = parent.streamNames;
   }

   boolean hasSubProg(String name) {
      return this.subProgs.containsKey(name);
   }

   void addSubProg(SubProgram subPrg) {
      this.subProgs.put(subPrg.getName(), subPrg);
   }

   SubProgram getSubProg(String name) {
      return (SubProgram)this.subProgs.get(name);
   }

   boolean hasVar(String name) {
      return this.vars.containsKey(name);
   }

   void addVar(String name, Type type) {
      this.vars.put(name, type);
   }

   public Type getVarType(String name) {
      return (Type)this.vars.get(name);
   }

   void addStream(StreamKind kind, String name) {
      ((Set)this.streamNames.get(kind)).add(name);
   }

   public Set<String> getStreams(StreamKind kind) {
      return (Set)this.streamNames.get(kind);
   }

   public StreamKind getStreamKind(String stream) {
      for(StreamKind kind : this.streamNames.keySet()) {
         if (((Set)this.streamNames.get(kind)).contains(stream)) {
            return kind;
         }
      }

      return null;
   }

   Map<String, Object> createVariables() {
      Set<String> names = this.vars.keySet();
      Map<String, Object> init = new HashMap();

      for(String name : names) {
         init.put(name, ((Type)this.vars.get(name)).initVal());
      }

      return init;
   }
}

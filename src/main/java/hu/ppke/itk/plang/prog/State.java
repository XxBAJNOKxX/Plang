package hu.ppke.itk.plang.prog;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;

public final class State implements Cloneable {
   private Statement current;
   private long steps;
   private Map<String, Object> vars;
   private Environment env;
   private Map<String, StreamData> streams;
   private Map<String, StreamState> streamStates;
   private Map<Integer, Double> randomState = null;
   private Map<Integer, List<State>> subStateLists = null;
   private String error;

   private State(Environment env, Map<String, Object> vars, Map<String, StreamData> streams, Map<String, StreamState> streamStates, long steps) {
      this.env = env;
      this.vars = vars;
      this.streams = streams;
      this.streamStates = streamStates;
      this.steps = steps;
   }

   State(Environment env, long maxSteps) {
      this.env = env;
      this.steps = maxSteps;
      this.vars = env.createVariables();
      this.streams = new TreeMap();
      this.streamStates = new TreeMap();

      StreamKind[] var7;
      for(StreamKind kind : var7 = StreamKind.values()) {
         for(String name : env.getStreams(kind)) {
            this.streams.put(name, new StreamData(kind));
            this.streamStates.put(name, new StreamState());
         }
      }

   }

   State(Environment env, State parent) {
      this.env = env;
      this.steps = parent.steps;
      this.vars = env.createVariables();
      this.streams = parent.streams;
      this.streamStates = new HashMap(parent.streamStates.size());

      for(String stream : parent.streamStates.keySet()) {
         this.streamStates.put(stream, ((StreamState)parent.streamStates.get(stream)).newSection());
      }

   }

   void setError(String error) {
      this.error = error;
   }

   public String getError() {
      return this.error;
   }

   State newState() {
      Map<String, StreamState> sst = new HashMap(this.streamStates.size());

      for(String stream : this.streamStates.keySet()) {
         sst.put(stream, ((StreamState)this.streamStates.get(stream)).newSection());
      }

      return new State(this.env, new HashMap(this.vars), this.streams, sst, this.steps);
   }

   State newError(String error) {
      State next = this.newState();
      next.setError(error);
      return next;
   }

   public State execNext() {
      if (this.current != null && this.error == null) {
         if (this.steps <= 0L) {
            return this.newError("A program elérte a futási lépések korlátját.");
         } else {
            --this.steps;
            return this.current.execute(this);
         }
      } else {
         return null;
      }
   }

   public void setStatement(Statement stmt) {
      for(this.current = stmt; this.current instanceof CommentStatement; this.current = this.current.getNext()) {
      }

   }

   void setVar(String name, Object value) {
      this.vars.put(name, value);
   }

   public Object getVar(String name) {
      return this.vars.get(name);
   }

   public Type getVarType(String name) {
      return this.env.getVarType(name);
   }

   public Set<String> getVarNames() {
      return this.vars.keySet();
   }

   public StreamData getStream(String name) {
      return (StreamData)this.streams.get(name);
   }

   public Set<String> getStreamNames() {
      return this.streams.keySet();
   }

   public StreamState getStreamState(String name) {
      return (StreamState)this.streamStates.get(name);
   }

   public int getLine() {
      return this.current.getLineIndex();
   }

   public double getRandom(int rndID) {
      if (this.randomState == null) {
         this.randomState = new TreeMap();
      }

      Object rnd = this.randomState.get(rndID);
      if (rnd != null) {
         return (Double)rnd;
      } else {
         double d = Math.random();
         this.randomState.put(rndID, d);
         return d;
      }
   }

   void addSubStates(int id, List<State> states) {
      if (this.subStateLists == null) {
         this.subStateLists = new TreeMap();
      }

      this.subStateLists.put(id, new Vector(states));
   }

   List<State> getSubStates(int id) {
      return this.subStateLists != null && this.subStateLists.containsKey(id) ? (List)this.subStateLists.get(id) : null;
   }
}

package hu.ppke.itk.plang.prog;

class FileType extends Type {
   static final FileType INPUT;
   static final FileType OUTPUT;
   static final String CLOSED_FILE = "<--LEZART-->";
   static final String STD_INPUT = "BEMENET";
   static final String STD_OUTPUT = "KIMENET";
   private FT type;

   static {
      INPUT = new FileType(FileType.FT.INPUT);
      OUTPUT = new FileType(FileType.FT.OUTPUT);
   }

   private FileType(FT type) {
      this.type = type;
   }

   String render() {
      return this.type == FileType.FT.INPUT ? "BEFÁJL" : "KIFÁJL";
   }

   boolean canCopy(Type type) {
      return false;
   }

   Object copy(Object val) {
      return new BadValue("Fájl típusú változókat nem lehet másolni.");
   }

   Object initVal() {
      return "<--LEZART-->";
   }

   Type operatorType(UnaryOperator op) {
      return null;
   }

   Object apply(UnaryOperator op, Object val) {
      return null;
   }

   Type operatorType(BinaryOperator op, Type rhs) {
      return null;
   }

   Object apply(BinaryOperator op, Type rhs, Object left, Object right) {
      return null;
   }

   Object readData(StreamData strm, StreamState sst) throws StreamData.DataError {
      return null;
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

   private static enum FT {
      INPUT,
      OUTPUT;
   }
}

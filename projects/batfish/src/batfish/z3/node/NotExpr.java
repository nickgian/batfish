package batfish.z3.node;

import java.util.ArrayList;
import java.util.List;

public class NotExpr extends BooleanExpr implements ComplexExpr {

   private BooleanExpr _arg;
   private List<Expr> _subExpressions;

   public NotExpr() {
      init();
   }

   public NotExpr(BooleanExpr arg) {
      init();
      _arg = arg;
      refreshSubExpressions();
   }

   @Override
   public List<Expr> getSubExpressions() {
      return _subExpressions;
   }

   private void init() {
      _subExpressions = new ArrayList<Expr>();
      _printer = new CollapsedComplexExprPrinter(this);
   }

   private void refreshSubExpressions() {
      _subExpressions.clear();
      _subExpressions.add(new IdExpr("not"));
      _subExpressions.add(_arg);
   }

   public void SetArgument(BooleanExpr arg) {
      _arg = arg;
      refreshSubExpressions();
   }

   @Override
   public BooleanExpr simplify() {
      BooleanExpr simplifiedArg = _arg.simplify();
      if (simplifiedArg == FalseExpr.INSTANCE) {
         return TrueExpr.INSTANCE;
      }
      else if (simplifiedArg == TrueExpr.INSTANCE) {
         return FalseExpr.INSTANCE;
      }
      else if (simplifiedArg != _arg) {
         return new NotExpr(simplifiedArg);
      }
      else {
         return this;
      }
   }

}

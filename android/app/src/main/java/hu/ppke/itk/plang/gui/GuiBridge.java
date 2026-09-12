package hu.ppke.itk.plang.gui;

import hu.ppke.itk.plang.prog.State;

import java.util.List;

/**
 * Híd az Android felület számára a {@code gui} csomag csomagvédett
 * metódusaihoz (a asztali Workbench ezekhez ugyanabból a csomagból fér hozzá).
 * Az értelmező osztályok érintetlenek maradnak.
 */
public final class GuiBridge {

    private GuiBridge() {}

    public static boolean hasError(ProgramLine line) {
        return line.hasError();
    }

    public static String error(ProgramLine line) {
        return line.getError();
    }

    public static ExprNode exprOf(ProgramLine line, State state) {
        return line.getExpr(state);
    }

    public static List<State> subStates(ExprNode node) {
        return node.getSubStates();
    }

    public static int childCount(ExprNode node) {
        return node.childNumber();
    }

    public static ExprNode child(ExprNode node, int index) {
        return node.getChild(index);
    }
}

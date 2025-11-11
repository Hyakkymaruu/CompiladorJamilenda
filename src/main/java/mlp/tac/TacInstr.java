package mlp.tac;

public class TacInstr {

    public enum Op {
        LOAD, LOADI, STORE,
        ADD, ADDI, SUB, SUBI, MUL, DIV, REM,
        CMPGT, CMPLT, CMPEQ, CMPLE, CMPGE, CMPNE,
        JMP, JMPFALSE, JMPTRUE,
        LABEL, NOP
    }

    public final Op op;
    public final String a1;
    public final String a2;
    public final String a3;
    public final String label; // usado somente para LABEL

    public TacInstr(Op op, String a1, String a2, String a3, String label) {
        this.op = op;
        this.a1 = a1;
        this.a2 = a2;
        this.a3 = a3;
        this.label = label;
    }

    // ---------- fábricas estáticas ----------

    public static TacInstr loadi(String dst, String value) {
        return new TacInstr(Op.LOADI, dst, value, null, null);
    }

    public static TacInstr load(String dst, String src) {
        return new TacInstr(Op.LOAD, dst, src, null, null);
    }

    public static TacInstr store(String var, String src) {
        return new TacInstr(Op.STORE, var, src, null, null);
    }

    public static TacInstr add(String dst, String a, String b) {
        return new TacInstr(Op.ADD, dst, a, b, null);
    }

    public static TacInstr addi(String dst, String a, String imm) {
        return new TacInstr(Op.ADDI, dst, a, imm, null);
    }

    public static TacInstr sub(String dst, String a, String b) {
        return new TacInstr(Op.SUB, dst, a, b, null);
    }

    public static TacInstr subi(String dst, String a, String imm) {
        return new TacInstr(Op.SUBI, dst, a, imm, null);
    }

    public static TacInstr mul(String dst, String a, String b) {
        return new TacInstr(Op.MUL, dst, a, b, null);
    }

    public static TacInstr div(String dst, String a, String b) {
        return new TacInstr(Op.DIV, dst, a, b, null);
    }

    public static TacInstr rem(String dst, String a, String b) {
        return new TacInstr(Op.REM, dst, a, b, null);
    }

    public static TacInstr cmplt(String dst, String a, String b) {
        return new TacInstr(Op.CMPLT, dst, a, b, null);
    }

    public static TacInstr cmpgt(String dst, String a, String b) {
        return new TacInstr(Op.CMPGT, dst, a, b, null);
    }

    public static TacInstr cmpeq(String dst, String a, String b) {
        return new TacInstr(Op.CMPEQ, dst, a, b, null);
    }

    public static TacInstr cmple(String dst, String a, String b) {
        return new TacInstr(Op.CMPLE, dst, a, b, null);
    }

    public static TacInstr cmpge(String dst, String a, String b) {
        return new TacInstr(Op.CMPGE, dst, a, b, null);
    }

    public static TacInstr cmpne(String dst, String a, String b) {
        return new TacInstr(Op.CMPNE, dst, a, b, null);
    }

    public static TacInstr jmp(String label) {
        return new TacInstr(Op.JMP, label, null, null, null);
    }

    public static TacInstr jmpFalse(String cond, String label) {
        return new TacInstr(Op.JMPFALSE, cond, label, null, null);
    }

    public static TacInstr jmpTrue(String cond, String label) {
        return new TacInstr(Op.JMPTRUE, cond, label, null, null);
    }

    public static TacInstr label(String name) {
        return new TacInstr(Op.LABEL, null, null, null, name);
    }

    public static TacInstr nop() {
        return new TacInstr(Op.NOP, null, null, null, null);
    }

    @Override
    public String toString() {
        return switch (op) {
            case LABEL -> "LABEL " + label;
            case JMP -> String.format("JMP %s", a1);
            case JMPFALSE -> String.format("JMPFALSE %s, %s", a1, a2);
            case JMPTRUE -> String.format("JMPTRUE %s, %s", a1, a2);
            case LOADI, LOAD, STORE -> {
                // 2 operandos: OP a1, a2
                yield String.format("%s %s, %s", op.name(), a1, a2);
            }
            case ADD, ADDI, SUB, SUBI, MUL, DIV, REM,
                 CMPGT, CMPLT, CMPEQ, CMPLE, CMPGE, CMPNE -> {
                // 3 operandos: OP a1, a2, a3
                yield String.format("%s %s, %s, %s", op.name(), a1, a2, a3);
            }
            case NOP -> "NOP";
        };
    }
}

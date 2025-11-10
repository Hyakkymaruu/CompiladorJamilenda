package mlp.tac;

public class TacInstr {
    public final String op;
    public final String a;
    public final String b;
    public final String c;

    public TacInstr(String op, String a, String b, String c) {
        this.op = op;
        this.a = a;
        this.b = b;
        this.c = c;
    }

    // Fábricas estáticas para facilitar leitura

    public static TacInstr loadi(String r, String value) {
        return new TacInstr("LOADI", r, value, null);
    }

    public static TacInstr load(String r, String var) {
        return new TacInstr("LOAD", r, var, null);
    }

    public static TacInstr store(String var, String r) {
        return new TacInstr("STORE", var, r, null);
    }

    public static TacInstr add(String r, String r1, String r2) {
        return new TacInstr("ADD", r, r1, r2);
    }

    public static TacInstr mul(String r, String r1, String r2) {
        return new TacInstr("MUL", r, r1, r2);
    }

    public static TacInstr div(String r, String r1, String r2) {
        return new TacInstr("DIV", r, r1, r2);
    }

    public static TacInstr resto(String r, String r1, String r2) {
        return new TacInstr("RESTO", r, r1, r2);
    }

    @Override
    public String toString() {
        // Formato bonitinho:
        //  - 1 operando:   OP A, B    (LOAD, LOADI, STORE)
        //  - 2 operandos:  OP A, B, C (ADD, MUL, etc.)
        if (c != null) {
            return op + " " + a + ", " + b + ", " + c;
        }
        if (b != null) {
            return op + " " + a + ", " + b;
        }
        return op + " " + a;
    }
}

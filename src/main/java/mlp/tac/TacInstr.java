package mlp.tac;

/**
 * Representa uma instrução de Código Intermediário (TAC)
 * no formato de até 3 endereços, por exemplo:
 *
 *   LOADI t0, 5
 *   LOAD  t1, a
 *   ADD   t2, t0, t1
 *   STORE a, t2
 *
 * Os campos r, a, b são genéricos:
 *  - Para operações aritméticas:   OP   r, a, b
 *  - Para LOAD / LOADI / STORE:    OP   r, a
 *  - Para jumps / labels (futuro): OP   r
 */
public class TacInstr {

    private final String op;  // ex: "LOADI", "ADD", "STORE"
    private final String r;   // resultado / destino
    private final String a;   // operando 1
    private final String b;   // operando 2

    public TacInstr(String op, String r, String a, String b) {
        this.op = op;
        this.r  = r;
        this.a  = a;
        this.b  = b;
    }

    public String getOp() { return op; }
    public String getR()  { return r; }
    public String getA()  { return a; }
    public String getB()  { return b; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(op);
        boolean first = true;

        if (r != null) {
            sb.append(' ').append(r);
            first = false;
        }
        if (a != null) {
            sb.append(first ? ' ' : ',').append(' ').append(a);
            first = false;
        }
        if (b != null) {
            sb.append(first ? ' ' : ',').append(' ').append(b);
        }

        return sb.toString();
    }
}

package mlp.tac;
import java.util.*;

public class InterpretadorTAC {
    Map<String, Object> memoria = new HashMap<>();

    public void executar(List<TacInstr> instrucoes) {
        int c= 0;
        
        Map<String, Object> labels = new HashMap<>();

        for (int i = 0; i < instrucoes.size(); i++) {
            String s = instrucoes.get(i).toString();
            if (s.startsWith("LABEL ")) {
                labels.put(s.substring(6).trim(), i);
            }
        }
        while (c < instrucoes.size()) {
            String instr = instrucoes.get(c).toString().trim();
            String[] parts = instr.split("\\s+", 2);
            String op = parts [0];

            switch (op) {
                case "LOADI" -> {
                    String[] args = parts[1].split(",");
                    memoria.put(args[0].trim(), Double.parseDouble(args[1].trim()));
                }
        
                case "LOAD" -> {
                    String[] args = parts[1].split(",");
                    memoria.put(args[0].trim(), memoria.get(args[1].trim()));
                }

                case "STORE" -> {
                    String[] args = parts[1].split(",");
                    memoria.put(args[0].trim(), memoria.get(args[1].trim()));
                }
            
                case "ADD" -> {
                    String[] args = parts[1].split(",");
                    double a = (double) memoria.get(args[1].trim());
                    double b = (double) memoria.get(args[2].trim());
                    memoria.put(args[0].trim(), a + b );
                }
            
            
                case "SUB" -> {
                    String[] args = parts[1].split(",");
                    double a = (double) memoria.get(args[1].trim());
                    double b = (double) memoria.get(args[2].trim());
                    memoria.put(args[0].trim(), a - b );
                }
            
            
                case "MUL" -> {
                    String[] args = parts[1].split(",");
                    double a = (double) memoria.get(args[1].trim());
                    double b = (double) memoria.get(args[2].trim());
                    memoria.put(args[0].trim(), a * b );
                }
            
            
                case "DIV" -> {
                    String[] args = parts[1].split(",");
                    double a = (double) memoria.get(args[1].trim());
                    double b = (double) memoria.get(args[2].trim());
                    memoria.put(args[0].trim(), a / b );
                }
            
            
                case "REM" -> {
                    String[] args = parts[1].split(",");
                    double a = (double) memoria.get(args[1].trim());
                    double b = (double) memoria.get(args[2].trim());
                    memoria.put(args[0].trim(), a % b );
                }
            
                case "PRINT" -> {
                    Object val = memoria.get(parts[1].trim());
                    double d = (double) val;
                    if (d == (long) d) {
                        System.out.println((long) d);
                    } else {
                        System.out.println(d);
                    }
                }

                case "LABEL" -> {}

                case "JMP" -> {
                    c = (Integer) labels.get(parts[1].trim());
                    continue;
                }

                case "JMPFALSE" -> {
                    String[] args = parts[1].split(",");
                    double cond = (double) memoria.get(args[0].trim());
                    if (cond == 0) {
                        c = (Integer) labels.get(args[1].trim());
                        continue;
                    }
                }
                case "JMPTRUE" -> {
                    String[] args = parts[1].split(",");
                    double cond = (double) memoria.get(args[0].trim());
                    if (cond != 0) {
                        c = (Integer) labels.get(args[1].trim());
                        continue;
                }
                }
                case "CMPLT", "CMPGT", "CMPEQ", "CMPLE", "CMPGE", "CMPNE" -> {
                    String[] args = parts[1].split(",");
                    double a = (double) memoria.get(args[1].trim());
                    double b = (double) memoria.get(args[2].trim());
                    boolean result = switch (op) {
                        case "CMPLT" -> a < b;
                        case "CMPGT" -> a > b;
                        case "CMPEQ" -> a == b;
                        case "CMPLE" -> a <= b;
                        case "CMPGE" -> a >= b;
                        case "CMPNE" -> a != b;
                        default -> false;
                };
                memoria.put(args[0].trim(), result ? 1.0 : 0.0);
            }
        }
        c++;
    }
}
}
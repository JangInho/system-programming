import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;


/**
 * Assembler :
 * �� ���α׷��� SIC/XE �ӽ��� ���� Assembler ���α׷��� ���� ��ƾ�̴�.
 * ���α׷��� ���� �۾��� ������ ����. <br>
 * 1) ó�� �����ϸ� Instruction ���� �о�鿩�� assembler�� �����Ѵ�. <br>
 * 2) ����ڰ� �ۼ��� input ������ �о���� �� �����Ѵ�. <br>
 * 3) input ������ ������� �ܾ�� �����ϰ� �ǹ̸� �ľ��ؼ� �����Ѵ�. (pass1) <br>
 * 4) �м��� ������ �������� ��ǻ�Ͱ� ����� �� �ִ� object code�� �����Ѵ�. (pass2) <br>
 *
 * <br><br>
 * �ۼ����� ���ǻ��� : <br>
 * 1) ���ο� Ŭ����, ���ο� ����, ���ο� �Լ� ������ �󸶵��� ����. ��, ������ ������ �Լ����� �����ϰų� ������ ��ü�ϴ� ���� �ȵȴ�.<br>
 * 2) ���������� �ۼ��� �ڵ带 �������� ������ �ʿ信 ���� ����ó��, �������̽� �Ǵ� ��� ��� ���� ����.<br>
 * 3) ��� void Ÿ���� ���ϰ��� ������ �ʿ信 ���� �ٸ� ���� Ÿ������ ���� ����.<br>
 * 4) ����, �Ǵ� �ܼ�â�� �ѱ��� ��½�Ű�� �� ��. (ä������ ����. �ּ��� ���Ե� �ѱ��� ��� ����)<br>
 *
 * <br><br>
 * + �����ϴ� ���α׷� ������ ��������� �����ϰ� ���� �е��� ������ ��� �޺κп� ÷�� �ٶ��ϴ�. ���뿡 ���� �������� ���� �� �ֽ��ϴ�.
 */
public class Assembler {
    /**
     * instruction ���� ������ ����
     */
    InstTable instTable;
    /**
     * �о���� input ������ ������ �� �� �� �����ϴ� ����.
     */
    ArrayList<String> lineList;
    /**
     * ���α׷��� section���� symbol table�� �����ϴ� ����
     */
    ArrayList<SymbolTable> symtabList;
    /**
     * ���α׷��� section���� ���α׷��� �����ϴ� ����
     */
    ArrayList<TokenTable> TokenList;
    /**
     * Token, �Ǵ� ���þ ���� ������� ������Ʈ �ڵ���� ��� ���·� �����ϴ� ����. <br>
     * �ʿ��� ��� String ��� ������ Ŭ������ �����Ͽ� ArrayList�� ��ü�ص� ������.
     */
    ArrayList<String> codeList;
    private static final int T_RECORD_MAX = 30; // �� T ���ڵ� 30����Ʈ(=60 hex)

    ArrayList<LiteralTable> literalList; // ���ͷ� ���̺��� �� ���Ǻ��� ����

    List<ExprFixup> exprList = new ArrayList<>(); // // MAXLEN EQU BUFEND?BUFFER ó���ϱ� ���� ����Ʈ


    /**
     * Ŭ���� �ʱ�ȭ. instruction Table�� �ʱ�ȭ�� ���ÿ� �����Ѵ�.
     *
     * @param instFile : instruction ���� �ۼ��� ���� �̸�.
     */
    public Assembler(String instFile) {
        instTable = new InstTable(instFile);
        lineList = new ArrayList<String>();

        symtabList = new ArrayList<SymbolTable>();
        TokenList = new ArrayList<TokenTable>();
        codeList = new ArrayList<String>();

        literalList = new ArrayList<LiteralTable>();
    }

    /**
     * ��U���� ���� ��ƾ
     */
    public static void main(String[] args) {
        Assembler assembler = new Assembler("inst_table.txt");
        assembler.loadInputFile("input.txt");

        assembler.pass1();
        assembler.printSymbolTable("output_symtab.txt");
        assembler.printLiteralTable("output_littab.txt");

        assembler.pass2();
        assembler.printObjectCode("output_objectcode.txt");

        // ������
//        for(int i =0; i<assembler.TokenList.size(); i++) {
//            for(int k =0; k<assembler.TokenList.get(i).tokenList.size(); k++) {
//                System.out.print(assembler.TokenList.get(i).tokenList.get(k).toString());
//                System.out.println();
//            }
//
//        }
    }

    /**
     * inputFile�� �о�鿩�� lineList�� �����Ѵ�.<br>
     *
     * @param inputFile : input ���� �̸�.
     */
    private void loadInputFile(String inputFile) {
        // TODO Auto-generated method stub

        try (BufferedReader br =
                     Files.newBufferedReader(Paths.get(inputFile), StandardCharsets.UTF_8)) {

            String line;
            while ((line = br.readLine()) != null) {
                if (line.endsWith("\r"))
                    line = line.substring(0, line.length() - 1);

                lineList.add(line);                 // �״�� ����
            }

        } catch (IOException e) {
            // ����� ���ܴ� ��Ÿ�� ���ܷ� ���� ������ �����ϰų�, ������ �α׸� ����� ����
            throw new RuntimeException("cannot read input file: " + inputFile, e);
        }
    }

    /**
     * �ۼ��� SymbolTable���� ������¿� �°� ����Ѵ�.<br>
     *
     * @param fileName : ����Ǵ� ���� �̸�
     */
    private void printSymbolTable(String fileName) {
        // TODO Auto-generated method stub

        // try-with-resources �� �ڵ� close
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(fileName))) {
            for (SymbolTable symtab : symtabList) {
                String name = "";
                for (int i = 0; i < symtab.symbolList.size(); i++) {

                    String symbol = symtab.symbolList.get(i);
                    int addr = symtab.locationList.get(i);

                    String line = String.format("%-8s 0x%04X %n", symbol, addr);
                    if (i == 0) {
                        name = symbol;
                    } else {
                        // EXTREF ó��
                        if (addr == -1) {
                            line = String.format("%-8s %-8s %n", symbol, "REF");
                        } else {
                            line = String.format("%-8s 0x%04X %-8s %n", symbol, addr, symtab.nameList.get(i));
                        }
                    }

                    bw.write(line);
                }
                bw.newLine();  // ���� ���� �� ��
            }
        } catch (IOException e) {
            // ����� ���� ó��
            System.err.println("Fail print symbol table: " + e.getMessage());
        }

    }

    /**
     * �ۼ��� LiteralTable���� ������¿� �°� ����Ѵ�.<br>
     *
     * @param fileName : ����Ǵ� ���� �̸�
     */
    private void printLiteralTable(String fileName) {
        // TODO Auto-generated method stub

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(fileName))) {
            // literalList: List<LiteralTable> Ŭ���� �ʵ��� ����
            for (LiteralTable lt : literalList) {
                for (int i = 0; i < lt.literalList.size(); i++) {
                    String lit  = lt.literalList.get(i);
                    int    addr = lt.locationList.get(i);
                    // %-8s : ���� ���� �� 8, 0x%04X : 16���� 4�ڸ�
                    bw.write(String.format("%-8s 0x%04X%n", lit, addr));
                }
                bw.newLine();  // ���� ���� �� ��
            }
        } catch (IOException e) {
            System.err.println("Fail print literal table: " + e.getMessage());
        }

    }

    /**
     * pass1 ������ �����Ѵ�.<br>
     * 1) ���α׷� �ҽ��� ��ĵ�Ͽ� ��ū������ �и��� �� ��ū���̺� ����<br>
     * 2) label�� symbolTable�� ����<br>
     * <br><br>
     * ���ǻ��� : SymbolTable�� TokenTable�� ���α׷��� section���� �ϳ��� ����Ǿ�� �Ѵ�.
     */
    private void pass1() {
        // TODO Auto-generated method stub

        /// START ����
        int csectIndex = 0;
        symtabList.add(new SymbolTable());
        literalList.add(new LiteralTable());
        TokenList.add(new TokenTable(symtabList.get(csectIndex), instTable, literalList.get(csectIndex)));
        int locctr = 0;

        for (String src : lineList) {

            // �ּ� ó��
            if (src.startsWith(".")) continue;

            Token token = new Token(src, instTable, 0);

            // ���� CSECT ��� ������
            if (token.operator.equals("CSECT")) {
                locctr = 0;

                // ù��° �ι�° control section EXTREF ó��
                String[] ref = TokenList.get(csectIndex).findTokenOperand("EXTREF");
                for (String r : ref) {
                    TokenList.get(csectIndex).symTab.putSymbol(r, -1, TokenList.get(csectIndex).tokenList.get(0).label);
                }

                // CSECT ���ο� control section
                csectIndex++;
                symtabList.add(new SymbolTable());
                literalList.add(new LiteralTable());
                TokenList.add(new TokenTable(symtabList.get(csectIndex), instTable, literalList.get(csectIndex)));
            }

            // ���ͷ� ���̺� ���ͷ��� �ֱ�
            String op0 = token.operand[0];
            if (op0 != null && (op0.startsWith("=C") || op0.startsWith("=X"))) {
                literalList.get(csectIndex).putLiteral(op0, -1);
            }

            TokenList.get(csectIndex).putToken(src, locctr);


            if (token.label != null) {
                boolean isFixup = "EQU".equalsIgnoreCase(token.operator) &&
                        token.operand[0] != null &&
                        (token.operand[0].contains("+") || token.operand[0].contains("-"));

                // 1) �ɺ��� �׻� ���(���� locctr)
                TokenList.get(csectIndex).symTab.putSymbol(token.label, locctr, isFixup ? "" : TokenList.get(csectIndex).tokenList.get(0).label);

                // 2) EQU �� ǥ�����̸� fixup ��Ͽ� �߰�
                if (isFixup) {

                    String expr = token.operand[0];
                    char op = expr.contains("+") ? '+' : '-';
                    String[] p  = expr.split("[+-]");
                    exprList.add(new ExprFixup(token.label, p[0], op, p[1], csectIndex));
                }
            }



            /* byteSize ���  */
            int inc = calcByteSize(token);
            locctr += inc;

            // ���ͷ� Ǯ ����
            if (token.operator.equals("LTORG") || token.operator.equals("END")) {
                for (int i = 0; i < literalList.get(csectIndex).literalList.size(); i++) {

                    String literal = literalList.get(csectIndex).literalList.get(i);
                    String line = "   " + literal + "   " + literal;
                    TokenList.get(csectIndex).putToken(line, locctr);

                    // ���ͷ� �ּ� ����
                    literalList.get(csectIndex).modifyLiteral(literal, locctr);

                    int byteSize = 0;
                    if (literal.startsWith("=C"))
                        byteSize = literal.length() - 4;     // C'EOF' �� 3
                    if (literal.startsWith("=X"))
                        byteSize = (literal.length() - 4) / 2; // X'F1'  �� 1

                    locctr += byteSize;
                }

            }

        }

        // MAXLEN EQU BUFEND BUFFER ó��
        for (ExprFixup fx : exprList) {
            SymbolTable st = symtabList.get(fx.csect);

            int v1 = st.searchSymbol(fx.left);
            int v2 = st.searchSymbol(fx.right);

            int value = (fx.op == '+') ? v1 + v2 : v1 - v2;
            st.modifySymbol(fx.label, value);
        }

        // ����° control section EXTREF ó��
        String[] ref = TokenList.get(csectIndex).findTokenOperand("EXTREF");
        for (String r : ref) {
            TokenList.get(csectIndex).symTab.putSymbol(r, -1, TokenList.get(csectIndex).tokenList.get(0).label);
        }

    }

    private int calcByteSize(Token t) {
        /* 1) ��ɾ� */
        if (instTable.isExist(t.operator)) {
            Instruction inst = instTable.find(t.operator);

            return switch (inst.format) {
                case 2 -> 2;
                case 3 -> 3;
                case 4 -> 4;
                default -> 0;
            };
        }

        /* 2) ���þ� */
        switch (t.operator.toUpperCase()) {
            case "WORD":
                return 3;
            case "BYTE":
                if (t.operand[0].startsWith("C'"))
                    return t.operand[0].length() - 3;     // C'EOF' �� 3
                if (t.operand[0].startsWith("X'"))
                    return (t.operand[0].length() - 3) / 2; // X'F1'  �� 1
                return 1;
            case "RESB":
                return Integer.parseInt(t.operand[0]);
            case "RESW":
                return 3 * Integer.parseInt(t.operand[0]);
            default:
                return 0;
        }
    }


    /**
     * pass2 ������ �����Ѵ�.<br>
     * 1) �м��� ������ �������� object code�� �����Ͽ� codeList�� ����.
     */
    private void pass2() {
        // TODO Auto-generated method stub

        codeList.clear();

        /* 1) ��� ��ū objectCode �ۼ� */
        for (TokenTable tt : TokenList)
            for (int i = 0; i < tt.tokenList.size(); i++)
                tt.makeObjectCode(i);

        /* 2) section ���� ���ڵ� ���� */
        for (int sec = 0; sec < TokenList.size(); sec++) {

            TokenTable tt = TokenList.get(sec);

            /* ���� H ���ڵ� ������������������������������������������ */
            String prog = pad6(tt.symTab.symbolList.get(0));
            int len = sectionLength(tt);
            codeList.add(String.format("H%s%06X%06X", prog, 0, len));

            /* ���� D / R ���ڵ� (EXTDEF / EXTREF ��) ���� */
            makeDefineReferFromDirective(tt);

            /* ���� T / M ���ڵ� ���������������������������������������� */
            makeTextAndMod(tt);

            /* ���� E ���ڵ� ���������������������������������������������� */
            if (sec == 0) {
                codeList.add("E000000");
            } else {
                codeList.add("E");
            }

        }

    }

    /**
     * �ۼ��� codeList�� ������¿� �°� ����Ѵ�.<br>
     *
     * @param fileName : ����Ǵ� ���� �̸�
     */
    private void printObjectCode(String fileName) {
        // TODO Auto-generated method stub

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(fileName))) {
            for (String rec : codeList) {
                bw.write(rec);
                bw.newLine();
            }
        } catch (IOException e) {
            System.err.println("Fail print object code: " + e.getMessage());
        }

    }

    /**
    * TokenTable ���� EXTDEF / EXTREF ���þ ���
    * */
    private void makeDefineReferFromDirective(TokenTable tt) {

        StringBuilder d = new StringBuilder("D");
        StringBuilder r = new StringBuilder("R");

        for (Token t : tt.tokenList) {
            if ("EXTDEF".equalsIgnoreCase(t.operator)) {
                for (String sym : t.operand) {
                    if (sym == null) break;
                    int addr = tt.symTab.searchSymbol(sym);
                    d.append(sym).append(String.format("%06X", addr));
                }
            } else if ("EXTREF".equalsIgnoreCase(t.operator)) {
                for (String sym : t.operand) {
                    if (sym == null) break;
                    r.append(sym);
                }
            }
        }
        if (d.length() > 1) codeList.add(d.toString());
        if (r.length() > 1) codeList.add(r.toString());
    }

    private void makeTextAndMod(TokenTable tt) {

        StringBuilder buf = new StringBuilder();
        int textStart = -1, byteCnt = 0;
        List<String>  mList = new ArrayList<>();

        for (Token t : tt.tokenList) {

            String obj = (t.objectCode == null) ? "" : t.objectCode;
            int    len = obj.length() / 2;                 // byte ��
            boolean hasObj = len > 0;

        /* ���� flush �ʿ� ���� ����������������������������������������������������
           1) �̹� ��ū�� ��� �ְ�,
              a) �� ���ڵ� ���� ����  -> X
              b) �ּ� �ҿ���          -> flush
              c) 30����Ʈ �ʰ�        -> flush
        */
            if (hasObj) {
                boolean needFlush =
                        (textStart < 0) ? false :
                                (t.location != textStart + byteCnt) ||
                                        (byteCnt + len > T_RECORD_MAX);

                if (needFlush) {
                    flushText(buf, textStart, byteCnt);
                    buf.setLength(0);  byteCnt = 0;  textStart = -1;
                }
            }

            /* ���� object code ���̱� ������������������������������������������ */
            if (hasObj) {
                if (textStart < 0) textStart = t.location;
                buf.append(obj);  byteCnt += len;

                /* format?4 �� M ���ڵ� �ĺ� ���� */
                if (t.getFlag(TokenTable.eFlag) != 0) {
                    String sym = extractModifySymbol(t);
                    mList.add(String.format("M%06X05+%s",
                            t.location + 1, sym));
                } else if ("WORD".equalsIgnoreCase(t.operator) &&
                        (t.operand[0].contains("+") || t.operand[0].contains("-"))) {

                    String expr = t.operand[0];          // "BUFEND-BUFFER"
                    int    addr = t.location;            // WORD ���� �ּ�

                    /* �� ��ȣ�� �ɺ� �и� */
                    List<Character> signs = new ArrayList<>();
                    List<String> syms     = new ArrayList<>();

                    StringBuilder cur = new StringBuilder();
                    signs.add('+');                       // ù ���� '+' �� ����
                    for (char ch : expr.toCharArray()) {
                        if (ch=='+' || ch=='-') {
                            syms.add(cur.toString());
                            cur.setLength(0);
                            signs.add(ch);
                        } else cur.append(ch);
                    }
                    syms.add(cur.toString());

                    /* �� �� �ɺ��� M ���ڵ� ���� = 06 half?bytes (3����Ʈ) */
                    for (int i = 0; i < syms.size(); i++) {
                        String s  = syms.get(i);
                        char   op = signs.get(i);
                        mList.add(String.format("M%06X06%c%s", addr, op, s));
                    }
                }

            }
        }

        flushText(buf, textStart, byteCnt);   // ������ T
        codeList.addAll(mList);               // T ���ڵ� �ڿ� M ���ڵ� �ϰ� �߰�
    }

    /* section �� ���� ��� */
    private int sectionLength(TokenTable tt) {
        Token last = tt.tokenList.get(tt.tokenList.size() - 1);
        return last.location + last.byteSize;
    }

    /* 6byte �е� */
    private String pad6(String s) { return String.format("%-6s", s).substring(0, 6); }

    /* D / R ���ڵ� �ۼ� */
    private void makeDefineRefer(SymbolTable st) {
        StringBuilder d = new StringBuilder("D");
        StringBuilder r = new StringBuilder("R");
        for (int i = 0; i < st.symbolList.size(); i++) {
            String sym = st.symbolList.get(i);
            int    loc = st.locationList.get(i);
            if (i == 0) continue;                 // section �̸�
            if (loc >= 0) d.append(String.format("%-6s%06X", sym, loc));   // EXTDEF
            else          r.append(String.format("%-6s", sym));            // EXTREF
        }
        if (d.length() > 1) codeList.add(d.toString());
        if (r.length() > 1) codeList.add(r.toString());
    }


    /* T ���ڵ� flush */
    private void flushText(StringBuilder buf, int start, int len) {
        if (len == 0) return;
        codeList.add(String.format("T%06X%02X%s", start, len, buf));
    }

    /* M ���ڵ忡 �� �ɺ� �̸� */
    private String extractModifySymbol(Token t) {
        if (t.operand[0] == null) return "000000";
        String op = t.operand[0];
        if (op.startsWith("="))   return "000000";           // ���ͷ� ����
        return op.replaceFirst("^[@#]", "");                 // #,@ ����
    }
}


// MAXLEN EQU BUFEND BUFFER ó���ϱ� ���� Ŭ����
class ExprFixup {
    String label;   // MAXLEN
    String left;    // BUFEND
    String right;   // BUFFER
    char   op;      // '+' �Ǵ� '-'
    int    csect;   // ���� �ε���
    ExprFixup(String l,String a,char o,String b,int cs){
        label=l; left=a; op=o; right=b; csect=cs;
    }
}
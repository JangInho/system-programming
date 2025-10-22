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
 * 이 프로그램은 SIC/XE 머신을 위한 Assembler 프로그램의 메인 루틴이다.
 * 프로그램의 수행 작업은 다음과 같다. <br>
 * 1) 처음 시작하면 Instruction 명세를 읽어들여서 assembler를 세팅한다. <br>
 * 2) 사용자가 작성한 input 파일을 읽어들인 후 저장한다. <br>
 * 3) input 파일의 문장들을 단어별로 분할하고 의미를 파악해서 정리한다. (pass1) <br>
 * 4) 분석된 내용을 바탕으로 컴퓨터가 사용할 수 있는 object code를 생성한다. (pass2) <br>
 *
 * <br><br>
 * 작성중의 유의사항 : <br>
 * 1) 새로운 클래스, 새로운 변수, 새로운 함수 선언은 얼마든지 허용됨. 단, 기존의 변수와 함수들을 삭제하거나 완전히 대체하는 것은 안된다.<br>
 * 2) 마찬가지로 작성된 코드를 삭제하지 않으면 필요에 따라 예외처리, 인터페이스 또는 상속 사용 또한 허용됨.<br>
 * 3) 모든 void 타입의 리턴값은 유저의 필요에 따라 다른 리턴 타입으로 변경 가능.<br>
 * 4) 파일, 또는 콘솔창에 한글을 출력시키지 말 것. (채점상의 이유. 주석에 포함된 한글은 상관 없음)<br>
 *
 * <br><br>
 * + 제공하는 프로그램 구조의 개선방법을 제안하고 싶은 분들은 보고서의 결론 뒷부분에 첨부 바랍니다. 내용에 따라 가산점이 있을 수 있습니다.
 */
public class Assembler {
    /**
     * instruction 명세를 저장한 공간
     */
    InstTable instTable;
    /**
     * 읽어들인 input 파일의 내용을 한 줄 씩 저장하는 공간.
     */
    ArrayList<String> lineList;
    /**
     * 프로그램의 section별로 symbol table을 저장하는 공간
     */
    ArrayList<SymbolTable> symtabList;
    /**
     * 프로그램의 section별로 프로그램을 저장하는 공간
     */
    ArrayList<TokenTable> TokenList;
    /**
     * Token, 또는 지시어에 따라 만들어진 오브젝트 코드들을 출력 형태로 저장하는 공간. <br>
     * 필요한 경우 String 대신 별도의 클래스를 선언하여 ArrayList를 교체해도 무방함.
     */
    ArrayList<String> codeList;
    private static final int T_RECORD_MAX = 30; // 한 T 레코드 30바이트(=60 hex)

    ArrayList<LiteralTable> literalList; // 리터럴 테이블을 각 섹션별로 저장

    List<ExprFixup> exprList = new ArrayList<>(); // // MAXLEN EQU BUFEND?BUFFER 처리하기 위한 리스트


    /**
     * 클래스 초기화. instruction Table을 초기화와 동시에 세팅한다.
     *
     * @param instFile : instruction 명세를 작성한 파일 이름.
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
     * 어셐블러의 메인 루틴
     */
    public static void main(String[] args) {
        Assembler assembler = new Assembler("inst_table.txt");
        assembler.loadInputFile("input.txt");

        assembler.pass1();
        assembler.printSymbolTable("output_symtab.txt");
        assembler.printLiteralTable("output_littab.txt");

        assembler.pass2();
        assembler.printObjectCode("output_objectcode.txt");

        // 디버깅용
//        for(int i =0; i<assembler.TokenList.size(); i++) {
//            for(int k =0; k<assembler.TokenList.get(i).tokenList.size(); k++) {
//                System.out.print(assembler.TokenList.get(i).tokenList.get(k).toString());
//                System.out.println();
//            }
//
//        }
    }

    /**
     * inputFile을 읽어들여서 lineList에 저장한다.<br>
     *
     * @param inputFile : input 파일 이름.
     */
    private void loadInputFile(String inputFile) {
        // TODO Auto-generated method stub

        try (BufferedReader br =
                     Files.newBufferedReader(Paths.get(inputFile), StandardCharsets.UTF_8)) {

            String line;
            while ((line = br.readLine()) != null) {
                if (line.endsWith("\r"))
                    line = line.substring(0, line.length() - 1);

                lineList.add(line);                 // 그대로 보존
            }

        } catch (IOException e) {
            // 입출력 예외는 런타임 예외로 감싸 상위로 전달하거나, 적당히 로그만 남기고 종료
            throw new RuntimeException("cannot read input file: " + inputFile, e);
        }
    }

    /**
     * 작성된 SymbolTable들을 출력형태에 맞게 출력한다.<br>
     *
     * @param fileName : 저장되는 파일 이름
     */
    private void printSymbolTable(String fileName) {
        // TODO Auto-generated method stub

        // try-with-resources 로 자동 close
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
                        // EXTREF 처리
                        if (addr == -1) {
                            line = String.format("%-8s %-8s %n", symbol, "REF");
                        } else {
                            line = String.format("%-8s 0x%04X %-8s %n", symbol, addr, symtab.nameList.get(i));
                        }
                    }

                    bw.write(line);
                }
                bw.newLine();  // 섹션 구분 빈 줄
            }
        } catch (IOException e) {
            // 입출력 오류 처리
            System.err.println("Fail print symbol table: " + e.getMessage());
        }

    }

    /**
     * 작성된 LiteralTable들을 출력형태에 맞게 출력한다.<br>
     *
     * @param fileName : 저장되는 파일 이름
     */
    private void printLiteralTable(String fileName) {
        // TODO Auto-generated method stub

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(fileName))) {
            // literalList: List<LiteralTable> 클래스 필드라고 가정
            for (LiteralTable lt : literalList) {
                for (int i = 0; i < lt.literalList.size(); i++) {
                    String lit  = lt.literalList.get(i);
                    int    addr = lt.locationList.get(i);
                    // %-8s : 좌측 정렬 폭 8, 0x%04X : 16진수 4자리
                    bw.write(String.format("%-8s 0x%04X%n", lit, addr));
                }
                bw.newLine();  // 섹션 구분 빈 줄
            }
        } catch (IOException e) {
            System.err.println("Fail print literal table: " + e.getMessage());
        }

    }

    /**
     * pass1 과정을 수행한다.<br>
     * 1) 프로그램 소스를 스캔하여 토큰단위로 분리한 뒤 토큰테이블 생성<br>
     * 2) label을 symbolTable에 정리<br>
     * <br><br>
     * 주의사항 : SymbolTable과 TokenTable은 프로그램의 section별로 하나씩 선언되어야 한다.
     */
    private void pass1() {
        // TODO Auto-generated method stub

        /// START 세팅
        int csectIndex = 0;
        symtabList.add(new SymbolTable());
        literalList.add(new LiteralTable());
        TokenList.add(new TokenTable(symtabList.get(csectIndex), instTable, literalList.get(csectIndex)));
        int locctr = 0;

        for (String src : lineList) {

            // 주석 처리
            if (src.startsWith(".")) continue;

            Token token = new Token(src, instTable, 0);

            // 만약 CSECT 라면 나누기
            if (token.operator.equals("CSECT")) {
                locctr = 0;

                // 첫번째 두번째 control section EXTREF 처리
                String[] ref = TokenList.get(csectIndex).findTokenOperand("EXTREF");
                for (String r : ref) {
                    TokenList.get(csectIndex).symTab.putSymbol(r, -1, TokenList.get(csectIndex).tokenList.get(0).label);
                }

                // CSECT 새로운 control section
                csectIndex++;
                symtabList.add(new SymbolTable());
                literalList.add(new LiteralTable());
                TokenList.add(new TokenTable(symtabList.get(csectIndex), instTable, literalList.get(csectIndex)));
            }

            // 리터럴 테이블에 리터럴만 넣기
            String op0 = token.operand[0];
            if (op0 != null && (op0.startsWith("=C") || op0.startsWith("=X"))) {
                literalList.get(csectIndex).putLiteral(op0, -1);
            }

            TokenList.get(csectIndex).putToken(src, locctr);


            if (token.label != null) {
                boolean isFixup = "EQU".equalsIgnoreCase(token.operator) &&
                        token.operand[0] != null &&
                        (token.operand[0].contains("+") || token.operand[0].contains("-"));

                // 1) 심볼은 항상 등록(값은 locctr)
                TokenList.get(csectIndex).symTab.putSymbol(token.label, locctr, isFixup ? "" : TokenList.get(csectIndex).tokenList.get(0).label);

                // 2) EQU ± 표현식이면 fixup 목록에 추가
                if (isFixup) {

                    String expr = token.operand[0];
                    char op = expr.contains("+") ? '+' : '-';
                    String[] p  = expr.split("[+-]");
                    exprList.add(new ExprFixup(token.label, p[0], op, p[1], csectIndex));
                }
            }



            /* byteSize 계산  */
            int inc = calcByteSize(token);
            locctr += inc;

            // 리터럴 풀 관리
            if (token.operator.equals("LTORG") || token.operator.equals("END")) {
                for (int i = 0; i < literalList.get(csectIndex).literalList.size(); i++) {

                    String literal = literalList.get(csectIndex).literalList.get(i);
                    String line = "   " + literal + "   " + literal;
                    TokenList.get(csectIndex).putToken(line, locctr);

                    // 리터럴 주소 갱신
                    literalList.get(csectIndex).modifyLiteral(literal, locctr);

                    int byteSize = 0;
                    if (literal.startsWith("=C"))
                        byteSize = literal.length() - 4;     // C'EOF' → 3
                    if (literal.startsWith("=X"))
                        byteSize = (literal.length() - 4) / 2; // X'F1'  → 1

                    locctr += byteSize;
                }

            }

        }

        // MAXLEN EQU BUFEND BUFFER 처리
        for (ExprFixup fx : exprList) {
            SymbolTable st = symtabList.get(fx.csect);

            int v1 = st.searchSymbol(fx.left);
            int v2 = st.searchSymbol(fx.right);

            int value = (fx.op == '+') ? v1 + v2 : v1 - v2;
            st.modifySymbol(fx.label, value);
        }

        // 세번째 control section EXTREF 처리
        String[] ref = TokenList.get(csectIndex).findTokenOperand("EXTREF");
        for (String r : ref) {
            TokenList.get(csectIndex).symTab.putSymbol(r, -1, TokenList.get(csectIndex).tokenList.get(0).label);
        }

    }

    private int calcByteSize(Token t) {
        /* 1) 명령어 */
        if (instTable.isExist(t.operator)) {
            Instruction inst = instTable.find(t.operator);

            return switch (inst.format) {
                case 2 -> 2;
                case 3 -> 3;
                case 4 -> 4;
                default -> 0;
            };
        }

        /* 2) 지시어 */
        switch (t.operator.toUpperCase()) {
            case "WORD":
                return 3;
            case "BYTE":
                if (t.operand[0].startsWith("C'"))
                    return t.operand[0].length() - 3;     // C'EOF' → 3
                if (t.operand[0].startsWith("X'"))
                    return (t.operand[0].length() - 3) / 2; // X'F1'  → 1
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
     * pass2 과정을 수행한다.<br>
     * 1) 분석된 내용을 바탕으로 object code를 생성하여 codeList에 저장.
     */
    private void pass2() {
        // TODO Auto-generated method stub

        codeList.clear();

        /* 1) 모든 토큰 objectCode 작성 */
        for (TokenTable tt : TokenList)
            for (int i = 0; i < tt.tokenList.size(); i++)
                tt.makeObjectCode(i);

        /* 2) section 단위 레코드 구성 */
        for (int sec = 0; sec < TokenList.size(); sec++) {

            TokenTable tt = TokenList.get(sec);

            /* ── H 레코드 ───────────────────── */
            String prog = pad6(tt.symTab.symbolList.get(0));
            int len = sectionLength(tt);
            codeList.add(String.format("H%s%06X%06X", prog, 0, len));

            /* ── D / R 레코드 (EXTDEF / EXTREF 만) ── */
            makeDefineReferFromDirective(tt);

            /* ── T / M 레코드 ──────────────────── */
            makeTextAndMod(tt);

            /* ── E 레코드 ─────────────────────── */
            if (sec == 0) {
                codeList.add("E000000");
            } else {
                codeList.add("E");
            }

        }

    }

    /**
     * 작성된 codeList를 출력형태에 맞게 출력한다.<br>
     *
     * @param fileName : 저장되는 파일 이름
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
    * TokenTable 안의 EXTDEF / EXTREF 지시어만 사용
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
            int    len = obj.length() / 2;                 // byte 수
            boolean hasObj = len > 0;

        /* ── flush 필요 조건 ──────────────────────────
           1) 이번 토큰에 기계어가 있고,
              a) 새 레코드 아직 없음  -> X
              b) 주소 불연속          -> flush
              c) 30바이트 초과        -> flush
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

            /* ── object code 붙이기 ───────────────────── */
            if (hasObj) {
                if (textStart < 0) textStart = t.location;
                buf.append(obj);  byteCnt += len;

                /* format?4 → M 레코드 후보 저장 */
                if (t.getFlag(TokenTable.eFlag) != 0) {
                    String sym = extractModifySymbol(t);
                    mList.add(String.format("M%06X05+%s",
                            t.location + 1, sym));
                } else if ("WORD".equalsIgnoreCase(t.operator) &&
                        (t.operand[0].contains("+") || t.operand[0].contains("-"))) {

                    String expr = t.operand[0];          // "BUFEND-BUFFER"
                    int    addr = t.location;            // WORD 시작 주소

                    /* ① 기호와 심볼 분리 */
                    List<Character> signs = new ArrayList<>();
                    List<String> syms     = new ArrayList<>();

                    StringBuilder cur = new StringBuilder();
                    signs.add('+');                       // 첫 항은 '+' 로 간주
                    for (char ch : expr.toCharArray()) {
                        if (ch=='+' || ch=='-') {
                            syms.add(cur.toString());
                            cur.setLength(0);
                            signs.add(ch);
                        } else cur.append(ch);
                    }
                    syms.add(cur.toString());

                    /* ② 각 심볼별 M 레코드 길이 = 06 half?bytes (3바이트) */
                    for (int i = 0; i < syms.size(); i++) {
                        String s  = syms.get(i);
                        char   op = signs.get(i);
                        mList.add(String.format("M%06X06%c%s", addr, op, s));
                    }
                }

            }
        }

        flushText(buf, textStart, byteCnt);   // 마지막 T
        codeList.addAll(mList);               // T 레코드 뒤에 M 레코드 일괄 추가
    }

    /* section 총 길이 계산 */
    private int sectionLength(TokenTable tt) {
        Token last = tt.tokenList.get(tt.tokenList.size() - 1);
        return last.location + last.byteSize;
    }

    /* 6byte 패딩 */
    private String pad6(String s) { return String.format("%-6s", s).substring(0, 6); }

    /* D / R 레코드 작성 */
    private void makeDefineRefer(SymbolTable st) {
        StringBuilder d = new StringBuilder("D");
        StringBuilder r = new StringBuilder("R");
        for (int i = 0; i < st.symbolList.size(); i++) {
            String sym = st.symbolList.get(i);
            int    loc = st.locationList.get(i);
            if (i == 0) continue;                 // section 이름
            if (loc >= 0) d.append(String.format("%-6s%06X", sym, loc));   // EXTDEF
            else          r.append(String.format("%-6s", sym));            // EXTREF
        }
        if (d.length() > 1) codeList.add(d.toString());
        if (r.length() > 1) codeList.add(r.toString());
    }


    /* T 레코드 flush */
    private void flushText(StringBuilder buf, int start, int len) {
        if (len == 0) return;
        codeList.add(String.format("T%06X%02X%s", start, len, buf));
    }

    /* M 레코드에 들어갈 심볼 이름 */
    private String extractModifySymbol(Token t) {
        if (t.operand[0] == null) return "000000";
        String op = t.operand[0];
        if (op.startsWith("="))   return "000000";           // 리터럴 제외
        return op.replaceFirst("^[@#]", "");                 // #,@ 제거
    }
}


// MAXLEN EQU BUFEND BUFFER 처리하기 위한 클래스
class ExprFixup {
    String label;   // MAXLEN
    String left;    // BUFEND
    String right;   // BUFFER
    char   op;      // '+' 또는 '-'
    int    csect;   // 섹션 인덱스
    ExprFixup(String l,String a,char o,String b,int cs){
        label=l; left=a; op=o; right=b; csect=cs;
    }
}
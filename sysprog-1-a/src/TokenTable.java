import java.util.ArrayList;

/**
 * 사용자가 작성한 프로그램 코드를 단어별로 분할 한 후, 의미를 분석하고, 최종 코드로 변환하는 과정을 총괄하는 클래스이다. <br>
 * pass2에서 object code로 변환하는 과정은 혼자 해결할 수 없고 symbolTable과 instTable의 정보가 필요하므로 이를 링크시킨다.<br>
 * section 마다 인스턴스가 하나씩 할당된다.
 *
 */
public class TokenTable {
	public static final int MAX_OPERAND=3;
	
	/* bit 조작의 가독성을 위한 선언 */
	public static final int nFlag=32;
	public static final int iFlag=16;
	public static final int xFlag=8;
	public static final int bFlag=4;
	public static final int pFlag=2;
	public static final int eFlag=1;

	/* Token을 다룰 때 필요한 테이블들을 링크시킨다. */
	SymbolTable symTab;
	InstTable instTab;

	LiteralTable literalTab;
	
	
	/** 각 line을 의미별로 분할하고 분석하는 공간. */
	ArrayList<Token> tokenList;
	
	/**
	 * 초기화하면서 symTable과 instTable을 링크시킨다.
	 * @param symTab : 해당 section과 연결되어있는 symbol table
	 * @param instTab : instruction 명세가 정의된 instTable
	 */
	public TokenTable(SymbolTable symTab, InstTable instTab, LiteralTable literalTab) {
		//...
		this.symTab = symTab;
		this.instTab = instTab;
		this.literalTab = literalTab;
		this.tokenList = new ArrayList<>();
	}
	
	/**
	 * 일반 문자열을 받아서 Token단위로 분리시켜 tokenList에 추가한다.
	 * @param line : 분리되지 않은 일반 문자열
	 */
	public void putToken(String line, int locctr) {
		tokenList.add(new Token(line, instTab, locctr));
	}
	
	/**
	 * tokenList에서 index에 해당하는 Token을 리턴한다.
	 * @param index
	 * @return : index번호에 해당하는 코드를 분석한 Token 클래스
	 */
	public Token getToken(int index) {
		return tokenList.get(index);
	}
	
	/**
	 * Pass2 과정에서 사용한다.
	 * instruction table, symbol table 등을 참조하여 objectcode를 생성하고, 이를 저장한다.
	 * @param index
	 */
	public void makeObjectCode(int index){

		Token t = tokenList.get(index);

		/* ─── 1) 명령어인지 확인 ────────────────── */
		if (instTab.isExist(t.operator)) {
			encodeInstruction(t);
			return;
		}

		/* ─── 2) BYTE / WORD ───────────────────── */
		if ("BYTE".equalsIgnoreCase(t.operator)) {
			t.objectCode = encodeByte(t.operand[0]);  // C'EOF' → 454F46
			t.byteSize   = t.objectCode.length() / 2; // 바이트 수
			return;
		}

		/* ─── 2?b) 리터럴 (=C'…' / =X'…') ─────────────── */
		if (t.operator.startsWith("=")) {            // ex) "=C'EOF'"
			String litBody = t.operator.substring(1); // 'C'EOF'' 또는 'X'F1''
			t.objectCode = encodeByte(litBody);
			t.byteSize   = t.objectCode.length() / 2; // 길이(바이트)
			return;
		}

		if ("WORD".equalsIgnoreCase(t.operator)) {

			/* 심볼 연산 여부 */
			if (t.operand[0].contains("+") || t.operand[0].contains("-")) {
				/* 아직 값을 계산하지 않으므로 0 으로 채움 */
				t.objectCode = "000000";
				t.byteSize   = 3;
				return;                  // ★ 여기서 끝!
			}

			/* 순수 상수라면 그대로 3바이트 */
			int val = Integer.parseInt(t.operand[0]);
			t.objectCode = String.format("%06X", val & 0xFFFFFF);
			t.byteSize   = 3;
			return;
		}

		/* ─── 3) RESB / RESW / LTORG / BASE 등 ─── */
		t.objectCode = "";           // object code 없음
	}

	private void encodeInstruction(Token t) {
		Instruction inst = instTab.find(t.operator);

		/* A. 형식 1, 2 는 간단 */
		if (inst.format == 1) {
			t.objectCode = String.format("%02X", inst.opcode);
			t.byteSize = 1;
			return;
		}
		if (inst.format == 2) {
			int r1 = regNum(t.operand[0]);
			int r2 = (inst.numberOfOperand == 2) ? regNum(t.operand[1]) : 0;
			int code = (inst.opcode << 8) | (r1 << 4) | r2;
			t.objectCode = String.format("%04X", code);
			t.byteSize = 2;
			return;
		}


		/* ── RSUB 전용 ───────────────────────── */
		if ("RSUB".equalsIgnoreCase(t.operator)) {
			t.setFlag(TokenTable.nFlag, 1);   // n = 1
			t.setFlag(TokenTable.iFlag, 1);   // i = 1
			int firstByte = (inst.opcode & 0xFC) | 0x03; // opcode 상위6bit + ni(11)
			int code = firstByte << 16;                  // xbpe = 0000, disp = 000
			t.objectCode = String.format("%06X", code);  // → 4F0000
			t.byteSize   = 3;
			return;
		}

		/* 3 / 4 형식 ───────────────────────────────────────── */

		/* 1) n,i flag 결정 (#,@,혹은 기본) */
		setNiFlags(t);  // (# → i , @ → n , 나머지 ni=1)

		/* 2) x flag 는 pass1에서 이미 setFlag(xFlag,1) */

		/* 3) 즉시 숫자 여부 선판단 (#123 등) */
		String op0 = t.operand[0];
		boolean immNumeric = false;
		int     immValue   = 0;
		if (op0 != null && op0.startsWith("#") &&
				op0.length() > 1 && Character.isDigit(op0.charAt(1))) {
			immNumeric = true;
			immValue   = Integer.parseInt(op0.substring(1));
		}

		/* 4) target 계산 */
		int target = immNumeric ? immValue           // 즉시 상수면 값 자체
				: calcTargetAddr(t); // 심볼·리터럴 주소

		/* 5) disp/addr + b,p,e 결정 */
		boolean isFmt4 = t.getFlag(TokenTable.eFlag) != 0;
		int instrLen   = isFmt4 ? 4 : 3;
		int disp20;

		if (immNumeric) {                            // 즉시 상수
			if (!isFmt4 && immValue <= 0xFFF) {      // Fmt3에 들어감
				disp20 = immValue;
				// b,p 는 기본 0 그대로
			} else {                                 // 값이 크면 format4 강제
				t.setFlag(TokenTable.eFlag, 1);
				isFmt4 = true;
				disp20 = immValue & 0xFFFFF;
			}
		} else {                                     // 심볼 / 리터럴
			if (!isFmt4) {
				int next = t.location + instrLen;
				int disp = target - next;
				if (disp >= -2048 && disp <= 2047) { // PC?relative
					t.setFlag(TokenTable.pFlag, 1);
					disp20 = disp & 0xFFF;
				} else {                             // BASE?relative
					int base = symTab.searchSymbol("BASE");
					disp = target - base;
					t.setFlag(TokenTable.bFlag, 1);
					disp20 = disp & 0xFFFFF;
				}
			} else {                                 // format 4
				disp20 = target & 0xFFFFF;
			}
		}

		/* 6) opcode 상위 6bit + ni 2bit */
		int op6 = inst.opcode & 0xFC;
		int ni  = (t.getFlag(TokenTable.nFlag) > 0 ? 2 : 0)
				| (t.getFlag(TokenTable.iFlag) > 0 ? 1 : 0);
		int firstByte = op6 | ni;

		/* 7) xbpe 4bit */
		int xbpe = t.nixbpe & 0x0F;

		/* 8) object code 어셈블 */
		if (!isFmt4) {                               // 3?byte
			int code = (firstByte << 16) | (xbpe << 12) | disp20;
			t.objectCode = String.format("%06X", code);
			t.byteSize   = 3;
		} else {                                     // 4?byte
			long code = ((long) firstByte << 24) | ((long) xbpe << 20) | disp20;
			t.objectCode = String.format("%08X", code);
			t.byteSize   = 4;
		}

	}

	/* 레지스터 문자열 → 번호 */
	private int regNum(String r) {
		return switch (r.toUpperCase()) {
			case "A" -> 0; case "X" -> 1; case "L" -> 2; case "B" -> 3;
			case "S" -> 4; case "T" -> 5; case "F" -> 6; case "PC" -> 8; case "SW" -> 9;
			default  -> 0;
		};
	}

	/* #, @, (없음) 에 따라 n,i flag 세팅 */
	private void setNiFlags(Token t) {
		String op0 = t.operand[0];
		if (op0 == null) {                    // e.g., RSUB
			t.setFlag(TokenTable.nFlag,1);
			t.setFlag(TokenTable.iFlag,1);
			return;
		}
		if (op0.startsWith("#")) {
			t.setFlag(TokenTable.iFlag,1);
		} else if (op0.startsWith("@")) {
			t.setFlag(TokenTable.nFlag,1);
		} else {
			t.setFlag(TokenTable.nFlag,1);
			t.setFlag(TokenTable.iFlag,1);
		}
	}

	/* 심볼/리터럴/즉시값 → 절대 주소 */
	private int calcTargetAddr(Token t) {
		String op = t.operand[0];
		if (op == null) return 0;

		/* 즉시 상수 (#123) */
		if (op.startsWith("#") && op.length()>1 && Character.isDigit(op.charAt(1))) {
			return Integer.parseInt(op.substring(1));
		}

		/* 리터럴 (=C'EOF') */
		if (op.startsWith("=")) {
			return literalTab.searchLiteral(op);
		}

		/* 심볼 (@BUF, BUF, BUFFER) */
		op = op.replaceFirst("^[@#]", "");    // @ 또는 # 제거
		int addr = symTab.searchSymbol(op);
		if (addr < 0) {
			addr = 0; // 우선 0으로
//			throw new IllegalStateException("undefined symbol: " + op);
		}

		return addr;
	}

	/* BYTE X'..' / C'..' 인코딩 */
	private String encodeByte(String literal) {
		if (literal.startsWith("C'") && literal.endsWith("'")) {
			String s = literal.substring(2, literal.length()-1);
			StringBuilder hex = new StringBuilder();
			for (char ch : s.toCharArray())
				hex.append(String.format("%02X", (int) ch));
			return hex.toString();
		}
		if (literal.startsWith("X'") && literal.endsWith("'")) {
			return literal.substring(2, literal.length()-1).toUpperCase();
		}
		return "00";
	}

	/** 
	 * index번호에 해당하는 object code를 리턴한다.
	 * @param index
	 * @return : object code
	 */
	public String getObjectCode(int index) {
		return tokenList.get(index).objectCode;
	}


	/**
	 * 특정 토큰의 operand를 반환한다.
	 * EXTREF를 위해 만들었음 ..
	 */
	public String[] findTokenOperand(String operator) {
        for (Token token : tokenList) {
            if (operator.equals(token.operator)) {
                return token.operand;
            }
        }
		return new String[0];
	}
	
}

/**
 * 각 라인별로 저장된 코드를 단어 단위로 분할한 후  의미를 해석하는 데에 사용되는 변수와 연산을 정의한다. 
 * 의미 해석이 끝나면 pass2에서 object code로 변형되었을 때의 바이트 코드 역시 저장한다.
 */
class Token{
	//의미 분석 단계에서 사용되는 변수들
	int location;
	String label;
	String operator;
	String[] operand;
	String comment;
	char nixbpe;

	// object code 생성 단계에서 사용되는 변수들 
	String objectCode;
	int byteSize;

	private final InstTable instTab; // token parsing 과정에서 필요함
	
	/**
	 * 클래스를 초기화 하면서 바로 line의 의미 분석을 수행한다. 
	 * @param line 문장단위로 저장된 프로그램 코드
	 */
	public Token(String line, InstTable instTab, int locctr) {
		//initialize 추가
		this.location = locctr;
		this.label = null;
		this.operator = null;
		this.operand = null;
		this.comment = null;
		this.nixbpe = 0;

		this.objectCode	= null;
		this.byteSize = 0;

		this.instTab = instTab;

		parsing(line);
	}
	
	/**
	 * line의 실질적인 분석을 수행하는 함수. Token의 각 변수에 분석한 결과를 저장한다.
	 * @param line 문장단위로 저장된 프로그램 코드.
	 */
	public void parsing(String line) {
		// ---------- 0. 초깃값 ----------
		label = operator = comment = null;
		operand = new String[TokenTable.MAX_OPERAND];
		for (int i = 0; i< TokenTable.MAX_OPERAND; i++) {operand[i]=null; }
		nixbpe = 0;

		if (line == null) return;
		line = line.replace("\r", ""); // CRLF 정리

		// 공백 기준 토큰화
		String[] tok = line.trim().split("\\s+");
		if (tok.length==0) return;     // 빈 줄

		// 첫 토큰이 연산자인지, 레이블인지 판단
		String first = tok[0];
		boolean firstIsLabel = !(instTab.isExist(first) || InstTable.isDirective(first));

		// label, operator 처리
		if (firstIsLabel) {
			label = first;
			operator = tok[1];

			// operand, comment 처리
			if (instTab.isExist(operator)) {
				if (instTab.getFormat(operator) == 4) setFlag(TokenTable.eFlag, 1);
				if (instTab.getNumberOfOperand(operator) > 0) {

					String[] parts = tok[2].split(",");
					int k = 0;
					for (String p : parts) {
						p = p.trim();
						if ("X".equalsIgnoreCase(p)) {
							setFlag(TokenTable.xFlag,1);
						}
						if (!p.isEmpty() && k < operand.length) {
							operand[k++] = p;            // 고정 배열에 복사
						}
					}

					if (tok.length > 3) {          // 배열 길이 먼저 확인
						comment = tok[3];
					}
				} else {
					comment = tok[2];
				}

			} else if(InstTable.isDirective(operator)) {
				if(tok.length > 2) {

					String[] parts = tok[2].split(",");
					int k = 0;
					for (String p : parts) {
						p = p.trim();
						if ("X".equalsIgnoreCase(p)) {
							setFlag(TokenTable.xFlag,1);
						}
						if (!p.isEmpty() && k < operand.length) {
							operand[k++] = p;            // 고정 배열에 복사
						}
					}

					if (tok.length > 3) {          // 배열 길이 먼저 확인
						comment = tok[3];
					}
				}
			}

		} else {
			operator = tok[0];

			// operand, comment 처리
			if (instTab.isExist(operator)) {
				if (instTab.getFormat(operator) == 4) setFlag(TokenTable.eFlag, 1);
				if (instTab.getNumberOfOperand(operator) > 0) {

					String[] parts = tok[1].split(",");
					int k = 0;
					for (String p : parts) {
						p = p.trim();
						if ("X".equalsIgnoreCase(p)) {
							setFlag(TokenTable.xFlag,1);
//							continue;
						}
						if (!p.isEmpty() && k < operand.length) {
							operand[k++] = p;            // 고정 배열에 복사
						}
					}

					if (tok.length > 2) {          // 배열 길이 먼저 확인
						comment = tok[2];
					}
				} else {
					if (tok.length > 2) {          // 배열 길이 먼저 확인
						comment = tok[2];
					}
				}

			} else if(InstTable.isDirective(operator)) {
				if(tok.length > 1) {

					String[] parts = tok[1].split(",");
					int k = 0;
					for (String p : parts) {
						p = p.trim();
						if ("X".equalsIgnoreCase(p)) {
							setFlag(TokenTable.xFlag,1);
//							continue;
						}
						if (!p.isEmpty() && k < operand.length) {
							operand[k++] = p;            // 고정 배열에 복사
						}
					}

					if (tok.length > 2) {          // 배열 길이 먼저 확인
						comment = tok[2];
					}
				}
			}

		}


	}

	/** 
	 * n,i,x,b,p,e flag를 설정한다. <br><br>
	 * 
	 * 사용 예 : setFlag(nFlag, 1); <br>
	 *   또는     setFlag(TokenTable.nFlag, 1);
	 * 
	 * @param flag : 원하는 비트 위치
	 * @param value : 집어넣고자 하는 값. 1또는 0으로 선언한다.
	 */
	public void setFlag(int flag, int value) {
		if (value == 0) {
			// flag 비트를 0으로 클리어
			nixbpe &= (char) ~flag;
		} else {
			// flag 비트를 1로 설정
			nixbpe |= (char) flag;
		}
	}
	
	/**
	 * 원하는 flag들의 값을 얻어올 수 있다. flag의 조합을 통해 동시에 여러개의 플래그를 얻는 것 역시 가능하다 <br><br>
	 * 
	 * 사용 예 : getFlag(nFlag) <br>
	 *   또는     getFlag(nFlag|iFlag)
	 * 
	 * @param flags : 값을 확인하고자 하는 비트 위치
	 * @return : 비트위치에 들어가 있는 값. 플래그별로 각각 32, 16, 8, 4, 2, 1의 값을 리턴할 것임.
	 */
	public int getFlag(int flags) {
		return nixbpe & flags;
	}

	@Override
	public String toString() {

		StringBuilder sb = new StringBuilder();
		// 주소
		sb.append(String.format("loc=0x%04X", location));
		// 라벨
		if (label != null)  sb.append(" label=").append(label);
		// 연산자
		if (operator != null) sb.append(" op=").append(operator);
		// 피연산자
		if (operand != null) {
			sb.append(" operands=[");
			boolean first = true;
			for (String op : operand) {
				if (op != null) {
					if (!first) sb.append(",");
					sb.append(op);
					first = false;
				}
			}
			sb.append("]");
		}
		// nixbpe 플래그
		sb.append(String.format(" flags=0x%02X", (int) nixbpe));
		// byteSize
		sb.append(" size=").append(byteSize);
		// objectCode
		if (objectCode != null) sb.append(" obj=").append(objectCode);
		// 주석
		if (comment != null) sb.append(" comment=").append(comment);

		// nixbpe 플래그 (16진 대신 2진으로, 6비트로 패딩)
		String bin = Integer.toBinaryString(nixbpe & 0x3F);       // 하위 6비트만
		bin = String.format("%6s", bin).replace(' ', '0');        // 왼쪽 0으로 채우기
		sb.append(" flags_bin=").append(bin);

		return sb.toString();

	}

}

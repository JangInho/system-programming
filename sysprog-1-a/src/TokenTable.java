import java.util.ArrayList;

/**
 * ����ڰ� �ۼ��� ���α׷� �ڵ带 �ܾ�� ���� �� ��, �ǹ̸� �м��ϰ�, ���� �ڵ�� ��ȯ�ϴ� ������ �Ѱ��ϴ� Ŭ�����̴�. <br>
 * pass2���� object code�� ��ȯ�ϴ� ������ ȥ�� �ذ��� �� ���� symbolTable�� instTable�� ������ �ʿ��ϹǷ� �̸� ��ũ��Ų��.<br>
 * section ���� �ν��Ͻ��� �ϳ��� �Ҵ�ȴ�.
 *
 */
public class TokenTable {
	public static final int MAX_OPERAND=3;
	
	/* bit ������ �������� ���� ���� */
	public static final int nFlag=32;
	public static final int iFlag=16;
	public static final int xFlag=8;
	public static final int bFlag=4;
	public static final int pFlag=2;
	public static final int eFlag=1;

	/* Token�� �ٷ� �� �ʿ��� ���̺���� ��ũ��Ų��. */
	SymbolTable symTab;
	InstTable instTab;

	LiteralTable literalTab;
	
	
	/** �� line�� �ǹ̺��� �����ϰ� �м��ϴ� ����. */
	ArrayList<Token> tokenList;
	
	/**
	 * �ʱ�ȭ�ϸ鼭 symTable�� instTable�� ��ũ��Ų��.
	 * @param symTab : �ش� section�� ����Ǿ��ִ� symbol table
	 * @param instTab : instruction ���� ���ǵ� instTable
	 */
	public TokenTable(SymbolTable symTab, InstTable instTab, LiteralTable literalTab) {
		//...
		this.symTab = symTab;
		this.instTab = instTab;
		this.literalTab = literalTab;
		this.tokenList = new ArrayList<>();
	}
	
	/**
	 * �Ϲ� ���ڿ��� �޾Ƽ� Token������ �и����� tokenList�� �߰��Ѵ�.
	 * @param line : �и����� ���� �Ϲ� ���ڿ�
	 */
	public void putToken(String line, int locctr) {
		tokenList.add(new Token(line, instTab, locctr));
	}
	
	/**
	 * tokenList���� index�� �ش��ϴ� Token�� �����Ѵ�.
	 * @param index
	 * @return : index��ȣ�� �ش��ϴ� �ڵ带 �м��� Token Ŭ����
	 */
	public Token getToken(int index) {
		return tokenList.get(index);
	}
	
	/**
	 * Pass2 �������� ����Ѵ�.
	 * instruction table, symbol table ���� �����Ͽ� objectcode�� �����ϰ�, �̸� �����Ѵ�.
	 * @param index
	 */
	public void makeObjectCode(int index){

		Token t = tokenList.get(index);

		/* ������ 1) ��ɾ����� Ȯ�� ������������������������������������ */
		if (instTab.isExist(t.operator)) {
			encodeInstruction(t);
			return;
		}

		/* ������ 2) BYTE / WORD ������������������������������������������ */
		if ("BYTE".equalsIgnoreCase(t.operator)) {
			t.objectCode = encodeByte(t.operand[0]);  // C'EOF' �� 454F46
			t.byteSize   = t.objectCode.length() / 2; // ����Ʈ ��
			return;
		}

		/* ������ 2?b) ���ͷ� (=C'��' / =X'��') ������������������������������ */
		if (t.operator.startsWith("=")) {            // ex) "=C'EOF'"
			String litBody = t.operator.substring(1); // 'C'EOF'' �Ǵ� 'X'F1''
			t.objectCode = encodeByte(litBody);
			t.byteSize   = t.objectCode.length() / 2; // ����(����Ʈ)
			return;
		}

		if ("WORD".equalsIgnoreCase(t.operator)) {

			/* �ɺ� ���� ���� */
			if (t.operand[0].contains("+") || t.operand[0].contains("-")) {
				/* ���� ���� ������� �����Ƿ� 0 ���� ä�� */
				t.objectCode = "000000";
				t.byteSize   = 3;
				return;                  // �� ���⼭ ��!
			}

			/* ���� ������ �״�� 3����Ʈ */
			int val = Integer.parseInt(t.operand[0]);
			t.objectCode = String.format("%06X", val & 0xFFFFFF);
			t.byteSize   = 3;
			return;
		}

		/* ������ 3) RESB / RESW / LTORG / BASE �� ������ */
		t.objectCode = "";           // object code ����
	}

	private void encodeInstruction(Token t) {
		Instruction inst = instTab.find(t.operator);

		/* A. ���� 1, 2 �� ���� */
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


		/* ���� RSUB ���� �������������������������������������������������� */
		if ("RSUB".equalsIgnoreCase(t.operator)) {
			t.setFlag(TokenTable.nFlag, 1);   // n = 1
			t.setFlag(TokenTable.iFlag, 1);   // i = 1
			int firstByte = (inst.opcode & 0xFC) | 0x03; // opcode ����6bit + ni(11)
			int code = firstByte << 16;                  // xbpe = 0000, disp = 000
			t.objectCode = String.format("%06X", code);  // �� 4F0000
			t.byteSize   = 3;
			return;
		}

		/* 3 / 4 ���� ���������������������������������������������������������������������������������� */

		/* 1) n,i flag ���� (#,@,Ȥ�� �⺻) */
		setNiFlags(t);  // (# �� i , @ �� n , ������ ni=1)

		/* 2) x flag �� pass1���� �̹� setFlag(xFlag,1) */

		/* 3) ��� ���� ���� ���Ǵ� (#123 ��) */
		String op0 = t.operand[0];
		boolean immNumeric = false;
		int     immValue   = 0;
		if (op0 != null && op0.startsWith("#") &&
				op0.length() > 1 && Character.isDigit(op0.charAt(1))) {
			immNumeric = true;
			immValue   = Integer.parseInt(op0.substring(1));
		}

		/* 4) target ��� */
		int target = immNumeric ? immValue           // ��� ����� �� ��ü
				: calcTargetAddr(t); // �ɺ������ͷ� �ּ�

		/* 5) disp/addr + b,p,e ���� */
		boolean isFmt4 = t.getFlag(TokenTable.eFlag) != 0;
		int instrLen   = isFmt4 ? 4 : 3;
		int disp20;

		if (immNumeric) {                            // ��� ���
			if (!isFmt4 && immValue <= 0xFFF) {      // Fmt3�� ��
				disp20 = immValue;
				// b,p �� �⺻ 0 �״��
			} else {                                 // ���� ũ�� format4 ����
				t.setFlag(TokenTable.eFlag, 1);
				isFmt4 = true;
				disp20 = immValue & 0xFFFFF;
			}
		} else {                                     // �ɺ� / ���ͷ�
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

		/* 6) opcode ���� 6bit + ni 2bit */
		int op6 = inst.opcode & 0xFC;
		int ni  = (t.getFlag(TokenTable.nFlag) > 0 ? 2 : 0)
				| (t.getFlag(TokenTable.iFlag) > 0 ? 1 : 0);
		int firstByte = op6 | ni;

		/* 7) xbpe 4bit */
		int xbpe = t.nixbpe & 0x0F;

		/* 8) object code ����� */
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

	/* �������� ���ڿ� �� ��ȣ */
	private int regNum(String r) {
		return switch (r.toUpperCase()) {
			case "A" -> 0; case "X" -> 1; case "L" -> 2; case "B" -> 3;
			case "S" -> 4; case "T" -> 5; case "F" -> 6; case "PC" -> 8; case "SW" -> 9;
			default  -> 0;
		};
	}

	/* #, @, (����) �� ���� n,i flag ���� */
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

	/* �ɺ�/���ͷ�/��ð� �� ���� �ּ� */
	private int calcTargetAddr(Token t) {
		String op = t.operand[0];
		if (op == null) return 0;

		/* ��� ��� (#123) */
		if (op.startsWith("#") && op.length()>1 && Character.isDigit(op.charAt(1))) {
			return Integer.parseInt(op.substring(1));
		}

		/* ���ͷ� (=C'EOF') */
		if (op.startsWith("=")) {
			return literalTab.searchLiteral(op);
		}

		/* �ɺ� (@BUF, BUF, BUFFER) */
		op = op.replaceFirst("^[@#]", "");    // @ �Ǵ� # ����
		int addr = symTab.searchSymbol(op);
		if (addr < 0) {
			addr = 0; // �켱 0����
//			throw new IllegalStateException("undefined symbol: " + op);
		}

		return addr;
	}

	/* BYTE X'..' / C'..' ���ڵ� */
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
	 * index��ȣ�� �ش��ϴ� object code�� �����Ѵ�.
	 * @param index
	 * @return : object code
	 */
	public String getObjectCode(int index) {
		return tokenList.get(index).objectCode;
	}


	/**
	 * Ư�� ��ū�� operand�� ��ȯ�Ѵ�.
	 * EXTREF�� ���� ������� ..
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
 * �� ���κ��� ����� �ڵ带 �ܾ� ������ ������ ��  �ǹ̸� �ؼ��ϴ� ���� ���Ǵ� ������ ������ �����Ѵ�. 
 * �ǹ� �ؼ��� ������ pass2���� object code�� �����Ǿ��� ���� ����Ʈ �ڵ� ���� �����Ѵ�.
 */
class Token{
	//�ǹ� �м� �ܰ迡�� ���Ǵ� ������
	int location;
	String label;
	String operator;
	String[] operand;
	String comment;
	char nixbpe;

	// object code ���� �ܰ迡�� ���Ǵ� ������ 
	String objectCode;
	int byteSize;

	private final InstTable instTab; // token parsing �������� �ʿ���
	
	/**
	 * Ŭ������ �ʱ�ȭ �ϸ鼭 �ٷ� line�� �ǹ� �м��� �����Ѵ�. 
	 * @param line ��������� ����� ���α׷� �ڵ�
	 */
	public Token(String line, InstTable instTab, int locctr) {
		//initialize �߰�
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
	 * line�� �������� �м��� �����ϴ� �Լ�. Token�� �� ������ �м��� ����� �����Ѵ�.
	 * @param line ��������� ����� ���α׷� �ڵ�.
	 */
	public void parsing(String line) {
		// ---------- 0. �ʱ갪 ----------
		label = operator = comment = null;
		operand = new String[TokenTable.MAX_OPERAND];
		for (int i = 0; i< TokenTable.MAX_OPERAND; i++) {operand[i]=null; }
		nixbpe = 0;

		if (line == null) return;
		line = line.replace("\r", ""); // CRLF ����

		// ���� ���� ��ūȭ
		String[] tok = line.trim().split("\\s+");
		if (tok.length==0) return;     // �� ��

		// ù ��ū�� ����������, ���̺����� �Ǵ�
		String first = tok[0];
		boolean firstIsLabel = !(instTab.isExist(first) || InstTable.isDirective(first));

		// label, operator ó��
		if (firstIsLabel) {
			label = first;
			operator = tok[1];

			// operand, comment ó��
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
							operand[k++] = p;            // ���� �迭�� ����
						}
					}

					if (tok.length > 3) {          // �迭 ���� ���� Ȯ��
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
							operand[k++] = p;            // ���� �迭�� ����
						}
					}

					if (tok.length > 3) {          // �迭 ���� ���� Ȯ��
						comment = tok[3];
					}
				}
			}

		} else {
			operator = tok[0];

			// operand, comment ó��
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
							operand[k++] = p;            // ���� �迭�� ����
						}
					}

					if (tok.length > 2) {          // �迭 ���� ���� Ȯ��
						comment = tok[2];
					}
				} else {
					if (tok.length > 2) {          // �迭 ���� ���� Ȯ��
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
							operand[k++] = p;            // ���� �迭�� ����
						}
					}

					if (tok.length > 2) {          // �迭 ���� ���� Ȯ��
						comment = tok[2];
					}
				}
			}

		}


	}

	/** 
	 * n,i,x,b,p,e flag�� �����Ѵ�. <br><br>
	 * 
	 * ��� �� : setFlag(nFlag, 1); <br>
	 *   �Ǵ�     setFlag(TokenTable.nFlag, 1);
	 * 
	 * @param flag : ���ϴ� ��Ʈ ��ġ
	 * @param value : ����ְ��� �ϴ� ��. 1�Ǵ� 0���� �����Ѵ�.
	 */
	public void setFlag(int flag, int value) {
		if (value == 0) {
			// flag ��Ʈ�� 0���� Ŭ����
			nixbpe &= (char) ~flag;
		} else {
			// flag ��Ʈ�� 1�� ����
			nixbpe |= (char) flag;
		}
	}
	
	/**
	 * ���ϴ� flag���� ���� ���� �� �ִ�. flag�� ������ ���� ���ÿ� �������� �÷��׸� ��� �� ���� �����ϴ� <br><br>
	 * 
	 * ��� �� : getFlag(nFlag) <br>
	 *   �Ǵ�     getFlag(nFlag|iFlag)
	 * 
	 * @param flags : ���� Ȯ���ϰ��� �ϴ� ��Ʈ ��ġ
	 * @return : ��Ʈ��ġ�� �� �ִ� ��. �÷��׺��� ���� 32, 16, 8, 4, 2, 1�� ���� ������ ����.
	 */
	public int getFlag(int flags) {
		return nixbpe & flags;
	}

	@Override
	public String toString() {

		StringBuilder sb = new StringBuilder();
		// �ּ�
		sb.append(String.format("loc=0x%04X", location));
		// ��
		if (label != null)  sb.append(" label=").append(label);
		// ������
		if (operator != null) sb.append(" op=").append(operator);
		// �ǿ�����
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
		// nixbpe �÷���
		sb.append(String.format(" flags=0x%02X", (int) nixbpe));
		// byteSize
		sb.append(" size=").append(byteSize);
		// objectCode
		if (objectCode != null) sb.append(" obj=").append(objectCode);
		// �ּ�
		if (comment != null) sb.append(" comment=").append(comment);

		// nixbpe �÷��� (16�� ��� 2������, 6��Ʈ�� �е�)
		String bin = Integer.toBinaryString(nixbpe & 0x3F);       // ���� 6��Ʈ��
		bin = String.format("%6s", bin).replace(' ', '0');        // ���� 0���� ä���
		sb.append(" flags_bin=").append(bin);

		return sb.toString();

	}

}

// instruction에 따라 동작을 수행하는 메소드를 정의하는 클래스

public class InstLuncher {

    ResourceManager rMgr;

    boolean swStatus; // sw 레지스터를 보조할 불리언값

    String currDevice = ""; // 현재 디바이스

    String targetAddr = ""; // 패널의 Target Addresss 에 나오는 주소

    public InstLuncher(ResourceManager resourceManager) {
	this.rMgr = resourceManager;
    }

    // 명령어 함수 관리
    public int execute(Instruction inst, int nixbpe, int disp, int locctr) {
	currDevice = "";
	targetAddr = "";
	int instLen = byteSize(inst, nixbpe);
	AddressHelper addressHelper = new AddressHelper(nixbpe, disp, locctr + instLen);
	switch (inst.instruction) {
	case "STA" -> STA(addressHelper);
	case "STX" -> STX(addressHelper);
	case "STL" -> STL(addressHelper);
	case "STCH" -> STCH(addressHelper);
	case "CLEAR" -> CLEAR(nixbpe);
	case "COMP" -> COMP(addressHelper);
	case "COMPR" -> COMPR(disp);
	case "JEQ" -> {
	    return JEQ(addressHelper, locctr, inst, nixbpe);
	}
	case "JLT" -> {
	    return JLT(addressHelper, locctr, inst, nixbpe);
	}
	case "J" -> {
	    return J(nixbpe, addressHelper);
	}
	case "LDA" -> LDA(addressHelper);
	case "LDT" -> LDT(addressHelper);
	case "LDCH" -> LDCH(addressHelper);
	case "JSUB" -> {
	    return JSUB(addressHelper, locctr, inst, nixbpe);
	}
	case "RSUB" -> {
	    return RSUB();
	}
	case "TD" -> TD(addressHelper);
	case "WD" -> WD(addressHelper);
	case "RD" -> RD(addressHelper);
	case "TIXR" -> TIXR(addressHelper, nixbpe);
	}
	return locctr + instLen;
    }

    /**
     * 명령어 함수 ---------------
     *
     */

    private int J(int nixbpe, AddressHelper addressHelper) {
	// 1. 간접(indirect) 모드인지 판별: nixbpe 상위 2비트 == 0x20 (n=1,i=0)
	if ((nixbpe & 0x30) == 0x20) {
	    // 1-1. 일단 disp-based 를 구한다
	    int ea = addressHelper.getAbsoluteAddress();
	    // 1-2. 메모리에서 바이트 단위로 3 byte를 읽어 옴
	    //  getmemory 는 raw 바이트를 char[]로 돌려줌
	    char[] raw3 = rMgr.getMemory(ea, 3);
	    // 1-3. 각 바이트를 (b & 0xFF)로 정수화하며 24비트 정수로 바꿈
	    int indirectAddr = ((raw3[0] & 0xFF) << 16)
		    | ((raw3[1] & 0xFF) << 8)
		    | (raw3[2] & 0xFF);
	    // 1-4. 만약 간접 주소가 0 이면 프로그램 종료로 간주
	    if (indirectAddr == 0) {
		targetAddr = "000000";
		return 0;
	    }
	    // 1-5. 정상적인 간접 점프: indirectAddr 을 다음 PC 로 사용
	    targetAddr = String.format("%06X", indirectAddr);
	    return indirectAddr;
	}
	// 2. Direct 모드(간접 모드 아님) → 그냥 절대 EA 로 점프
	int ea = addressHelper.getAbsoluteAddress();
	targetAddr = format24Bit(ea);
	return ea;
    }

    private int JEQ(AddressHelper addressHelper, int locctr, Instruction inst, int nixbpe) {
	if (swStatus) {
	    targetAddr = format24Bit(addressHelper.getAbsoluteAddress());
	    return addressHelper.getAbsoluteAddress();
	} else {
	    // TODO
	    //		if (disp > 0x800)
	    //		    disp |= 0xFFFFF000;
	    //		targetAddr = String.format("%06X", locctr + disp);
	    targetAddr = format24Bit(addressHelper.getAbsoluteAddress());
	    return locctr + byteSize(inst, nixbpe);
	}
    }

    private int JLT(AddressHelper addressHelper, int locctr, Instruction inst, int nixbpe) {
	//  cc == 1 면 less
	boolean less = (rMgr.getRegister(9) == 1);
	targetAddr = format24Bit(addressHelper.getAbsoluteAddress());
	return less ? addressHelper.getAbsoluteAddress() : locctr + byteSize(inst, nixbpe);
    }

    private int JSUB(AddressHelper addressHelper, int locctr, Instruction inst, int nixbpe) {
	rMgr.setRegister(2, locctr + byteSize(inst, nixbpe)); // L 레지스터
	targetAddr = format24Bit(addressHelper.getAbsoluteAddress());
	return addressHelper.getAbsoluteAddress();
    }

    private void LDA(AddressHelper addressHelper) {
	rMgr.setRegister(0, addressHelper.value());
    }

    private void LDCH(AddressHelper addressHelper) {
	rMgr.setRegister(0, addressHelper.valueByte());
    }

    private void LDT(AddressHelper addressHelper) {
	rMgr.setRegister(5, addressHelper.value());
    }

    private int RSUB() {
	targetAddr = format24Bit(rMgr.getRegister(2));
	return rMgr.getRegister(2);        // L
    }

    private void STA(AddressHelper addressHelper) {
	addressHelper.storeWord(rMgr.getRegister(0));
    }

    private void STCH(AddressHelper addressHelper) {
	addressHelper.storeByte(rMgr.getRegister(0) & 0xFF);
    }

    private void STL(AddressHelper addressHelper) {
	addressHelper.storeWord(rMgr.getRegister(2));
    }

    private void STX(AddressHelper addressHelper) {
	addressHelper.storeWord(rMgr.getRegister(1));
    }

    private void TD(AddressHelper addressHelper) {
	swStatus = !rMgr.testDevice(addressHelper.deviceName()); // TODO: !붙여야하나 ?
	rMgr.setRegister(9, rMgr.testDevice(addressHelper.deviceName()) ? 0 : 1);
	targetAddr = format24Bit(addressHelper.getAbsoluteAddress());
	currDevice = addressHelper.deviceName();
    }

    private void WD(AddressHelper addressHelper) {
	char[] data = rMgr.intToChar(rMgr.getRegister(0));
	rMgr.writeDevice(addressHelper.deviceName(), data, 1);
	currDevice = addressHelper.deviceName();
	targetAddr = format24Bit(addressHelper.getAbsoluteAddress());
    }

    private void RD(AddressHelper addressHelper) {
	targetAddr = format24Bit(addressHelper.getAbsoluteAddress());
	char[] rd = rMgr.readDevice(addressHelper.deviceName(), 1);
	rMgr.setRegister(0, rd[0]);
	currDevice = addressHelper.deviceName();
    }

    private void TIXR(AddressHelper addressHelper, int nixbpe) {
	//  X 레지스터
	int newX = rMgr.getRegister(1) + 1;
	rMgr.setRegister(1, newX);
	//  두 번째 바이트 하위 4 bit
	int r2 = rMgr.getRegister(nixbpe & 0xF);
	int cc = (newX == r2) ? 0 : (newX < r2 ? 1 : 2);
	rMgr.setRegister(9, cc);
	//  JEQ 를  위한 swStatus 세팅..
	swStatus = (cc == 0);
    }

    private void CLEAR(int nixbpe) {
	rMgr.setRegister(nixbpe, 0);
    }

    private void COMP(AddressHelper addressHelper) {
	swStatus = (rMgr.getRegister(0) == addressHelper.value());
	//	    0 = equal
	//	    1 = less
	//	    2 = greater
	rMgr.setRegister(9, rMgr.getRegister(0) == addressHelper.value() ? 0 : 1);
    }

    private void COMPR(int disp) {
	int r1 = rMgr.getRegister((disp >> 4) & 0xF);  // 상위 4bit
	int r2 = rMgr.getRegister(disp & 0xF); // 하위 4bit
	rMgr.setRegister(9, r1 == r2 ? 0 : 1);
	swStatus = (r1 == r2);
    }
    /**
     * 계산을 도와주는 헬퍼 클래스와 메소드 -------------------------
     */
    /**
     * 형식별 몇바이트인지
     */
    private static int byteSize(Instruction inst, int nixbpe) {
	return switch (inst.format) {
	    case 1 -> 1;
	    case 2 -> 2;
	    default -> ((nixbpe & 1) == 1) ? 4 : 3;   // e-bit가 1이면 format 4
	};
    }

    /**
     * 주소계산 + 메모리 접근 원래 계획은 모든 명령어 처리 메소드에 이 헬퍼클래스만 넘겨주면 계산이 다 되는 걸 생각했는데, 시간 관계상 일부만 처리
     */
    private class AddressHelper {

	final int nixbpe, disp, pcByte;

	AddressHelper(int n, int d, int pc) {
	    this.nixbpe = n;
	    this.disp = d;
	    this.pcByte = pc;
	}

	// pc와 같은 상대주소가 아닌 실제 주소를 얻는 함수
	int getAbsoluteAddress() {
	    int ea;
	    // 4형식 e == 1
	    if ((nixbpe & 0x01) == 1) {
		ea = disp;  // 20-bit absolute
	    } else {
		// 3형식
		int delta = (disp & 0x800) != 0 ? (disp | 0xFFFFF000) : disp;
		if ((nixbpe & 0x02) != 0) // p-bit = 1 → PC-relative
		    ea = pcByte + delta;
		else if ((nixbpe & 0x04) != 0) // b-bit = 1 → BASE-relative
		    ea = rMgr.getRegister(3) + delta;
		else // direct 12-bit
		    ea = disp;
	    }
	    // x-bit == 1
	    if ((nixbpe & 0x08) != 0)
		ea += rMgr.getRegister(1); // X 레지스터
	    return ea;
	}

	int value() {
	    if ((nixbpe & 0x30) == 0x10) { // immediate
		targetAddr = format24Bit(disp);
		return disp;
	    }
	    int adr = getAbsoluteAddress();
	    targetAddr = format24Bit(adr);
	    return rMgr.byteToIntFromChars(rMgr.getMemory(adr, 3));
	}

	int valueByte() {
	    int adr = getAbsoluteAddress();
	    targetAddr = format24Bit(adr);
	    //	    return rMgr.byteToIntFromChars(rMgr.getMemory(adr,2));
	    return rMgr.getMemory(adr, 1)[0] & 0xFF;
	}

	void storeWord(int v) {
	    int adr = getAbsoluteAddress();
	    targetAddr = format24Bit(adr);
	    rMgr.setMemory(adr, rMgr.intToChar(v), 3);
	}

	void storeByte(int v) {
	    int adr = getAbsoluteAddress();
	    targetAddr = format24Bit(adr);
	    rMgr.setMemory(adr, new char[] { (char) (v & 0xFF) }, 1);
	}

	String deviceName() {
	    int byteVal = rMgr.getMemory(getAbsoluteAddress(), 1)[0] & 0xFF;
	    return String.format("%02X", byteVal);
	}
    }

    /* 24bit 주소 포맷터 */
    private static String format24Bit(int adr) {
	return String.format("%06X", adr);
    }
}

class Instruction {

    String instruction; // 명령어 이름

    int opcode; // 8-bit opcode

    int operandNum; // 피연산자 개수

    int format; // 포맷

    public Instruction(String line) {
	parse(line);
    }

    // 파서
    public void parse(String rawLine) {
	// trim
	String line = rawLine.split(";", 2)[0].trim();
	if (line.isEmpty())
	    throw new IllegalArgumentException("blank / comment line");
	// 공백 탭 구분 없이 split → 최대 4토큰
	String[] tok = line.split("[\\s&&[^\\r\\n]]+");
	if (tok.length != 4)
	    throw new IllegalArgumentException("Malformed inst spec: " + rawLine);
	instruction = tok[0].toUpperCase();
	try {
	    format = Integer.parseInt(tok[1]);
	    opcode = Integer.parseInt(tok[2], 16);
	    operandNum = Integer.parseInt(tok[3]);
	} catch (NumberFormatException nfe) {
	    throw new IllegalArgumentException("Numeric parse error: " + rawLine, nfe);
	}
	// 유효성 검증
	if (format < 1 || format > 4)
	    throw new IllegalArgumentException("Unsupported format: " + format);
	if (operandNum < 0 || operandNum > 3)
	    throw new IllegalArgumentException("Operand cnt out of range: " + operandNum);
	if (opcode < 0x00 || opcode > 0xFC || (opcode & 0x03) != 0)
	    throw new IllegalArgumentException("Invalid opcode boundary: " + opcode);
    }

    @Override
    public String toString() {
	return String.format("%-6s F=%d OPC=%02X (#%d)", instruction, format, opcode, operandNum);
    }
}
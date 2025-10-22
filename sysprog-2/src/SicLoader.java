import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * SicLoader는 프로그램을 해석해서 메모리에 올리는 역할을 수행한다. 이 과정에서 linker의 역할 또한 수행한다. SicLoader가 수행하는 일을 예를 들면 다음과 같다. - program code를
 * 메모리에 적재시키기 - 주어진 공간만큼 메모리에 빈 공간 할당하기 - 과정에서 발생하는 symbol, 프로그램 시작주소, control section 등 실행을 위한 정보 생성 및 관리
 */
public class SicLoader {

    ResourceManager rMgr;

    String programName = ""; // 프로그램 이름

    int programTotalLength = 0; // 프로그램 전체 길이

    int programStartAddress = 0; // 프로그램 시작주소

    int firstInstruction = 0; // 첫번쨰 명령어

    public SicLoader(ResourceManager resourceManager) {
	// 필요하다면 초기화
	setResourceManager(resourceManager);
    }

    /**
     * Loader와 프로그램을 적재할 메모리를 연결시킨다.
     *
     * @param rMgr
     */
    public void setResourceManager(ResourceManager resourceManager) {
	this.rMgr = resourceManager;
    }

    /**
     * object code를 읽어서 load과정을 수행한다. load한 데이터는 resourceManager가 관리하는 메모리에 올라가도록 한다. load과정에서 만들어진 symbol table 등 자료구조
     * 역시 resourceManager에 전달한다.
     *
     * @param objectCode 읽어들인 파일
     */
    public void load(File objectCode) {
	// 상태 초기화
	programName = "";
	programStartAddress = programTotalLength = firstInstruction = 0;
	List<MRec> mBuffer = new ArrayList<>();
	try (BufferedReader bufferedReader = new BufferedReader(new FileReader(objectCode))) {
	    String line;
	    int curSectionLen = 0; // 현재 CSECT 길이

	    // 1 pass
	    while ((line = bufferedReader.readLine()) != null) {
		if (line.isEmpty())
		    continue;
		switch (line.charAt(0)) {

		// Header Record
		case 'H' -> {
		    String name = line.substring(1, 7).trim();
		    int start = Integer.parseInt(line.substring(7, 13), 16)
			    + programTotalLength;
		    curSectionLen = Integer.parseInt(line.substring(13), 16);
		    if (programName.isEmpty()) {        // 첫 CSECT
			programName = name;
			programStartAddress = start;
		    }
		    rMgr.symtabList.putSymbol(name, start);
		}

		// Define Record
		case 'D' -> {
		    for (int i = 1; i + 11 < line.length(); i += 12) {
			String sym = line.substring(i, i + 6).trim();
			int adr = Integer.parseInt(line.substring(i + 6, i + 12), 16)
				+ programTotalLength;
			rMgr.symtabList.putSymbol(sym, adr);
		    }
		}

		// Text Record
		case 'T' -> {
		    int tAddr = Integer.parseInt(line.substring(1, 7), 16)
			    + programTotalLength;
		    int tLen = Integer.parseInt(line.substring(7, 9), 16);
		    String hex = line.substring(9);
		    char[] raw = new char[tLen];
		    for (int i = 0; i < tLen; i++)
			raw[i] = (char) (Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16) & 0xFF);
		    rMgr.setMemory(tAddr, raw, tLen);
		}

		// Modify Record
		case 'M' -> {
		    int addr = Integer.parseInt(line.substring(1, 7), 16) + programTotalLength;
		    int nibbles = Integer.parseInt(line.substring(7, 9), 16);   // 05 or 06
		    char sign = line.charAt(9);
		    String symbol = line.substring(10).trim();

		    mBuffer.add(new MRec(addr, nibbles, sign, symbol));
		}

		// End Record
		case 'E' -> {
		    if (line.length() > 1)
			firstInstruction = Integer.parseInt(line.substring(1).trim(), 16);
		    programTotalLength += curSectionLen;
		}
		}
	    }
	} catch (IOException ioe) {
	    throw new RuntimeException("Loader I/O", ioe);
	}

	// 2 pass M 처리
	for (MRec m : mBuffer) {
	    int bytes = (m.nibbles + 1) / 2;          // 05→3, 06→3
	    boolean odd = (m.nibbles % 2) == 1;         // 상위 nib부터?

	    // 원본 값 읽고
	    char[] src = rMgr.getMemory(m.addr, bytes);
	    int val = 0;
	    for (char b : src)
		val = (val << 8) | (b & 0xFF);

	    //  고정 nibble 보존
	    int highNib = 0;
	    if (odd) {
		highNib = val >>> (m.nibbles * 4); // 상위 nibble (XBPE)
		val &= (1 << (m.nibbles * 4)) - 1;     // 하위 nibbles만
	    }

	    // 외부 심볼
	    int symbolAddress = rMgr.symtabList.search(m.symbol);
	    int patched = (m.sign == '+') ? val + symbolAddress
		    : val - symbolAddress;

	    // 길이만큼 마스킹
	    int mask = (1 << (m.nibbles * 4)) - 1;
	    patched &= mask;

	    //  high nibble 복구
	    if (odd)
		patched |= (highNib << (m.nibbles * 4));

	    // 다시 byte[] big-endian 으로
	    char[] out = new char[bytes];
	    for (int i = bytes - 1; i >= 0; i--) {
		out[i] = (char) (patched & 0xFF);
		patched >>>= 8;
	    }
	    rMgr.setMemory(m.addr, out, bytes);
	}
//	dump();   // 디버깅용
    }

    //  내부용 M 레코드
    private record MRec(int addr,  // byte 주소 (절대)
	    int nibbles, // 필드 길이 (half-byte)
	    char sign,   // '+' | '-'
	    String symbol) {

    }

    /** 디버그용: 로드가 끝난 뒤 호출하면 메모리·심벌·정보를 콘솔에 뿌려 줍니다 */
    public void dump() {
	System.out.println("=== LOADER DUMP ===");
	System.out.printf("Program  : %s%n", programName);
	System.out.printf("StartAdr : %06X%n", programStartAddress);
	System.out.printf("Length   : %06X%n", programTotalLength);
	System.out.printf("1st Inst : %06X%n", firstInstruction);
	System.out.println("\n-- SYMTAB --");
	for (int i = 0; i < rMgr.symtabList.symbolList.size(); i++)
	    System.out.printf("%-8s : %06X%n",
		    rMgr.symtabList.symbolList.get(i),
		    rMgr.symtabList.addressList.get(i));
	System.out.println("\n-- FIRST 64 BYTES --");
	char[] mem = rMgr.getMemory(programStartAddress, 64);
	for (int i = 0; i < mem.length; i++) {
	    if (i % 16 == 0)
		System.out.printf("%n%06X : ", programStartAddress + i);
	    System.out.printf("%02X ", mem[i] & 0xFF);
	}
	System.out.println("\n====================\n");
    }
}
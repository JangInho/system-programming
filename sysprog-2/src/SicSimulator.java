import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;

/**
 * 시뮬레이터로서의 작업을 담당한다. VisualSimulator에서 사용자의 요청을 받으면 이에 따라 ResourceManager에 접근하여 작업을 수행한다. 작성중의 유의사항 : 1) 새로운 클래스, 새로운
 * 변수, 새로운 함수 선언은 얼마든지 허용됨. 단, 기존의 변수와 함수들을 삭제하거나 완전히 대체하는 것은 지양할 것. 2) 필요에 따라 예외처리, 인터페이스 또는 상속 사용 또한 허용됨. 3) 모든 void
 * 타입의 리턴값은 유저의 필요에 따라 다른 리턴 타입으로 변경 가능. 4) 파일, 또는 콘솔창에 한글을 출력시키지 말 것. (채점상의 이유. 주석에 포함된 한글은 상관 없음) + 제공하는 프로그램 구조의
 * 개선방법을 제안하고 싶은 분들은 보고서의 결론 뒷부분에 첨부 바랍니다. 내용에 따라 가산점이 있을 수 있습니다.
 */
public class SicSimulator {

    /**
     * 주어진 변수
     */
    ResourceManager rMgr;

    /**
     * 새로 선언한 변수들
     */
    InstLuncher instLuncher;

    private String lastInst = ""; // 직전  instruction

    private int startAddress = 0;

    private final HashMap<Integer, Instruction> instructionTable = new HashMap<>();  // opcode → meta

    private final List<String> logList = new ArrayList<>();  // 실행 로그

    public SicSimulator(ResourceManager resourceManager) {
	// 필요하다면 초기화 과정 추가
	this.rMgr = resourceManager;
	this.instLuncher = new InstLuncher(rMgr);

	parseInstruction();
    }

    private void parseInstruction() {
	try {
	    File file = new File("inst_table.txt");
	    FileReader fileReader = new FileReader(file);
	    BufferedReader bufferedReader = new BufferedReader(fileReader);
	    String line;

	    while ((line = bufferedReader.readLine()) != null) {
		Instruction inst = new Instruction(line);
		instructionTable.put(inst.opcode, inst);
	    }
	} catch (IOException e) {
	    e.printStackTrace();
	}
    }
    /**
     * 레지스터, 메모리 초기화 등 프로그램 load와 관련된 작업 수행. 단, object code의 메모리 적재 및 해석은 SicLoader에서 수행하도록 한다.
     */
    public void load(File program) {
	/* 메모리 초기화, 레지스터 초기화 등 */
	rMgr.initializeResource();
	logList.clear();
	lastInst = "";
	instLuncher = new InstLuncher(rMgr);
    }

    /**
     * 1개의 instruction이 수행된 모습을 보인다.
     */
    public boolean oneStep() {

	// 현재 pc
	int pc = rMgr.getRegister(8);
	startAddress = pc;

	// 첫번째 바이트, opcode 1 byte
	int opByte = rMgr.getMemory(pc, 1)[0] & 0xFF;

	// 명령어 테이블 조회 (상위 6비트만 비교)
	Instruction meta = instructionTable.get(opByte & 0xFC);
	if (meta == null) {
	    addLog("UNKNOWN OPC");
	    return false;
	}

	// 두번째 바이트
	int byte1 = rMgr.getMemory(pc + 1, 1)[0] & 0xFF;

	int nixbpe = ((opByte & 0x03) << 4) | (byte1 >> 4);

	// format
	int format = meta.format;
	if (format > 2 && (nixbpe & 0x01) == 1) format++; //4형식 보정

	int disp = 0;
	switch (format) {
	case 1 -> { /* disp = 0 */ }
	case 2 -> {  // r1 | r2
	    disp = byte1;
	}
	case 3 -> {  // 12-bit disp
	    int byte2 = rMgr.getMemory(pc + 2, 1)[0] & 0xFF;
	    disp = ((byte1 & 0x0F) << 8) | byte2;
	}
	case 4 -> {                               // 20-bit addr
	    int byte2 = rMgr.getMemory(pc + 2, 1)[0] & 0xFF;
	    int byte3 = rMgr.getMemory(pc + 3, 1)[0] & 0xFF;
	    disp = ((byte1 & 0x0F) << 16) | (byte2 << 8) | byte3;
	}
	}

	//  명령 길이 계산
	int instLen = switch (format){ case 1->1; case 2->2; case 4->4; default->3; };
	// 현재 명령의 객체코드 hex 문자열 생성
	StringBuilder sb = new StringBuilder(instLen*2);
	for (int i = 0; i < instLen; i++)
	    sb.append(String.format("%02X", rMgr.getMemory(pc + i, 1)[0] & 0xFF));
	lastInst = sb.toString();


	// 명령어 실행 → 다음 PC 얻어옴
	int next = instLuncher.execute(meta, nixbpe, disp, pc);

	rMgr.setRegister(8, next);

	addLog(meta.instruction);

	return next != 0;
    }

    /**
     * 남은 모든 instruction이 수행된 모습을 보인다.
     */
    public boolean allStep() {
	// 원스텝을 호출하지만 allStep을 호출하는 곳에서 update를 사용해야하기때문에 이렇게 사용함
	// boolean으로 리턴 바꿈
	return oneStep();
    }

    /**
     * 각 단계를 수행할 때 마다 관련된 기록을 남기도록 한다.
     */
    public void addLog(String log) {
	logList.add(String.format("%s", log));
    }

    /**
     * getter
     * @return
     */
    public List<String> getTrace() {
	return logList;
    }

    public String getLastInst() {
	return lastInst;
    }

    public int getStartAddress() {
	return startAddress;
    }
}

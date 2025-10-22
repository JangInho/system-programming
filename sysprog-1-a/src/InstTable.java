import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;

import java.io.BufferedReader;
import java.util.HashSet;
import java.util.Set;

/**
 * 모든 instruction의 정보를 관리하는 클래스. instruction data들을 저장한다. <br>
 * 또한 instruction 관련 연산, 예를 들면 목록을 구축하는 함수, 관련 정보를 제공하는 함수 등을 제공 한다.
 */
public class InstTable {
	/** 
	 * inst.data 파일을 불러와 저장하는 공간.
	 *  명령어의 이름을 집어넣으면 해당하는 Instruction의 정보들을 리턴할 수 있다.
	 */
	HashMap<String, Instruction> instMap;
	
	/**
	 * 클래스 초기화. 파싱을 동시에 처리한다.
	 * @param instFile : instuction에 대한 명세가 저장된 파일 이름
	 */
	public InstTable(String instFile) {
		instMap = new HashMap<String, Instruction>();
		openFile(instFile);
	}
	
	/**
	 * 입력받은 이름의 파일을 열고 해당 내용을 파싱하여 instMap에 저장한다.
	 */
	public void openFile(String fileName) {

		// InstTable.openFile()
		try (BufferedReader br = Files.newBufferedReader(Paths.get(fileName))) {
			String line;
			while ((line = br.readLine()) != null) {
				line = line.trim();
				if (line.isEmpty()) continue;
				Instruction inst = new Instruction(line);
				instMap.put(inst.instruction, inst);   // instruction 필드는 Instruction.parsing()에서 채움
			}
		} catch (IOException e) {
            throw new RuntimeException(e);
        }

	}
	
	//get, set, search 등의 함수는 자유 구현

	/* ────────────────────── 새로 추가 ────────────────────── */

	public Instruction find(String instruction) {
		return instMap.get(instruction);
	}

	public boolean isExist(String instruction) {
		if (instruction == null) return false;
		return instMap.get(instruction.toUpperCase()) != null;
	}

	int getNumberOfOperand(String instruction) {
		return instMap.get(instruction.toUpperCase()).numberOfOperand;
	}

	int getFormat(String instruction) {
		return instMap.get(instruction.toUpperCase()).format;
	}

	/** BYTE·RESW 같은 어셈블러 지시어·제어어인지 판별 */
	private static final Set<String> DIRECTIVES =
			new HashSet<>(Arrays.asList(
					"START","END","CSECT","EXTDEF","EXTREF",
					"BYTE","WORD","RESB","RESW","LTORG","EQU",
					"ORG","BASE","NOBASE"));

	public static boolean isDirective(String word) {
		return word != null && DIRECTIVES.contains(word.toUpperCase());
	}

}
/**
 * 명령어 하나하나의 구체적인 정보는 Instruction클래스에 담긴다.
 * instruction과 관련된 정보들을 저장하고 기초적인 연산을 수행한다.
 */
class Instruction {
	/* 
	 * 각자의 inst.data 파일에 맞게 저장하는 변수를 선언한다.
	 *  
	 * ex)
	 * String instruction;
	 * int opcode;
	 * int numberOfOperand;
	 * String comment;
	 */

	String instruction;
	int opcode;
	int numberOfOperand;

	/** instruction이 몇 바이트 명령어인지 저장. 이후 편의성을 위함 */
	int format;
	
	/**
	 * 클래스를 선언하면서 일반문자열을 즉시 구조에 맞게 파싱한다.
	 * @param line : instruction 명세파일로부터 한줄씩 가져온 문자열
	 */
	public Instruction(String line) {
		parsing(line);
	}
	
	/**
	 * 일반 문자열을 파싱하여 instruction 정보를 파악하고 저장한다.
	 * @param line : instruction 명세파일로부터 한줄씩 가져온 문자열
	 */
	public void parsing(String line) {
		// TODO Auto-generated method stub
		String[] tok = line.split("\\s+");
		instruction = tok[0];
		format = Integer.parseInt(tok[1]);
		opcode = Integer.parseInt(tok[2], 16);
		numberOfOperand = Integer.parseInt(tok[3]);
	}
		
	//그 외 함수 자유 구현
	
	
}

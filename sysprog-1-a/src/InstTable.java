import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;

import java.io.BufferedReader;
import java.util.HashSet;
import java.util.Set;

/**
 * ��� instruction�� ������ �����ϴ� Ŭ����. instruction data���� �����Ѵ�. <br>
 * ���� instruction ���� ����, ���� ��� ����� �����ϴ� �Լ�, ���� ������ �����ϴ� �Լ� ���� ���� �Ѵ�.
 */
public class InstTable {
	/** 
	 * inst.data ������ �ҷ��� �����ϴ� ����.
	 *  ��ɾ��� �̸��� ��������� �ش��ϴ� Instruction�� �������� ������ �� �ִ�.
	 */
	HashMap<String, Instruction> instMap;
	
	/**
	 * Ŭ���� �ʱ�ȭ. �Ľ��� ���ÿ� ó���Ѵ�.
	 * @param instFile : instuction�� ���� ���� ����� ���� �̸�
	 */
	public InstTable(String instFile) {
		instMap = new HashMap<String, Instruction>();
		openFile(instFile);
	}
	
	/**
	 * �Է¹��� �̸��� ������ ���� �ش� ������ �Ľ��Ͽ� instMap�� �����Ѵ�.
	 */
	public void openFile(String fileName) {

		// InstTable.openFile()
		try (BufferedReader br = Files.newBufferedReader(Paths.get(fileName))) {
			String line;
			while ((line = br.readLine()) != null) {
				line = line.trim();
				if (line.isEmpty()) continue;
				Instruction inst = new Instruction(line);
				instMap.put(inst.instruction, inst);   // instruction �ʵ�� Instruction.parsing()���� ä��
			}
		} catch (IOException e) {
            throw new RuntimeException(e);
        }

	}
	
	//get, set, search ���� �Լ��� ���� ����

	/* �������������������������������������������� ���� �߰� �������������������������������������������� */

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

	/** BYTE��RESW ���� ����� ���þ��������� �Ǻ� */
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
 * ��ɾ� �ϳ��ϳ��� ��ü���� ������ InstructionŬ������ ����.
 * instruction�� ���õ� �������� �����ϰ� �������� ������ �����Ѵ�.
 */
class Instruction {
	/* 
	 * ������ inst.data ���Ͽ� �°� �����ϴ� ������ �����Ѵ�.
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

	/** instruction�� �� ����Ʈ ��ɾ����� ����. ���� ���Ǽ��� ���� */
	int format;
	
	/**
	 * Ŭ������ �����ϸ鼭 �Ϲݹ��ڿ��� ��� ������ �°� �Ľ��Ѵ�.
	 * @param line : instruction �����Ϸκ��� ���پ� ������ ���ڿ�
	 */
	public Instruction(String line) {
		parsing(line);
	}
	
	/**
	 * �Ϲ� ���ڿ��� �Ľ��Ͽ� instruction ������ �ľ��ϰ� �����Ѵ�.
	 * @param line : instruction �����Ϸκ��� ���پ� ������ ���ڿ�
	 */
	public void parsing(String line) {
		// TODO Auto-generated method stub
		String[] tok = line.split("\\s+");
		instruction = tok[0];
		format = Integer.parseInt(tok[1]);
		opcode = Integer.parseInt(tok[2], 16);
		numberOfOperand = Integer.parseInt(tok[3]);
	}
		
	//�� �� �Լ� ���� ����
	
	
}

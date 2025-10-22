import java.util.ArrayList;

/**
 * symbol�� ���õ� �����Ϳ� ������ �����Ѵ�.
 * section ���� �ϳ��� �ν��Ͻ��� �Ҵ��Ѵ�.
 */
public class SymbolTable {
	ArrayList<String> symbolList;
	ArrayList<Integer> locationList;
	ArrayList<String> nameList;

	public SymbolTable() {
		this.symbolList   = new ArrayList<>();
		this.locationList = new ArrayList<>();
		this.nameList = new ArrayList<>();
	}

	/**
	 * ���ο� Symbol�� table�� �߰��Ѵ�.
	 * @param symbol : ���� �߰��Ǵ� symbol�� label
	 * @param location : �ش� symbol�� ������ �ּҰ�
	 * @param name : �ش� symbol�� ������ CSECT�� ���α׷� �̸�
	 *
	 * <br><br>
	 * ���� : ���� �ߺ��� symbol�� putSymbol�� ���ؼ� �Էµȴٸ� �̴� ���α׷� �ڵ忡 ������ ������ ��Ÿ����. 
	 * ��Ī�Ǵ� �ּҰ��� ������ modifySymbol()�� ���ؼ� �̷������ �Ѵ�.
	 */
	public void putSymbol(String symbol, int location, String name) {
		if (symbol == null) return;                   // ��� �ڵ�
		if (searchIndex(symbol) >= 0) {
			throw new IllegalStateException("duplicate symbol: " + symbol);
		}
		symbolList.add(symbol);
		locationList.add(location);
		nameList.add(name);
	}

	/**
	 * ������ �����ϴ� symbol ���� ���ؼ� ����Ű�� �ּҰ��� �����Ѵ�.
	 * @param symbol : ������ ���ϴ� symbol�� label
	 * @param newLocation : ���� �ٲٰ��� �ϴ� �ּҰ�
	 */
	public void modifySymbol(String symbol, int newLocation) {

		int idx = searchIndex(symbol);
		if (idx < 0) {
			throw new IllegalStateException("not found symbol: " + symbol);
		}
		locationList.set(idx, newLocation);

	}

	/**
	 * ���ڷ� ���޵� symbol�� � �ּҸ� ��Ī�ϴ��� �˷��ش�. 
	 * @param symbol : �˻��� ���ϴ� symbol�� label
	 * @return symbol�� ������ �ִ� �ּҰ�. �ش� symbol�� ���� ��� -1 ����
	 */
	public int searchSymbol(String symbol) {
		int address = 0;

		int idx = searchIndex(symbol);
		address = (idx >= 0) ? locationList.get(idx) : -1;

		return address;
	}

	/** symbolList ���� �ε��� �˻� (������ -1) */
	private int searchIndex(String symbol) {
		for (int i = 0; i < symbolList.size(); i++) {
			if (symbolList.get(i).equalsIgnoreCase(symbol)) {
				return i;
			}
		}
		return -1;
	}

}

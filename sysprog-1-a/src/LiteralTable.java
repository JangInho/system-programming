import java.util.ArrayList;


public class LiteralTable {

    ArrayList<String> literalList;
    ArrayList<Integer> locationList;

    public LiteralTable() {
        this.literalList = new ArrayList<>();
        this.locationList = new ArrayList<>();
    }

    public void putLiteral(String literal, Integer location) {

        // 이미 같은 리터럴이 들어있으면 무시
        if (literalList.contains(literal)) {
            return;
        }

        literalList.add(literal);
        locationList.add(location);
    }

    /**
     * 이미 들어있는 리터럴의 위치를 갱신한다.
     * @param literal      수정할 리터럴 (예: "=C'EOF'")
     * @param newLocation  새로 할당된 주소값
     * @throws IllegalArgumentException 해당 리터럴이 없으면
     */
    public void modifyLiteral(String literal, Integer newLocation) {
        int idx = literalList.indexOf(literal);
        if (idx < 0) {
            throw new IllegalArgumentException("not found literal: " + literal);
        }
        locationList.set(idx, newLocation);
    }

    // (선택) 리터럴의 주소 조회
    public int searchLiteral(String literal) {
        int idx = literalList.indexOf(literal);
        return (idx >= 0 ? locationList.get(idx) : -1);
    }



    // 필요 메서드 추가 구현
}

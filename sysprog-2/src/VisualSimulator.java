import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

/**
 * VisualSimulator는 사용자와의 상호작용을 담당한다. 즉, 버튼 클릭등의 이벤트를 전달하고 그에 따른 결과값을 화면에 업데이트 하는 역할을 수행한다. 실제적인 작업은 SicSimulator에서
 * 수행하도록 구현한다.
 */
public class VisualSimulator extends JFrame {

    /**
     * 과제에서 주어진 변수
     */
    ResourceManager resourceManager = new ResourceManager();

    SicLoader sicLoader = new SicLoader(resourceManager);

    SicSimulator sicSimulator = new SicSimulator(resourceManager);

    /**
     * form을 사용하여 자동 생성된 UI 객체들
     */
    public static JFrame mainFrame;

    private JPanel mainPanel;

    private JTextField fileNameField;

    private JButton openButton;

    private JTextArea logTextArea;

    private JTextField aDecField, aHexField;

    private JTextField xDecField, xHexField;

    private JTextField lDecField, lHexField;

    private JTextField pcDecField, pcHexField;

    private JTextField bDecField, sDecField, tDecField;

    private JTextField bHecField, sHexField, tHexField;

    private JTextField swHexField, fHexField;

    private JTextField firstInstructionField;

    private JTextField startAddressInMemoryField;

    private JTextField deviceField;

    private JButton oneStepButton, allButton, exitButton;

    private JTextField targetAddressField;

    private JTextField programNameField, startAddressField, lengthOfProgramField;

    private JPanel instructionPanel;

    // form 외에 추가: 리스트 모델
    private DefaultListModel<String> instModel;

    private JList<String> instructionList;

    /**
     * VisualSimulator 생성자 GUI 창을 생성하고 띄운다.
     */
    public VisualSimulator(String title) {
	super(title);
	this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	this.setContentPane(mainPanel);
	this.pack();
	this.setResizable(false);
	setInstructionPanel(); // JList 세팅
	addListener(); // 버튼 바인딩
	this.setVisible(true);
    }

    /**
     * 메인 함수
     */
    public static void main(String[] args) {
	mainFrame = new VisualSimulator("SIC/XE Simulator");
    }

    /**
     * form으로 설정이 어려운 instruction List만 따로 설정함
     */
    private void setInstructionPanel() {
	instModel = new DefaultListModel<>();
	instructionList = new JList<>(instModel);
	instructionList.setVisibleRowCount(8);
	instructionPanel.add(new JScrollPane(instructionList));
    }

    /**
     * 버튼 이벤트 바인딩
     */
    private void addListener() {
	openButton.addActionListener(new onOpenButton());
	oneStepButton.addActionListener(new onOneStepButton());
	allButton.addActionListener(new onAllButton());
	exitButton.addActionListener(new onExitButton());
    }

    /**
     * open 버튼 눌렀을 때 파일 선택하고 load()
     */
    private class onOpenButton implements ActionListener {

	@Override
	public void actionPerformed(ActionEvent e) {
	    FileDialog dialog = new FileDialog(VisualSimulator.mainFrame, "Select a file", FileDialog.LOAD);
	    dialog.setDirectory("./");
	    dialog.setVisible(true);
	    String fileName = dialog.getFile();
	    String path = dialog.getDirectory() + fileName;
	    if (fileName != null) {
		try {
		    load(new File(path));
		    fileNameField.setText(fileName);
		} catch (IOException ex) {
		    throw new RuntimeException("Error loading file", ex);
		}
	    }
	}
    }

    /**
     * 원스텝 버튼 눌렀을 떄 명령어 1개 실행
     */
    private class onOneStepButton implements ActionListener {

	@Override
	public void actionPerformed(ActionEvent e) {
	    try {
		oneStep();
	    } catch (IOException ex) {
		throw new RuntimeException("Error one Step button", ex);
	    }
	}
    }

    /**
     * All 버튼 눌렀을 때 명령어 모두 실행
     */
    private class onAllButton implements ActionListener {

	@Override
	public void actionPerformed(ActionEvent e) {
	    try {
		allStep();
	    } catch (IOException ex) {
		throw new RuntimeException("Error all Step button", ex);
	    }
	}
    }

    /**
     * Exit 버튼 눌렀을 때
     */
    private class onExitButton implements ActionListener {

	@Override
	public void actionPerformed(ActionEvent e) {
	    resourceManager.closeDevice();
	    System.exit(0);
	}
    }

    /**
     * 프로그램 로드 명령을 전달한다.
     */
    public void load(File program) throws IOException {
	sicSimulator.load(program);
	sicLoader.load(program);
	setInfo();           // 고정 필드 세팅 + 첫 update()
    }

    /**
     * 하나의 명령어만 수행할 것을 SicSimulator에 요청한다.
     */
    public void oneStep() throws IOException {
	sicSimulator.oneStep();
	update();

	//  종료(PC==0) 면 버튼 비활성화+디바이스 close
	if (resourceManager.getRegister(8) == 0) {
	    oneStepButton.setEnabled(false);
	    allButton.setEnabled(false);
	    resourceManager.closeDevice();
	}
    }

    /**
     * 남아있는 모든 명령어를 수행할 것을 SicSimulator에 요청한다.
     */
    public void allStep() throws IOException {
	while (sicSimulator.allStep()) {
	    update();
	}
	oneStepButton.setEnabled(false);
	allButton.setEnabled(false);
	resourceManager.closeDevice();
	update();
    }

    /**
     * 화면을 최신값으로 갱신하는 역할을 수행한다.
     */
    public void update() {
	// 레지스터 값
	int A = resourceManager.getRegister(0);
	int X = resourceManager.getRegister(1);
	int L = resourceManager.getRegister(2);
	int B = resourceManager.getRegister(3);
	int S = resourceManager.getRegister(4);
	int T = resourceManager.getRegister(5);
	int PC = resourceManager.getRegister(8);
	int SW = resourceManager.getRegister(9);
	aDecField.setText(String.valueOf(A));
	aHexField.setText(String.format("%06X", A));
	xDecField.setText(String.valueOf(X));
	xHexField.setText(String.format("%06X", X));
	lDecField.setText(String.valueOf(L));
	lHexField.setText(String.format("%06X", L));
	pcDecField.setText(String.valueOf(PC));
	pcHexField.setText(String.format("%06X", PC));
	swHexField.setText(String.format("%06X", SW));
	bDecField.setText(String.valueOf(B));
	bHecField.setText(String.format("%06X", B));
	sDecField.setText(String.valueOf(S));
	sHexField.setText(String.format("%06X", S));
	tDecField.setText(String.valueOf(T));
	tHexField.setText(String.format("%06X", T));
	fHexField.setText("000000");

	// 명령어 Device TargetAddr */
	targetAddressField.setText(sicSimulator.instLuncher.targetAddr);
	deviceField.setText(sicSimulator.instLuncher.currDevice);
	startAddressInMemoryField.setText(String.format("%06X", sicSimulator.getStartAddress()));

	//  로그 (traceLog 의 마지막 줄)
	var trace = sicSimulator.getTrace();
	if (!trace.isEmpty()) {
	    String last = trace.get(trace.size() - 1);
	    logTextArea.append(last + "\n");
	    logTextArea.setCaretPosition(logTextArea.getDocument().getLength());
	}

	// instruction 리스트
	String raw = sicSimulator.getLastInst();
	if (!raw.isEmpty()) {
	    instModel.addElement(raw);
	    instructionList.setSelectedIndex(instModel.size() - 1);
	}
    }

    /** 로드 직후 한 번만 채우는 고정 정보 */
    private void setInfo() {
	programNameField.setText(sicLoader.programName);
	startAddressField.setText(String.format("%06X", sicLoader.programStartAddress));
	lengthOfProgramField.setText(String.format("%06X", sicLoader.programTotalLength));
	firstInstructionField.setText(String.format("%06X", sicLoader.firstInstruction));

	// 버튼 활성화 + 화면 1회 업데이트
	oneStepButton.setEnabled(true);
	allButton.setEnabled(true);
	logTextArea.setText("");
	instModel.clear();
	update();
    }
}

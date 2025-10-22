/* 
 * my_assembler 함수를 위한 변수 선언 및 매크로를 담고 있는 헤더 파일이다. 
 * 
 */
#define MAX_INST 256
#define MAX_LINES 5000

#define MAX_COLUMNS 4
#define MAX_OPERAND 3


 /*
 * instruction 목록을 저장하는 구조체이다.
 * instruction 목록 파일로부터 정보를 받아와서 생성한다.
 * instruction 목록 파일에는 라인별로 하나의 instruction을 저장한다.
 * 
 */
typedef struct _inst
{
	char str[10];
	unsigned char op;
	int format;
	int ops;
} inst;


// 기계어를 관리하는 테이블
inst* inst_table[MAX_INST];
int inst_index;


// 어셈블리 할 소스코드를 파일로부터 불러와 라인별로 관리하는 테이블
char* input_data[MAX_LINES];
static int line_num;

int label_num ; 


/*
 * 어셈블리 할 소스코드를 토큰으로 변환하여 저장하는 구조체 변수이다.
 * operator 변수명은 renaming을 허용한다.
 */
typedef struct _token {
	char *label;
	char *operator;
	char *operand[MAX_OPERAND];
	char comment[100];
	// char nixbpe;    // 추후 프로젝트에서 사용
} token;

// 어셈블리 할 소스코드를 5000라인까지 관리하는 테이블
token* token_table[MAX_LINES] ;
static int token_line;

/*
 * 심볼을 관리하는 구조체이다.
 * 심볼 테이블은 심볼 이름, 심볼의 위치로 구성된다.
 * 추후 프로젝트에서 사용 예정
 */
typedef struct _symbol
{
	char symbol[10];
	int addr;
} symbol;

/*
* 리터럴을 관리하는 구조체이다.
* 리터럴 테이블은 리터럴의 이름, 리터럴의 위치로 구성된다.
* 추후 프로젝트에서 사용 예정
*/
typedef struct _literal {
	char* literal;
	int addr;
} literal;

symbol sym_table[MAX_LINES];
symbol literal_table[MAX_LINES];


/**
 * 오브젝트 코드 전체에 대한 정보를 담는 구조체이다.
 * Header Record, Define Recode,
 * Modification Record 등에 대한 정보를 모두 포함하고 있어야 한다. 이
 * 구조체 변수 하나만으로 object code를 충분히 작성할 수 있도록 구조체를 직접
 * 정의해야 한다.
 * 
 * 추후 프로젝트에서 사용 예정
 */
typedef struct _object_code {
	/* add fields */
} object_code;


static int locctr;
//--------------

static char* input_file;
static char* output_file;

int init_my_assembler(void);
int init_inst_file(char* inst_file);
int init_input_file(char* input_file);
int token_parsing(char* str);
int search_opcode(char* str);
static int assem_pass1(void);
void make_opcode_output(char* file_name);
void make_symtab_output(char* file_name);
void make_literaltab_output(char* filename);
static int assem_pass2(void);
void make_objectcode_output(char* file_name);

void print_inst_table(void);
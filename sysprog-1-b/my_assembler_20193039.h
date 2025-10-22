/*
 * my_assembler 함수를 위한 변수 선언 및 매크로를 담고 있는 헤더 파일이다.
 *
 */
#define MAX_INST 256
#define MAX_LINES 5000
#define MAX_OPERAND 3

// NEW
/*
 * nixbpe를 처리하기 위한 비트 플래그
 */
#define N 0x20  // 0010 0000
#define I 0x10  // 0001 0000
#define X 0x08  // 0000 1000
#define B 0x04  // 0000 0100
#define P 0x02  // 0000 0010
#define E 0x01  // 0000 0001

#define MAX_OBJ_LENGTH 60  // 한 줄에 60자리 (30바이트까지 허용)

 /*
  * instruction 목록 파일로 부터 정보를 받아와서 생성하는 구조체 변수이다.
  * 라인 별로 하나의 instruction을 저장한다.
  */
typedef struct _inst
{
	char str[10];
	unsigned char op;
	int format;
	int ops;
} inst;

inst* inst_table[MAX_INST];
int inst_index;

/*
 * 어셈블리 할 소스코드를 입력받는 테이블이다. 라인 단위로 관리할 수 있다.
 */
char* input_data[MAX_LINES];
static int line_num;

/*
 * 어셈블리 할 소스코드를 토큰단위로 관리하기 위한 구조체 변수이다.
 * operator는 renaming을 허용한다.
 */
typedef struct _token
{
	char* label;
	char* operator;
	char* operand[MAX_OPERAND];
	char comment[100];
	char nixbpe;
} token;

token* token_table[MAX_LINES];
static int token_line;

// NEW
void init_sym_table();
void set_literal_address();
void init_loc_table();
void init_nixbpe();
int loc_table[MAX_LINES];  // token_table[i] 에 해당하는 LOCCTR 저장

/*
 * 심볼을 관리하는 구조체이다.
 * 심볼 테이블은 심볼 이름, 심볼의 위치로 구성된다.
 * 추후 과제에 사용 예정
 */
typedef struct _symbol
{
	char symbol[10];
	int addr;
} symbol;

/*
* 리터럴을 관리하는 구조체이다.
* 리터럴 테이블은 리터럴의 이름, 리터럴의 위치로 구성된다.
* 추후 과제에 사용 예정
*/
typedef struct _literal {
	char* literal;
	int addr;
} literal;

symbol sym_table[MAX_LINES];
literal literal_table[MAX_LINES];

// NEW
static int sym_line;
static int literal_line;

int literal_inserted[MAX_LINES] = {0};  // 토큰테이블에 리터럴이 들어갔는지 확인하는 플래그

/**
 * 오브젝트 코드 전체에 대한 정보를 담는 구조체이다.
 * Header Record, Define Recode,
 * Modification Record 등에 대한 정보를 모두 포함하고 있어야 한다. 이
 * 구조체 변수 하나만으로 object code를 충분히 작성할 수 있도록 구조체를 직접
 * 정의해야 한다.
 */

typedef struct _object_code {
	/* add fields */

	// Header Record
    char name[10];
    int start_addr;
    int length;

    // Define Record (D)
    char def_symbols[10][10];
    int def_addresses[10];
    int def_count;

    // Refer Record (R)
    char ref_symbols[10][10];
    int ref_count;

    // Modification Record (M)
    struct {
        int addr;
        int length; 
        char sign; 
        char symbol[10];
    } m_records[100];
    int m_count;

    // Text Record (T)
    struct {
        int start_addr;
        int length; // byte size
        char code[MAX_OBJ_LENGTH]; // hex string
    } t_records[100];
    int t_count;

    // End Record
    int exec_start_addr;

} object_code;

static int locctr;

// NEW
static object_code obj_list[10];
static int csect_index = -1;

// NEW
int objcode_table[100]; // token_table의 index 와 동일하고, objcode를 저장해둠

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

// NEW
void insert_literal_pool();
void add_literal_table(char* lit);
int search_symbol(char* label);
int search_literal(char* label);
int get_register_number(char* reg);
int generate_object_code(token* t, int i);
int find_csect_end(int start_idx);
void init_obj_list();
void parse_operand_expr(const char *expr, char *lhs, char *op, char *rhs);
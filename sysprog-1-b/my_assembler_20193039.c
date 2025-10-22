/*
 * 파일명 : my_assembler_20193039.c 
 * 설  명 : 이 프로그램은 SIC/XE 머신을 위한 간단한 Assembler 프로그램의 메인루틴으로,
 * 입력된 파일의 코드 중, 명령어에 해당하는 OPCODE를 찾아 출력한다.
 * 파일 내에서 사용되는 문자열 "00000000"에는 자신의 학번을 기입한다.
 */

/*
 *
 * 프로그램의 헤더를 정의한다. 
 *
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <fcntl.h>

#include <ctype.h>

// 파일명의 "00000000"은 자신의 학번으로 변경할 것.
#include "my_assembler_20193039.h"

/* ----------------------------------------------------------------------------------
 * 설명 : 사용자로 부터 어셈블리 파일을 받아서 명령어의 OPCODE를 찾아 출력한다.
 * 매계 : 실행 파일, 어셈블리 파일 
 * 반환 : 성공 = 0, 실패 = < 0 
 * 주의 : 현재 어셈블리 프로그램의 리스트 파일을 생성하는 루틴은 만들지 않았다. 
 *		   또한 중간파일을 생성하지 않는다. 
 * ----------------------------------------------------------------------------------
 */
int main(int args, char *arg[])
{

	// 문제
	// 1. 심볼테이블 잘 안나옴 CSECT 만나고 0이 안나옴 -> ok
	// 2. tokentable에 마지막에 end 끝에 리터럴 다 들어감 -> ok
	// 3. literal table 안맞음 -> ok
	// 4. maxlen 처리 -> 나중에 필요하면 처리
	// 5. nibpe -> ok
	// 6. pass2 ㄱㄱ
	// opcode 찾기
	// nixbpe 비트 조합
	// operand 주소 계산
	// object code 구성 -> ok
	// 7. 4형식 비트수 맞추기
	// 8. obj 만들기
	// 9. obj 만들기 전에 CSECT 기준으로 obj_code 3개 만들기 
	// 10. 2형식 6자리로 출력
	// 11. maxlen 처리
	// 12. EXTREF, EXTDEF operand 처리 ok

	if (init_my_assembler() < 0)
	{
		printf("init_my_assembler: 프로그램 초기화에 실패 했습니다.\n");
		return -1;
	}

	if (assem_pass1() < 0)
	{
		printf("assem_pass1: 패스1 과정에서 실패하였습니다.  \n");
		return -1;
	}

	make_symtab_output("output_symtab.txt");
	make_literaltab_output("output_littab.txt");

	if (assem_pass2() < 0)
	{
		printf(" assem_pass2: 패스2 과정에서 실패하였습니다.  \n");
		return -1;
	}

	make_objectcode_output("output_objectcode.txt");
	
	return 0;
}

/* ----------------------------------------------------------------------------------
 * 설명 : 프로그램 초기화를 위한 자료구조 생성 및 파일을 읽는 함수이다. 
 * 매계 : 없음
 * 반환 : 정상종료 = 0 , 에러 발생 = -1
 * 주의 : 각각의 명령어 테이블을 내부에 선언하지 않고 관리를 용이하게 하기 
 *		   위해서 파일 단위로 관리하여 프로그램 초기화를 통해 정보를 읽어 올 수 있도록
 *		   구현하였다. 
 * ----------------------------------------------------------------------------------
 */
int init_my_assembler(void)
{
	int result;

	if ((result = init_inst_file("inst_table.txt")) < 0)
		return -1;
	if ((result = init_input_file("input.txt")) < 0)
		return -1;
	return result;
}

/* ----------------------------------------------------------------------------------
 * 설명 : 머신을 위한 기계 코드목록 파일(inst_table.txt)을 읽어 
 *       기계어 목록 테이블(inst_table)을 생성하는 함수이다. 
 *
 * 
 * 매계 : 기계어 목록 파일
 * 반환 : 정상종료 = 0 , 에러 < 0 
 * 주의 : 기계어 목록파일 형식은 자유롭게 구현한다. 예시는 다음과 같다.
 *	
 *	===============================================================================
 *		   | 이름 | 형식 | 기계어 코드 | 오퍼랜드의 갯수 | \n |
 *	===============================================================================	   
 *		
 * ----------------------------------------------------------------------------------
 */
int init_inst_file(char *inst_file)
{
	FILE *file;
	int errno;

	/* add your code here */

	file = fopen(inst_file, "r");
	if(file == NULL) {
		perror("inst_table.txt 파일 열기 실패");
        return -1;
	}

	// 한 줄을 읽어올 버퍼
	char buffer[100];
    inst_index = 0;
	

	while (fgets(buffer, sizeof(buffer), file) != NULL) {
        if (inst_index >= MAX_INST) {
            printf("inst_table 크기를 초과했습니다.\n");
            break;
        }

        inst* new_inst = (inst*)malloc(sizeof(inst));
        if (new_inst == NULL) {
            perror("메모리 할당 실패");
            fclose(file);
            return -1;
        }

        char opcode_str[10];
        int format, opcode, ops;
        int count = sscanf(buffer, "%s %d %x %d", opcode_str, &format, &opcode, &ops);
        if (count != 4) {
            printf("inst_table.txt 형식 오류: %s\n", buffer);
            free(new_inst);
            continue;
        }

        strcpy(new_inst->str, opcode_str);
        new_inst->format = format;
        new_inst->op = (unsigned char)opcode;
        new_inst->ops = ops;

        inst_table[inst_index++] = new_inst;
    }

	errno = 0;
	return errno;
}

/* ----------------------------------------------------------------------------------
 * 설명 : 어셈블리 할 소스코드를 읽어 소스코드 테이블(input_data)를 생성하는 함수이다. 
 * 매계 : 어셈블리할 소스파일명
 * 반환 : 정상종료 = 0 , 에러 < 0  
 * 주의 : 라인단위로 저장한다.
 *		
 * ----------------------------------------------------------------------------------
 */
int init_input_file(char *input_file)
{
	FILE *file;
	int errno;

	/* add your code here */

	file = fopen(input_file, "r");
	if (file == NULL) {
        perror("input.txt 파일 열기 실패");
        return -1;
    }
	char buffer[200]; // 한 줄을 담을 버퍼
    line_num = 0;
	
	while (fgets(buffer, sizeof(buffer), file) != NULL) {
        if (line_num >= MAX_LINES) {
            printf("input_data 크기를 초과했습니다.\n");
            break;
        }

        // 줄 끝 개행 문자 제거 (선택사항)
        buffer[strcspn(buffer, "\n")] = '\0';

        input_data[line_num] = (char*)malloc(strlen(buffer) + 1);
        if (input_data[line_num] == NULL) {
            perror("input_data 메모리 할당 실패");
            fclose(file);
            return -1;
        }

        strcpy(input_data[line_num], buffer);
		
        line_num++;
    }

	fclose(file);

	errno = 0;
	return errno;
}

/* ----------------------------------------------------------------------------------
 * 설명 : 소스 코드를 읽어와 토큰단위로 분석하고 토큰 테이블을 작성하는 함수이다. 
 *        패스 1로 부터 호출된다. 
 * 매계 : 파싱을 원하는 문자열  
 * 반환 : 정상종료 = 0 , 에러 < 0 
 * 주의 : my_assembler 프로그램에서는 라인단위로 토큰 및 오브젝트 관리를 하고 있다. 
 * ----------------------------------------------------------------------------------
 */
int token_parsing(char *str)
{
	/* add your code here */

	// strtok은 원본을 바꾸기 때문에 버퍼 사용
	char buffer[200];
	strcpy(buffer, str);

	token* new_token = (token*)malloc(sizeof(token));
	if (new_token == NULL) {
		perror("토큰 메모리 할당 실패");
		return -1;
	}

	// malloc의 쓰레기값 방지를 위한 초기화
	new_token->label = NULL;
	new_token->operator = NULL;
	for (int i = 0; i < MAX_OPERAND; i++) new_token->operand[i] = NULL;
	new_token->comment[0] = '\0';
	
	char* token = strtok(buffer, " \t\n\r");

	// 첫번째 토큰이 주석인 경우
	// TODO: 100 보다 클 때 에러처리 해야할 것 같음
	if (strcmp(token, ".") == 0 || str[0] == '.') {
		return 0;
	}
	
	/*
	* 첫번째 토큰 경우의 수
	* . 주석
	* 명령어
	* 지시어
	* 레이블
	*/
    if (token != NULL) {

        // 첫 번째 토큰이 명령어나 지시어일 수도 있으니 미리 검사
        if (search_opcode(token) >= 0 || 
			strcmp(token, "START") == 0 || 
			strcmp(token, "WORD") == 0 ||
			strcmp(token, "BYTE") == 0 ||
			strcmp(token, "RESW") == 0 || 
			strcmp(token, "RESB") == 0 || 
			strcmp(token, "EQU") == 0 ||
			strcmp(token, "END") == 0 ||
			strcmp(token, "EXTDEF") == 0 ||
			strcmp(token, "EXTREF") == 0 ||
			strcmp(token, "LTORG") == 0 ||
			strcmp(token, "CSECT") ==0) {
            // label 없음
            new_token->operator = strdup(token);
        } else {
            new_token->label = strdup(token);
            token = strtok(NULL, " \t\n\r");
            if (token != NULL) {
                new_token->operator = strdup(token);
            }
        }

		int op_idx = 0;
		token = strtok(NULL, " \t\n\r");
		int expected_ops = -1;
		
		// 1. 명령어라면 inst_table에서 operand 개수 확인
		int target_index = search_opcode(new_token->operator);
		
		if (target_index >= 0) {
			expected_ops = inst_table[target_index]->ops;
		} 
		// 2. 지시어라면 하드코딩
		else if (strcmp(new_token->operator, "START") == 0 ||
				strcmp(new_token->operator, "WORD") == 0 ||
				strcmp(new_token->operator, "BYTE") == 0 ||
				strcmp(new_token->operator, "RESW") == 0 ||
				strcmp(new_token->operator, "RESB") == 0 ||
				strcmp(new_token->operator, "EQU") == 0 ||
				strcmp(new_token->operator, "END") == 0 || 
				strcmp(new_token->operator, "EXTDEF") == 0 ||
				strcmp(new_token->operator, "EXTREF") == 0) {
			expected_ops = 1;
		 } else if (strcmp(new_token->operator, "LTORG") == 0 ||
		 			strcmp(new_token->operator, "CSECT") == 0
		 ) {
			expected_ops = 0;
		}
		

		// TODO: 개선이 필요함
		// if (expected_ops == 1) {

		// 	// EXTREF, EXTDEF 의 경우 콤마가 몇개 나올지 몰라서 그냥 반복문 처리 
		// 	// token 에 원본이 들어있습니다.
		// 	char *ops = strdup(token);        
		// 	char *p = strtok(ops, ",");
		// 	int opcnt = 0;
		// 	// MAX_OPERAND 까지만 처리
		// 	while (p != NULL && opcnt < MAX_OPERAND) {
		// 		new_token->operand[opcnt++] = strdup(p);
		// 		p = strtok(NULL, ",");
		// 	}
		// 	free(ops);

		// 	// 리터럴 발견 시 테이블 넣기
		// 	if (new_token->operand[0][0] == '=') {

		// 		add_literal_table(new_token->operand[0]);
		// 	}

		// }

		if (expected_ops == 1) {
        /* EXTREF, EXTDEF 등은 콤마 구분 */
        char *ops = strdup(token);        
        char *p = strtok(ops, ",");
        int opcnt = 0;
        while (p != NULL && opcnt < MAX_OPERAND) {
            new_token->operand[opcnt++] = strdup(p);
            p = strtok(NULL, ",");
        }
        free(ops);

        /* ------------------ [MODIFIED] 리터럴 처리 ------------------ */
        if (new_token->operand[0] != NULL) {
            /* #숫자 immediate → =숫자 리터럴 */
            if (new_token->operand[0][0] == '#' &&
                isdigit((unsigned char)new_token->operand[0][1])) {
                char buf[32];
                sprintf(buf, "=%s", new_token->operand[0] + 1); // ‘#’ 제거
                free(new_token->operand[0]);
                new_token->operand[0] = strdup(buf);
                add_literal_table(new_token->operand[0]);
            }
            /* 이미 =… 형태면 그대로 리터럴 테이블에 추가 */
            else if (new_token->operand[0][0] == '=') {
                add_literal_table(new_token->operand[0]);
            }
        }
        /* ----------------------------------------------------------------- */

    } else if (expected_ops == 2) {
			char* save;
			char* temp_token = strtok_r(token, ",", &save);
			if(temp_token != NULL) {
				new_token->operand[0] = strdup(temp_token);
				temp_token = strtok_r(NULL, ",", &save);
				new_token->operand[1] = strdup(temp_token);
			}
		}

		token = strtok(NULL, " \t\n\r");
		
		if (token != NULL) {
			char temp_comment[100] = {0};
			strcat(temp_comment, token);
			while((token = strtok(NULL, " \t\n\r")) != NULL) {
				strcat(temp_comment, " ");
        		strcat(temp_comment, token);
			}
			strncpy(new_token->comment, temp_comment, sizeof(new_token->comment) - 1);
		}
        
    }

	token_table[token_line] = new_token;
	token_line++;
	
	if (strcmp(new_token->operator, "LTORG") == 0 || strcmp(new_token->operator, "END") == 0) {
		insert_literal_pool(); 
	}

	return 0;
}

// NEW
/* ----------------------------------------------------------------------------------
 * 설명 : 리터럴 테이블에 리터럴을 저장하는 함수이다.
 * 매계 : 리터럴 ex) =C'test'
 * 반환 : 없음
 * 주의 : 
 *		
 * ----------------------------------------------------------------------------------
 */
void add_literal_table(char* lit) {

	for (int i = 0; i < literal_line; i++) {
        if (strcmp(literal_table[i].literal, lit) == 0)
            return;
    }

	literal_table[literal_line].literal = strdup(lit);
	literal_table[literal_line].addr = -1; // 아직 주소 없음
	literal_line++; 
}

// NEW
/* ----------------------------------------------------------------------------------
 * 설명 : 리터럴 테이블에 있는 리터럴들을 token_table의 LTORG 다음 위치에 저장하는 함수이다.
 * 매계 : 
 * 반환 : 없음
 * 주의 : 
 *		
 * ----------------------------------------------------------------------------------
 */
void insert_literal_pool() {

	for (int i = 0; i < literal_line; i++) {
        if (literal_inserted[i] != 1) {
            token* t = (token*)malloc(sizeof(token));
            t->label =  strdup(literal_table[i].literal);

            /* ---------- [MODIFIED] 숫자/문자 리터럴 구분 ---------- */
            if (literal_table[i].literal[1] == 'C' ||
                literal_table[i].literal[1] == 'X') {
                t->operator = strdup("BYTE");   /* 문자/16진수 상수 */
            } else {
                t->operator = strdup("WORD");   /* 순수 숫자 → 3바이트 */
            }
            /* ------------------------------------------------------- */

            t->operand[0] = strdup(literal_table[i].literal + 1); // ‘=’ 제거
            t->comment[0] = '\0';
            literal_inserted[i] = 1;
            token_table[token_line++] = t;
        }
    }

	// for (int i = 0; i < literal_line; i++) {
	// 	if(literal_inserted[i] != 1) {
	// 		token* t = (token*)malloc(sizeof(token));
	// 		t->label =  strdup(literal_table[i].literal);
	// 		t->operator = strdup("BYTE");  // 리터럴 BYTE로 처리
	// 		t->operand[0] = strdup(literal_table[i].literal + 1); // +1은 = 제거
	// 		t->comment[0] = '\0';
	// 		literal_inserted[i] = 1;
	// 		token_table[token_line++] = t;
	// 	}
    // }

}

/* ----------------------------------------------------------------------------------
 * 설명 : 입력 문자열이 기계어 코드인지를 검사하는 함수이다. 
 * 매계 : 토큰 단위로 구분된 문자열 
 * 반환 : 정상종료 = 기계어 테이블 인덱스, 에러 < 0 
 * 주의 : 기계어 목록 테이블에서 특정 기계어를 검색하여, 해당 기계어가 위치한 인덱스를 반환한다.
 *        '+JSUB'과 같은 문자열에 대한 처리는 자유롭게 처리한다.
 *		
 * ----------------------------------------------------------------------------------
 */
int search_opcode(char *str)
{
	/* add your code here */

	if(str == NULL) {
		return -1;
	}

	int target_index = -1;

	for (int i = 0; i < inst_index; i++) {
		// 두 문자열이 같은 경우
		if(strcmp(str, inst_table[i]->str) == 0) {
			target_index = i;
		}
	}

	return target_index;
}

/* ----------------------------------------------------------------------------------
* 설명 : 어셈블리 코드를 위한 패스1과정을 수행하는 함수이다.
*		   패스1에서는..
*		   1. 프로그램 소스를 스캔하여 해당하는 토큰단위로 분리하여 프로그램 라인별 토큰
*		   테이블을 생성한다.
*          2. 토큰 테이블은 token_parsing()을 호출하여 설정한다.
*          3. assem_pass2 과정에서 사용하기 위한 심볼테이블 및 리터럴 테이블을 생성한다.
*
* 매계 : 없음
* 반환 : 정상 종료 = 0 , 에러 = < 0
* 주의 : 현재 초기 버전에서는 에러에 대한 검사를 하지 않고 넘어간 상태이다.
*	  따라서 에러에 대한 검사 루틴을 추가해야 한다.
*
* -----------------------------------------------------------------------------------
*/
static int assem_pass1(void)
{
	/* add your code here */

	/* input_data의 문자열을 한줄씩 입력 받아서 
	 * token_parsing()을 호출하여 _token에 저장
	 */

	token_line = 0;
	literal_line = 0; // 리터럴 테이블 index 초기화

	for (int i = 0; i < line_num; i++) {
		if (token_parsing(input_data[i]) < 0) {
			printf("토큰 파싱 에러 발생!");
		}
	}

	// locaiton counter 세팅
	init_loc_table();
	// literal table address만 세팅
	set_literal_address();
	// 심볼테이블 세팅
	init_sym_table();

	init_nixbpe();

	make_opcode_output("output_20193039.txt");

}

// NEW
void init_nixbpe() {
    for (int i = 0; i < token_line; i++) {
        token* t = token_table[i];

        // 명령어가 아닌 경우 스킵
        if (search_opcode(t->operator) < 0)
            continue;

        t->nixbpe = 0;

        // format 4 확인
        if (t->operator[0] == '+') {
            t->nixbpe |= E;
        }

        // operand가 immediate or indirect?
        if (t->operand[0] != NULL) {
            if (t->operand[0][0] == '#') {
                t->nixbpe |= I;
            } else if (t->operand[0][0] == '@') {
                t->nixbpe |= N;
            } else {
                // default: both n, i → simple addressing
                t->nixbpe |= N | I;
            }

            // 인덱싱?
            if (t->operand[1] != NULL && strcmp(t->operand[1], "X") == 0) {
                t->nixbpe |= X;
            }
        } else {
            // no operand → typically format 2 or no-op
            t->nixbpe |= N | I;
        }
    }
}

// NEW
void set_literal_address() {
	// C='EOF'를 리터럴 테이블에서 찾는다
	// 토큰테이블에서 찾는다
	// 인덱스 찾는다
	// loc_tab에서 찾는다
	int target_index = -1;
	for (int i = 0; i < literal_line; i++) {
	
		for (int k = 0; k < token_line; k++) {
			if (token_table[k]->label != NULL && strcmp(literal_table[i].literal, token_table[k]->label) == 0) {
				target_index = k;
				break;
			}
		}
		
		if (target_index != -1) {
			literal_table[i].addr = loc_table[target_index];
		}
		
	}
}


//NEW
void init_loc_table() {

	// locctr 초기 세팅
	if (strcmp(token_table[0]->operator, "START") == 0) {
		// strtol은 long 으로 변환 후 16진수로 변환
    	locctr = (int)strtol(token_table[0]->operand[0], NULL, 16);
	} else {
    	locctr = 0;
	}

	// label에 locctr 세팅
	for (int i = 0; i < token_line; i++) {
		token* token = token_table[i];

		if (strcmp(token->operator, "CSECT") == 0) {
			locctr = 0;  // CSECT 는 0으로 세팅
		} 

		loc_table[i] = locctr;
		
		// locctr 업데이트 
		if (strcmp(token->operator, "RESW") == 0) {
			locctr += 3 * atoi(token->operand[0]);  // 워드 수 × 3바이트 with ASCII to Integer
		} else if (strcmp(token->operator, "RESB") == 0) {
			locctr += atoi(token->operand[0]);      // 바이트 수만큼 증가
		} else if (strcmp(token->operator, "BYTE") == 0) {
			char *op = token->operand[0];
			if (op[0] == 'C') {
				locctr += strlen(op) - 3;  // 예: C'EOF' → 3글자
			} else if (op[0] == 'X') {
				locctr += (strlen(op) - 3) / 2;  // 예: X'F1' → 1바이트
			}
		} else if (strcmp(token->operator, "WORD") == 0) {
			locctr += 3;
		} else if (
			strcmp(token->operator, "START") == 0 || 
			strcmp(token->operator, "END") == 0 ||
			strcmp(token->operator, "EXTDEF") == 0 ||
			strcmp(token->operator, "EXTREF") == 0 ||
			strcmp(token->operator, "LTORG") == 0 ||
			strcmp(token->operator, "EQU") == 0 ||
			strcmp(token->operator, "CSECT") == 0) {
			locctr += 0; 
		} else if (token->operator[0] == '+') {  // Format 4
			locctr += 4;
		} else if (inst_table[search_opcode(token->operator)]->format == 2) { // Format 2
			locctr += 2;
		}else {
			locctr += 3;  // Format 3
		} 


		// printf("loc tab %s, %X\n", token_table[i]->operator,  loc_table[i]);
	}
	
}

// NEW 
/* ----------------------------------------------------------------------------------
* 설명 : location counter를 세팅하는 함수이다. 
* 매계 : 없음
* 반환 : 없음
* 주의 : loc_table은 token_table의 index를 따라간다.
* 
* -----------------------------------------------------------------------------------
*/
void init_sym_table() {

	sym_line = 0;

	// label에 locctr 세팅
	for (int i = 0; i < token_line; i++) {
		token* token = token_table[i];
		// label이 있다면
		if (token->label != NULL && strlen(token->label) > 0 && token->label[0] != '=') {

			// 심볼테이블에 추가
			strcpy(sym_table[sym_line].symbol, token->label);
    		sym_table[sym_line].addr = loc_table[i];

			if (strcmp(sym_table[sym_line].symbol, "MAXLEN") == 0) {
				sym_table[sym_line].addr = loc_table[i-1] - loc_table[i-2];
			}

			sym_line++;
		}

	}
}

/* ----------------------------------------------------------------------------------
* 설명 : 입력된 문자열의 이름을 가진 파일에 프로그램의 결과를 저장하는 함수이다.
*        
* 매계 : 생성할 오브젝트 파일명
* 반환 : 없음
* 주의 : 소스코드 명령어 앞에 OPCODE가 기록된 코드를 파일에 출력한다.
*        파일이 NULL값이 들어온다면 프로그램의 결과를 stdout으로 보내어
*        화면에 출력해준다.
*        프로젝트 1에서는 불필요하다.
 * 
* -----------------------------------------------------------------------------------
*/
void make_opcode_output(char *file_name)
{
	/* add your code here */

	FILE* out = NULL;

    if (file_name == NULL)
        out = stdout;
    else {
        out = fopen(file_name, "w");
        if (out == NULL) {
            perror("출력 파일 열기 실패");
            return;
        }
    }

    for (int i = 0; i < token_line; i++) {
        token* t = token_table[i];
		int idx = search_opcode(t->operator);

        // 1. LABEL
        if (t->label != NULL)
            fprintf(out, "%-10s", t->label);
        else
            fprintf(out, "%-10s", "");

        // 2. OPERATOR
        if (t->operator != NULL)
            fprintf(out, "%-10s", t->operator);
        else
            fprintf(out, "%-10s", "");

		// 3. OPERANDS
		int has_operand = 0;
		for (int j = 0; j < MAX_OPERAND; j++) {
			if (t->operand[j] != NULL) {
				has_operand = 1;
				fprintf(out, "%s", t->operand[j]);
				if (j < MAX_OPERAND - 1 && t->operand[j + 1] != NULL)
					fprintf(out, ",");
			}
		}

		// 줄 맞춤용 탭 추가 (오퍼랜드 없을 경우)
		if (!has_operand) {
			fprintf(out, "%-10s", "");
		}

        // 4. OPCODE (원래 comment 자리)
        if (t->operator != NULL) {
            if (idx >= 0) {
				fprintf(out, "\t%02X", inst_table[idx]->op);  // 뒤에 OPCODE 출력
			}
                
        }

        fprintf(out, "\n");
    }

    if (out != stdout)
        fclose(out);
}

/* ----------------------------------------------------------------------------------
* 설명 : 입력된 문자열의 이름을 가진 파일에 프로그램의 결과를 저장하는 함수이다.
*        여기서 출력되는 내용은 SYMBOL별 주소값이 저장된 TABLE이다.
* 매계 : 생성할 오브젝트 파일명 혹은 경로
* 반환 : 없음
* 주의 : 파일이 NULL값이 들어온다면 프로그램의 결과를 stdout으로 보내어
*        화면에 출력해준다.
*
* -----------------------------------------------------------------------------------
*/
void make_symtab_output(char *file_name)
{

	/* add your code here */
	FILE* out = NULL;

    if (file_name == NULL)
        out = stdout;
    else {
        out = fopen(file_name, "w");
        if (out == NULL) {
            perror("출력 파일 열기 실패");
            return;
        }
    }
	
	for(int i = 0; i < sym_line; i++) {
		fprintf(out, "%-10s ", sym_table[i].symbol);
		fprintf(out, "%X\n", sym_table[i].addr);	
	}
	

	if (out != stdout)
        fclose(out);

}


/* ----------------------------------------------------------------------------------
* 설명 : 입력된 문자열의 이름을 가진 파일에 프로그램의 결과를 저장하는 함수이다.
*        여기서 출력되는 내용은 LITERAL별 주소값이 저장된 TABLE이다.
* 매계 : 생성할 오브젝트 파일명
* 반환 : 없음
* 주의 : 파일이 NULL값이 들어온다면 프로그램의 결과를 stdout으로 보내어
*        화면에 출력해준다.
*
* -----------------------------------------------------------------------------------
*/
void make_literaltab_output(char* file_name)
{
	/* add your code here */
	FILE* out = NULL;

    if (file_name == NULL)
        out = stdout;
    else {
        out = fopen(file_name, "w");
        if (out == NULL) {
            perror("출력 파일 열기 실패");
            return;
        }
    }

	
	for (int i = 0; i < literal_line; i++) {

		char* lit = literal_table[i].literal;
    	// '가 반드시 두 개 있다고 가정: =C'EOF'
    	char* value = lit + 3;  // =C' -> 건너뛰기

		// 끝에 있는 '를 제거하기 위해 복사 출력
		int len = strlen(value);
		char temp[100] = {0};
		strncpy(temp, value, len - 1);  // 마지막 ' 제외

		fprintf(out, "%-10s %X\n", temp, literal_table[i].addr);
	}

	if (out != stdout)
        fclose(out);

}


/* ----------------------------------------------------------------------------------
* 설명 : 어셈블리 코드를 기계어 코드로 바꾸기 위한 패스2 과정을 수행하는 함수이다.
*		   패스 2에서는 프로그램을 기계어로 바꾸는 작업은 라인 단위로 수행된다.
*		   다음과 같은 작업이 수행되어 진다.
*		   1. 실제로 해당 어셈블리 명령어를 기계어로 바꾸는 작업을 수행한다.
* 매계 : 없음
* 반환 : 정상종료 = 0, 에러발생 = < 0
* 주의 :
* -----------------------------------------------------------------------------------
*/


object_code text_records[100];  // 최대 Text 레코드 수
int text_record_count = 0;

static int assem_pass2(void)
{
	/* add your code here */
	for (int i = 0; i < token_line; i++) {
		// object code 계산해서 obj 저장
        int objcode = generate_object_code(token_table[i], i);
		
		if (objcode < 0) {
			objcode_table[i] = 0;
		} else {
			objcode_table[i] = objcode;
		}

	}

	// object_code 배열 세팅
	init_obj_list();
	
	return 0;
}

int generate_object_code(token* t, int i) {

        // 명령어가 아니라면 지시어 처리 
        int opcode_index = search_opcode(t->operator);
		// 지시어
        if (opcode_index < 0) {
			if (strcmp(t->operator, "BYTE") == 0) {
				char* op = t->operand[0];

				if (op[0] == 'C') {
					// ex) C'EOF'
					int obj = 0;
					for (int j = 2; op[j] != '\''; j++) {
						obj <<= 8; // 왼쪽으로 1바이트 이동
						obj |= (unsigned char)op[j];
					}
					return obj;
				} else if (op[0] == 'X') {
					// ex) X'F1'
					char hexstr[10] = {0};
					strncpy(hexstr, op + 2, strlen(op) - 3); // 'X' 와 ' 따옴표 제거
					int obj = (int)strtol(hexstr, NULL, 16);
					return obj;
				}
			} else if (strcmp(t->operator, "WORD") == 0) {
				// WORD는 3바이트 정수

				// MAXLEN WORD BUFFER-BUFEND 처리
				if (t->operand[0][0] != NULL && isdigit(t->operand[0][0])) {
					return 0x000000;
				}
				return atoi(t->operand[0]) & 0xFFFFFF;
				
			} else if (
				strcmp(t->operator, "RESW") == 0 || 
				strcmp(t->operator, "RESB") == 0 ||
				strcmp(t->operator, "START") == 0 ||
				strcmp(t->operator, "EXTDEF") == 0 ||
				strcmp(t->operator, "EXTREF") == 0 ||
				strcmp(t->operator, "EQU") == 0 ||
				strcmp(t->operator, "CSECT") == 0 ||
				strcmp(t->operator, "LTORG") == 0 ||
				strcmp(t->operator, "END") == 0 
				) {
				return -1;
			}
		}

		// 명령어
        int format = inst_table[opcode_index]->format;

        unsigned char op = inst_table[opcode_index]->op;
        unsigned char ni = t->nixbpe & (N | I);  // upper 2 bits

        int objcode = 0;

        if (format == 1) {
            objcode = op;
        }
        else if (format == 2) {
            // ex) CLEAR X → op | r1 | r2
            int r1 = get_register_number(t->operand[0]);
            int r2 = (t->operand[1] != NULL) ? get_register_number(t->operand[1]) : 0;
            objcode = (op << 8) | (r1 << 4) | r2;
        }
        else if (format == 3 || format == 4) {
            // 1. opcode + ni
            objcode |= ((op | (ni >> 4)) << 16);  // 상위 6비트

            // 2. xbpe 설정
            int xbpe = t->nixbpe & (X | B | P | E);

            // 3. 주소 or 상수 값
            int disp = 0;

            if (t->operand[0] != NULL) {
                char* operand = t->operand[0];

                if (operand[0] == '#') {
                    if (isdigit(operand[1])) {
                        // immediate constant (ex. #5)
                        disp = atoi(&operand[1]);
                        xbpe &= ~(B | P);  // 상수면 상대 주소 X
                    } else {
                        // symbol 참조 (ex. #LENGTH)
                        int addr = search_symbol(&operand[1]);
                        disp = (format == 3) ? (addr - (loc_table[i] + 3)) : addr;
                        xbpe |= (format == 3 ? P : E);
                    }
                }
                else if (operand[0] == '@') {
                    int addr = search_symbol(&operand[1]);
                    disp = (format == 3) ? (addr - (loc_table[i] + 3)) : addr;
                    xbpe |= (format == 3 ? P : E);
                }
				else if (operand[0] == '=') {
					int addr = search_literal(&operand[0]);
                    disp = (format == 3) ? (addr - (loc_table[i] + 3)) : addr;
                    xbpe |= (format == 3 ? P : E);
				}
                else {
					// TODO: 우선 4형식에서 EXTREF에 있으면 외부에서 참조하는거라 0으로 넣어야함
                    int addr = search_symbol(operand);
                    disp = (format == 3) ? (addr - (loc_table[i] + 3)) : 0;
                    xbpe |= (format == 3 ? P : E);
                }

				if(t->operand[1] != NULL && t->operand[1] == 'X') {
					xbpe |= X;
					printf("xbpe@@@여기 안타는데 ??? : %X \n", xbpe);
				}
            }

			objcode |= (xbpe << 12);  // xbpe는 중간 4비트
			objcode |= (disp & 0xFFF);  // 마지막 12비트
            
        }

		if (format == 2) {
        	// format 2는 2바이트 (4자리)니까 그대로 사용
        	return objcode & 0xFFFF;  // 16bit만 남긴다
			
		} else if (format == 3) {
			// format 3은 3바이트 (6자리)니까 그대로 사용
			return objcode & 0xFFFFFF;  // 24bit만 남긴다
		} else if (format == 4) {
			// format 4는 4바이트 (8자리) 만들어야 하니까
			return objcode << 8;  // 8비트 왼쪽 shift
		}

		return objcode;

        // 출력 확인
        // printf("Line %d | %s | Object Code: %06X\n", i, t->operator, objcode);    
}




int get_register_number(char* reg) {
    if (strcmp(reg, "A") == 0) return 0;
    if (strcmp(reg, "X") == 0) return 1;
    if (strcmp(reg, "L") == 0) return 2;
    if (strcmp(reg, "B") == 0) return 3;
    if (strcmp(reg, "S") == 0) return 4;
    if (strcmp(reg, "T") == 0) return 5;
    if (strcmp(reg, "F") == 0) return 6;
    return 0; // default
}

int search_symbol(char* label) {

	if (label == NULL) return -1;

    for (int i = 0; i < sym_line; i++) {
        if (strcmp(sym_table[i].symbol, label) == 0) {
            return sym_table[i].addr;
        }
    }
    return -1;
}

int search_literal(char* label) {
    for (int i = 0; i < literal_line; i++) {
        if (strcmp(literal_table[i].literal, label) == 0) {
            return literal_table[i].addr;
        }
    }
    return -1;
}

/* ----------------------------------------------------------------------------------
* 설명 : 입력된 문자열의 이름을 가진 파일에 프로그램의 결과를 저장하는 함수이다.
*        여기서 출력되는 내용은 object code이다.
* 매계 : 생성할 오브젝트 파일명
* 반환 : 없음
* 주의 : 파일이 NULL값이 들어온다면 프로그램의 결과를 stdout으로 보내어
*        화면에 출력해준다.
*        명세서의 주어진 출력 결과와 완전히 동일해야 한다.
*        예외적으로 각 라인 뒤쪽의 공백 문자 혹은 개행 문자의 차이는 허용한다.
*
* -----------------------------------------------------------------------------------
*/
void make_objectcode_output(char *file_name)
{
	/* add your code here */

	FILE* out = (file_name == NULL) ? stdout : fopen(file_name, "w");

    
	
	for (int i=0; i <= csect_index; i++) {
		object_code* obj = &obj_list[i];
		// H
		fprintf(out, "H%s  %06X%06X\n", obj->name, obj->start_addr, obj->length);

		// D
		if (obj->def_count > 0) {
			fprintf(out, "D");
			for(int i = 0; i < obj->def_count; i++) {
				fprintf(out, "%s%06X", obj->def_symbols[i], obj->def_addresses[i]);
			}
			fprintf(out, "\n");
		}
		
		// R
		if (obj->ref_count > 0) {
			fprintf(out, "R");
			for(int i = 0; i < obj->ref_count; i++) {
				fprintf(out, "%s", obj->ref_symbols[i]);
			}
			fprintf(out, "\n");
		}

		// T
		if(obj->t_count >= 0) {
			for (int i = 0; i <=obj->t_count; i++) {
				// TODO: length, start_address 부분 계산식, 첫번째 섹션 t_count RESB 수정해야함
				if (obj->t_records[i].code != NULL && strcmp(obj->t_records[i].code, "")) {
					fprintf(out, "T%06X %02X %s\n", obj->t_records[i].start_addr, strlen(obj->t_records[i].code) / 2, obj->t_records[i].code);		
				}
			}
		}

		// M
		if (obj->m_count>0) {
			for(int i = 0; i< obj->m_count; i++) {
				fprintf(out, "M%06X%02X%c%s\n", obj->m_records[i].addr, obj->m_records[i].length, obj->m_records[i].sign, obj->m_records[i].symbol);
			}
		}

		// E

		// TODO: 첫번쨰 섹션은 시작주소로 가서 실행하라
		if(i == 0) {
			fprintf(out, "E%06X\n", obj->start_addr);	
		} else {
			fprintf(out, "E\n");	
		}
		

		fprintf(out, "\n");
		
	}

    if (out != stdout) fclose(out);
}

void write_header_record(FILE* out, char* progname, int start_addr, int length) {
    fprintf(out, "H%-6s%06X%06X\n", progname, start_addr, length);
}

int get_inst_format(char* op) {
    int idx = search_opcode(op);
    if (idx >= 0) {
        return inst_table[idx]->format;
    }
    return -1; // opcode가 없을 때는 -1 리턴
}

// ------------

// objcode: 10진수 오브젝트 코드
// fmt: Format (1, 2, 3, 4)
// out: 변환된 16진수 문자열이 저장될 버퍼
void format_objcode_hex(int objcode, int fmt, char* out) {
    int hex_digits = fmt * 2; // 예: Format 3 → 6자리, Format 4 → 8자리
    sprintf(out, "%0*X", hex_digits, objcode);
}

void init_obj_list() {
	csect_index = -1;

	for(int i =0; i<token_line; i++) {

		// token* t = token_table[i];

		// CSECT 마다 object_code 생성
		if(strcmp(token_table[i]->operator, "START") == 0 || strcmp(token_table[i]->operator, "CSECT") == 0) {
			csect_index++;
			object_code* obj = &obj_list[csect_index];

			if (token_table[i]->label != NULL)
            	strncpy(obj->name, token_table[i]->label, sizeof(obj->name));


			obj->start_addr = loc_table[i]; 
        	obj->exec_start_addr = loc_table[i]; // 기본 실행 시작 주소

			// 다음 CSECT 넘어가기 전 단계 obj 렝스 저장
			if(strcmp(token_table[i]->operator, "CSECT") == 0) {
				object_code* temp = &obj_list[csect_index -1];
				temp->length = loc_table[i-1] - temp->start_addr;
				// TODO: 하드코딩
				if (strcmp(token_table[i-1]->operator, "WORD") == 0) {
					temp->length += 3;
				} else if (strcmp(token_table[i]->operator, "=X'05'") == 0) {
					temp->length += 1;
				}
			}
		}

		// END 렝스 계산
		if (strcmp(token_table[i]->operator, "END") == 0) {
			object_code* temp = &obj_list[csect_index];
			temp->length = loc_table[i] - temp->start_addr + 1; // 마지막은 +1 해야 계산이 맞음
		}

		// EXTDEF
		if (strcmp(token_table[i]->operator, "EXTDEF") == 0) {
			object_code* obj = &obj_list[csect_index];


			int count = 0;
			while(token_table[i]->operand[count] != NULL && (strcmp(token_table[i]->operand[count], "") != 0)) {
				strcpy(obj->def_symbols[count], token_table[i]->operand[count]);
				obj->def_addresses[count] = search_symbol(obj->def_symbols[obj->def_count]);
				obj->def_count++;
				count++;
			}
		}

		object_code* obj = &obj_list[csect_index];
		int objcode = objcode_table[i];

		// EXTREF
		if (strcmp(token_table[i]->operator, "EXTREF") == 0) {
			object_code* obj = &obj_list[csect_index];

			int operand_count = 0;
			while(token_table[i]->operand[operand_count] != NULL && (strcmp(token_table[i]->operand[operand_count], "") != 0)) {
				strcpy(obj->ref_symbols[operand_count], token_table[i]->operand[operand_count]);
				obj->ref_count++;
				operand_count++;
			}
		}


		// T
		int size = 3;
		// BYTE, WORD 등일 때 사이즈 조정 필요
		if (strcmp(token_table[i]->operator, "BYTE") == 0) {
			char* op = token_table[i]->operand[0];
			size = (op[0] == 'C') ? strlen(op) - 3 : (strlen(op) - 3) / 2;
		} else if (strcmp(token_table[i]->operator, "WORD") == 0) {
			size = 3;
		} else if (get_inst_format(token_table[i]->operator) == 2) {
			size = 2;
		}

		// 줄바꿈 기준
		// 60글자 or RESB or RESW
		int cur_len = strlen(obj->t_records[obj->t_count].code);
		if(cur_len + size > MAX_OBJ_LENGTH || 
		strcmp(token_table[i]->operator, "RESB") == 0 ||
		strcmp(token_table[i]->operator, "RESW") == 0) {
			obj->t_records[obj->t_count].length = strlen(obj->t_records[obj->t_count].code);
			// 연속으로 RESB RESW나오면 방지 처리
			if(!(strcmp(token_table[i-1]->operator, "RESB") == 0 ||
			strcmp(token_table[i-1]->operator, "RESW") == 0)) {
				obj->t_count++;	
				obj->t_records[obj->t_count].start_addr = loc_table[i];
			} else {
				// TODO 첫번쨰 섹션 =C'EOF' 대응
				obj->t_records[obj->t_count].start_addr = loc_table[i+1];
			}
		} 
		char temp[10];
		
		if (strcmp(token_table[i]->operator, "START") == 0 ||
			strcmp(token_table[i]->operator, "CSECT") == 0 ||
			strcmp(token_table[i]->operator, "EXTDEF") == 0 ||
			strcmp(token_table[i]->operator, "EXTREF") == 0 ||
			strcmp(token_table[i]->operator, "RESB") == 0 ||
			strcmp(token_table[i]->operator, "RESW") == 0 || 
			strcmp(token_table[i]->operator, "EQU") == 0 ||
			strcmp(token_table[i]->operator, "LTORG") == 0 ||
			strcmp(token_table[i]->operator, "END") == 0 ) {		
		} else {
			sprintf(temp, "%0*X", size * 2, objcode);  // size 바이트니까 *2 문자
    		strcat(obj->t_records[obj->t_count].code, temp);
		}

		// 연산이 있는 WORD인지 확인 예: MAXLEN WORD BUFFER-BUFEND
		int isWordWithOperand = (strcmp(token_table[i]->operator, "WORD")==0) && !isdigit(token_table[i]->operand[0][0]);
		if (get_inst_format(token_table[i]->operator) == 4 || isWordWithOperand) {

    		char* operand = token_table[i]->operand[0];

			if (isWordWithOperand) {
				// BUFFER-BEFEND 분해
				char lhs[64], rhs[64];
				char op;

				parse_operand_expr(operand, lhs, &op, rhs);

				obj->m_records[obj->m_count].addr = loc_table[i];  // objcode 전체 수정해야하니 +1 안 함
				obj->m_records[obj->m_count].length = 6;               // 20bit 수정
				obj->m_records[obj->m_count].sign = '+';              // 기본은 +
				strncpy(obj->m_records[obj->m_count].symbol, lhs, 10);
				obj->m_count++;

				obj->m_records[obj->m_count].addr = loc_table[i];  // objcode 전체 수정해야하니 +1 안 함
				obj->m_records[obj->m_count].length = 6;               // 20bit 수정
				obj->m_records[obj->m_count].sign = op;              // 기본은 +
				strncpy(obj->m_records[obj->m_count].symbol, rhs, 10);
				obj->m_count++;
				
			} else {
				// EXTREF에 있는 심볼인지 확인
				for (int r = 0; r < obj->ref_count; r++) {
					if (strcmp(obj->ref_symbols[r], operand) == 0) {
						obj->m_records[obj->m_count].addr = loc_table[i] + 1;  // 주소 필드는 object code의 2번째 바이트부터 시작)
						obj->m_records[obj->m_count].length = 5;               // 20bit 수정
						obj->m_records[obj->m_count].sign = '+';              // 기본은 +
						strncpy(obj->m_records[obj->m_count].symbol, operand, 10);
						obj->m_count++;
						break;
					}
				}
			}

			
		}
    	
	}
	// 디버깅용
	for (int i = 0; i <= csect_index; i++) {
		// printf("[i] %s, length: %X def: %s def_address: %X ref_sym: %s\n", obj_list[i].name, obj_list[i].length, obj_list[i].def_symbols[1], obj_list[i].def_addresses[2], obj_list[i].ref_symbols[0]);
		// printf("[i] t_record: %s length: %02X\n", obj_list[i].t_records[1].code, strlen(obj_list[i].t_records[1].code) / 2);
		// printf("[i] m_record addr %06X \n", obj_list[i].m_records[1].addr);
	}

}

// expr: 입력 문자열 ("BUFFER-BUFEND" 등)
// lhs: 왼쪽 심볼 버퍼 (충분한 크기로 할당할 것)
// op:  연산자 문자('+', '-', ...) 저장 위치
// rhs: 오른쪽 심볼 버퍼 (충분한 크기로 할당할 것)
void parse_operand_expr(const char *expr, char *lhs, char *op, char *rhs) {
    const char *p = expr;
    // 연산자 위치 찾기
    while (*p && *p!='+' && *p!='-') {
        p++;
    }
    if (*p=='+' || *p=='-') {
        // 1) 연산자 앞부분 복사
        size_t len = p - expr;
        strncpy(lhs, expr, len);
        lhs[len] = '\0';
        // 2) 연산자 복사
        *op = *p;
        // 3) 연산자 뒷부분 복사
        strcpy(rhs, p+1);
    } else {
        // 연산자가 없으면 전체를 lhs로, op는 '\0'
        strcpy(lhs, expr);
        *op = '\0';
        rhs[0] = '\0';
    }
}
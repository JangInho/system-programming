/*
 * 파일명 : my_assembler.c 
 * 설  명 : 이 프로그램은 SIC/XE 머신을 위한 간단한 Assembler 프로그램의 메인루틴으로,
 * 입력된 파일의 코드 중, 명령어에 해당하는 OPCODE를 찾아 출력한다.
 *
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

 // 파일명의 "00000000"은 자신의 학번으로 변경할 것.
#include "my_assembler_20193039.h"

/* ------------------------------------------------------------
 * 설명 : 사용자로 부터 어셈블리 파일을 받아서 명령어의 OPCODE를 찾아 출력한다.
 * 매계 : 실행 파일, 어셈블리 파일 
 * 반환 : 성공 = 0, 실패 = < 0 
 * 주의 : 현재 어셈블리 프로그램의 리스트 파일을 생성하는 루틴은 만들지 않았다. 
 *		   또한 중간파일을 생성하지 않는다. 
 * ------------------------------------------------------------
 */


int main(int args, char *arg[]) 
{
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

	// make_symtab_output("output_symtab.txt");         //  추후 과제에 사용 예정
	// make_literaltab_output("output_littab.txt");     //  추후 과제에 사용 예정

	if (assem_pass2() < 0)
	{
		printf(" assem_pass2: 패스2 과정에서 실패하였습니다.  \n");
		return -1;
	}

	// make_objectcode_output("output_objectcode.txt"); //  추후 과제에 사용 예정
}

/* ------------------------------------------------------------
 * 설명 : 프로그램 초기화를 위한 자료구조 생성 및 파일을 읽는 함수이다. 
 * 매계 : 없음
 * 반환 : 정상종료 = 0 , 에러 발생 = -1
 * 주의 : 각각의 명령어 테이블을 내부에 선언하지 않고 관리를 용이하게 하기 
 *		   위해서 파일 단위로 관리하여 프로그램 초기화를 통해 정보를 읽어 올 수 있도록
 *		   구현하였다. 
  * ------------------------------------------------------------
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

/* ------------------------------------------------------------
 * 설명 : 머신을 위한 기계 코드목록 파일(inst_table.txt)을 읽어
 *       기계어 목록 테이블(inst_table)을 생성하는 함수이다.
 *
 *
 * 매계 : 기계어 목록 파일
 * 반환 : 정상종료 = 0 , 에러 < 0
 * 주의 : 기계어 목록파일 형식은 자유롭게 구현한다. 예시는 다음과 같다.
 *
 * =======================================================
 *		   | 이름 | 형식 | 기계어 코드 | 오퍼랜드의 갯수 | \n |
 * ============================================================
 *
 * ------------------------------------------------------------
 */

int init_inst_file(char* inst_file)
{
	FILE* file;
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
	
	// print_inst_table();

	errno = 0;
	return errno;
}

// TODO: 꼭 삭제해야함
void print_inst_table()
{
    printf("======= Instruction Table =======\n");
    printf("%-10s %-6s %-6s %-6s\n", "Mnemonic", "Format", "Opcode", "Operands");

    for (int i = 0; i < inst_index; i++) {
        inst* curr = inst_table[i];
        printf("%-10s %-6d %02X      %-6d\n", curr->str, curr->format, curr->op, curr->ops);
    }

    printf("======= Total: %d instructions =======\n", inst_index);
}

/* ------------------------------------------------------------
 * 설명 : 어셈블리 할 소스코드 파일(input.txt)을 읽어 소스코드 테이블(input_data)를 생성하는 함수이다.
 * 매계 : 어셈블리할 소스파일명
 * 반환 : 정상종료 = 0 , 에러 < 0
 * 주의 : 라인단위로 저장한다.
 *
 * ------------------------------------------------------------
 */
int init_input_file(char* input_file)
{
	FILE* file;
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

    // 디버깅용 출력
    // printf("======= input.txt 내용 =======\n");
    // for (int i = 0; i < line_num; i++) {
    //     printf("%4d | %s\n", i + 1, input_data[i]);
    // }

	errno = 0;
	return errno;
}

/* ------------------------------------------------------------
 * 설명 : 소스 코드를 읽어와 토큰단위로 분석하고 토큰 테이블을 작성하는 함수이다.
 *        패스 1로 부터 호출된다.
 * 매계 : 파싱을 원하는 문자열
 * 반환 : 정상종료 = 0 , 에러 < 0
 * 주의 : my_assembler 프로그램에서는 라인단위로 토큰 및 오브젝트 관리를 하고 있다.
 * ------------------------------------------------------------
 */
int token_parsing(char* str)
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
	
	char* token = strtok(buffer, " \t\n");

	// 첫번째 토큰이 주석인 경우
	// TODO: 100 보다 클 때 에러처리 해야할 것 같음
	if (strcmp(token, ".") == 0 || str[0] == '.') {
		return 0;
	}
	
	/*
	* 첫번째 토큰 경우의 수
	* - . 주석
	* - 명령어
	* - 지시어
	* - 레이블
	*/

    if (token != NULL) {
        // 첫 번째 토큰이 명령어나 지시어일 수도 있으니 미리 검사
        if (search_opcode(token) >= 0 || strcmp(token, "START") == 0 || strcmp(token, "END") == 0 ||
            strcmp(token, "BYTE") == 0 || strcmp(token, "RESW") == 0 || strcmp(token, "RESB") == 0 
			|| strcmp(token, "WORD") == 0) {
            // label 없음
            new_token->operator = strdup(token);
        } else {
            new_token->label = strdup(token);
            token = strtok(NULL, " \t\n");
            if (token != NULL) {
                new_token->operator = strdup(token);
            }
        }


		int op_idx = 0;
		token = strtok(NULL, " \t\n");
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
				strcmp(new_token->operator, "BASE") == 0 ||
				strcmp(new_token->operator, "ORG") == 0 ||
				strcmp(new_token->operator, "EQU") == 0 ||
				strcmp(new_token->operator, "END") == 0) {
			expected_ops = 1;
		 } else if (strcmp(new_token->operator, "NOBASE") == 0 ||
         			strcmp(new_token->operator, "LTORG") == 0) {
			expected_ops = 0;
		}
		

		// TODO: 개선이 필요함
		if (expected_ops == 1) {
			new_token->operand[0] = strdup(token);  // 문자열 복사 + 메모리 자동 할당
		} else if (expected_ops == 2) {
			char* save;
			char* temp_token = strtok_r(token, ",", &save);
			if(temp_token != NULL) {
				new_token->operand[0] = strdup(temp_token);
				temp_token = strtok_r(NULL, ",", &save);
				new_token->operand[1] = strdup(temp_token);
			}
		}


		// ver1.
		// char* op_token = strdup(token);
		// if (op_token == NULL) {
		// 	perror("strdup failed");
		// 	exit(1);
		// }
		// op_token = strtok(op_token, ",");
		// for (int i =0; i < expected_ops; i++) {
		// 	new_token->operand[i] = strdup(op_token);
		// 	op_token = strtok(NULL, ",");	
		// }
		// token = strtok(NULL, " \t");

		// operand 1 or 2
		// while (token != NULL && op_idx < expected_ops) {
		// 	char* op_token =  strtok(NULL, ",");
		// 		// token =  strtok(NULL, "");

		// ver2. 
		// 		while (token != NULL && op_idx < expected_ops) {
		// 			new_token->operand[op_idx++] = strdup(token);
		// 			token = strtok(NULL, ",");
		// 		}
		// 		token = strtok(NULL, " \t\n");
		// }	

		token = strtok(NULL, " \t");
		// 초기 버전
		if (token != NULL) {
			char temp_comment[100] = {0};
			strcat(temp_comment, token);
			while((token = strtok(NULL, " \t\n")) != NULL) {
				strcat(temp_comment, " ");
        		strcat(temp_comment, token);
			}
			strncpy(new_token->comment, temp_comment, sizeof(new_token->comment) - 1);
		}

        
    }

	token_table[token_line] = new_token;
	token_line++;

	return 0;
}

void print_token_table() {
    printf("=========== TOKEN TABLE ===========\n");
    for (int i = 0; i < token_line; i++) {
        token* t = token_table[i];

        printf("[%3d] ", i);

        if (t->label)     printf("LABEL: %-10s  ", t->label);
        else              printf("LABEL: %-10s  ", "-");

        if (t->operator)  printf("OPERATOR: %-10s  ", t->operator);
        else              printf("OPERATOR: %-10s  ", "-");

        for (int j = 0; j < MAX_OPERAND; j++) {
			if (t->operand[j])
                printf("OPERAND[%d]: %-10s  ", j, t->operand[j]);
        }
		if(t->operand[0] == NULL) {
			printf("OPERAND[00]: %-10s  ", "-");
		}

        if (strlen(t->comment) > 0)
            printf("COMMENT: %s", t->comment);

        printf("\n");
    }
    printf("====================================\n");
}


/* ------------------------------------------------------------
* 설명 : 어셈블리 코드를 위한 패스1과정을 수행하는 함수이다.
*		   패스1에서는..
*		   1. 프로그램 소스를 스캔하여 해당하는 토큰단위로 분리하여 프로그램 라인별 토큰
*		   테이블을 생성한다.
*          2. 토큰 테이블은 token_parsing()을 호출하여 설정한다.
*          3. assem_pass2 과정에서 사용하기 위한 심볼테이블 및 리터럴 테이블을 생성한다.
* 
*    
*
* 매계 : 없음
* 반환 : 정상 종료 = 0 , 에러 = < 0
* 주의 : 현재 초기 버전에서는 에러에 대한 검사를 하지 않고 넘어간 상태이다.
*	     따라서 에러에 대한 검사 루틴을 추가해야 한다.
* 
*        OPCODE 출력 프로그램에서는 심볼테이블, 리터럴테이블을 생성하지 않아도 된다.
*        그러나, 추후 프로젝트 1을 수행하기 위해서는 심볼테이블, 리터럴테이블이 필요하다.
*
* ------------------------------------------------------------
*/
static int assem_pass1(void)
{
	/* add your code here */

	/* input_data의 문자열을 한줄씩 입력 받아서
	 * token_parsing()을 호출하여 _token에 저장
	 */

	token_line = 0;
	for (int i = 0; i < line_num; i++) {
		if (token_parsing(input_data[i]) < 0) {
			printf("토큰 파싱 에러 발생!");
		}
	}

	make_opcode_output(NULL);

	// print_token_table();

}

/* ------------------------------------------------------------
 * 설명 : 입력 문자열이 기계어 코드인지를 검사하는 함수이다.
 * 매계 : 토큰 단위로 구분된 문자열
 * 반환 : 정상종료 = 기계어 테이블 인덱스, 에러 < 0
 * 주의 : 기계어 목록 테이블에서 특정 기계어를 검색하여, 해당 기계어가 위치한 인덱스를 반환한다.
 *        '+JSUB'과 같은 문자열에 대한 처리는 자유롭게 처리한다.
 *
 * ------------------------------------------------------------
 */
int search_opcode(char* str)
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

/* ------------------------------------------------------------
* 설명 : 입력된 문자열의 이름을 가진 파일에 프로그램의 결과를 저장하는 함수이다.
*
* 매계 : 생성할 오브젝트 파일명
* 반환 : 없음
* 주의 : 소스코드 명령어 앞에 OPCODE가 기록된 코드를 파일에 출력한다.
*        파일이 NULL값이 들어온다면 프로그램의 결과를 stdout으로 보내어
*        화면에 출력해준다.
*        
*        OPCODE 출력 프로그램의 최종 output 파일을 생성하는 함수이다.
*        (추후 프로젝트 1에서는 불필요)
*
* ------------------------------------------------------------
*/
void make_opcode_output(char* file_name)
{
	/* add your code here */

	// ver1.
	// for (int i = 0; i < token_line; i++) {
    //     token* t = token_table[i];

    //     if (t->label)     printf("%-10s  ", t->label);
    //     else              printf("%-10s  ", "");

    //     if (t->operator)  printf("%-10s  ", t->operator);
    //     else              printf("%-10s  ", "");

    //     for (int j = 0; j < MAX_OPERAND; j++) {
	// 		if (t->operand[j])
    //             printf("%-10s  ", t->operand[j]);
    //     }
	// 	if(t->operand[0] == NULL) {
	// 		printf("%-10s  ", "");
	// 	}

    //     // if (strlen(t->comment) > 0)
    //     //     printf("COMMENT: %s", t->comment);

	// 	if (t->operator != NULL) {
    //         int idx = search_opcode(t->operator);
    //         if (idx >= 0) {
	// 			printf("OPCODE: %02X", inst_table[idx]->op);  // 뒤에 OPCODE 출력
	// 		}
                
    //     }

    //     printf("\n");
    // }


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
        // for (int j = 0; j < MAX_OPERAND; j++) {
        //     if (t->operand[j] != NULL) {
        //         fprintf(out, "%s", t->operand[j]);
        //         if (j < MAX_OPERAND - 1 && t->operand[j + 1] != NULL)
        //             fprintf(out, ",");
        //     }
        // }
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

/* ------------------------------------------------------------
* 설명 : 입력된 문자열의 이름을 가진 파일에 프로그램의 결과를 저장하는 함수이다.
*        여기서 출력되는 내용은 SYMBOL별 주소값이 저장된 TABLE이다.
* 매계 : 생성할 오브젝트 파일명 혹은 경로
* 반환 : 없음
* 주의 : 파일이 NULL값이 들어온다면 프로그램의 결과를 stdout으로 보내어
*        화면에 출력해준다.
*
* ------------------------------------------------------------
*/
void make_symtab_output(char* file_name)
{
	/* add your code here */
}


/* ------------------------------------------------------------
* 설명 : 입력된 문자열의 이름을 가진 파일에 프로그램의 결과를 저장하는 함수이다.
*        여기서 출력되는 내용은 LITERAL별 주소값이 저장된 TABLE이다.
* 매계 : 생성할 오브젝트 파일명
* 반환 : 없음
* 주의 : 파일이 NULL값이 들어온다면 프로그램의 결과를 stdout으로 보내어
*        화면에 출력해준다.
*
* ------------------------------------------------------------
*/
void make_literaltab_output(char* filename)
{
	/* add your code here */
}


/* ------------------------------------------------------------
 * 설명 : 어셈블리 코드를 기계어 코드로 바꾸기 위한 패스2 과정을 수행하는 함수이다. 
 *		   패스 2에서는 프로그램을 기계어로 바꾸는 작업은 라인 단위로 수행된다. 
 *		   다음과 같은 작업이 수행되어 진다. 
 *		   1. 실제로 해당 어셈블리 명령어를 기계어로 바꾸는 작업을 수행한다. 
 * 매계 : 없음
 * 반환 : 정상종료 = 0, 에러발생 = < 0 
 * 주의 : 
 * ------------------------------------------------------------
 */

static int assem_pass2(void)
{

	/* add your code here */

}

/* ------------------------------------------------------------
* 설명 : 입력된 문자열의 이름을 가진 파일에 프로그램의 결과를 저장하는 함수이다.
*        여기서 출력되는 내용은 object code이다.
* 매계 : 생성할 오브젝트 파일명
* 반환 : 없음
* 주의 : 파일이 NULL값이 들어온다면 프로그램의 결과를 stdout으로 보내어
*        화면에 출력해준다.
*        명세서의 주어진 출력 결과와 완전히 동일해야 한다.
*        예외적으로 각 라인 뒤쪽의 공백 문자 혹은 개행 문자의 차이는 허용한다.
*
* ------------------------------------------------------------
*/
void make_objectcode_output(char* file_name)
{
	/* add your code here */
}

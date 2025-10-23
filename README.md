# system-programming
시스템 프로그래밍 강의 구현 과제

## **#1**
**과제내용**
  - SIC/XE 어셈블러를 개발하기 전에, 프로그램을 입력받을 수 있는 파싱 프로그램 작성
  - 명령어에 해당하는 OPCODE를 찾아서 해당 명령어 옆에 출력
  - 과제에 주어진 C 인터페이스 라이브러리를 사용

## **#1-a**
**과제내용**
  - ControlSection 방식의 SIC/XE 소스(Fig 2.15)를 Object Program Code로 바꾸는 어셈블러 만들기
  - SIC/XE 소스를 라인별로 처리해서 Object Code로 바꾼 후, Object Program Code로 변환하는 프로그램 
  - 과제에 주어진 C 소스코드와 헤더 파일 사용하기

**과제 목적**
  - SIC/XE 소스를 Object Program Code 로 변환하여 봄으로써 SIC/XE 어셈블러의 동작을 이해한다.
  - 주어진 C 소스코드 외 헤더파일을 이용하여 SIC/XE 소스를 Object Program Code로 변환하는 과정을 이해하고 이 후 확장되는 과제 내용에 맞추어 프로그램의 확장성을 효과적으로 증진시키기 위한 기본 지식을 학습한다.

## **#1-b**
**과제내용**
  - ControlSection 방식(2.3 35p)의 SIC/XE 소스(Fig 2.15)를 Object Program Code (2.3 48p)로 바꾸는 어셈블러 만들기
  - SIC/XE 소스를 라인별로 처리해서 Object Code로 바꾼 후, Object Program Code로 변환하는 프로그램 
  - 과제에 주어진 자바 프로젝트 파일 사용하기

**과제 목적**
  - SIC/XE 소스를 Object Program Code로 변환하여 봄으로써 SIC/XE 어셈블러의 동작을 이해한다.
  - 주어진 자바파일을 이용하여 SIC/XE 소스를 Object Program Code로 변환하는 과정을 복습하고 C와 비교한다.

## **#2**
**프로젝트 내용**
  - ControlSection 방식으로 생성된 ObjectCode(프로젝트#1-b의 결과물)를 실행하고 시뮬레이션할 수 있는 시뮬레이터 만들기
  - 시뮬레이션 과정이 Step-by-Step으로 Visual하게 보여주는 Java GUI 프로그램
  - GUI를 위한 모듈, 연산 모듈, 가상 장치(메모리, 레지스터) 모듈, 로더를 통하여 시뮬레이터 구현

**과제 목적**
  - ControlSection 방식으로 생성된 ObjectCode를 입력으로 삼아 실제 코드가 동작하는 방식을 시뮬레이션할 수 있는 GUI Java 프로그램을 만든다. 

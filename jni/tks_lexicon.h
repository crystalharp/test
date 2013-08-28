
#ifndef TKSLEXICON_H_
#define TKSLEXICON_H_

typedef struct Index {
	char key[2];
	unsigned short int start_s;
	unsigned short int end_s;
	unsigned short int start_l;
	unsigned short int end_l;
} Index;

// wordInfo flags, 2 bytes: 
//  0  0  0  0  0  0  0  0
// |    reserved    | type |   
typedef struct SWord {
	char key[12];               //��������
	unsigned short int wordInfo;    //���͡��Ƿ��о�γ��
	unsigned short int rank;
	float lon;                  //��γ��
	float lat;  
} SWord;

typedef struct LWord {
	char key[24];               //��������
	unsigned short int wordInfo;    //���͡��Ƿ��о�γ��
	unsigned short int rank;
	float lon;                  //��γ��
	float lat; 
} LWord;

#endif

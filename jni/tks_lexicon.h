
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
	char key[12];               //联想词名称
	unsigned short int wordInfo;    //类型、是否有经纬度
	unsigned short int rank;
	float lon;                  //经纬度
	float lat;  
} SWord;

typedef struct LWord {
	char key[24];               //联想词名称
	unsigned short int wordInfo;    //类型、是否有经纬度
	unsigned short int rank;
	float lon;                  //经纬度
	float lat; 
} LWord;

#endif

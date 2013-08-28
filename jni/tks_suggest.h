/*
 * TKSuggestWords.h
 *
 *  Created on: 2010-8-9
 *      Author: xiaozhongmin
 */

#ifndef TKSUGGESTWORDS_H_
#define TKSUGGESTWORDS_H_

typedef struct Suggest_Word {
	char key[26];    //联想词名称
	float lon;       //经纬度
	float lat;
} Suggest_Word;


#define SWCOUNT 15
extern Suggest_Word suggestwords[SWCOUNT];
int tk_decompress(int citycode, char* filepath);
int tk_inf(char* source, char* dest);

int tk_suggestword_init(const char* filepath, const char* filepath_common, int citycode);
//if there is no hit, it will return 0 for hit value. So the wordslist are
//predefined words.
int tk_getwordslist(const char* searchword, unsigned int type, int *hit);
void tk_suggestword_destroy();

/*
 * get the suggest lexicon's version of certain city.
 * @filepath: 
 * @citycode: 
 */
int tk_getrevision(const char* filepath, int citycode);

#endif /* TKSUGGESTWORDS_H_ */

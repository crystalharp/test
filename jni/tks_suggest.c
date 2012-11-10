#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>
#include <string.h>
#include <fcntl.h>
#include <sys/types.h>
#include <sys/stat.h>
#include<ctype.h>
#include "tks_suggest.h"
#include "tks_lexicon.h"

char cur_word[2];
int cilen, silen, csnum, clnum, ssnum, slnum, wordscount;//索引长度和词条长度 
FILE *csfp, *clfp, *ssfp, *slfp;             //联想词库文件指针
int cityv, nationv;
int nflag, cflag;

Index* index_special;
Index* index_common;

SWord* ssword, *csword;
LWord* slword, *clword;

LWord words[2000];
Suggest_Word suggestwords[SWCOUNT];

void tk_suggestword_destroy() 
{

    if (cflag) { 
        if (ssfp) {
            fclose(ssfp);
            ssfp = NULL;
        }
        if (slfp) {
            fclose(slfp);
            slfp = NULL;
        }
        if (index_special) {
            free(index_special);
            index_special = NULL;
        }
    }
    if (nflag) {
        if (csfp) {
            fclose(csfp);
            csfp = NULL;
        }
        if (clfp) {
            fclose(clfp);
            clfp = NULL;
        }
        if (index_common) {
            free(index_common);
            index_common = NULL;
        }
    }
}

unsigned int dora(char c) 
{
	if ((c > 47)&&(c < 58))
		return 1;
	if ((c > 64)&&(c < 91))
		return 1;
	if ((c > 96)&&(c < 123))
		return 1;
	return 0;
}

static void loadindex(FILE *fp, Index** indexarray, int* indexlength) 
{
//load the common index
	int filelen;
	fseek(fp, 0, SEEK_END); 
	filelen = ftell(fp); 
	*indexlength = (filelen - 4) / sizeof(Index);
	*indexarray = (Index*)malloc(sizeof(Index) * *indexlength);
#ifdef TK_S_SYMBIAN
rewind(fp);
#endif
	fseek(fp, 4, SEEK_SET);
	fread(*indexarray, 1, sizeof(Index) * (*indexlength), fp);
}
int set_context(const char* filepath, int citycode, int* version, FILE** sfp, FILE** lfp, Index** indexarray, int* indexlength)
{
	char ifilename[256];
	char sfilename[256];
    char lfilename[256];
    struct stat buf;
	FILE *fpindex;
	int sword_tnum = 0;
    int lword_tnum = 0;
    int slength, llength; 
	int i;
    *version = 0;

    //首先判断联想词文件的完整性：三个文件中只要有一个不存在，则全部删除
    //如果都存在，则说明index文件完整，加载index，判断_s,_l两个文件的完整性，如果不完整则清除全部文件
    sprintf(ifilename, "%s/sw2_%d_index", filepath, citycode);
    sprintf(sfilename, "%s/sw2_%d_s", filepath, citycode);
    sprintf(lfilename, "%s/sw2_%d_l", filepath, citycode);

    if ((access(ifilename, 0) != 0) || (access(sfilename, 0) != 0) || (access(lfilename, 0) != 0)) {
        remove(ifilename);
        remove(sfilename);
        remove(lfilename);
        return 0;
    }
      
    stat(sfilename, &buf);
    slength = buf.st_size;
    stat(lfilename, &buf);
    llength = buf.st_size;

    if ((fpindex = fopen(ifilename, "rb")) <= 0)
        return 0;	

    if (fpindex) {
        fread(version, 1, 4, fpindex);
        loadindex(fpindex, indexarray, indexlength);
        fclose(fpindex);
    }

    for (i = 0; i < *indexlength; i++) {
        sword_tnum += (*indexarray)[i].end_s - (*indexarray)[i].start_s;
        lword_tnum += (*indexarray)[i].end_l - (*indexarray)[i].start_l;
    } 
  
    if ((slength != sword_tnum * sizeof(SWord)) || (llength != lword_tnum * sizeof(LWord))) {
        remove(lfilename);
        remove(sfilename);
        remove(ifilename);
        *version = 0;
        free(*indexarray);
        return 0;
    } 

    if (!sfp || !lfp) {
        free(*indexarray);
        return 0;
    }

    if ((*sfp = fopen(sfilename, "rb")) <= 0)
        return 0;	
    if ((*lfp = fopen(lfilename, "rb")) <= 0) {
        fclose(*sfp);
        return 0;	
    }
    return 1;
} 

int tk_suggestword_init(const char* filepath, const char* filepath_common, int citycode) 
{
    nationv = 0;
    cityv = 0;
    cur_word[0] = '\0';
    cur_word[1] = '\0';
    cflag = set_context(filepath, citycode, &cityv, &ssfp, &slfp, &index_special, &silen);
//    nflag = set_context(filepath_common, 9999, &nationv, &csfp, &clfp, &index_common, &cilen);
    if (nflag || cflag)
        return 1;    
    else return 0;
}

int bisearch(Index* indexarray,int i, int j, const char* inputword) 
{
	int m, res;
	
	while(i < j-1){		
		m = (i+j)/2;
		res = memcmp(inputword, indexarray[m].key, 2);
		if (res <= 0)
			j = m;
		if (res >0 )
			i = m;
	}
	
	if (!memcmp(inputword, indexarray[i].key, 2))
		m = i;
	else {
		if (!memcmp(inputword, indexarray[j].key, 2))
			m = j;
		else m = -1;
	}
	return m;
}

int mergewords() 
{
	int ss = 0, sl = 0, cs = 0, cl = 0;
	int rank,tag;
	wordscount = 0;
	//printf("ssnum=%d,slnum=%d,csnum=%d,clnum=%d\n",ssnum, slnum, csnum, clnum);
	while (((ss >= 0) && (ss < ssnum)) 
		 || ((sl >= 0) && (sl < slnum))
		 || ((cs >= 0) && (cs < csnum))
		 || ((cl >= 0) && (cl < clnum))) {
		rank = -1;
		if ((ss >= 0) && (ss < ssnum) && (ssword[ss].rank > rank)) {
			rank = ssword[ss].rank;
			tag = 1;
		}
		if ((cs >= 0) && (cs < csnum) && (csword[cs].rank > rank)) {
			rank = csword[cs].rank;
			tag = 2;
		}
		if ((sl >= 0) && (sl < slnum) && (slword[sl].rank > rank)) {
			rank = slword[sl].rank;
			tag = 3;
		}
		if ((cl >= 0) && (cl < clnum) && (clword[cl].rank > rank)) {
			rank = clword[cl].rank;
			tag = 4;
		}
		switch(tag) {
		case 1:
			memcpy(words[wordscount].key, ssword[ss].key,12);
			memset(words[wordscount].key+12,0,12);
			words[wordscount].lon = ssword[ss].lon;
			words[wordscount].lat = ssword[ss].lat;
			words[wordscount].wordInfo = ssword[ss].wordInfo;
			words[wordscount].rank = ssword[ss].rank;
			ss++;
			break;
		case 2:
			memcpy(words[wordscount].key, csword[cs].key,12);
			memset(words[wordscount].key+12,0,12);
			words[wordscount].lon = csword[cs].lon;
			words[wordscount].lat = csword[cs].lat;
			words[wordscount].wordInfo = csword[cs].wordInfo;
			words[wordscount].rank = csword[cs].rank;
			cs++;
			break;
		case 3:
			memcpy(&words[wordscount], &slword[sl], sizeof(LWord));
			sl++;
			break;
		case 4:
			memcpy(&words[wordscount], &clword[cl], sizeof(LWord));
			cl++;
			break;
			
		}
//		printf("word=%s,type=%d, rank=%d\n",words[wordscount].key, words[wordscount].wordInfo, words[wordscount].rank);
		wordscount++;
	}
	return wordscount;
}

int loadwords(const char* inputword) 
{
	int m = -1, n = -1;
    if(nflag)
        m = bisearch(index_common, 0, cilen, inputword);
	if(cflag)
        n = bisearch(index_special, 0, silen, inputword);
	if (m >= 0) {
		csnum = index_common[m].end_s - index_common[m].start_s;
		clnum = index_common[m].end_l - index_common[m].start_l;
		if (csnum > 0) {
            if(!(csword = (SWord*)malloc(sizeof(SWord) * csnum)))
                return 0;
#ifdef TK_S_SYMBIAN
rewind(csfp);
#endif
    		fseek(csfp, index_common[m].start_s * sizeof(SWord), SEEK_SET);
	    	fread(csword, sizeof(SWord), csnum, csfp);
        }
		if (clnum > 0) {
            if (!(clword = (LWord*)malloc(sizeof(LWord) * clnum)))
                return 0;
#ifdef TK_S_SYMBIAN
rewind(clfp);
#endif
		    fseek(clfp, index_common[m].start_l * sizeof(LWord), SEEK_SET);
		    fread(clword, sizeof(LWord), clnum, clfp);
        }
	}
	else {
		csnum = -1;
		clnum = -1;
	}

	if (n >= 0) {
		ssnum = index_special[n].end_s - index_special[n].start_s;
		slnum = index_special[n].end_l - index_special[n].start_l;
		if (ssnum > 0) {
            if(!(ssword = (SWord*)malloc(sizeof(SWord) * ssnum)))
                return 0;
#ifdef TK_S_SYMBIAN
rewind(ssfp);
#endif
            fseek(ssfp, index_special[n].start_s * sizeof(SWord), SEEK_SET);
		    fread(ssword, sizeof(SWord), ssnum, ssfp);
        }
		if (slnum > 0) {
		    if (!(slword = (LWord*)malloc(sizeof(LWord) * slnum)))
                return 0;
#ifdef TK_S_SYMBIAN
rewind(slfp);
#endif
        fseek(slfp, index_special[n].start_l * sizeof(LWord), SEEK_SET);
		fread(slword, sizeof(LWord), slnum, slfp);
        }
	}
	else {
		ssnum = -1;
		slnum = -1;
	}
	wordscount = mergewords();
	if (csnum > 0) free(csword);
	if (clnum > 0) free(clword);
	if (ssnum > 0) free(ssword);
	if (slnum > 0) free(slword);
    return 1;
}

int isprefix(const char* word1, const char* word2) 
{
	//若word1为1-2个字节，则肯定返回1
	int i = 0,j=0;
	if (dora(word1[0]) && word1[1]=='\0')
		return 1;
	if (!dora(word1[0]) && word1[2]=='\0')
		return 1;
	if (dora(word1[0]))
		j=1;
	else j = 2;

	while ((word1[j] == word2[i]) && (word1[j]!='\0') && (i<24)){
		i++;
		j++;
	}
	if (word1[j] == '\0')
		return 1;
	else return 0;
}

#define LIFE_COMMON_WORD_NUM 7
static const char* life_common_words[] = {
	"餐馆", "酒店", "银行", "商场", "公交站", "医院", "学校"
};

#define TRAFFIC_COMMON_WORD_NUM 8
static const char* traffic_common_words[TRAFFIC_COMMON_WORD_NUM] = {
	"路", "胡同", "大学", "广场", "大厦", "小区", "景点", "公园"
};

#define DEFAULT_WORDS_NUM 3
static const char* default_words[DEFAULT_WORDS_NUM] = {
    "我的当前位置", "全市范围", "当前地图中心"
}; 

static int fill_nohit_sw_by_common_word(const char* searchword, const char** common_words, int common_word_num) {
	int i = 0;
	int count = 0;
	unsigned int key_len = sizeof(suggestwords[0].key) / sizeof(suggestwords[0].key[0]);
	for (; i < common_word_num; i++) {
		unsigned int str_len = strlen(searchword) + strlen(" ") + strlen(common_words[i]);
		if (str_len < key_len) {
			sprintf(suggestwords[i].key, "%s %s", searchword, common_words[i]);
			count++;
		} else {
			int j = 0;
			count = 0;				
			for (; j < i; j++)
				suggestwords[j].key[0] = '\0';
			break;
		}
	}
	return count;
}

static int fill_nohit_words(const char* searchword, unsigned int type) {
	int fill_count = 0;
	if (type == 1 || type == 2)
		fill_count = fill_nohit_sw_by_common_word(searchword, life_common_words, LIFE_COMMON_WORD_NUM);
	else
		fill_count = fill_nohit_sw_by_common_word(searchword, traffic_common_words, TRAFFIC_COMMON_WORD_NUM);
	return fill_count;
} 

int tk_getwordslist(const char* searchword, unsigned int type, int *hit) 
{
	int count, i, suffixed;
	//遍历、根据类别和前缀筛选出前 SWCOUNT 个联想词
	if (dora(searchword[0])) {
		if (memcmp(cur_word, searchword, 1) != 0){
			if (loadwords(searchword))
    			memcpy(cur_word, searchword, 2);
		}
	}
	else if (memcmp(cur_word, searchword, 2) != 0){
		if (loadwords(searchword))
    		memcpy(cur_word, searchword, 2);
	}

	count = 0, i = 0;
    memset(suggestwords, 0, sizeof(Suggest_Word) * SWCOUNT);
    while ((count < SWCOUNT) && (i < wordscount)) {
        if (isprefix(searchword,words[i].key)) 
            if ((type == 2) || words[i].wordInfo == type) {
                suggestwords[count].lon = words[i].lon;
                suggestwords[count].lat = words[i].lat;
                if (dora(cur_word[0])) {
                    memcpy(suggestwords[count].key, cur_word, 1);
                    memcpy(suggestwords[count].key + 1, words[i].key, strlen(words[i].key));
                    suggestwords[count].key[1 + strlen(words[i].key)] = '\0';
                } else {
                    memcpy(suggestwords[count].key, cur_word, 2);
                    memcpy(suggestwords[count].key + 2, words[i].key, strlen(words[i].key));
                    suggestwords[count].key[2 + strlen(words[i].key)] = '\0';
                }
                count++;
            }
        i++;
    }
    if (0 == count) {
        *hit = 0;
        /*
        suffixed = 0;
        //confirm whether the search word has  added the words such as "餐馆" 
        if (type == 1 || type == 2) {
            for (i = 0; i < LIFE_COMMON_WORD_NUM; i++)
                if (strstr(searchword, life_common_words[i]) != NULL) {
                    suffixed = 1;
                    break;
                } 
        }
        else {
            for (i = 0; i < TRAFFIC_COMMON_WORD_NUM; i++)
                if (strstr(searchword, traffic_common_words[i]) != NULL) {
                    suffixed = 1;
                    break;
                }
        }
        for (i = 0; i < DEFAULT_WORDS_NUM; i++) 
            if (strstr(searchword, default_words[i]) != NULL) {
                suffixed = 1;
                break;
            }
        if(!suffixed)
            count = fill_nohit_words(searchword, type);
        */
    } else {
        *hit = 1;
    }
    if (count < SWCOUNT )
        suggestwords[count].key[0] = '\0';
    /*for (int i = 0; i < count; i++) 
         printf("sw=%s, lat = %f, lon = %f\n",suggestwords[i].key, 
                 suggestwords[i].lat, suggestwords[i].lon);
    printf("That's all!\n");*/
    return count;
}

void userhelp(const char* pname)
{
	printf("Usage: %s <citycode> <version>\n", pname);
	printf("This program will  return the suggest words list according to your input.\n");
}

int tk_getrevision(const char* filepath, int citycode) 
{
    int version, indexlen;
    Index* index;

    set_context(filepath, citycode, &version, NULL, NULL, &index, &indexlen);

    return version;
}

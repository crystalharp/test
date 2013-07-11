/*
 * TKSuggestLexiconMerger.c
 *
 *  Created on: 2010-8-17
 *      Author: XiaoZhongmin
 */
#include <errno.h>
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <sys/stat.h>
#include "tk_util.h"
#include "tks_suggest.h"
#include "tks_lexicon.h"
#include "zlib.h"

#define CHUNK (1024) // 16KB
#define FNL 256

typedef struct DiffData {
	unsigned char label[3];
	char command;//1 增加 2 删除
	short int count;
} DiffData;


int mergefile(int citycode, char* suffix, char* filepath, int wordlen) {

	char filename[FNL];
	unsigned char* ofile_content = NULL;
	unsigned char* dfile_content = NULL;
	unsigned int ofilesize, dfilesize;
	DiffData dd;
	int i, label, curl;
    FILE* newfp;
    struct stat buf;

	sprintf(filename,"%s/diff2_%d%s", filepath, citycode, suffix);
    if (stat(filename, &buf) < 0) 
        return -1;
	dfile_content = read_whole_file(filename, &dfilesize);
	//删除diff文件
    remove((char*)filename);
	sprintf(filename,"%s/sw2_%d%s", filepath, citycode, suffix);
	ofile_content = read_whole_file(filename, &ofilesize);
	//删除原词库文件
    remove((char*)filename);
    newfp = fopen(filename, "wb");	
	i = 0;
	label = 0;
	while (i < dfilesize) {
		memcpy(&dd, &dfile_content[i], sizeof(DiffData));
		curl = 0;

		curl = dd.label[0] + dd.label[1] * 256 + dd.label[2] * 65536;
		i += sizeof(dd);
		if (label < curl) {
			fwrite(&ofile_content[label * wordlen], 1, wordlen * (curl - label), newfp);
			label = curl;
		}
		if (dd.command == 1) {
			fwrite(&dfile_content[i], 1, wordlen * dd.count, newfp);
			i += wordlen * dd.count;
		}
		if (dd.command == 2) {
			label += dd.count;
		}
	}
    if (wordlen * label < ofilesize) {
        fwrite(&ofile_content[label * wordlen], 1, ofilesize - wordlen * label, newfp);
    }

	free(ofile_content);
	free(dfile_content);
    fflush(newfp);
    fclose(newfp);
	return 1;
}

void merge(int citycode, char* filepath) {

	mergefile(citycode, "_s", filepath, sizeof(SWord));
	mergefile(citycode, "_l", filepath, sizeof(LWord));
}

int tk_inf(char* srcname, char* dstname)
{
    int ret;
    unsigned int have, actual;
    z_stream strm;
    unsigned char in[CHUNK];
    unsigned char out[CHUNK];
	FILE *source, *dest;
	if ((source = fopen(srcname, "rb")) <= 0)
		return 0;
	if((dest = fopen(dstname, "wb")) <= 0)
		return 0;
	fseek(source, 28, SEEK_SET); 
    /* allocate inflate state */
    strm.zalloc = Z_NULL;
    strm.zfree = Z_NULL;
    strm.opaque = Z_NULL;
    strm.avail_in = 0;
    strm.next_in = Z_NULL;
    ret = inflateInit(&strm);
    if (ret != Z_OK)
        return ret;
	
    /* decompress until deflate stream ends or end of file */
    do {
        actual = fread(in, 1, CHUNK, source);
		strm.avail_in = actual;
        if (ferror(source)) {
            (void)inflateEnd(&strm);
            return Z_ERRNO;
        }
        if (strm.avail_in == 0)
            break;
        strm.next_in = in;
        /* run inflate() on input until output buffer not full */
        do {
            strm.avail_out = CHUNK;
            strm.next_out = out;
            ret = inflate(&strm, Z_NO_FLUSH);
            if(ret == Z_STREAM_ERROR)  /* state not clobbered */
				return Z_STREAM_ERROR;
            switch (ret) {
            case Z_NEED_DICT:
                ret = Z_DATA_ERROR;     /* and fall through */
            case Z_DATA_ERROR:
            case Z_MEM_ERROR:
                (void)inflateEnd(&strm);
                return ret;
            }
            have = CHUNK - strm.avail_out;
			actual = fwrite(out, 1, have, dest);
            if (actual != have || ferror(dest)) {
                (void)inflateEnd(&strm);
                return Z_ERRNO;
            }
        } while (strm.avail_out == 0);
		
        /* done when inflate() says it's done */
    } while (ret != Z_STREAM_END);
	
    /* clean up and return */
	fclose(source);
	fclose(dest);
    (void)inflateEnd(&strm);
    return ret == Z_STREAM_END ? Z_OK : Z_DATA_ERROR;
}

int tk_decompress(int citycode, char* filepath) {
	char filename[FNL], srcname[FNL], dstname[FNL];
	char tz[2];
	char chr;
	unsigned int length_index, length_s, length_l, sov, snv;
	unsigned int deflate_sz;//压缩数据块的长度
	unsigned int filelen, actual;
	int ret;
    unsigned int have, left;
	unsigned int writenlen, expectedlen;
    z_stream strm;
    unsigned char in[CHUNK];
    unsigned char out[CHUNK];
	FILE *source, *dest;

	sprintf(srcname,"%s/%d", filepath, citycode);
	if ((source = fopen(srcname, "rb")) <= 0)
		return 0;

	fread(&sov, 1, 4, source);
	fread(&snv, 1, 4, source);

	fread(&length_index, 1, 4, source);
	fread(&length_s, 1, 4, source);
	fread(&length_l, 1, 4, source);
	fread(tz, 1, 2, source);
	if (memcmp(tz, "tz", 2)!=0)
		return 0;
	fread(&chr, 1, 1, source);
	if (chr != 1)
		return 0;
	fread(&chr, 1, 1, source);
	if (chr != 0)
		return 0;
	fread(&deflate_sz, 1, 4, source);
	fseek(source, 0, SEEK_END); 
	filelen = ftell(source); 
	if (deflate_sz != (filelen - 28))
		return 0;
	sprintf(dstname,"%s/sw2_%d_index", filepath, citycode);
    //删除原词库索引文件
    remove(dstname);
    if((dest = fopen(dstname, "wb")) <= 0)
		return 0;
	fseek(source, 28, SEEK_SET); 
    /* allocate inflate state */
    strm.zalloc = Z_NULL;
    strm.zfree = Z_NULL;
    strm.opaque = Z_NULL;
    strm.avail_in = 0;
    strm.next_in = Z_NULL;
    ret = inflateInit(&strm);
    if (ret != Z_OK)
        return ret;
	
    /* decompress until deflate stream ends or end of file */
	writenlen = 0;
	expectedlen = length_index;
    do {
        actual = fread(in, 1, CHUNK, source);
		strm.avail_in = actual;
        if (ferror(source)) {
            perror("error:");
            (void)inflateEnd(&strm);
            return 0;
        }
        if (strm.avail_in == 0)
            break;
        strm.next_in = in;
        /* run inflate() on input until output buffer not full */
        do {
            strm.avail_out = CHUNK;
            strm.next_out = out;
            ret = inflate(&strm, Z_NO_FLUSH);
            if(ret == Z_STREAM_ERROR)  /* state not clobbered */
				return 0;
            switch (ret) {
            case Z_NEED_DICT:
                ret = Z_DATA_ERROR;     /* and fall through */
            case Z_DATA_ERROR:
            case Z_MEM_ERROR:
                (void)inflateEnd(&strm);
                return 0;
            }
            have = CHUNK - strm.avail_out;
			if (writenlen + have <= expectedlen) {
				fwrite(out, 1, have, dest);
				writenlen += have;
			}
			else {
				fwrite(out, 1, expectedlen - writenlen, dest);
				left = have - (expectedlen - writenlen);
				fclose(dest);
				if (sov == 0) {
					if (expectedlen == length_index) {
						sprintf(filename,"%s/sw2_%d_s", filepath, citycode);
                        //直接删除原词库文件
                        remove(filename);
						if((dest = fopen(filename, "wb")) <= 0)
		                    return 0;
						expectedlen = length_index + length_s;
					}
					else if (expectedlen == length_index + length_s) {
						sprintf(filename,"%s/sw2_%d_l", filepath, citycode);
                        //直接删除原词库文件
                        remove(filename);
						if((dest = fopen(filename, "wb")) <= 0)
		                    return 0;
						expectedlen = length_index + length_s +length_l;
					}
				}
				else {
					if (expectedlen == length_index) {
						sprintf(filename,"%s/diff2_%d_s", filepath, citycode);
                        remove(filename);
						if((dest = fopen(filename, "wb")) <= 0)
		                    return 0;
						expectedlen = length_index + length_s;
					}
					else if (expectedlen == length_index + length_s) {
						sprintf(filename,"%s/diff2_%d_l", filepath, citycode);
                        remove(filename);
						if((dest = fopen(filename, "wb")) <= 0)
		                    return 0;
						expectedlen = length_index + length_s +length_l;
					}
				}
				if (left <= expectedlen - length_index)
					fwrite(out + have - left, 1, left, dest);
				else {
					fwrite(out + have - left, 1, length_s, dest);
					left = have - (expectedlen - writenlen);
					fclose(dest);
					if (sov == 0) {
						sprintf(filename,"%s/sw2_%d_l", filepath, citycode);
                        remove(filename);
						if((dest = fopen(filename, "wb")) <= 0)
		                    return 0;
						expectedlen = length_index + length_s +length_l;
					}
					else {
						sprintf(filename,"%s/diff2_%d_l", filepath, citycode);
                        remove(filename);
						if((dest = fopen(filename, "wb")) <= 0)
		                    return 0;
						expectedlen = length_index + length_s +length_l;
					}
					fwrite(out + have - left, 1, left, dest);
				}
				writenlen += have;
			}
        } while (strm.avail_out == 0);
		
        /* done when inflate() says it's done */
    } while (ret != Z_STREAM_END);
	
    /* clean up and return */
	fclose(source);
    fclose(dest);
    (void)inflateEnd(&strm);

	if (sov != 0)
        merge(citycode, filepath);
    return 1;
}

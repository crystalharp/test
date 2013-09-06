//
//  tk_file_util.c
//  tigermap
//
//  Created by Chen Ming on 13-6-13.
//  Copyright (c) 2013年 TigerKnows. All rights reserved.
//

#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>
#include <assert.h>
#include "tk_file_util.h"


#ifndef WINDOWS_PLAT
#include <sys/stat.h>
#endif

unsigned int tk_get_file_size(const char *file_name) {
#ifdef WINDOWS_PLAT
    int fsize;
    FILE *fp = fopen(file_name, "rb");
    if (!fp) {
        tk_set_result(TK_STATUS_FILE_OPEN_FAILED);
        return 0;
    }
    fseek(fp, 0, SEEK_END);
    fsize = ftell(fp);
    fclose(fp);
    tk_set_result(TK_STATUS_SUCCESS);
    return fsize;
#else
    struct stat buf;
    if(stat(file_name, &buf) < 0) {
        tk_set_result(TK_STATUS_FILE_OPEN_FAILED);
        return 0;
    }
    tk_set_result(TK_STATUS_SUCCESS);
    return buf.st_size;
#endif
}

unsigned char *tk_read_file_content(const char *file_name, unsigned int *size) {
    FILE *fh;
	unsigned char *content = NULL;
    unsigned int fsize;
    assert(file_name != NULL && size != NULL);
	if ((fh = fopen(file_name, "rb")) == NULL) {
		tk_set_result(TK_STATUS_FILE_OPEN_FAILED);
		return NULL;
	}
    fsize = tk_get_file_size(file_name);
    if (size != NULL) {
        *size = fsize;
    }
    
    if (fsize > 0) {
        content = malloc(fsize);
        if (!content) {
            tk_set_result(TK_STATUS_NO_MEMORY);
            fclose(fh);
            return NULL;
        }
        else {
            if (fread(content, 1, fsize, fh) != fsize) {
                tk_set_result(TK_STATUS_FILE_READ_ERROR);
                free(content);
                return NULL;
            }
        }
        fclose(fh);
        tk_set_result(TK_STATUS_SUCCESS);
        return content;
    }
    else {
        fclose(fh);
        tk_set_result(TK_STATUS_EMPTY_FILE);
        return NULL;
    }
}

char *tk_read_text_file_content(const char *file_name, unsigned int *length) {
    FILE *fh;
	char *content = NULL;
    unsigned int fsize;
    
	if ((fh = fopen(file_name, "rb")) == NULL) {
		tk_set_result(TK_STATUS_FILE_OPEN_FAILED);
		return NULL;
	}
    fsize = tk_get_file_size(file_name);
    if (tk_get_last_result() != TK_STATUS_SUCCESS) {
        return NULL;
    }
    if (length != NULL) {
        *length = fsize;
    }
    
    content = malloc(fsize + 1);
    if (!content) {
        tk_set_result(TK_STATUS_NO_MEMORY);
        fclose(fh);
        return NULL;
    }
    
    if (fsize > 0) {
        if (fread(content, 1, fsize, fh) != fsize) {
            tk_set_result(TK_STATUS_FILE_READ_ERROR);
            free(content);
            return NULL;
        }
    }
    content[fsize] = '\0';
    fclose(fh);
    tk_set_result(TK_STATUS_SUCCESS);
    return content;
}

int tk_mkdir(const char *file_path)
{
#ifdef WIN32
	return CreateDirectory(file_path, NULL);
#else
	return mkdir(file_path, 0755); /* rwxr-xr-x */
#endif
}


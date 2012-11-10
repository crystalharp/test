/*
 * =====================================================================================
 *
 *       Filename:  tk_util.c
 *
 *    Description:  
 *
 *        Version:  1.0
 *        Created:  2010年12月28日 18时16分29秒
 *       Revision:  none
 *       Compiler:  gcc
 *
 *         Author:  YOUR NAME (), 
 *        Company:  
 *
 * =====================================================================================
 */
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include "tk_util.h"
#include "tk_log.h"
#include "tk_config.h"

#ifndef WINDOWS_PLAT
#include <sys/stat.h>
#endif

/**
 * purpose: get the file size of giving file handle
 * warning: this function will rewind the file handle!
 */
int get_fsize(char *fn) 
{
#ifdef WINDOWS_PLAT
    //TODO:need find a better implementation
    int fsize;
    FILE *fp = fopen(fn, "rb");
    fseek(fp, 0, SEEK_END);
    fsize = ftell(fp);
    fclose(fp);
    return fsize;
#else
    struct stat buf;
    stat(fn, &buf);
    return buf.st_size;
#endif
}

unsigned char *read_whole_file(char *file_name, unsigned int *size) 
{
	FILE *fh;
	unsigned char *str = NULL;
    int fsize;
    
	if ((fh = fopen(file_name, "rb")) == NULL) {
		LOG_ERR("Open file %s failed: %s\n", file_name, strerror(errno));
		return NULL;
	}

    fsize = get_fsize(file_name);
    if (size != NULL) {
        *size = fsize;
    }

    if (fsize > 0) {
        str = xmalloc(fsize);
        if (fread(str, 1, fsize, fh) != fsize) {
            LOG_ERR("fread error: %s\n", strerror(errno));
        }
    }
	fclose(fh);
    return str;
}

/*============================================
 *  Memory allocate part
 *============================================ */

void *xmalloc(size_t size)
{
    void *res;

    res = malloc(size);
    if (!res) {
        LOG_ERR("Malloc: Not enough memory.\n");
    }
    return res;
}

void *xrealloc(void *obj, size_t size)
{
    void *res;

    if (obj) {
        res = realloc(obj, size);
    } else {
        res = malloc(size);
    }
    if (!res) {
        LOG_ERR("Realloc: Not enough memory.\n");
    }
    return res;
}

void *xcalloc(size_t nobj, size_t size)
{
    void *res;

    //res = malloc(nobj * size);
    res = calloc(nobj, size);
    if (!res) {
        LOG_ERR("Calloc: Not enough memory.\n");
    } 
    return res;
}

/*==============================================
 *  arraylist used internal 
 *============================================== */

#define ARRAY_LIST_MIN_SIZE 32 
#define ARRAY_LIST_MAX_STEP 256

void arraylist_init(struct array_list *this, array_list_free_fn *free_fn) 
{
	this->size = 0;
    this->length = 0;
	this->free_fn = free_fn;
}

void arraylist_reset(struct array_list *this)
{
	int i;
	if (this->length) {
		for (i = 0; i < this->length; i++) {
		    this->free_fn(this->array[i]);
		}
        this->length = 0;
	}
    if (this->size) {
		free(this->array);
        this->size = 0;
    }
}

void arraylist_add(struct array_list *this, void *data) 
{
    int newsize;
    if (this->length == this->size) {
        newsize = this->size + TK_MIN(ARRAY_LIST_MAX_STEP, 0.5 * this->size);
        newsize = TK_MAX(ARRAY_LIST_MIN_SIZE, newsize);
        this->size = newsize;
        this->array = xrealloc(this->array, this->size * sizeof(void *));
    }
	this->array[this->length] = data;
    this->length++;
}

void arraylist_del(struct array_list *this, int idx) 
{
	int j;
	void *del;

	if (idx >= 0 && idx < this->length) {
		del = this->array[idx];
		for (j = idx; j < this->length-1; j++) {
			this->array[j] = this->array[j+1];
		}
		this->array[this->length-1] = NULL;
		this->free_fn(del);
		this->length--;
        if (this->length > ARRAY_LIST_MIN_SIZE && this->length < (0.5 * this->size)) {
            this->size *= 0.5;
            this->array = xrealloc(this->array, this->size);
        }
	}
	return;
}

void *arraylist_get(struct array_list *this, int idx)
{
	if (idx < 0 || idx >= this->length) {
		return NULL;
    }
	return this->array[idx];
}

int tkmc_mkdir(const char *file_path)
{
#ifdef WIN32
	return CreateDirectory(file_path, NULL);
#else
	return mkdir(file_path, 0755); /* rwxr-xr-x */
#endif
}

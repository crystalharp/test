/*
 * =====================================================================================
 *
 *       Filename:  tk_util.h
 *
 *    Description:  
 *
 *        Version:  1.0
 *        Created:  2010年12月28日 18时16分08秒
 *       Revision:  none
 *       Compiler:  gcc
 *
 *         Author:  MapEngine Group
 *        Company:  TigerKnows
 *
 * =====================================================================================
 */

#ifndef __TK_UTIL_H
#define __TK_UTIL_H

#define TK_MAX_NAME 64
#define TK_MAX_PATH  256 /* The max path length of file */ 
#define TK_MAX_LINE_LENGTH 1024     /* the max length of each line in files */
#define TK_MIN(a, b) (((a) < (b))? (a) : (b))
#define TK_MAX(a, b) (((a) > (b))? (a) : (b))
#define TK_ABS(x)    ((x) >= 0.0 ? (x) : (-(x)))

#define GETNUM2B(buff) (*(buff) << 8 | *(buff + 1))
#define GETNUM3B(buff) (*(buff) << 16 | *(buff + 1) << 8 | *(buff + 2))
#define GETNUM4B(buff) (*(buff) << 24 | *(buff + 1) << 16 | *(buff + 2) << 8 | *(buff + 3))

extern int get_fsize(char *fname);
extern unsigned char *read_whole_file(char *file_name, unsigned int *size);


/*============================================
 *Memory allocate part
 *============================================ */

extern void *xmalloc(size_t size);
extern void *xrealloc(void *obj, size_t size);
extern void *xcalloc(size_t nobj, size_t size);


/*============================================
 *arraylist used internal 
 *============================================ */
typedef void (array_list_free_fn)(void *);

struct array_list {
    void **array;
    int length;
    int size;
    array_list_free_fn *free_fn;
};

void arraylist_init(struct array_list *li, array_list_free_fn *free_fn);
void arraylist_reset(struct array_list *this);
void arraylist_add(struct array_list *this, void *data);
void arraylist_del(struct array_list *this, int idx);
void *arraylist_get(struct array_list *this, int idx);


/*============================================
 *Misc utility routine
 *============================================ */
extern int tkmc_mkdir(const char *file_path);
#endif

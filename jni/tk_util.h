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

#include "tk_error.h"
#include "tk_types.h"

#define TK_MAX_NAME 64
#define TK_MAX_PATH  256 /* The max path length of file */ 
#define TK_MAX_LINE_LENGTH 1024     /* the max length of each line in files */
#define TK_MIN(a, b) (((a) < (b))? (a) : (b))
#define TK_MAX(a, b) (((a) > (b))? (a) : (b))
#define TK_ABS(x)    ((x) >= 0.0 ? (x) : (-(x)))

#define GETNUM2B(buff) (*(buff) << 8 | *(buff + 1))
#define GETNUM3B(buff) (*(buff) << 16 | *(buff + 1) << 8 | *(buff + 2))
#define GETNUM4B(buff) (*(buff) << 24 | *(buff + 1) << 16 | *(buff + 2) << 8 | *(buff + 3))

# define tk_atomic_int_inc(x) ((void) __sync_fetch_and_add(x, 1))
# define tk_atomic_int_dec(x) ((void) __sync_fetch_and_add(x, -1))
# define tk_atomic_int_dec_and_test(x) (__sync_fetch_and_add(x, -1) == 1)

#define GET_RED(color) (((unsigned)color >> (16)) & (0xff))
#define GET_GREEN(color) (((unsigned)color >> (8)) & (0xff))
#define GET_BLUE(color) (((unsigned)color) & (0xff))

#define TK_ARRAY_LENGTH(__array) ((int) (sizeof (__array) / sizeof (__array[0])))

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
 * bit buf used internal
 *============================================ */
typedef struct _buf_info {
    unsigned char *buf;
    int buf_pos;
    unsigned int buf_length;
    unsigned char remain_bits;
    unsigned char remain_value;
} tk_buf_info_t;

unsigned int tk_read_data_from_buf(tk_buf_info_t *data_buf, unsigned char bits);
tk_status_t tk_skip_buf_bits(tk_buf_info_t *data_buf, unsigned int bits);
tk_status_t tk_skip_buf_bytes(tk_buf_info_t *data_buf, unsigned int length);
tk_status_t tk_read_string_from_buf(tk_buf_info_t *data_buf, char *res, unsigned int length);
void tk_align_buf(tk_buf_info_t *data_buf);
unsigned int tk_buf_info_read_xint(tk_buf_info_t *data_buf);

/*============================================
 * point buf used internal
 * 不能用来分配给指针来分段引用，因为内存有可能重新分配(realloc)，使得原指针失效，只能使用索引或者全部拷贝
 *============================================ */
typedef struct _tk_point_buf {
#define TK_POINT_BUF_MAX_INCREASE_NUM 64
    tk_point_t *points;
    int point_num;
    int size;
    tk_point_t points_embedded[128];
} tk_point_buf_t;

tk_status_t tk_point_buf_add_one(tk_point_buf_t *point_buf, int x, int y, int level_code);
tk_status_t tk_point_buf_add_point(tk_point_buf_t *point_buf, tk_point_t *point_to_add);
tk_status_t tk_point_buf_add_points(tk_point_buf_t *point_buf, tk_point_t *points, int point_num);
void tk_point_buf_clean(tk_point_buf_t *point_buf);

/*============================================
 *Misc utility routine
 *============================================ */
extern int tk_mkdir(const char *file_path);

// borrow from cairo
extern unsigned long tk_hash_string (const char *c);

void tk_gps_latlon_transform(double lon, double lat, int wg_heit, int wg_week, unsigned int wg_time, double* out_lon, double* out_lat);

#endif


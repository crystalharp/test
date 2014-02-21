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
#include <math.h>
#include "tk_util.h"
#include "tk_log.h"
#include "tk_config.h"
#include "tk_error.h"

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

unsigned long tk_hash_string (const char *c)
{
    /* This is the djb2 hash. */
    unsigned long hash = 5381;
    while (c && *c)
        hash = ((hash << 5) + hash) + *c++;
    return hash;
}

// //todo: 会不会太耗时？考虑用局部变量
unsigned int tk_read_data_from_buf(tk_buf_info_t *data_buf, unsigned char bits) {
    unsigned int result_value;
    if (bits >= 32)
        return 0xffffffff;
    if (bits <= data_buf->remain_bits) {
        data_buf->remain_bits -= bits;
        result_value = data_buf->remain_value >> data_buf->remain_bits;
        data_buf->remain_value = (data_buf->remain_value & ((1 << data_buf->remain_bits) - 1));
        return result_value;
    } else {
        result_value = data_buf->remain_value;
        bits -= data_buf->remain_bits;
        while (bits >= 8) {
            result_value = result_value << 8;
            result_value |= data_buf->buf[data_buf->buf_pos];
            bits -= 8;
            ++ data_buf->buf_pos;
            if (data_buf->buf_pos > data_buf->buf_length) {
                tk_set_result(TK_STATUS_BUF_OVERFLOAT);
                return -1;
            }
        }
        if (bits != 0) {
            data_buf->remain_bits = 8 - bits;
            data_buf->remain_value = (unsigned char)(data_buf->buf[data_buf->buf_pos] & ((1 << data_buf->remain_bits) - 1));
            result_value = (data_buf->buf[data_buf->buf_pos] >> data_buf->remain_bits) | (result_value << bits);
            ++ data_buf->buf_pos;
            if (data_buf->buf_pos > data_buf->buf_length) {
                tk_set_result(TK_STATUS_BUF_OVERFLOAT);
                return -1;
            }
        }
        else {
            data_buf->remain_value = 0;
            data_buf->remain_bits = 0;
        }
        return result_value;
    }
}

void tk_align_buf(tk_buf_info_t *data_buf) {
    if (data_buf->remain_bits > 0) {
        ++data_buf->buf_pos;
        data_buf->remain_bits = 0;
        data_buf->remain_value = 0;
    }
}

unsigned int tk_buf_info_read_xint(tk_buf_info_t *data_buf) {
    int i = 0;
    unsigned int addition_length_part = tk_read_data_from_buf(data_buf, 8);
    unsigned int addition_length = addition_length_part & 0x7f;
    while ((addition_length_part & 0x80) != 0) {
        ++i;
        addition_length_part = tk_read_data_from_buf(data_buf, 8);
        addition_length = (addition_length << 7) + (addition_length_part & 0x7f);
    }
    return addition_length;
}

tk_status_t tk_skip_buf_bits(tk_buf_info_t *data_buf, unsigned int bits) {
    int byte_num, remain_bit;
    if (bits == 0) {
        return TK_STATUS_SUCCESS;
    }
    if (bits <= data_buf->remain_bits) {
        data_buf->remain_bits -= bits;
        data_buf->remain_value = (data_buf->remain_value & ((1 << data_buf->remain_bits) - 1));
        return TK_STATUS_SUCCESS;
    }
    bits -= data_buf->remain_bits;
    byte_num = bits / 8;
    remain_bit = bits % 8;
    if (data_buf->buf_pos + byte_num < data_buf->buf_length) {
        data_buf->buf_pos += byte_num;
        if (remain_bit != 0) {
            data_buf->remain_bits = 8 - remain_bit;
            data_buf->remain_value = (unsigned char)(data_buf->buf[data_buf->buf_pos] & ((1 << data_buf->remain_bits) - 1));
            ++ data_buf->buf_pos;
            if (data_buf->buf_pos > data_buf->buf_length) {
                return TK_STATUS_BUF_OVERFLOAT;
            }
        }
        else {
            data_buf->remain_value = 0;
            data_buf->remain_bits = 0;
        }
        return TK_STATUS_SUCCESS;
    }
    else {
        return TK_STATUS_BUF_OVERFLOAT;
    }
}

tk_status_t tk_skip_buf_bytes(tk_buf_info_t *data_buf, unsigned int length) {
    if (length == 0) {
        return TK_STATUS_SUCCESS;
    }
    if (data_buf->buf_pos + length < data_buf->buf_length) {
        data_buf->buf_pos += length;
        data_buf->remain_value = (unsigned char)(data_buf->buf[data_buf->buf_pos - 1]) & ((1 << data_buf->remain_bits) -1);
        return TK_STATUS_SUCCESS;
    }
    else {
        return TK_STATUS_BUF_OVERFLOAT;
    }
}

tk_status_t tk_read_string_from_buf(tk_buf_info_t *data_buf, char *res, unsigned int length) {
    unsigned int i;
    if (length == 0) {
        return TK_STATUS_SUCCESS;
    }
    if (data_buf->buf_pos + length < data_buf->buf_length) {
        res[0] =  ((unsigned char)(data_buf->remain_value << (8 - data_buf->remain_bits))) | (data_buf->buf[data_buf->buf_pos] >> (data_buf->remain_bits));
        for (i = 1; i < length; i++) {
            res[i] =  (unsigned char)((((unsigned short)data_buf->buf[data_buf->buf_pos + i - 1]) << (8 - data_buf->remain_bits))
                                      | (data_buf->buf[data_buf->buf_pos + i] >> (data_buf->remain_bits)));
        }
        data_buf->buf_pos += length;
        data_buf->remain_value = (unsigned char)(data_buf->buf[data_buf->buf_pos - 1]) & ((1 << data_buf->remain_bits) -1);
        return TK_STATUS_SUCCESS;
    }
    else {
        return TK_STATUS_BUF_OVERFLOAT;
    }
}

static tk_status_t tk_point_buf_grow(tk_point_buf_t *buf) {
    tk_point_t *new_points;
    int old_size = buf->size;
    int new_size = old_size < TK_POINT_BUF_MAX_INCREASE_NUM ? (old_size + old_size) : (old_size + TK_POINT_BUF_MAX_INCREASE_NUM);
    
    if (buf->points == buf->points_embedded) {
        new_points = calloc(new_size, sizeof (tk_point_t));
        if (new_points != NULL)
            memcpy (new_points, buf->points, old_size * sizeof (tk_point_t));
    } else {
        new_points = realloc(buf->points, new_size * sizeof (tk_point_t));
    }
    
    if (new_points == NULL) {
        return TK_STATUS_NO_MEMORY;
    }
    buf->points = new_points;
    buf->size = new_size;
    return TK_STATUS_SUCCESS;
}

static tk_status_t tk_point_buf_grow_with_num(tk_point_buf_t *buf, int num) {
    tk_point_t *new_points;
    int old_size = buf->size;
    int new_size = old_size < TK_POINT_BUF_MAX_INCREASE_NUM ? (old_size + old_size) : (old_size + TK_POINT_BUF_MAX_INCREASE_NUM);
    while (new_size - buf->point_num < num) {
        new_size = new_size < TK_POINT_BUF_MAX_INCREASE_NUM ? (new_size + new_size) : (new_size + TK_POINT_BUF_MAX_INCREASE_NUM);
    }
    if (buf->points == buf->points_embedded) {
        new_points = calloc(new_size, sizeof (tk_point_t));
        if (new_points != NULL)
            memcpy (new_points, buf->points, old_size * sizeof (tk_point_t));
    } else {
        new_points = realloc(buf->points, new_size * sizeof (tk_point_t));
    }
    
    if (new_points == NULL) {
        return TK_STATUS_NO_MEMORY;
    }
    buf->points = new_points;
    buf->size = new_size;
    return TK_STATUS_SUCCESS;
}

tk_status_t tk_point_buf_add_one(tk_point_buf_t *point_buf, int x, int y, int level_code) {
    tk_point_t *point = NULL;
    if (point_buf->point_num == point_buf->size) {
        if(tk_point_buf_grow(point_buf) == TK_STATUS_NO_MEMORY) {
            return TK_STATUS_NO_MEMORY;
        }
    }
    point = point_buf->points + point_buf->point_num;
    point->x = x;
    point->y = y;
    point->level_code = level_code;
    ++ point_buf->point_num;
    return TK_STATUS_SUCCESS;
}

tk_status_t tk_point_buf_add_point(tk_point_buf_t *point_buf, tk_point_t *point_to_add) {
    tk_point_t *point = NULL;
    if (point_buf->point_num == point_buf->size) {
        if(tk_point_buf_grow(point_buf) == TK_STATUS_NO_MEMORY) {
            return TK_STATUS_NO_MEMORY;
        }
    }
    point = point_buf->points + point_buf->point_num;
    point->x = point_to_add->x;
    point->y = point_to_add->y;
    point->level_code = point_to_add->level_code;
    ++ point_buf->point_num;
    return TK_STATUS_SUCCESS;
}

tk_status_t tk_point_buf_add_points(tk_point_buf_t *point_buf, tk_point_t *points, int point_num) {
    if (point_num + point_buf->point_num > point_buf->size) {
        if(tk_point_buf_grow_with_num(point_buf, point_num) == TK_STATUS_NO_MEMORY) {
            return TK_STATUS_NO_MEMORY;
        }
    }
    memcpy(point_buf->points + point_buf->point_num, points, point_num * sizeof(tk_point_t));
    point_buf->point_num += point_num;
    return TK_STATUS_SUCCESS;
}

void tk_point_buf_clean(tk_point_buf_t *buf) {
    if (!buf) {
        return;
    }
    buf->size = TK_ARRAY_LENGTH(buf->points_embedded);
    buf->point_num = 0;
    if (buf->points != buf->points_embedded) {
        if (buf->points) {
            free(buf->points);
        }
        buf->points = buf->points_embedded;
    }
}


/* ===================
 * GPS Transformer
 * ===================*/
pthread_mutex_t _transformer_lock = PTHREAD_MUTEX_INITIALIZER;
static double casm_rr ;
static unsigned int casm_t1;
static unsigned int casm_t2;
static double casm_x1;
static double casm_y1;
static double casm_x2;
static double casm_y2;
static double casm_f;

static double yj_sin2(double x) {
	double tt ;
	double ss;
    int ff ;
	double s2;
	int cc;
	ff=0;
	if (x<0) {
		x=-x;
		ff=1;
	}
	cc=(int)(x/6.28318530717959);
	tt=x-cc*6.28318530717959;
	if (tt>3.1415926535897932) {
	    tt=tt-3.1415926535897932;
        if (ff==1)
            ff=0;
	    else if (ff==0)
            ff=1;
	}
    
	x=tt;
	ss=x;
	s2=x;
	tt=tt*tt;
	s2=s2*tt;
	ss=ss-s2* 0.166666666666667;
	s2=s2*tt;
	ss=ss+s2* 8.33333333333333E-03;
	s2=s2*tt;
	ss=ss-s2* 1.98412698412698E-04;
	s2=s2*tt;
	ss=ss+s2* 2.75573192239859E-06;
	s2=s2*tt;
	ss=ss-s2* 2.50521083854417E-08;
	if (ff==1)
		ss=-ss;
	return ss;
}

static double Transform_yj5(double x , double y ) {
	double tt;
	tt = 300 + 1 * x + 2 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * sqrt(sqrt(x*x));
	tt = tt + (20 *yj_sin2(18.849555921538764 * x ) + 20 * yj_sin2(6.283185307179588 * x))*0.6667;
	tt = tt + (20 * yj_sin2(3.141592653589794 * x ) + 40 * yj_sin2(1.047197551196598 * x))*0.6667;
	tt = tt + (150 * yj_sin2(0.2617993877991495 * x) + 300 * yj_sin2(0.1047197551196598 * x))*0.6667;
	return tt;
}

static double Transform_yjy5(double x , double y ) {
	double tt ;
	tt = -100 +  2 * x + 3 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * sqrt(sqrt(x*x));
	tt = tt + (20 * yj_sin2(18.849555921538764 * x) + 20 * yj_sin2(6.283185307179588 * x))*0.6667;
	tt = tt + (20 * yj_sin2(3.141592653589794 * y)+ 40 * yj_sin2(1.047197551196598 * y))*0.6667;
	tt = tt + (160 * yj_sin2(0.2617993877991495 * y) + 320 * yj_sin2(0.1047197551196598 * y))*0.6667;
	return tt;
}

static double Transform_jy5(double x , double xx ) {
	double n ;
	double a ;
	double e ;
	a = 6378245;
	e = 0.00669342;
	n = sqrt (1 - e * yj_sin2(x * 0.0174532925199433) * yj_sin2(x * 0.0174532925199433) );
	n = (xx * 180) /(a / n * cos(x * 0.0174532925199433) * 3.1415926) ;
	return n;
}

static double Transform_jyj5(double x , double yy ) {
	double m ;
	double a ;
	double e ;
    double mm;
	a = 6378245;
	e = 0.00669342;
	mm= 1 - e * yj_sin2(x * 0.0174532925199433) * yj_sin2(x * 0.0174532925199433) ;
	m = (a * (1 - e)) / (mm * sqrt(mm));
	return (yy * 180) / (m * 3.1415926);
}

static double random_yj() {
    /* int t;
     int casm_a ;
     int casm_c ;
     casm_a = 314159269;
     casm_c = 453806245;
     casm_rr = casm_a * casm_rr + casm_c ;
     t = casm_rr /2 ;
     casm_rr = casm_rr - t * 2;
     casm_rr = casm_rr / 2 ;
     */
	return (casm_rr);
}

static void IniCasm(unsigned int w_time, unsigned int w_lng, unsigned int w_lat) {
	int tt;
	casm_t1 = w_time ;
	casm_t2 = w_time ;
	tt = (int)(w_time / 0.357);
	casm_rr = w_time-tt * 0.357;
	if (w_time == 0)
        casm_rr = 0.3;
	casm_x1 = w_lng;
	casm_y1 = w_lat;
	casm_x2 = w_lng;
	casm_y2 = w_lat;
	casm_f = 3;
}

static unsigned int wgtochina_lb(int wg_flag, unsigned int wg_lng, unsigned int wg_lat,
                          int wg_heit,  int wg_week, unsigned int wg_time,
                          unsigned int *china_lng, unsigned int *china_lat) {
	double x_add ;
	double y_add ;
	double h_add ;
	double x_l;
	double y_l;
	double casm_v ;
    double t1_t2;
    double x1_x2;
    double y1_y2;
    
    if (wg_heit>5000) {
		*china_lng = 0 ;
		*china_lat = 0;
		return 0xFFFF95FF;
	}
    
	x_l =  wg_lng;
	x_l =  x_l / 3686400.0;
	y_l =  wg_lat ;
	y_l =  y_l / 3686400.0;
    if (x_l < 72.004) {
		*china_lng = 0 ;
		*china_lat = 0;
		return 0xFFFF95FF;
	}
    
	if ( x_l > 137.8347) {
		*china_lng = 0 ;
		*china_lat = 0;
		return 0xFFFF95FF;
	}
    
	if (y_l < 0.8293) {
		*china_lng = 0 ;
		*china_lat = 0;
		return 0xFFFF95FF;
	}
	
    if ( y_l > 55.8271) {
		*china_lng = 0 ;
		*china_lat = 0;
		return 0xFFFF95FF;
	}
	
    if (wg_flag ==0) {
	    IniCasm(wg_time,wg_lng,wg_lat);
	    *china_lng = wg_lng;
	    *china_lat = wg_lat;
	    return 0x00000000;
	}
    
	casm_t2= wg_time ;
	t1_t2 =(double)(casm_t2 - casm_t1)/1000.0;
	if ( t1_t2<=0 ) {
		casm_t1= casm_t2 ;
        casm_f=casm_f + 1;
		casm_x1 = casm_x2;
        casm_f=casm_f + 1;
		casm_y1 = casm_y2;
        casm_f=casm_f + 1;
    } else {
        if ( t1_t2 > 120 ) {
			if (casm_f == 3) {
                casm_f = 0;
                casm_x2 = wg_lng;
                casm_y2 = wg_lat;
                x1_x2 = casm_x2 - casm_x1;
                y1_y2 = casm_y2 - casm_y1;
                casm_v = sqrt(x1_x2 * x1_x2 + y1_y2 * y1_y2 ) / t1_t2;
                if (casm_v > 3185) {
                    *china_lng = 0 ;
                    *china_lat = 0;
                    return (0xFFFF95FF);
			    }
                
            }
			casm_t1 = casm_t2 ;
            casm_f = casm_f + 1;
			casm_x1 = casm_x2;
            casm_f = casm_f + 1;
			casm_y1 = casm_y2;
            casm_f = casm_f + 1;
		}
	}
    
    x_add = Transform_yj5(x_l - 105, y_l - 35);
	y_add = Transform_yjy5(x_l - 105, y_l - 35);
	h_add = wg_heit;
    
	x_add = x_add + h_add * 0.001 + yj_sin2(wg_time*0.0174532925199433) + random_yj();
	y_add = y_add + h_add * 0.001 + yj_sin2(wg_time*0.0174532925199433) + random_yj();
	*china_lng = (unsigned int) ((x_l + Transform_jy5(y_l, x_add)) * 3686400);
	*china_lat = (unsigned int) ((y_l + Transform_jyj5(y_l, y_add)) * 3686400);
	return (0x00000000);
}

void tk_gps_latlon_transform(double lon, double lat, int wg_heit, int wg_week, unsigned int wg_time, double* out_lon, double* out_lat) {
	static int wg_flag = 0;
	unsigned int wg_lng = (unsigned int)(lon * (double)3600 * (double)1024);
	unsigned int wg_lat = (unsigned int)(lat * (double)3600 * (double)1024);
	unsigned int china_lng = 0;
	unsigned int china_lat = 0;
	unsigned int ret = 0;
    pthread_mutex_lock(&_transformer_lock);
	if(wg_flag == 0) {
		wgtochina_lb(wg_flag, wg_lng, wg_lat, wg_heit, wg_week, wg_time, &china_lng, &china_lat);
		wg_flag = 1;
	}
	ret = wgtochina_lb(wg_flag, wg_lng, wg_lat, wg_heit, wg_week, wg_time, &china_lng, &china_lat);
    pthread_mutex_unlock(&_transformer_lock);
	if(0 == ret) {
	    *out_lon = ((double)china_lng) / (double)(3600 * 1024);
	    *out_lat = ((double)china_lat) / (double)(3600 * 1024);
	} else {
	    *out_lon = 0.0;
	    *out_lat = 0.0;
	}
}

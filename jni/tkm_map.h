/*
 * =====================================================================================
 *
 *       Filename:  tkm_map.h
 *
 *    Description:  
 *
 *        Version:  1.0
 *        Created:  2010年12月28日 17时46分54秒
 *       Revision:  none
 *       Compiler:  gcc
 *
 *        Company:  
 *
 * =====================================================================================
 */

#ifndef __TKM_MAP_H
#define __TKM_MAP_H

#include "tkm_comm.h"
#define TILE_SIZE 256

extern struct tk_map_lostdata lostdata[TK_LOST_DATA_PIECE];
extern int lostdata_idx;
extern int l_opt;

extern struct tk_label *label_buffer;
extern int glabels_num;
extern unsigned char *label_name_buffer;
extern int label_name_len;
extern cairo_surface_t *surface; 
extern char png_buf[256*256*4];
extern int  png_buf_size;

/* ============================================ 
 *  Initialize and destroy the engine  
 *  =========================================== */

/*
 * Initialize the engine. Should only be called once at the very start 
 * @resdir: resource dir
 * @mapdir: map data dir
 * @w: the width of map
 * @h: the height of map
 * @bmpbuf: the bmp buffer
 * @need_opt: use L-option
 */
extern int tk_init_engine(const char *resdir, const char *mapdir, int w, int h, void *bmpbuf, int need_opt);
/*
 * Destroy the enine. Should be called for each tk_init_engine()
 */
extern void tk_destroy_engine();
/* 
 * reset the map's width and height 
 */
extern void tk_reset_matrix_size(int w, int h, void *bmpbuf); 
extern void tk_reset_font_size(float offset);
/*
 * purpose: get the size of the bmp buffer
 * @w: width
 * @h: height
 * return value: the size of the bmp buffer */
extern int tk_get_matrix_size(int w, int h); 


/* ============================================ 
 * Refresh the map buffer
 * ============================================ */

extern void tk_refresh_map_buffer();


/* ============================================ 
 * Map moving routines.
 * ============================================ */

/*
 * Move the map with latitude, longitude and zoom level
 * @lat: the latitude of the center
 * @lon: the longitude of the center
 * @zoom: the zoom level
 */
extern int tk_move_latlonzoom(double lat, double lon, int zoom);
/*
 * Move the map with latitude, longitude and the now-used zoom level
 * @lat: the latitude of the center
 * @lon: the longitude of the center
 */
extern int tk_move_latlon(double lat, double lon);
/*
 * Move the map with a deltax/deltay in pixel on screen
 * @dx: delta value in x axis
 * @dy: delta value in y axis
 */
extern void tk_move_delta(int dx, int dy);
/*
 * set the map to the scope specified by the leftop and right bottom lat/lon value
 * @dx: delta value in x axis
 * @dy: delta value in y axis
 */

extern int tk_set_scope(double lat0, double lon0, double lat1, double lon1, int w, int h);

/* ===========================================
 * Zoom control interfaces
 * =========================================== */

extern int tk_zoom_in(void);
extern int tk_zoom_out(void);
extern int tk_set_zoom(int level);
extern int tk_get_zoom();


/* ============================================
 * Region management interfaces
 * ============================================ */

/** 
 * purpose: recover map data file from the temp meta file
 * metafile: the name of the temp meta file
 * rid: the region id corresponding to the temp meta file
 * return value: -1 for fail, 0 for success
 */
extern int tk_init_region(char *metafile, int rid);
/** 
 * purpose: fill the buffer in the map data file 
 * @rid: the region id corresponding to the map data file
 * @off: the offset in the map data file
 * @len: the length of the buffer
 * @buf: the buffer
 * return value: -1 for false, 0 for success
 */
extern int tk_write_region(int rid, int off, int len, char *buf);

/**
 * purpose: get the percent of the downloaded data of a given region id.
 * @rid: region id
 * @ptotal_size: return the size of total region data
 * @pdownloaded_size: return the size of data downloaded already
 * return value: return 0 if success and save the offset and index of the offset 
 * in the global array lostdata[], furthermore we use the offset 0 to indicate
 * the meta data is required. return -1 if some error happens.
 * */
extern int tk_get_region_stat(int rid, int *ptotal_size, int *pdownloaded_size);
/* 
 * purpose: get the region file path from the region id
 * @rid: region id
 * return value: the pointer to the string of the whole path. */
extern const char *tk_get_region_path(int rid);
/**
 * purpose: get the version of the region data file whose region id is rid.
 * @rversion: a 6 byte array user pass to the function.
 * return value: if success, return 0 and the 6 byte version saved in rversion,
 * else return -1;
 */
extern int tk_get_region_version(int rid, unsigned char *rversion);

extern int tk_get_cur_rversion(unsigned char *rversion);
/** 
 * purpose: get the regionid according to the region name;
 * @rname: region name
 * return value: region id if found, -1 if not found
 * */
extern int tk_get_region_id(char *rname);
/** 
 * purpose: get the region name according to the regionid
 * @rid: the region id
 * return value: the region name
 * */
extern const char *tk_get_region_name(int rid);

/**
 * get the region id list of given city 
 * */
extern int *tk_get_rid_list(int cityid);

/* ============================================ 
 * Misc helper routines 
 * =========================================== */

/* 
 * purpose: get the province name according to the region id
 * @rid: region id
 * return value: the pointer to the string of the prov name */
extern const char *tk_get_provname(int rid);
/** 
 * purpose: get the city list according to the name of province.
 * @pname: the name of province.
 * @num_of_cities: used to return the number of cities in this province.
 * return value: the string array of cities.
 * */
extern char **tk_get_citylist(char *pname, int *num_of_cities);
extern int tk_get_cityid(const char *cname);
/**
 * purpose: get the region info from the region id
 * @rid: region id
 * return value: the pointer to the string of the region info 
 * in the format "中文名字, English name, file size, city ename". 
 */
extern char *tk_get_region_info(int rid);
/**
 * purpose: get the city info from the city id
 * cityid: city id
 * return value: the pointer to the string of the city info in the format
 * "中文名字, English name, latitude, longitude, level, 省份中文名字, province
 * English name" such as
 * "北京,beijing,39.90415599,116.397772995,11, 北京,beijing"
 */
extern char *tk_get_city_info(int cityid);
/**
 * purpose: get the region list of the city 
 * cityid: city id
 * return value: the pointer to the string of all the region names
 */
extern char *tk_get_regionlist(int cityid);
/** 
 * purpose: remove the city directory and the data
 * */

/**
 * purpose: get the province list 
 * return value: the pointer to the string of all the province Chinese names
 */
char *tk_get_provincelist();

/**
 * purpose: get the province list 
 * return value: the pointer to the string of all the province English names
 */
char *tk_get_eprovincelist();

extern void tk_remove_city_data(const char *cname);
/** 
 * purpose: remove the region data and checksum file. 
 * */
extern void tk_remove_region_data(int rid);

/* the pixels of one scale in current zoom level */
extern int tk_scale_in_pixels(double lat, short z);
/* the meters of one scale in current zoom level */
extern int tk_scale_in_meters(short z);
extern double tk_meters_per_pixel(double lat, short z);
extern char *tk_get_data_root();
/*
* purpose: 根据经纬度返回city id
* [in]: pos 经纬度
* return value: 如果为NULL，则表示无法获得city id
* 调用者无需释放返回指针内存
*/
extern int tk_get_city_id(struct tk_latlon pos);

/*
 * purpose: Get current city id
 * par: 
 * return value: 相应值
 */
int tk_get_current_city_id();

/*============================
 * Cordinate conversion
 *========================== */
extern void tk_latlon2scr(struct tk_latlon *latlon, struct tk_point *point);
extern void tk_scr2latlon(struct tk_latlon *latlon, struct tk_point *point);
extern void tk_latlon_transform(double lon, double lat, int wg_heit, int wg_week, 
        unsigned int wg_time, double* out_lon, double* out_lat);

/**
 * purpose: get the nearest poi's name of point(x, y)
 * return value: the nearest feature's name
 */
char *tk_get_poi_name(int x, int y, int flag);
int tk_init_icon_num( int icon_num);
void tk_set_icon(int icon_id, int w, int h);
extern void tk_bmp2png();
#endif

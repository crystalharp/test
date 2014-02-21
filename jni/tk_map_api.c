//
//  tk_map_api.c
//  tigermap
//
//  Created by Chen Ming on 13-6-7.
//  Copyright (c) 2013年 TigerKnows. All rights reserved.
//

#include <stdio.h>
#include <string.h>
#include <assert.h>
#include <math.h>
#include "tk_map_api.h"
#include "tk_global_info.h"
#include "tk_file_util.h"
#include "tk_style_config.h"
#include "tk_feature_data.h"
#include "tk_util.h"
#include "tk_static_data_loader.h"
#include "tk_context.h"
#include "tk_data_loader.h"
#include "tk_map_render.h"
#include "tk_region.h"
#include "tk_log.h"
#include "tk_geo_util.h"

#define MAX_INFO_LEN 1024

//渲染线程为reader，下载线程为writer
static pthread_mutex_t sem_rc = PTHREAD_MUTEX_INITIALIZER;     //保证readcount被正确更新
static pthread_mutex_t sem_wc = PTHREAD_MUTEX_INITIALIZER;     //保证writercount被正确更新
static pthread_mutex_t sem_rq = PTHREAD_MUTEX_INITIALIZER;     //竞争rsem失败后,后续读者在此排队
static pthread_mutex_t sem_wsem = PTHREAD_MUTEX_INITIALIZER;  //保证读写，写写互斥
static pthread_mutex_t sem_rsem = PTHREAD_MUTEX_INITIALIZER;  //当一个写者出现，用于禁止所有的读进程
static int readcount = 0;
static int writecount = 0;

enum {
    TK_TILE_PIXEL_FORMAT_ARGB32,
    TK_TILE_PIXEL_FORMAT_RGB565,
};

tk_engine_global_info_t tk_global_info;

int tk_init_engine_config(const char *resdir, const char *mapdir, int tile_size) {
    tk_status_t result = TK_STATUS_SUCCESS;
    if (resdir == NULL || mapdir == NULL) {
		return TK_STATUS_INVALID_ARGS;
	}
    
	if (strlen(resdir) >= TK_MAX_PATH_LENGTH || strlen(mapdir) >= TK_MAX_PATH_LENGTH) {
		return TK_STATUS_INVALID_ARGS;
    }
    memset(&tk_global_info, 0, sizeof(tk_engine_global_info_t));
    LOG_DBG("init: %s, %s", resdir, mapdir);
    size_t res_str_len = strlen(resdir);
    size_t map_str_len = strlen(mapdir);
	strcpy(tk_global_info.respath, resdir);
    if (resdir[res_str_len - 1] == '/' || resdir[res_str_len - 1] == '\\' ) {
        tk_global_info.respath[res_str_len - 1] = 0;
        LOG_DBG("init: %s", tk_global_info.respath);
    }
	strcpy(tk_global_info.datapath, mapdir);
    if (mapdir[map_str_len - 1] == '/' || mapdir[map_str_len - 1] == '\\' ) {
        tk_global_info.datapath[map_str_len - 1] = 0;
    }
    
    tk_init_tile_data_pool();
    tk_init_region_cache();
    tk_global_info.tile_size_pix = tile_size;
    tk_context_t *context = tk_get_context();
    memset(context, 0, sizeof(tk_context_t));
    result = _tk_load_static_data();
    if(result == TK_STATUS_SUCCESS) {
    	tk_global_info.is_initialized = 1;
    }
    else {
    	tk_global_info.is_initialized = 0;
    }
    return result;
}

void tk_engine_reset_map_dir(const char *mapdir) {
    size_t map_str_len = strlen(mapdir);
	strcpy(tk_global_info.datapath, mapdir);
    if (mapdir[map_str_len - 1] == '/' || mapdir[map_str_len - 1] == '\\' ) {
        tk_global_info.datapath[map_str_len - 1] = 0;
    }
}

unsigned char tk_is_engine_initialized() {
	return tk_global_info.is_initialized;
}

void tk_set_global_tile_size(int tile_size) {
	tk_global_info.tile_size_pix = tile_size;
}

void tk_destroy_engine_config() {
    tk_destroy_static_data();
    tk_global_info.is_initialized = 0;
}

int tk_init_context(unsigned char *img_buf, int tile_size_bit, int mod) {
    return tk_init_default_context(img_buf, tile_size_bit, mod); //这里以后可以考虑桥接或适配
}

void tk_fini_context(void) {
    tk_fini_default_context();//这里以后可以考虑桥接或适配
}

int tk_get_matrix_size(tile_size) {
	return tile_size * tile_size;
}

int tk_get_tile_buffer_size(int tile_size_bit, int format) {
    int size = (1 << (tile_size_bit * 2)) * (format == TK_TILE_PIXEL_FORMAT_ARGB32 ? 4 : 2);

#ifdef NEED_BMP_HEADER
    return size + BMP_HEADER_SIZE;
#else
    return size;
#endif
}

int tk_get_cid_by_rid(int rid) {
    int i;
    for (i = 0; i < tk_global_info.reg_num; ++ i) {
        if (tk_global_info.reg_list[i].rid == rid)
            return tk_global_info.city_list[tk_global_info.reg_list[i].city_idx].cid;
    }
    return -1;
}

static void _tk_set_label_points(tk_context_t *context) {
    tk_label_t *labels = context->label_buf.labels;
    int label_num = context->label_buf.label_num;
    tk_label_t *label = NULL;
    tk_point_t *points_pool = context->label_point_pool.points;
    for (int i = 0; i < label_num; ++ i) {
        label = labels + i;
        label->points = points_pool + label->point_start_idx;
    }
}

static void _tk_set_building_points(tk_context_t *context) {
    tk_building_t *buildings = context->building_buf.buildings;
    int building_num = context->building_buf.building_num;
    tk_building_t *building = NULL;
    tk_point_t *points_pool = context->label_point_pool.points;
    for (int i = 0; i < building_num; ++i) {
        building = buildings + i;
        building->points = points_pool + building->point_start_idx;
    }
}

static void _tk_reader_prepare()
{
    pthread_mutex_lock(&sem_rq);
    pthread_mutex_lock(&sem_rsem);
    pthread_mutex_lock(&sem_rc);
    ++readcount;
    if (readcount == 1) {
        pthread_mutex_lock(&sem_wsem);
    }
    pthread_mutex_unlock(&sem_rc);
    pthread_mutex_unlock(&sem_rsem);
    pthread_mutex_unlock(&sem_rq);
}

static void _tk_reader_end()
{
    pthread_mutex_lock(&sem_rc);
    --readcount;
    if (readcount == 0) {
        pthread_mutex_unlock(&sem_wsem);
    }
    pthread_mutex_unlock(&sem_rc);
}

static void _tk_writer_prepare()
{
    pthread_mutex_lock(&sem_wc);
    ++writecount;
    if (writecount == 1) {
        pthread_mutex_lock(&sem_rsem);
    }
    pthread_mutex_unlock(&sem_wc);
    pthread_mutex_lock(&sem_wsem);
}

static void _tk_writer_end()
{
    pthread_mutex_unlock(&sem_wsem);
    pthread_mutex_lock(&sem_wc);
    --writecount;
    if (writecount == 0) {
        pthread_mutex_unlock(&sem_rsem);
    }
    pthread_mutex_unlock(&sem_wc);
}

tk_label_t *tk_render_tile(int tile_x, int tile_y, int zoom, int *label_num) {
	LOG_INFO("to render :%i, %i, %i", tile_x, tile_y, zoom);
    tk_status_t result = TK_STATUS_SUCCESS;
    tk_context_t *context = tk_get_context();
    assert(label_num != NULL);
    tk_clean_lost_data(context);
    tk_clean_labels(context);
    tk_context_clear_building_buf(context);
    _tk_reader_prepare();
    zoom += (context->tile_size_bit - 8);
    result = tk_load_tile_data_default(tile_x, tile_y, zoom, TK_NO);
    if (result != TK_STATUS_SUCCESS) {
        goto CATCH;
    }
    result = tk_render_tile_default(tile_x, tile_y, zoom);
    if (result != TK_STATUS_SUCCESS) {
        goto CATCH;
    }
    _tk_set_label_points(context);
    tk_clean_tiles(context);
    _tk_reader_end();
    *label_num = context->label_buf.label_num;
    return context->label_buf.labels;
CATCH:
    LOG_INFO("render :%i, %i, %i, failed", tile_x, tile_y, zoom);
    tk_clean_tiles(context);
    _tk_reader_end();
    *label_num = 0;
    tk_set_result(result);
    return NULL;
}

tk_building_t *tk_get_buildings(int *building_num) {
    tk_context_t *context = tk_get_context();
    _tk_set_building_points(context);
    *building_num = context->building_buf.building_num;
    return context->building_buf.buildings;
}

tk_label_t *tk_get_labels(int tile_x, int tile_y, int zoom, int *label_num) {
    tk_status_t result = TK_STATUS_SUCCESS;
    tk_context_t *context = tk_get_context();
    assert(label_num != NULL);
    tk_clean_lost_data(context);
    tk_clean_labels(context);
    tk_context_clear_building_buf(context);
    _tk_reader_prepare();
    result = tk_load_tile_data_default(tile_x, tile_y, zoom, TK_YES);
    if (result != TK_STATUS_SUCCESS) {
        goto CATCH;
    }
    result = tk_get_tile_labels(tile_x, tile_y, zoom);
    if (result != TK_STATUS_SUCCESS) {
        goto CATCH;
    }
    _tk_set_label_points(context);
    tk_clean_tiles(context);
    _tk_reader_end();
    *label_num = context->label_buf.label_num;

    return context->label_buf.labels;
CATCH:
    tk_clean_tiles(context);
    _tk_reader_end();
    *label_num = 0;
    tk_set_result(result);
    return NULL;
}

void tk_clean_cache(void) {
    _tk_writer_prepare();
    tk_region_clean_unuse_cache();
    tk_tile_data_clean();
    _tk_writer_end();
}

void tk_clean_label_mem(void) {
    tk_context_t *context = tk_get_context();
    tk_clean_labels(context);
}

int tk_get_max_label_priority() {
    return tk_global_info.max_label_priority;
}

void tk_get_max_and_min_font_size(int *max_font_size, int *min_font_size) {
    *max_font_size = tk_global_info.max_font_size;
    *min_font_size = tk_global_info.min_font_size;
}

int tk_init_region_file(const char *metafile, int rid)
{
    _tk_writer_prepare();
    tk_status_t result = tk_region_init_file(metafile, rid);
    _tk_writer_end();
    return result;
}

int tk_write_to_region(int rid, int off, int len, const char* buf)
{
    LOG_DBG("to write %i, %i, %i", rid, off, len);
    _tk_writer_prepare();
    tk_status_t result = tk_region_write_file(rid, off, len, buf);
    _tk_writer_end();
    LOG_DBG("write %i, %i, %i success", rid, off, len);
    return result;
}

int tk_get_region_state(int rid, int *ptotal_size, int *pdownloaded_size)
{
    _tk_reader_prepare();
    tk_clean_lost_data(tk_get_context());
    tk_status_t result = tk_region_get_state(rid, ptotal_size, pdownloaded_size);
    _tk_reader_end();
    return result;
}

int tk_get_region_version_by_id(int rid, unsigned char *rversion)
{
    _tk_reader_prepare();
    tk_status_t result = tk_region_get_version(rid, rversion);
    _tk_reader_end();
    return result;
}

int tk_get_region_id_by_name(const char *rname)
{
    int i;
    if (rname != NULL ) {
        if (strcmp(rname, "quanguo") == 0) {
            return tk_global_info.nat.rid;
        }
        for (i = 0; i < tk_global_info.reg_num; i++) {
            if (strcmp(tk_global_info.reg_list[i].ename, rname) == 0) {
                return tk_global_info.reg_list[i].rid;
            }
        }
    }
    return -1;
}

const char *tk_get_region_name_by_id(int rid)
{
    if ((rid < 0 && rid != -3) || rid >= tk_global_info.reg_num) {
        return NULL;
    }
    
    if (rid == -3) {
        return tk_global_info.nat.rename;
    }
    return tk_global_info.reg_list[rid].ename;
}

const char *tk_get_provname_by_rid(int rid)
{
    char *prov_name = NULL;
    tk_context_t *context = tk_get_context();
    if (!context->string_buf) {
        context->string_buf = malloc(MAX_INFO_LEN);
    }
    prov_name = context->string_buf;
    int prov_idx;
    
    if ((rid < 0 && rid != -3) || rid >= tk_global_info.reg_num) {
        return NULL;
    }
    
    if (rid == -3) {
        sprintf(prov_name, "%s", tk_global_info.nat.pename);
    } else {
        prov_idx = tk_global_info.city_list[tk_global_info.reg_list[rid].city_idx].prov_idx;
        sprintf(prov_name, "%s", tk_global_info.prov_list[prov_idx].ename);
    }
    return prov_name;
}

int tk_get_city_num(char *pname) {
    int cnum = 0, i = 0, fcity = 0, c_idx = 0;
    for (i = 1; i <= tk_global_info.prov_num ; i++) {
        if (strcmp(tk_global_info.prov_list[i].name, pname) == 0) {
            fcity = tk_global_info.prov_list[i].city_idx;
            break;
        }
    }
    /* No provice is found */
    if (i > tk_global_info.prov_num) {
        return 0;
    }
    /* find num of city */
    cnum = 1;
    c_idx = fcity;
    while ((c_idx = tk_global_info.city_list[c_idx].next) != -1) {
        cnum++;
    }

    return cnum;
}

char **tk_get_city_list(const char *pname, int *num_of_cities)
{
    int i;
    int fcity;
    int cnum;
    int c_idx;
    char **cities;
    
    for (i = 1; i <= tk_global_info.prov_num ; i++) {
        if (strcmp(tk_global_info.prov_list[i].name, pname) == 0) {
            fcity = tk_global_info.prov_list[i].city_idx;
            break;
        }
    }
    
    /* No provice is found */
    if (i > tk_global_info.prov_num) {
        return NULL;
    }
    
    /* find num of city */
    cnum = 1;
    c_idx = fcity;
    while ((c_idx = tk_global_info.city_list[c_idx].next) != -1) {
        cnum++;
    }
    
    *num_of_cities = cnum;
    cities = (char **)xmalloc(sizeof(char *) * cnum);
    
    cnum = 0;
    c_idx = fcity;
    do {
        cities[cnum] = (char *)xmalloc(sizeof(char *) * (strlen(tk_global_info.city_list[c_idx].name) + 1));
        sprintf(cities[cnum], "%s", tk_global_info.city_list[c_idx].name);
        cnum++;
    } while ((c_idx = tk_global_info.city_list[c_idx].next) != -1);
    
    return cities;
}


int tk_get_cityid_by_name(const char *cname)
{
    int i;
    
    if (strcmp(cname, "全国") == 0)
        return -3;
    
    for (i = 1; i <= tk_global_info.city_num; i++) {
        if (tk_global_info.city_list[i].name != NULL) {
            if (strcmp(cname, tk_global_info.city_list[i].name) == 0)
                break;
        }
    }
    if (i > tk_global_info.city_num)
        return -1;
    return tk_global_info.city_list[i].cid;
}

/**
 * purpose: get the region info from the region id
 * @rid: region id
 * return value: the pointer to the string of the region info
 * in the format "中文名字, English name, file size, city ename".
 */
char *tk_get_region_info_by_id(int rid)
{
    char *region_info = NULL;
    tk_context_t *context = tk_get_context();
    if (!context->string_buf) {
        context->string_buf = malloc(MAX_INFO_LEN);
    }
    region_info = context->string_buf;
    if ((rid < 0 && rid != -3) || rid >= tk_global_info.reg_num)
        return NULL;
    
    if (rid == -3) {
        sprintf(region_info, "%s,%s,%d,%s",
                tk_global_info.nat.rname,
                tk_global_info.nat.rename,
                tk_global_info.nat.file_size,
                tk_global_info.nat.cname);
    } else {
        sprintf(region_info, "%s,%s,%d,%s",
                tk_global_info.reg_list[rid].name,
                tk_global_info.reg_list[rid].ename,
                tk_global_info.reg_list[rid].file_size,
                tk_global_info.city_list[tk_global_info.reg_list[rid].city_idx].name);
    }
    return region_info;
}

char *tk_get_city_info_by_id(int city_id)
{
    char *city_info = NULL;
    tk_context_t *context = tk_get_context();
    if (!context->string_buf) {
        context->string_buf = malloc(MAX_INFO_LEN);
    }
    city_info = context->string_buf;
    if ((city_id < 0 && city_id != -3) || city_id > tk_global_info.city_num)
        return NULL;
    if (city_id == -3) {
        sprintf(city_info, "%s,%s,%f,%f,%i,%s,%s",
                tk_global_info.nat.cname,
                tk_global_info.nat.cename,
                tk_global_info.nat.center.lat,
                tk_global_info.nat.center.lon,
                tk_global_info.nat.default_zoom_level,
                tk_global_info.nat.cname,
                tk_global_info.nat.cename);
    } else {
        if (tk_global_info.city_list[city_id].cid != -1)
            sprintf(city_info, "%s,%s,%f,%f,%i,%s,%s",
                    tk_global_info.city_list[city_id].name,
                    tk_global_info.city_list[city_id].ename,
                    tk_global_info.city_list[city_id].center.lat,
                    tk_global_info.city_list[city_id].center.lon,
                    tk_global_info.city_list[city_id].default_zoom_level,
                    tk_global_info.prov_list[tk_global_info.city_list[city_id].prov_idx].name,
                    tk_global_info.prov_list[tk_global_info.city_list[city_id].prov_idx].ename);
        else return NULL;
    }
    return city_info;
}

char *tk_get_regionname_list_by_cityid(int cityid)
{
    int i, rid;
    char *regionlist;
    
    if (cityid != -3 && (cityid <= 0 || cityid > tk_global_info.city_num || cityid == 78 || cityid == 139)) {
        LOG_ERR("Invalid city id %d!\n", cityid);
        return NULL;
    }
    
    regionlist = (char *)malloc(sizeof(char) * MAX_INFO_LEN);
    regionlist[0] = '\0';
    
    if (cityid == -3) {
        strcat(regionlist, tk_global_info.nat.rename);
        return regionlist;
    }
    
    for (i = 0; i < tk_global_info.city_num; i++)
        if (tk_global_info.city_list[i].cid == cityid)
            break;
    
    rid = tk_global_info.city_list[i].reg_idx;
    while (rid != -1) {
        strcat(regionlist, tk_global_info.reg_list[rid].ename);
        strcat(regionlist, ",");
        rid = tk_global_info.reg_list[rid].next;
    }
    
    return regionlist;
}

int *tk_get_regionid_list_by_cityid(int cityid, int *region_num)
{
    int i, j, rid;
    int *regionlist = NULL;
    
    if (cityid != -3 && (cityid <= 0 || cityid > tk_global_info.city_num || cityid == 78 || cityid == 139)) {
        LOG_ERR("Invalid city id %d!\n", cityid);
        return NULL;
    }
    
    if (cityid == -3) {
        regionlist = malloc(sizeof(int));
        *regionlist = tk_global_info.nat.rid;
        *region_num = 1;
        return regionlist;
    }
    
    for (i = 0; i < tk_global_info.city_num; i++)
        if (tk_global_info.city_list[i].cid == cityid)
            break;
    *region_num = tk_global_info.city_list[i].reg_count;
    regionlist = malloc(sizeof(int) * (*region_num));
    memset(regionlist, 0, sizeof(int) * (*region_num));
    rid = tk_global_info.city_list[i].reg_idx;
    for (j = 0; j < *region_num && rid != -1; ++j) {
        regionlist[j] = rid;
        rid = tk_global_info.reg_list[rid].next;
    }
    return regionlist;
}

char *tk_get_province_list_ename()
{
	int i;
    char *provinceinfo = (char *)malloc(sizeof(char) * MAX_INFO_LEN);
    *provinceinfo = '\0';
    
    for (i = 1; i <= tk_global_info.prov_num; i++) {
        if (i == 1)
            sprintf(provinceinfo, "%s,%s",
                    tk_global_info.prov_list[i].name,
                    tk_global_info.prov_list[i].ename);
        else
            sprintf(provinceinfo, "%s,%s,%s",
                    provinceinfo,
                    tk_global_info.prov_list[i].name,
                    tk_global_info.prov_list[i].ename);
    }
    return provinceinfo;
}

char *tk_get_province_list()
{
	int i;
    char *provinceinfo = (char *)malloc(sizeof(char) * MAX_INFO_LEN);
    
    *provinceinfo = '\0';
    for (i = 1; i <= tk_global_info.prov_num - 2; i++) {
        if (i == 1)
            sprintf(provinceinfo, "%s", tk_global_info.prov_list[i].name);
        else
            sprintf(provinceinfo, "%s,%s", provinceinfo, tk_global_info.prov_list[i].name);
    }
    return provinceinfo;
}

void tk_remove_city_data_by_name(const char *cname)
{
    int cid;
    int *rlist = NULL, rnum = 0;
    cid = tk_get_cityid_by_name(cname);
    rlist = tk_get_regionid_list_by_cityid(cid, &rnum);
    _tk_writer_prepare();
    for (int i = 0; i < rnum; ++ i) {
        tk_region_remove(rlist[i]);
    }
    _tk_writer_end();
    free(rlist);
}

void tk_remove_region_data_by_id(int rid)
{
    _tk_writer_prepare();
    tk_region_remove(rid);
    _tk_writer_end();
}

tk_lost_data_t *tk_get_lost_data(int *count) {
    tk_context_t *context = tk_get_context();
    *count = context->lost_data_count;
    return context->lost_data;
}

unsigned int tk_lon_to_mercx(double lon)
{
    return (unsigned int)floor(TK_ABS(-(lon + 180) / 360 * (1 << 24)));
}

unsigned int tk_lat_to_mercy(double lat)
{
    double lat_in_rad, y_temp, y, sin_rad;
    lat_in_rad = lat * (MATH_PI) / 180;
    sin_rad = sin(lat_in_rad);
    y_temp = log((sin_rad + 1) / (1 - sin_rad)) / 2;
    y = (1 - y_temp / MATH_PI) * (1 << (24 - 1));
    return (int)floor(y);
}

const char *tk_find_nearest_poi_by_latlon(double lon, double lat, int zoom) {
    tk_context_t *context = tk_get_context();
    int x = (tk_lon_to_mercx(lon) << 2) >> (18 - zoom);
    int y = (tk_lat_to_mercy(lat) << 2) >> (18 - zoom);
    int tile_x = x >> context->tile_size_bit;
    int tile_y = y >> context->tile_size_bit;
    return tk_find_nearest_poi(tile_x, tile_y, zoom, x, y);
}

const char *tk_find_nearest_poi (int tile_x, int tile_y, int zoom, int x, int y) {
    tk_status_t result = TK_STATUS_SUCCESS;
    tk_context_t *context = tk_get_context();
    tk_clean_labels(context);
    tk_clean_lost_data(context);
    tk_context_set_xyz(context, tile_x, tile_y, zoom);
    int max_distance_sqr = INFINITY, dx = 0, dy = 0, dist_sqr = 0;
    _tk_reader_prepare();
    result = tk_load_tile(context, tile_x, tile_y, zoom);
    if (result != TK_STATUS_SUCCESS) {
        LOG_INFO("_tk_get_tiles failed");
        goto CATCH;
    }
    tk_feature_data_t *nearest_feature = NULL;
    tk_tile_data_t *tile_data = context->tile_data[0];
    assert(tile_data);
    tk_base_tile_data_node_t *base_tile_node = tile_data->base_tile_head;
    while (base_tile_node) {
        tk_base_tile_data_t *base_tile = base_tile_node->data;
        for (int i = 0; i < base_tile->feature_num; ++ i) {
            tk_feature_data_t *feature = base_tile->features + i;
            int obj_type = context->cur_style_buf->obj_type[feature->type];
            if (!feature->has_name || (obj_type != TKGEO_ENMFTYPE_POINT && obj_type != TKGEO_ENMFTYPE_POLY)) {
                continue;
            }
            if (feature->points_num == 1) {
                dx = x - feature->points[0].x;
                dy = y - feature->points[0].y;
            }
            else {
                dx = x - ((feature->left_top.x + feature->right_bottom.x)>>1);
                dy = y - ((feature->left_top.y + feature->right_bottom.y)>>1);
            }
            dist_sqr = dx*dx+dy*dy;
            if (max_distance_sqr > dist_sqr) {
                max_distance_sqr = dist_sqr;
                nearest_feature = feature;
            }
        }
        base_tile_node = base_tile_node->next;
    }
    if (nearest_feature) {
        tk_clean_tiles(context);
        _tk_reader_end();
        return tk_str_pool_add_string(&context->label_name_pool, nearest_feature->name, nearest_feature->name_length);
    }
    else {
        tk_clean_tiles(context);
        result = TK_STATUS_CANNOT_FIND_NEAREST_POI;
    }
CATCH:
    _tk_reader_end();
    tk_set_result(result);
    return NULL;
}

int tk_get_rid_by_point(tk_point_t *pt) {
    int rid = -1;
    for (int i = 0; i < tk_global_info.reg_num; i++) {
        if (TK_POINT_IN_ENV(pt, tk_global_info.reg_bounds[i].env) == 1) {
            if (tk_is_point_in_region(i, pt)) {
                rid = i;
                break;
            }
        }
    }
    return rid;
}

int tk_get_cid_by_pos(double lon, double lat) {
    tk_point_t pos_point;
    int cur_pos_region_id;
    
    pos_point.x = tk_lon_to_mercx(lon);
    pos_point.y = tk_lat_to_mercy(lat);
    cur_pos_region_id = tk_get_rid_by_point(&pos_point);
    if (cur_pos_region_id == -1) {
        return -1;
    }
    else {
        return tk_global_info.city_list[tk_global_info.reg_list[cur_pos_region_id].city_idx].cid;
    }
}


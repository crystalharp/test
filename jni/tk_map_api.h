//
//  tk_map_api.h
//  tigermap
//
//  Created by Chen Ming on 13-6-7.
//  Copyright (c) 2013年 TigerKnows. All rights reserved.
//

#ifndef tigermap_tk_map_api_h
#define tigermap_tk_map_api_h

typedef unsigned int tk_color_t;

struct _tk_point {
    int x;
    int y;
	int level_code;
};

typedef struct _tk_point tk_point_t;

struct _tk_label {
    char *name;
    tk_color_t text_color;
    tk_color_t bg_color;
    int icon_id;
    tk_point_t *points;
    int point_num;
    int priority;
    int type;
    int font_size;
    int point_start_idx;
};

typedef struct _tk_label tk_label_t;

struct _tk_building {
    int point_start_idx;
    tk_point_t *points;
    int point_num;
    int height;
    int has_point_out_of_tile;
    tk_color_t side_color1;
    tk_color_t side_color2;
    tk_color_t top_color;
};

typedef struct _tk_building tk_building_t;

typedef enum _tk_lost_type {
    TK_LOST_TYPE_DATA_LOST,
    TK_LOST_TYPE_DATA_ERROR,
} tk_lost_type_t;

struct _tk_lostdata {
    int rid;
    int offset;
    int length;
    tk_lost_type_t type;
};

typedef struct _tk_lostdata tk_lost_data_t;

enum {
    POINT_TYPE_NORMAL,
    POINT_TYPE_SRC_DIVIDE,
    
};

/* ============================================
 *  Initialize and destroy the engine
 *  =========================================== */

/*
 * Initialize the engine. Should only be called once at the very start
 * @resdir: resource dir
 * @mapdir: map data dir
 */
int tk_init_engine_config(const char *resdir, const char *mapdir, int tile_size_bit);

void tk_engine_reset_map_dir(const char *mapdir);

unsigned char tk_is_engine_initialized(void);

void tk_set_global_tile_size(int tile_size);

/*
 * Destroy the enine. Should be called for each tk_init_engine()
 */
void tk_destroy_engine_config(void);

int tk_get_tile_buffer_size(int tile_size_bit, int format);

int tk_init_context(unsigned char *img_buf, int tile_size_bit, int mod);

void tk_fini_context(void);

int tk_get_matrix_size(int tile_size);

tk_label_t *tk_render_tile(int tile_x, int tile_y, int zoom, int *label_num);

tk_building_t *tk_get_buildings(int *building_num);

tk_label_t *tk_get_labels(int tile_x, int tile_y, int zoom, int *label_num);

void tk_clean_cache(void);

void tk_clean_label_mem(void);

/**
 * purpose: fill the buffer in the map data file
 * @rid: the region id corresponding to the map data file
 * @off: the offset in the map data file
 * @len: the length of the buffer
 * @buf: the buffer
 * return value: -1 for false, 0 for success
 */
int tk_write_to_region(int rid, int off, int len, const char* buf);

/**
 * purpose: recover map data file from the temp meta file
 * metafile: the name of the temp meta file
 * rid: the region id corresponding to the temp meta file
 * return value: -1 for fail, 0 for success
 */
int tk_init_region_file(const char *metafile, int rid);

int tk_get_region_state(int rid, int *ptotal_size, int *pdownloaded_size);

int tk_get_region_version_by_id(int rid, unsigned char *rversion);

int tk_get_cid_by_rid(int rid);

int tk_get_region_id_by_name(const char *rname);

const char *tk_get_region_name_by_id(int rid);

const char *tk_get_provname_by_rid(int rid);

int tk_get_city_num(char *pname);

char **tk_get_city_list(const char *pname, int *num_of_cities);

int tk_get_cityid_by_name(const char *cname);

char *tk_get_region_info_by_id(int rid);

char *tk_get_city_info_by_id(int city_id);

char *tk_get_regionname_list_by_cityid(int cityid);

int *tk_get_regionid_list_by_cityid(int cityid, int *region_num);

char *tk_get_province_list_ename(void);

char *tk_get_province_list(void);

void tk_remove_city_data_by_name(const char *cname);

void tk_remove_region_data_by_id(int rid);

int tk_get_max_label_priority(void);

void tk_get_max_and_min_font_size(int *max_font_size, int *min_font_size);

tk_lost_data_t *tk_get_lost_data(int *count);

unsigned int tk_lon_to_mercx(double lon);

unsigned int tk_lat_to_mercy(double lat);

const char *tk_find_nearest_poi_by_latlon(double lon, double lat, int zoom);

const char *tk_find_nearest_poi (int tile_x, int tile_y, int zoom, int x, int y);

void tk_gps_latlon_transform(double lon, double lat, int wg_heit, int wg_week, unsigned int wg_time, double* out_lon, double* out_lat);

double tk_meters_per_pixel(double lat, short int z);

int tk_get_pix_count_of_scale(double lat, short int z);

int tk_get_meters_of_scale(short int z);

int tk_get_rid_by_point(tk_point_t *pt);

int tk_get_cid_by_pos(double lon, double lat);

#endif



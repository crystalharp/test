/*
 * =====================================================================================
 *
 *       Filename:  tkm_mapint.h
 *
 *    Description:
 *
 *        Version:  1.0
 *        Created:  2010年12月28日 17时47分23秒
 *       Revision:  none
 *       Compiler:  gcc
 *
 *        Company:
 *
 * =====================================================================================
 */
#ifndef __TKM_MAPINT_H
#define __TKM_MAPINT_H
#include <stdio.h>
#include "tk_util.h"
#include "tkm_comm.h"

/* =======================================================
 * the global variables declaration
 *=========================================================*/
#define LEVEL_MAX 18
#define LEVEL_MIN 4
#define LEVEL_SKIP 9

#define TK_CLIP_REDUNDENT  40
#define TK_CLIP_CAIRO      10
#define TK_MAX_MARGIN_TILE 20

#define TK_NATIONAL_LEVEL_A 8
#define TK_NATIONAL_LEVEL_B 6
#define TK_NATIONAL_LEVEL_C 3

#define TK_BASE_LEVEL_A 16
#define TK_BASE_LEVEL_B 13
#define TK_BASE_LEVEL_C 11

#define GSCALE_BASE 24
#define TILE_SIZE_EXP 8
#define GSCALE_FACTOR (GSCALE_BASE - TILE_SIZE_EXP)
//China's latitude's scope:(4, 55.1)
//China's longitude's scope: (72.14, 140)

#define MIN_CENTER_X  11750844
#define MAX_CENTER_X  14913080
#define MIN_CENTER_Y  5306597
#define MAX_CENTER_Y  8202042
#define TK_X_IN_CHINA(x)  (x >= MIN_CENTER_X && x < MAX_CENTER_X)
#define TK_Y_IN_CHINA(y)  (y >= MIN_CENTER_Y && y < MAX_CENTER_Y)

#define TK_X_IN_CHINA_ZOOM(x,z) (x >= (MIN_CENTER_X >> (z)) && x < (MAX_CENTER_X >> (z)))
#define TK_Y_IN_CHINA_ZOOM(y,z) (y >= (MIN_CENTER_Y >> (z)) && y < (MAX_CENTER_Y >> (z)))

#ifdef TK_BPP_16
typedef unsigned short tk_pixel;
#else
typedef unsigned int tk_pixel;
#endif

typedef unsigned int gdi_color;

#define SUBWAYCITY  8
#define MAXSWCITY   50
#define MAXSWNUM    30
#define MAXSWNAMELEN 32

struct subway_color_info {
    char sw_name[MAXSWNAMELEN];
    gdi_color color;
};

struct subway_city_info {
    struct subway_color_info color_info[MAXSWNUM];
    int    rid;
    int color_num;
};

struct subway_city {
    struct subway_city_info city_info[MAXSWCITY];
    int    city_num;
};

struct subway_city  sw_city;

extern char respath[TK_MAX_PATH];
extern char datapath[TK_MAX_PATH];

#define DATA_BUFF_LEN  4096//4k
extern int data_buffer_offset;
extern unsigned char data_buffer[DATA_BUFF_LEN];

extern int need_read_whole_map;
extern struct style *pstyle;
extern cairo_surface_t *surface;
extern cairo_t *cr;

struct envelope
{
	int left;
	int top;
	int right;
	int bottom;
    //float slope;
};

struct envelope_slope {
    struct envelope elp;
    float  slope;
};

struct style
{
	gdi_color	    border_color;
	gdi_color	    fill_color;
	char			*pen_width;
	char			line_type; //line style's type: 0 for polygons and points, 1 for the lines, and 2 for Zha Dao, Railway, Subway
	char			dash_ratio;
	char			zoom_min;
	char			zoom_max;
	char			icon_id;
    unsigned char   label_style;
    gdi_color       fontcolor;//feature's label's color
    char            label_min;
    char            label_max;
    int             font_size;
    int             font_alter;
};

struct styles_buffer
{
    int layer_num;
    short *feature_type;
    short *priority;
    struct style *styles;
};

struct tk_pool {
    char *tk_map_names;
    struct tk_point *tk_points;
    int cur_name_index;
    int cur_point;
};

struct tile {
    unsigned int coder_lat;//the left bottom value
    unsigned int coder_lon;//基本坐标
    //转化为屏幕坐标需要的量
    int level;// the num of tiles merged in this tile in one direction
    int bias_x;
    int bias_y;
    int level_dif; // = tk_engine.bl - tk_engine.current_z
    struct feature *features;
    int fnum;
    struct tk_pool mem_pool;
    
    int region_id;
    int overall_name_len;
    int overall_point_num;
    int is_active;
    struct feature *header_features;
    struct envelope whole_screen;
    struct envelope clip_screen;
    struct envelope bbox;
    int flag;//the flag whether it is needed now
    struct tile *next; //the pointer to next tile in tile buff
    int length;//the tile's length in dat file
};

struct feature
{
    struct tk_point *points;
    short int name_index;
   	unsigned char type;
    short int name_length;
	short int point_nums;
    char *name;
    // the tile this feature belonged to
    struct tile *tile;
    // the pointers to the feature which belongs to another tile but is adjacent
	struct feature *next;
    struct feature *previous;
    // the next record of the tile's features' linked list which is seperated. It is the end if the value is NULL
    struct feature *header_next;
    // the next recored of the layer's features
    struct feature *layer_next;
    // the left top of its outside rectangle
    struct tk_point left_top;
    struct tk_point right_bottom;
    int label_priority;
};


struct tk_struct_tile_region_id {
    int x;
    int y;
    int region_id;
};

enum ENGINE_FLAG {
    F_ZOOMED,
    F_MOVED
};

//define the feature type
enum TKGeoENMFType
{
	TKGEO_ENMFTYPE_POINT = 0,			//
	TKGEO_ENMFTYPE_LINE,			//
	TKGEO_ENMFTYPE_RAIL,			//
	TKGEO_ENMFTYPE_ROAD,			//
	TKGEO_ENMFTYPE_POLY,			//
	TKGEO_ENMFTYPE_MAX
};

struct tk_engine {
    unsigned int flags;
    int center_x; //center x for zoom level 24
    int center_y; //center y for zoom level 24
    int current_z; //current zoom level
    int old_x; //old center for calculating screen update area
    int old_y;
    int old_z;
    int width;  // the width of map screen
    int height; // the height of map screen
    
    // base level
    int bl;
    // base level index : A ==> 0, B ==> 1, C ==> 2
    int bl_idx;
    int bl_dif;
    
    int tile_flag;
    int tilebuf_length;
    /* map_buffer (tile buffer)*/
    struct tile *tb;
    int tb_size; // the total size of tiles in tile buffer
    int tile_num;  //the tiles' total num for the current map screen
    
    /* an array of layer */
    struct layer *layer_list;
    
    struct envelope old_bbox;
 	struct envelope cur_bbox;//最细级别mercator坐标
    
    struct styles_buffer n_styles;
    struct styles_buffer c_styles;
    struct styles_buffer *ps;
    //add min tile box
    struct envelope min_tile_bbox;
};

extern struct tk_engine tk_engine; //meta data for tile buffer
extern int  regnum_in_bound;

/* =======================================================
 * the region structure management part
 * ====================================================== */

/* release all the loaded region */
extern void release_regs();
/* get version of region rid */
extern int get_rversion(int rid, unsigned char *rversion);
extern int get_cur_rversion(unsigned char *rversion);

/* ========================================================
 * the interfaces for map initial.
 *=========================================================*/

#define BMP_HEADER_SIZE 56
/*
 * TigerMap Image Resource ID definition
 */
enum TK_IMG_IDS
{
    TK_IMG_TIGER_MAP_VECTOR_DEFAULT,
    TK_IMG_TIGER_MAP_VECTOR_TRAIN_STATION,
    TK_IMG_TIGER_MAP_VECTOR_BUS_STATION,
    TK_IMG_TIGER_MAP_VECTOR_SUBWAY,
    TK_IMG_TIGER_MAP_VECTOR_AIRPORT,
    TK_IMG_TIGER_MAP_VECTOR_HOSPITAL,
    TK_IMG_TIGER_MAP_VECTOR_PORT,
    TK_IMG_TIGER_MAP_VECTOR_HOTEL,
    TK_IMG_TIGER_MAP_VECTOR_GOVERMENT1,
    TK_IMG_TIGER_MAP_VECTOR_GOVERMENT2,
    TK_IMG_TIGER_MAP_VECTOR_MARKET,
    TK_IMG_TIGER_MAP_VECTOR_BUILDING,
    TK_IMG_TIGER_MAP_VECTOR_SCHOOL,
    TK_IMG_TIGER_MAP_VECTOR_STADIUM,
    TK_IMG_TIGER_MAP_VECTOR_PARK,
    TK_IMG_TIGER_MAP_VECTOR_MUSEUM,
    TK_IMG_TIGER_MAP_VECTOR_PARK_2,
    TK_IMG_TIGER_MAP_VECTOR_THEATER,
    TK_IMG_TIGER_MAP_VECTOR_CULTURE,
    TK_IMG_TIGER_MAP_VECTOR_LIBRARY,
    TK_IMG_TIGER_MAP_VECTOR_BANK,
    TK_IMG_TIGER_MAP_VECTOR_TV_TOWER,
    TK_IMG_TIGER_MAP_VECTOR_RESIDENTIAL,
    TK_IMG_TIGER_MAP_VECTOR_RESERCH,
    TK_IMG_TIGER_MAP_VECTOR_HI_TECH_PARK,
    TK_IMG_TIGER_MAP_VECTOR_GOLF,
	TK_IMG_TIGER_MAP_VECTOR_BRIDGE,
    TK_IMG_TIGER_MAP_VECTOR_TV,
    TK_IMG_TIGER_MAP_VECTOR_CAPITAL,
    TK_IMG_TIGER_MAP_VECTOR_FOREIGN_CAPITAL,
    TK_IMG_TIGER_MAP_VECTOR_PROVINCE_CAPITAL,
    TK_IMG_TIGER_MAP_VECTOR_OTHER_CITIES,
    TK_IMG_TIGER_MAP_VECTOR_OTHER_CITIES_3,
    TK_IMG_TIGER_MAP_VECTOR_STATE_ROAD,
    TK_IMG_TIGER_MAP_VECTOR_OTHER_PROVINCIAL_ROAD,
    TK_IMG_TIGER_MAP_VECTOR_DISTRICT,
    //the num of icons can not exceed this value
    TK_IMG_TIGER_MAP_VECTOR_MAX,
};

extern unsigned char *img_buffer;
extern int img_offset[TK_IMG_TIGER_MAP_VECTOR_MAX];


struct prov_unit
{
    char *name;
    char *ename;
    int city_idx;
};

struct city_unit
{
    int cid;
    char *name;
    char *ename;
    short int zoom_level; //initial zoom level
    struct tk_latlon center;
    int reg_idx;
    int next;
    int prov_idx;
};

struct reg_unit
{
    int rid;
    char *name;
    char *ename;
    int file_size;
    int next;
    int city_idx;
};

struct nat_unit
{
    int rid;
    int cid;
    char *pname;
    char *pename;
    char *cname;
    char *cename;
    char *rname;
    char *rename;
    int zoom_level;
    int file_size;
    struct tk_latlon center;
};

extern struct prov_unit *prov_list;
extern struct city_unit *city_list;
extern struct reg_unit  *reg_list;
extern struct nat_unit nat;
extern int prov_num;
extern int city_num;
extern int reg_num;

struct local_feature {
	unsigned short int type_code;
	char name[TK_MAX_NAME];
	int distance_to_center;
    struct tk_point screen_center_coord;// in_screen_coord, the centre to find featurelist
    struct tk_point screen_center;
    int radius;//the local radius of the featurelist
};

extern struct local_feature best_feature;

/* initialize the bmp buffer */
extern void set_bmpbuf(unsigned char *bmpbuf);
/*  get the context of map engine for next action */
extern void load_context();
extern int tk_init_bestfeature();
extern void release_layers();
/* init the boundary of regions */
extern int load_envelope();
extern void release_reg_bounds();
/* load citylist */
extern int load_citylist();
/* load regionlist */
extern int load_regionlist();
/* find region by rid */
extern struct reg_unit *find_region(int rid);
/* load subway color */
int load_sw_colorcfg();

extern FILE *find_fp(int rid);
extern void release_region(int rid);
/* release reg_list, city_list, prov_list */
extern void release_list();
/* release tile buffer */
extern void release_tb();
void release_styles();

#define TK_NAME_NOT_EXIST -1
#define TK_NAME_NEW 0
#define TK_NAME_NEED_DISPLAY 1
#define TK_NAME_DO_NOT_NEED_DISPLAY 2
#define TK_NAME_TOO_SHORT_TO_DISPLAY 3

/* =======================================
 * mapdata file processing functions
 * ======================================= */
/*
 * purpose: read the checksum file into memory
 * @filename: the name of checksum file
 * @pverifycode: used to return the pointer to verifycode
 * @pbyte_num: the length of the verify code
 * */
extern int read_chk_file(char *filename, unsigned char **pverifycode, unsigned short int *pbyte_num);

/*
 * purpose: get the size of region data file
 * @filename: the name of the region data file
 * */
extern int get_datafile_size(char *filename);

/*
 * purpose: get the num of tile of level A,B,C respectively
 * @tk_buffer_tile_index: the pointer to the index
 * @pA_num: used to return the tilenum of level A
 * @pB_num: used to return the tilenum of level B
 * @pC_num: used to return the tilenum of level C
 * */
extern int get_tile_num(unsigned char *tk_buffer_tile_index, int *pA_num,
                        int *pB_num, int *pC_num, int index_length);

/*
 * purpose: get the size of the tile and the last byte of the tile
 * @cur_pointer: the pointers pointed to the index of the current tile
 * @ptail: used to return the last byte of the tail
 * @ptile_size: used to return the size of the tile
 * */
extern int get_tile_info(FILE *map_data, unsigned char *cur_pointer, int map_data_bias,
                         int pre_pos,  int *ppos, unsigned char *ptail,
                         int *ptile_size, int *ptotal_size);

/*
 * purpose: record the offset and length according to the code
 * @rid: the region id
 * @tail: the last byte of the tile
 * @code: the verify code */
extern int add_lostdata(int rid, unsigned char tail, unsigned char code,
                        unsigned char shift_num, unsigned int map_data_bias, int pre_pos,
                        int tile_size, int *pdownload_size, int *poffset,
                        int *plength, int *pis_in_block);

/* this is a common routine called by all functions that will change
 * the zoom level
 */
extern void set_zlevel(int zl);

extern  void get_tile_bbox_from_point(struct envelope *bbox);

int tk_get_rid_by_point_and_level(struct tk_point *pt, int level);
extern int tk_get_rid_by_point(struct tk_point *pt);

extern unsigned int wgtochina_lb(int wg_flag, unsigned int wg_lng, unsigned int wg_lat,
                                 int wg_heit,  int wg_week, unsigned int wg_time,
                                 unsigned int *china_lng, unsigned int *china_lat);

extern struct local_feature *get_best_feature();

extern void get_tile_base_level_bound(int x, int y, int bl_dif, struct envelope *box);

extern void clean_tile(int zl);

extern int get_base_level(int zoomlevel);
extern int load_tile(int x, int y, struct envelope *tile_box);
extern int draw_tile(int x, int y, struct envelope *el);
extern void reset_tile_screen(struct envelope *env);
#endif

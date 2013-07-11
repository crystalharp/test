/*
 * =====================================================================================
 *
 *       Filename:  tk_mapint.c
 *
 *    Description:  
 *
 *        Version:  1.0
 *        Created:  2010年12月28日 17时41分35秒
 *       Revision:  none
 *       Compiler:  gcc
 *        Company:  
 *
 * =====================================================================================
 */

#include <stdio.h>
#include <sys/stat.h>
#include <string.h>
#include <stdlib.h>
#include <unistd.h>
#include <math.h>
#include <errno.h>
#include "tk_log.h"
#include "tkm_render.h"
#include "tkm_layer.h"
#include "tkm_map.h"


/* =========================================================
 * the global variables and defines
 * ========================================================= */
#define F_CSTYLE "render.cfg"
#define F_NSTYLE "render_n.cfg"
#define F_RLIST "regionlist"
#define F_CLIST "citylist"
#define F_IMG "images"
#define F_ENV "env.dat"
#define F_SPI "SPI.dat"

/* exceptional region id */
#define TK_REGION_ID_OUT_BOUND -1
#define TK_REGION_ID_ERROR -2
#define TK_REGION_ID_NATIONAL -3

struct local_feature best_feature;
struct tk_map_lostdata lostdata[TK_LOST_DATA_PIECE];
int lostdata_idx = 0;
extern int tk_font_offset;
char respath[TK_MAX_PATH];
char datapath[TK_MAX_PATH];
cairo_t *cr;
static struct tk_point cached_polygon[TK_MAX_POINT_IN_BOUND];
int is_draw_subway;
cairo_surface_t *surface; 

struct reg_rects
{
    struct envelope env;
    int offset;
};

static FILE *pf_spi = NULL;
static struct reg_rects *reg_bounds = NULL;
int need_read_whole_map = 1;
int *layer_ctl;

FILE* polygon_reader = NULL;

/* qkhe add subway color */
//struct subway_color  sw_color[SUBWAYCITY][16]; 
//int sw_city[] = {1, 2, 3, 4, 8, 6, 11, 7};
/* =========================================================
 * the region structure management part
 * ========================================================= */

#define TK_VERNO_LENGTH 12 /* two version number length */ 
#define TK_BLEVEL_NUM 3
#define TK_CODER_UNIT_SIZE 3
#define TK_STOP_UNIT_SIZE 4 

/* the region stucture used in map engine internal */
struct region {
    int rid;
    FILE *fp;
    unsigned char *index; /* index part of region data */
    unsigned int index_size; 
    unsigned char *code_levels;
    unsigned char *code_read_con;
    int code_con_num;
    int map_data_bias;
    int level_num;
    unsigned char *verifycode;
    char version[6];
    struct tk_point *cached_polygon;
    int point_num;
    struct tile_bound {
        int up;
        int low;
    } tile_bound[TK_BLEVEL_NUM];
};

#define TK_REGION_CACHED_MAX 12 /* Max num of cached regions */
#define INC_IDX(idx) (idx = ((idx) + 1) % TK_REGION_CACHED_MAX)
#define DEC_IDX(idx) (idx = ((idx) - 1 + TK_REGION_CACHED_MAX) % TK_REGION_CACHED_MAX)
#define NEXT_IDX(idx) (((idx) + 1) % TK_REGION_CACHED_MAX)
#define PREV_IDX(idx) (((idx) - 1 + TK_REGION_CACHED_MAX) % TK_REGION_CACHED_MAX)

static struct region reg[TK_REGION_CACHED_MAX];
static int front = 0; 
static int rear = 0;
static int isfull = 0;  /* reg[] is full? */


static void release_reg(int idx);

/* Find region rid in reg[]. If found, return index in reg[].  
 * Else return -1.*/
static int find_reg(int rid)
{
    int i;
    
    if ((rear == front) && (isfull == 0)) { 
        return -1;
    }
    i = PREV_IDX(front); 
    while (1) { /* break in the loop */
        if (reg[i].rid == rid) {
            return i; /* found */
        }
        if (i == rear) {
            break; /* at the rear */
        }
        DEC_IDX(i);
    }
    /* get here, indicate not found */
    return -1;
}

/* Get an empty index in reg[]. If reg[] is full, release an region 
 * and return the just released index. */
static int get_empty_idx()
{
    if (isfull) {
        release_reg(rear);
        INC_IDX(rear);
    }
    return front; 
}

#define FILE_NO_EXIST -2

/* load the region rid into memory */
static int load_reg(int rid)
{
    FILE* fp;
    int idx;
    int i, res = 0;
    int coder_length = 0;
    int index_length = 0;
    unsigned char *coder_code;
    unsigned char *cur_pointer;
    unsigned char buff[8];
    unsigned short int byte_num;
    unsigned char buf[8];
    char map_data_path[TK_MAX_PATH];

    if ((rid < 0 && rid != -3) || rid > 507) {
        LOG_ERR("invalid region id %d.\n", rid);
        return -1;
    }

    if ((idx = find_reg(rid)) >= 0) {/* already been loaded */
        return idx; 
    }
	strcpy(map_data_path, tk_get_region_path(rid));
    if (access(map_data_path, 0) != 0) {
        LOG_INFO("Region data file %s doesn't exist.\n", map_data_path);
        return FILE_NO_EXIST;
    }
    fp = fopen(map_data_path, "r+b");
    if (fp == NULL) {
        LOG_INFO("Region data file %s open fail.\n", map_data_path);
        return -1;
    } else {
        idx = get_empty_idx();
    }
    reg[idx].fp = fp;
    reg[idx].rid = rid;
    fread(buff, 1, 6, reg[idx].fp);

    coder_length = GETNUM3B(buff);
    index_length = GETNUM3B(buff + 3);

    reg[idx].map_data_bias = coder_length + index_length + 6;
    coder_code = malloc(coder_length);
    fread(coder_code, 1, coder_length, reg[idx].fp);

    /* get the version number */
    memcpy(reg[idx].version, coder_code, 6);
    reg[idx].index = malloc(index_length);
    fread(reg[idx].index, 1, index_length, reg[idx].fp);
    reg[idx].index_size = index_length / 6;
    reg[idx].level_num = (coder_code[TK_VERNO_LENGTH+0] << 8) + coder_code[TK_VERNO_LENGTH + 1];
    reg[idx].code_levels = malloc(reg[idx].level_num * TK_BLEVEL_NUM
            * TK_CODER_UNIT_SIZE);
    reg[idx].code_con_num = coder_code[TK_VERNO_LENGTH + 2];
    reg[idx].code_read_con = malloc(TK_STOP_UNIT_SIZE * reg[idx].code_con_num);

    memcpy(reg[idx].code_read_con, coder_code + TK_VERNO_LENGTH + 3, reg[idx].code_con_num
            * TK_STOP_UNIT_SIZE);
    memcpy(reg[idx].code_levels, coder_code+TK_VERNO_LENGTH + 3 + reg[idx].code_con_num
            * TK_STOP_UNIT_SIZE, reg[idx].level_num*TK_BLEVEL_NUM * 3);

   	free(coder_code);
    
    /* read the checksum data */
    strcat(map_data_path, ".chk");
    if (access(map_data_path, 0) == 0) {
        fp = fopen(map_data_path, "rb");
        fread(buff, 1, 8, fp);
        if (memcmp(reg[idx].version, buff, 6) != 0) {
            reg[idx].verifycode = NULL;
            LOG_DBG("The chk file and data file 's version don't match\n");
        }
        else {
            LOG_DBG("The chk file is used to confirm whether mapdata is missed\n");
            byte_num = (buff[7] << 8) | (buff[6]);
            reg[idx].verifycode = malloc(byte_num);
            fread(reg[idx].verifycode, 1, byte_num, fp);
        }
        fclose(fp);
    } else {
        reg[idx].verifycode = NULL;
    }
    /* update global varible front */
    if (INC_IDX(front) == rear) {
        isfull = 1;
    }

    /* read the region polygon bound */
    fseek(pf_spi, reg_bounds[rid].offset, SEEK_SET);
    fread(&reg[idx].point_num, 1, 1, pf_spi);
    if (reg[idx].point_num >= TK_MAX_POINT_IN_BOUND) {
        LOG_INFO("Too many points of city bound order %d, point num: %d\n", rid, reg[idx].point_num);
    }

    if ((reg[idx].cached_polygon = (struct tk_point *)malloc(sizeof(struct tk_point) * reg[idx].point_num)) == NULL) {
        LOG_INFO("Memory allocated error in %s : %d\n", __FILE__, __LINE__);
    }
    for (i = 0; i < reg[idx].point_num; i++) {
        fread(buf, 1, 8, pf_spi);
        reg[idx].cached_polygon[i].levelCode = (buf[0] >> 7) & 0x1f; /* &0x1? YHQ */
        reg[idx].cached_polygon[i].x = ((buf[0] & 0x7f) << 24) + (buf[1] << 16) + (buf[2] << 8) + buf[3];
        reg[idx].cached_polygon[i].y = (buf[4] << 24) + (buf[5] << 16) + (buf[6] << 8) + buf[7];
    }

    /* get upperbound and lowerbound of each level's tile index */
    for (i = 0; i < TK_BLEVEL_NUM && res < reg[idx].index_size; i++) {
        reg[idx].tile_bound[i].low = res + 2;
        cur_pointer = reg[idx].index + res * 6;
        res += ((cur_pointer[9] << 16) | (cur_pointer[10] << 8) | (cur_pointer[11])) + 2;
        reg[idx].tile_bound[i].up = res - 1;
    }

    return idx;
}

static void release_reg(int idx)
{
    fclose(reg[idx].fp);
    free(reg[idx].index);
    free(reg[idx].code_levels);
    free(reg[idx].code_read_con);
    if (reg[idx].verifycode != NULL) {
        free(reg[idx].verifycode);
        reg[idx].verifycode = NULL;
    }
    free(reg[idx].cached_polygon);
}

void release_region(int rid)
{
    int i;
    
    if ((rear == front) && (isfull == 0)) { 
        return;
    }
    i = PREV_IDX(front); 
    while (1) { /* break in the loop */
        if (reg[i].rid == rid) { /* found */
            release_reg(i);
            reg[i] = reg[rear];
            INC_IDX(rear);
            isfull = 0;
            return; 
        }
        if (i == rear) {
            return; /* at the rear */
        }
        DEC_IDX(i);
    }
}

void release_regs()
{
    int i;
    
    if ((front == rear) && (isfull == 0)) {
        return;
    }
    i = PREV_IDX(front);
    while (1) { /* break in the internal */
        release_reg(i);
        if (i == rear) {
            break; /* at the rear */
        }
        DEC_IDX(i);
    }
    isfull = front = rear  = 0; /* set empty */
}

int get_rversion(int rid, unsigned char *rversion)
{
    int idx;
    const char *rname;
    FILE *fp;

    idx = find_reg(rid);
    if (idx != -1) {
        memcpy(rversion, reg[idx].version, 6);
        return 0;
    }
    rname = tk_get_region_path(rid);
    if ((fp = fopen(rname, "r")) == NULL) {
        LOG_DBG("%s %d: %s", __FILE__, __LINE__, strerror(errno)); 
        return -1;
    }
    fseek(fp, 6, SEEK_CUR);
    fread(rversion, 1, 6, fp); 
    fclose(fp);   
    return 0;
}

static struct tk_point* get_region_bound(int rid, int *point_num)
{
    int idx;
    unsigned char buf[8];
	int i;

    idx = find_reg(rid);
    if (idx != -1) {
        *point_num = reg[idx].point_num;
        return reg[idx].cached_polygon;
    }
    
    *point_num = 0;
    i = fseek(pf_spi, reg_bounds[rid].offset, SEEK_SET);
    i = fread(point_num, 1, 1, pf_spi);
    if (*point_num >= TK_MAX_POINT_IN_BOUND) {
        LOG_INFO("Too many points of city bound order %d, point num: %d\n", rid, *point_num);
    }

    for (i = 0; i < *point_num; i++) {
        fread(buf, 1, 8, pf_spi);
        cached_polygon[i].levelCode = (buf[0] >> 7) & 0x1f; /* &0x1? YHQ */
        cached_polygon[i].x = ((buf[0] & 0x7f) << 24) + (buf[1] << 16) + (buf[2] << 8) + buf[3];
        cached_polygon[i].y = (buf[4] << 24) + (buf[5] << 16) + (buf[6] << 8) + buf[7];
    }
    return cached_polygon;
}

/*======================================================
 * the code block for the map engine initial
 *======================================================*/
struct tk_engine tk_engine; //meta data for tile buffer

//the offset of each image's content
int img_offset[TK_IMG_TIGER_MAP_VECTOR_MAX];
// the buffer which store all the images.
unsigned char *img_buffer;
/*
 * load image files of the icons
 * return 1 if load success, else -1
 */
extern int tk_icon_size;

int load_images()
{
    int actual;
    int num;
    int curpos;
    int file_size;
    FILE *fp;
    char file_name[TK_MAX_PATH];
    unsigned char *img_buffer_16 = NULL;
    int  img_16_len = 0;

	//sprintf(file_name, "%s/%s", respath, F_IMG);
    sprintf(file_name, "%s/%s%d.dat", respath, F_IMG, tk_icon_size);
    if ((fp = fopen(file_name, "rb")) == NULL) {
        LOG_INFO("%s %d %s: %s\n", __FILE__, __LINE__, file_name,  strerror(errno));
        return -1;
    }
    file_size = get_fsize(file_name);
    
    actual = fread(&num, 1, 4, fp);
    if (actual != 4) {
        fclose(fp);
        LOG_INFO("error while reading images' count!\n");
        return -1;
    }
    if (num != TK_IMG_TIGER_MAP_VECTOR_MAX) {
        fclose(fp);
        LOG_INFO("the image's count not unified with the definition in the code!\n");
        return -1;
    }
    actual = fread(img_offset, 1, 4 * TK_IMG_TIGER_MAP_VECTOR_MAX, fp);
    if (actual != 4 * TK_IMG_TIGER_MAP_VECTOR_MAX) {
        fclose(fp);
        LOG_INFO("error while reading image offset!\n");
        return -1;
    }
    
    curpos = ftell(fp);
    if ((img_buffer = (unsigned char *)malloc(file_size - curpos)) == NULL) {
        fclose(fp);
        LOG_INFO("fail to alloc mem for img_buffer!\n");
        return -1;
    }
    actual = fread(img_buffer, 1, file_size - curpos, fp);
    fclose(fp);
    if (actual != file_size - curpos) {
        LOG_INFO("error while reading image!\n");
        return -1;
    }
    
#ifdef TK_BPP_16
    /*  (whole_len - w_h_len) /  2 , and + w_h_len */ 
    img_16_len = ((actual - (TK_IMG_TIGER_MAP_VECTOR_MAX << 2)) >> 1) + (TK_IMG_TIGER_MAP_VECTOR_MAX << 2);
    img_buffer_16 = (unsigned char *)xmalloc(img_16_len);
    converse_bmp32_to_16(img_buffer_16,img_buffer,img_offset);
    free(img_buffer);
    img_buffer = img_buffer_16;
#endif
    return 0;
}
extern short  tk_gdi_rgb565(int Color);
void converse_bmp32_to_16(char *img_buffer_16,char *img_buffer,int *offset)
{
    int     i = 0,j = 0;
    int     valid_img = 0;
    int     cur_off = 0;
    int     wh = 0;
    int     w_h = 0;
    char    *cur_data = 0;
    short * bmp_16 = 0;
    int   * bmp_32 = 0;
    for(; i < TK_IMG_TIGER_MAP_VECTOR_MAX; i++){
        cur_off = offset[i];
        if(-1 == cur_off)
            continue;
        wh = *(int *)(img_buffer + cur_off);
        w_h = (wh & 0xff) * ((wh >> 16) & 0xff);
        cur_data = img_buffer_16 + ((cur_off - valid_img * 4) >> 1) + valid_img * 4;
        *(int *)(cur_data) = wh;
        for(j = 0; j < w_h; j++){
            bmp_16 = (short *)(cur_data + 4 + j * 2);
            bmp_32 = (int *)(img_buffer + cur_off + 4 + j * 4);
            *bmp_16 = tk_gdi_rgb565(*bmp_32);
        }
        offset[i] = ((cur_off - valid_img * 4) >> 1) + valid_img * 4;
        valid_img ++;
    }
}

void release_images()
{
    if (NULL != img_buffer) {
        free(img_buffer);
        img_buffer = NULL;
    }
//todo: free map
}

/*======================================================
 * the code block for the map engine context setting
 *======================================================*/

int reg_num = 0;


static int get_nInt(unsigned char *content, int *off) {
    int i;
    char str[256];
    int cnt = 0, flag = 0;

    do {
        i = *off;
        while (content[i] != '\n') {
            if (content[i] == '=') {
                flag = 1;
                i++;
                continue;
            }
            if (content[i] == '@') {
                flag = 0;
            }
            if (flag) {
                str[cnt++] = content[i];
            }
            i++;
        }
        *off = i + 1;
    } while (cnt == 0);
    str[cnt] = '\0';
    
    return atoi(str);
}

unsigned int tk_hash(char *str,int length)
{
    unsigned int hash = 0;
    unsigned int i = 0;
    for(i = 0; i < length; i++){
        hash ^= ((hash << 5) + (*str) + hash);
    }
    return hash;
}
//return layer_num
static int load_stylecfg(char *fname, struct styles_buffer *buff) 
{
    unsigned char *str;
    int offset, fsize;
    int i, k;
    FILE *fp = fopen(fname, "rb");
    
    fseek(fp, 0, SEEK_END);
    fsize = ftell(fp);
    str = malloc(fsize);
    if (str == NULL) {
        LOG_ERR("alloc memory failed!\n");
        return -1;
    }
    rewind(fp);
    fread(str, 1, fsize, fp);
    fclose(fp);
    offset = 0;
    buff->layer_num = get_nInt(str, &offset);
    buff->feature_type = malloc(sizeof(int) * buff->layer_num);
    buff->styles = malloc(sizeof(struct style) * buff->layer_num);
    if (buff->feature_type == NULL|| buff->styles == NULL) {
        return 0;
    }

    for (i = 0; i < buff->layer_num; i++) {
        int setting_num = 0;
        char pwnum = 0;
        
        setting_num = get_nInt(str, &offset); 
        buff->feature_type[i] = get_nInt(str, &offset);
        setting_num--;
        buff->styles[i].fontcolor = TK_COLOR2INDEX(get_nInt(str, &offset));
        setting_num--;
        get_nInt(str, &offset);
        setting_num--;
        get_nInt(str, &offset);
        setting_num--;
        buff->styles[i].zoom_min = get_nInt(str, &offset);
        setting_num--;
        buff->styles[i].zoom_max = get_nInt(str, &offset);
        setting_num--;
        buff->styles[i].fill_color = TK_COLOR2INDEX(get_nInt(str, &offset));
        setting_num--;
        if (buff->feature_type[i] != TKGEO_ENMFTYPE_ROAD) {
            tk_add_color(buff->styles[i].fill_color);
        }
        buff->styles[i].border_color = TK_COLOR2INDEX(get_nInt(str, &offset));
        setting_num--;
        pwnum = get_nInt(str, &offset);
        setting_num--;
        if (pwnum) {
            buff->styles[i].pen_width = malloc(sizeof(char) * pwnum);
            for (k = 0; k < pwnum; k++) {
                buff->styles[i].pen_width[k] = get_nInt(str, &offset);
                setting_num--;
            }
        } else {
            buff->styles[i].pen_width = malloc(sizeof(char));
            buff->styles[i].pen_width[0] = 0;
        }
        buff->styles[i].line_type = get_nInt(str, &offset);
        setting_num--;
        buff->styles[i].icon_id = get_nInt(str, &offset);
        setting_num--;
        buff->styles[i].label_style = get_nInt(str, &offset);
        setting_num--;
        buff->styles[i].label_min = get_nInt(str, &offset);
        setting_num--;
        buff->styles[i].label_max = get_nInt(str, &offset);
        setting_num--;
        buff->styles[i].font_size = get_nInt(str, &offset);
        setting_num--;
        buff->styles[i].font_alter = get_nInt(str, &offset);
        setting_num--;
        if (setting_num > 0) {
            int cir = 0;
            int lim = setting_num;
            for (cir = 0; cir < lim; cir++)
              get_nInt(str, &offset);
        }
    }
    free(str);
    return buff->layer_num;
}

struct BITMAPFILEHEADER {
	unsigned int bfSize; 
	unsigned short int bfReserved1; 
	unsigned short int bfReserved2; 
	unsigned int bfOffBits; 
};

struct BITMAPINFOHEADER {
	unsigned int biSize; 
	unsigned int biWidth; //the width of bitmap in pixels
	int biHeight;// the width of bitmap in pixels
	unsigned short int biPlanes; 
	unsigned short int biBitCount;//the bit number of each pixel
	unsigned int biCompression; 
	unsigned int biSizeImage; 
	unsigned int biXPelsPerMeter; 
	unsigned int biYPelsPerMeter; 
	unsigned int biClrUsed; 
	unsigned int biClrImportant; 
}; 

void load_context() 
{ 
    int i;
    int n_num, c_num;
    char file_path[TK_MAX_PATH];
    int layer_num_malloced;
 
    sprintf(file_path, "%s/"F_CSTYLE, respath);
    c_num = load_stylecfg(file_path, &(tk_engine.c_styles));
    sprintf(file_path, "%s/"F_NSTYLE, respath); 
    n_num = load_stylecfg(file_path, &(tk_engine.n_styles));

    layer_num_malloced = TK_MAX(c_num, n_num);
    layer_ctl = xmalloc(sizeof(int) * layer_num_malloced);
    for (i = 0; i < layer_num_malloced; i++) {
        layer_ctl[i] = 1;
    }
    tk_engine.layer_list = (struct layer*)xcalloc(layer_num_malloced, sizeof(struct layer));
    tk_gdi.draw_point_buffer_len = MAX_POINT_CLIP;
    tk_gdi.draw_point_buffer = malloc(MAX_POINT_CLIP * sizeof(struct tk_point));
}

void release_layers()
{
    free(tk_engine.layer_list);
}

int load_envelope() 
{
    FILE *envelopes;
    char file_path[TK_MAX_PATH] = "\0";
    int i;
	unsigned char envelopebuf[20];
    
    sprintf(file_path, "%s/"F_ENV, respath);
    envelopes = fopen(file_path, "rb");
	LOG_DBG("envelopes %d, path: %s\n", envelopes, file_path);

    sprintf(file_path, "%s/"F_SPI, respath);
    pf_spi = fopen(file_path, "rb");
	LOG_DBG("pf_spi %d, path: %s\n", pf_spi, file_path);
    
    // skip the version of map data and software
    fseek(envelopes, TK_VERNO_LENGTH, SEEK_CUR);
    
    reg_bounds = malloc(sizeof(struct reg_rects)* (reg_num + 1));
    for (i = 0; i < reg_num; i++) {
        fread(envelopebuf, sizeof(unsigned char), 20, envelopes);
        reg_bounds[i].env.left = (envelopebuf[0] << 24) + (envelopebuf[1] << 16) + (envelopebuf[2] << 8) + envelopebuf[3];
        reg_bounds[i].env.bottom = (envelopebuf[4] << 24) + (envelopebuf[5] << 16) + (envelopebuf[6] << 8) + envelopebuf[7];
        reg_bounds[i].env.right = (envelopebuf[8] << 24) + (envelopebuf[9] << 16) + (envelopebuf[10] << 8) + envelopebuf[11];
        reg_bounds[i].env.top = (envelopebuf[12] << 24) + (envelopebuf[13] << 16) + (envelopebuf[14] << 8) + envelopebuf[15];
        reg_bounds[i].offset = (envelopebuf[16] << 24) + (envelopebuf[17] << 16) + (envelopebuf[18] << 8) + envelopebuf[19];
    }
    LOG_DBG("Reading Region Num: %d\n", reg_num);
    fclose(envelopes);
	return 0;
}

void release_reg_bounds()
{
    fclose(pf_spi);
    free(reg_bounds);
}

//周边地物到中心点距离的上限
#define TK_DEFAULT_DISTANCE_OF_LOCAL_FEATURE 16
int tk_init_bestfeature() {
    best_feature.name[0] = '\0';
    best_feature.distance_to_center = 6000; // set a max value for initial
    best_feature.screen_center_coord.x = tk_engine.width / 2;
	best_feature.screen_center_coord.y = tk_engine.height / 2;
	best_feature.radius = TK_DEFAULT_DISTANCE_OF_LOCAL_FEATURE;
	best_feature.screen_center.x = tk_engine.center_x -
		(tk_engine.width >> 1 << (16 - tk_engine.current_z + 2) >> 2) + (best_feature.screen_center_coord.x << (16 - tk_engine.current_z + 2) >> 2);
	best_feature.screen_center.y = tk_engine.center_y -
		(tk_engine.height >> 1 << (16 - tk_engine.current_z + 2) >> 2 ) + (best_feature.screen_center_coord.y << (16 - tk_engine.current_z + 2) >> 2);
    return 1;
}

/* =========================================================
 *        Part for load regionlist and citylist.
 * ========================================================= */

struct prov_unit *prov_list = NULL;
struct city_unit *city_list = NULL;
struct reg_unit  *reg_list = NULL;
struct nat_unit nat;

static char *name_pool = NULL;
static int pool_size = 0;
static int mem_pos = 0;
int city_num = 0;
int prov_num = 0;

static char* add_name(const char *str)
{
    char *result;
    if (mem_pos + 1 + ((int)strlen(str)) > pool_size) {
        return NULL;
    }
    result = name_pool + mem_pos;
    strcpy(result, str);
    mem_pos = mem_pos + strlen(str) + 1;
    return result;
}

static int str_pos = 0; /* indicate the read position of string str */
static char* sgets(char *line, int n, const char *str)
{
    register char *p;
    int c;

    if (n <= 0) {
        goto ERROR;
    }

    p = line;
    str = str + str_pos;

    while (--n) {
        if ((c = *str) == '\0') {
            break;
        }
        if ((*p++ = c) == '\n') {
            str_pos++;
            str++;
            break;
        }
        str_pos++;
        str++;
    }

    if (p > line) {
        *p = '\0';
        return line;
    }
ERROR:
    return NULL;
}

#define str(x) # x 
#define xstr(x) str(x)

int load_citylist()
{
    FILE *fp;
    int i;
    char city_path[TK_MAX_PATH];
    char line[TK_MAX_LINE_LENGTH];
    char cname[TK_MAX_NAME], cename[TK_MAX_NAME], pname[TK_MAX_NAME], pename[TK_MAX_NAME];
    int city_id, zoom_level, p_priority, c_priority;
    int fsize;
    int data_pos;
    struct tk_latlon center;
    char *str_buff;
    int *cur_city;
    char *format_str = "%" xstr(TK_MAX_NAME) "s%" xstr(TK_MAX_NAME) "s%" xstr(TK_MAX_NAME) "s%" xstr(TK_MAX_NAME) "s%lf%lf%d%d%d%d\r\n";
    sprintf(city_path, "%s/citylist", respath); 
    fsize = get_fsize(city_path);
    if ((str_buff = malloc(fsize + 1)) == NULL) {
        LOG_INFO("Memory allocate fail in %s, %d.\n", __FILE__, __LINE__);
        return -1;
    }
    if ((fp = fopen(city_path, "r")) == NULL) {
        LOG_INFO("Load %s file fail in %s, %d.\n", city_path, __FILE__, __LINE__);
        return -1;
    }
    if (fread(str_buff, 1, fsize, fp) != fsize) {
        LOG_INFO("fread error in %s, %d.\n", __FILE__, __LINE__);
        return -1;
    }
    *(str_buff + fsize) = '\0';
    str_pos = 0;
    sgets(line, TK_MAX_LINE_LENGTH, str_buff);
    sscanf(line, "%d", &pool_size);
    if ((name_pool = malloc(pool_size)) == NULL) {
        LOG_INFO("Memory allocate fail in %s, %d.\n", __FILE__, __LINE__);
        return -1;
    }
    /* read the header line */
    sgets(line, TK_MAX_LINE_LENGTH, str_buff);
    /* read the national related data */
    sgets(line, TK_MAX_LINE_LENGTH, str_buff);
    data_pos = str_pos;
    sscanf(line, format_str, cname, cename, pname, pename, &center.lon, &center.lat,
           &c_priority, &p_priority, &zoom_level, &city_id);
    nat.cname = add_name(cname);
    nat.cename = add_name(cename);
    nat.pname = add_name(pname);
    nat.pename = add_name(pename);
    nat.center.lon = center.lon;
    nat.center.lat = center.lat;
    prov_num = city_num = 0;
 	while (sgets(line, TK_MAX_LINE_LENGTH, str_buff) != NULL) {
        sscanf(line, format_str, cname, cename, pname, pename, &center.lon, &center.lat,
               &c_priority, &p_priority, &zoom_level, &city_id);
        if (p_priority > prov_num) {
            prov_num = p_priority;
        }
        if (city_id > city_num) {
            city_num = city_id;
        }
    }
    prov_list = (struct prov_unit *)malloc(sizeof(struct prov_unit) * (prov_num + 1));
    city_list = (struct city_unit *)malloc(sizeof(struct city_unit) * (city_num + 1));
    cur_city = (int *)malloc(sizeof(int) * (prov_num + 1));
    if ((city_list == NULL) || (prov_list == NULL) || (cur_city == NULL)) {
        LOG_INFO("Memory allocate fail in %s, %d.\n", __FILE__, __LINE__);
        return -1;
    }
    for (i = 0; i < prov_num + 1; i++) {
        cur_city[i] = -1;
        prov_list[i].name = NULL;
        prov_list[i].ename = NULL;
        prov_list[i].city_idx = -1;
    }
    for (i = 0; i < city_num + 1; i++) {
       city_list[i].cid = -1;
       city_list[i].name = NULL;
       city_list[i].ename = NULL;
       city_list[i].reg_idx = -1;
       city_list[i].next = -1;
       city_list[i].prov_idx = -1;
    }
    str_pos = data_pos;
 	while (sgets(line, TK_MAX_LINE_LENGTH, str_buff) != NULL) {
        sscanf(line, format_str, cname, cename, pname, pename, &center.lon, &center.lat,
               &c_priority, &p_priority, &zoom_level, &city_id);
        if (prov_list[p_priority].name == NULL) {/* first encount this province */
            prov_list[p_priority].name = add_name(pname);
            prov_list[p_priority].ename = add_name(pename);
            prov_list[p_priority].city_idx = city_id;
            city_list[city_id].cid = city_id; 
            city_list[city_id].name = add_name(cname);
            city_list[city_id].ename = add_name(cename);
            city_list[city_id].center.lon = center.lon;
            city_list[city_id].center.lat = center.lat;
            city_list[city_id].zoom_level = zoom_level;
            city_list[city_id].prov_idx = p_priority; 
            cur_city[p_priority] = city_id;
        } else {
            city_list[city_id].cid = city_id; 
            city_list[city_id].name = add_name(cname);
            city_list[city_id].ename = add_name(cename);
            city_list[city_id].center.lon = center.lon;
            city_list[city_id].center.lat = center.lat;
            city_list[city_id].zoom_level = zoom_level;
            city_list[city_id].prov_idx = p_priority; 
            city_list[cur_city[p_priority]].next = city_id;
            cur_city[p_priority] = city_id;
        }
    }
    /* ----use for test--- */
    //for (i = 1; i < prov_num + 1; i++) {
    //    LOG_DBG("%s %s %d\n", prov_list[i].name, prov_list[i].ename, prov_list[i].city_idx);
    //}
    //for (i = 1; i < city_num + 1; i++) {
    //    if (city_list[i].cid != -1) {
    //        LOG_DBG("%d %s %s %lf %lf %d %d %d\n", city_list[i].cid, city_list[i].name, 
    //                city_list[i].ename, city_list[i].center.lon, city_list[i].center.lat,
    //                city_list[i].zoom_level, city_list[i].prov_idx, city_list[i].next);
    //    }
    //}
    /* ----use for test--- */
    free(cur_city);
    free(str_buff);
    fclose(fp);
    return 0;
}

int find_index_swcity(int rid);
int update_swcity(int rid) {
        if(rid == -1)
            return -1;
        int i = 0;
        for(i = 0; i < sw_city.city_num; i++){
               if(sw_city.city_info[i].rid == rid){
                    return i;
               }
        }
        sw_city.city_info[i].rid = rid;
        sw_city.city_num++;
        return i;
}

int update_swcolor(int rid,int color,char* name) {
    int i = 0;
    int index = update_swcity(rid);
    int color_num = sw_city.city_info[index].color_num;
    if (index == -1)
        return -1;
    sw_city.city_info[index].color_info[color_num].color = color;
    strcpy(sw_city.city_info[index].color_info[color_num].sw_name, name);
    sw_city.city_info[index].color_num++;
    return 0;
}
int load_sw_colorcfg()
{
    int i = 0;
    int fsize;
    char *str_buff;
    char city_path[TK_MAX_PATH];
    char line[TK_MAX_LINE_LENGTH];
    FILE *fp;
    char *format =  "%s %d %s %s\r\n";
    char city[32];
    int cityid;
    char sw_name[32];
    char color[32];
    int  hash_index = 0;
    int  old_rid = 0;
    char *end;
    memset(&sw_city,0,sizeof(sw_city));
    sprintf(city_path, "%s/subwaycolor", respath);
    fsize = get_fsize(city_path);
    if ((str_buff = malloc(fsize + 1)) == NULL) {
        LOG_INFO("Memory allocate fail in %s, %d.\n", __FILE__, __LINE__);
        return -1;
    }
    fp = fopen(city_path,"rb");
    if (fread(str_buff, 1, fsize, fp) != fsize) {
        LOG_INFO("fread error in %s, %d.\n", __FILE__, __LINE__);
        return -1;
    }
    str_pos = 0;
    *(str_buff + fsize) = '\0';
    memset(city,0,sizeof(city));
    memset(sw_name,0,sizeof(sw_name));
    while (sgets(line, TK_MAX_LINE_LENGTH, str_buff) != NULL) {
        sscanf(line, format, city, &cityid, sw_name, color);
        if(old_rid != cityid){
            old_rid = cityid;
            i = 0;
        }
        update_swcolor(cityid,strtol(color,&end,16),sw_name);
        //hash_index = find_index_swcity(cityid);
        //hash_index = update_swcity(cityid);
        //if(hash_index == -1)
        //    return -1;
        //strcpy(sw_color[hash_index][i].sw_name, sw_name);
        //code_convert("utf-8","gbk",sw_name,strlen(sw_name),sw_color[hash_index][i].sw_name,32);
        //sw_color[hash_index][i].color = strtol(color,&end,16);
        memset(city,0,sizeof(city));
        memset(sw_name,0,sizeof(sw_name));
        memset(line,0,TK_MAX_LINE_LENGTH);
        i++;
    }
    free(str_buff);
    return 0;
}

int load_regionlist()
{
    FILE *fp;
    int i;
    int *cur_reg;
    int file_size;
    int data_pos;
    int city_id, region_id;
    char cname[TK_MAX_NAME], ename[TK_MAX_NAME];
    char reg_path[TK_MAX_PATH];
    char line[TK_MAX_LINE_LENGTH];
    char *str_buff;
    char *format_str = "%d%d" "%" xstr(TK_MAX_NAME) "s%" xstr(TK_MAX_NAME) "s%d\r\n";

    sprintf(reg_path, "%s/"F_RLIST, respath); 
    file_size = get_fsize(reg_path);
    if ((str_buff = malloc(file_size + 1)) == NULL) {
        LOG_INFO("Memory allocate fail in %s, %d.\n", __FILE__, __LINE__);
        return -1;
    }
    if ((fp = fopen(reg_path, "r")) == NULL) {
        LOG_INFO("Load %s file fail in %s %d.\n", reg_path, __FILE__, __LINE__);
        return -1;
    }
    if (fread(str_buff, 1, file_size, fp) != file_size) {
        LOG_INFO("fread error in %s, %d.\n", __FILE__, __LINE__);
        return -1;
    }
    *(str_buff + file_size) = '\0';
    str_pos = 0;    /* read a string at the beginning */
    /* read header */
    sgets(line, TK_MAX_LINE_LENGTH, str_buff);
    /* read national related data */
    sgets(line, TK_MAX_LINE_LENGTH, str_buff);
    data_pos = str_pos;
    sscanf(line, format_str, &region_id, &city_id, ename, cname, &file_size);
    nat.rid = region_id;
    nat.rname = add_name(cname);
    nat.rename = add_name(ename);
    nat.file_size = file_size;

    while (sgets(line, TK_MAX_LINE_LENGTH, str_buff) != NULL) {
        reg_num++;
    }
    reg_list = (struct reg_unit *)malloc(sizeof(struct reg_unit) * reg_num);
    cur_reg = (int *)malloc(sizeof(int) * reg_num);
    if (reg_list == NULL || cur_reg == NULL) {
        LOG_INFO("Memory allocate fail in %s, %d.\n", __FILE__, __LINE__);
    }
    for (i = 0; i < reg_num; i++) {
        reg_list[i].rid = -1;
        reg_list[i].name = NULL;
        reg_list[i].ename = NULL;
        reg_list[i].city_idx = -1;
        reg_list[i].next = -1;
        cur_reg[i] = -1;
    }
    str_pos = data_pos;
    while (sgets(line, TK_MAX_LINE_LENGTH, str_buff) != NULL) {
        sscanf(line, format_str, &region_id, &city_id, ename, cname, &file_size);
        reg_list[region_id].rid = region_id;
        reg_list[region_id].name = add_name(cname);
        reg_list[region_id].ename = add_name(ename);
        reg_list[region_id].file_size = file_size;
        reg_list[region_id].city_idx = city_id;
        if (cur_reg[city_id] == -1) {
            city_list[city_id].reg_idx = region_id;
        } else {
            reg_list[cur_reg[city_id]].next = region_id;
        }
        cur_reg[city_id] = region_id;
    }
    /* -------use for test -------- */
    /* for (i = 0; i < reg_num; i++) {
        printf("%d, %s, %s, %d, %d, %d\n", reg_list[i].rid,
               reg_list[i].name, reg_list[i].ename,
               reg_list[i].file_size, reg_list[i].city_idx,
               reg_list[i].next); 
    } */
    /* -------use for test -------- */
    free(str_buff);
    free(cur_reg);
    fclose(fp);
    return 0;
}

void release_list()
{
    if (polygon_reader)
        fclose(polygon_reader);
    free(name_pool);
    mem_pos = 0;
    free(city_list);
    free(prov_list);
    free(reg_list);
}

static void release_pool(struct tk_pool *tar) 
{
    if (tar != NULL) {
        if (tar->tk_map_names != NULL) {
            free(tar->tk_map_names);
        }
        if (tar->tk_points != NULL) {
            free(tar->tk_points);
        }
    }
    return;
}

static void release_tile_struct(struct tile *tl) 
{

    if (tl != NULL) {
        release_pool(&tl->mem_pool);
        free(tl->features);
        free(tl);
    }
    return;
}

void release_tb()
{
    struct tile *prev, *temp;
    temp = tk_engine.tb;
    tk_engine.tb = NULL;
    while (temp) {
        prev = temp;
        temp = temp->next;
        release_tile_struct(prev);
    }
    tk_engine.tilebuf_length = 0;
    tk_engine.tb_size = 0;
}

static void free_style(struct styles_buffer *ps)
{
    int i; 
    free(ps->feature_type);
    for (i = 0; i < ps->layer_num; i++) {
        free(ps->styles[i].pen_width);
    }
    free(ps->styles);
}

void release_styles()
{
    free_style(&tk_engine.n_styles);
    free_style(&tk_engine.c_styles);
}
/* =========================================================
 *        Part for load vector map.
 * ========================================================= */

#define TK_MAPLOADER_OK              0
#define TK_MAPLOADER_ERROR          -1
#define TK_MAPLOADER_OUT_BOUND      -2
#define TK_MAPLOADER_FILE_NOT_FOUND -3
#define TK_INDEX_OUT_OF_BOUND       -4
#define TK_MAPLOADER_NO_FILE        -5



#define TK_ENV_NO_OVERLAP( rect1, rect2) \
          (((rect1).right < (rect2).left) || ((rect1).bottom > (rect2).top) \
        || ((rect1).left > (rect2).right) || ((rect1).top < (rect2).bottom))

#define TK_POINT_IN_ENV( pt, rect) (pt->x >= rect.left && pt->x <= rect.right \
        && pt->y <= rect.top && pt->y >= rect.bottom)

#define TK_CODE_NOT_DISPLAY -1 
#define TK_TILE_BUFF_POINT 1200
#define TK_FEATURE_MAX_TILE  400

#define TK_POINT_NUMBER 8

#define TILE_SIZE_BITS 8

enum EN_TKREADSTATE {
	TK_READ_OK=0,
	TK_READ_TILE_OUT_BORDER,
	TK_READ_TILE_MISS,
	TK_READ_MAX,
    TK_READ_TILE_LENGTH_ERR,
    TK_READ_TILE_FTNUM_ERR,
    TK_READ_TILE_NAMENUM_ERR,
    TK_READ_TILE_POINTNUM_ERR,
    TK_READ_FT_POINTNUM_ERR,
    TK_READ_FT_NAMELEN_ERR
};

static struct tk_pool *cur_mem_pool;

static int cur_tile_level = 0; //level标志对应tile单向上由多少个原始大小tile合并而来.
static unsigned char *tk_tile_databuf = NULL;
static unsigned int tk_tile_databuflen = 0;
static int _tk_isdl = 0;
static int buf_pos;
static unsigned char remain_bits;
static unsigned char remain_value;
static int ref_tile_x = 0;
static int ref_tile_y = 0;
int regnum_in_bound = 0;
static int reg_in_bound[TK_REGION_CACHED_MAX];
static int cur_idx;
static int is_old_data = 0;
int get_cur_rversion(unsigned char *rversion)
{
    if (regnum_in_bound > 0)
        return get_rversion(reg_in_bound[0], rversion);
    else return -1;
} 

/* judge the relation of point with poly */
static int is_point_in_region(int rid, struct tk_point *pt)
{
	int i;
    int cnt = 0;
    struct envelope env;
    int point_num;
    struct tk_point *polygon;

    polygon = get_region_bound(rid, &point_num);
    if (rid == TK_REGION_ID_OUT_BOUND) {
        return 0;
    } 

	for (i = 0;	i < point_num - 1; i++) {
        env.bottom = TK_MIN(polygon[i].y, polygon[i + 1].y);
        env.left = TK_MIN(polygon[i].x, polygon[i + 1].x);
        env.top = TK_MAX(polygon[i].y, polygon[i + 1].y);
        env.right = TK_MAX(polygon[i].x, polygon[i + 1].x);

		/* levelCode=1 indicate the point is belong to another polygon */
        if (polygon[i+1].levelCode == 1) {
            continue;
        }
        /* 计算点是否在多边形的外围矩形中 */

        if (!TK_POINT_IN_ENV(pt, env)) {
            if (pt->x > env.right && pt->y < env.top && pt->y >= env.bottom) {
				cnt++;
			}
           continue;
        }

		if( polygon[i].y != polygon[i+1].y )
		{
            if((env.top == pt->y )){
				continue;
			}
			if(( polygon[i].y == pt->y && polygon[i].x < pt->x)
                    || ( polygon[i + 1].y == pt->y && polygon[i + 1].x < pt->x)) {
				cnt++;
				continue;
			} else {
                double x_pro;
                double y_pro;

                if ((polygon[i].y >= polygon[i + 1].y && polygon[i].x >= polygon[i + 1].x)
                    || (polygon[i].y <= polygon[i + 1].y && polygon[i].x <= polygon[i + 1].x)){
                    x_pro = ((double) (env.top - env.bottom)) * (pt->x - env.left);
                    y_pro = ((double) (pt->y - env.bottom)) * (env.right - env.left);
                } else {
                    x_pro = ((double) (env.top - env.bottom)) * (pt->x - env.right);
                    y_pro = ((double) (pt->y - env.bottom)) * (env.left - env.right);
                }

                if (x_pro == y_pro) {
                	return 1;
				} else {
                    if (x_pro > y_pro) {
                        LOG_DBG("\n left one: start : %d %d end: %d %d, x_pro: %f, y_pro: %f \n ",
                            polygon[i].x, polygon[i].y, polygon[i + 1].x, polygon[i + 1].y, x_pro, y_pro);
                        cnt++;
                    }
                }
			}
		} else {
            /* horizontal line */
            if (pt->y == env.bottom) {
                if (pt->x >= env.left) {
                    if (pt->x <= env.right) {
                        return 1;
                    }
                }
            }
        }
	}

	if (cnt % 2 == 1) {
		return 1;
    }
	return 0;
}

#define TK_LUMBDA(min, max, p) (double)(p - min) / (max - min) 

/* function:judge the relation of rect with poly, true iff edge intersects */
static int is_rect_in_region(int rid, struct envelope src_env)
{
    int i;
    struct envelope env;
    int point_num;
    struct tk_point *polygon;
    int in_poly1 = 0;
    int in_poly2 = 0;

    polygon = get_region_bound(rid, &point_num);
    if (rid == TK_REGION_ID_OUT_BOUND) {
        return 0;
    } 

    for (i = 0; i < point_num - 1;  i++)
    {
        double u=0;
        int intserc_p = 0;
        int is_tan_positive = 1; 
        env.bottom = TK_MIN(polygon[i].y, polygon[i + 1].y);
        env.left = TK_MIN(polygon[i].x, polygon[i + 1].x);
        env.top = TK_MAX(polygon[i].y, polygon[i + 1].y);
        env.right = TK_MAX(polygon[i].x, polygon[i + 1].x);
        if (TK_ENV_NO_OVERLAP(src_env, env)) {
            continue;
        }            
        if (polygon[i + 1].levelCode==1) {
            continue;
        }
        in_poly1 = TK_POINT_IN_ENV((polygon + i), src_env);
        in_poly2 = TK_POINT_IN_ENV((polygon + i + 1), src_env);
 
        if (in_poly1 || in_poly2) {
            return 1;
        }
        if ((env.bottom == polygon[i].y && env.left == polygon[i].x)||(env.bottom == polygon[i+1].y && env.left == polygon[i+1].x)) {
            is_tan_positive = 1; 
        } else {
            is_tan_positive = 0;
        }
        if (src_env.bottom <= env.top && src_env.bottom >= env.bottom) {
             u = TK_LUMBDA(env.bottom, env.top, src_env.bottom);
             if (is_tan_positive) {
                intserc_p = (int)((double)env.left + u * (env.right - env.left));
             } else {
                intserc_p = (int)((double)env.right + u * (env.left - env.right));
             }

             if (intserc_p >= src_env.left && intserc_p <= src_env.right) {
                return 1;
             }
        }
        
        if (src_env.top <= env.top && src_env.top >= env.bottom) {
             u = TK_LUMBDA(env.bottom, env.top, src_env.top);  
             if (is_tan_positive) {
                 intserc_p = (int)((double)env.left + u * (env.right - env.left));
             } else {
                 intserc_p = (int)((double)env.right + u * (env.left - env.right));
             }

             if (intserc_p >= src_env.left && intserc_p <= src_env.right) {
                return 1;
             }
        }
        if (src_env.left <= env.right&& src_env.left >= env.left) {
             u = TK_LUMBDA(env.left, env.right, src_env.left);
             
             if (is_tan_positive) {
                 intserc_p= (int)((double)env.bottom + u * (env.top - env.bottom));
             } else {
                 intserc_p= (int)((double)env.top + u * (env.bottom - env.top)); 
             }
             
             if (intserc_p >= src_env.bottom && intserc_p <= src_env.top) {
                return 1;
             }
        }
        if (src_env.right <= env.right&& src_env.right >= env.left) {
             u = TK_LUMBDA(env.left, env.right, src_env.right); 
             
             if (is_tan_positive) {
                 intserc_p= (int)((double)env.bottom + u * (env.top - env.bottom));
             } else {
                 intserc_p= (int)((double)env.top + u * (env.bottom - env.top)); 
             }
             if (intserc_p >= src_env.bottom && intserc_p <= src_env.top) {
                return 1;
             }
        }
    }
    return 0;
}


static int get_morton_code(int x, int y) 
{
    int j, newX, newY;
    if (y == 0) {
        if (x == 0)
            return 0;
        if (x == 1)
            return 1;
    }
    if (y == 1) {
        if (x == 0)
            return 2;
        if (x == 1)
            return 3;
    }
    if (x == 0) {
        newX = -1;
    } else {
        j = x;
        newX = 0;
        while (j > 1) {
            j = j >> 1;
            newX++;
        }
    }
    j = y;
    newY = 0;
    while (j > 1) {
        j = j >> 1;
        newY++;
    }    

    if (newX > newY) {
        return ((1 << (newX << 1)) + get_morton_code(x - (1 << newX), y));
    } else if (newY > newX) {
        return ((2 << (newY << 1)) + get_morton_code(x, y - (1 << newY)));
    } else {
        return ((3 << (newY << 1)) + get_morton_code(x - (1 << newX), y - (1 << newY)));
    }
}

static int morton_equal(int morton_tile, int morton_file,
                            int morton_level) 
{
    if (((morton_tile - morton_file) >= 0) 
         && ((morton_tile - morton_file) >> (morton_level * 2) == 0))
        return 1;
    else
        return 0;
}

static void  morton_read(int index, int *code, int *level) 
{
    unsigned char *pointer = reg[cur_idx].index + 6 * index;
    *level = (int)pointer[0] >> 4;
    *code = (int)(((pointer[0] & 0xf) << 16) | (pointer[1] << 8) | (pointer[2]));
    return;
}
static int update_tile(int x, int y, int region_id) 
{
    tk_remove_region_data(region_id);
        return 0;
}


static int seek_tile(int x, int y, short int* state, int region_id, int *tile_length) 
{
    int pos = 0, nextpos = 0;
    int cur_code, cur_level;
    int upbound;
    int lowbound;
    int cur_t_number;
    int final;
    int morton_code;
    FILE *fp;
    unsigned char* cur_pointer;
    unsigned char* reg_index;
    unsigned char* verifycode;
    int index_size;
    int map_data_bias;
    unsigned char i, j, k;
    int rid, offset = 0, length = 0;

    *state = TK_READ_OK;
    fp = reg[cur_idx].fp;
    reg_index = reg[cur_idx].index;
    index_size = reg[cur_idx].index_size;
    verifycode = reg[cur_idx].verifycode;
    map_data_bias = reg[cur_idx].map_data_bias;

    upbound = reg[cur_idx].tile_bound[tk_engine.bl_idx].up; 
    lowbound = reg[cur_idx].tile_bound[tk_engine.bl_idx].low; 
    final = upbound;
    cur_t_number = (upbound + lowbound) / 2;

    if (x - ref_tile_x < 0 || y - ref_tile_y < 0)
        *state = TK_READ_TILE_OUT_BORDER;
    if (*state == TK_READ_OK)
        morton_code = get_morton_code(x - ref_tile_x, y - ref_tile_y);

    while (*state == TK_READ_OK) {
        if (upbound < lowbound) {
            *state = TK_READ_TILE_OUT_BORDER;
            break;
        }
        /*cur_level = tile levelcode  tile 合并次数最大位3(参考数据生成程序) */
        morton_read(cur_t_number, &cur_code, &cur_level);
        if (morton_equal(morton_code, cur_code, cur_level)) {
            break;
        } else {
            if (morton_code < cur_code) {
                cur_t_number--;
                upbound = cur_t_number;
                cur_t_number = (lowbound + upbound) / 2;
            } else {
                cur_t_number++;
                lowbound = cur_t_number;
                cur_t_number = (lowbound + upbound) / 2;
            }
        }
    }
    if (*state != TK_READ_OK) {
        cur_tile_level = 0;
        return *state;
    }
    cur_tile_level = cur_level;
    cur_pointer = reg_index + 6 * cur_t_number + 3;
    pos = (cur_pointer[0] << 16) | (cur_pointer[1] << 8) | cur_pointer[2];

    if (cur_t_number < index_size - 1) {
        /* if the seeked tile is the final tile of current level, it should be
           add 12 extra bytes which record the tile nums of next level. */
        if (cur_t_number == final) {
            cur_pointer = reg_index + 6 * cur_t_number + 3 + 18;
        } else {
            cur_pointer = reg_index + 6 * cur_t_number + 3 + 6;
        }
        nextpos = (cur_pointer[0]<<16) | (cur_pointer[1]<<8) | cur_pointer[2];
        offset = pos + map_data_bias;
        length = nextpos - pos;
    } else {
        fseek(fp, 0, SEEK_END); 
        nextpos = ftell(fp); 
        offset = pos + map_data_bias;
        length = nextpos - pos - map_data_bias;
    }
    
    rid = -1;
    //add by xzm 2010.11.05 for the downloading while exploring
    if (NULL != verifycode) {
        if (cur_t_number < index_size - 1) {
            if (-1 == fseek(fp, nextpos - 1 + map_data_bias, SEEK_SET))
                LOG_ERR("Error while reading map's Data from File:#1!\n");
            if (1 != fread(&i, 1, 1, fp))
                LOG_ERR("Error while reading map's Data from File:#2!\n");
            j = verifycode[cur_t_number/8];
            k = cur_t_number % 8;
            if (((0x01L)&(i ^ (j >> k))) == 0) {
                rid = region_id;
                *state = TK_READ_TILE_MISS;
            }
        } else {
            if (-1 == fseek(fp, nextpos - 1, SEEK_SET))
                LOG_ERR("Error while reading map's Data from File:#3!\n");
            if (1 != fread(&i, 1, 1, fp))
                LOG_ERR("Error while reading map's Data from File:#4!\n");
            j = verifycode[cur_t_number/8];
            k = cur_t_number % 8;
            if (((0x01L)&(i ^ (j >> k))) == 0) {
                rid = region_id;
                *state = TK_READ_TILE_MISS;
            }
        }
        if ((_tk_isdl == 0) && (lostdata_idx < TK_LOST_DATA_PIECE) && (*state == TK_READ_TILE_MISS)) {
            lostdata[lostdata_idx].rid = rid;
            lostdata[lostdata_idx].offset = offset;
            lostdata[lostdata_idx].length = length;
            lostdata_idx++;
        }
    }
   
    *tile_length = length;

    if (*state == TK_READ_OK) {
        tk_tile_databuf = (unsigned char *)malloc(sizeof(unsigned char) * length);
        if (tk_tile_databuf == NULL) {
            printf("%s %d: %s, memory alloc failed!\n", __FILE__, __LINE__, strerror(errno));
            *state = TK_READ_TILE_MISS;
            return *state;
        }
        if (-1 == fseek(fp, offset, SEEK_SET))
            LOG_ERR("Error while reading map's Data from File:#5!\n");
        if (fread(tk_tile_databuf, 1, length, fp) != length)
            LOG_ERR("Error while reading map's Data from File:#6!\n");
        tk_tile_databuflen = length;
        remain_bits = 0;
        remain_value = 0;
        buf_pos = 0;
    }
    LOG_DBG("rid is %10d, level is %d, tile offset is %10d, length is %10d, state is %10d\n", 
            region_id, cur_tile_level, offset, length, *state);
    return *state;
}

static unsigned int read_tile_databuf(unsigned char bits) 
{
    unsigned int res;
    if (bits > 32)
        return 0xffffffff;
    if (bits == 32) {
        return 0xffffffff;
    }
    if (bits <= remain_bits) {
        remain_bits -= bits;
        res = remain_value >> remain_bits;
        remain_value = (remain_value & ((1 << remain_bits) - 1));
        return res;
    } else {
        int temp = 0;
        temp = remain_value;
        bits -= remain_bits;
        while (bits >= 8) {
            temp = temp << 8;
            temp |= tk_tile_databuf[buf_pos];
            bits -= 8;
            remain_value = 0;
            remain_bits = 0;
            buf_pos++;
        }
        if (bits != 0) {
            remain_bits = 8 - bits;
            remain_value = (unsigned char)(tk_tile_databuf[buf_pos] & ((1 << remain_bits) - 1));
            temp = (tk_tile_databuf[buf_pos] >> remain_bits) | (temp << bits);
            buf_pos++;
        }
        return temp;
    }
}

static unsigned short read_code_idx() 
{
    unsigned short i = 0;
    unsigned short huffnum = 0;
    unsigned int res = 0;
    unsigned short cur_bit;
    unsigned int cur_stop;
    unsigned short pre_bit = 0;
    unsigned int pre_stop = 0;
    unsigned char *code;
    int code_num;

    code = reg[cur_idx].code_read_con;
    code_num = reg[cur_idx].code_con_num;

    for (i = 0; i < code_num; i++) {
        cur_bit = code[i * TK_STOP_UNIT_SIZE];
        cur_stop = (code[i * TK_STOP_UNIT_SIZE + 1] << 16) + (code[i * TK_STOP_UNIT_SIZE + 2] << 8) 
                                                           + code[i * TK_STOP_UNIT_SIZE + 3];
        if (huffnum < cur_stop) {
            break;
        }
        huffnum = (huffnum << cur_bit) | (unsigned short)(read_tile_databuf((unsigned char)cur_bit));

        if (i != 0) {
            res += cur_stop - (pre_stop << pre_bit);
        }
        pre_bit = cur_bit;
        pre_stop = cur_stop;
    }
    res += huffnum - (pre_stop << pre_bit);
    return (unsigned short)res;
}

static short int read_code(int rid) 
{
    unsigned short translated_code;
    unsigned char *levels;

    levels = reg[cur_idx].code_levels;
    translated_code = read_code_idx();
    if (tk_engine.current_z < (tk_engine.bl - levels[translated_code * 3 + 2])) {
        return TK_CODE_NOT_DISPLAY;
    }
    return (levels[translated_code * 3 + 0] << 8)
          +(levels[translated_code * 3 + 1]);
}

static void bit_read_string(unsigned int length, char *res) {
    unsigned int i;
    if (length == 0) {
        return;
    }
    if (buf_pos + length < tk_tile_databuflen) {
        res[0] =  ((unsigned char)(remain_value << (8 - remain_bits))) 
                | (tk_tile_databuf[buf_pos] >> (remain_bits));
        for (i = 1; i < length; i++) {
            res[i] =  (unsigned char)((((unsigned short)tk_tile_databuf[buf_pos + i - 1]) << (8 - remain_bits))
                    | (tk_tile_databuf[buf_pos + i] >> (remain_bits)));
        }
        buf_pos += length;
        remain_value = (unsigned char)(tk_tile_databuf[buf_pos - 1]) 
            & ((1 << remain_bits) -1);
    }
    return;
}
// must = 1 if the point is the first or last
static int read_point(struct tk_point *res, int must)
{
    int i = 0;
    unsigned int dummy;

    i = read_tile_databuf(2);
    if (i == 1 || must)
        res->levelCode = i;
    else {// 当 i > 0 时，如果当前级别与base_level的差别比i大，(如，16，i=1,则z=15,14都满足条件；
        //且 i == 0 且当前级别小于base_level，
        if (((tk_engine.bl - tk_engine.current_z) > (i - 1) && (i > 0)) || ((tk_engine.bl
                        > tk_engine.current_z) && (i == 0)))
            i = -1;
        else
            res->levelCode = i;
    }

    if (i != -1) {
        if (i != 1) {
            dummy = read_tile_databuf((unsigned char)((TILE_SIZE_BITS + cur_tile_level) << 1));
            res->x = dummy >> (TILE_SIZE_BITS + cur_tile_level);
            res->y = dummy & ((1 << (TILE_SIZE_BITS + cur_tile_level)) -1);
            if (i == 3) {
                dummy = ((1 << (TILE_SIZE_BITS + cur_tile_level)) -1);
                if (res->x == (int)dummy && ((res->y == 0)||(res->y == (int)dummy)))
                    res->x++;
                if (res->y == (int)dummy && ((res->x == 0)||(res->x == (int)dummy + 1)))
                    res->y++;
            }
        } else {//i == 1
            int coord;
            int in_which_edge;
            int up_bound = (1 << ((unsigned char)(TILE_SIZE_BITS + cur_tile_level)));
            dummy = read_tile_databuf((unsigned char)(TILE_SIZE_BITS + cur_tile_level + 2));
            in_which_edge = dummy & 0x3;
            coord = dummy >> 2;

            switch (in_which_edge) {
                case 0:
                    res->x = 0;
                    res->y = coord;
                    break;
                case 1:
                    res->x = coord;
                    res->y = up_bound;
                    break;
                case 2:
                    res->x = up_bound;
                    res->y = coord;
                    break;
                case 3:
                    res->x = coord;
                    res->y = 0;
                    break;
                default:
                    LOG_ERR("Wrong dim!!");
                    break;
            }
        }
        return 1;
    } else {//i == -1
            unsigned int x, y;
            x = read_tile_databuf((unsigned char)(TILE_SIZE_BITS + cur_tile_level));
            y = read_tile_databuf((unsigned char)(TILE_SIZE_BITS + cur_tile_level));
            //read_tile_databuf((unsigned char)((TILE_SIZE_BITS + cur_tile_level) << 1));
            return 0;
    }
}

static int read_feature(int rid, int* size, struct feature *this, int point_tile, int namelen_tile)
{
    int i;
    int need_read;
    int point_num = 0;
    int code;

    if (this->point_nums == 1) {
        i = 0;
    }
    code = read_code(rid);
    if (code != TK_CODE_NOT_DISPLAY) {
        if (code >= tk_engine.ps->layer_num) {
            code = tk_engine.ps->layer_num - 1;
        }
        need_read = 1;
        this->name_index = -1;
        this->name_length = -1;
        this->type = code;
        this->label_priority = 0;
        if (this->type == 4 && tk_engine.current_z >= 10 && !is_old_data){ 
            this->type = 3;
        }
        if (read_tile_databuf(1) == 1) {
            this->name_length = (short)read_tile_databuf(8);
            if (this->name_length < 0 || this->name_length > namelen_tile) {
                return TK_READ_FT_NAMELEN_ERR;
            }
            this->name = cur_mem_pool->tk_map_names + cur_mem_pool->cur_name;
            bit_read_string(this->name_length, this->name);
            cur_mem_pool->cur_name += this->name_length;
            this->name_index = TK_NAME_NEW;
             
            /*if (this->type == 39 || this->type == 38) {
                char ss[100];
                memset(ss,0,100);
                strncpy(ss,this->name,this->name_length);
                printf("%s------%d\n",ss, this->type);
                if (this->name[this->name_length - 1] == 248) {

                }
            }
            if(this->type >= 3 && this->type <= 6 && tk_engine.current_z >= 10 && !is_old_data) {
                if ((unsigned char)this->name[this->name_length - 1] == 197 &&
                       (unsigned char)this->name[this->name_length - 2] == 199) {
                    if (this->type == 3)
                        this->type = this->type + 38;
                    else
                        this->type = this->type + 37;
                }
            }*/

        } else {
            this->name_index = -1;
        }
            
        if (tk_engine.ps->feature_type[this->type] != 0) {
            this->point_nums = read_tile_databuf(TK_POINT_NUMBER);
            if (this->point_nums == 0xff) 
                this->point_nums += (short int)read_tile_databuf(16);
        }
        else {
            this->point_nums = 1;
        }
        if (this->point_nums > point_tile || this->point_nums < 0) {
            return TK_READ_FT_POINTNUM_ERR;
        }
        this->points = cur_mem_pool->tk_points + cur_mem_pool->cur_point;
        /* this->type is layer number 0-40 */
     
        if (cur_mem_pool->cur_point >= TK_TILE_BUFF_POINT) {
            //LOG_ERR("POINT Exceed In Tile!! %d %d\n",
              //          cur_mem_pool->cur_point, cur_mem_pool->tk_points
                //        + cur_mem_pool->cur_point);
        }

        for (i = 0; i < this->point_nums; i++) {
            if (i == 0 || i == this->point_nums - 1) {
                read_point((this->points) + point_num, 1);
                if (i == 0 ){
                    this->left_top = this->points[0];
                    this->right_bottom = this->points[0];
                } 
                else {
                    this->left_top.x = TK_MIN(this->left_top.x, this->points[point_num].x);
                    this->left_top.y = TK_MIN(this->left_top.y, this->points[point_num].y);
                    this->right_bottom.x = TK_MAX(this->right_bottom.x, this->points[point_num].x);
                    this->right_bottom.y = TK_MAX(this->right_bottom.y, this->points[point_num].y);
                }
                point_num++;
            } else {
                if (read_point((this->points) + point_num, 0)){
                    this->left_top.x = TK_MIN(this->left_top.x, this->points[point_num].x);
                    this->left_top.y = TK_MIN(this->left_top.y, this->points[point_num].y);
                    this->right_bottom.x = TK_MAX(this->right_bottom.x, this->points[point_num].x);
                    this->right_bottom.y = TK_MAX(this->right_bottom.y, this->points[point_num].y);
                    point_num++;
                }
            }
        }
        this->point_nums = point_num;
        // TK_TRACE_DEBUG(("FEATURE ENV lt: %d %d rb: %d %d", this->left_top.x, this->left_top.y
        // , this->right_bottom.x, this->right_bottom.y));
        cur_mem_pool->cur_point += this->point_nums;
        //   TK_TRACE_DEBUG(("The type %d and point number %d", this->type,this->point_nums ));
        need_read = 0;
    } else {
        need_read = 1;
    }
    *size = (*size) + 1;
    return need_read;
}

static struct tile* get_tile_data_struct(int fnum, int point_num, int name_num)
{
    struct tile* res = xcalloc(1, sizeof(struct tile));
    res->features = xcalloc(fnum, sizeof(struct feature));
    res->mem_pool.tk_map_names = xmalloc(sizeof(char) * name_num);
    res->mem_pool.tk_points = xmalloc(sizeof(struct tk_point) * point_num);
    res->mem_pool.cur_name = 0;
    res->mem_pool.cur_point = 0;
    //add
    res->fnum = 0;
    res->header_features = NULL;
    return res;
}

#define DATA_ERROR_NUMBER 100000
static int read_tile(int x, int y, short *state, int region_id, struct tile **tl_res) 
{
    struct tile *res;
    int size;
    int len;
    int name_num;
    int point_num;
    int tile_length;

    struct feature *ftail_clipped = NULL;
    int lvl_dif = tk_engine.bl - tk_engine.current_z;
    if (seek_tile(x, y, state, region_id, &tile_length) != TK_READ_OK) {
        *tl_res = NULL;
        return TK_READ_OK;//TK_READ_TILE_MISS;
    }
    if (lvl_dif == 2) {
        len = (tk_tile_databuf[buf_pos] << 8) + tk_tile_databuf[buf_pos + 1];
        name_num = (tk_tile_databuf[buf_pos + 2] << 4) + ((tk_tile_databuf[buf_pos + 3] >> 4) & 0x0f);
        point_num = ((tk_tile_databuf[buf_pos + 3] & 0xf) << 8) + tk_tile_databuf[buf_pos + 4];
    } else {
        if (lvl_dif == 1) {
            buf_pos = 5;
            len = (tk_tile_databuf[buf_pos] << 8) + tk_tile_databuf[buf_pos + 1];
            name_num = (tk_tile_databuf[buf_pos + 2] << 4) + ((tk_tile_databuf[buf_pos + 3] >> 4) & 0x0f);
            point_num = ((tk_tile_databuf[buf_pos + 3] & 0xf) << 8) + tk_tile_databuf[buf_pos + 4];
        } else {
            buf_pos = 10;
            len = (tk_tile_databuf[buf_pos] << 8) + tk_tile_databuf[buf_pos + 1];
            name_num = (tk_tile_databuf[buf_pos + 2] << 4) + ((tk_tile_databuf[buf_pos + 3] >> 4) & 0x0f);
            point_num = ((tk_tile_databuf[buf_pos + 3] & 0xf) << 8) + tk_tile_databuf[buf_pos + 4];
        }
    }
    buf_pos = 15;
    LOG_DBG("read tile len = %d, name_num = %d, point_num = %d\n", len, name_num, point_num);
    if (len <= 0) {
        if (tk_tile_databuf != NULL) {
            free(tk_tile_databuf);
            tk_tile_databuf = NULL;
        }
        *tl_res = NULL;
        return TK_READ_OK;// TK_READ_TILE_LENGTH_ERR;
    }
    res = get_tile_data_struct(len + 1, point_num + 1, name_num + 1);
    if (res->fnum <= 0 && len > DATA_ERROR_NUMBER) {
        if (tk_tile_databuf != NULL) {
            free(tk_tile_databuf);
            tk_tile_databuf = NULL;
        }
        return TK_READ_TILE_FTNUM_ERR;
    }
    if (name_num < 0 || name_num > DATA_ERROR_NUMBER) {
        if (tk_tile_databuf != NULL) {
            free(tk_tile_databuf);
            tk_tile_databuf = NULL;
        }
        return TK_READ_TILE_NAMENUM_ERR;
    }

    if (point_num < 0 || point_num > DATA_ERROR_NUMBER) {
        if (tk_tile_databuf != NULL) {
            free(tk_tile_databuf);
            tk_tile_databuf = NULL;
        }
        return TK_READ_TILE_POINTNUM_ERR;
    }
    res->region_id = region_id;
    res->length = tile_length;
    cur_mem_pool = &res->mem_pool;
    size = 0;
    while (size < len) {
        int ft_flag = read_feature(region_id, &size, res->features + (res->fnum), point_num, name_num);
        //printf("flag == %d\n", ft_flag);
        if (ft_flag > 0) {
            if (tk_tile_databuf != NULL) {
                free(tk_tile_databuf);
                tk_tile_databuf = NULL;
            }
            return ft_flag;

        }
        if (ft_flag == 0) {
            struct feature *cur_ft = res->features + (res->fnum);
            struct feature *pre_ft = res->features + (res->fnum - 1);
            if(pre_ft->name_index != -1 && cur_ft->name_index == -1 && res->fnum != 0){
                  //cur_ft->name_index = TK_NAME_NEW;
                  //cur_ft->name = pre_ft->name;
                  //cur_ft->name_length = pre_ft->name_length;  
            }

            cur_ft->tile = res;
            if ((cur_ft->points[0].levelCode == 1 || cur_ft->points[cur_ft->point_nums - 1].levelCode == 1)
                    && (tk_engine.ps->feature_type[cur_ft->type] != 0)
                    && (tk_engine.ps->feature_type[cur_ft->type] != 4)){
                    //&& cur_ft->type <= 40){
                if (res->header_features == NULL){
                    res->header_features = cur_ft;
                    ftail_clipped = cur_ft;
                } else {
                    ftail_clipped->header_next = cur_ft;
                    ftail_clipped = cur_ft;
                }
            }

            res->overall_point_num += res->features[res->fnum].point_nums;
            if (res->features[res->fnum].name_index >= 0)
                res->overall_name_len += res->features[res->fnum].name_length;
            res->fnum++;
        } else {
            break;
        }

        if (res->fnum >= TK_FEATURE_MAX_TILE - 1) {
            //LOG_ERR("FEATURE Exceed In Tile!! %d %d\n",
              //       res->fnum, res->features + (res->fnum));
        }
    }
    /* cur_tile_level =  tile 合并次数 */
    res->coder_lat = y - ((y - ref_tile_y) % (1 << cur_tile_level));
    res->coder_lon = x - ((x - ref_tile_x) % (1 << cur_tile_level));
    res->region_id = region_id;
    res->level = cur_tile_level;
    LOG_DBG("ref_tile_x=%d, ref_tile_y=%d, x=%d, y=%d, level=%d\n",
            ref_tile_x, ref_tile_y, x, y, cur_tile_level);
    res->need_move_bits = lvl_dif;
    if (lvl_dif >= 0){
        res->bias_x = (res->coder_lon) << (8 - lvl_dif);
        res->bias_y = (res->coder_lat) << (8 - lvl_dif);
    } else {
        res->bias_x = (res->coder_lon) << 8 << (-lvl_dif);
        res->bias_y = (res->coder_lat) << 8 << (-lvl_dif);
    }
    LOG_DBG("tile bias{x=%d, y=%d\n}", res->bias_x, res->bias_y);
    res->bbox.bottom = res->coder_lat;
    res->bbox.left = res->coder_lon;
    res->bbox.top = res->coder_lat + (1 << res->level);
    res->bbox.right = res->coder_lon + (1 << res->level);
    LOG_DBG("tile bbox{bottom=%d, left=%d, top=%d, right=%d}\n", 
            res->bbox.bottom, res->bbox.left, res->bbox.top, res->bbox.right);
    if (tk_tile_databuf != NULL) {
        free(tk_tile_databuf);
        tk_tile_databuf = NULL;
    }
    cur_mem_pool = NULL;
    *tl_res = res;
    return TK_READ_OK;
}

static void tile_delete_connect(struct tile* temp)
{
    struct feature *cur_ft = temp->header_features;
    while(cur_ft) {
        if (cur_ft->next) {
            cur_ft->next->previous = NULL;
        }
        if (cur_ft->previous) {
            cur_ft->previous->next = NULL;
        }
        cur_ft->tile = NULL;
        cur_ft = cur_ft->header_next;
    }
    return;
}

static void update_tile_ref(int rid) 
{
    int res=0;
    unsigned char *cur_pointer;
    unsigned char *index;

    index = reg[cur_idx].index;
    if (tk_engine.bl == TK_BASE_LEVEL_A || tk_engine.bl == TK_NATIONAL_LEVEL_A) {
        cur_pointer = index + res * 6;
        ref_tile_x = (cur_pointer[0]<<16) | (cur_pointer[1]<<8) | cur_pointer[2];
        ref_tile_y = (cur_pointer[3]<<16) | (cur_pointer[4]<<8) | cur_pointer[5];
        return;
    }
    cur_pointer = index + res * 6;
    res += (cur_pointer[9]<<16) | (cur_pointer[10]<<8) | cur_pointer[11];
    res += 2;

    if (tk_engine.bl == TK_BASE_LEVEL_B || tk_engine.bl == TK_NATIONAL_LEVEL_B) {
        cur_pointer = index + res * 6;
        ref_tile_x = (cur_pointer[0]<<16) | (cur_pointer[1]<<8) | cur_pointer[2];
        ref_tile_y = (cur_pointer[3]<<16) | (cur_pointer[4]<<8) | cur_pointer[5];
        return;
    }
    cur_pointer = index + res * 6;
    res += (cur_pointer[9]<<16) | (cur_pointer[10]<<8) | cur_pointer[11];
    res += 2;

    if (tk_engine.bl == TK_BASE_LEVEL_C || tk_engine.bl == TK_NATIONAL_LEVEL_C) {
        cur_pointer = index + res * 6;
        ref_tile_x = (cur_pointer[0]<<16) | (cur_pointer[1]<<8) | cur_pointer[2];
        ref_tile_y = (cur_pointer[3]<<16) | (cur_pointer[4]<<8) | cur_pointer[5];
        return;
    }
}


static void connect_two_ft(struct feature* f1, struct feature* f2) 
{
    struct tk_point *start, *end;

    if (f1->next == NULL && f2->previous == NULL) {
        start = f1->points + (f1->point_nums - 1);
        end = f2->points;
        if (start->levelCode == 1 && end->levelCode == 1) {
            if ((start->x + ((f1->tile->coder_lon) << TILE_SIZE_BITS)) == (end->x + ((f2->tile->coder_lon) << TILE_SIZE_BITS))
                    && (start->y + ((f1->tile->coder_lat) << TILE_SIZE_BITS)) == (end->y + ((f2->tile->coder_lat) << TILE_SIZE_BITS))){
                f1->next = f2;
                f2->previous = f1;
                return;
            }
        }
    }
    if (f2->next == NULL && f1->previous == NULL) {
        start = f2->points + (f2->point_nums - 1);
        end = f1->points;
        if (start->levelCode == 1 && end->levelCode == 1) {
            if ((start->x + ((f2->tile->coder_lon) << TILE_SIZE_BITS)) == (end->x + ((f1->tile->coder_lon) << TILE_SIZE_BITS))
                    && (start->y + ((f2->tile->coder_lat) << TILE_SIZE_BITS)) == (end->y + ((f1->tile->coder_lat) << TILE_SIZE_BITS))) {
                f2->next = f1;
                f1->previous = f2;
                return;
            }
        }
    }
}


// 判断两个Tile 是否邻接(指有重叠,或边界一样）
#define IS_ADJACENT(tile1, tile2) (!((tile1->bbox.left >= tile2->bbox.right || tile2->bbox.left >= tile1->bbox.right) && \
                                     (tile1->bbox.bottom >= tile2->bbox.top || tile2->bbox.bottom >= tile1->bbox.top)))

/*对active tile 来说，应该对相邻tile进行连接
 * current_tile: the current tile which is active
 */
static void _active_tile_connect()
{
    struct feature* cur_feature;
    struct feature* tar_feature;
    struct tile *current_tile = tk_engine.tb;
    struct tile *tar_tile = current_tile;
//下面通过循环遍历所有该tile中的可以进行连接的features，以及与该Tile相邻接的tile进行连接
    while(tar_tile) {
        if (IS_ADJACENT(tar_tile, current_tile)) {
            cur_feature = current_tile->header_features;
            tar_feature = tar_tile->header_features;
            while(tar_feature) {
                while(cur_feature) {
              //判断是否应当连接，并且连接需要连接的feature
                      if ((cur_feature->type == tar_feature->type) && \
                            (cur_feature->name_length == tar_feature->name_length) && \
                            (cur_feature->name_length != -1)) {
                        if(0 == strncmp(tar_feature->name,cur_feature->name,cur_feature->name_length)){
                            if(cur_feature != tar_feature)
                                connect_two_ft(cur_feature, tar_feature);
                            }
                    }
                    cur_feature = cur_feature->header_next;
                }
                cur_feature = current_tile->header_features;
                tar_feature = tar_feature->header_next;
            }
        }
        tar_tile = tar_tile->next;
    }
}

/* add tile to the tile buffer's head
 * if the tile buffer's memory is more than 100k, than remove from the link
 * list's tail to free 20k memory*/
static void add_tile(struct tile *tile)
{
    tk_engine.tile_num++;
    tile->next = tk_engine.tb;
    tk_engine.tb = tile;
    tk_engine.tb_size += tile->length;
    tk_engine.tilebuf_length++;

    if (tk_engine.tb_size > MAX_TILE_BUFFER_SIZE) {
        int tmpsize = 0;
        int newsize = MAX_TILE_BUFFER_SIZE * 4 / 5;
        struct tile *prev_tile, *cur_tile;
        cur_tile = tk_engine.tb;
        do {
            tmpsize += cur_tile->length;    
            prev_tile = cur_tile;
            cur_tile = cur_tile->next;
        } while (tmpsize < newsize);
        prev_tile->next = NULL;
        while (cur_tile) {
            prev_tile = cur_tile;
            cur_tile = cur_tile->next; 
            tk_engine.tb_size -= prev_tile->length; 
            /* TODO:delete connect */
            tile_delete_connect(prev_tile);
            release_tile_struct(prev_tile);
            tk_engine.tilebuf_length--;
        }
    }
    /*TODO: connect*/
    _active_tile_connect();
}

/* move the current tile to the tile buffer's(link list's) header */
static int find_in_list(int x, int y, int rid) 
{
    int i = 0;
    int res = -1;

    struct tile *temp = tk_engine.tb;
    struct tile *prev_tile; 
    while (temp) {
        if ((((x - temp->coder_lon) >> temp->level) == 0) 
                && (((y - temp->coder_lat) >> temp->level) == 0)
                && ((x - temp->coder_lon) >= 0)
                && ((y - temp->coder_lat) >= 0) 
                && (rid == temp->region_id)) {

            res = i;
            if (i >= tk_engine.tile_num) {//else then the tile is just added 
                if (i > 0) {// when i == 0, no need to change order
                    prev_tile->next = temp->next;
                    temp->next = tk_engine.tb;
                    tk_engine.tb = temp;
                }
                tk_engine.tile_num++;
                temp->flag = tk_engine.tile_flag;
            }
            break;
        }
        prev_tile = temp;
        temp = temp->next;
        i++;
    }
    return res;
}

static int set_cur_region(int rid)
{
    if ((cur_idx = load_reg(rid)) >= 0) {
        return 0;
    } else if (cur_idx == FILE_NO_EXIST) {
        if (lostdata_idx < TK_LOST_DATA_PIECE) { 
            lostdata[lostdata_idx].rid = rid;
            lostdata[lostdata_idx].offset = 0;
            lostdata[lostdata_idx].length = 0;
            lostdata_idx++;
        }
    } 
    return -1;
}

/* el : tile's envelope */
int  increase_tile(struct envelope *bbox, int add_tile_bound)
{
    int cir_id = 0;
    int x, y;
    int index;
    unsigned char ver[6 + 1];
    //heqingkun add 2012-03-13
    //int add_tile_bound = 0;
    tk_engine.tile_flag ++;
    tk_engine.tile_num = 0;
    int main_verno, sub_verno;

#ifdef LOG_LEVEL_DEBUG
    int read_tile_num = 0;
#endif
    /*if (tk_engine.bl_dif > 0)
        add_tile_bound = 1 << tk_engine.bl_dif;
    else
        add_tile_bound = 1 << 0;*/
    for (cir_id = 0; cir_id < regnum_in_bound; cir_id++) {
        if ((set_cur_region(reg_in_bound[cir_id])) == 0) { /* data file exist */
            update_tile_ref(cir_id);
            memset(ver, 0, 6 + 1);
            if (tk_get_region_version(reg_in_bound[cir_id],ver) != -1) {
                main_verno = ver[0];
                sub_verno = ver[1];
            }
            for (x = bbox->left - 1 * add_tile_bound; x <= bbox->right + 1 * add_tile_bound; x++) {
                for (y = bbox->bottom - 1 * add_tile_bound; y <= bbox->top + 1 * add_tile_bound; y++) {
                    index = find_in_list(x, y, reg_in_bound[cir_id]);
                    if (index < 0) {
                        short state = 0;
                        struct tile *tempt;
                        int flag;
                        flag = read_tile(x, y, &state, reg_in_bound[cir_id], &tempt);
                        if (flag == TK_READ_OK) {
                            if (tempt != NULL) {
                                tempt->is_active = 1;
                                tempt->flag = tk_engine.tile_flag;
                                add_tile(tempt);
#ifdef LOG_LEVEL_DEBUG
                                read_tile_num++;
#endif
                            }
                        } else {
                            update_tile(x, y, reg_in_bound[cir_id]);
                            return  flag;
                        }

                    }
                }
            }
        }
    }
    return 0;
#ifdef LOG_LEVEL_DEBUG
    struct tk_point pos;
    struct tk_latlon posT;
    pos.x = 180;
    pos.y = 295;
    tk_scr2latlon(&posT, &pos);
    LOG_DBG("@@@current tile_num is %d, read %d, total %d, size %d, flag %d, zoom_level %d, x=%f, y=%f\n", tk_engine.tile_num, 
            read_tile_num, tk_engine.tilebuf_length, tk_engine.tb_size, tk_engine.tile_flag, tk_engine.current_z, posT.lat, posT.lon);
#endif
}

/*
 * in fact : count the tile bound in the screen 
 * example level 14 15 16 
 * 16 : tile_size = 256
 * 15 : tile_size = 128
 * 14 : tile_size = 64
 * if level > 16 clip tile : tile < tk_engine.bl_dif
 */
void get_tile_bbox_from_point(struct envelope *bbox) 
{
    //if (tk_engine.current_z <= 16) {
        bbox->bottom = (tk_engine.cur_bbox.bottom) >> (TILE_SIZE_BITS + TK_BASE_LEVEL_A - tk_engine.bl);
        bbox->top = (tk_engine.cur_bbox.top) >> (TILE_SIZE_BITS + TK_BASE_LEVEL_A - tk_engine.bl);
        bbox->left = (tk_engine.cur_bbox.left) >> (TILE_SIZE_BITS + TK_BASE_LEVEL_A - tk_engine.bl);
        bbox->right = (tk_engine.cur_bbox.right) >> (TILE_SIZE_BITS + TK_BASE_LEVEL_A - tk_engine.bl);
    /* } else {
        bbox->bottom = (tk_engine.cur_bbox.bottom) >> (TILE_SIZE_BITS + tk_engine.bl_dif);
        bbox->top = (tk_engine.cur_bbox.top) >> (TILE_SIZE_BITS + tk_engine.bl_dif);
        bbox->left = (tk_engine.cur_bbox.left) >> (TILE_SIZE_BITS +  tk_engine.bl_dif);
        bbox->right = (tk_engine.cur_bbox.right) >> (TILE_SIZE_BITS + tk_engine.bl_dif);
    }*/
}
/* convert 10 12 14 15 level to 11 13 16 level tile_bound*/

void get_tile_base_level_bound(int x, int y, int bl_dif, struct envelope *box) {
    if (bl_dif >= 0) {
        box->left = x << bl_dif;
        box->bottom = y << bl_dif;
        box->right = box->left + (1 << bl_dif) - 1;
        box->top = box->bottom + (1 << bl_dif) - 1;
    } else {
        box->left = x >> (-bl_dif);
        box->bottom = y >> (-bl_dif);
        box->right = box->left;
        box->top = box->bottom;
    }
}

int get_tile_by_xy(int x, int y, int add_tile_bound) {
    struct envelope tile_bbox;
    get_tile_base_level_bound(x, y, tk_engine.bl_dif, &tile_bbox);
    //if (tk_engine.bl_dif > 0)
      //  add_tile_bound = 1 << tk_engine.bl_dif;
    //else
      //  add_tile_bound = 1 << 0;
    return increase_tile(&tile_bbox, add_tile_bound);
}

static void update_cur_buffer()
{
    struct envelope tile_bbox;

    memcpy(&tile_bbox, &tk_engine.cur_bbox, sizeof(struct envelope));
    get_tile_bbox_from_point(&tile_bbox);
    //increase_tile(&tile_bbox);
}
void calculate_box(int w, int h)
{
    int zoom;

	tk_gdi.rtview.left = 0;
    tk_gdi.rtview.right = w - 1;
    tk_gdi.rtview.top = 0;
    tk_gdi.rtview.bottom = h - 1;

    tk_engine.old_bbox.left = tk_engine.cur_bbox.left;
    tk_engine.old_bbox.right = tk_engine.cur_bbox.right;
    tk_engine.old_bbox.top = tk_engine.cur_bbox.top;
    tk_engine.old_bbox.bottom = tk_engine.cur_bbox.bottom;

    /* calculate the four corners' global coordination */
    zoom = 16 - tk_engine.current_z;
    tk_engine.cur_bbox.left = tk_engine.center_x - (((tk_gdi.rtview.right - tk_gdi.rtview.left) >> 1) << (zoom + 2) >> 2);
    tk_engine.cur_bbox.right = tk_engine.center_x + (((tk_gdi.rtview.right - tk_gdi.rtview.left) >> 1) << (zoom + 2) >> 2);
    tk_engine.cur_bbox.top = tk_engine.center_y + (((tk_gdi.rtview.bottom - tk_gdi.rtview.top) >> 1) << (zoom + 2) >> 2);
    tk_engine.cur_bbox.bottom = tk_engine.center_y - (((tk_gdi.rtview.bottom - tk_gdi.rtview.top) >> 1) << (zoom + 2) >> 2);
}

int get_base_level(int zoomlevel) {
    if (zoomlevel > TK_BASE_LEVEL_B) {
        tk_engine.bl_idx = 0;
        return TK_BASE_LEVEL_A;
    } else if (zoomlevel > TK_BASE_LEVEL_C) {
        tk_engine.bl_idx = 1;
        return TK_BASE_LEVEL_B;
    } else if (zoomlevel > TK_NATIONAL_LEVEL_A) {
        tk_engine.bl_idx = 2;
        return TK_BASE_LEVEL_C;
    } else if (zoomlevel > TK_NATIONAL_LEVEL_B) {
        tk_engine.bl_idx = 0;
        return TK_NATIONAL_LEVEL_A;
    } else if (zoomlevel > TK_NATIONAL_LEVEL_C) {
        tk_engine.bl_idx = 1;
        return TK_NATIONAL_LEVEL_B;
    } else {
        tk_engine.bl_idx = 2;
        return TK_NATIONAL_LEVEL_C;
    }
}

void clean_tile(int zl) {
    // free the tile buffer when zoom level changed
    //if (tk_engine.current_z != zl) {
        release_tb();
        //tk_engine.tilebuf_length = 0;
        //tk_engine.tb_size = 0;
    //}
}

// TODO this is a common routine called by all functions that will change 
// the zoom level. Do the check of the level before call this function.
void set_zlevel(int zl)
{

    tk_engine.old_z = tk_engine.current_z;
    tk_engine.current_z = zl;

    tk_engine.bl_dif = tk_engine.bl - tk_engine.current_z;

    if (tk_engine.current_z > TK_NATIONAL_LEVEL_A) {
             tk_engine.ps = &(tk_engine.c_styles);
    } else {
             tk_engine.ps = &(tk_engine.n_styles);
    }
}

// TODO the two lines below should be called when the center is changed
//
// tk_engine.old_x = tk_engine.center_x;
//  tk_engine.old_y = tk_engine.center_y;

int find_regions_in_bbox(struct envelope *tile_box)
{
    int result_id = TK_REGION_ID_OUT_BOUND;
    if (tk_engine.current_z > TK_NATIONAL_LEVEL_A) {
        int i;
        struct tk_point center_p;
        int is_inpoly = 0;
        int is_intersect = 0;
        regnum_in_bound = 0;
		center_p.x = (tile_box->left + tile_box->right) >> 1;
        center_p.y = (tile_box->bottom + tile_box->top) >> 1;
        for (i = 0; i < reg_num; i++) {
            if (TK_ENV_NO_OVERLAP((*tile_box), reg_bounds[i].env) == 0) {
                is_inpoly = is_point_in_region(i, &center_p);
                if (is_inpoly) {
                    if (regnum_in_bound < TK_REGION_CACHED_MAX){ 
                        reg_in_bound[regnum_in_bound] = i;
                        regnum_in_bound++;
                    } else {
                        reg_in_bound[regnum_in_bound-1] = i;
                    }
                    if (result_id == TK_REGION_ID_OUT_BOUND) {
                        result_id = i;
                    }
                    continue;
                }

                is_intersect = is_rect_in_region(i, *tile_box);
                if (is_intersect) {
                    if (regnum_in_bound < TK_REGION_CACHED_MAX){
                        reg_in_bound[regnum_in_bound] = i;
                        regnum_in_bound++;
                    }
                    continue;
                }
            }
        }
    } else {
        regnum_in_bound = 1;
        reg_in_bound[0] = TK_REGION_ID_NATIONAL;//National
        result_id = TK_REGION_ID_NATIONAL;
    }
    return result_id;
}

void adjust_lostdata() 
{
    int i, j, l, r, now, tmp;
    struct tk_map_lostdata tmpdata;

    if (lostdata_idx > 0) {
        now = 0;
        l = 0;
        r = l;
        do {
            while ((r < (lostdata_idx - 1)) && (lostdata[r].rid == lostdata[r + 1].rid))
                r++;

            for(i = l; i < r; i++) {
                tmp = i;
                for (j = i + 1; j <= r; j++)
                    if (lostdata[tmp].offset > lostdata[j].offset)
                        tmp = j;
                if (tmp != i) {
                    tmpdata.rid = lostdata[i].rid;
                    tmpdata.offset = lostdata[i].offset;
                    tmpdata.length = lostdata[i].length;

                    lostdata[i].rid = lostdata[tmp].rid;
                    lostdata[i].offset = lostdata[tmp].offset;
                    lostdata[i].length = lostdata[tmp].length;

                    lostdata[tmp].rid = tmpdata.rid;
                    lostdata[tmp].offset = tmpdata.offset;
                    lostdata[tmp].length = tmpdata.length;
                }
            }
            l = r + 1;
            r = l;
        } while (r< lostdata_idx - 1);

        now = 0;
        for (i = 1; i < lostdata_idx; i++) {
            if (lostdata[i].rid == lostdata[now].rid) {
                if (lostdata[i].offset == lostdata[now].offset + lostdata[now].length) //相邻，需拼接）
                    lostdata[now].length += lostdata[i].length;
                else if (lostdata[i].offset > lostdata[now].offset + lostdata[now].length) {
                    now++;
                    if (now != i) {
                        lostdata[now].rid = lostdata[i].rid;
                        lostdata[now].offset = lostdata[i].offset;
                        lostdata[now].length = lostdata[i].length;
                    }
                }                    
            }
            else {
                now++;
                if (now != i) {
                    lostdata[now].rid = lostdata[i].rid;
                    lostdata[now].offset = lostdata[i].offset;
                    lostdata[now].length = lostdata[i].length;
                }
            }
        }
        lostdata_idx = now + 1;
    }
}
#ifdef LOG_LEVEL_DEBUG
    static int railfnum = 0;
#endif
static void _tile_syc_map(struct tile* current_tile)
{
    struct layer *cur_layer;
    int j;
    struct feature *cur_feature;
//需要绘制的feature仅包括所有连接前端不是activetile
//feature的,或前端无连接的active tile feature,
//以及上一屏已经绘制label的feature;
    for (j = 0; j < current_tile->fnum; j++) {
        cur_feature = current_tile->features + j;
        if (current_tile->is_active == 0
                && (cur_feature->name_index != TK_NAME_NEED_DISPLAY
                || (cur_feature->previous != NULL))) {
            continue;
        }
        //if (cur_feature->previous != NULL) {
        if ((cur_feature->previous != NULL) && (cur_feature->previous->tile->flag == tk_engine.tile_flag)) {
            struct feature* temp = cur_feature;
            while((temp->previous) && (temp->previous->tile->flag == tk_engine.tile_flag)) {
          //  while(temp->previous) {
                temp = temp->previous;
                if (temp->tile->is_active == 1)
                    break;
            }
            if (temp->tile->is_active == 1)
                continue;
        }
        cur_layer = &tk_engine.layer_list[cur_feature->type];
#ifdef LOG_LEVEL_DEBUG
        if (cur_feature->type == 8) 
            railfnum++;
#endif
        if (cur_layer->features == NULL) {
            cur_layer->ftail = cur_feature;
            cur_layer->features = cur_feature;
            cur_feature->layer_next = NULL;
        } else {
            cur_layer->ftail->layer_next = cur_feature;
            cur_layer->ftail = cur_feature;
            cur_feature->layer_next = NULL;
        }
    }
}

/* 将maploader中的各个layer的指针指向合适的feature,包括activetile中间的所有feature和非active中需要label的feature
 * */

int maploader_syc_from_buffer(void)
{
    int i;
    struct layer *temp_layer = NULL;
//首先置空所有的layer的features头指针
    for (i = 0; i < tk_engine.ps->layer_num; i++) {
        temp_layer = &tk_engine.layer_list[i];
        temp_layer->features = NULL;
        temp_layer->ftail = NULL;
    }
//对activetile进行连接, discarded, connecting is incremental
     /*for (i = 0; i < tk_engine.tl.length; i++) {
        struct tile *current_tile = arraylist_get(&tk_engine.tl, i);
        if (current_tile->is_active) { 
            _active_tile_connect(current_tile, i);
        }
    } */
//把所有的layer挂上各个tile的链表,对非active的,仅挂名字需要显示的
    struct tile *cur_tile = tk_engine.tb;
#ifdef LOG_LEVEL_DEBUG
    railfnum = 0;
#endif
    while(cur_tile) {
        if (tk_engine.tile_flag == cur_tile->flag)
            _tile_syc_map(cur_tile);
        cur_tile = cur_tile->next;
    }
    //LOG_DBG("rail feature num:%d\n%", railfnum);
    return 0;
}
/* 
 * convert  x,y to  16_level's x,y 
 * */
void get_tile_level_16_bound(struct envelope *s_el, struct envelope *d_el) {
    if (tk_engine.current_z <= 16) {
        d_el->left = s_el->left << (TK_BASE_LEVEL_A - tk_engine.current_z);
        d_el->right = s_el->right << (TK_BASE_LEVEL_A - tk_engine.current_z);
        d_el->bottom = s_el->bottom << (TK_BASE_LEVEL_A - tk_engine.current_z);
        d_el->top = s_el->top << (TK_BASE_LEVEL_A - tk_engine.current_z);
        memcpy(&(tk_engine.min_tile_bbox), d_el, sizeof(struct envelope));
    } else {
        memcpy(&(tk_engine.min_tile_bbox), s_el, sizeof(struct envelope));
        d_el->left = s_el->left >> (-TK_BASE_LEVEL_A + tk_engine.current_z);
        d_el->right = s_el->right >> (-TK_BASE_LEVEL_A + tk_engine.current_z);
        d_el->bottom = s_el->bottom >> (-TK_BASE_LEVEL_A + tk_engine.current_z);
        d_el->top = s_el->top >> (-TK_BASE_LEVEL_A + tk_engine.current_z);
    }
}
int load_tile(int x, int y, struct envelope *tile_box) {
    int center_id;
    int current_status = TK_MAPLOADER_OK;
    struct envelope el;
    /* add 2012-03-12 */
    int add_tile_x,add_tile_y;
    int add_tile_bound;
    int flag = 0;
    /*draw one tile ,add four other tile for drawing the line better
     * ...... ...... ......
     * ...1.. ...2.. ...3..
     * ...... ...... ......
     * ...4.. ..0... ...5..   
     * ...... ...... ......
     * ...... ...... ......
     * ..6... ...7.. ...8..
     * ...... ...... ......
     * end */

    LOG_DBG("process observer: in load_map\n");
    tk_geo_cleanmaplabel();
    get_tile_level_16_bound(tile_box, &el);
    //printf("el center %d---%d---%d--%d", tile_box->left, tile_box->right, tile_box->top, tile_box->bottom);
    //printf("el center %d---%d---%d--%d", el.left, el.right, el.top, el.bottom);
    center_id = find_regions_in_bbox(&el);
    if (center_id == -1) {
        current_status = TK_MAPLOADER_OUT_BOUND;
    }

    LOG_DBG("The num of region in bound is %d\n", regnum_in_bound);
    lostdata_idx = 0;
    memset(lostdata, 0, sizeof(struct tk_map_lostdata) * TK_LOST_DATA_PIECE);
    if (tk_engine.bl_dif > 0)
        add_tile_bound = 1 << tk_engine.bl_dif;
    else
        add_tile_bound = 1 << 0;
    flag = get_tile_by_xy(x, y, add_tile_bound);
    if (flag > 0) 
        return flag;
    /* get outher four tile info */
    /*get_tile_by_xy(x, y + 1);
    get_tile_by_xy(x, y - 1);
    get_tile_by_xy(x - 1, y);
    get_tile_by_xy(x + 1, y);
    /*end*/
    adjust_lostdata();
    maploader_syc_from_buffer();
    return current_status;
}

int load_map()
{
    int center_id;
    int current_status = TK_MAPLOADER_OK;

    LOG_DBG("process observer: in load_map\n");
    tk_geo_cleanmaplabel();
    center_id = 0;//find_regions_in_bbox(tile_box);
    if (center_id == -1) {
        current_status = TK_MAPLOADER_OUT_BOUND;
    }

    LOG_DBG("The num of region in bound is %d\n", regnum_in_bound);
    lostdata_idx = 0;
    memset(lostdata, 0, sizeof(struct tk_map_lostdata) * TK_LOST_DATA_PIECE); 
    update_cur_buffer();
    adjust_lostdata();
    maploader_syc_from_buffer();
    return current_status;
}
/*
 * 更新在该tile中屏幕框和裁剪框的坐标。之所以要在每个矢量数据块中记录此事，是出于对绘制时裁剪的高效要求。
 * clip_scr: clip screen，单位为屏幕坐标
 * draw_scr: draw screen, 单位为屏幕坐标
 */
void tk_update_buffer_screens(struct envelope clip_scr, struct envelope draw_scr) 
{
    struct tile *cur_tl;
    int tile_bias_x, tile_bias_y, lvl_dif;

    cur_tl = tk_engine.tb;
    while (cur_tl) {
        if (cur_tl->flag == tk_engine.tile_flag) {
                lvl_dif = cur_tl->need_move_bits;
                if (tk_engine.current_z <= 16) {
                    tile_bias_x = (short)(((cur_tl->coder_lon) << (8 - lvl_dif))
                                - (tk_engine.cur_bbox.left >> (16 - tk_engine.current_z)));
                    //当前比例尺下参考点的偏差
                    tile_bias_y = (short)(((cur_tl->coder_lat) << (8-lvl_dif))
                                - (tk_engine.cur_bbox.bottom >> (16 - tk_engine.current_z)));
                    //转换为16级坐标下的屏幕坐标
                    cur_tl->whole_screen.left = (draw_scr.left - tile_bias_x) << lvl_dif;
                    cur_tl->whole_screen.right = (draw_scr.right - tile_bias_x) << lvl_dif;
                    cur_tl->whole_screen.bottom = (draw_scr.bottom - tile_bias_y) << lvl_dif;
                    cur_tl->whole_screen.top = (draw_scr.top - tile_bias_y) << lvl_dif;

                    cur_tl->clip_screen.left = (clip_scr.left - tile_bias_x) << lvl_dif;
                    cur_tl->clip_screen.right = (clip_scr.right - tile_bias_x) << lvl_dif;
                    cur_tl->clip_screen.bottom = (clip_scr.bottom - tile_bias_y) << lvl_dif;
                    cur_tl->clip_screen.top = (clip_scr.top - tile_bias_y) << lvl_dif; 
                }
                else {
                    tile_bias_x = (short)(((cur_tl->coder_lon) << (8 - lvl_dif))
                            - (tk_engine.cur_bbox.left << (-(16 - tk_engine.current_z))));
                    tile_bias_y = (short)(((cur_tl->coder_lat) << (8 - lvl_dif))
                            - (tk_engine.cur_bbox.bottom << (-(16 - tk_engine.current_z))));
                    cur_tl->whole_screen.left = (draw_scr.left - tile_bias_x) >> (-lvl_dif);
                    cur_tl->whole_screen.right = (draw_scr.right - tile_bias_x) >> (-lvl_dif);
                    cur_tl->whole_screen.bottom = (draw_scr.bottom - tile_bias_y) >> (-lvl_dif);
                    cur_tl->whole_screen.top = (draw_scr.top - tile_bias_y) >> (-lvl_dif);

                    cur_tl->clip_screen.left = (clip_scr.left - tile_bias_x) >> (-lvl_dif);
                    cur_tl->clip_screen.right = (clip_scr.right - tile_bias_x) >> (-lvl_dif);
                    cur_tl->clip_screen.bottom = (clip_scr.bottom - tile_bias_y) >> (-lvl_dif);
                    cur_tl->clip_screen.top = (clip_scr.top - tile_bias_y) >> (-lvl_dif);         
                }
            }
        cur_tl = cur_tl->next;
    }
}

/* TODO: To be delete*/
struct style *pstyle;
int layindex;
static void draw_map(struct envelope filter, int is_file_not_found_scene)
{
	int i = 0;
    int j = 0;
	struct layer *pl = NULL;
    int layindex = 0;
    LOG_INFO("process observer: in draw_map\n");    
    /* Update each tile's clip_screen and whole_screen */
    tk_update_buffer_screens(tk_gdi.rtview, vm_screen);

    tk_set_filter_screen(filter);
	tk_gdi_cleancdata(filter, tk_engine.current_z, is_file_not_found_scene);
    if ( tk_engine.current_z >= 10) {
        for(i = 0; i < tk_engine.ps->layer_num - 3; i++){
        if (i == 0) {
                layindex = i + 38;
                pl = &tk_engine.layer_list[i + 38];
                pstyle = &(tk_engine.ps->styles[i + 38]);
        }
        if (i == 12) {
                layindex = i + 2;
                pl = &tk_engine.layer_list[i + 2];
                pstyle = &(tk_engine.ps->styles[i + 2]);
        }
        if (i > 0 && i <= 38 && i != 12) {
                if (i > 12 && i <= 15) {
                    layindex = i - 2;
                    pl = &tk_engine.layer_list[i - 2];
                    pstyle = &(tk_engine.ps->styles[i - 2]);
                } else {
                    layindex = i - 1;
                    pl = &tk_engine.layer_list[i - 1];
                    pstyle = &(tk_engine.ps->styles[i - 1]);
                }
        }
        if (i > 38) {
            pl = &tk_engine.layer_list[i];
            pstyle = &(tk_engine.ps->styles[i]);
        }
        //pl = &tk_engine.layer_list[i];
        //pstyle = &(tk_engine.ps->styles[i]);
        if (pl == NULL)
            continue;
        if (layer_ctl[layindex] == 1) {
            if (tk_engine.ps->feature_type[layindex] == TKGEO_ENMFTYPE_POINT) {
                ;//draw_point(pl, tk_engine.current_z);
            } else if (tk_engine.ps->feature_type[layindex] == TKGEO_ENMFTYPE_LINE) {
                draw_linelayer(pl, tk_engine.current_z);
            } else if (tk_engine.ps->feature_type[layindex] == TKGEO_ENMFTYPE_RAIL) {
                draw_rail(pl, tk_engine.current_z);
            } else if (tk_engine.ps->feature_type[layindex] == TKGEO_ENMFTYPE_ROAD) {
                draw_road(pl, tk_engine.current_z);
            } else if (tk_engine.ps->feature_type[layindex] == TKGEO_ENMFTYPE_POLY) {
               tk_geo_drawvmlpoly(pl, tk_engine.current_z);
            }
            }
        }
        return ;
    }
    if (tk_engine.current_z < 10) {
        for(i = 0; i < tk_engine.ps->layer_num; i++){
            pl = &tk_engine.layer_list[i];
            pstyle = &(tk_engine.ps->styles[i]);
        if (pl == NULL)
            continue;
        if (layer_ctl[i] == 1) {
            if (tk_engine.ps->feature_type[i] == TKGEO_ENMFTYPE_POINT) {
                ;//draw_point(pl, tk_engine.current_z);
            } else if (tk_engine.ps->feature_type[i] == TKGEO_ENMFTYPE_LINE) {
                draw_linelayer(pl, tk_engine.current_z);
            } else if (tk_engine.ps->feature_type[i] == TKGEO_ENMFTYPE_RAIL) {
                draw_rail(pl, tk_engine.current_z);
            } else if (tk_engine.ps->feature_type[i] == TKGEO_ENMFTYPE_ROAD) {
                draw_road(pl, tk_engine.current_z);
            } else if (tk_engine.ps->feature_type[i] == TKGEO_ENMFTYPE_POLY) {
               tk_geo_drawvmlpoly(pl, tk_engine.current_z);
            }
            }
        }
        return ;
    }

    for (i = -1; i < tk_engine.ps->layer_num - 2; i++) {
        layindex = i;
          /*if(i >= 3 && i <= 8){
            if (i == 3) {
                j = i + 38;
                pl = &tk_engine.layer_list[i + 38];
                pstyle = &(tk_engine.ps->styles[i + 38]);
            }
            if (i == 4) {
                j = i - 1;
                pl = &tk_engine.layer_list[i - 1];
                pstyle = &(tk_engine.ps->styles[i - 1]);
            }
            if (i == 5) {
                j = i + 37;
                pl = &tk_engine.layer_list[i + 37];
                pstyle = &(tk_engine.ps->styles[i + 37]);
            }
            if ( i == 7) {
                j = i - 2;
                pl = &tk_engine.layer_list[i - 2];
                pstyle = &(tk_engine.ps->styles[i - 2]);
            }
            if (i == 6) {
                j = i + 37;
                pl = &tk_engine.layer_list[i + 37];
                pstyle = &(tk_engine.ps->styles[i + 37]);
            }
            if (i == 8) {
                j = i - 2;
                pl = &tk_engine.layer_list[i - 2];
                pstyle = &(tk_engine.ps->styles[i - 2]);
            }
          } else if (i > 8){
                if (i == 39)
                    continue;
                j = i - 1;
                pl = &tk_engine.layer_list[i - 1];
                pstyle = &(tk_engine.ps->styles[i - 1]);
            } else {
                if (i == -1) {
                    j = 38;
                    pl = &tk_engine.layer_list[38];
                    pstyle = &(tk_engine.ps->styles[38]);
                } else {
                    j = i;
                    pl = &tk_engine.layer_list[i];
                    pstyle = &(tk_engine.ps->styles[i]);
                }
            }*/
        /*  if (i == 4)
            continue;
        if (i == 3 || i == 6 || i == 5) {  
            if (i == 3) {   
                pl = &tk_engine.layer_list[i + 38];
                pstyle = &(tk_engine.ps->styles[i + 38]);
            } else {
                pl = &tk_engine.layer_list[i + 37];
                pstyle = &(tk_engine.ps->styles[i + 37]);
            }
        }
        else if (i > 6) {
            if (i == 7) {
                pl = &tk_engine.layer_list[i - 4];
                pstyle = &(tk_engine.ps->styles[i - 4]);
            }
            else {
                pl = &tk_engine.layer_list[i - 3];
                pstyle = &(tk_engine.ps->styles[i - 3]);
            }

        } else {*/
         //   pl = &tk_engine.layer_list[i];
         //   pstyle = &(tk_engine.ps->styles[i]);
        //}

        if (pl == NULL)
            continue;
        if (i != 44) {
        if (layer_ctl[j] == 1) {
            if (tk_engine.ps->feature_type[j] == TKGEO_ENMFTYPE_POINT) {
                draw_point(pl, tk_engine.current_z);
            } else if (tk_engine.ps->feature_type[i] == TKGEO_ENMFTYPE_LINE) {
                draw_linelayer(pl, tk_engine.current_z);
            } else if (tk_engine.ps->feature_type[i] == TKGEO_ENMFTYPE_RAIL) {
                draw_rail(pl, tk_engine.current_z);
            } else if (tk_engine.ps->feature_type[i] == TKGEO_ENMFTYPE_ROAD) {
                draw_road(pl, tk_engine.current_z);
            } else if (tk_engine.ps->feature_type[i] == TKGEO_ENMFTYPE_POLY) {
               tk_geo_drawvmlpoly(pl, tk_engine.current_z);
            }
        }
        }
    }

    tk_reset_filter_screen();
}


static int tk_gdi_compute_reusable_area(struct envelope* keep, 
		const int x, const int y, const int z, 
		const int oldx, const int oldy, const int oldz){
    if (oldz!=z
			|| (TK_ABS(oldx - x) >= ((tk_engine.width << (2 + 16 - z)) >>2 ))
			|| (TK_ABS(oldy - y) >= ((tk_engine.height << (2 + 16 - z)) >>2 ))){
        keep->bottom = tk_engine.height-1;
        keep->top = 0;
        keep->left = 0;
        keep->right= tk_engine.width-1;
        return 0;
    } else {
        short bias_x;
        short bias_y;
        //short dz_base_a = z - TK_BASE_LEVEL_A;
        //short dx = x - oldx;
        //short dy = y - oldy;
        if (z<=16){
            bias_x=(TK_ABS(x-oldx))>>(16-z);
            bias_y=(TK_ABS(y-oldy))>>(16-z);
        } else {
            bias_x=(TK_ABS(x-oldx))<<(z-16);
            bias_y=(TK_ABS(y-oldy))<<(z-16);
        }
        
        if (x-oldx<0)
                bias_x=-bias_x;
        if (y-oldy<0)
                bias_y=-bias_y;
        

        if ((unsigned int)TK_ABS(bias_x*bias_y)>=tk_engine.width* tk_engine.height) 
            return 0;
        _tk_mem_blt_16((unsigned char *)tk_gdi.pd, (unsigned short)tk_engine.width, 
				bias_x,bias_y, tk_engine.height);
        keep->bottom=TK_MIN(tk_engine.height-1 ,tk_engine.height-1 -bias_y  );
        keep->top=TK_MAX(0 ,-bias_y );
        keep->left=TK_MAX(0 ,-bias_x );
        keep->right=TK_MIN(tk_engine.width-1 ,tk_engine.width-1 - bias_x);
        return 1;        
    }
}

static void fill_envelope_with_screen(struct envelope *env) 
{
	env->left = 0;
	env->right = tk_engine.width - 1;
	env->top = 0;
	env->bottom = tk_engine.height - 1;
}

static void set_tile_screen(struct envelope *env) {
	env->left = 0;
	env->right = 256 - 1;
	env->top = 0;
	env->bottom = 256 - 1;
}

int current_rid = 0;
int can_resuse_buffer = 0;

int draw_tile(int x, int y, struct envelope *el) {
    struct envelope filter;
    int flag = load_tile(x, y, el);
    if (flag > 0)
       return flag;

    current_rid = tk_get_current_city_id();

    set_tile_screen(&filter);
    draw_map(filter, 0);
    return 0;
}

void draw_whole_map()
{
	struct envelope keep, filter;
	//int can_resuse_buffer = 0;

    LOG_DBG("process observer: in draw_map\n");    
    if (l_opt == 0) {
        need_read_whole_map = 1; 
    }
    int need_map = load_map(tk_engine.cur_bbox);
    int is_file_not_found_scene = (need_map != TK_MAPLOADER_OK);   

    current_rid = tk_get_current_city_id();
	fill_envelope_with_screen(&filter);
	fill_envelope_with_screen(&tk_gdi.rtview);

	if (need_read_whole_map == 0) {
		can_resuse_buffer = tk_gdi_compute_reusable_area(
			&keep, tk_engine.center_x, tk_engine.center_y, tk_engine.current_z, 
			tk_engine.old_x, tk_engine.old_y, tk_engine.old_z);
	}
	
    tk_engine.old_x = tk_engine.center_x;
    tk_engine.old_y = tk_engine.center_y;
    tk_engine.old_z = tk_engine.current_z;
    memset(&tk_gdi.unuse_view,0,sizeof(struct envelope));

	if (!can_resuse_buffer || need_read_whole_map == 1) {
		draw_map(filter, is_file_not_found_scene);
		return;
	}
	
	if (keep.top == tk_gdi.rtview.top && keep.left == tk_gdi.rtview.left) {
		if (keep.right != tk_gdi.rtview.right) {
			tk_gdi.rtview.bottom = keep.bottom ;
			filter.bottom = keep.bottom ;	
			tk_gdi.rtview.left = TK_MAX(keep.right - TK_CLIP_REDUNDENT, 0);
			filter.left = TK_MAX(keep.right - TK_CLIP_REDUNDENT/2 + 1, 0); 

            tk_gdi.unuse_view.left = tk_gdi.rtview.left - TK_CLIP_CAIRO;
            tk_gdi.unuse_view.right = filter.left - 1;
            tk_gdi.unuse_view.bottom = filter.bottom;
            tk_gdi.unuse_view.top = filter.top;
			draw_map(filter, is_file_not_found_scene);
		} 

		fill_envelope_with_screen(&filter);
		fill_envelope_with_screen(&tk_gdi.rtview);
		tk_gdi.rtview.top = TK_MAX(keep.bottom -TK_CLIP_REDUNDENT , 0);
		filter.top = TK_MAX(keep.bottom -TK_CLIP_REDUNDENT/2 + 1, 0);

        tk_gdi.unuse_view.left = filter.left;
        tk_gdi.unuse_view.right = filter.right ;
        tk_gdi.unuse_view.bottom =  filter.top - 1;
        tk_gdi.unuse_view.top = tk_gdi.rtview.top - TK_CLIP_CAIRO;
		
		draw_map(filter, is_file_not_found_scene);
	} else if (keep.left == tk_gdi.rtview.left && keep.bottom == tk_gdi.rtview.bottom) {
		if (keep.right != tk_gdi.rtview.right) {
			tk_gdi.rtview.top = keep.top;
			filter.top = keep.top;			
            //tk_gdi.rtview.right += 1;
            //tk_gdi.rtview.bottom += 1;
			tk_gdi.rtview.left = TK_MAX(keep.right - TK_CLIP_REDUNDENT, 0);
			filter.left = TK_MAX(keep.right - TK_CLIP_REDUNDENT/2 + 1, 0); 
            tk_gdi.unuse_view.left = tk_gdi.rtview.left - TK_CLIP_CAIRO;
            tk_gdi.unuse_view.right = filter.left - 1;
            tk_gdi.unuse_view.bottom = filter.bottom ;
            tk_gdi.unuse_view.top = filter.top;
			
			draw_map(filter, is_file_not_found_scene);					
		} 
		
		fill_envelope_with_screen(&filter);
		fill_envelope_with_screen(&tk_gdi.rtview);
        //tk_gdi.rtview.right += 1;
		tk_gdi.rtview.bottom = TK_MIN(keep.top + TK_CLIP_REDUNDENT, tk_gdi.rtview.bottom);
		filter.bottom = TK_MIN(keep.top + TK_CLIP_REDUNDENT/2 - 1 , tk_gdi.rtview.bottom);
        tk_gdi.unuse_view.left = filter.left  ;
        tk_gdi.unuse_view.right = filter.right;
        tk_gdi.unuse_view.bottom =  tk_gdi.rtview.bottom + TK_CLIP_CAIRO;
        tk_gdi.unuse_view.top = filter.bottom + 1;
		
		draw_map(filter, is_file_not_found_scene);
        
	} else if (keep.top == tk_gdi.rtview.top && keep.right == tk_gdi.rtview.right) {
		if (keep.left != tk_gdi.rtview.left) {
			//tk_gdi.rtview.bottom = keep.bottom + 1;
			filter.bottom = keep.bottom;			
			tk_gdi.rtview.right = TK_MIN(keep.left + TK_CLIP_REDUNDENT, tk_gdi.rtview.right);
			filter.right = TK_MIN(keep.left + TK_CLIP_REDUNDENT/2 - 1, tk_gdi.rtview.right);
            tk_gdi.unuse_view.left = filter.right + 1;
            tk_gdi.unuse_view.right = tk_gdi.rtview.right + TK_CLIP_CAIRO;
            tk_gdi.unuse_view.bottom = filter.bottom ;
            tk_gdi.unuse_view.top = filter.top;
			//printf("%d--%d--%d--%d\n",tk_gdi.rtview.left,tk_gdi.rtview.right,tk_gdi.rtview.bottom,tk_gdi.rtview.top);


			draw_map(filter, is_file_not_found_scene);				

		}
		
		fill_envelope_with_screen(&filter);
		fill_envelope_with_screen(&tk_gdi.rtview);
		tk_gdi.rtview.top = TK_MAX(keep.bottom - TK_CLIP_REDUNDENT, 0);
		filter.top = TK_MAX(keep.bottom - TK_CLIP_REDUNDENT/2 + 1, 0);
            //tk_gdi.rtview.top -= 1;
            //tk_gdi.rtview.right += 1;
        tk_gdi.unuse_view.left = filter.left;
        tk_gdi.unuse_view.right = filter.right;
        tk_gdi.unuse_view.bottom =  filter.top - 1;
        tk_gdi.unuse_view.top = tk_gdi.rtview.top - TK_CLIP_CAIRO;
		
		draw_map(filter, is_file_not_found_scene);
	} else if (keep.bottom == tk_gdi.rtview.bottom && keep.right == tk_gdi.rtview.right) {
		if (keep.left != tk_gdi.rtview.left) {
			tk_gdi.rtview.top = keep.top;
			filter.top = keep.top;			
			tk_gdi.rtview.right = TK_MIN(keep.left + TK_CLIP_REDUNDENT, tk_gdi.rtview.right);
			filter.right = TK_MIN(keep.left + TK_CLIP_REDUNDENT/2 - 1, tk_gdi.rtview.right);
            tk_gdi.unuse_view.left = filter.right + 1;
            tk_gdi.unuse_view.right = tk_gdi.rtview.right + TK_CLIP_CAIRO;
            tk_gdi.unuse_view.bottom = filter.bottom ;
            tk_gdi.unuse_view.top = filter.top;
			
			draw_map(filter, is_file_not_found_scene);
		}
		
		fill_envelope_with_screen(&filter);
		fill_envelope_with_screen(&tk_gdi.rtview);
		tk_gdi.rtview.bottom = TK_MIN(keep.top + TK_CLIP_REDUNDENT, tk_gdi.rtview.bottom);
		filter.bottom = TK_MIN(keep.top + TK_CLIP_REDUNDENT/2 - 1 , tk_gdi.rtview.bottom);
        tk_gdi.unuse_view.left = filter.left;
        tk_gdi.unuse_view.right = filter.right;
        tk_gdi.unuse_view.bottom =  tk_gdi.rtview.bottom + TK_CLIP_CAIRO;
        tk_gdi.unuse_view.top = filter.bottom + 1;
		
		draw_map(filter, is_file_not_found_scene);			
	}
}

FILE* find_fp(int rid)
{
    int idx;

    if ((idx = find_reg(rid)) >= 0)
        return reg[idx].fp;
    else
        return NULL;
}

int read_chk_file(char *filename, unsigned char **pverifycode, unsigned short int *pbyte_num)
{
    unsigned short int byte_num;
    unsigned char *buff;
    unsigned char *verifycode;
    FILE *this;
    char chk_data_path[TK_MAX_PATH];
    sprintf(chk_data_path, "%s%s", filename, ".chk");
    if (access(chk_data_path, 0) == 0) {
        this = fopen(chk_data_path, "rb");
        if (this == NULL) {
            //printf("%s %d: file open fail.\n", __FILE__, __LINE__);
            return -1;
        }


        buff = malloc(8);
        if (buff == NULL) {
            //printf("%s %d: memory alloc fail.\n", __FILE__, __LINE__);
            fclose(this);
            return -1;
        }
        fread(buff, 1, 8, this);

        byte_num = (buff[7] << 8) | buff[6]; /* the byte order is changed */ 
        verifycode = malloc(byte_num);
        if (verifycode == NULL) {
            //printf("%s %d: memory alloc fail.\n", __FILE__, __LINE__);
            fclose(this);
            free(buff);
            return -1;
        }
        fread(verifycode, 1, byte_num, this);

        free(buff);
        fclose(this); 
        *pverifycode = verifycode;
        *pbyte_num = byte_num;
        return 0;
    } else {
        return -1;
    }
} 

int get_datafile_size(char *filename)
{
    /* get the size of this data file! */
    struct stat buf;
    if (stat(filename, &buf) < 0) {
        //printf("%s %d: %s\n", __FILE__, __LINE__, strerror(errno));
        return -1;
    }
    return buf.st_size;
}

int get_tile_num(unsigned char *tk_buffer_tile_index,
        int *pA_num, int *pB_num, int *pC_num, int index_length)
{
    unsigned char *cur_pointer;
    int res;
    cur_pointer = tk_buffer_tile_index;
    *pA_num = GETNUM3B(cur_pointer+9);
    res = *pA_num + 2;
    cur_pointer = tk_buffer_tile_index + res * 6;
    *pB_num = GETNUM3B(cur_pointer+9);
    res += *pB_num + 2;
    if (res < index_length/6) { /* if C_level tiles exist */
        cur_pointer = tk_buffer_tile_index + res * 6;
        *pC_num = GETNUM3B(cur_pointer+9);
    }
	return 0;
}

int data_buffer_offset;
unsigned char data_buffer[DATA_BUFF_LEN];

int get_tile_info(FILE *map_data, unsigned char *cur_pointer, int map_data_bias,
        int pre_pos, int *ppos, unsigned char *ptail, 
        int *ptile_size, int *ptotal_size)
{
    int offset;
    *ppos = GETNUM3B(cur_pointer);
    /* bit_jump_to(*ppos + map_data_bias - 1); */   
//    fseek(map_data, *ppos + map_data_bias - 1, SEEK_SET);
    /* change */
    offset = *ppos - 1;
    while (data_buffer_offset + DATA_BUFF_LEN <= offset) {
        fread(data_buffer, 1, DATA_BUFF_LEN, map_data);
        data_buffer_offset += DATA_BUFF_LEN;
        printf("read %d bytes from map_data\n", DATA_BUFF_LEN);
    }

    *ptail = data_buffer[offset % DATA_BUFF_LEN];

    /* *ptail = (int) bit_read_uint(8); */  
//    fread(ptail, 1, 1, map_data);
    /* change */

    *ptile_size = *ppos - pre_pos;
    *ptotal_size += *ptile_size;
    printf("pre_pos = %d, pos = %d, tile size = %d, total_size = %d, tail = %d\n", 
            pre_pos, *ppos, *ptile_size, *ptotal_size, *ptail);
	return 0;
}

int add_lostdata(int rid, unsigned char tail, unsigned char code, 
        unsigned char shift_num, unsigned int map_data_bias, int pre_pos,
        int tile_size, int *pdownloaded_size, int *poffset,
        int *plength, int *pis_in_block)
{
    if (((0x01L)&(tail ^ (code >> shift_num))) != 0) {
		*pdownloaded_size += tile_size;
        if (*pis_in_block) {/* in a block, now we must begin a new block */
            if (lostdata_idx < TK_LOST_DATA_PIECE) {
                lostdata[lostdata_idx].rid = rid;
                lostdata[lostdata_idx].offset = *poffset;
                lostdata[lostdata_idx].length = *plength;
                lostdata_idx++;
            }
            *pis_in_block = 0;/* not in a block now */
        }
    } else {
        if (*pis_in_block) {/* in a block, just increase the length */
            *plength += tile_size;
        } else {/* not in a block, now begin a new block */
            *poffset = pre_pos + map_data_bias;
            *plength = tile_size;/* attention now is assignment operator */
            *pis_in_block = 1;
        }
    }
	return 0;
}

struct reg_unit *find_region(int rid)
{
    return &reg_list[rid];
}

/* judge the relation of point with poly */
static int tk_point_in_region(int rid, struct tk_point *pt)
{
	int i;
    int cnt = 0;
    int point_num;
    struct tk_point *polygon;
    struct envelope env;

    if (rid == TK_REGION_ID_OUT_BOUND) {
        return 0;
    } 

    polygon = get_region_bound(rid, &point_num);

	for (i = 0;	i < point_num - 1;	i++ )
	{
        env.bottom = TK_MIN(polygon[i].y, polygon[i + 1].y);
        env.left = TK_MIN(polygon[i].x, polygon[i + 1].x);
        env.top = TK_MAX(polygon[i].y, polygon[i + 1].y);
        env.right = TK_MAX(polygon[i].x, polygon[i + 1].x);
		//levelCode=1代表下面的点是另一个多边形
        if( polygon[i+1].levelCode==1 )
            continue;
        if (!TK_POINT_IN_ENV(pt, env)){
            if(pt->x > env.right && pt->y < env.top && pt->y >= env.bottom){
                //在ENV的右中侧外部, 不包括上边界,包括下边界	
				cnt++;
			}
           continue;
        }            

        //该线段不是水平的
		if( polygon[i].y != polygon[i+1].y ) {
			//pt在矩形框上边界上,不应计入cnt
            if((env.top == pt->y )){
				continue;
			}
			//PT向左的射线上有端点,因为pt只可能在下边界上,故cnt应该计数
			if(( polygon[i].y == pt->y && polygon[i].x < pt->x) || ( polygon[i + 1].y == pt->y && polygon[i + 1].x < pt->x)){
				cnt++;
				continue;
			}
			else {
                //通过两个乘积x_pro和y_pro算出是否为左侧的线
                double x_pro;
                double y_pro;
                //斜率为正
                if ((polygon[i].y >= polygon[i + 1].y && polygon[i].x >= polygon[i + 1].x)
                    || (polygon[i].y <= polygon[i + 1].y && polygon[i].x <= polygon[i + 1].x)){
                    x_pro = ((double) (env.top - env.bottom)) * (pt->x - env.left);
                    y_pro = ((double) (pt->y - env.bottom)) * (env.right - env.left);
                } else {
                    x_pro = ((double) (env.top - env.bottom)) * (pt->x - env.right);
                    y_pro = ((double) (pt->y - env.bottom)) * (env.left - env.right);
                }

                //边界上
                if(x_pro == y_pro){
                	return 1;
				}
                else{
                    if (x_pro > y_pro){
                        cnt++;
                    }
                } 
			}
		}
        else{
            //水平线
            if (pt->y == env.bottom) {
                if (pt->x >= env.left) {
                    if (pt->x <= env.right)
                        return 1;
                }
            }
        }
	}
	if (cnt % 2 == 1)
		return 1;
	return 0;
}

int tk_get_rid_by_point(struct tk_point *pt) 
{
	int rid = -1;
    int level = tk_engine.current_z;

    //if (level > TK_NATIONAL_LEVEL_A) {
        for (int i = 0; i < reg_num; i++) {
            if (TK_POINT_IN_ENV(pt, reg_bounds[i].env) == 1) {
                if (tk_point_in_region(i, pt)) {
                    rid = i;
                    break;
                }
            }
        }
	//} else { 
	//	rid = TK_REGION_ID_NATIONAL;//National
    //}
    return rid;
}
/* ===================
 * GPS Transformer 
 * ===================*/
double casm_rr ;
unsigned int casm_t1;
unsigned int casm_t2;
double casm_x1;
double casm_y1;
double casm_x2;
double casm_y2;
double casm_f;

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

double Transform_yj5(double x , double y ) {
	double tt;
	tt = 300 + 1 * x + 2 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * sqrt(sqrt(x*x));
	tt = tt + (20 *yj_sin2(18.849555921538764 * x ) + 20 * yj_sin2(6.283185307179588 * x))*0.6667;
	tt = tt + (20 * yj_sin2(3.141592653589794 * x ) + 40 * yj_sin2(1.047197551196598 * x))*0.6667;
	tt = tt + (150 * yj_sin2(0.2617993877991495 * x) + 300 * yj_sin2(0.1047197551196598 * x))*0.6667;
	return tt;
}

double Transform_yjy5(double x , double y ) {
	double tt ;
	tt = -100 +  2 * x + 3 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * sqrt(sqrt(x*x));
	tt = tt + (20 * yj_sin2(18.849555921538764 * x) + 20 * yj_sin2(6.283185307179588 * x))*0.6667;
	tt = tt + (20 * yj_sin2(3.141592653589794 * y)+ 40 * yj_sin2(1.047197551196598 * y))*0.6667;
	tt = tt + (160 * yj_sin2(0.2617993877991495 * y) + 320 * yj_sin2(0.1047197551196598 * y))*0.6667;
	return tt;
}

double Transform_jy5(double x , double xx ) {
	double n ;
	double a ;
	double e ;
	a = 6378245;
	e = 0.00669342;
	n = sqrt (1 - e * yj_sin2(x * 0.0174532925199433) * yj_sin2(x * 0.0174532925199433) );
	n = (xx * 180) /(a / n * cos(x * 0.0174532925199433) * 3.1415926) ;
	return n;
}

double Transform_jyj5(double x , double yy ) {
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

double random_yj() {
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

void IniCasm(unsigned int w_time, unsigned int w_lng, unsigned int w_lat) {
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

unsigned int wgtochina_lb(int wg_flag, unsigned int wg_lng, unsigned int wg_lat, 
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

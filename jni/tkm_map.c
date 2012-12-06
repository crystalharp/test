;/*
 * =====================================================================================
 *
 *       Filename:  tk_map.c
 *
 *    Description:   
 *
 *        Version:  1.0
 *        Created:  2010年12月28日 17时45分55秒
 *       Revision:  none
 *       Compiler:  gcc
 *
 *         Author:  YOUR NAME (), 
 *        Company:  
 *
 * =====================================================================================
 */
#include <sys/stat.h>
#include <stdlib.h>
#include <errno.h>
#include <string.h>
#include <math.h>
#include <errno.h>
#include <assert.h>
#include <unistd.h>
#include "tk_config.h"
#include "tk_log.h"
#include "tkm_map.h"
#include "tkm_render.h"
#include "tkm_mapint.h"
#include "tkm_font.h"
#include "tkm_layer.h"
#include "png.h"
#include "pngpriv.h"

/* ============================================ 
 *  Initialize and destroy the engine  
 *  =========================================== */
// whether use L optimization, yes if l_opt == 1 
int l_opt;
static int inner_buf_size;
unsigned char *inner_buf;
unsigned char *out_buf;

float tk_font_offset = 1.0;
int   tk_icon_size = 3;

typedef struct tagBITMAPFILEHEADER {
	unsigned int bfSize; 
	unsigned short int bfReserved1; 
	unsigned short int bfReserved2; 
	unsigned int bfOffBits; 
} BITMAPFILEHEADER;

typedef struct tagBITMAPINFOHEADER{
	unsigned int biSize; 
	unsigned int biWidth; //the width of bitmap in pixel
	int biHeight;// the height of bitmap in pixel 
	unsigned short int biPlanes; 
	unsigned short int biBitCount;// the bit num of each pixel 
	unsigned int biCompression; 
	unsigned int biSizeImage; 
	unsigned int biXPelsPerMeter; 
	unsigned int biYPelsPerMeter; 
	unsigned int biClrUsed; 
	unsigned int biClrImportant; 
} BITMAPINFOHEADER; 

static void set_bmp_head(unsigned char *pt, unsigned short int w, unsigned short int h)
{
    memset(pt, 0, 56);
    BITMAPFILEHEADER s1;
	BITMAPINFOHEADER s2;
    pt[0] = 0x42;
	pt[1] = 0x4D;
    memset(&s1, 0, sizeof(BITMAPFILEHEADER));
    s1.bfOffBits = 56;
    memcpy(pt+2, &s1, sizeof(BITMAPFILEHEADER));
     
    memset(&s2, 0, sizeof(BITMAPINFOHEADER));
	s2.biSize = 0x28;
	s2.biWidth = w;
	s2.biHeight = -h;
	s2.biPlanes = 1;
#ifdef TK_BPP_16
    s1.bfSize = 56 + w*h*2;
	s2.biBitCount = 16;
	s2.biSizeImage = w*h*2;
#endif
#ifdef TK_BPP_32
    s1.bfSize = 56 + w*h*4;
	s2.biBitCount = 32;
	s2.biSizeImage = w*h*4;
#endif
	memcpy(pt + 2 + sizeof(BITMAPFILEHEADER), &s2, sizeof(BITMAPINFOHEADER));
}


int tk_init_engine(const char *resdir, const char *mapdir, int w, int h, void *bmpbuf, int need_opt)
{
    is_draw_subway = 1;
	tk_engine.width = w;
	tk_engine.height = h;
    w = TILE_SIZE;
    h = TILE_SIZE;

#ifdef LOG_TO_FILE
    char log_path[TK_MAX_PATH];
    sprintf(log_path, "%s/tigermap.log", mapdir);
    if ((log_fp = fopen(log_path, "w+")) == NULL) {
        log_fp = stdout;
    }
#else
    log_fp = stdout;
#endif
	if (resdir == NULL || mapdir == NULL) {
        LOG_INFO("engine initial failed, the resdir|mapdir is null!");
		return -1;
	}

	if (strlen(resdir) >= TK_MAX_PATH || strlen(mapdir) >= TK_MAX_PATH) {
        LOG_INFO("engine initial failed, the length of resdir|mapdir is too large!");
		return -1;
    }

	strcpy(respath, resdir);
	strcpy(datapath, mapdir);

    //img_buffer = init_icon_num(50);

	//if (load_images() == -1) {
    //    LOG_INFO("failed to load the images data file!\n");
    //    return -1;
    //}

    l_opt = need_opt;
#ifdef NEED_BMP_HEADER
    set_bmp_head(bmpbuf, w, h);
    bmpbuf += 56;
#endif
    if (l_opt == 1) {
        out_buf = bmpbuf;
    #ifdef TK_BPP_16
        inner_buf = xmalloc(w*h*2);
        inner_buf_size = w*h*2;
    #endif 
    #ifdef TK_BPP_32
        inner_buf = xmalloc(w*h*4);
        inner_buf_size = w*h*4;
    #endif
        tk_gdi.pd = (tk_pixel *)inner_buf;
    } else {
        tk_gdi.pd = (tk_pixel *)bmpbuf;
    }
    if(is_draw_subway){
    surface =
        cairo_image_surface_create_for_data ((unsigned char *)tk_gdi.pd, CAIRO_FORMAT_ARGB32, w, h, w * 4);
    cr = cairo_create (surface);
    }
    load_context();

    LOG_DBG("process observer: in init_engine, cr created!\n");
    if (load_citylist() == -1) {
        LOG_INFO("failed to load the citylist!\n");
        return -1;
    }

    if (load_regionlist() == -1) {
        LOG_INFO("failed to load the regionlist!\n");
        return -1;
    }

    if (load_envelope() == -1) {
        LOG_INFO("failed to load the envelope!\n");
        return -1; 
    }
    load_sw_colorcfg();
    //init_subway_color();

    LOG_INFO("map engine initialized successfully!\n");
    tk_init_label();

#ifdef LINUX_PLAT
    tk_font_init();
#endif

    tk_init_bestfeature();

    tk_init_gdicontext();
    struct envelope e;
    e.bottom = h -1; e.left = 0; e.top = 0; e.right = w - 1; 
    tk_gdi_cleancdata(e, 12, 0);

    tk_engine.current_z = 10;

    return 0;
}
//extern int tk_font_offset;

void tk_reset_font_size(float offset) {
    tk_font_offset = offset;
}
void tk_reset_icon_size(int size) {
       tk_icon_size = size;
}

void tk_reset_matrix_size(int w, int h, void *bmpbuf)
{
    cairo_destroy (cr);
    cairo_surface_destroy (surface);

    int same_size = 0;
    if (w * h == tk_engine.width * tk_engine.height)
        same_size = 1;

	tk_engine.width = w;
	tk_engine.height = h;
    calculate_box();

#ifdef NEED_BMP_HEADER
    set_bmp_head(bmpbuf, w, h);
    bmpbuf += 56;
#endif
    if (l_opt == 1) {
        out_buf = bmpbuf;
    #ifdef TK_BPP_16
        if (!same_size) {
            free(inner_buf);
            inner_buf = xmalloc(w * h *2);
            inner_buf_size = w * h * 2;
        }
    #endif
    #ifdef TK_BPP_32
        if (!same_size) {
            free(inner_buf);
            inner_buf = xmalloc(w * h * 4);
            inner_buf_size = w * h * 4;
        }
    #endif
        tk_gdi.pd = (tk_pixel *)inner_buf;
    } else {
        tk_gdi.pd = (tk_pixel *)bmpbuf;
    }

    surface =
        cairo_image_surface_create_for_data ((unsigned char *)tk_gdi.pd, CAIRO_FORMAT_ARGB32, w, h, w * 4);
    cr = cairo_create (surface);

    need_read_whole_map = 1;
    /*TODO:  delete connect while the map's width and height is changed
    int i = 0;
    while (tk_engine.tl.length) {
        struct tile *tempt = (struct tile*)arraylist_get(&tk_engine.tl, i);
        int is_in = in_envelope_tile(tilebbox, tempt->coder_lon, tempt->coder_lat, tempt->level);

        if ((!is_in) || tk_engine.flags & F_ZOOMED) {
            if (!tk_engine.flags & F_ZOOMED) {
                tile_delete_connect(tempt);
            }
            arraylist_del(&tk_engine.tl, i);
            i--;
        }
        i++;
        if (i >= tk_engine.tl.length) {
            break;
        }
    }*/

}

void tk_destroy_engine()
{
    cairo_destroy (cr);
    cairo_surface_destroy (surface);

    release_gdicontext();
    release_images();
    release_regs();
    release_list();
    release_tb();
    release_styles();
    release_reg_bounds();
    release_points();
    release_layers();
    tk_destroy_label();

    if (l_opt == 1) {
        free(inner_buf);
    }
#ifdef LINUX_PLAT
    tk_font_destroy();
#endif
#ifdef LOG_TO_FILE
    fclose(log_fp);
#endif
}
static void print_lostdata(){
    if (lostdata_idx == 0)
        LOG_DBG("data no lost!\n");
    for (int i = 0; i < lostdata_idx; i++)
        LOG_DBG("lostdata: rid=%d, offset=%d, length=%d\n", lostdata[lostdata_idx].rid,
                lostdata[lostdata_idx].offset, lostdata[lostdata_idx]);
}

/* ============================================ 
 *    add png func 
 * ============================================ */
#define MIN(a,b) (a <= b ? a : b)
int  png_buf_size = 0;
void *png_bufp = NULL;

void PNGCBAPI
tk_png_write_data(png_structp png_ptr, png_bytep data, png_size_t length)
{
   png_uint_32 check;
   png_byte *near_data;  /* Needs to be "png_byte *" instead of "png_bytep" */
   png_FILE_p io_ptr;

   if (png_ptr == NULL)
      return;

   /* Check if data really is near. If so, use usual code. */
   near_data = (png_byte *)CVT_PTR_NOCHECK(data);
   io_ptr = (png_FILE_p)CVT_PTR(png_ptr->io_ptr);
   png_buf_size += length;
   //char *png_bufp = &png_buf;

   if ((png_bytep)near_data == data)
   {
      memcpy(png_bufp, near_data, length);
      //check = fwrite(near_data, 1, length, io_ptr);
      //memcpy(png_bufp,near_data,length);
      png_bufp += length;
   }

   else
   {
      png_byte buf[1024];
      png_size_t written, remaining, err;
      check = 0;
      remaining = length;
      do
      {
         written = MIN(1024, remaining);
         png_memcpy(buf, data, written); /* Copy far buffer to near buffer */
         //err = fwrite(buf, 1, written, io_ptr);
         memcpy(png_bufp,data,written);
         /*err = fwrite(png_bufp, 1, written, io_ptr);

         if (err != written)
            break;

         else
            check += err;
         */
         data += written;
         png_bufp += written;
         remaining -= written;
      }
      while (remaining != 0);
   }

   //if (check != length)
      //png_error(png_ptr, "Write Error");
}

static void
png_simple_error_callback (png_structp png,
                           png_const_charp error_msg)
{
             cairo_status_t *error = png_get_error_ptr (png);
             
                 /*  default to the most likely error */
             if (*error == CAIRO_STATUS_SUCCESS)
               *error = _cairo_error (CAIRO_STATUS_NO_MEMORY);
            
       #ifdef PNG_SETJMP_SUPPORTED
              longjmp (png_jmpbuf (png), 1);
       #endif
              
             /*  if we get here, then we have to choice but to abort ... */
}

static void                    
png_simple_warning_callback (png_structp png,
                             png_const_charp error_msg)
{   
              cairo_status_t *error = png_get_error_ptr (png);
              
                  /*  default to the most likely error */
              if (*error == CAIRO_STATUS_SUCCESS)
                  *error = _cairo_error (CAIRO_STATUS_NO_MEMORY);
             
              /*  png does not expect to abort and will try to tidy up after a warning */
}   
   
static void               
png_simple_output_flush_fn (png_structp png_ptr)
{   
} 
png_rw_ptr write_func;

static void
unpremultiply_data (png_structp png, png_row_infop row_info, png_bytep data)
{   
             unsigned int i;
             
             for (i = 0; i < row_info->rowbytes; i += 4) {
                         char *b = &data[i];
                         int pixel;
                         char  alpha; 
                     
                         memcpy (&pixel, b, sizeof (int));
                         alpha = (pixel & 0xff000000) >> 24;
                         if (alpha == 0) {
                             b[0] = b[1] = b[2] = b[3] = 0;
                         } else {
                                     b[0] = (((pixel & 0xff0000) >> 16) * 255 + alpha / 2) / alpha;
                                     b[1] = (((pixel & 0x00ff00) >>  8) * 255 + alpha / 2) / alpha;
                                     b[2] = (((pixel & 0x0000ff) >>  0) * 255 + alpha / 2) / alpha;
                                     b[3] = alpha;
                             }
              }
}
#include "pngstruct.h"


int bmp_to_png(void *bmp_buf,void *png_buf){
    png_buf_size = 0;
    cairo_status_t status;
    png_struct *png;
    png_info *info;
    png_byte **volatile rows = NULL;
    png_color_16 white;
    int png_color_type;
    int depth;
    void *closure = NULL;
    png_bufp = png_buf;

    png = png_create_write_struct (PNG_LIBPNG_VER_STRING, &status,
            png_simple_error_callback,
            png_simple_warning_callback);
    //png_set_write_fn (png, closure, write_func, png_simple_output_flush_fn);
    png_set_write_fn (png, closure, tk_png_write_data, png_simple_output_flush_fn);
    png_color_type = PNG_COLOR_TYPE_RGB_ALPHA;
    depth = 8;
    int i = 0;
    rows = malloc (tk_engine.width*sizeof(png_byte*));
    for (i = 0; i < tk_engine.width; i++)
            rows[i] = (png_byte *) bmp_buf + i * tk_engine.width * 4 + 56;

    info = png_create_info_struct (png);
    png_set_IHDR (png, info,
                  tk_engine.width,
                  tk_engine.width, depth,
                  png_color_type,
                  PNG_INTERLACE_NONE,
                  PNG_COMPRESSION_TYPE_DEFAULT,
                  PNG_FILTER_TYPE_DEFAULT);

    white.gray = (1 << depth) - 1;
    white.red = white.blue = white.green = white.gray;
    png_set_bKGD (png, info, &white);

    png_write_info (png, info);
    png_set_write_user_transform_fn (png, unpremultiply_data);
    png_write_image (png, rows);
    //png_write_end (png, info);
    png_write_end (png, info);
    free(rows);
    return png_buf_size;
}
/*
*   end
*/

/* ============================================ 
 *    Get the rendered map data  
 * ============================================ */

void tk_refresh_map_buffer()
{
    draw_whole_map();   
    print_lostdata();
    if (lostdata_idx != 0) {
        need_read_whole_map = 1;
    } else {
        need_read_whole_map = 0;
    }
    GDI_SETBGCOLOR((255 << 16) + (255 << 8)+ 255);
    GDI_SETCOLOR(0);

    if (l_opt == 1) {
        memcpy(out_buf, inner_buf, inner_buf_size);
    }

    if (l_opt == 1) {
        tk_gdi.pd = (tk_pixel *)out_buf;
    }
    draw_label();
    if (l_opt == 1) {
        tk_gdi.pd = (tk_pixel *)inner_buf;
    }
}


/* ===========================================
 * zoom control interface 
 * =========================================== */

int tk_zoom_in(void) 
{
    int level = tk_engine.current_z;

	if (level == LEVEL_MAX) {
//        is_zoomed_to_max = 1;
		return -1;
    }
    
	if (++level == LEVEL_SKIP)
	    level++;

    set_zlevel(level);
    return 0;
}

int tk_zoom_out(void) 
{

    int level = tk_engine.current_z;

	if (level == LEVEL_MIN)
		return -1;
    
	if (--level == LEVEL_SKIP)
	    level--;

    set_zlevel(level);
    return 0;
}

int tk_get_zoom()
{
    int level;
    level = (int)tk_engine.current_z;
    //map 4-8 to 5-9 for upper application
    if (level < LEVEL_SKIP)
        level++;
	return level;
}

/* zoom level */
#define MIN_Z 5
#define MAX_Z 18

int tk_set_zoom(int level) 
{
    if (level < MIN_Z || level > MAX_Z) {
        LOG_INFO("Zoom level must be between 5 and 18: %d\n", level);
        return -1;
    }
     //map 5-9 to 4-8
	if (level <= LEVEL_SKIP)
		level--;

    set_zlevel(level);
   
    if (tk_engine.current_z != level) {
        tk_engine.flags |= F_ZOOMED;
    }
	return 0;
}


/* ============================================ 
 * Map moving routines.
 * ============================================ */

/* latitude and longitude range */
#define MIN_LAT -85.5
#define MAX_LAT 85.5
#define MIN_LON -180.0
#define MAX_LON 180.0


/* earth radius */
#define EARTH_RADIUS 6378137

#define MATH_PI  3.141592653589793238

unsigned int tk_lon_to_x(double lon) 
{
    return (int)floor(TK_ABS(-(lon + 180) / 360 * (1 << GSCALE_FACTOR) * TILE_SIZE));
}

unsigned int tk_lat_to_y(double lat)
{
    double lat_in_rad, y_temp, y;

    lat_in_rad = lat * (MATH_PI) / 180;
    y_temp = (sin(lat_in_rad) + 1) / -(sin(lat_in_rad) - 1);
    y = -(log(y_temp) / (MATH_PI * 2) - 1) * (1 << (GSCALE_FACTOR - 1)) * TILE_SIZE;

    return (int)floor(y);
}


int tk_move_latlonzoom(double lat, double lon, int zoom)
{
	int temp_x;
	int temp_y;

    if (zoom < MIN_Z || zoom > MAX_Z) {
        LOG_INFO("Zoom level must be between 5 and 18: %d\n", zoom);
        return -1;
    }
    if (lon < (MIN_LON) || lon > (MAX_LON)) {
        LOG_INFO("longitude is out of range: %f\n", lon);
        return -1;
    }
    if (lat < (MIN_LAT) || lat > (MAX_LAT)) {
        LOG_INFO("latitude is out of range: %f\n", lat);
        return -1;
    }

	temp_x = tk_lon_to_x(lon);
	temp_y = tk_lat_to_y(lat);

	if (temp_x < 0 || temp_y < 0) {
		return -1;
	}

    if (TK_X_IN_CHINA(temp_x)) {
        tk_engine.center_x = temp_x;
    }
    if (TK_Y_IN_CHINA(temp_y)) {
        tk_engine.center_y = temp_y;
    }

    //map 5-9 to 4-8
	if (zoom <= LEVEL_SKIP)
		zoom--;
    set_zlevel(zoom);
    return 0;
}

void get_tile_box(int x, int y, int bl_dif, struct envelope *el,int tile_w,int tile_h) {
    struct envelope box;
    get_tile_base_level_bound(x, y, bl_dif, &box);

    if (bl_dif >= 0) {
        el->left = box.left << (8 - bl_dif);
        el->right = (box.left << (8 - bl_dif)) + tile_w ;
        el->bottom = box.bottom << (8 - bl_dif);
        el->top = (box.bottom << (8 - bl_dif)) + tile_h ;
    } else {
        el->left = (box.left << (8 - bl_dif))  + tile_w * (x - ((x >> -bl_dif) << -bl_dif));
        el->right = el->left + tile_w;
        el->bottom = (box.bottom << (8 - bl_dif)) +tile_h * (y - ((y >> -bl_dif) << -bl_dif));
        el->top = el->bottom + tile_h;
    }
}

int tk_get_tile_size(int level_dif) {
    if (level_dif >= 0)
        return TILE_SIZE >> level_dif;
    else
        return TILE_SIZE << level_dif; 
}
/* level 11 13 16 tile_size = 256
 * level 10 12 14 15 tile_size = 256 >> tk_engine.bl_dif
 * so if level == 10 need to compose 4 level_11 tile to a 256_size tile 
 *    -----------
 *    - 11 - 11 -
 * 10 -----------
 *    - 11 - 11 -
 *    -----------
 * level 17 18 need clip level_16 
 * cliping in function  get_tile_bbox_from_point();
 * */ 

void get_tile_256_bound(int *min_x, int *min_y, int *max_x, int *max_y, int bl_dif) {
    if (bl_dif >= 0) {
        *min_x = *min_x >> bl_dif;
        *max_x = *max_x >> bl_dif;
        *min_y = *min_y >> bl_dif;
        *max_y = *max_y >> bl_dif;
    } 
    /*else {
        *min_x = *min_x << (-bl_dif);
        *max_x = (*max_x << (-bl_dif)) ;
        *min_y = *min_y << (-bl_dif);
        *max_y = (*max_y << (-bl_dif)) ;//+ (1 << (-bl_dif)) -1;
    }*/
}

/* 根据经纬度 算出点所在的tile */
int tk_get_tile_xy(double lat, double lon, int zoom, int *min_tile_x, int *min_tile_y) {
	int temp_x;
	int temp_y;
    struct envelope el;

    if (zoom < MIN_Z  - 1 || zoom > MAX_Z) {
        LOG_INFO("Zoom level must be between 5 and 18: %d\n", zoom);
        return -1;
    }
    if (lon < (MIN_LON) || lon > (MAX_LON)) {
        LOG_INFO("longitude is out of range: %f\n", lon);
        return -1;
    }
    if (lat < (MIN_LAT) || lat > (MAX_LAT)) {
        LOG_INFO("latitude is out of range: %f\n", lat);
        return -1;
    }
    if (zoom <= LEVEL_SKIP)
        zoom--;

	temp_x = tk_lon_to_x(lon);
	temp_y = tk_lat_to_y(lat);

	if (temp_x < 0 || temp_y < 0) {
		return -1;
	}
    if (zoom <= 16) {
        *min_tile_x = temp_x >> (8 + TK_BASE_LEVEL_A - zoom);
        *min_tile_y = temp_y >> (8 + TK_BASE_LEVEL_A - zoom);
    } else {
        *min_tile_x = temp_x >> (8 + (TK_BASE_LEVEL_A - zoom));  
        *min_tile_y = temp_y >> (8 + (TK_BASE_LEVEL_A - zoom));
    }
    return 0;
}


/* 根据屏幕中心点的经纬度计算出tile 范围 */
int tk_get_tile_limits(double lat, double lon, int zoom, int *min_tile_x, int *min_tile_y, int *max_tile_x, int *max_tile_y) {
	int temp_x;
	int temp_y;
    struct envelope el;

    if (zoom < MIN_Z || zoom > MAX_Z) {
        LOG_INFO("Zoom level must be between 5 and 18: %d\n", zoom);
        return -1;
    }
    if (lon < (MIN_LON) || lon > (MAX_LON)) {
        LOG_INFO("longitude is out of range: %f\n", lon);
        return -1;
    }
    if (lat < (MIN_LAT) || lat > (MAX_LAT)) {
        LOG_INFO("latitude is out of range: %f\n", lat);
        return -1;
    }

	temp_x = tk_lon_to_x(lon);
	temp_y = tk_lat_to_y(lat);

	if (temp_x < 0 || temp_y < 0) {
		return -1;
	}

    if (!TK_X_IN_CHINA(temp_x) || !TK_Y_IN_CHINA(temp_y)) {
        return -1;
    }
    tk_engine.center_x = temp_x;
    tk_engine.center_y = temp_y;

    clean_tile(zoom);

    tk_engine.bl = get_base_level(zoom);

    set_zlevel(zoom);

    calculate_box();

    get_tile_bbox_from_point(&el);

    if (tk_engine.current_z > TK_NATIONAL_LEVEL_A) {
             tk_engine.ps = &(tk_engine.c_styles);
         } else {
             tk_engine.ps = &(tk_engine.n_styles);
    }
    *min_tile_x = el.left;
    *max_tile_x = el.right;
    *min_tile_y = el.bottom;
    *max_tile_y = el.top;

    get_tile_256_bound(min_tile_x, min_tile_y, max_tile_x, max_tile_y, tk_engine.bl_dif);

    return 0;
}

/* 得到一定范围内 tile的lostadata */
void tk_get_lostdata_by_bound_tile(int zoom, int min_tile_x, int min_tile_y, int max_tile_x, int max_tile_y) {
    int blevel;
    int bl_diff;
    int i, j;
    struct envelope tile_bbox;
    blevel= get_base_level(zoom);
    bl_diff = blevel - zoom;
    lostdata_idx = 0;
    memset(lostdata, 0, sizeof(struct tk_map_lostdata) * TK_LOST_DATA_PIECE); 
    for (i = min_tile_x; i <= max_tile_x; i++) {
        for (j = min_tile_y; j <= max_tile_y; j++) {
            get_tile_base_level_bound(i, j, bl_diff, &tile_bbox);
            increase_tile(&tile_bbox);
        }
    }
}



/* 修改接口 只需要中心点的经纬度
 * 根据屏幕最大最小经纬度，得到需要的tile 范围 */
/*int tk_get_tile_xy(double min_lat, double min_lon, double max_lat, double max_lon, int zoom, \ 
                        int *min_tile_x, int *min_tile_y, int *max_tile_x, int *max_tile_y) {
    return tk_get_tile_limits((min_lat + max_lat) / 2, (min_lon + max_lon) / 2, zoom, 
            min_tile_x,min_tile_y,max_tile_x,max_tile_y);
}*/

extern int *layer_ctl;
//extern void get_map_road_name(struct layer *pl, int vlevel);
//extern void get_map_polygon_name(struct layer *pl, int vlevel);
extern int regnum_in_bound;
int  tk_get_screen_label(double lon, double lat, int w, int h, int zoom) {
    struct envelope bbox;
    struct envelope tile_box;
    struct envelope el;
    int x, y;
    int i;
    int center_id;
	x = tk_lon_to_x(lon);
	y = tk_lat_to_y(lat);
    /*if (zoom <= 16) {
        x = x >> (16 - zoom );
        y = y >> (16 - zoom );
    } else {
        x = x << (-16 + zoom );
        y = y << (-16 + zoom );
    }*/
    clean_tile(zoom);
    tk_engine.bl = get_base_level(zoom);
    set_zlevel(zoom);
    //tk_engine.bl = get_base_level(zoom);

    tk_engine.center_x = x;
    tk_engine.center_y = y;
    tk_reinit_gdicontext(w, h);
    calculate_box(w,h);
    get_tile_bbox_from_point(&bbox);
    //increase_tile(&bbox);
    //get_tile_bbox_from_point(&bbox);
    memcpy(&(tk_engine.min_tile_bbox), &tk_engine.cur_bbox, sizeof(struct envelope));
    if (zoom > 16) {
        tk_engine.min_tile_bbox.left = tk_engine.min_tile_bbox.left << -tk_engine.bl_dif;
        tk_engine.min_tile_bbox.right = tk_engine.min_tile_bbox.right << -tk_engine.bl_dif;
        tk_engine.min_tile_bbox.top = tk_engine.min_tile_bbox.top << -tk_engine.bl_dif;
        tk_engine.min_tile_bbox.bottom = tk_engine.min_tile_bbox.bottom << -tk_engine.bl_dif;
    }
    tk_geo_cleanmaplabel();

    //get_tile_level_16_bound(&tile_box, &el);
    center_id = find_regions_in_bbox(&tk_engine.cur_bbox);
    if (regnum_in_bound == 0)
        return -1;

    lostdata_idx = 0;
    memset(lostdata, 0, sizeof(struct tk_map_lostdata) * TK_LOST_DATA_PIECE);

    increase_tile(&bbox,0); 
    /*for (x = bbox.left; x <= bbox.right; x++) {
        for (y = bbox.bottom; y <= bbox.top; y++)
            get_tile_by_xy(x, y, 0);
    }*/

    adjust_lostdata();
    maploader_syc_from_buffer();
    tk_update_buffer_screens(tk_gdi.rtview, vm_screen);

	struct layer *pl = NULL;
    int layindex = 0;
    if ( tk_engine.current_z >= 10) {
        for(i = 0; i < tk_engine.ps->layer_num; i++){
        if (i == 0) {
                layindex = i + 39;
                pl = &tk_engine.layer_list[i + 39];
                pstyle = &(tk_engine.ps->styles[i + 39]);
        }
        if (i == 12) {
                layindex = i + 2;
                pl = &tk_engine.layer_list[i + 2];
                pstyle = &(tk_engine.ps->styles[i + 2]);
        }
        if (i > 0 && i <= 39 && i != 12) {
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
        if (i > 39) {
            layindex = i;
            pl = &tk_engine.layer_list[i];
            pstyle = &(tk_engine.ps->styles[i]);
        }
            //pl = &tk_engine.layer_list[i];
            //pstyle = &(tk_engine.ps->styles[i]);
        if (pl == NULL)
            continue;
        if (layer_ctl[layindex] == 1) {
            //printf("thishitshithis\n");
            /*if (tk_engine.ps->feature_type[layindex] == TKGEO_ENMFTYPE_POINT) {
                ;//draw_point(pl, tk_engine.current_z);
            } else if (tk_engine.ps->feature_type[layindex] == TKGEO_ENMFTYPE_LINE) {
                draw_linelayer(pl, tk_engine.current_z);
            } else if (tk_engine.ps->feature_type[layindex] == TKGEO_ENMFTYPE_RAIL) {
                draw_rail(pl, tk_engine.current_z);
            } else if (tk_engine.ps->feature_type[layindex] == TKGEO_ENMFTYPE_ROAD) {
                draw_road(pl, tk_engine.current_z);
            } else if (tk_engine.ps->feature_type[layindex] == TKGEO_ENMFTYPE_POLY) {
               tk_geo_drawvmlpoly(pl, tk_engine.current_z);*/
            if (tk_engine.ps->feature_type[layindex] == TKGEO_ENMFTYPE_POINT) {
                draw_point(pl, tk_engine.current_z);
            } else if (tk_engine.ps->feature_type[layindex] == TKGEO_ENMFTYPE_POLY) {
               get_map_polygon_name(pl, tk_engine.current_z);
            } else if (tk_engine.ps->feature_type[layindex] == TKGEO_ENMFTYPE_LINE) {
                get_map_road_name(pl, tk_engine.current_z);
            } else if (tk_engine.ps->feature_type[layindex] == TKGEO_ENMFTYPE_RAIL) {
                get_map_road_name(pl, tk_engine.current_z);
            } else if (tk_engine.ps->feature_type[layindex] == TKGEO_ENMFTYPE_ROAD) {
                get_map_road_name(pl, tk_engine.current_z); 
            }
            }
        }
        return 0;
    }
    if (tk_engine.current_z < 10) {
        for(i = 0; i < tk_engine.ps->layer_num; i++){
            pl = &tk_engine.layer_list[i];
            pstyle = &(tk_engine.ps->styles[i]);
        if (pl == NULL)
            continue;
        if (layer_ctl[i] == 1) {
            if (tk_engine.ps->feature_type[i] == TKGEO_ENMFTYPE_POINT) {
                draw_point(pl, tk_engine.current_z);
            } else if (tk_engine.ps->feature_type[i] == TKGEO_ENMFTYPE_POLY) {
               get_map_polygon_name(pl, tk_engine.current_z);
            } else {
                get_map_road_name(pl, tk_engine.current_z);
            }
            }
        }
        return 0;
    }
}

int tk_get_tile_buffer(int tile_x, int tile_y, int zoom) {
    struct envelope el;
    int level_24 = 0;
    int flag = 0;
    //int diff_lv = tk_engine.bl - tk_engine.current_z;
    //
    //if (zoom <= LEVEL_SKIP)
      //  zoom--;
    //if (zoom <= LEVEL_SKIP)
      //  level_24 =  16 - (zoom + 1) + 8;
    //else
        level_24 = 16 - zoom + 8;

    if (zoom < 4 || zoom > 18)
        return -1;
    //if (zoom <= LEVEL_SKIP)
      //  zoom--;
    
    if (!TK_X_IN_CHINA_ZOOM(tile_x , level_24) || !TK_Y_IN_CHINA_ZOOM(tile_y , level_24)) {
        //return -1;
    }

    tk_engine.center_x = tile_x;
    tk_engine.center_y = tile_y;
    //add for antroid
    clean_tile(zoom);

    tk_engine.bl = get_base_level(zoom);

    set_zlevel(zoom);
    /*
    if (tk_engine.current_z > TK_NATIONAL_LEVEL_A) {
             tk_engine.ps = &(tk_engine.c_styles);
         } else {
             tk_engine.ps = &(tk_engine.n_styles);
    }*/
    //end
    lostdata_idx = 0;
    memset(lostdata, 0, sizeof(struct tk_map_lostdata) * TK_LOST_DATA_PIECE);

    get_tile_box(tile_x, tile_y, tk_engine.bl_dif, &el, TILE_SIZE, TILE_SIZE);
    flag = draw_tile(tile_x,tile_y,&el);
    //if (flag > 0)
    //    return flag;
    if (l_opt == 1) {
        memcpy(out_buf, inner_buf, inner_buf_size);
    }

    if (l_opt == 1) {
        tk_gdi.pd = (tk_pixel *)out_buf;
    }
    draw_label();
    if (l_opt == 1) {
        tk_gdi.pd = (tk_pixel *)inner_buf;
    }

    if (flag > 0)
        return flag;

    if (lostdata_idx > 0)
        return -1;

    return 0;
}

void tk_get_screen_box(struct envelope * el) {
    int vlevel = tk_engine.current_z;
    if (tk_engine.current_z > 16) {
            el->left = (tk_engine.cur_bbox.left << (vlevel - 16));
            el->right = (tk_engine.cur_bbox.right << (vlevel - 16));
            el->bottom = (tk_engine.cur_bbox.bottom << (vlevel - 16));
            el->top = (tk_engine.cur_bbox.top << (vlevel - 16));
    }
    else {
            el->left = (tk_engine.cur_bbox.left >> (16 - vlevel));
            el->right = (tk_engine.cur_bbox.right >> (16 - vlevel));
            el->bottom = (tk_engine.cur_bbox.bottom >> (16 - vlevel));
            el->top = (tk_engine.cur_bbox.top >> (16 - vlevel));
    }
}

static int tk_calc_zoom(double lat0, double lon0, double lat1, double lon1)
{
    int x0, y0, x1, y1;
    int zoom, lvl_zoom;
    int x_dif, y_dif, x_diff, y_diff;

	x0 = tk_lon_to_x(lon0);
	y0 = tk_lat_to_y(lat0);
	x1 = tk_lon_to_x(lon1);
	y1 = tk_lat_to_y(lat1);
    x_diff = abs(x0 - x1);
    y_diff = abs(y0 - y1);  
    for(zoom = 16; zoom >= 4; zoom--){
        lvl_zoom = 16 - zoom;
        x_dif = (tk_engine.width - 1) << (lvl_zoom + 2) >> 2;
        y_dif = (tk_engine.height - 1) << (lvl_zoom + 2) >> 2;
        if((x_diff < x_dif) && (y_diff < y_dif)) {
            if (zoom <= 8)
                zoom++;
            return zoom;
        }
    }
    return -1;
}

/*static int tk_calc_zoom(double lat0, double lon0, double lat1, double lon1)
{
    int x0, y0, x1, y1;
    int zoom, lvl_zoom;
    int x_dif, y_dif, x_diff, y_diff;

	x0 = tk_lon_to_x(lon0);
	y0 = tk_lat_to_y(lat0);
	x1 = tk_lon_to_x(lon1);
	y1 = tk_lat_to_y(lat1);
    x_diff = abs(x0 - x1);
    y_diff = abs(y0 - y1);  
    for(zoom = 16; zoom >= 8; zoom--){
        lvl_zoom = 16 - zoom;
        x_dif = (tk_engine.width - 1) << (lvl_zoom + 2) >> 2;
        y_dif = (tk_engine.height - 1) << (lvl_zoom + 2) >> 2;
        if((x_diff < x_dif) && (y_diff < y_dif)) {
            return zoom;
        }
    }
    return -1;
}*/

int tk_set_scope(double lat0, double lon0, double lat1, double lon1, int w, int h)
{
    int zoom = 0;
    double center_x, center_y;
    struct tk_latlon latlon;
    struct tk_point point;

    center_y = (lon0 + lon1) / 2;
    center_x = (lat0 + lat1) / 2;
    if ((zoom = tk_calc_zoom(lat0, lon0, lat1, lon1)) < 0)
        return -1;
    if(tk_move_latlonzoom(center_x, center_y, zoom) < 0)
        return -1;

    latlon.lat = lat0;
    latlon.lon = lon0;
    tk_latlon2scr(&latlon, &point);
    if ((point.x < w) || (point.y < h) || (tk_engine.width - point.x < w) || (tk_engine.height - point.y < h))
        tk_zoom_out();

    latlon.lat = lat1;
    latlon.lon = lon1;
    tk_latlon2scr(&latlon, &point);
    if ((point.x < w) || (point.y < h) || (tk_engine.width - point.x < w) || (tk_engine.height - point.y < h))
        tk_zoom_out();
    return 0;
}
int tk_move_latlon(double lat, double lon)
{
	int temp_x;
	int temp_y;

    if (lon < (MIN_LON) || lon > (MAX_LON)) {
        LOG_INFO("longitude is out of range: %f\n", lon);
        return -1;
    }
    if (lat < (MIN_LAT) || lat > (MAX_LAT)) {
        LOG_INFO("latitude is out of range: %f\n", lat);
        return -1;
    }

	temp_x = tk_lon_to_x(lon);
	temp_y = tk_lat_to_y(lat);

	if (temp_x < 0 || temp_y < 0) {
		return -1;
	}

    if (TK_X_IN_CHINA(temp_x)) {
        tk_engine.center_x = temp_x;
    }
    if (TK_Y_IN_CHINA(temp_y)) {
        tk_engine.center_y = temp_y;
    }

    calculate_box();

    return 0;
}

#define TK_MOVE_MANAGER_PIXEL2COORD(p) (p << (8 + GSCALE_BASE - tk_engine.current_z - TILE_SIZE_EXP) >> 8)

void tk_move_delta(int dx, int dy)
{
    int new_x = tk_engine.center_x + TK_MOVE_MANAGER_PIXEL2COORD(dx);
    int new_y = tk_engine.center_y + TK_MOVE_MANAGER_PIXEL2COORD(dy);

    if (TK_X_IN_CHINA(new_x)) {
        tk_engine.center_x = new_x;
    }
    if (TK_Y_IN_CHINA(new_y)) {
        tk_engine.center_y = new_y;
    }
    calculate_box();
}


/* ============================================
 * download mapdata files' interfaces
 * ============================================ */

int tk_get_region_stat(int rid, int *ptotal_size, int *pdownloaded_size)
{
    unsigned char* buff;
    unsigned short int byte_num;
    int filesize;
    unsigned char* verifycode;
    int total_size;
    int downloaded_size;
    char map_data_path[TK_MAX_PATH];
    char chk_data_path[TK_MAX_PATH];

    unsigned char *cur_pointer;
    int i;
    char j = 0;
    char k = 0;
    unsigned char *coder_code;
    unsigned char *tk_buffer_tile_index;
    int coder_length = 0;
    int index_length = 0;
    int offset;
    int length;
    int is_in_block;
    int map_data_bias, pos, A_num, B_num, C_num;
    int tile_num;
    int pre_pos, tile_size;
    unsigned char tail;
    FILE *map_data;
    FILE *chk_fp;
    unsigned char chk_ver[8];
    unsigned char data_ver[6];

    strcpy(map_data_path, tk_get_region_path(rid));

    lostdata_idx = 0; /* need to be reset to 0 */
    if (access(map_data_path, 0) == 0) {
        if ((filesize = get_datafile_size(map_data_path)) < 0) {
            return -1;
        }
        sprintf(chk_data_path, "%s%s", map_data_path, ".chk");
        if (access(chk_data_path, 0) == 0) {
            chk_fp = fopen(chk_data_path, "rb");
            if (chk_fp == NULL) {
                //printf("%s %d: file open fail.\n", __FILE__, __LINE__);
                return -1;
            }
            else {
                fread(chk_ver, 1, 8, chk_fp);
                byte_num = (chk_ver[7] << 8) | chk_ver[6]; /* the byte order is changed */ 
            }
        }
        else { /* if checksum file doesn't exist and the data file exist */
//            printf("%s %d:Doesn't have checksum file\n", __FILE__, __LINE__);
            *ptotal_size = filesize; 
            *pdownloaded_size = filesize; 
            return 0;
        } 
        map_data = fopen(map_data_path, "rb");
        if (map_data == NULL) {
            //                printf("%s %d: Open file %s failed!\n", __FILE__, __LINE__, map_data_path);
            return -1;
        } 

        buff = malloc(6);
        if (buff == NULL) {
            //                printf("%s %d: memory alloc fail\n", __FILE__, __LINE__);
            fclose(map_data);
            return -1;
        }
        fread(buff, 1, 6, map_data);

        coder_length = GETNUM3B(buff);
        index_length = GETNUM3B(buff+3);
        map_data_bias = coder_length + index_length + 6;

        coder_code = malloc(coder_length);    /* free in the end of function */
        if (coder_code == NULL) {
            //                printf("%s %d: memory malloc fail.\n", __FILE__, __LINE__);
            fclose(map_data);
            fclose(chk_fp);
            free(buff);
            return -1;
        }
        fread(coder_code, 1, coder_length, map_data);
        /* get the data's version number */
        memcpy(data_ver, coder_code, 6);

        if (memcmp(data_ver, chk_ver, 6) != 0) { 
            LOG_DBG("The chk file and data file 's version don't match\n");
            *ptotal_size = filesize; 
            *pdownloaded_size = filesize; 
            fclose(map_data);
            fclose(chk_fp);
            free(buff);
            free(coder_code);
            return 0;
        }

        verifycode = malloc(byte_num);
        if (verifycode == NULL) {
            //printf("%s %d: memory alloc fail.\n", __FILE__, __LINE__);
            fclose(chk_fp);
            return -1;
        }
        fread(verifycode, 1, byte_num, chk_fp);
        fclose(chk_fp); 

        tk_buffer_tile_index = malloc(index_length);    /* free in the end of function */
        if (tk_buffer_tile_index == NULL) {
            //                printf("%s %d: memory malloc fail.\n", __FILE__, __LINE__);
            fclose(map_data);
            free(buff);
            free(coder_code);
            return -1;
        }
        fread(tk_buffer_tile_index, 1, index_length, map_data);

        data_buffer_offset = 0;
        fread(data_buffer, 1, DATA_BUFF_LEN, map_data);

        cur_pointer = tk_buffer_tile_index;
        get_tile_num(tk_buffer_tile_index, &A_num, &B_num, &C_num, index_length);
        tile_num = A_num + B_num + C_num;   /* tiles' total num */

        offset = 0;
        length = 0;
        downloaded_size = 0;
        total_size = 0;
        is_in_block = 0;
        cur_pointer = tk_buffer_tile_index + 6 * 2 + 3;
        pos = GETNUM3B(cur_pointer);
        pre_pos = pos;
        assert(pre_pos == 0);
        /* need 2 redundant bits in LEVEL_A's tile */
        for (i = 1; i < A_num; i++) {
            cur_pointer = tk_buffer_tile_index + 6 * (2 + i) + 3;
            get_tile_info(map_data, cur_pointer, map_data_bias, pre_pos, 
                    &pos, &tail, &tile_size, &total_size);        
            j = verifycode[(i+2-1)/8]; /* the (i+2-1)th tile */
            k = (i+2-1)% 8;
            add_lostdata(rid, tail, j, k, map_data_bias, pre_pos, 
                    tile_size, &downloaded_size, &offset, &length, &is_in_block);
            pre_pos = pos;
        }
        /* read LEVEL_B's first tile */
        cur_pointer = tk_buffer_tile_index + 6 * (4 + i) + 3;
        get_tile_info(map_data, cur_pointer, map_data_bias, pre_pos, 
                &pos, &tail, &tile_size, &total_size);        
        j = verifycode[(i+2-1)/8]; /* the (i+2-1)th tile */
        k = (i+2-1)% 8;
        add_lostdata(rid, tail, j, k, map_data_bias, pre_pos, 
                tile_size, &downloaded_size, &offset, &length, &is_in_block);
        pre_pos = pos;
        i++;
        /* need 4 redundant bits in LEVEL_B's tile */
        /* process the tiles in LEVEL_B */
        for ( ; i < A_num + B_num; i++) {
            cur_pointer = tk_buffer_tile_index + 6 * (4 + i) + 3;
            get_tile_info(map_data, cur_pointer, map_data_bias, pre_pos, 
                    &pos, &tail, &tile_size, &total_size);        
            j = verifycode[(i+4-1)/8]; /* the (i+4-1)th tile */
            k = (i+4-1)% 8;
            add_lostdata(rid, tail, j, k, map_data_bias, pre_pos, 
                    tile_size, &downloaded_size, &offset, &length, &is_in_block);
            pre_pos = pos;
        }
        if (i + 4 < index_length/6) {
            /* read LEVEL_C's first tile */
            cur_pointer = tk_buffer_tile_index + 6 * (6 + i) + 3;
            get_tile_info(map_data, cur_pointer, map_data_bias, pre_pos, 
                    &pos, &tail, &tile_size, &total_size);        
            j = verifycode[(i+4-1)/8]; /* the (i+4-1)th tile */
            k = (i+4-1)% 8;
            add_lostdata(rid, tail, j, k, map_data_bias, pre_pos, 
                    tile_size, &downloaded_size, &offset, &length, &is_in_block);
            pre_pos = pos;
            i++;
            /* need 6 redundant bits in LEVEL_C's tile */
            /*process the tiles in LEVEL_C */
            for ( ; i < A_num + B_num + C_num ; i++) {
                cur_pointer = tk_buffer_tile_index + 6 * (6 + i) + 3;
                get_tile_info(map_data, cur_pointer, map_data_bias, pre_pos, 
                        &pos, &tail, &tile_size, &total_size);        
                j = verifycode[(i+6-1)/8]; /* the (i+6-1)th tile */
                k = (i+6-1)% 8;
                add_lostdata(rid, tail, j, k, map_data_bias, pre_pos, 
                        tile_size, &downloaded_size, &offset, &length, &is_in_block);
                pre_pos = pos;
            }
            //                printf("map bias %d tile%d, off %d, len %d\n",map_data_bias,  i-1, pre_pos + map_data_bias, filesize-pre_pos-map_data_bias);
            //                printf("Total tile num is %d\n", index_length/6);
            fseek(map_data, filesize-1, SEEK_SET);

            fread(&tail, 1, 1, map_data);

            total_size += filesize - pre_pos - map_data_bias;
            tile_size = filesize - pre_pos - map_data_bias; /* fixed bug */
            j = verifycode[(i+6-1)/8]; /* the (i+6-1)th tile */
            k = (i+6-1)% 8;
            add_lostdata(rid, tail, j, k, map_data_bias, pre_pos, 
                    tile_size, &downloaded_size, &offset, &length, &is_in_block);
            if (is_in_block == 1) {
                is_in_block = 0;
                if (lostdata_idx < TK_LOST_DATA_PIECE) { 
                    lostdata[lostdata_idx].rid = rid;
                    lostdata[lostdata_idx].offset = offset;
                    lostdata[lostdata_idx].length = length;
                    lostdata_idx++;
                }
            }
        } else {
            //                printf("map bias %d tile%d, off %d, len %d\n",map_data_bias,  i-1, pre_pos + map_data_bias, filesize-pre_pos-map_data_bias);
            //                printf("Total tile num is %d\n", index_length/6);

            fseek(map_data, filesize-1, SEEK_SET);

            fread(&tail, 1, 1, map_data);

            total_size += filesize - pre_pos - map_data_bias;
            tile_size = filesize - pre_pos - map_data_bias; /* fixed bug */
            j = verifycode[(i+4-1)/8]; /* the (i+4-1)th tile */
            k = (i+4-1)% 8;
            add_lostdata(rid, tail, j, k, map_data_bias, pre_pos, 
                    tile_size, &downloaded_size, &offset, &length, &is_in_block);
            if (is_in_block == 1) {
                is_in_block = 0;
                if (lostdata_idx < TK_LOST_DATA_PIECE) { 
                    lostdata[lostdata_idx].rid = rid;
                    lostdata[lostdata_idx].offset = offset;
                    lostdata[lostdata_idx].length = length;
                    lostdata_idx++;
                }
            }
        }
        *ptotal_size = total_size + map_data_bias;
        *pdownloaded_size = map_data_bias + downloaded_size;
        free(buff);
        free(coder_code);
        free(tk_buffer_tile_index);
        fclose(map_data);
        free(verifycode);
        return 0;
    } else {
        /* the data file doesn't exist */
        if (lostdata_idx < TK_LOST_DATA_PIECE) { 
            lostdata[lostdata_idx].rid = rid;
            lostdata[lostdata_idx].offset = 0;
            lostdata[lostdata_idx].length = 0;
            lostdata_idx++;
        }
        *ptotal_size = 0;
        *pdownloaded_size = 0;
        return 0;
    }
}

#define TILE_BUFF_SIZE 4096
#define MAX_LINE_LEN 1024
/* Generate the empty map data file according to the metadata */
int tk_init_region(char *metafile, int rid)
{
    FILE *fp;
    struct stat statbuf;
    int i;
    int size, metasize, tile_num;
    short int byte_num;
    char buf[MAX_LINE_LEN];
    char rname[TK_MAX_PATH];
    char tmp_path[TK_MAX_PATH];
    const char *ename, *pname;
    int coder_length=0, index_length=0;
    unsigned char tail;
    unsigned char *coder_code, *tk_buffer_tile_index, *cur_pointer, *data;
    int map_data_bias, pos, A_num, B_num, C_num, res;
    int pre_pos;
	FILE *map_data = NULL;
    unsigned char buff[6];
    unsigned char *index;
#ifdef TK_S_SYMBIAN
    unsigned char tile_buff[TILE_BUFF_SIZE];
    int buff_pos;
    int tile_size;
#endif

    release_region(rid);
    /* initialize first */
    coder_code = tk_buffer_tile_index = data = NULL;
    /* get the city name and province name according to the region id */
    ename = tk_get_region_name(rid);
    pname = tk_get_provname(rid);

    sprintf(rname,"%s/%s", datapath, pname);
    if (access(rname, 0) < 0) { //judge whether the directory exists
        if (tkmc_mkdir(rname) < 0) {
            //printf("%s %d: %s %s", __FILE__, __LINE__, rname, strerror(errno));
            return -1;
        }
    }
    strcat(rname, "/");
    strcat(rname, ename);
    strcat(rname, ".dat");
    // printf("%s\n", rname);
    sprintf(tmp_path, "%s.tmp", rname);
    if ((fp = fopen(tmp_path, "w+b")) == NULL) {
        //printf("%s %d: %s", __FILE__, __LINE__, strerror(errno));
        return -1;
    }

    /* get the size of this data file! */
    if (stat(metafile, &statbuf) < 0) {
//        printf("%s %d:stat error\n", __FILE__, __LINE__);
        goto err;
    }
    metasize = statbuf.st_size;
    /* printf("metafile size is %d\n", metasize); */
    /* get size end */

    /* parse the temp meta data file */
    map_data = fopen(metafile, "rb");
    if (map_data == NULL){
//       printf("%s, %d: map_data get error\n", __FILE__, __LINE__);
        goto err;
    }

    fread(buff, 1, 4, map_data);
    size = (buff[0] << 24) + (buff[1] << 16) + (buff[2] << 8) + buff[3];

#ifdef TK_S_SYMBIAN
    fread(buff, 1, 6, map_data);
    coder_length = (buff[0]<<16) + (buff[1]<<8) + (buff[2]);
    index_length = (buff[3]<<16) + (buff[4]<<8) + (buff[5]);

    map_data_bias = coder_length + index_length + 6;

    index = malloc(coder_length + index_length);    /* free in the end of function */
    if (index == NULL) {
        goto err;
    }
    fread(index, 1, coder_length + index_length, map_data);

    coder_code = index;
    tk_buffer_tile_index = index + coder_length;

    /* write index */
    fwrite(buff, 1, 6, fp);
    fwrite(index, 1, coder_length + index_length, fp);
   
    A_num = B_num = C_num = 0; 
    cur_pointer = tk_buffer_tile_index;
    A_num = (cur_pointer[9] << 16) | (cur_pointer[10] << 8) | cur_pointer[11];
    res = A_num + 2;
    cur_pointer = tk_buffer_tile_index + res * 6;
    B_num = (cur_pointer[9] << 16) | (cur_pointer[10] << 8) | cur_pointer[11];
    res += B_num + 2;
    if (res < index_length/6) { /* if the C_level tiles exist */
        cur_pointer = tk_buffer_tile_index + res * 6;
        C_num = (cur_pointer[9] << 16) | (cur_pointer[10] << 8) | cur_pointer[11];   
    }

    tile_num = A_num + B_num + C_num;   /* tiles' total num */
    byte_num = (tile_num + 6 + 7)/8;    /* more 6 bit added */

    //printf("bias is %d %d %d\n", map_data_bias,  byte_num +4, tile_num);
    //printf("metafile %d %d tile_num %d\n", metasize, map_data_bias + byte_num + 4, tile_num);
    assert((map_data_bias + byte_num + 4) == metasize);

    data = malloc(byte_num); /* read the check sum data */
    if (data == NULL) {
//        printf("%s %d: memory malloc fail.\n", __FILE__, __LINE__);
        goto err;
    }
    fread(data, 1, byte_num, map_data);
    fclose(map_data);

    assert(map_data_bias == ftell(fp));
    /* write tile */
    cur_pointer = tk_buffer_tile_index + 6 * 2 + 3;
    pos = (cur_pointer[0] << 16) | (cur_pointer[1] << 8) | cur_pointer[2];
    pre_pos = pos;
    assert(pos == 0);
    buff_pos = 0;

    /* the codes below use i as index，remain 2 bits at the start */
    for (i = 1; i < A_num; i++) {
        cur_pointer = tk_buffer_tile_index + 6 * (2 + i) + 3;
        pos = (cur_pointer[0] << 16) | (cur_pointer[1] << 8) | cur_pointer[2];
        //printf("pos is %d\n", pos);
        tail = data[(i+2-1)/8] & (1<<(i+2-1)%8);    //get the related bits
        tail = tail >> (i+2-1)%8;
        //printf("tail %X data %X\n", tail, data[(i-1)/8]);
        tile_size = pos - pre_pos;
        if (TILE_BUFF_SIZE - buff_pos > tile_size) {
            tile_buff[buff_pos + tile_size - 1] = tail;
            buff_pos += tile_size;
        } else {
            fwrite(tile_buff, 1, buff_pos, fp);
            buff_pos = 0;
            tile_buff[buff_pos + tile_size - 1] = tail;
            buff_pos += tile_size;
        }
        pre_pos = pos;
    }
    cur_pointer = tk_buffer_tile_index + 6 * (4 + i) + 3;//4+i not 2+i
    pos = (cur_pointer[0] << 16) | (cur_pointer[1] << 8) | cur_pointer[2];
    tail = data[(i+2-1)/8] & (1<<(i+2-1)%8);    //get the related bits
    tail = tail >> (i+2-1)%8;
    tile_size = pos - pre_pos;
    if (TILE_BUFF_SIZE - buff_pos > tile_size) {
        tile_buff[buff_pos + tile_size - 1] = tail;
        buff_pos += tile_size;
    } else {
        fwrite(tile_buff, 1, buff_pos, fp);
        buff_pos = 0;
        tile_buff[buff_pos + tile_size - 1] = tail;
        buff_pos += tile_size;
    }
    pre_pos = pos;
    //printf("pos is %d\n", pos);
    //printf("tail %X data %X\n", tail, data[(i-1)/8]);
    i++;
    /* process LEVEL_B's tiles */
    for ( ; i < A_num + B_num; i++) {
        cur_pointer = tk_buffer_tile_index + 6 * (4 + i) + 3;
        pos = (cur_pointer[0] << 16) | (cur_pointer[1] << 8) | cur_pointer[2];
        tail = data[(i+4-1)/8] & (1<<(i+4-1)%8);    //get the related bits
        tail = tail >> (i+4-1)%8;
        tile_size = pos - pre_pos;
        if (TILE_BUFF_SIZE - buff_pos > tile_size) {
            tile_buff[buff_pos + tile_size - 1] = tail;
            buff_pos += tile_size;
        } else {
            fwrite(tile_buff, 1, buff_pos, fp);
            buff_pos = 0;
            tile_buff[buff_pos + tile_size - 1] = tail;
            buff_pos += tile_size;
        }
        pre_pos = pos;
    }
    if (i < tile_num) {
        cur_pointer = tk_buffer_tile_index + 6 * (6 + i) + 3;
        pos = (cur_pointer[0] << 16) | (cur_pointer[1] << 8) | cur_pointer[2];
        tail = data[(i+4-1)/8] & (1<<(i+4-1)%8);    //get the related bits
        tail = tail >> (i+4-1)%8;
        tile_size = pos - pre_pos;
        if (TILE_BUFF_SIZE - buff_pos > tile_size) {
            tile_buff[buff_pos + tile_size - 1] = tail;
            buff_pos += tile_size;
        } else {
            fwrite(tile_buff, 1, buff_pos, fp);
            buff_pos = 0;
            tile_buff[buff_pos + tile_size - 1] = tail;
            buff_pos += tile_size;
        }
        pre_pos = pos;
        i++;
        /* process LEVEL_C's tiles */
        for ( ; i < A_num + B_num + C_num ; i++) {
            cur_pointer = tk_buffer_tile_index + 6 * (6 + i) + 3;
            pos = (cur_pointer[0] << 16) | (cur_pointer[1] << 8) | cur_pointer[2];
            tail = data[(i+6-1)/8] & (1<<(i+6-1)%8);    //get the related bits
            tail = tail >> (i+6-1)%8;
            tile_size = pos - pre_pos;
            if (TILE_BUFF_SIZE - buff_pos > tile_size) {
                tile_buff[buff_pos + tile_size - 1] = tail;
                buff_pos += tile_size;
            } else {
                fwrite(tile_buff, 1, buff_pos, fp);
                buff_pos = 0;
                tile_buff[buff_pos + tile_size - 1] = tail;
                buff_pos += tile_size;
            }
            pre_pos = pos;
        }
        tail = data[(i+6-1)/8] & (1<<(i+6-1)%8);    //get the related bits
        tail = tail >> (i+6-1)%8;
        assert((i+6-1)/8 == byte_num-1);
        pos = size - map_data_bias;
        tile_size = pos - pre_pos;
        if (TILE_BUFF_SIZE - buff_pos > tile_size) {
            tile_buff[buff_pos + tile_size - 1] = tail;
            buff_pos += tile_size;
        } else {
            fwrite(tile_buff, 1, buff_pos, fp);
            buff_pos = 0;
            tile_buff[buff_pos + tile_size - 1] = tail;
            buff_pos += tile_size;
        }
        fwrite(tile_buff, 1, buff_pos, fp);
    } else {
        tail = data[(i+6-1)/8] & (1<<(i+6-1)%8);    //get the related bits
        tail = tail >> (i+6-1)%8;
        assert((i+6-1)/8 == byte_num-1);
        pos = size - map_data_bias;
        tile_size = pos - pre_pos;
        if (TILE_BUFF_SIZE - buff_pos > tile_size) {
            tile_buff[buff_pos + tile_size - 1] = tail;
            buff_pos += tile_size;
        } else {
            fwrite(tile_buff, 1, buff_pos, fp);
            buff_pos = 0;
            tile_buff[buff_pos + tile_size - 1] = tail;
            buff_pos += tile_size;
        }
        fwrite(tile_buff, 1, buff_pos, fp);
    }
#else
    fseek(fp, size - 1, SEEK_SET);
    fwrite("\0", 1, 1, fp);
    rewind(fp);

    fread(buff, 1, 6, map_data);
    coder_length = (buff[0]<<16) + (buff[1]<<8) + (buff[2]);
    index_length = (buff[3]<<16) + (buff[4]<<8) + (buff[5]);

    map_data_bias = coder_length + index_length + 6;

    index = malloc(coder_length + index_length);    /* free in the end of function */
    if (index == NULL) {
        goto err;
    }
    fread(index, 1, coder_length + index_length, map_data);

    coder_code = index;
    tk_buffer_tile_index = index + coder_length;

    /* write index */
    fwrite(buff, 1, 6, fp);
    fwrite(index, 1, coder_length + index_length, fp);

    A_num = B_num = C_num = 0; 
    cur_pointer = tk_buffer_tile_index;
    A_num = (cur_pointer[9] << 16) | (cur_pointer[10] << 8) | cur_pointer[11];
    res = A_num + 2;
    cur_pointer = tk_buffer_tile_index + res * 6;
    B_num = (cur_pointer[9] << 16) | (cur_pointer[10] << 8) | cur_pointer[11];
    res += B_num + 2;
    if (res < index_length/6) { /* if the C_level tiles exist */
        cur_pointer = tk_buffer_tile_index + res * 6;
        C_num = (cur_pointer[9] << 16) | (cur_pointer[10] << 8) | cur_pointer[11];   
    }

    tile_num = A_num + B_num + C_num;   /* tiles' total num */
    byte_num = (tile_num + 6 + 7)/8;    /* more 6 bit added */

    //printf("bias is %d %d %d\n", map_data_bias,  byte_num +4, tile_num);
    //printf("metafile %d %d tile_num %d\n", metasize, map_data_bias + byte_num + 4, tile_num);
    assert((map_data_bias + byte_num + 4) == metasize);

    data = malloc(byte_num); /* read the check sum data */
    if (data == NULL) {
//        printf("%s %d: memory malloc fail.\n", __FILE__, __LINE__);
        goto err;
    }
    fread(data, 1, byte_num, map_data);
    fclose(map_data);

    assert(map_data_bias == ftell(fp));
    /* write tile */
    cur_pointer = tk_buffer_tile_index + 6 * 2 + 3;
    pos = (cur_pointer[0] << 16) | (cur_pointer[1] << 8) | cur_pointer[2];
    pre_pos = pos;
    assert(pos == 0);

    /* the codes below use i as index，remain 2 bits at the start*/
    for (i = 1; i < A_num; i++) {
        cur_pointer = tk_buffer_tile_index + 6 * (2 + i) + 3;
        pos = (cur_pointer[0] << 16) | (cur_pointer[1] << 8) | cur_pointer[2];
        //printf("pos is %d\n", pos);
        tail = data[(i+2-1)/8] & (1<<(i+2-1)%8);    //get the related bits
        tail = tail >> (i+2-1)%8;
        //printf("tail %X data %X\n", tail, data[(i-1)/8]);
        fseek(fp, pos + map_data_bias - 1, SEEK_SET);
        fwrite(&tail, 1, 1, fp);
        //getchar();
    }   
    cur_pointer = tk_buffer_tile_index + 6 * (4 + i) + 3;//4+i not 2+i
    pos = (cur_pointer[0] << 16) | (cur_pointer[1] << 8) | cur_pointer[2];
    tail = data[(i+2-1)/8] & (1<<(i+2-1)%8);    //get the related bits
    tail = tail >> (i+2-1)%8;
    //printf("pos is %d\n", pos);
    //printf("tail %X data %X\n", tail, data[(i-1)/8]);
    fseek(fp, pos + map_data_bias - 1, SEEK_SET);
    fwrite(&tail, 1, 1, fp);
    i++;
    /* process LEVEL_B's tiles */
    for ( ; i < A_num + B_num; i++) {
        cur_pointer = tk_buffer_tile_index + 6 * (4 + i) + 3;
        pos = (cur_pointer[0] << 16) | (cur_pointer[1] << 8) | cur_pointer[2];
        tail = data[(i+4-1)/8] & (1<<(i+4-1)%8);    //get the related bits
        tail = tail >> (i+4-1)%8;
        fseek(fp, pos + map_data_bias - 1, SEEK_SET);  
        fwrite(&tail, 1, 1, fp);
    }
    if (i < tile_num) {
        cur_pointer = tk_buffer_tile_index + 6 * (6 + i) + 3;
        pos = (cur_pointer[0] << 16) | (cur_pointer[1] << 8) | cur_pointer[2];
        tail = data[(i+4-1)/8] & (1<<(i+4-1)%8);    //get the related bits
        tail = tail >> (i+4-1)%8;
        fseek(fp, pos + map_data_bias - 1, SEEK_SET);  
        fwrite(&tail, 1, 1, fp);
        i++;
        /* process LEVEL_C's tiles */
        for ( ; i < A_num + B_num + C_num ; i++) {
            cur_pointer = tk_buffer_tile_index + 6 * (6 + i) + 3;
            pos = (cur_pointer[0] << 16) | (cur_pointer[1] << 8) | cur_pointer[2];
            tail = data[(i+6-1)/8] & (1<<(i+6-1)%8);    //get the related bits
            tail = tail >> (i+6-1)%8;
            fseek(fp, pos + map_data_bias - 1, SEEK_SET);  
            fwrite(&tail, 1, 1, fp);
        }
        fseek(fp, size - 1, SEEK_SET);
        tail = data[(i+6-1)/8] & (1<<(i+6-1)%8);    //get the related bits
        tail = tail >> (i+6-1)%8;
        //assert((i+6-1)/8 == byte_num-1);
        fwrite(&tail, 1, 1, fp);
    } else {
        fseek(fp, size - 1, SEEK_SET);
        tail = data[(i+4-1)/8] & (1<<(i+4-1)%8);    //get the related bits
        tail = tail >> (i+4-1)%8;
        assert((i+4-1)/8 == byte_num-1);
        fwrite(&tail, 1, 1, fp);
    }
#endif /* TK_S_SYMBIAN */
    fclose(fp); 

    /* write tempory check file of map data*/
    sprintf(buf, "%s.chk", rname);
    if ((fp = fopen(buf, "w+b")) == NULL) {
//       printf("%s %d: Open file %s fail\n", __FILE__, __LINE__, buf);
       exit(1);
    }
    fwrite(coder_code, 1, 6, fp);
    fwrite(&byte_num, 1, 2, fp);
    fwrite(data, 1, byte_num, fp);
    /* end of writing tempory check file */

    free(index);
    free(data);
    fclose(fp);
    rename(tmp_path, rname);
    return 0;
err:
    if (index != NULL) {
        free(index);
    }
    if (data != NULL) {
        free(data);
    }
    if (map_data != NULL) {
        fclose(map_data);
    }
    fclose(fp);
    return -1;    
}

int tk_write_region(int rid, int off, int len, char* buf)
{
    FILE *fpdata;
    const char *dataname;
    int flag = 1;

    dataname = tk_get_region_path(rid);
    if (dataname == NULL) {
        return -1;
    }
    fpdata = find_fp(rid);
    if (fpdata == NULL) {
        flag = 0;
        if ((fpdata = fopen(dataname, "r+b")) == NULL) {
            perror("error");
            return -1;
        }
    }
    if (fseek(fpdata, off, SEEK_SET) < 0) {
        perror("error");
        return -1;
    }
    if (fwrite(buf, 1, len, fpdata) < len) {
        perror("error");
        return -1;
    }
    fflush(fpdata);
    if (flag == 0) {
        fclose(fpdata);
    }
    return 0;
}

/* ============================================ 
 * Misc helper routines 
 * =========================================== */

int tk_get_matrix_size(int w, int h) 
{
    int size;
#ifdef TK_BPP_16
    size = w * h * 2;
#else 
    size = w * h * 4;
#endif    

#ifdef NEED_BMP_HEADER
    return size + BMP_HEADER_SIZE;
#else
    return size;
#endif
}

const char *tk_get_region_path(int rid)
{
    static char reg_path[TK_MAX_PATH];
    int prov_idx;

    if ((rid < 0 && rid != -3) || rid >= reg_num) {
        return NULL;
    }

    if (rid == -3) {
        sprintf(reg_path, "%s/%s/%s.dat", datapath, nat.pename, nat.rename);
    } else {
        prov_idx = city_list[reg_list[rid].city_idx].prov_idx;
        sprintf(reg_path, "%s/%s/%s.dat", datapath, prov_list[prov_idx].ename, reg_list[rid].ename);
    }
    return reg_path;
}

int tk_get_region_version(int rid, unsigned char *rversion)
{
    return get_rversion(rid, rversion);
}

int tk_get_cur_rversion(unsigned char *rversion)
{
    return get_cur_rversion(rversion);
}
int tk_get_region_id(char *rname)
{
    int i;

    if (rname != NULL ) {
        if (strcmp(rname, "quanguo") == 0) {
            return nat.rid;
        }
        for (i = 0; i < reg_num; i++) {
            if (strcmp(reg_list[i].ename, rname) == 0) {
                return reg_list[i].rid;
            }
        }
    }

    return -1;
}

const char *tk_get_region_name(int rid)
{
    if ((rid < 0 && rid != -3) || rid >= reg_num) {
        return NULL;
    }

    if (rid == -3) {
        return nat.rename;
    }
    return reg_list[rid].ename; 
}

const char *tk_get_provname(int rid)
{
    static char prov_name[TK_MAX_NAME];
    int prov_idx;

    if ((rid < 0 && rid != -3) || rid >= reg_num) {
        return NULL;
    }

    if (rid == -3) {
        sprintf(prov_name, "%s", nat.pename);
    } else {
        prov_idx = city_list[reg_list[rid].city_idx].prov_idx;
        sprintf(prov_name, "%s", prov_list[prov_idx].ename);
    }
    return prov_name;
}

char **tk_get_citylist(char *pname, int *num_of_cities)
{
    int i;
    int fcity;
    int cnum;
    int c_idx;
    char **cities;

    for (i = 1; i <= prov_num ; i++) {
        if (strcmp(prov_list[i].name, pname) == 0) {
            fcity = prov_list[i].city_idx;
            break;
        } 
    }

    /* No provice is found */
    if (i > prov_num) {
        return NULL;
    }

    /* find num of city */
    cnum = 1;
    c_idx = fcity;
    while ((c_idx = city_list[c_idx].next) != -1) {
        cnum++;
    }
    
    *num_of_cities = cnum;
    cities = (char **)xmalloc(sizeof(char *) * cnum);

    cnum = 0;
    c_idx = fcity;
    do {
        cities[cnum] = (char *)xmalloc(sizeof(char *) * (strlen(city_list[c_idx].name) + 1));
        sprintf(cities[cnum], "%s", city_list[c_idx].name);
        cnum++;
    } while ((c_idx = city_list[c_idx].next) != -1);

    return cities;
}

/* map scale to the meters, use the zoom level as the index to get the value */
static const int scale2meter[19] = {0, 0, 0, 0, 200000, 100000, 50000, 25000,
    20000, 10000, 5000, 2000, 1000, 500, 200, 100, 50, 25, 10};

double tk_meters_per_pixel(double lat, short int z) {
    if (z <= LEVEL_SKIP)
        z--;    
	double meters_per_pixel = MATH_PI * 2 * EARTH_RADIUS / (1 << z) / TILE_SIZE;
    meters_per_pixel = meters_per_pixel * (cos(lat * MATH_PI / 180));
	return meters_per_pixel;
}

static double tk_round(double num) {
    double floor_num = floor(num);
    if (num < floor_num + 0.5) {
        return floor_num;
    }
    return floor_num + 1;
}

int tk_scale_in_pixels(double lat, short int z)
{
    double meters_per_pixel;
	double scale_in_meters;
	int scale_in_pixels;
	if (z < 4 || z > 18) {
        return -1;
    }
    if (z <= LEVEL_SKIP)
        z--;
    meters_per_pixel = tk_meters_per_pixel(lat, z);
    scale_in_meters = scale2meter[z];
    scale_in_pixels = (int) tk_round(scale_in_meters / meters_per_pixel);
    return scale_in_pixels;
}

int tk_scale_in_meters(short int z) 
{
    if (z <= LEVEL_SKIP)
        z--;
	return scale2meter[z];
}

int tk_get_cityid(const char *cname) 
{
    int i;

    if (strcmp(cname, "全国") == 0)
        return -3;

    for (i = 1; i <= city_num; i++) {
        if (city_list[i].name != NULL) {
            if (strcmp(cname, city_list[i].name) == 0)
                break;
        }
    }
    if (i > city_num)
        return -1;
    return city_list[i].cid;
}

/**
 * purpose: get the region info from the region id
 * @rid: region id
 * return value: the pointer to the string of the region info 
 * in the format "中文名字, English name, file size, city ename". 
 */
char *tk_get_region_info(int rid) 
{
    static char region_info[MAX_LINE_LEN];

    if ((rid < 0 && rid != -3) || rid >= reg_num)
        return NULL;
    
    if (rid == -3) {
        sprintf(region_info, "%s,%s,%d,%s", nat.rname, nat.rename, nat.file_size, nat.cname); 
    } else {
        sprintf(region_info, "%s,%s,%d,%s", reg_list[rid].name, reg_list[rid].ename, reg_list[rid].file_size, city_list[reg_list[rid].city_idx].name); 
    }
    return region_info;
}

char *tk_get_city_info(int city_id)
{
    static char city_info[MAX_LINE_LEN];

    if ((city_id < 0 && city_id != -3) || city_id > city_num)
       return NULL; 
    if (city_id == -3) {
        sprintf(city_info, "%s,%s,%f,%f,%d,%s,%s", nat.cname, nat.cename, nat.center.lat, nat.center.lon, nat.zoom_level, nat.cname, nat.cename);
    } else {
        if (city_list[city_id].cid != -1)
            sprintf(city_info, "%s,%s,%f,%f,%d,%s,%s", city_list[city_id].name, city_list[city_id].ename, 
                city_list[city_id].center.lat, city_list[city_id].center.lon, city_list[city_id].zoom_level, 
                prov_list[city_list[city_id].prov_idx].name, prov_list[city_list[city_id].prov_idx].ename);     
        else return NULL;
    }
    return city_info;
}

char *tk_get_regionlist(int cityid)
{
    int i, rid;
    char *regionlist;

    if (cityid != -3 && (cityid <= 0 || cityid > city_num || cityid == 78 || cityid == 139)) {
        LOG_ERR("Invalid city id %d!\n", cityid);
        return NULL;
    }
   
    regionlist = (char *)malloc(sizeof(char) * MAX_LINE_LEN);
    regionlist[0] = '\0';
 
    if (cityid == -3) {
        strcat(regionlist, nat.rename);
        return regionlist;
    }
   
    for (i = 0; i < city_num; i++)
        if (city_list[i].cid == cityid)
            break;

    rid = city_list[i].reg_idx;
    while (rid != -1) {
        strcat(regionlist, reg_list[rid].ename);
        strcat(regionlist, ",");
        rid = reg_list[rid].next;
    }

    return regionlist;
}

char *tk_get_eprovincelist()
{
	int i;
    char *provinceinfo = (char *)malloc(sizeof(char) * MAX_LINE_LEN);
    *provinceinfo = '\0';

    for (i = 1; i <= prov_num; i++) {
        if (i == 1)
            sprintf(provinceinfo, "%s,%s", prov_list[i].name, prov_list[i].ename);
        else 
            sprintf(provinceinfo, "%s,%s,%s", provinceinfo, prov_list[i].name, prov_list[i].ename);
    }
    return provinceinfo;
}

char *tk_get_provincelist()
{
	int i;
    char *provinceinfo = (char *)malloc(sizeof(char) * MAX_LINE_LEN);

    *provinceinfo = '\0';
    for (i = 1; i <= prov_num - 2; i++) {
        if (i == 1)
            sprintf(provinceinfo, "%s", prov_list[i].name);
        else 
            sprintf(provinceinfo, "%s,%s", provinceinfo, prov_list[i].name);
    }
    return provinceinfo;
}

void tk_remove_city_data(const char *cname)
{
    int rid, cid;
    char *rlist;
    char *rname;
    cid = tk_get_cityid(cname);
    rlist = tk_get_regionlist(cid);
    rname = strtok(rlist, ",");
    do {
        rid = tk_get_region_id(rname);
        remove(tk_get_region_path(rid));        
        release_region(rid);
        release_tb();
    } while ((rname = strtok(NULL, ",")) != NULL);
    free(rlist);
}

void tk_remove_region_data(int rid)
{
    const char *tmp;
    char path[TK_MAX_PATH];
    release_region(rid);
    tmp = tk_get_region_path(rid);
    sprintf(path, "%s", tmp);
    remove(path);
    strcat(path, ".chk");
    remove(path);
}

char *tk_get_data_root()
{
    return datapath;
}

/**
* 将经纬度转化为全局坐标，再得到rid，再得到cityid
* purpose: return the city id according latitude and longitude
* [in]: pos longitude and latitude
* return value: NULL if can not get the city id
*/
int tk_get_city_id(struct tk_latlon pos) {
    struct tk_point pos_point;
    int cur_pos_region_id;

    //if (tk_get_zoom() <= TK_NATIONAL_LEVEL_A) {
      //  return -3;
    //}

    pos_point.x = tk_lon_to_x(pos.lon);
    pos_point.y = tk_lat_to_y(pos.lat);
    cur_pos_region_id = tk_get_rid_by_point(&pos_point);

    if (cur_pos_region_id == -1) {
        return -1;
    }

    return city_list[reg_list[cur_pos_region_id].city_idx].cid;
}

int tk_get_current_city_id() 
{
    struct tk_point pos_point;
    int cur_pos_region_id;
    int i;
    pos_point.x = tk_engine.min_tile_bbox.right ;//tk_engine.center_x;
    pos_point.y = tk_engine.min_tile_bbox.top;//tk_engine.center_y;
    cur_pos_region_id = tk_get_rid_by_point(&pos_point);
    for (i = 0; i< reg_num; i++) {
        if (reg_list[i].rid == cur_pos_region_id)
            return city_list[reg_list[i].city_idx].cid;
    }
    return -1;
}

/*
 * Mercator投影的坐标到屏幕坐标
 * 
 */
double tk_y_to_lat(int y) {
    double temp = exp((MATH_PI) * 2 * -((double)y / (1 << (16 - 1)) / TILE_SIZE - 1));
    double lat = asin((temp - 1) / (temp + 1));
    return lat * 180 / MATH_PI;
}

double tk_x_to_lon(int x) {
    double lon = ((double)x / (TILE_SIZE * (1 << 16))) * 360;
    return lon - 180;
}

void tk_latlon2scr(struct tk_latlon *latlon, struct tk_point *point) {
    short int shift;

    point->x = tk_lon_to_x(latlon->lon);
    point->y = tk_lat_to_y(latlon->lat);
    shift = 16 - tk_engine.current_z; //2^tk_engine.tile_size_exp is tile size;
    point->x -= tk_engine.center_x;
    point->y -= tk_engine.center_y;
    point->x = (point->x << 2 >> (shift + 2)) + (tk_engine.width >> 1);
    point->y = (point->y << 2 >> (shift + 2)) + (tk_engine.height >> 1);

}

void tk_scr2latlon(struct tk_latlon *latlon, struct tk_point *point) {
    short int shift = 16 - tk_engine.current_z; 

    point->x -= tk_engine.width >> 1;
    point->y -= tk_engine.height >> 1;
    point->x = point->x << (shift + 2) >> 2;
    point->y = point->y << (shift + 2) >> 2;
    point->x += tk_engine.center_x;
    point->y += tk_engine.center_y;
    latlon->lat = tk_y_to_lat(point->y);
    latlon->lon = tk_x_to_lon(point->x);
}

void tk_latlon_transform(double lon, double lat, int wg_heit, int wg_week, unsigned int wg_time, double* out_lon, double* out_lat) {
	static int wg_flag = 0;
	unsigned int wg_lng = (unsigned int)(lon * (double)3600 * (double)1024);
	unsigned int wg_lat = (unsigned int)(lat * (double)3600 * (double)1024);
	unsigned int china_lng = 0;
	unsigned int china_lat = 0;
	unsigned int ret = 0;
	if(wg_flag == 0) {
		wgtochina_lb(wg_flag, wg_lng, wg_lat, wg_heit, wg_week, wg_time, &china_lng, &china_lat);
		wg_flag = 1;
	}
	ret = wgtochina_lb(wg_flag, wg_lng, wg_lat, wg_heit, wg_week, wg_time, &china_lng, &china_lat);
	if(0 == ret) {
	    *out_lon = ((double)china_lng) / (double)(3600 * 1024);
	    *out_lat = ((double)china_lat) / (double)(3600 * 1024);
	} else {
	    *out_lon = 0.0;
	    *out_lat = 0.0;
	}
}

char *tk_get_poi_name(int x, int y, int flag)
{
    refresh_local_feature(x, y,flag);	
    return best_feature.name;
}
void restore_context() {
    struct envelope el;
    tk_engine.center_x = tk_engine.old_x;
    tk_engine.center_y = tk_engine.old_y;
    tk_engine.bl = get_base_level(tk_engine.old_z);
    clean_tile(tk_engine.old_z);
    set_zlevel(tk_engine.old_z);
    get_tile_box(tk_engine.center_x, tk_engine.center_y, tk_engine.bl_dif, &el,TILE_SIZE, TILE_SIZE);
    load_tile(tk_engine.center_x,tk_engine.center_y,&el);
    //draw_tile(tk_engine.center_x,tk_engine.center_y,&el);
    //load_map();
    need_read_whole_map = 1;
}

char *tk_get_poi_namel(double lon, double lat, int zoom, int flag)
{
    int x, y;
    int need_map = 0;
    int city_id;
    static char poiname[128];
    int temp_x, temp_y;
    struct envelope el, bbox;
    int w, h;
    int center_id;
    w = h = 1024;

    //if (tk_get_tile_xy(lon, lat, zoom, &temp_x, &temp_y) < 0)
      //  return NULL;
	x = tk_lon_to_x(lon);
	y = tk_lat_to_y(lat);
    /*if (zoom <= 16) {
        x = x >> (16 - zoom );
        y = y >> (16 - zoom );
    } else {
        x = x << (-16 + zoom );
        y = y << (-16 + zoom );
    }

    clean_tile(zoom);
    tk_engine.old_x = tk_engine.center_x;
    tk_engine.old_y = tk_engine.center_y;
    // delete tile
    tk_engine.bl = get_base_level(zoom);

    set_zlevel(zoom);
    
    //tk_engine.old_x = tk_engine.center_x;
    //tk_engine.old_y = tk_engine.center_y;
    //tk_move_latlonzoom(lat, lon, 16);
    //tk_engine.bl = get_base_level(zoom);

    //set_zlevel(zoom);
    
    //need_map = load_map();
    get_tile_box(temp_x, temp_y, tk_engine.bl_dif, &el, TILE_SIZE, TILE_SIZE);
    //get_tile_box(temp_x, temp_y, 0, &el);
    need_map = load_tile(temp_x,temp_y,&el);
    */
    clean_tile(zoom);
    tk_engine.bl = get_base_level(zoom);
    zoom = tk_engine.bl;
    set_zlevel(zoom);
    //tk_engine.bl = get_base_level(zoom);

    tk_engine.center_x = x;
    tk_engine.center_y = y;
    tk_reinit_gdicontext(w, h);
    calculate_box(w,h);
    get_tile_bbox_from_point(&bbox);
    //increase_tile(&bbox);
    //get_tile_bbox_from_point(&bbox);
    memcpy(&(tk_engine.min_tile_bbox), &tk_engine.cur_bbox, sizeof(struct envelope));
    if (zoom > 16) {
        tk_engine.min_tile_bbox.left = tk_engine.min_tile_bbox.left << -tk_engine.bl_dif;
        tk_engine.min_tile_bbox.right = tk_engine.min_tile_bbox.right << -tk_engine.bl_dif;
        tk_engine.min_tile_bbox.top = tk_engine.min_tile_bbox.top << -tk_engine.bl_dif;
        tk_engine.min_tile_bbox.bottom = tk_engine.min_tile_bbox.bottom << -tk_engine.bl_dif;
    }
    tk_geo_cleanmaplabel();

    //get_tile_level_16_bound(&tile_box, &el);
    center_id = find_regions_in_bbox(&tk_engine.cur_bbox);
    if (regnum_in_bound == 0)
        return -1;

    lostdata_idx = 0;
    memset(lostdata, 0, sizeof(struct tk_map_lostdata) * TK_LOST_DATA_PIECE);

    increase_tile(&bbox,0); 

    adjust_lostdata();
    maploader_syc_from_buffer();
    tk_update_buffer_screens(tk_gdi.rtview, vm_screen);

    //if (lostdata_idx > 0)
      //  need_map = -1;
    
    if (need_map != 0) {
        city_id = tk_get_current_city_id();
        //restore_context();
        if ((city_id < 0 && city_id != -3) || city_id > city_num)
            return NULL; 
        if (city_id == -3) {
            sprintf(poiname, "U%s", nat.cname);
            return poiname;
        }
        else {
            if (city_list[city_id].cid != -1) {
                sprintf(poiname, "U%s", city_list[city_id].name);
                LOG_ERR("{北京beijing｝process observer: %s\n", poiname);
                return poiname;
                }
            else return NULL;
        }
    }
    else {
        //x = tk_engine.width / 2;
        //y = tk_engine.height / 2;
        //x = x % 256;
        //y = y % 256;
        x = w / 2;
        y = h / 2;
        refresh_local_feature(x, y, flag);

        //restore_context();
        sprintf(poiname, "G%s", best_feature.name);
        
        return poiname;
    }
}
#define MAX_REG_NUM_PER_CITY 10
int *tk_get_rid_list(int cityid)
{
    int i, rid;
    int *ridlist;

    if (cityid != -3 && (cityid <= 0 || cityid > city_num || cityid == 78 || cityid == 139)) {
        LOG_ERR("Invalid city id %d!\n", cityid);
        return NULL;
    }
    ridlist = xmalloc(sizeof(int) * MAX_REG_NUM_PER_CITY);

    for(i = 0; i < MAX_REG_NUM_PER_CITY; i++){
        ridlist[i] = -1;
    }

    if (cityid == -3) {
        ridlist[0] = nat.rid;
        return ridlist;
    }

    if ((rid = city_list[cityid].reg_idx) == -1) {
        LOG_ERR("Invalid city id %d!\n", cityid);
        free(ridlist);
        return NULL;
    }
    i = 0;
    while (rid != -1) {
        ridlist[i++] = rid;
        rid = reg_list[rid].next;
    }

    return ridlist;
}

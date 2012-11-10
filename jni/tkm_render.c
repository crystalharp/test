/*
 * =====================================================================================
 *
 *       Filename:  tkm_render.c
 *
 *    Description:  
 *
 *        Version:  1.0
 *        Created:  2010年12月28日 17时53分32秒
 *       Revision:  none
 *       Compiler:  gcc
 *
 *        Company:  
 *
 * =====================================================================================
 */
#include <string.h>
#include <math.h>
#include <stdlib.h>
#include <stdio.h>
#include <sys/types.h>
#include <fcntl.h>

#include "tk_log.h"
#include "tk_config.h"
#include "tkm_font.h"
#include "tkm_map.h"
#include "tkm_render.h"
#include "tkm_layer.h"
#include "tk_cairo.h"

#ifdef TK_BPP_16  
    #define TK_COLOR_WHITE  0xffff
    #define TK_COLOR_BLACK  0x0000
    #define TK_COLOR_FILTER 0x0000 
    #define TK_COLOR_RED    0xff00
    #define TK_COLOR_YELLOW 0xabcd
#else
    #define TK_COLOR_WHITE  0xffffff       
    #define TK_COLOR_BLACK  0x000000
    #define TK_COLOR_FILTER 0x000000
    #define TK_COLOR_RED    0xff0000
    #define TK_COLOR_YELLOW 0xabcdef
#endif

#define TK_COLOR_DISTRICT 0x0090f5

#define M_PI 3.14159265358979323846

static int cur_buffer_of_clip = MAX_POINT_CLIP;
extern float tk_font_offset ;

typedef enum {
	TK_ALIGN_LEFT,
	TK_ALIGN_RIGHT,
	TK_ALIGN_CENTER,
	TK_ALIGN_TOP,
	TK_ALIGN_BOTTOM
} tk_alignment_enum;

//定义画笔的形状
typedef enum EN_TK_GDI_PENSHAPE {
	TKGDI_ENPSHAPE_ROUND = 0,		//
	TKGDI_ENPSHAPE_FLAT,		//
	TKGDI_ENPSHAPE_SQUARE,		//
	TKGDI_ENPSHAPE_MAX
}TKGdiENPShape;

//定义线型
typedef enum EN_TK_GDI_LINESTYLE {
	TKGDI_ENLSTYLE_SOLID = 0,		//
	TKGDI_ENLSTYLE_PIPE,		//绘制管状线(包边效果)
	TKGDI_ENLSTYLE_DASH,
	TKGDI_ENLSTYLE_MAX
} TKGdiENLStyle;

typedef enum EN_TK_GDI_DFATTR {
	TKGDI_ENDFATTR_NORMAL = 0,
	TKGDI_ENDFATTR_UNDLINE,
	TKGDI_ENDFATTR_ITALIC,
	TKGDI_ENDFATTR_OBLIQUE,
	TKGDI_ENDFATTR_MARIGN,		//对文字进行包边处理
	TKGDI_ENDFATTR_MAX
} TKGdiENDFAttr;

struct gdi tk_gdi;

static struct envelope draw_filter;

#define TK_GEO_ROAD_COLOR_NUM_LIMIT   16

#define TK_GDI_COLOR_PAR 0xffffff

#define TK_GDI_CI_COLOR		tk_gdi.color
#define REVERSECOLOR(color) (color&((0xff<<24)|(0xff<<8)) )|((color&0xff)<<16)|((color&(0xff<<16))>>16)
#ifdef TK_BPP_16
#define TK_COLOR2INDEX(Color)	tk_gdi_rgb565(Color)
#define TK_INDEX2COLOR(Index)	tk_gdi_rgb888(Index)
#define TK_GDI_COLOR_BG         0xef3b
#define TK_GDI_COLOR_WATER      40345
#endif

#ifdef TK_BPP_32
#define TK_COLOR2INDEX(Color)	(Color)
#define TK_INDEX2COLOR(Index)	(Index)
#define TK_GDI_COLOR_WATER      10007243
#define TK_GDI_COLOR_BG         0xf4f3ef //15460060
#endif

#define TK_IMG_HEADER_SIZE 4        //size of tbp file header
#define TK_TRF_TRANSPARENT_COLOR 0x1F   //transparent color: blue
static gdi_color tk_geo_road_colors[TK_GEO_ROAD_COLOR_NUM_LIMIT];
static unsigned char tk_geo_road_color_num;
static struct tk_point* tk_clip_mem_a = NULL;
static struct tk_point* tk_clip_mem_b = NULL;
static short *tk_clip_index = NULL;
#define MAX_EDGE_POINT_NUM 1024

// change from RGB888(format is 0rgb) to RGB565 
 short  tk_gdi_rgb565(int Color)
{
	int r, g, b;
	r = (Color >> (16 + 3)) & 0x1f;
	g = (Color >> (8 + 2)) & 0x3f;
	b = (Color >> (0 + 3))  & 0x1f;
	return b + (g << 5) + (r << 11);
}
/* 修改输入参数和输出参数 hqk 2011-5-19 */
// change from RGB565 to RGB888(format is 0rgb)  
 int  tk_gdi_rgb888(short Index) {
#define B_BITS 5
#define G_BITS 6
#define R_BITS 5
	
#define R_MASK ((1 << R_BITS) -1)
#define G_MASK ((1 << G_BITS) -1)
#define B_MASK ((1 << B_BITS) -1)

	unsigned int r, g, b;
	/* Seperate the color masks */
	r = ((unsigned)Index >> (R_BITS + G_BITS)) & R_MASK;
	g = (Index >> R_BITS)                      & G_MASK;
	b = Index                                  & B_MASK;
	/* Convert the color masks */
	r = r * 255 / R_MASK;
	g = g * 255 / G_MASK;
	b = b * 255 / B_MASK;
	return b + (g << 8) + (((unsigned)r) << 16);
}

//保存道路的颜色，后面根据这个颜色表绘制，以免被覆盖
//这里的转换需要保留，因为配置文件的颜色都是24色的4字节表示
void tk_add_color(gdi_color color)
{
    int i;
    for (i = 0; i < tk_geo_road_color_num; i++) {
        if (tk_geo_road_colors[i] == (color)) {
            return;
        }
    }
    tk_geo_road_colors[tk_geo_road_color_num] = (color);
    tk_geo_road_color_num++;
}

void tk_init_gdicontext()
{
	LOG_INFO("Gdi init begin!\n");
    tk_geo_road_color_num = 0;    
    tk_gdi.pixel_should_overlap = 0;
#ifdef TK_BPP_16
	tk_gdi.nSPixel = 2;
#endif
#ifdef TK_BPP_32
	tk_gdi.nSPixel = 4;
#endif 
	tk_gdi.color = (TK_COLOR_BLACK);
	tk_gdi.bgcolor = (TK_COLOR_WHITE);
	tk_gdi.nPShape = TKGDI_ENPSHAPE_ROUND;
	tk_gdi.nLStyle = TKGDI_ENLSTYLE_SOLID;
	tk_gdi.nPSize = 1;
	tk_gdi.nTStyle = TKGDI_ENDFATTR_NORMAL | TKGDI_ENDFATTR_MARIGN;
    tk_gdi.is_road = 0;

    tk_clip_mem_a = malloc((sizeof (struct tk_point)) * (MAX_POINT_CLIP + 2));
    tk_clip_mem_b = malloc((sizeof (struct tk_point)) * (MAX_POINT_CLIP + 2));
    tk_clip_index = malloc((sizeof(short) * MAX_EDGE_POINT_NUM));

    vm_screen.left = 0;
    vm_screen.right = TILE_SIZE -1; //tk_engine.width - 1;
    vm_screen.top = 0;
    vm_screen.bottom = TILE_SIZE -1;//tk_engine.height - 1;
	LOG_INFO("Gdi init end!\n");
}

void release_gdicontext() 
{
    if (tk_clip_mem_a)
        free(tk_clip_mem_a);
    if (tk_clip_mem_b)
        free(tk_clip_mem_b);
    if (tk_clip_index)
        free(tk_clip_index);
    cur_buffer_of_clip = MAX_POINT_CLIP;
    tk_clip_mem_a = NULL;
    tk_clip_mem_b = NULL;
    tk_clip_index = NULL;
}

void tk_reinit_gdicontext(w, h)
{
	LOG_INFO("Gdi reinit begin!\n");
    vm_screen.right = w;
    vm_screen.bottom = h -1;
	LOG_INFO("Gdi reinit end!\n");
}


//是否已经缩放到最高级
int is_zoomed_to_max = 0;
static char _tk_gdi_fpcnt_;					//fill polygon point count
static short _tk_gdi_fpsx_[TK_GDI_FPMAXCOUNT];	//the point x's position(for sort arrays)
static short _tk_gdi_fpsy_[TK_GDI_FPMAXCOUNT]; 

struct envelope vm_screen;
int tk_draw_district(short src_w, short src_h, short x, short y, int color) {
    int i, j;
    int pixel_size = 4;
    //x = x - (src_w >> 1);
    //y = y - (src_h >> 1);
    unsigned char *dest_ptr = (unsigned char *)(tk_gdi.pd);
    dest_ptr += pixel_size * (TILE_SIZE * y + x);
    for (i = 0; i < src_h; i++ ) {
        for(j = 0; j < src_w; j++) {
                *((tk_pixel*)dest_ptr) = color |0xff000000;
                dest_ptr += pixel_size;
        }
        //jump to next line
        dest_ptr += pixel_size * (TILE_SIZE - src_w);
    }
    return 0;
}

/* 
 * x, y: the reference point's coordinates of the image
 * */
int tk_draw_img_mem(unsigned char *img_ptr, short dest_w, short dest_h, 
    short x, short y, int x_align, int y_align) {
    unsigned char *dest_ptr = (unsigned char *)(tk_gdi.pd);
    short pixel_size = 4;
    short src_w; //the width of the image
    short src_h; //the height of the image
    short src_pitch;//the origin width of the image
    short i, j;
    unsigned char *src_ptr = img_ptr;

#ifdef TK_BPP_16
    pixel_size = 2;
#endif

    if (img_ptr == NULL) {
        LOG_ERR("image pointer is null!!!\n");
        return -1;
    }
    
    src_w = *((short*)src_ptr);
    src_h = *((short*)(src_ptr + 2));

    src_pitch = src_w;
    //LOG_DBG("image size is %d, %d\n", src_w, src_h);
    
    //adjust x and y from reference to leftop according to x_align and y_align settings
	if (x_align != TK_ALIGN_LEFT || y_align != TK_ALIGN_TOP) {
		if (x_align == TK_ALIGN_RIGHT) {
			x -= src_w;
		} else if (x_align == TK_ALIGN_CENTER) {
			x -= (src_w / 2);
		}
		if (y_align == TK_ALIGN_BOTTOM) {
			y -= src_h;
		} else if (y_align == TK_ALIGN_CENTER) {
			y -= (src_h / 2);
		}
	}
    
    //clip according area outside dest window
    if (x < 0 || y < 0 || x + src_w >= dest_w || y + src_h  >= dest_h) {
        //outside dest window
        return -1; //PUZZLE: why return error while not drawing go on
    }
    //y is above screen top
    if (y < 0) {
        src_ptr += pixel_size * ((-y) * src_pitch);
        src_h += y;
        y = 0;
    }

    //x is on the left of screen left edge
    if (x < 0) {
        src_ptr += pixel_size * (-x);
        src_w += x;
        x = 0;
    }

    //avoiding drawing beyond dest window right edge
    if (dest_w < x + src_w) {
        src_w = dest_w - x;
    }
    
    //avoid drawing beyond dest window bottom
    if (dest_h < y + src_h) {
        src_h = dest_h - y;
    }

    //move src_ptr and dest_ptr to first point location
    dest_ptr += pixel_size * (dest_w * y + x);
    src_ptr += TK_IMG_HEADER_SIZE;
    
    //draw points
    //LOG_DBG("After adjustment src_w is %d, src_h is %d, x is %d, y is %d, dest_w is %d\n", 
    //         src_w, src_h, x, y, dest_w);
    //LOG_DBG("First point value is %x\n", *((short*)src_ptr));
    //TK_TRF_TRANSPARENT_COLOR

    for(i = 0; i < src_h; i++) {
        for(j = 0; j < src_w; j++) {
            if (*((tk_pixel* )src_ptr) == TK_COLOR_FILTER ) {
                //transparent point, skip
            } else {
                //draw one pixel
                //*((tk_pixel*)dest_ptr) = tk_gdi_rgb565(*((int *)src_ptr));
               // *((tk_pixel*)dest_ptr) = *((short*)src_ptr);
                *((tk_pixel*)dest_ptr) = *((tk_pixel *)src_ptr) |0xff000000;
                //*((tk_pixel*)dest_ptr) = tk_gdi_rgb888(*((short*)src_ptr));
            }
            dest_ptr += pixel_size;
            src_ptr += pixel_size;
        }
        //jump to next line
        dest_ptr += pixel_size * (dest_w - src_w);
        src_ptr += pixel_size * (src_pitch - src_w);
    }
    return 0;
}

/*
 * Draw image by image resource id
 */
int tk_draw_img(int x, int y, int x_align, int y_align, unsigned int img_id) 
{
    if (img_id < TK_IMG_TIGER_MAP_VECTOR_MAX) {
        if (img_offset[img_id] >= 0) {
                if (img_id != 35)
                     return tk_draw_img_mem(img_buffer + img_offset[img_id], tk_engine.width, tk_engine.height, \
                        x, y, x_align, y_align);
                //else
                  //   return tk_draw_img_mem(img_buffer + img_offset[img_id], TILE_SIZE, TILE_SIZE, \
                    //    x, y, x_align, y_align);
                else
                    return tk_draw_district(22, 22, x, y,TK_COLOR_DISTRICT);
        }
    } else {
        return -1;
    }
    return 0;
}

#define TK_IS_ASCII_CHAR(c) c >= 0 && c < 128
#define TK_ASCII_FONT_WIDTH font_size >> 1

static void tk_draw_char(struct char_data *pChar, int cur_x, int cur_y, gdi_color color, tk_pixel* layer_ptr) {
    int i = 0;
    unsigned short fData = 0;
    int j = 0;
    if (!pChar || !layer_ptr)
        return;
    //if (cur_y < 0 || cur_x < 0 || (cur_x + font_size) >= tk_engine.width || (cur_y + font_size) >= tk_engine.height)
    if (cur_y < 0 || cur_x < 0 || (cur_x + font_size) >= TILE_SIZE || (cur_y + font_size) >= TILE_SIZE)
        return;
    for (i = 0; i < font_size; i++){
        if (pChar->BytesPerLine == 2)
            fData = (pChar->pData[i << 1] << 8) | (pChar->pData[(i << 1) + 1] & 0xff);
        else
            fData = (pChar->pData[i] << 8);               
        for (j = 15; j >= 16 - font_size; j--){
            if ((fData & (1 << j)) != 0 
                //&& cur_x + (15 - j) >= 0 
                //&& (cur_y + i) >=0 
                //&& cur_x + (15 - j) <= tk_screen.width
                //&& (cur_y + i) < tk_screen.height
                )
                  //layer_ptr[cur_x + (15 - j) + (cur_y + i) * tk_engine.width] = color;
                  layer_ptr[cur_x + (15 - j) + (cur_y + i) * TILE_SIZE] = color;
                
        }
    }
}

static void tk_gdi_disp_string(char *string, int x, int y,  gdi_color color){
	char *s = string;
    struct char_data *pChar = NULL;
    int cur_x = x;
    int trasferred_color = color;
    tk_pixel *layer_ptr = (tk_pixel *)tk_gdi.pd;
    unsigned short uChar = 0;

#if TK_FB_BPP == 16
    trasferred_color = ((color>> (0+3))  &0x1f)
        + (((color>> (8+2)) &0x3f) << 5) + (((color>>(16+3)) &0x1f) << 11);
#endif //#if TK_FB_BPP == 16

    while(*s) {
        if (TK_IS_ASCII_CHAR(*s)) {
            uChar = s[0];
            pChar = tk_x_get_char(uChar);
    		if (pChar)
    			tk_draw_char(pChar, cur_x, y, trasferred_color, layer_ptr);
			s++;
            cur_x += TK_ASCII_FONT_WIDTH;
        }
        else {
            uChar = (s[0] << 8) | (((unsigned short)s[1]) & 0xff);
            pChar = tk_x_get_char(uChar);
    		if (pChar)
    			tk_draw_char(pChar,cur_x, y, trasferred_color, layer_ptr);
    		s += 2;
            cur_x += font_size;    		
        }
   }
}

void tk_gdi_draw_string(int x, int y, char *string, gdi_color color) {
    //printf("xxxxxxx  %d-----%d\n",x,y);

	//if ( y + font_size > tk_engine.height || y < 0)
	if ( y + font_size > TILE_SIZE || y < 0)
        return;
    tk_gdi_disp_string(string, x + 1, y + 1, TK_COLOR_WHITE);
#ifndef TK_S_MTK
    tk_gdi_disp_string(string, x - 1, y - 1, TK_COLOR_WHITE);  
#endif //#ifndef TK_S_MTK    
    tk_gdi_disp_string(string, x, y,color);
    //tk_gdi_disp_string(string, x, y, REVERSECOLOR(color));
}

#define TK_IS_IN_SCREEN(x,y)  (x>=0&&x <= tk_engine.width - 1 && y >= 0 && y <= tk_engine.height - 1)
#define TK_GDI_GETPIXEL(x, y)			tk_gdi.pd[x+(y) * tk_engine.width]
#define TK_GDI_SETPIXEL(x, y, nIndex)	if(TK_IS_IN_SCREEN(x,y)) tk_gdi.pd[x+(y) * tk_engine.width] = nIndex

static void _tk_set_pixel_no_overlap(int x, int y, int nIndex)
{  
/*     short i;  

    if (!TK_IS_IN_SCREEN(x,y) || TK_GDI_GETPIXEL(x, y) == TK_COLOR_WHITE) {
        return; 
    }

    for(i = 0; i < tk_geo_road_color_num; i++) {        
        if (TK_GDI_GETPIXEL(x, y) == tk_geo_road_colors[i]) {
            TK_GDI_SETPIXEL(x, y, nIndex); 
            break;  
        } 
    }    */
}
  
//画铁路或地铁的边界
#define TK_GDI_SET_PIXEL_NO_WHITE(x, y, nIndex) if (TK_GDI_GETPIXEL(x, y) != TK_COLOR_WHITE && TK_IS_IN_SCREEN(x,y)) tk_gdi.pd[x+(y)*tk_engine.width] = nIndex
#define TK_GDI_XORPIXEL(x, y)			{	\
	int nIndex = (int)TK_GDI_GETPIXEL(x,y);	\
	TK_GDI_SETPIXEL(x, y, (1<<(tk_gdi.nSPixel*8))-1-nIndex ); \
}
#define TK_GDI_SETPIXEL_NO_OVERLAP(x, y, nIndex) _tk_set_pixel_no_overlap(x, y, nIndex)
#define RED(x) ((x >> 16) & 0xff)
#define GREEN(x) ((x >> 8) & 0xff)
#define BLUE(x) (x & 0xff) 
#define MIXCOLOR(x, a, y, b) ((a) == 0 && (b) == 0) ? x : (((RED(x) * (a) \
                + RED(y) * (b)) / ((a) + (b))) << 16) | (((GREEN(x) * (a) + \
                GREEN(y) * (b)) / ((a)+(b))) << 8) | ((BLUE(x) * (a) + BLUE(y) * (b)) / ((a)+(b)))
/* 
 * x0:
 * y0:
 * x1:
 * */
void draw_hline(int x0, int y0, int x1)
{
	//if (x0 > (tk_engine.width-1) || x1 > (tk_engine.width-1)
	if (x0 > (TILE_SIZE - 1) || x1 > (TILE_SIZE - 1)
            || x0 < 0 || x1 < 0) {
        return;
    }
    if (y0 > (tk_engine.height - 1) || y0 < 0) {
        return;
    }
    if (tk_gdi.is_road == 1 ) {
       if (tk_gdi.nLStyle == TKGDI_ENLSTYLE_PIPE) {
            if ( x0 - 1 >= 0)
                TK_GDI_SETPIXEL_NO_OVERLAP(x0-1, y0, tk_gdi.bgcolor);
            //if ( x1 + 1 <= tk_engine.width - 1)
            if ( x1 + 1 <= TILE_SIZE - 1)
                TK_GDI_SETPIXEL_NO_OVERLAP(x1 + 1, y0, tk_gdi.bgcolor);
        } else {
            if (x0 - 1 >= 0)
                TK_GDI_SET_PIXEL_NO_WHITE(x0 - 1, y0, tk_gdi.bgcolor);
            //if (x1 + 1 <= tk_engine.width - 1)
            if (x1 + 1 <= TILE_SIZE - 1)
                TK_GDI_SET_PIXEL_NO_WHITE(x1 + 1, y0, tk_gdi.bgcolor);
        }
    }
     
    //int x_cur = x0 + y0 * tk_engine.width;
    int x_cur = x0 + y0 * TILE_SIZE;
    int x_max = x1 + x_cur - x0;

    for ( ; x_cur<=x_max; x_cur++)
    {
        tk_gdi.pd[x_cur] = tk_gdi.color | 0xff000000;
    }
    
}

static void	draw_vline(int x0, int y0, int y1)
{

   	if (y0 > (tk_engine.height-1) || y1 > (tk_engine.height-1)
            || y0 < 0 || y1 < 0) {
        return;
    }
    if (x0 > (tk_engine.width - 1) || x0 < 0) {
        return;
    }
    if(tk_gdi.is_road == 1 )
    {
        if(tk_gdi.nLStyle == TKGDI_ENLSTYLE_PIPE){
            if( y0 - 1 >= 0)
                TK_GDI_SETPIXEL_NO_OVERLAP(x0, y0-1, tk_gdi.bgcolor);
            if( y1 + 1 <= tk_engine.height-1)
                TK_GDI_SETPIXEL_NO_OVERLAP(x0, y1+1, tk_gdi.bgcolor);
        } else{
            if( y0 - 1 >= 0)
                TK_GDI_SET_PIXEL_NO_WHITE(x0, y0-1, tk_gdi.bgcolor);
            if( y1 + 1 <= tk_engine.height-1)
                TK_GDI_SET_PIXEL_NO_WHITE(x0, y1+1, tk_gdi.bgcolor);
        }
    }
    while(y0 <= y1) {
        TK_GDI_SETPIXEL(x0, y0, tk_gdi.color);
        y0++;
    }
} 


//  Find previous point which is not on the same height
static int _tk_gdi_getprevpointdiffy(struct tk_point *pPts, int ptPos, int ptNum, int y0)
{
	int j, y1;
	for (j = 0;	j < ptNum - 1;	j++) {
		ptPos = (ptPos != 0) ? (ptPos - 1) : (ptNum - 1);
		y1 = (pPts+ptPos)->y;
		if( y1 != y0 )
			return y1;
	}
	return y0;
}

//  Find previous point which is not on the same height
static int _tk_gdi_getprevpointdiffx(struct tk_point *pPts, int ptPos, int ptNum, int x0)
{
    int j, x1;
    for( j=0;	j<(ptNum-1);	j++  )
    {
        ptPos = (ptPos!=0)?(ptPos-1):(ptNum-1);
        x1 = (pPts+ptPos)->x;
        if( x1 != x0 )
            return x1;
    }
    return x0;
}

// This function adds a point into the sorted array
static void _tk_gdi_addxsort(int x)
{
	int i=0;
	if( _tk_gdi_fpcnt_ < TK_GDI_FPMAXCOUNT )
	{
		//Move all entries to the right (bigger x-value) 
		for( i=_tk_gdi_fpcnt_;	i>0;	i-- )
		{
			if( _tk_gdi_fpsx_[i-1] < x )
				break;
			_tk_gdi_fpsx_[i] = _tk_gdi_fpsx_[i-1];
		}
		//Insert new entry 
		_tk_gdi_fpsx_[i] = x;
		_tk_gdi_fpcnt_++;
	}
}

// This function adds a point into the sorted array
static void _tk_gdi_addysort(int y)
{
    int i=0;
    if( _tk_gdi_fpcnt_ < TK_GDI_FPMAXCOUNT )
    {
        //Move all entries to the right (bigger x-value) 
        for( i=_tk_gdi_fpcnt_;	i>0;	i-- )
        {
            if( _tk_gdi_fpsy_[i-1] < y )
                break;
            _tk_gdi_fpsy_[i] = _tk_gdi_fpsy_[i-1];
        }
        //Insert new entry 
        _tk_gdi_fpsy_[i] = y;
        _tk_gdi_fpcnt_++;
    }
}

//  This function decides either if there a V-point or a
//  X-point. An X-point is added to the array, a V-point is drawn.
static void _tk_gdi_addpoint(int x, int y, int y0, int y1)
{
	// V-point, not crossing the polygon
	if( (y0^y1) >= 0 )
		draw_hline(x, y, x);
	else
		_tk_gdi_addxsort(x);
}

//  X-point. An X-point is added to the array, a V-point is drawn.
static void _tk_gdi_addpointy(int y, int x, int x0, int x1)
{
    // V-point, not crossing the polygon
    if( (x0^x1) >= 0 )
        draw_vline(x, y, y);
    else
        _tk_gdi_addysort(y);
}

//This function returns the x-coordinate of the intersection
//of the given line at the given y-coordinate.
//If there is no intersection, GUI_XMAX is returned.
//This routine does not work for horizontal lines, as there
//would be more than a single point as intersection. This situation
//needs to be checked prior to calling the routine.
//Returns:
//0 if no intersection
// 1 if have intersection
static int _tk_gdi_checkyintersect(int y, int *px, struct tk_point *pPts1, struct tk_point *pPts2 )
{
	int x0,y0,x1,y1;
	double mul;
	if( pPts1->y <= pPts2->y ) {
		// Check if there is an intersection ... (early out) 
		y0 = pPts1->y;
		if( y0 > y )	return 0;
		// Check if there is an intersection ... (early out) 
		y1 = pPts2->y;
		if( y1 < y )	return 0;
		x0 = pPts1->x;
		x1 = pPts2->x;
	} else {
		// Check if there is an intersection ... (early out) 
		y0 = pPts2->y;
		if( y0 > y )	return 0;
		// Check if there is an intersection ... (early out) 
		y1 = pPts1->y;
		if( y1 < y )	return 0;
		x0 = pPts2->x;
		x1 = pPts1->x;
	}
	// Calculate intersection 
	mul = (x1-x0)*(y-y0);
	if( mul > 0 )
		*px = (int)(x0+mul/(y1-y0)+0.5);
	else
		*px = (int)(x0+mul/(y1-y0)-0.5);
	return 1;
}

static int _tk_gdi_checkxintersect(int x, int *py, struct tk_point *pPts1, struct tk_point *pPts2 )
{
    int y0,x0,y1,x1;
    double mul;
    if( pPts1->x <= pPts2->x ) {
        // Check if there is an intersection ... (early out) 
        x0 = pPts1->x;
        if( x0 > x )	return 0;
        // Check if there is an intersection ... (early out) 
        x1 = pPts2->x;
        if( x1 < x )	return 0;
        y0 = pPts1->y;
        y1 = pPts2->y;
    } else {
        // Check if there is an intersection ... (early out) 
        x0 = pPts2->x;
        if( x0 > x )	return 0;
        // Check if there is an intersection ... (early out) 
        x1 = pPts1->x;
        if( x1 < x )	return 0;
        y0 = pPts2->y;
        y1 = pPts1->y;
    }
    // Calculate intersection 
    mul = (y1-y0)*(x-x0);
    if( mul > 0 )
        *py = (int)(y0+mul/(x1-x0)+0.5);
    else
        *py = (int)(y0+mul/(x1-x0)-0.5);
    return 1;
}

static void	fill_polygon(struct tk_point *pPts, int ptNum, int is_horizontal)
{
    if (is_horizontal) {
        int i,y, x0, x1;
        int j, x, xNew;
        char on = 0;
        int yMin = TK_GDI_YMAX, yMax = TK_GDI_YMIN;
        
        // First step : find uppermost and lowermost coordinates 
        for( i=0;	i<ptNum;	i++ )
        {
            y= (pPts+i)->y;
            if( y<yMin )
                yMin = y;
            if( y>yMax )
                yMax = y;
        }
        
        // Use Clipping rect to reduce calculation (if possible) 
        for( y=yMin;	y<=yMax;	y++ )
        {
            _tk_gdi_fpcnt_ = 0;
            // find next intersection and count lines
            for( i=0;	i<ptNum;	i++ )
            {
                int i1 = (i<ptNum-1)?i+1:0;
                int y0 = (pPts+i)->y;
                int y1 = (pPts+i1)->y;
                // Check if starting point is on line 
                if( y0 == y )
                {
                    // Add the entire line 
                    if( y1 == y )
                    {
                        if( (pPts+i)->x < (pPts+i1)->x )
                        {
                            x0 = (pPts+i)->x;
                            x1 = (pPts+i1)->x;
                        }
                        else
                        {
                            x0 = (pPts+i1)->x;
                            x1 = (pPts+i)->x;
                        }
                        if( y>=0	&&  y < tk_engine.height )
                        {
                            if( x0 <0 ) 	x0 = 0;
                            if( x0 >= tk_engine.width ) x0 = tk_engine.width-1;
                            if( x1 <0 ) x1 = 0;	
                            if( x1 >= tk_engine.width ) x1 = tk_engine.width-1;
                            draw_hline( x0, y, x1 );
    //						for( ;	x0<=x1;	x0++ )
                            //	TK_GDI_SETPIXEL(x0, y0, TK_GDI_CI_COLOR);
                        }
                    }
                    // Add only one point 
                    else
                    {
                        int yPrev = _tk_gdi_getprevpointdiffy(pPts, i, ptNum, y);
                        if( yPrev != y )
                            _tk_gdi_addpoint((pPts+i)->x, y, yPrev-y, y1-y);
                    }
                }
                // Ignore if end-point is on the line 
                else if( y1!= y )
                {
                    if( ((y1>=y) && (y0<=y)) || ((y0>=y) && (y1<=y)) )
                    {
                        int xIntersect;
                        if( _tk_gdi_checkyintersect(y, &xIntersect, pPts+i, pPts+i1) == 1 )
                            _tk_gdi_addxsort(xIntersect);
                    }
                }
            }
            on = 0;
            for( j=0;	j<_tk_gdi_fpcnt_;	j++ )
            {
                xNew = _tk_gdi_fpsx_[j];
                
                if( on )
                    draw_hline(x, y, xNew);
                
                on ^= 1;
                x = xNew;
            }
            
        }
    } else {
        int i,x, y0, y1;
        int j, y, yNew;
        char on = 0;
        int xMin = TK_GDI_YMAX, xMax = TK_GDI_YMIN;
        
        // First step : find uppermost and lowermost coordinates 
        for( i=0;	i<ptNum;	i++ )
        {
            x = (pPts+i)->x;
            if( x<xMin )
                xMin = x;
            if( x>xMax )
                xMax = x;
        }
        
        // Use Clipping rect to reduce calculation (if possible) 
        for( x=xMin;	x<=xMax;	x++ )
        {
            _tk_gdi_fpcnt_ = 0;
            // find next intersection and count lines
            for( i=0;	i<ptNum;	i++ )
            {
                int i1 = (i<ptNum-1)?i+1:0;
                int x0 = (pPts+i)->x;
                int x1 = (pPts+i1)->x;
                // Check if starting point is on line 
                if( x0 == x )
                {
                    // Add the entire line 
                    if( x1 == x )
                    {
                        if( (pPts+i)->y < (pPts+i1)->y )
                        {
                            y0 = (pPts+i)->y;
                            y1 = (pPts+i1)->y;
                        }
                        else
                        {
                            y0 = (pPts+i1)->y;
                            y1 = (pPts+i)->y;
                        }
                        if( x>=0	&&  x < tk_engine.width )
                        {
                            if( y0 <0 ) 	y0 = 0;
                            if( y0 >= tk_engine.height ) y0 = tk_engine.height-1;
                            if( y1 <0 ) y1 = 0;	
                            if( y1 >= tk_engine.height ) y1 = tk_engine.height-1;
                            draw_vline( x, y0, y1 );
                        }
                    }
                    // Add only one point 
                    else
                    {
                        int xPrev = _tk_gdi_getprevpointdiffx(pPts, i, ptNum, x);
                        if( xPrev != x )
                            _tk_gdi_addpointy((pPts+i)->y, x, xPrev-x, x1-x);
                    }
                }
                // Ignore if end-point is on the line 
                else if( x1!= x )
                {
                    if( ((x1>=x) && (x0<=x)) || ((x0>=x) && (x1<=x)) )
                    {
                        int yIntersect;
                        if( _tk_gdi_checkxintersect(x, &yIntersect, pPts+i, pPts+i1) == 1 )
                            _tk_gdi_addysort(yIntersect);
                    }
                }
            }
            on = 0;
            for( j=0;	j<_tk_gdi_fpcnt_;	j++ )
            {
                yNew = _tk_gdi_fpsy_[j];
                if( on )
                    draw_vline(x, y, yNew);
                on ^= 1;
                y = yNew;
		    }
	    }
    }
}

/* 
 * tk_gdi.rtview: filter's envelope.
 * zoom_level: current zoom level.
 * is_file_not_found_scene: 0 if the mapdata file exists.
 * */
void draw_bg(short zoom_level, int is_file_not_found_scene)
{
    if (zoom_level > TK_NATIONAL_LEVEL_A || is_file_not_found_scene)
        GDI_SETCOLOR(TK_GDI_COLOR_WATER);
    else
        GDI_SETCOLOR(TK_GDI_COLOR_BG);

    int i;
    for (i = 0; i < (tk_engine.width * tk_engine.height); i++) {
       tk_gdi.pd[i] = TK_GDI_CI_COLOR;
	}
}



void _tk_mem_blt_16(unsigned char* dest, unsigned short width, short dx, short dy
        ,unsigned short height) 
{
    int moved = dy * width + dx;
    int nbytes = 4;
#ifdef TK_BPP_16
    nbytes = 2;
#endif  
    if (moved >= 0)
        memmove(dest,  dest + (moved * nbytes), (height * width - moved) * nbytes);
    else
        memmove(dest - (moved * nbytes),  dest, (height * width + moved) * nbytes);

}


/* helper function
 *
 */
// Angle : 90/1024?//  Data  : 1/1024
static const unsigned short _tk_asin_[] = {
	0,       // 1/16 *90?   
		100,     // 1/16 *90?   
		200,     // 2/16 *90?  
		297,     // 3/16 *90?  
		392,     // 4/16 *90?  
		483,     // 5/16 *90?  
		569,     // 6/16 *90?  
		650,     // 7/16 *90?  
		724,     // 8/16 *90?  
		792,     // 9/16 *90?  
		851,     // 10/16 *90? 
		903,     // 11/16 *90? 
		946,     // 12/16 *90? 
		980,     // 13/16 *90? 
		1004,    // 14/16 *90? 
		1019,    // 15/16 *90? 
		1024     // 16/16 *90? 
};

int tk_sin(int angle)
{
	char IsNeg = 0;
	int i, Faktor, t;
	angle &= ((1 << 12) - 1);
	if (angle > TK_GDI_180DEG) {// reduce to 0-360 degrees  
		angle -= TK_GDI_180DEG;
		IsNeg = 1;
	}
	if( angle > TK_GDI_90DEG )// reduce to between 90-180  
		angle = TK_GDI_180DEG - angle;// use sine symetry  
	// Now angle is reduced to 0?<= <= 90? 
	i = (angle >> 6);
	Faktor = ((1 << 6) - (angle & ((1 << 6) - 1)));
	t = _tk_asin_[i] * Faktor;
	if (Faktor != (1 << 6))
		t += _tk_asin_[i + 1] * ((1 << 6) - Faktor);
	t = ((t + (1 << 5)) >> 6);
	return (IsNeg) ? (0 - t) : t;
}

int tk_cos(int angle)
{
	return tk_sin(angle + TK_GDI_90DEG);
}

//Angle : 360/4096?
const short _tk_atan_[] = {
	0,       // atan(0/16)  
		41,      // atan(1/16)  
		81,      // atan(2/16)  
		121,     // atan(3/16)  
		160,     // atan(4/16)  
		197,     // atan(5/16)  
		234,     // atan(6/16)  
		269,     // atan(7/16)  
		302,     // atan(8/16)  
		334,     // atan(9/16)  
		364,     // atan(10/16)  
		393,     // atan(11/16)  
		419,     // atan(12/16)  
		445,     // atan(13/16)  
		469,     // atan(14/16)  
		491,     // atan(15/16)  
		512      // atan(1) = 45?= 512/1024  
};

// Calculate arctan of q, where q is any where between 0 and 1024  
static int _tk_atan0_45(int q)
{
	int r, i, Faktor;
	// Now angle is reduced to 0?<= <= 90?==>  0 <= <= 256 
	q >>= 2;    // make sure we do not exceed 16 bits in calculations  
	i = q >> 4;
	Faktor = (1 << 4) - (q & ((1 << 4) - 1));
	r = _tk_atan_[i] * Faktor;
	if (Faktor != ( 1 << 4)) {
		r += _tk_atan_[i + 1] * ((1 << 4) - Faktor);
	}
	r = (r + (1<< 3)) / (1 << 4);   // divide  incl. rounding  
	return r;
}

int tk_atan(int x, int y)
{
    unsigned char q = 0;
	int angle = 0;
	// first make sure we are in angle between 0 and 45? 
	if( x < 0 ) {
		q = 1;
		x = -x;
	}
	if( y < 0 ) {
		q |= (1 << 1);
		y = -y;
	}
	if( y > x ) {
		int t = y;
		y = x;
		x = t;
		q |= (1 << 2);
	}
	y = ((y << 10) + (x >> 1));
	if (x == 0)
		return 0;
	y = y / x;
	angle = _tk_atan0_45(y);
	if (q & (1 << 2) )// y/x reverse ?  
		angle = TK_GDI_90DEG - angle;
	if (q & 1 )// x reverse ?  
		angle = TK_GDI_180DEG - angle;
	if (q & (1<<1) )// y-reverse ?  
		angle = TK_GDI_360DEG - angle;
	return angle;
}

//function:calculate the length
static double calclength(int nSx, int nSy, int nEx, int nEy) {
	double nLen = 0;
	int nAngle = 0;
	if (nSy - nEy == 0)
		return TK_ABS(nSx - nEx);
	nAngle = tk_atan(nSx - nEx, nSy - nEy);
	nLen = TK_ABS((nSy - nEy) / (tk_sin(nAngle) / 1024.0));
	return nLen;
}

static double tk_geo_calctotallength(struct tk_point *pPtList, int nPtNum)
{
	double dbLen = 0.0;
	int i = 0;
	for(i = 0; i < nPtNum - 1; i++)
		dbLen += calclength(pPtList[i].x, pPtList[i].y, pPtList[i + 1].x, pPtList[i + 1].y);
	return dbLen;
}

//function:calculate the point on line(by the distance to start)
static void calcpointonline(struct tk_point ptStart, struct tk_point ptEnd, double dbDis, struct tk_point *ptRes)
{
	int dbAng = 0;
	int cx, cy;

    *ptRes = ptEnd;
	if (ptRes->x == ptStart.x) {
		ptRes->x = ptStart.x;
		ptRes->y = (int)(ptStart.y + (ptStart.y > ptRes->y ? -dbDis : dbDis));
        return ; 
	}
	else if (ptRes->y == ptStart.y)	{
		ptRes->y = ptStart.y;
		ptRes->x = (int)(ptStart.x + (ptStart.x > ptRes->x ? -dbDis : dbDis));
        return;
	}
	else {
		dbAng = tk_atan(-ptStart.x + ptEnd.x, -ptStart.y + ptEnd.y);
		cx = (int)(dbDis * tk_cos(dbAng) + 0.5);
		cy = (int)(dbDis * tk_sin(dbAng) + 0.5);
		cx = (cx < 0) ? (((-cx + 512) >> 10)) : ((cx + 512) >> 10);
		cy = (cy < 0) ? (((-cy + 512) >> 10)) : ((cy + 512) >> 10);
		if(ptRes->x > ptStart.x && ptRes->y > ptStart.y) {
			ptRes->x = ptStart.x + cx;
			ptRes->y = ptStart.y + cy;
            return;
		}
		else if (ptRes->x < ptStart.x && ptRes->y > ptStart.y) {
			ptRes->x = ptStart.x - cx;
			ptRes->y = ptStart.y + cy;
            return;
		}
		else if (ptRes->x <ptStart.x && ptRes->y < ptStart.y) {
			ptRes->x = ptStart.x - cx;
			ptRes->y = ptStart.y - cy;
            return;
		}
		else if(ptRes->x > ptStart.x && ptRes->y < ptStart.y) {
			ptRes->x = ptStart.x + cx;
			ptRes->y = ptStart.y - cy;
            return;
		}
	}
}
/* check the the slope if int(45 - 135) */
int check_slope(float slope)
{
    if((slope <= 0.707106 && slope >= 0.0) || (slope >= -0.707106 && slope <= 0.0))
        return 0;
    else
        return 1;
}

/* 只有两条线段的夹角 > 120度，才将文字分拆显示在线段上 */
int check_sharp_angle(int x, int y, int x1, int y1, int x2, int y2)
{
    float ds1 = y - y1 / x - x1;
    float ds2 = y2 - y1 / x2 - x1;
    if (ds1 * ds2 == -1.0)
        return -1;
    float tan2 = ds1 - ds2 / (1.0 + ds1*ds2);
    if (tan2 < 0 && tan2 > 1.73205)
        return 0;
    return -1;
}

// 获得折线段上的离散点链表
static int calcpointsonline(struct tk_point *pPtLine, int nPtLNum, int dbDMin, struct tk_point_slope *pPtList, int *nPtNum, int *number)
{
    //dbDMin = 13;
    int i;
    struct tk_point ptStart = pPtLine[0];
    int    dbLen = 0, index[nPtLNum - 1];
    int    j = *nPtNum, k = *nPtNum , g, q;
    int    nAngle, nADir;
    int    temp_len = 0;
    int    dis_valid = 0;
    int    dx,dx_slope;
    float  slope = 0.0;
    float  slope_v[2];

    /* 两点之间线段长度大于dbDmin 在两点之间绘制文字 */
    for(i = *number; i < nPtLNum - 1; i++) {
	    index[i] = calclength(pPtLine[i].x, pPtLine[i].y, pPtLine[i + 1].x, pPtLine[i + 1].y);
        dbLen = index[i];
        if (dbLen > dbDMin) {
            if (dbLen < (*nPtNum *dbDMin)) {
                //continue;
                if (i == *number)
                    continue;
                if ( (index[i]/dbDMin + index[i - 1]/dbDMin) < *nPtNum || *nPtNum == 2)
                    continue;
                else {
                     nAngle = tk_atan(pPtLine[i + 1].x - pPtLine[i].x, pPtLine[i + 1].y - pPtLine[i].y);
                    if ((nAngle < 1536) || (nAngle > 3794))
                        nADir = 0;
                    else
                        nADir = 1;
                    q = i - 1;
                    dx = pPtLine[q + 1].x - pPtLine[q].x;
                    dx_slope = pPtLine[q + 1].y < pPtLine[q].y ? dx : -dx;
                    slope_v[0] = (float) (dx_slope) / sqrt((pPtLine[q + 1].x - pPtLine[q].x)*\
                                    (pPtLine[q + 1].x - pPtLine[q].x) + (pPtLine[q + 1].y - pPtLine[q].y)*(pPtLine[q + 1].y - pPtLine[q].y));
                    q++;
                    dx = pPtLine[q + 1].x - pPtLine[q].x;
                    dx_slope = pPtLine[q + 1].y < pPtLine[q].y ? dx : -dx;
                    slope_v[1] = (float) (dx_slope) / sqrt((pPtLine[q + 1].x - pPtLine[q].x)*\
                                    (pPtLine[q + 1].x - pPtLine[q].x) + (pPtLine[q + 1].y - pPtLine[q].y)*(pPtLine[q + 1].y - pPtLine[q].y));
                    if (check_slope(slope_v[0]) != check_slope(slope_v[1]))
                            slope_v[0] = slope_v[1] = 1.0;

                    float angle[2];
                    dis_valid = index[i - 1] % dbDMin;
                    calcpointonline(pPtLine[i - 1], pPtLine[i] , dis_valid, &ptStart);
                    for(q = i - 1; q < i + 1; q++) {
                        g = index[q] / dbDMin ;
                        //dx = pPtLine[q + 1].x - ptStart.x;
                        //dx_slope = pPtLine[q + 1].y < pPtLine[q].y ? dx : -dx;
                        //slope_v[q - i + 1] = (float) (dx_slope) / sqrt((pPtLine[q + 1].x - ptStart.x)*\
                        //            (pPtLine[q + 1].x - ptStart.x) + (pPtLine[q + 1].y - ptStart.y)*(pPtLine[q + 1].y - ptStart.y));

                        while(g-- && j--) {
                            if (nADir == 0) {
                                k = *nPtNum - j - 1;
                            }
                            else {
                                k = j;
                            }
                            calcpointonline(ptStart, pPtLine[q + 1] , dbDMin, &pPtList[k].pt);
                            ptStart = pPtList[k].pt;
                            pPtList[k].slope = slope_v[q - i + 1];
                        }
                       ptStart = pPtLine[q + 1];
                    }

                    }
                    *number = i++;
                    return 1;
            }
            else {
                temp_len = 0;
                dx = pPtLine[i + 1].x - pPtLine[i].x;
                dx_slope = pPtLine[i + 1].y < pPtLine[i].y ? dx : -dx;

                nAngle = tk_atan(pPtLine[i + 1].x - pPtLine[i].x, pPtLine[i + 1].y - pPtLine[i].y);
                if ((nAngle < 1536) || (nAngle > 3794))
                    nADir = 0;
                else
                    nADir = 1;

                ptStart = pPtLine[i];
                slope = (float) (dx_slope) / sqrt((pPtLine[i + 1].x - ptStart.x)*(pPtLine[i + 1].x - ptStart.x) \
                                                                        + (pPtLine[i + 1].y - ptStart.y)*(pPtLine[i + 1].y - ptStart.y));
                while(j--) {
                    if (nADir == 0) {
                        k = *nPtNum - j - 1;
                    }
                    else {
                        k = j;
                    }
                    calcpointonline(ptStart, pPtLine[i + 1], dbDMin, &pPtList[k].pt);
                    ptStart = pPtList[k].pt;
                    pPtList[k].slope = slope;
                }
                *number = ++i;
                return 1;
         }
    }
  }
    return 0;
}

//具体的绘制部分
/*  判断两个矩形之间的位置关系
 *  Retuern value: 0无交集，1有交集，2、3包含关系 
 *  */
int tk_geo_intersectrect(struct envelope rect1, struct envelope rect2)
{
    if ((rect1.right < rect2.left) || (rect1.bottom < rect2.top) ||
            (rect1.left > rect2.right) || (rect1.top > rect2.bottom)) {
        return 0;		    
    } else if ((rect1.left < rect2.left) && (rect1.right > rect2.right) 
            && (rect1.top < rect2.top) && (rect1.bottom > rect2.bottom)) {
        return 2;		// rect1包含rect2
    } else if ((rect1.left > rect2.left) && (rect1.right < rect2.right) 
            && (rect1.top > rect2.top) && (rect1.bottom < rect2.bottom)) {
        return 3;		// rect2包含rect1
    } else {
        return 1;		// rect1和rect2交叉
    }
}


/* 
 * 绘制点、线、面的模块
 * */

static short get_clip_index(short i) 
{
    if (i >= MAX_EDGE_POINT_NUM)
        return -1;
    return tk_clip_index[i];
}

//Liang-Barsky Line Clipping  Test
static int lb_clip_test(float p, float q, float *u1, float *u2) 
{
    float r;
    int retval = 1;

    if (p < 0.0) {
        r = q / p;
        if (r > *u2)
            retval = 0;
        else if (r > *u1)
            *u1 = r;
    } else if (p > 0.0) {
        r = q / p;
        if (r < * u1)
            retval = 0;
        else if (r < *u2)
            *u2 = r;
    } else
        /* p = 0, so line is parallel to this clipping edge */
        if (q < 0.0)
            /* Line is outside clipping edge */
            retval = 0;
    return retval;
}

/*
    pPt: 目标，已分配好
    pPoints:源，不能发生状态改变
    rect: 裁剪框
    ptNum: 传入裁剪前的点数，调用结束后为裁减后的点数
*/
static int line_clip(struct tk_point* pPt, int *ptNum, struct envelope rect, struct tk_point* pPoints)
{
    int i, cnt=0;
    int idx_cnt = 0;//计算被裁开的点数目 ,用来存储这些点前面的点的index到clip_index里面以计算铁路的Dash_remain 长度
    for (i = 0; i < (*ptNum) - 1; i++){
        struct tk_point *p1 = pPoints + i;
        struct tk_point *p2 = pPoints + i + 1;
        float u1 = 0.0, u2 = 1.0, dx, dy;
        dx = (float)(p2->x - p1->x);
        if (lb_clip_test(-dx, (float)(p1->x - rect.left), &u1, &u2)) {
            //right edge
            if (lb_clip_test(dx, (float)(rect.right - p1->x), &u1, &u2)) {
                dy =(float)( p2->y - p1->y);
                //top edge
                if (lb_clip_test(-dy,(float)( p1->y - rect.top), &u1, &u2))
                    //bottom edge
                    if (lb_clip_test(dy,(float)( rect.bottom- p1->y), &u1, &u2)) {
                        if (u1 > 0.0) {
                            pPt[cnt].x = ((int)(u1 * dx) + p1->x);
                            pPt[cnt].y = ((int)(u1 * dy) + p1->y);
                            pPt[cnt].levelCode=1;
                            tk_clip_index[idx_cnt] = i;
                            idx_cnt++;
                        }
                        else {
                            pPt[cnt].x = p1->x;
                            pPt[cnt].y = p1->y;
                            pPt[cnt].levelCode=0;
                        }
                        cnt++;
                        if (i == (*ptNum) - 2 && u2 >= 1.0) {
                            pPt[cnt].x = p2->x;
                            pPt[cnt].y = p2->y;
                            pPt[cnt].levelCode = 0;
                            cnt++;
                            break;
                        }
                        if (u2 < 1.0) {
                            pPt[cnt].x = (int)(u2 * dx) + p1->x;
                            pPt[cnt].y = (int)(u2 * dy) + p1->y;
                            pPt[cnt].levelCode = 1;
                            cnt++;
                            tk_clip_index[idx_cnt] = i;
                            idx_cnt++;
                        }
                    }
            }
        }
    }
    *ptNum = cnt;
    if (*ptNum == 0)
        return 0;
    else return 1;
}

static struct tk_point calcsideintersectpoint(int edge, int side, float x1, float y1, float x2, float y2)
{
    struct tk_point pt;
    if (side == 1 || side == 3) {
        pt.x = edge;
        if (x1 == x2) {
            if( side == 1 )
                pt.y = y2;
            else
                pt.y = y1;
        }
        else
            pt.y = y1 + (y2 - y1) * (edge - x1) / (x2 - x1);
    }
    else if (side == 2 || side == 4 )
    {
        if (y1 == y2 ) {
            if (side == 2)
                pt.x = x2;
            else
                pt.x = x1;
        }
        else
            pt.x = x1 + (x2 - x1) * (edge - y1) / (y2 - y1);
        pt.y = edge;
    }

    pt.levelCode = 0;
    return pt;
}


/* 当线段或多边形与屏幕相交而非被包含时，需裁剪线段或多边形
 * pPoints: the original point of line, 存的是屏幕坐标
 * ptNbr: the original number of points of line
 * rect: vm_screen
 * nGType:1 is line, 2 is polygon
 * pPts: the pointer of points of line after clip
 * ptNum: the number of points after clip
 * return 0 if
 * return 1 if
 */
int clipgeopoints(struct tk_point *pPoints, int ptNbr, struct envelope rect, int nGType, struct tk_point **pPts, int *ptNum)
{
    struct tk_point *pTPtList;//=tk_clip_mem_a; 
    struct tk_point *pPt;//=tk_clip_mem_b;
    struct tk_point *pTemp; //Buffer  for swap
    struct tk_point ptstart, ptend;
    int side, i, edge, ptnum = 0, res = 1;
    struct envelope rect_extend;

    if (ptNbr * 2 > cur_buffer_of_clip) {
        free(tk_clip_mem_a);
        free(tk_clip_mem_b);
        tk_clip_mem_a = xmalloc((sizeof(struct tk_point)) * (ptNbr + 2) * 2);
        tk_clip_mem_b = xmalloc((sizeof(struct tk_point)) * (ptNbr + 2) * 2);
        cur_buffer_of_clip = ptNbr;
        LOG_INFO("\n Too many point in a feature! %d" ,ptNbr);
    }
    pPt = tk_clip_mem_b;
    *ptNum = ptNbr;
    memcpy(&rect_extend,&rect,sizeof(struct envelope));
    if (nGType == 1) {
        if(rect.left == 0)
            rect_extend.left -= 20;
        if(rect.right == tk_engine.width - 1)
            rect_extend.right += 20;
        if(rect.top == 0)
            rect_extend.top -= 20;
        if(rect.bottom == tk_engine.height - 1)
            rect_extend.bottom += 20; 
        res = line_clip(pPt, ptNum, rect_extend, pPoints);
        *pPts = pPt;
        return res;
    }
    
    pTPtList = tk_clip_mem_a;
    memcpy(pTPtList, pPoints, sizeof(struct tk_point) * ptNbr);
    memset(pPt, 0, sizeof(struct tk_point) * cur_buffer_of_clip);
    for(i = 0; i < *ptNum; i++)
        pTPtList[i].levelCode = 0;
    for(side = 1; side <= 4; side++) {
        if( side == 1 )         edge = rect.right;
        else if( side == 2 )    edge = rect.bottom;
        else if( side == 3 )    edge = rect.left;
        else if( side == 4 )    edge = rect.top;
        ptnum = 0;
        for(i = 0; i < (*ptNum); i++) {
            ptstart = pTPtList[i];
            if (i != (*ptNum) - 1)
                ptend = pTPtList[i + 1];
            else ptend = pTPtList[0];

            switch(side) {
                case 1:
                    if (ptstart.x < edge) {
                        pPt[ptnum] = ptstart;
                        ptnum++;
                    }
                    if ((ptstart.x <= edge && ptend.x >= edge) || (ptstart.x >= edge && ptend.x <= edge)) {
                        if (ptstart.y == ptend.y) {
                            pPt[ptnum].x = rect.right;
                            pPt[ptnum].y = ptstart.y;
                            if (pPt[ptnum].levelCode) {
                                res = 2;
                                pPt[ptnum].levelCode = 3;
                            }
                            else {
                                if (ptstart.y == rect.top || ptstart.y == rect.bottom) {
                                    pPt[ptnum].levelCode = 3;
                                    res = 2;
                                }
                                else
                                    pPt[ptnum].levelCode = side;
                            }
                            ptnum++;
                        }
                        else {
                            pPt[ptnum] = calcsideintersectpoint(edge, side, (float)ptstart.x, (float)ptstart.y, (float)ptend.x, (float)ptend.y);
                            if (pPt[ptnum].levelCode) {
                                res = 2;
                                pPt[ptnum].levelCode = 3;
                            }
                            else
                                pPt[ptnum].levelCode = 1;
                            ptnum++;
                        }
                        if (ptnum > 2 && pPt[ptnum - 1].levelCode == pPt[ptnum - 2].levelCode)
                            res = 2;
                    }
                    break;
                case 2:
                    if (ptstart.y < edge) {
                        pPt[ptnum] = ptstart;
                        ptnum++;
                    }
                    if( (ptstart.y <= edge && ptend.y >= edge) || (ptstart.y >= edge && ptend.y <= edge)) {
                        if (ptstart.x == ptend.x) {
                            pPt[ptnum].x = ptstart.x;
                            pPt[ptnum].y = rect.bottom;
                            if (pPt[ptnum].levelCode) {
                                res = 2;
                                pPt[ptnum].levelCode = 3;
                            }
                            else {
                                if (ptstart.x == rect.left || ptstart.x == rect.right) {
                                    pPt[ptnum].levelCode = 3;
                                    res = 2;
                                }
                                else
                                    pPt[ptnum].levelCode = side;
                            }
                            ptnum++;
                        }
                        else {
                            pPt[ptnum] = calcsideintersectpoint(edge, side, (float)ptstart.x, (float)ptstart.y, (float)ptend.x, (float)ptend.y);
                            if (pPt[ptnum].levelCode) {
                                res = 2;
                                pPt[ptnum].levelCode = 3;
                            }
                            else
                                pPt[ptnum].levelCode = 2;
                            ptnum++;
                        }
                        if (ptnum > 2 && pPt[ptnum - 1].levelCode == pPt[ptnum - 2].levelCode)
                            res = 2;
                    }
                    break;
                case 3:
                    if(ptstart.x > edge) {
                        pPt[ptnum] = ptstart;
                        ptnum++;
                    }
                    if ((ptstart.x <= edge && ptend.x >= edge) || (ptstart.x >= edge && ptend.x <= edge)) {
                        if (ptstart.y == ptend.y) {
                            pPt[ptnum].x = rect.left;
                            pPt[ptnum].y = ptstart.y;
                            if (pPt[ptnum].levelCode) {
                                res = 2;
                                pPt[ptnum].levelCode = 3;
                            }
                            else {
                                if (ptstart.y == rect.top || ptstart.y == rect.bottom) {
                                    pPt[ptnum].levelCode = 3;
                                    res = 2;
                                }
                                else
                                    pPt[ptnum].levelCode = side;
                            }
                            ptnum++;
                        }
                        else {
                            pPt[ptnum] = calcsideintersectpoint(edge, side, (float)ptstart.x, (float)ptstart.y, (float)ptend.x, (float)ptend.y);
                            if (pPt[ptnum].levelCode) {
                                res = 2;
                                pPt[ptnum].levelCode = 3;
                            }
                            else
                                pPt[ptnum].levelCode = 3;
                            ptnum++;
                        }
                        if (ptnum > 2 && pPt[ptnum - 1].levelCode == pPt[ptnum - 2].levelCode)
                            res = 2;
                    }
                    break;
                case 4:
                    if (ptstart.y > edge) {
                        pPt[ptnum] = ptstart;
                        ptnum++;
                    }
                    if ((ptstart.y <= edge && ptend.y >= edge) || (ptstart.y >= edge && ptend.y <= edge)) {
                        if (ptstart.x == ptend.x) {
                            pPt[ptnum].x = ptstart.x;
                            pPt[ptnum].y = rect.top;
                            if (pPt[ptnum].levelCode) {
                                res = 2;
                                pPt[ptnum].levelCode = 3;
                            }
                            else {
                                if (ptstart.x == rect.left || ptstart.x == rect.right) {
                                    pPt[ptnum].levelCode = 3;
                                    res = 2;
                                }
                                else
                                    pPt[ptnum].levelCode = side;
                            }
                            ptnum++;
                        }
                        else {
                            pPt[ptnum] = calcsideintersectpoint(edge, side, (float)ptstart.x, (float)ptstart.y, (float)ptend.x, (float)ptend.y);
                            if (pPt[ptnum].levelCode) {
                                res = 2;
                                pPt[ptnum].levelCode = 3;
                            }
                            else
                                pPt[ptnum].levelCode = 3;
                            ptnum++;
                        }
                        if (ptnum > 2 && pPt[ptnum - 1].levelCode == pPt[ptnum - 2].levelCode)
                            res = 2;
                    }
                    break;
            }
        }
        pTemp = pPt;
        pPt = pTPtList;
        pTPtList = pTemp;
        *ptNum = ptnum;
        //         TK_TRACE_DEBUG((" Pt num :%d",ptnum));
    }
    *pPts = NULL;
    if (*ptNum > 0)
        (*pPts) = pTPtList;
    if (*pPts == NULL) {
        *ptNum = 0;
        return 0;
    }
    return res;
}

/* 判断一个feature的外接矩形与给定方框的关系 
 * 输入:TK_WHOLE_SCREEN_ENV , TK_CLIP_SCREEN_ENV
 * return 0	:无交叉
 * return 1: 有交叉且feature不被包含,
 * return 3: 给定方框包含feature */
int judge_feature_env_relation(struct feature* pft, int scr_env_id)
{
    struct envelope temp_env;
    struct feature *cur_ft = NULL;
    int relation = -1;
    int temp_relation = -1;
    cur_ft = pft;
    
    while (cur_ft != NULL && cur_ft->tile->flag == tk_engine.tile_flag) {
        temp_env.left = cur_ft->left_top.x;
        temp_env.top = cur_ft->left_top.y;
        temp_env.right = cur_ft->right_bottom.x;
        temp_env.bottom = cur_ft->right_bottom.y;
        if (scr_env_id == TK_WHOLE_SCREEN_ENV)
             temp_relation = tk_geo_intersectrect(temp_env, cur_ft->tile->whole_screen);
        else 
             temp_relation = tk_geo_intersectrect(temp_env, cur_ft->tile->clip_screen);
        if (temp_relation == 1 || temp_relation == 2)
            return 1;
        else if ((temp_relation == 3 && relation == 0) ||(temp_relation == 0 && relation == 3)) {
            return 1;
        }
        relation = temp_relation;
        cur_ft = cur_ft ->next;
    }
    return relation;
}

//根据feature 以及level生成 point列表,直接写入tk_gdi.draw_point_buffer;tk_gdi.draw_point_buffer_len可能改变
void get_point_from_feature(struct feature* pfeature, int* ptnum)
{
    //坐标转换的变量
    int  tile_bias_x = 0, tile_bias_y = 0;
    short lvl_dif = pfeature->tile->need_move_bits;
    struct feature* cur_feature = pfeature;
    int vlevel = tk_engine.current_z;
    int i, cur_point_number;
    cur_point_number = 0;
    //循环连接cur_feature的所有tile
    while (cur_feature) {
        if (lvl_dif >= 0) {
            tile_bias_x = (short)(((cur_feature->tile->coder_lon) << (8 - lvl_dif))
                                - (tk_engine.min_tile_bbox.left >> (16 - vlevel)));
            tile_bias_y = (short)(((cur_feature->tile->coder_lat)<<(8-lvl_dif))
                                - (tk_engine.min_tile_bbox.bottom >>(16 - vlevel)));
            //如果buffer不够大,重新分配
           //printf("tile_bias_x mod%d-----%d---%d---%d\n",tile_bias_x,tile_bias_y,cur_feature->tile->coder_lon, cur_feature->tile->coder_lat);
            
            if (cur_point_number + cur_feature->point_nums > tk_gdi.draw_point_buffer_len) {
                struct tk_point* newbuff = malloc((cur_point_number + cur_feature->point_nums) * sizeof(struct tk_point));
                memcpy(newbuff, tk_gdi.draw_point_buffer, cur_point_number * sizeof(struct tk_point));
                tk_gdi.draw_point_buffer_len = cur_point_number + cur_feature->point_nums;
                free(tk_gdi.draw_point_buffer);
                tk_gdi.draw_point_buffer = newbuff;
            }
            //写入buffer
            if (cur_feature == pfeature) {
                tk_gdi.draw_point_buffer[cur_point_number].x = ((cur_feature->points->x) >> lvl_dif) + tile_bias_x;
                tk_gdi.draw_point_buffer[cur_point_number].y = ((cur_feature->points->y) >> lvl_dif) + tile_bias_y;
                cur_point_number++;
            }
            //printf("featur type %d\n",pfeature->type);
            for (i = 1; i<cur_feature->point_nums - 1; i++) {
                //printf("point x y %d----%d\n",(cur_feature->points + i)->x,(cur_feature->points + i)->y);
                tk_gdi.draw_point_buffer[cur_point_number].x = (((cur_feature->points) + i)->x >> lvl_dif) + tile_bias_x;
                tk_gdi.draw_point_buffer[cur_point_number].y = (((cur_feature->points) + i)->y >> lvl_dif) + tile_bias_y;
                cur_point_number++;
            }
            if ((cur_feature->next == NULL) || cur_feature->next->tile->flag != tk_engine.tile_flag) {
                tk_gdi.draw_point_buffer[cur_point_number].x = (((cur_feature->points) + (cur_feature->point_nums-1))->x >> lvl_dif) + tile_bias_x;
                tk_gdi.draw_point_buffer[cur_point_number].y = (((cur_feature->points) + (cur_feature->point_nums-1))->y >> lvl_dif) + tile_bias_y;
                cur_point_number++;
            }
        } else { // lvl_dif < 0
            tile_bias_x = (((cur_feature->tile->coder_lon) << (8 - lvl_dif))
                    - (tk_engine.min_tile_bbox.left));// <<(-(16 - vlevel))));
            tile_bias_y = (((cur_feature->tile->coder_lat) << (8 - lvl_dif))
                    - (tk_engine.min_tile_bbox.bottom));// << (-(16 - vlevel))));
            //如果buffer不够大,重新分配
            if (cur_point_number + cur_feature->point_nums > tk_gdi.draw_point_buffer_len) {
                struct tk_point* newbuff =
                malloc((cur_point_number + cur_feature->point_nums) * sizeof(struct tk_point));
                memcpy(newbuff, tk_gdi.draw_point_buffer, cur_point_number * sizeof(struct tk_point));
                tk_gdi.draw_point_buffer_len = cur_point_number + cur_feature->point_nums;
                free(tk_gdi.draw_point_buffer);
                tk_gdi.draw_point_buffer = newbuff;
            }
            //写入buffer
            if (cur_feature == pfeature) {
                tk_gdi.draw_point_buffer[cur_point_number].x = ((cur_feature->points->x) << (-lvl_dif)) + tile_bias_x;
                tk_gdi.draw_point_buffer[cur_point_number].y = ((cur_feature->points->y) << (-lvl_dif)) + tile_bias_y;
                cur_point_number++;
            }
            for (i = 1; i < cur_feature->point_nums - 1; i++) {
                tk_gdi.draw_point_buffer[cur_point_number].x = (((cur_feature->points) + i)->x << (-lvl_dif)) + tile_bias_x;
                tk_gdi.draw_point_buffer[cur_point_number].y = (((cur_feature->points) + i)->y << (-lvl_dif)) + tile_bias_y;
                cur_point_number++;
            }
            if ((cur_feature->next == NULL) || cur_feature->next->tile->flag != tk_engine.tile_flag) {
                tk_gdi.draw_point_buffer[cur_point_number].x = (((cur_feature->points) + (cur_feature->point_nums-1))->x << (-lvl_dif)) + tile_bias_x;
                tk_gdi.draw_point_buffer[cur_point_number].y = (((cur_feature->points) + (cur_feature->point_nums-1))->y << (-lvl_dif)) + tile_bias_y;
                cur_point_number++;
            }
        }
            cur_feature = cur_feature->next;
            if ((cur_feature != NULL) && (cur_feature->tile->flag != tk_engine.tile_flag))
                break;
    }
    (*ptnum) = cur_point_number;
}

/*
 * draw polygon module
 */

//#define MAX_EDGE_NUM 16384
#define MAX_EDGE_NUM 2048
typedef struct tEdge {
	int ymin;
	int direction;
	float x, dx;
	struct tEdge *next;
} Edge;

static Edge **pedges = NULL;//the pointer to each tile's edge
static Edge *pedge = NULL;	
static Edge *edges;//all the edges
static int edge_counter = 0;
static int scan_max = 0; // = env.bottom
static unsigned short max_tile_size = 0;// = tk_gdi_get_screen_height() + 160

//在一点上按边从左到右顺序排序
static void insert_edge(Edge *list, Edge *edge) 
{
	Edge *p = list->next, *q = list;

	while (p) {
		if (edge->x < p->x || edge->x + edge->dx < p->x + p->dx)
			p = 0;
		else {
			q = p;
			p = p->next;
		}
	}
	edge->next = q->next;
	q->next = edge;
}

//当前绘图时的活化边的建立
static void build_active_list(int scan, Edge *active, Edge *edges[]) 
{
	Edge *p, *q;

	p = edges[scan]->next;
	while (p) {
		q = p->next;
		insert_edge(active, p);
		p = q;
	}
}

//填充多边形边界外
static void fill_scan_line_in(int scan, Edge *active) 
{
	Edge *p1, *p2;
	p1 = active->next;
	if (p1 == NULL)
		return;
	while (p1) {
		p2 = p1->next;
		if (p2) {
          if (p1->x != p2->x) {
            cairo_line_to(cr, (int)(p1->x + 0.5), scan);
            cairo_line_to(cr, (int)(p2->x + 0.5), scan);
            cairo_stroke(cr);
          
		    //draw_hline((int)(p1->x + 0.5), scan, (int)(p2->x + 0.5));
          }
			p1 = p2->next;
		}
		else {
            cairo_line_to(cr , (int)(p1->x + 0.5), scan);
            cairo_line_to(cr , (int)(tk_gdi.rtview.right), scan);
            cairo_stroke(cr);
            //draw_hline((int)(p1->x + 0.5), scan, tk_gdi.rtview.right);
			p1 = NULL;
		}
	}
    //cairo_stroke(cr);
}

//更新活性边的x
static void update_active_list(int scan, Edge *active) 
{
	Edge *q = active, *p = active->next;

	while (p) {
		if (scan <= (int)p->ymin) {
			p = p->next;
			//当一条边失活的时候要将其活化边表中删除
			q->next = p;
		} else {
			p->x = p->x + p->dx;
			q = p;
			p = p->next;
		}
	}
}

/* 
 * env: the envelope of the area
 * */
void begin_area_fill() 
{
	int scan;

	max_tile_size = tk_engine.height + 160;
	scan_max = tk_gdi.rtview.bottom;
	pedges = malloc(max_tile_size * sizeof(Edge *));
    edges = malloc((MAX_EDGE_NUM * 2 + max_tile_size * 2) * sizeof(Edge));
	pedge = edges + (MAX_EDGE_NUM * 2 + max_tile_size * 2) - 1;

	for (scan = 0; scan < max_tile_size; scan++) {
		pedges[scan] = pedge;
		pedges[scan]->next = 0;
		pedge--;
	}
	pedge = edges;
}

/*
 * left : the area's left value
 * right: the area's right value 
 * */
void draw_area()
{
	Edge active;
	int scan, direction = 0;

	max_tile_size = tk_engine.height + 80;
	for (scan = max_tile_size - 1; scan >= 0; scan--) {
		if (pedges[scan]->next != NULL)
			break;
	}
	if (scan == -1)
		return;
	active.next = NULL;
	direction = pedges[scan]->next->direction;

	scan = scan_max; 
	while (scan >= 0) {
		build_active_list(scan, &active, pedges);
        fill_scan_line_in(scan, &active);
		update_active_list(--scan, &active);
	}
}

void end_area_fill() 
{
    free(edges);
    free(pedges);	
    pedge = NULL;
    scan_max = 0;
	edge_counter = 0;
	max_tile_size = 0;
}

/* 从pedge中抽出边来构造多边形的边，其中要求n多边形具有n+1个点,当绘图完毕后其结构会被改变
 * n: num of the feature's points.
 * pts: pointer to the feature's points.
 */
void add_polygon(int n, struct tk_point *pts) 
{
    Edge *edge;
    struct tk_point *p1, *p2;
    int i;

    p1 = pts + 0;
    if (edge_counter + n > MAX_EDGE_NUM * 2)
        return;
    for (i = 1; i < n; i++) {
        p2 = pts + i;
        if (p1->y != p2->y) {
            edge = pedge;
            edge->next = NULL;
            edge_counter++;
            (pedge)++;
            edge->dx = -(float)(p2->x - p1->x)/(p2->y - p1->y);
            if (p1->y > p2->y) {
                edge->direction = 1;
                edge->x = (float)(p1->x);
                edge->ymin = p2->y;
                insert_edge(pedges[p1->y], edge);
            } else {
                edge->direction = -1;
                edge->x = (float)(p2->x);
                edge->ymin = p1->y;
                insert_edge(pedges[p2->y], edge);
            }
        }
        p1 = p2;
    }
    p1 = pts + 0;
    if (p1->x != p2->x || p1->y != p2->y) {
        if (p1->y != p2->y) {
            edge = pedge;
            edge->next = NULL;
            (pedge)++;
            edge_counter++;
            edge->dx = -(float)(p2->x - p1->x)/(p2->y - p1->y);
            if (p1->y > p2->y) {
                edge->direction = 1;
                edge->x = (float)(p1->x);
                edge->ymin = p2->y;
                insert_edge(pedges[p1->y], edge);
            } else {
                edge->direction = -1;
                edge->x = (float)(p2->x);
                edge->ymin = p1->y;
                insert_edge(pedges[p2->y], edge);
            }
        }
    }
}


/*绘制地图的面状图层
 * pl: the pointer to the map layer which type is polygon such as water and greenland.
 * tk_gdi.rtview: the screen
 * vlevel: map's current zoom_level
 * return 0 if fail
 * return 1 if success.
 */



/* draw linefeature, including rail, road, etc */

static int double_round(double d)
{
    if (d - floor(d) < 0.5)
        return (int) floor(d);
    else
        return (int) floor(d) + 1;
}

static void _tk_gdi_calcorto(int xDiff, int yDiff, double r, int *px, int *py)
{
	double sq;
   if (xDiff == 0 && yDiff == 0) {
       *px = 0;
       *py = 0;
       return;
   }
   sq = sqrt(xDiff * xDiff + yDiff * yDiff);
   *px = (int)double_round(r * (-yDiff) / sq);
   *py = (int)double_round(r * xDiff / sq);
   return;
}
static  void draw_line_Bresenham(int x1,int y1,int x2,int y2)
{
    int rx, ry, e,deltax,deltay,deltax2,deltay2;
    e = 0;
    deltax = x2 - x1;
    deltay = y2 - y1;
    deltax2 = deltax << 1;
    deltay2 = deltay << 1;
    TK_GDI_SETPIXEL( x1, y1, TK_GDI_CI_COLOR);
    for(rx = x1 + 1,ry = y1 + 1;rx <= x2;++rx){
        e += deltay2;
        if(e > deltax2){
            e -= deltax2;
            ++ry;
        }
        TK_GDI_SETPIXEL( rx, ry, TK_GDI_CI_COLOR);
    }
}

static void add_slope_xy(int x1,int y1,int x2,int y2,int length)
{
    int rx, ry, e,deltax,deltay,deltax2,deltay2;
    e = 0;
    deltax = x2 - x1;
    deltay = y2 - y1;
    deltax2 = deltax << 1;
    deltay2 = deltay << 1;
    for(rx = x2 + 1,ry = y2 + 1;rx <= length;++rx){
        e += deltay2;
        if(e > deltax2){
            e -= deltax2;
            ++ry;
        }
        x2 = rx;
        y2 = ry;
    }
}
static void draw_line1(struct tk_point *pts,int num)
{
    int loop_index;
    for(loop_index = 0;loop_index < num - 1;loop_index++){
        draw_line_Bresenham(pts[loop_index].x,pts[loop_index].y, \
                pts[loop_index+1].x,pts[loop_index+1].y);
    }
}

static int draw_line(struct tk_point *pts, int num)
{
    int j, x1, y1, x2, y2;

    if (is_draw_subway) {
        cairo_set_line_width(cr,tk_gdi.nPSize); 
        cairo_set_source_rgb(cr,(double)((TK_GDI_CI_COLOR >> 16)&0xff)/(double)256,\
                (double)((TK_GDI_CI_COLOR >> 8)&0xff)/(double)256,\
                (double)((TK_GDI_CI_COLOR )&0xff)/(double)256);
        cairo_move_to(cr, pts[0].x, pts[0].y);
        for (j = 1; j < num; j++) {
            x1 = pts[j].x;
            y1 = pts[j].y;
            cairo_line_to(cr, pts[j].x, pts[j].y);
        }
        cairo_stroke(cr);
        return 1;
    }
    for (j = 0; j < num-1; j++) {
        x1 = pts[j].x;
        y1 = pts[j].y;
        x2 = pts[j + 1].x;
        y2 = pts[j + 1].y;

        int xa, ya, xb, yb, i, dx, dy, pk, towdx, towdy, towdydx;//, t;
        if (x1 > x2) {
            xa = x2;
            ya = y2;
            xb = x1;
            yb = y1;
        } else {
            xa = x1;
            ya = y1;
            xb = x2;
            yb = y2;
        }
        dx = xb - xa;
        dy = yb - ya;

        if (dy >= 0) {
            towdy = dy * 2;
            towdx = dx * 2;
            towdydx = (dy - dx)*2;

            TK_GDI_SETPIXEL(xa,ya,TK_GDI_CI_COLOR);

            if (abs(dx) >= abs(dy)) {
                pk = 2 * dy - dx;
                for (i = 0; i < abs(dx); i++) {
                    xa++;
                    if (pk > 0) {
                        ya++;
                        pk += towdydx;
                    } else {
                        pk += towdy;
                    }

                    TK_GDI_SETPIXEL(xa,ya,TK_GDI_CI_COLOR);

                }
            } else {
                TK_GDI_SETPIXEL(xa,ya,TK_GDI_CI_COLOR);
                pk = 2 * dx + 2 * dy - 1;
                for (i = 0; i < abs(dy); i++) {
                    ya++;
                    if (pk > 0) {
                        xa++;
                        pk -= towdydx;
                    } else {
                        pk += towdx;
                    }
                    TK_GDI_SETPIXEL(xa,ya,TK_GDI_CI_COLOR);
                }
            }
        } else {
            TK_GDI_SETPIXEL(xa,ya,TK_GDI_CI_COLOR);
            towdy = (-1)*(dy * 2);
            towdx = (-1)*(dx * 2);
            towdydx = (-1)*(dy + dx)*2;
            if (abs(dx) >= abs(dy)) {
                pk = (-1)*dx;
                for (i = 0; i < abs(dx); i++) {
                    xa++;
                    if (pk > 0) {
                        ya--;
                        pk += towdydx;
                    } else {
                        pk += towdy;
                    }

                    TK_GDI_SETPIXEL(xa,ya,TK_GDI_CI_COLOR);

                }
            } else {
                pk = 2*(dx + dy) + 1;

                TK_GDI_SETPIXEL(xa,ya,TK_GDI_CI_COLOR);

                for (i = 0; i < abs(dy); i++) {
                    yb++;
                    if (pk > 0) {
                        xb--;
                        pk -= towdydx;
                    } else {
                        pk -= towdx;
                    }

                    TK_GDI_SETPIXEL(xb,yb,TK_GDI_CI_COLOR);

                }
            }
        }
    }
    return 1;
}

extern int layindex;
//绘制带边框的粗线,且边框不能覆盖道路色
static int draw_normal_line(int x0, int y0, int x1, int y1)
{
    if(is_draw_subway){
        cairo_line_to(cr, x0, y0);
        cairo_line_to(cr, x1, y1);

        tk_gdi.is_road = 0;
        return 1;
    }
    int xdiff = x0 - x1, ydiff = y0 - y1;
    struct tk_point pts[2];
    pts[0].x = x0;
    pts[0].y = y0;
    pts[1].x = x1;
    pts[1].y = y1;
    if (tk_gdi.nPSize == 0) {
        draw_line(pts, 2);//hqk
        return 1;
    }
    if (tk_gdi.nPSize == 1) {
        draw_line(pts, 2);
        if (abs(xdiff) > abs(ydiff)) {
            pts[0].y = y0 + 1;
            pts[1].y = y1 + 1;
            draw_line(pts, 2);//hqk
        } else {
            pts[0].x = x0 + 1;
            pts[1].x = x1 + 1;
            draw_line(pts, 2);//hqk
        }
        return 1;
    }

    if (xdiff | ydiff) {
        struct tk_point ptPoly[4];
        int xOff, yOff, xOffP, yOffP, xOffM, yOffM;

        if (tk_gdi.nPSize % 2 == 0 && tk_gdi.nPSize != 2)
            _tk_gdi_calcorto(x0 - x1, y0 - y1, tk_gdi.nPSize - 0.6, &xOff, &yOff);
        else
            _tk_gdi_calcorto(x0 - x1, y0 - y1, tk_gdi.nPSize, &xOff, &yOff);

        /* Do rounding for offsets */
        if (xOff > 0) {
            xOffP = (xOff + 1) / 2;
            xOffM =  xOff   / 2;
        } else {
            xOffP = xOff / 2;
            xOffM = (xOff - 1) / 2;
        }
        if (yOff >0) {
            yOffP = (yOff + 1)/ 2;
            yOffM = yOff / 2;
        } 
        else {
            yOffP =  yOff / 2;
            yOffM =  (yOff - 1) / 2;
        }
        ptPoly[0].x = x0 + xOffP;			ptPoly[0].y = y0 + yOffP;
        ptPoly[1].x = x0 - xOffM;			ptPoly[1].y = y0 - yOffM;
        ptPoly[2].x = x1 - xOffM;			ptPoly[2].y = y1 - yOffM;
        ptPoly[3].x = x1 + xOffP;			ptPoly[3].y = y1 + yOffP;
        tk_gdi.is_road = 1;
        if (TK_ABS(xdiff) >= TK_ABS(ydiff))
            fill_polygon(ptPoly, 4, 0);
        else
            fill_polygon(ptPoly, 4, 1);
        tk_gdi.is_road = 0;
    }
    return 1;
}

static int draw_wide_line(int x0, int y0, 
        int x1, int y1, int width, int color) 
{
    if(is_draw_subway)
    {
        cairo_line_to(cr, x0, y0);
        cairo_line_to(cr, x1, y1);
        return 1;
    }
    int xdiff = x0 - x1, ydiff = y0 - y1, pwidth = width;
    struct tk_point ptPoly[4];
    int xOff, yOff, xOffP, yOffP, xOffM, yOffM;

    int buff = tk_gdi.color;
    GDI_SETCOLOR(color);
    GDI_SETPENSIZE((unsigned char)(pwidth - 1));

    _tk_gdi_calcorto(x0 - x1, y0 - y1, pwidth - 1, &xOff, &yOff);

    /* Do rounding for offsets */
    if (xOff >0) {
        xOffP = (xOff+1)/2;
        xOffM =  xOff   /2;
    } else {
        xOffP =   xOff/2;
        xOffM =  (xOff-1)   /2;
    }
    if (yOff >0) {
        yOffP = (yOff+1)/2;
        yOffM =  yOff   /2;
    } else {
        yOffP =   yOff/2;
        yOffM =  (yOff-1)   /2;
    }
    ptPoly[0].x = x0 + xOffP;
    ptPoly[0].y = y0 + yOffP;
    ptPoly[1].x = x0 - xOffM;
    ptPoly[1].y = y0 - yOffM;
    ptPoly[2].x = x1 - xOffM;
    ptPoly[2].y = y1 - yOffM;
    ptPoly[3].x = x1 + xOffP;
    ptPoly[3].y = y1 + yOffP;
    if (TK_ABS(xdiff) >= TK_ABS(ydiff))
        fill_polygon(ptPoly, 4, 0);
    else
        fill_polygon(ptPoly, 4, 1);
    tk_gdi.color = buff;
    return 1;    
}

static int draw_dash_line(int x0, int y0, int x1, int y1, double* remain)
{
    unsigned int buff_color = tk_gdi.color;
    unsigned int buff_bk_color = tk_gdi.bgcolor;
    double all_distance = sqrt((x0 - x1) * (x0 - x1) + (y0 - y1) * (y0 -y1));
    int cur_x = x0;
    int cur_y = y0;
    double cos_x = (x1 - x0) / all_distance ; 
    double sin_y = (y1 - y0) / all_distance ; 
    int drawed_distance = 0;
    int dash_remain = WHITE_DASH_LEN + COLOR_DASH_LEN
        - ((int)(*remain) % (WHITE_DASH_LEN + COLOR_DASH_LEN));
    
    (* remain) += all_distance;
    if (all_distance == 0 ) return 1;
        tk_gdi.nPSize = 2;
        tk_gdi.bgcolor = tk_gdi.color;
        if(is_draw_subway){
            cairo_set_line_width(cr,tk_gdi.nPSize);
            cairo_set_source_rgb(cr,(double)((TK_GDI_CI_COLOR >> 16)&0xff)/(double)255,\
                                (double)((TK_GDI_CI_COLOR >> 8)&0xff)/(double)255,\
                                (double)((TK_GDI_CI_COLOR )&0xff)/(double)255); 
            draw_normal_line(x0, y0, x1, y1);
            cairo_stroke(cr); 
        }
        else
            draw_normal_line(x0, y0, x1, y1);
        tk_gdi.color = TK_COLOR_WHITE;
        //需要先开始画部分白色
    if ((dash_remain) < WHITE_DASH_LEN && (dash_remain) >= 0 ){
        tk_gdi.color = TK_COLOR_WHITE;
        if ((dash_remain) >= all_distance){
            if(is_draw_subway){
                cairo_set_line_width(cr,tk_gdi.nPSize);
                cairo_set_source_rgb(cr,(double)((TK_GDI_CI_COLOR >> 16)&0xff)/(double)255,\
                                    (double)((TK_GDI_CI_COLOR >> 8)&0xff)/(double)255,\
                                    (double)((TK_GDI_CI_COLOR )&0xff)/(double)255);
                draw_normal_line(x0, y0, x1, y1);
                cairo_stroke(cr);
            }else
                draw_normal_line(x0, y0, x1, y1);
            (dash_remain) -= (int)all_distance;
            tk_gdi.color = buff_color;
            tk_gdi.nPSize = 4;
            return 1;
        } else{
            cur_x = (int)(x0 + (dash_remain) * cos_x);
            cur_y = (int)(y0 + (dash_remain) * sin_y);
            if(is_draw_subway){
                cairo_set_line_width(cr,tk_gdi.nPSize);
                cairo_set_source_rgb(cr,(double)((TK_GDI_CI_COLOR >> 16)&0xff)/(double)256,\
                                    (double)((TK_GDI_CI_COLOR >> 8)&0xff)/(double)256,\
                                    (double)((TK_GDI_CI_COLOR )&0xff)/(double)256); 
                draw_normal_line(x0, y0, cur_x, cur_y);
                cairo_stroke(cr);
            }else
                draw_normal_line(x0, y0, cur_x, cur_y);
            all_distance -= (dash_remain);
            drawed_distance += (dash_remain); 
            (dash_remain) = 0;

        } 
    }
    if ((dash_remain) == 0) 
        (dash_remain) = WHITE_DASH_LEN + COLOR_DASH_LEN;
    while(1){
        if (all_distance > (dash_remain)){
            if(is_draw_subway){
                cairo_set_line_width(cr,tk_gdi.nPSize);
                cairo_set_source_rgb(cr,(double)((TK_GDI_CI_COLOR >> 16)&0xff)/(double)256,\
                                    (double)((TK_GDI_CI_COLOR >> 8)&0xff)/(double)256,\
                                                    (double)((TK_GDI_CI_COLOR )&0xff)/(double)256);
                draw_normal_line((int)(x0 + (drawed_distance + (dash_remain) - WHITE_DASH_LEN) * cos_x), 
                    y0 + (int)((drawed_distance + (dash_remain) - WHITE_DASH_LEN) * sin_y),
                    x0 + (int)((drawed_distance + (dash_remain)) * cos_x),
                    y0 + (int)((drawed_distance + (dash_remain)) * sin_y));
                cairo_stroke(cr);
            }else
            draw_normal_line((int)(x0 + (drawed_distance + (dash_remain) - WHITE_DASH_LEN) * cos_x), 
                    y0 + (int)((drawed_distance + (dash_remain) - WHITE_DASH_LEN) * sin_y),
                    x0 + (int)((drawed_distance + (dash_remain)) * cos_x),
                    y0 + (int)((drawed_distance + (dash_remain)) * sin_y));
            all_distance -= (dash_remain);
            drawed_distance += (dash_remain);
            (dash_remain) = WHITE_DASH_LEN + COLOR_DASH_LEN;
        } else {
            if (all_distance < ((dash_remain) - WHITE_DASH_LEN)){
                dash_remain = (int)(dash_remain - all_distance);
                break;
            } else {
                if(is_draw_subway){
                    cairo_set_line_width(cr,tk_gdi.nPSize);
                    cairo_set_source_rgb(cr,(double)((TK_GDI_CI_COLOR >> 16)&0xff)/(double)256,\
                                        (double)((TK_GDI_CI_COLOR >> 8)&0xff)/(double)256,\
                                                        (double)((TK_GDI_CI_COLOR )&0xff)/(double)256);
                    draw_normal_line(x0 + (int)((drawed_distance + (dash_remain) - WHITE_DASH_LEN) * cos_x), 
                        y0 + (int)((drawed_distance + (dash_remain) - WHITE_DASH_LEN) * sin_y),
                        x1,  y1);
                    cairo_stroke(cr);
                }else
                draw_normal_line(x0 + (int)((drawed_distance + (dash_remain) - WHITE_DASH_LEN) * cos_x), 
                        y0 + (int)((drawed_distance + (dash_remain) - WHITE_DASH_LEN) * sin_y),
                        x1,  y1);
                dash_remain = (int)(dash_remain - all_distance);
                break;
            }
        }
    }
    tk_gdi.color = buff_color;
    tk_gdi.bgcolor = buff_bk_color;
    tk_gdi.nPSize = 4;   
    return 1;
}

/* fill the line corner */
static void fill_circle_bound(int x0, int y0, int r, unsigned int nCIndex)
{
    int i, imax = (int)(r*707/1000+1);
    int sqmax = r*r+r/2, x = r, color;
    color = TK_GDI_CI_COLOR;
    TK_GDI_CI_COLOR = nCIndex;
    draw_hline(x0-r, y0, x0+r);
    for( i=1; i<=imax; i++ )
    {
        if( (i*i+x*x) > sqmax )
        {
            // draw lines from outside 
            if(x > imax) {
                if (TK_IS_IN_SCREEN(x0-i,y0+x))
                    TK_GDI_SETPIXEL_NO_OVERLAP(x0-i, y0+x, tk_gdi.bgcolor);
                if (TK_IS_IN_SCREEN(x0-i,y0-x))
                    TK_GDI_SETPIXEL_NO_OVERLAP(x0-i, y0-x, tk_gdi.bgcolor); 
                if (TK_IS_IN_SCREEN(x0+i,y0+x))
                    TK_GDI_SETPIXEL_NO_OVERLAP(x0+i, y0+x, tk_gdi.bgcolor); 
                if (TK_IS_IN_SCREEN(x0+i,y0-x))
                    TK_GDI_SETPIXEL_NO_OVERLAP(x0+i, y0-x, tk_gdi.bgcolor);
                draw_hline(x0-i+1, y0+x, x0+i-1);
                draw_hline(x0-i+1, y0-x, x0+i-1);
            }
            x--;
        }
        //draw lines from inside (center) 
        if ( TK_IS_IN_SCREEN(x0-x-1,y0+i+1))
            TK_GDI_SETPIXEL_NO_OVERLAP(x0-x-1, y0+i+1, tk_gdi.bgcolor);
        if ( TK_IS_IN_SCREEN(x0-x-1,y0-i-1))
            TK_GDI_SETPIXEL_NO_OVERLAP(x0-x-1, y0-i-1, tk_gdi.bgcolor); 
        if ( TK_IS_IN_SCREEN(x0+x+1,y0+i+1))
            TK_GDI_SETPIXEL_NO_OVERLAP(x0+x+1, y0+i+1, tk_gdi.bgcolor); 
        if ( TK_IS_IN_SCREEN(x0+x+1,y0-i-1))
            TK_GDI_SETPIXEL_NO_OVERLAP(x0+x+1, y0-i-1, tk_gdi.bgcolor);
        draw_hline(x0-x, y0+i, x0+x);
        draw_hline(x0-x, y0-i, x0+x);
    }
    TK_GDI_CI_COLOR = color;
}

float get_slope(float  x1,float  y1,float x2,float y2)
{
    if(0 == (x2 - x1))
        return 1;
    return (y2 - y1)/(x2 - x1);
} 


//void get_point_offset(tk_point *pt1, tk_point *pt2, int width, tk_point *pt_offset1, tk_point * pt_offset2)
//绘制线
int draw_misc_line(struct tk_point *pPts, int ptNum, double* remain, int ft_type)
{
    int i; 
    //for(i = 0; i < ptNum; i++)
      //  LOG_DBG("----%d------%d\n",pPts[i].x, pPts[i].y);
    if (tk_gdi.nPSize < 1)
        return 1; 
    if (tk_gdi.nPSize == 1) {
        draw_line(pPts, ptNum); //hqk
        return 1;
    }
    if (tk_gdi.nLStyle == TKGDI_ENLSTYLE_DASH) {
        for( i = 0; i < ptNum - 1; i++){
               draw_dash_line(pPts[i].x, pPts[i].y, pPts[i + 1].x, pPts[i + 1].y, remain);
            }
        return 1;
    }
    if (tk_gdi.nPSize == 2) {
        if(is_draw_subway){
            cairo_set_line_width(cr,tk_gdi.nPSize);
            cairo_set_source_rgb(cr,(double)((TK_GDI_CI_COLOR >> 16)&0xff)/(double)256,\
                                    (double)((TK_GDI_CI_COLOR >> 8)&0xff)/(double)256,\
                                    (double)((TK_GDI_CI_COLOR )&0xff)/(double)256);
            cairo_move_to(cr,pPts[0].x,pPts[0].y);
        }
            for(i = 0; i < ptNum - 1; i++){
                draw_wide_line(pPts[i].x, pPts[i].y, pPts[i + 1].x, pPts[i + 1].y, 3, tk_gdi.color);
            }
       if(is_draw_subway){
            cairo_stroke(cr);
       }
        return 1;
    }
    if ((tk_gdi.nPSize > 2) && (tk_gdi.nLStyle == TKGDI_ENLSTYLE_PIPE))
    {
        /*for (i = 0; i < ptNum; i++){
                  fill_circle_bound(pPts[i].x, pPts[i].y, (tk_gdi.nPSize)/2-1, TK_GDI_CI_COLOR);
           }*/
    }
    if(is_draw_subway){
        cairo_set_line_width(cr,tk_gdi.nPSize );
        cairo_set_line_cap(cr, CAIRO_LINE_CAP_BUTT);
        cairo_set_source_rgb(cr,(double)((tk_gdi.bgcolor >> 16)&0xff)/(double)256,\
                            (double)((tk_gdi.bgcolor >> 8)&0xff)/(double)256,\
                            (double)((tk_gdi.bgcolor )&0xff)/(double)256);
        cairo_move_to(cr,pPts[0].x,pPts[0].y);
        //if(pPts[0].x == 0 || pPts[0].x >= 254 ||
          // pPts[0].y == 0 || pPts[0].y >= 254)
            //cairo_set_line_cap(cr, CAIRO_LINE_CAP_SQUARE);
    
        for(i = 0; i < ptNum - 1; i++) 
            draw_normal_line(pPts[i].x, pPts[i].y, pPts[i + 1].x, pPts[i + 1].y);
        if(pPts[ptNum - 1].x == 0 || pPts[ptNum - 1].x >= 254 ||
           pPts[ptNum - 1].y == 0 || pPts[ptNum - 1].y >= 254)
            //cairo_set_line_cap(cr, CAIRO_LINE_CAP_SQUARE);
        //cairo_stroke_preserve(cr);
        if (ft_type == 9)
            cairo_stroke(cr);
    }

    if(is_draw_subway && ft_type == 9){
    //if(is_draw_subway){
        //cairo_fill_preserve (cr);
        cairo_set_line_width(cr,tk_gdi.nPSize - 2);
        cairo_set_line_cap(cr, CAIRO_LINE_CAP_ROUND);
        cairo_set_source_rgb(cr,(double)((TK_GDI_CI_COLOR >> 16)&0xff)/(double)256,\
                            (double)((TK_GDI_CI_COLOR >> 8)&0xff)/(double)256,\
                            (double)((TK_GDI_CI_COLOR )&0xff)/(double)256);
        cairo_move_to(cr,pPts[0].x,pPts[0].y);
    
        for(i = 0; i < ptNum - 1; i++) 
            draw_normal_line(pPts[i].x, pPts[i].y, pPts[i + 1].x, pPts[i + 1].y);
        cairo_stroke(cr);
    }
    return 1;
}

static double get_all_distance(struct tk_point* dpb, struct tk_point* source, int index)
{
    short dpb_index = get_clip_index(index);
    double cur_rle = 0;
    int i;
    struct tk_point* start;
    struct tk_point* end;
    for (i = 0 ; i< dpb_index; i++){
        start = dpb + (i );
        end = dpb + (i + 1);
        cur_rle += 
            sqrt((start->x - end->x) * (start->x - end->x) +(start->y - end->y) *(start->y - end->y)); 
    }
    start = dpb + dpb_index;
    end = source;
    cur_rle += 
        sqrt((start->x - end->x) * (start->x - end->x) +(start->y - end->y) *(start->y - end->y)); 
    return cur_rle;
}

//计算中心点到折线的距离， 从横向和纵向选择，取最小者
static int point_toLine_distance(struct tk_point* ptL, int ptnum, struct tk_point* center)
{
	double y_dis_min = 320;//tk_g_lcd_height;
	double x_dis_min = 240;//tk_g_lcd_width;
	int i = 0;
	for ( i = 0; i < ptnum-1; i++){
		int dx1 = center->x -  ptL[i+1].x;
		int dx0 = center->x - ptL[i].x;  
		int dy1 = center->y -  ptL[i+1].y;
		int dy0 = center->y - ptL[i].y; 
		
		if ((dx1 <= 0 && dx0 >= 0)||(dx0 <= 0 && dx1 >= 0)){
			double y_new = -1 ;
						//cal y new
			if (dx1 == 0 && dx0 == 0){
				 y_new = TK_MIN (TK_ABS(dy1), TK_ABS(dy0));
			} else{
				y_new = TK_ABS(dy1 * TK_ABS(dx0) + dy0 * TK_ABS(dx1))/(TK_ABS(dx0) + TK_ABS(dx1));
			}

			if (y_new < y_dis_min && y_new != -1)
				y_dis_min = y_new;
		}

		if ((dy1 <= 0 && dy0 >= 0)||(dy0 <= 0 && dy1 >= 0)){
			double x_new = -1;
			//cal x new
			if (dy1 == 0 && dy0 == 0){
				x_new = TK_MIN (TK_ABS(dx1), TK_ABS(dx0));
			} else{
				x_new = TK_ABS(dx1 * TK_ABS(dy0) + dx0 * TK_ABS(dy1))/(TK_ABS(dy0) + TK_ABS(dy1));
			}
			
			if (x_new < x_dis_min && x_new != -1)
				x_dis_min = x_new;
		}
	}
	return (int) TK_MIN(x_dis_min, y_dis_min);
}

static int point_distance(struct tk_point* pt, struct tk_point* center)
{
	return (TK_ABS(pt->x - center->x) + TK_ABS(pt->y - center->y))*2/3;
}

int pointinrect(int nx, int ny, int nx1, int ny1, int nx2, int ny2)
{
	if( (nx >= TK_MIN(nx1, nx2)) && (nx <= TK_MAX(nx1, nx2)) &&
		(ny >= TK_MIN(ny1, ny2)) && (ny <= TK_MAX(ny1, ny2)) )
		return 1;
	return 0;
}


void tk_set_filter_screen(struct envelope rect)
{
    draw_filter.bottom = rect.bottom;
    draw_filter.top = rect.top;
    draw_filter.left = rect.left;
    draw_filter.right = rect.right;
    return;
}

void tk_set_rect(struct envelope *rect,int left,int right,int bottom,int top)
{
    rect->left = left;
    rect->right = right;
    rect->bottom = bottom;
    rect->top = top;
}


static void	tk_gdi_fillrect(int x0, int y0, int x1, int y1)
{
	for( ; y0 <= y1; y0++) {
		draw_hline(x0, y0, x1);
    }
}

static void	tk_gdi_fillrect_color(int x0, int y0, int x1, int y1,int color)
{
    int i = 0;
	for( ; y0 <= y1; y0++) {
        for(i = x0; i <= x1; i++)
            tk_gdi.pd[i + y0 * tk_engine.width] = color;
    }
}

void tk_reset_filter_screen()
{
    draw_filter.bottom = TILE_SIZE - 1;//tk_engine.height-1;
    draw_filter.top = 0;
    draw_filter.left = 0;
    draw_filter.right = TILE_SIZE - 1;//tk_engine.width-1;
    return;
}

//static void	tk_gdi_fillrect(int x0, int y0, int x1, int y1)
//{
//	for( ; y0 <= y1; y0++) {
//		draw_hline(x0, y0, x1);
  //  }
//}

void tk_gdi_cleancdata(struct envelope filter, short int zoom_level, int is_file_not_found_scene)
{
    struct envelope env;
    if (zoom_level > TK_NATIONAL_LEVEL_A || is_file_not_found_scene){
        GDI_SETCOLOR(TK_GDI_COLOR_BG);
    }
    else{
        GDI_SETCOLOR(TK_GDI_COLOR_WATER);
    }

    memcpy(&env, &draw_filter, sizeof(struct envelope));

    tk_reset_filter_screen();

    tk_gdi_fillrect(filter.left, filter.top, filter.right, filter.bottom);

    memcpy(&draw_filter, &env, sizeof(struct envelope));
}


/* =========================================================
 * the label management part
 * ========================================================= */

/* TODO: Will add more element in label_struct in future */
struct tk_label_struct
{
    char *name; 
    int name_idx;
    unsigned char name_len;
    char label_level; 
    unsigned char rect_num;
    char need_draw;
    struct envelope *pRects;/* why point*/ 
    struct feature *pfeature;
    gdi_color txtcol;
    int icon_id;  //add by heqk  2011-05-30
    struct tk_point p_poi;
};

struct must_show_icon {
    struct tk_point icon_p;
    int    icon_id;
};

unsigned char *label_name_buffer;


/* Designe the pattern of Label */
typedef enum EN_TK_GEO_LABELSTYLE {
	TKGEO_LABEL_RIGHT_DOWN=0,
	TKGEO_LABEL_TOP_CENTER,	
	TKGEO_LABEL_COVER,	
	TKGEO_LABEL_NO_ICON, 
	TKGEO_LABEL_MAX			
} TKGeoLABELSTYLE;


#define TK_GEO_LABEL_MAXNUM 512	

#define MIN_LABEL_NUM 16
#define LABEL_NUM_INC_STEP 8

#define TK_LABEL_NAME_BUFF_MIN 2560
#define LABEL_NAME_BUFF_INC_STEP 128

struct tk_label_struct *tk_geo_labels;
int label_array_size;
int tk_geo_labels_num;

struct tk_label *label_buffer;
int glabels_num;
int glabel_array_size;

unsigned char *label_name_buffer;
int label_name_len;
int label_name_size;

//struct tk_label_struct tk_geo_labels[TK_GEO_LABEL_MAXNUM];
//int tk_geo_labels_num = 0;

void tk_init_label()
{
    label_buffer = xmalloc(sizeof(struct tk_label) * MIN_LABEL_NUM);
    glabel_array_size = MIN_LABEL_NUM;
    glabels_num = 0;

    label_name_buffer = xmalloc(TK_LABEL_NAME_BUFF_MIN);
    label_name_size = TK_LABEL_NAME_BUFF_MIN;
    label_name_len = 0;

    tk_geo_labels = xmalloc(sizeof(struct tk_label_struct) * MIN_LABEL_NUM);
    label_array_size = MIN_LABEL_NUM;
    tk_geo_labels_num = 0;
}

void tk_destroy_label()
{
    int i;

    for (i = 0; i < tk_geo_labels_num; i++) {
        free(tk_geo_labels[i].pRects);
    }
    free(tk_geo_labels);
    tk_geo_labels_num = 0;
    
    free(label_buffer);
    free(label_name_buffer);
}

static int get_empty_glabel_idx()
{
    if (glabels_num >= glabel_array_size) {
        label_buffer = xrealloc(label_buffer, sizeof(struct tk_label) * (glabel_array_size + LABEL_NUM_INC_STEP));
        glabel_array_size += LABEL_NUM_INC_STEP;
    }
    return glabels_num++;
}

static  char *add_label_name(char *source, int len)
{
    if (label_name_len + len >= label_name_size) {
        label_name_buffer = xrealloc(label_name_buffer, label_name_size + LABEL_NAME_BUFF_INC_STEP);
        label_name_size += LABEL_NAME_BUFF_INC_STEP;
    }
    memcpy(label_name_buffer + label_name_len, source, len);
    *(label_name_buffer + label_name_len + len) ='\0';
    //LOG_DBG("add label_name: {%s}\n", label_name_buffer + label_name_len);
    label_name_len += len + 1;
    return (char *)label_name_buffer + label_name_len - len - 1;
}

static int get_empty_label_idx()
{
    if (tk_geo_labels_num >= label_array_size) {
        tk_geo_labels = xrealloc(tk_geo_labels, sizeof(struct tk_label_struct) * (label_array_size + LABEL_NUM_INC_STEP));
        label_array_size += LABEL_NUM_INC_STEP;
    }
    return tk_geo_labels_num++;
}

/* Delete the map label information */
void tk_geo_cleanmaplabel() 
{
    struct tk_label_struct *plabel = NULL;
    int i = 0;
    for (i = 0; i < tk_geo_labels_num; i++) {
        plabel = tk_geo_labels + i;
        if (plabel->pRects) {
            free(plabel->pRects);
            plabel->pRects = 0;
        }
    }
    tk_geo_labels_num = 0;
    memset(tk_geo_labels, 0 ,sizeof(struct tk_label_struct) * label_array_size);

    glabels_num = 0;
    memset(label_buffer, 0 , sizeof(struct tk_label) * glabel_array_size);
    label_name_len = 0;
    memset(label_name_buffer, 0, label_name_size);
}

/* 判断feature的标注是否已经在labels列表里了
 * pft the pointer of feature
 * plabels: the list of labels.
 * label_num : the num of labels.
 * */
static int label_already_drawed(struct feature* pft, struct tk_label_struct *plabels, int label_num)
{
    int i = 0;
    int is_same_name = 0;
    struct tk_label_struct* cur_label = 0;
    for (i = 0; i < label_num; i++) {
        cur_label = plabels + i;
        if (pft->name_length != cur_label->name_len || cur_label->need_draw == 0)
            continue;
        if(strncmp(pft->name,cur_label->name,pft->name_length) == 0 ){
            is_same_name = 1;
            break;
        }
        /*   
        for(j = 0; j < pft->name_length; j++) {
            if (pft->name[j] != cur_label->name[j]) {
                is_same_name = 0;
                break;
            }
        }
        if (is_same_name == 1)
            return 1; */   
    }
    return is_same_name;
}

//判断标注是否存在覆盖(0.不覆盖 1.覆盖, 并且更改盖住的label的绘制状态)
static int geo_label_isoverleap(struct envelope label_rect,
        struct tk_label_struct *plabels, int label_num, int priority) 
{
    struct tk_label_struct *plabel = NULL;
    int i, j, k;
    for (i = 0; i < label_num; i++) {
        plabel = plabels + i;
        //printf("plabels name length = %d --- labe_num = %d\n",plabel->name_len,label_num);
        if (plabel == NULL)
            continue;
        if (plabel->need_draw == 0)
            continue;
        for (j = 0; j < plabel->rect_num; j++) {
            if (tk_geo_intersectrect(label_rect, plabel->pRects[j]) != 0) {//相交或包含
                if (0 == is_zoomed_to_max)
                    return 1;
                if (plabel->pfeature->label_priority < priority) {//比较两者的优先级
                    LOG_DBG("string Conflict ! %s", plabel->name);
                    if (plabel->need_draw != 0)
                        plabel->need_draw = -1;
                    LOG_DBG("now need draw %d", plabel->need_draw );
                } else {
                    for (k = 0; k < i; k++) {
                        LOG_DBG("Ori del string Conflict ! %s", plabel->name);
                        if (plabels[k].need_draw == -1) {
                            plabels[k].need_draw = 1;
                            LOG_DBG("nees draw changed %s", plabels[k].name);
                            LOG_DBG("now need draw %d", plabels[k].need_draw);
                        }
                    }
                    return 1;
                }
            }
        }
    }
    if (0 == is_zoomed_to_max)
        return 0;

    for (j = 0; j < label_num; j++) {
        if (plabels[j].need_draw == -1) {
            plabels[j].need_draw = 0;
            LOG_DBG("nees draw changed %s", plabels[j].name);
            LOG_DBG("now need draw %d", plabels[j].need_draw);
        }
    }
    return 0;
}

/* 如果不覆盖，则返回1
 * 其他（相交，包含）则返回0
 */
static int label_calclabelrect(struct envelope *label_rect,
        struct tk_label_struct *plabels, int label_num, int priority)
{
    struct envelope cur_rect ;
    cur_rect.left = label_rect->left;// + TK_GEO_LABEL_WIDTH;
    cur_rect.right = label_rect->right;// + TK_GEO_LABEL_WIDTH;
    cur_rect.top = label_rect->top;// + TK_GEO_LABEL_HEIGHT/3;
    cur_rect.bottom = label_rect->bottom;// + TK_GEO_LABEL_HEIGHT/3;
    //如果不覆盖，则返回1
    if (geo_label_isoverleap(cur_rect, plabels, label_num, priority) == 0)
        return 1;
    return 0;
}

int tk_init_icon_num( int icon_num) {
    img_buffer = (char *)malloc(sizeof(int) * icon_num);
    if (img_buffer == NULL)
        return -1;
    memset(img_buffer, 0, sizeof(int) * icon_num);
    return 0;
}

void tk_set_icon(int icon_id, int w, int h) {
    char *ptr = img_buffer + icon_id * sizeof(int);
    *((short *)ptr) = w;
    *((short *)(ptr + 2)) = h;
}

int  get_img_w_h(unsigned char *img_ptr, int poi_id, int *w, int *h)
{
    unsigned char *ptr = img_ptr + sizeof(int) * poi_id;
    if (img_offset[poi_id] < 0 || poi_id >= TK_IMG_TIGER_MAP_VECTOR_MAX)
        return 1;
    *w = *((short*)ptr);
    *h = *((short*)(ptr + 2));
    return 0;
}

void tk_fix_envelope_size(struct envelope *rect, struct envelope *fix_rect,int fix_size) {
     fix_rect->left = rect->left - fix_size;
     fix_rect->right = rect->right + fix_size;
     fix_rect->top = rect->top - fix_size;
     fix_rect->bottom = rect->bottom + fix_size;
}

/* 
 * ft: pointer of feature
 * pPts:pointer of points of feature
 * ptNum:num of points of feature
 * tk_gdi.rtview: the screen
 * label_type:0 continuous name, else is line name which have more than one
 * rects.
 * return 0, if error while adding the feature's label.
 * return 1 if adding success.
 * */
int tk_geo_add_label(struct feature* ft, struct tk_point *pPts, int ptNum, struct envelope rect, int label_type, int icon_id, int outline_color, int font_alter)
{
    struct envelope label_rects[32]; //the label's location
    int label_rect_num = 0;
    char *name = ft->name;
    int name_len = ft->name_length;
    int w = 12, h = 12;

    struct tk_point ptRes = pPts[0];//the continuous label's center point 

    struct tk_point_slope label_point[32];//the line label's center points
    int nLX = -1024, nLY = -1024;
    int i, j;
    /* second line index */
    struct envelope *label_rect_p = &label_rects[0];
    struct envelope label_add_poi;
    int icon_x, icon_y;

    get_img_w_h(img_buffer, icon_id, &w, &h);
    h = TK_GEO_LABEL_WIDTH;

    if (tk_geo_labels_num >= TK_GEO_LABEL_MAXNUM) {
        ft->label_priority++;
        return 0;
    }
    //printf("exit 1\n");
    if (name_len <= 0 || name_len > 60)
        return 0;
    if (label_type == 0) {
        if (ptNum == 2) {
            ptRes.x = (pPts[0].x + pPts[1].x) / 2;
            ptRes.y = (pPts[0].y + pPts[1].y) / 2;
        }
        else if (ptNum > 2)
            ptRes = pPts[(ptNum + 1) / 2];
        switch (GDI_GETLABELSTYLE()) {
            case TKGEO_LABEL_RIGHT_DOWN:
                label_rects[0].left = ptRes.x;
                label_rects[0].top = ptRes.y;
                label_rects[0].right = ptRes.x + TK_GEO_LABEL_WIDTH * (name_len / 2);
                label_rects[0].bottom = ptRes.y + TK_GEO_LABEL_HEIGHT;
                break;
            case TKGEO_LABEL_TOP_CENTER:
                if(name_len < 16){
                    label_rects[0].left = ptRes.x + w / 2;
                    label_rects[0].top = ptRes.y - TK_GEO_LABEL_WIDTH / 2 - 2;
                    label_rects[0].right = ptRes.x + TK_GEO_LABEL_WIDTH * name_len / 2 + w / 2;
                    label_rects[0].bottom = ptRes.y + TK_GEO_LABEL_HEIGHT / 2;
                } else {
                    label_rects[0].left = ptRes.x + w / 2;
                    label_rects[0].top = ptRes.y - TK_GEO_LABEL_WIDTH - 2;
                    label_rects[0].right = ptRes.x + w / 2 + TK_GEO_LABEL_WIDTH * (name_len / 2 + 1) / 2;
                    label_rects[0].bottom = ptRes.y + TK_GEO_LABEL_HEIGHT;
                }
                icon_x = ptRes.x - w / 2;
                icon_y = ptRes.y - h / 2;
                /* if(name_len < 16){
                    label_rects[0].left = ptRes.x - TK_GEO_LABEL_WIDTH * (name_len / 2 + 1) / 2;
                    label_rects[0].top = ptRes.y + h / 2;
                    label_rects[0].right = ptRes.x + TK_GEO_LABEL_WIDTH * (name_len / 2 - 1) / 2;
                    label_rects[0].bottom = ptRes.y + TK_GEO_LABEL_HEIGHT + h / 2;
                }else{
                    label_rects[0].left = ptRes.x - TK_GEO_LABEL_WIDTH * ((name_len / 2 + 1) / 2 + 1) / 2;
                    label_rects[0].top = ptRes.y  + h / 2;
                    label_rects[0].right = ptRes.x + TK_GEO_LABEL_WIDTH *((name_len / 2 - 1) / 2) / 2;
                    label_rects[0].bottom = ptRes.y + TK_GEO_LABEL_HEIGHT * 2 + h / 2;
                }*/
                break;
            case TKGEO_LABEL_COVER:
               if (name_len < 16) {
                    label_rects[0].left = ptRes.x - ((TK_GEO_LABEL_WIDTH * (name_len >> 1)) >> 1 );//TK_GEO_LABEL_WIDTH / 2 * (name_len / 2 + 1) / 2 / 2 ;
                    //label_rects[0].top = ptRes.y - TK_GEO_LABEL_HEIGHT / 2  ;
                    label_rects[0].top = ptRes.y - h / 2 - TK_GEO_LABEL_HEIGHT ;
                    label_rects[0].right = ptRes.x + TK_GEO_LABEL_WIDTH * (name_len / 2  + 1)/ 2;
                    //label_rects[0].bottom = ptRes.y + TK_GEO_LABEL_HEIGHT - TK_GEO_LABEL_HEIGHT / 2;
                    label_rects[0].bottom = ptRes.y - h / 2;
               } else {
                    label_rects[0].left = ptRes.x - (((TK_GEO_LABEL_WIDTH * (name_len >> 1)) >> 1) >> 1 );//TK_GEO_LABEL_WIDTH / 2 * (name_len / 2 + 1) / 2 / 2 ;
                    //label_rects[0].top = ptRes.y - TK_GEO_LABEL_HEIGHT / 2  ;
                    label_rects[0].top = ptRes.y - h / 2 - TK_GEO_LABEL_HEIGHT * 2 ;
                    label_rects[0].right = ptRes.x + TK_GEO_LABEL_WIDTH / 2 * (name_len / 2  + 1)/ 2 / 2;
                    //label_rects[0].bottom = ptRes.y + TK_GEO_LABEL_HEIGHT - TK_GEO_LABEL_HEIGHT / 2;
                    label_rects[0].bottom = ptRes.y - h / 2 ;
               }

               icon_x = ptRes.x - w / 2;
               icon_y = ptRes.y - h / 2;
               // label_rects[0].left = ptRes.x - TK_GEO_LABEL_WIDTH * (name_len / 2 + 1) / 2;
               // label_rects[0].top = ptRes.y - TK_GEO_LABEL_HEIGHT / 2;
               // label_rects[0].right = ptRes.x + TK_GEO_LABEL_WIDTH * (name_len / 2 - 1) / 2;
               // label_rects[0].bottom = ptRes.y + TK_GEO_LABEL_HEIGHT / 2;
                break;
            case TKGEO_LABEL_NO_ICON:
                label_rects[0].left = ptRes.x - TK_GEO_LABEL_WIDTH * (name_len / 2 + 1) / 2;
                label_rects[0].top = ptRes.y - TK_GEO_LABEL_HEIGHT / 2;
                label_rects[0].right = ptRes.x + TK_GEO_LABEL_WIDTH * (name_len / 2 - 1) / 2;
                label_rects[0].bottom = ptRes.y + TK_GEO_LABEL_HEIGHT / 2;
                break;
            default:
                label_rects[0].left = ptRes.x;
                label_rects[0].top = ptRes.y;
                label_rects[0].right = ptRes.x + TK_GEO_LABEL_WIDTH * (name_len / 2);
                label_rects[0].bottom = ptRes.y + TK_GEO_LABEL_HEIGHT;
                break;
        }
        tk_set_rect(&label_add_poi, label_rects->left - TK_GEO_LABEL_WIDTH, label_rects->right, label_rects->bottom, label_rects->top);
        //tk_fix_envelope_size( &label_add_poi,  &label_add_poi, font_alter);


        /* 如果label_rect 不包含在屏幕rect中 直接跳出 */
        /*  if (tk_geo_intersectrect(label_add_poi, rect) != 3) {
                if(name_len < 16){
                    label_rect_p->left = ptRes.x - w / 2 - TK_GEO_LABEL_WIDTH * name_len / 2;
                    label_rect_p->top = ptRes.y - TK_GEO_LABEL_WIDTH / 2 - 2;
                    label_rect_p->right = ptRes.x  - w / 2;
                    label_rect_p->bottom = ptRes.y + TK_GEO_LABEL_HEIGHT / 2;
                }else{
                    label_rect_p->left = ptRes.x - w / 2 - TK_GEO_LABEL_WIDTH * (name_len / 2 + 1) / 2;
                    label_rect_p->top = ptRes.y -  TK_GEO_LABEL_WIDTH - 2;
                    label_rect_p->right = ptRes.x - w / 2;
                    label_rect_p->bottom = ptRes.y + TK_GEO_LABEL_HEIGHT;
                }
        tk_set_rect(&label_add_poi, label_rects->left , label_rects->right + TK_GEO_LABEL_WIDTH , label_rects->bottom, label_rects->top);
        printf("%d--%d--%d--%d\n", label_add_poi.left, label_add_poi.right, label_add_poi.top, label_add_poi.bottom);
        //printf("%d--%d--%d--%d\n", rect.left, rect.right, rect.top, rect.bottom);

                if (tk_geo_intersectrect(label_add_poi, rect) != 3)
                    return 0;
        }*/
        if (tk_geo_intersectrect(label_add_poi, rect) != 3)
                    return 0;
        icon_x = ptRes.x - w / 2;
        icon_y = ptRes.y - h / 2;
        //the label of this feature need display
        ft->name_index = TK_NAME_NEED_DISPLAY;

        //是否重复绘制
        //如果之前已经添加, 则不予添加 
         if (ptNum >= 1 && label_already_drawed(ft, tk_geo_labels,
                    tk_geo_labels_num))
            return 0;
        tk_fix_envelope_size( &label_add_poi,  &label_add_poi, font_alter);
        //LOG_DBG("Begin detect ! %s", name);           
        //如果不是“不相交”的位置关系, 则不予添加 
        /* 检查跟当前存在的label是否重叠 如果重叠直接删除*/
        if (label_calclabelrect(&label_add_poi, tk_geo_labels,
                    tk_geo_labels_num, ft->label_priority) != 1) {
            if (1 == is_zoomed_to_max)
                ft->label_priority++;
            return 0;
        }
        //tk_geo_labels[tk_geo_labels_num].pfeature = ft;
       
        label_rect_num = 1;
        insert_label_buffer(label_rects, label_point, pPts, label_rect_num, name, name_len, tk_gdi.font_size, icon_id, outline_color,icon_x,icon_y);
    }
    else { //label_type != 0
        label_rect_num = name_len / 2;
        int magic_number = 0;
        int insert_flag = 0;
        int draw_flag = 0;
        
        
        while (calcpointsonline(pPts, ptNum, TK_GEO_LABEL_WIDTH, label_point, &label_rect_num, &magic_number)) {
            insert_flag = 0;
            
            //draw_flag ++;
            if ((draw_flag & 1) != 0)
                continue;
        //可能绘制
        if ((unsigned char)name[0] == 163 && ((unsigned char)name[1] == 199 /*|| (unsigned char)name[1] == 211 */)
               && (ft->type == 6 || ft->type == 5)) {

           w = 26;
           h = 12;
           label_rects[0].left = ptRes.x - TK_GEO_LABEL_WIDTH / 2 * (name_len / 2 + 1) / 2 / 2  - 4;
           label_rects[0].top = ptRes.y - TK_GEO_LABEL_HEIGHT / 2 - 2 ;
           label_rects[0].right = ptRes.x + TK_GEO_LABEL_WIDTH / 2 * name_len / 2 / 2;
           label_rects[0].bottom = ptRes.y + TK_GEO_LABEL_HEIGHT - TK_GEO_LABEL_HEIGHT / 2;
           label_rect_num = 1;
           icon_x = ptRes.x - w / 2;
           icon_y = ptRes.y - h / 2;
           if (tk_geo_intersectrect(label_rects[0], rect) != 3) {
               continue;
           }
           if (label_calclabelrect(&label_rects[0], tk_geo_labels,
                    tk_geo_labels_num, ft->label_priority) != 1)
                    continue; 
           insert_label_buffer(label_rects, label_point, pPts, label_rect_num, name, name_len, 8, icon_id, outline_color, icon_x, icon_y);
           continue;
        }
        ft->name_index = TK_NAME_NEED_DISPLAY;
        // calc the char whether overloap with annotation
        for (i = 0; i < label_rect_num; i++) {
            j = i;
            if (TK_ABS(nLX - label_point[j].pt.x) < TK_GEO_LABEL_WIDTH 
                    && TK_ABS(nLY - label_point[j].pt.y) < TK_GEO_LABEL_HEIGHT){
                insert_flag ++;
                break;
            }
            label_rects[i].left = label_point[j].pt.x - (TK_GEO_LABEL_WIDTH / 2);
            label_rects[i].top = label_point[j].pt.y - TK_GEO_LABEL_WIDTH / 2;
            label_rects[i].right = label_point[j].pt.x + (TK_GEO_LABEL_WIDTH / 2);
            label_rects[i].bottom = label_point[j].pt.y + TK_GEO_LABEL_HEIGHT - TK_GEO_LABEL_WIDTH / 2;
            //label_rects[i].slope = label_point[j].slope;
            if (tk_geo_intersectrect(label_rects[i], rect) != 3) {
                insert_flag++;
                break;
            }

            nLX = label_point[j].pt.x;
            nLY = label_point[j].pt.y;

            if (((nLX < TK_GEO_LABEL_WIDTH / 2) || (nLX > rect.right))
                    || ((nLY < 1) || (nLY > rect.bottom - TK_GEO_LABEL_HEIGHT))){
                insert_flag ++;
                break;
            }
            //检查是否覆盖，覆盖则不添加
            if (geo_label_isoverleap(label_rects[i], tk_geo_labels,
                        tk_geo_labels_num, ft->label_priority) == 1) {
                if (1 == is_zoomed_to_max)
                    ft->label_priority ++;
                insert_flag ++; 
                break; 
            }
            //tk_geo_labels[tk_geo_labels_num].pfeature = ft;
        }
        if (insert_flag == 0) {
            insert_label_buffer(label_rects, label_point, pPts, label_rect_num, name, name_len, tk_gdi.font_size, -1, outline_color, 0 , 0);
            draw_flag ++;
        }
    }
    }
    return 1;
}

float get_font_slope(float road_slope) 
{
   /*   if (road_slope > 90.00) {
        if ((road_slope - 90.00) <= 45.00)
            return  road_slope - 90;
        else
            return 180 + road_slope ;
    } else {
        if (road_slope <= 45.00)
            return  road_slope;
        else
            return  360 - road_slope;
    }*/
    return   road_slope;
}


int insert_label_buffer(struct envelope *label_rects,struct tk_point_slope *label_point, struct tk_point *pPts,int label_rect_num, \
                                                              char *name, int name_len, int font_size, int icon_id,int outline_color, int icon_x, int icon_y)
{
    int i;
    int label_idx;
    int glabel_idx;
    int sl_index;
    /* store labels for upper application */
    for (i = 0; i < label_rect_num; i++) {
        if (name_len < 16) {
            glabel_idx = get_empty_glabel_idx();
            label_buffer[glabel_idx].x = label_rects[i].left;
            label_buffer[glabel_idx].y = label_rects[i].top;
            label_buffer[glabel_idx].icon_x = icon_x;
            label_buffer[glabel_idx].icon_y = icon_y;
            label_buffer[glabel_idx].icon_id = icon_id;
            //label_buffer[glabel_idx].angle = 0;
            label_buffer[glabel_idx].font_color = tk_gdi.txtcol;
            label_buffer[glabel_idx].font_size = TK_GEO_LABEL_WIDTH;
            if (label_rect_num == 1) {
                //label_buffer[glabel_idx].font_color = tk_gdi.txtcol;
                label_buffer[glabel_idx].outline_color = outline_color;
                label_buffer[glabel_idx].slope = 0;
                label_buffer[glabel_idx].name = add_label_name(name, name_len);
            }
            else {
                //label_buffer[glabel_idx].font_color = tk_gdi.bgcolor;
                label_buffer[glabel_idx].outline_color = outline_color;
                label_buffer[glabel_idx].slope = acosf(label_point[i].slope) *180/3.14;
                label_buffer[glabel_idx].name = add_label_name(name + i * 2, 2);
            }
        }
        else {
            sl_index = (((name_len >> 1) + 1) >> 1) << 1 ;
            glabel_idx = get_empty_glabel_idx();
            label_buffer[glabel_idx].x = label_rects[i].left;
            label_buffer[glabel_idx].y = label_rects[i].top;
            //label_buffer[glabel_idx].angle = 0;
            label_buffer[glabel_idx].font_color = tk_gdi.txtcol;
            label_buffer[glabel_idx].font_size = TK_GEO_LABEL_WIDTH;
            label_buffer[glabel_idx].icon_x = icon_x;
            label_buffer[glabel_idx].icon_y = icon_y;
            label_buffer[glabel_idx].icon_id = icon_id;
            if (label_rect_num == 1) {
                //label_buffer[glabel_idx].font_color = tk_gdi.txtcol;
                label_buffer[glabel_idx].outline_color = outline_color;
                label_buffer[glabel_idx].slope = 0;
                label_buffer[glabel_idx].name = add_label_name(name, sl_index);
            }
            else {
                //label_buffer[glabel_idx].font_color = tk_gdi.bgcolor;
                label_buffer[glabel_idx].outline_color = outline_color;
                label_buffer[glabel_idx].slope = acosf(label_point[i].slope) *180/3.14;
                label_buffer[glabel_idx].name = add_label_name(name + i * 2, 2);
            }
            if (label_rect_num == 1) {
                glabel_idx = get_empty_glabel_idx();
                label_buffer[glabel_idx].x = label_rects[i].left;
                label_buffer[glabel_idx].y = label_rects[i].top + TK_GEO_LABEL_HEIGHT;
                label_buffer[glabel_idx].icon_x = icon_x;
                label_buffer[glabel_idx].icon_y = icon_y;
                label_buffer[glabel_idx].icon_id = -1;
                label_buffer[glabel_idx].font_color = tk_gdi.txtcol;
                label_buffer[glabel_idx].outline_color = outline_color;
                //label_buffer[glabel_idx].outline_color = tk_gdi.color;
                label_buffer[glabel_idx].font_size = TK_GEO_LABEL_WIDTH;
                label_buffer[glabel_idx].slope = 0;
                label_buffer[glabel_idx].name = add_label_name(name + sl_index ,name_len - sl_index);
            }
        }
    } 
    label_idx = get_empty_label_idx();
    tk_geo_labels[label_idx].pRects = malloc(sizeof(struct envelope)
            * label_rect_num);
    if (tk_geo_labels[label_idx].pRects == NULL) {
        LOG_ERR("prec error!!\n");
        return 0;
    }

    memcpy(tk_geo_labels[label_idx].pRects, label_rects,
            sizeof(struct envelope)*label_rect_num);
    tk_geo_labels[label_idx].name_len = name_len;
    tk_geo_labels[label_idx].rect_num = label_rect_num;
    tk_geo_labels[label_idx].name_idx = -1;
    tk_geo_labels[label_idx].name = name;
    tk_geo_labels[label_idx].need_draw = 1;
    tk_geo_labels[label_idx].pfeature = NULL;//ft;
    tk_geo_labels[label_idx].icon_id = icon_id;
    tk_geo_labels[label_idx].p_poi = *pPts;
    tk_geo_labels[label_idx].txtcol = tk_gdi.txtcol;
    //LOG_DBG("name = %s , feature type = %d\n", name, ft->type);
    
    return 1;
}

int draw_label()
{
    struct tk_label_struct *plabel;
    int i, j;
    char label_buff[128];//buffer of label's name
    char name[4];
    struct envelope label;//?
    int label_num = tk_geo_labels_num; 
    struct tk_label_struct *plabels = tk_geo_labels; 
    int icon_id;

    label.left = label.top = 0;
    label.right = TILE_SIZE;//tk_engine.width;
    label.bottom = TILE_SIZE;//tk_engine.height;

    for (i = 0; i < label_num; i++) {
        plabel = plabels + i;
        if (plabel == NULL)
            continue;
        if (plabel->need_draw != 1)
            continue;
        //plabel->pfeature->label_priority = 0;
        memcpy(label_buff, plabel->name, plabel->name_len);
        label_buff[plabel->name_len] = 0;
        if (plabel->rect_num == 1 
                   && tk_geo_intersectrect(plabel->pRects[0], label) != 0 ) {
            if (TK_IMG_TIGER_MAP_VECTOR_MAX > TK_IMG_TIGER_MAP_VECTOR_DEFAULT + plabel->icon_id)
                if (plabel->icon_id != 35)
                    tk_draw_img(plabel->p_poi.x, plabel->p_poi.y,TK_ALIGN_CENTER, \
                        TK_ALIGN_CENTER, TK_IMG_TIGER_MAP_VECTOR_DEFAULT+plabel->icon_id);
                else
                    tk_draw_district(plabel->name_len / 2 * TK_GEO_LABEL_WIDTH, TK_GEO_LABEL_WIDTH, plabel->p_poi.x, plabel->p_poi.y, TK_COLOR_DISTRICT);

        if ((unsigned char )plabel->name[0] == 163 && (unsigned char )plabel->name[1] == 199 )  {
            icon_id = TK_IMG_TIGER_MAP_VECTOR_STATE_ROAD;
            tk_draw_img(plabel->p_poi.x, plabel->p_poi.y,TK_ALIGN_CENTER, \
                        TK_ALIGN_CENTER, icon_id);
        }
        if ((unsigned char)plabel->name[0] == 163 && (unsigned char)plabel->name[1] == 211) {
            icon_id = TK_IMG_TIGER_MAP_VECTOR_OTHER_PROVINCIAL_ROAD;
            tk_draw_img(plabel->p_poi.x, plabel->p_poi.y,TK_ALIGN_CENTER, \
                        TK_ALIGN_CENTER, icon_id);
            }
#ifdef DRAW_STRING
            tk_gdi_draw_string(plabel->pRects[0].left, plabel->pRects[0].top,
                    label_buff, plabel->txtcol);
#endif
            continue;
        }

#ifdef DRAW_STRING
        for (j = 0; j < plabel->rect_num; j++) {
            name[0] = label_buff[j * 2];
            name[1] = label_buff[j * 2 + 1];
            name[2] = name[3] = '\0';
            if (tk_geo_intersectrect(plabel->pRects[j], label) != 0)
                tk_gdi_draw_string(plabel->pRects[j].left,
                        plabel->pRects[j].top, name, plabel->txtcol);
        }
#endif
    }
    return 1;
}

/*  ==============================
 *  */

//extern int layindex;
extern int current_rid;
int find_index_swcity(int rid)
{
        if(rid == -1)
            return -1;
        int i = 0;
        for(i = 0; i < sw_city.city_num; i++){
               if(sw_city.city_info[i].rid == rid){
                      return  i;
               }
        }
                return -1;
}
void get_map_polygon_name(struct layer *pl, int vlevel) {
    int label_level = 16;
    struct feature *pfeature = NULL;
    int p_num = 0;
    int count = 0;
    int isrval = 0;
    struct tk_point *ppts;// = pfeature->points;
    int ptnum = 0;// = pfeature->point_nums;
    int transferred_coord_num = 0;
    int i = 0;

    GDI_SETLABELSTYLE(pstyle[0].label_style);
    GDI_SETFONTSIZE(pstyle[0].font_size);
    GDI_SETTEXTCOLOR(pstyle[0].fontcolor);

    if (vlevel < pstyle[0].zoom_min || vlevel > pstyle[0].zoom_max)
        return;
    if (pl == NULL || pl->features == NULL)
        return ;
    if (pstyle == NULL)
        return ;
    pfeature = pl->features;
    while (pfeature) {
        if (pfeature->name_index >= 0) {
            //如果名字需要显示,在tk_geo_add_label确认
            pfeature->name_index = TK_NAME_DO_NOT_NEED_DISPLAY;//TODO:?
            isrval = judge_feature_env_relation(pfeature, TK_WHOLE_SCREEN_ENV);
            if (isrval == 0)//无交叉
                return ;

            //tk_gdi.draw_point_buffer中存入屏幕坐标,ptnum更新为tk_gdi.draw_point_buffer点数目,以后的裁减基于tk_gdi.draw_point_buffer
            get_point_from_feature(pfeature, &ptnum);
            ppts = tk_gdi.draw_point_buffer;
            transferred_coord_num = ptnum;
            /*if (isrval != 3) {//不是被包含，即交叉, 因此需裁剪
                if (clipgeopoints(tk_gdi.draw_point_buffer, transferred_coord_num,
                        vm_screen, 2, &ppts, &ptnum) == 0)
                    return ;
            }*/
            if (ptnum >= 2 && (vlevel >= pstyle->label_min && vlevel <= pstyle->label_max)) {
                //在其中会更改pfeature->name_index以便确认以后是否可能依然需要显示
                tk_geo_add_label(pfeature, ppts, ptnum, vm_screen, 0, UNVALID_IMG_ID, TK_COLOR_WHITE, pstyle->font_alter);
            }
            //ppts = tk_gdi.draw_point_buffer;
        }
        pfeature = pfeature->layer_next;
    }
}


void get_map_road_name(struct layer *pl, int vlevel) {
    int label_level = 16;
    struct feature *pfeature = NULL;
    int isrval = 0;
    int ptnum;
    struct tk_point *ppts = NULL;
    int transferred_coord_num;
    int outline_color;
    int color_index;
    if (pl == NULL || pl->features == NULL)
        return;
    if (vlevel < pstyle[0].zoom_min || vlevel > pstyle[0].zoom_max)
        return;
    if (pstyle[0].pen_width[vlevel - pstyle[0].zoom_min] == 0)
        return;
    //printf("insert into road name @@@@@@@@@@@@@@@@@@@@@\n");
    
    GDI_SETCOLOR(pstyle[0].fill_color);
    GDI_SETBGCOLOR(pstyle[0].border_color);
    GDI_SETLABELSTYLE(pstyle[0].label_style);
    GDI_SETTEXTCOLOR(pstyle[0].fontcolor);
    //GDI_SETTEXTCOLOR(pstyle[0].border_color);
    GDI_SETFONTSIZE(pstyle[0].font_size);
    GDI_SET_SHOULD_PIXEL_OVERLAP(0);
    outline_color = tk_gdi.color;

    pfeature =  pl->features;
    while(pfeature) {
        if(pfeature->type == 9)
        {
            color_index = find_index_swcity(current_rid);
            if(color_index != -1)
            {
              int i = 0;
               for(;i < 16; i++){
                   if(strlen(sw_city.city_info[color_index].color_info[i].sw_name) == pfeature->name_length){
                        if(strncmp(sw_city.city_info[color_index].color_info[i].sw_name,pfeature->name,pfeature->name_length) == 0){
                            tk_gdi.color = sw_city.city_info[color_index].color_info[i].color;
                            tk_gdi.txtcol = tk_gdi.color;
                            outline_color = TK_COLOR_WHITE;
                            break;
                        }else{
                            tk_gdi.color = 0x4ABAA5;
                            outline_color = TK_COLOR_WHITE;
                        }
                    }
                }
            } else {
                tk_gdi.color =0x4ABAA5;
                outline_color = TK_COLOR_WHITE;
            }
        }
        if (pfeature->name_index >= 0 && pfeature->name_index != TK_NAME_TOO_SHORT_TO_DISPLAY) {
            //如果名字需要显示,在tk_geo_add_label确认
            isrval = judge_feature_env_relation(pfeature, TK_WHOLE_SCREEN_ENV);
            //printf("isrval %d\n", isrval);
            if (isrval == 0)
                goto next;
            //tk_gdi.draw_point_buffer中存入屏幕坐标,ptnum更新为tk_gdi.draw_point_buffer点数目,以后的裁减基于tk_gdi.draw_point_buffer
            get_point_from_feature(pfeature, &ptnum);
            ppts = tk_gdi.draw_point_buffer;
            transferred_coord_num = ptnum;
            if (isrval != 3) {
                if (clipgeopoints(tk_gdi.draw_point_buffer, transferred_coord_num,
                            vm_screen, 1, &ppts, &ptnum) == 0)
                    goto next;
            }
            if (ptnum >= 2 && (vlevel >= pstyle->label_min && vlevel <= pstyle->label_max)) {
                //在其中会更改pfeature->name_index以便确认以后是否可能依然需要显示
                //printf("############ add road name\n");
                tk_geo_add_label(pfeature, ppts, ptnum, vm_screen, 1, UNVALID_IMG_ID, outline_color, pstyle->font_alter);
            }
        }
        next:   pfeature = pfeature->layer_next;
    }
}


// 1. 绘制线型地物
void draw_line_feature(struct feature *pfeature, int vlevel, int label_level) 
{
    int ptnum, count = 0;
    struct tk_point *ppts;
    int isrval, resc;
    int transferred_coord_num;
    double remain;
    int color_index = 0;
    int outline_color = 0;
    int ft_type = pfeature->type;
    cairo_set_line_join(cr, CAIRO_LINE_JOIN_ROUND);
    
    while(pfeature) {
        count++;
        ptnum = 0;
        ppts = NULL;
        isrval = 0;
        resc = 0;
        transferred_coord_num = 0;
        remain = 0;
        outline_color = tk_gdi.color;
        if(pfeature->type == 9)
        {
            color_index = find_index_swcity(current_rid);
            if(color_index != -1)
            {
              int i = 0;
               for(;i < 16; i++){
                   if(strlen(sw_city.city_info[color_index].color_info[i].sw_name) == pfeature->name_length){
                        if(strncmp(sw_city.city_info[color_index].color_info[i].sw_name,pfeature->name,pfeature->name_length) == 0){
                            tk_gdi.color = sw_city.city_info[color_index].color_info[i].color;
                            tk_gdi.txtcol = tk_gdi.color;
                            outline_color = TK_COLOR_WHITE;
                            break;
                        }else{
                            tk_gdi.color = 0x4ABAA5;
                            outline_color = TK_COLOR_WHITE;
                        }
                    } else {
                       tk_gdi.color = 0x4ABAA5;
                       outline_color = TK_COLOR_WHITE;
                    }
                }
            } else {
                tk_gdi.color =0x4ABAA5;
                outline_color = TK_COLOR_WHITE;
            }
        }
        if (pfeature->name_index >= 0 && pfeature->name_index != TK_NAME_TOO_SHORT_TO_DISPLAY) {
            //如果名字需要显示,在tk_geo_add_label确认
            isrval = judge_feature_env_relation(pfeature, TK_WHOLE_SCREEN_ENV);
            isrval = 3;
            if (isrval == 0)
                goto next;
            //tk_gdi.draw_point_buffer中存入屏幕坐标,ptnum更新为tk_gdi.draw_point_buffer点数目,以后的裁减基于tk_gdi.draw_point_buffer
            get_point_from_feature(pfeature, &ptnum);
            ppts = tk_gdi.draw_point_buffer;
            transferred_coord_num = ptnum;        
            if (isrval != 3) {
                if (clipgeopoints(tk_gdi.draw_point_buffer, transferred_coord_num,
                            vm_screen, 1, &ppts, &ptnum) == 0)
                    goto next;
            }
            if (ptnum >= 2 && (vlevel >= pstyle->label_min && vlevel <= pstyle->label_max)) {
                //在其中会更改pfeature->name_index以便确认以后是否可能依然需要显示
                ;//tk_geo_add_label(pfeature, ppts, ptnum, vm_screen, 1, UNVALID_IMG_ID, outline_color);
            }
        }
        if (pfeature->tile->is_active != 1)
            goto next;
        isrval = judge_feature_env_relation(pfeature, TK_CLIP_SCREEN_ENV);
            isrval = 3;
        if (isrval == 0)
            goto next;
        if (pfeature->name_index < 0 || pfeature->name_index == TK_NAME_TOO_SHORT_TO_DISPLAY) {
            get_point_from_feature(pfeature, &ptnum);
            ppts = tk_gdi.draw_point_buffer;
            transferred_coord_num = ptnum;
        }
        if (isrval != 3) {
           if ((resc = clipgeopoints(tk_gdi.draw_point_buffer, transferred_coord_num,
                           tk_gdi.rtview, 1, &ppts, &ptnum)) == 0)
                goto next;
        }
        //if (pfeature->type >= 3 && pfeature->type <= 9) 
          //      printf("road type %d points_num %d\n",pfeature->type, ptnum);
        if (ptnum >= 2) {
            if (resc != 0) {// 剪裁之后 ，因为isrval !=3
                int begin = 0, i;
                struct tk_point* pts;
                int in = 1;
                int the_counter = 0;
                pts = ppts;
                for (i = 1; i<ptnum; i++) {
                    if (ppts[i].levelCode > 0) {
                        if (in) {
                            if (GDI_GETLINESTYLE() == TKGDI_ENLSTYLE_DASH) {
                                remain = get_all_distance(tk_gdi.draw_point_buffer, pts, the_counter);
                                the_counter++;
                            }
                            draw_misc_line(pts, i-begin+1, &remain,ft_type);
                            in = 0;
                        } else {
                            pts = ppts + i;
                            begin = i;
                            in = 1;
                            the_counter++;
                        }
                    }
                }
                if (in)
                    draw_misc_line(pts, ptnum-begin, &remain,ft_type);
            } else //isrval == 3 
                draw_misc_line(ppts, ptnum, &remain,ft_type);
        }
next:       pfeature = pfeature->layer_next;
              
    }
             cairo_set_line_width(cr,tk_gdi.nPSize );
             cairo_set_line_cap(cr, CAIRO_LINE_CAP_ROUND);
             //cairo_set_line_cap(cr, CAIRO_LINE_CAP_BUTT);
             cairo_set_source_rgb(cr,(double)((tk_gdi.bgcolor >> 16)&0xff)/(double)256,\
                                 (double)((tk_gdi.bgcolor >> 8)&0xff)/(double)256,\
                                 (double)((tk_gdi.bgcolor )&0xff)/(double)256);
             cairo_stroke_preserve(cr); 
                 
             if(is_draw_subway && ft_type != 8 && ft_type != 9){
                //cairo_fill_preserve (cr);
                if(tk_gdi.nPSize >= 4)
                    cairo_set_line_width(cr,tk_gdi.nPSize - 3);
                else
                    cairo_set_line_width(cr,tk_gdi.nPSize - 2);

                cairo_set_line_cap(cr, CAIRO_LINE_CAP_ROUND);
                cairo_set_source_rgb(cr,(double)((TK_GDI_CI_COLOR >> 16)&0xff)/(double)256,\
                                 (double)((TK_GDI_CI_COLOR >> 8)&0xff)/(double)256,\
                                 (double)((TK_GDI_CI_COLOR )&0xff)/(double)256);
                cairo_stroke(cr);
            }

}
 
// 2. 绘制地图的线状地物图层
void draw_linelayer(struct layer *pl, int vlevel) 
{

    int label_level = 16;
    struct feature *pfeature = NULL;
    if (pl == NULL || pl->features == NULL)
        return;
    if (vlevel < pstyle[0].zoom_min || vlevel > pstyle[0].zoom_max)
        return;
    if (pstyle[0].pen_width[vlevel - pstyle[0].zoom_min] == 0)
        return;

    GDI_SETCOLOR(pstyle[0].fill_color);
    GDI_SETBGCOLOR(pstyle[0].border_color);
    GDI_SETPENSIZE(pstyle[0].pen_width[vlevel - pstyle[0].zoom_min]);
    GDI_SETLINESTYLE(pstyle[0].line_type);
    GDI_SETLABELSTYLE(pstyle[0].label_style);
    GDI_SETTEXTCOLOR(pstyle[0].fontcolor);
    GDI_SETFONTSIZE(pstyle[0].font_size);
    GDI_SET_SHOULD_PIXEL_OVERLAP(0);
    

    pfeature =  pl->features;
    draw_line_feature(pfeature, vlevel, label_level);
    return;
}
// 3. 绘制地图的线状地物图层
void draw_road(struct layer *pl, int vlevel)
{
    int label_level = 16;
    struct feature *pfeature = NULL;
    if (pl == NULL || pl->features == NULL)
        return;
    if (vlevel < pstyle[0].zoom_min || vlevel > pstyle[0].zoom_max)
        return;
    if (pstyle[0].pen_width[vlevel - pstyle[0].zoom_min] == 0)
        return;
    
    GDI_SETCOLOR(pstyle[0].fill_color);
    GDI_SETBGCOLOR(pstyle[0].border_color);
    GDI_SETPENSIZE(pstyle[0].pen_width[vlevel - pstyle[0].zoom_min]);
    GDI_SETLINESTYLE(pstyle[0].line_type);
    GDI_SETLABELSTYLE(pstyle[0].label_style);
    GDI_SETTEXTCOLOR(pstyle[0].fontcolor);
    GDI_SETFONTSIZE(pstyle[0].font_size);
    GDI_SET_SHOULD_PIXEL_OVERLAP(0);
   
    pfeature =  pl->features;
    draw_line_feature(pfeature, vlevel, label_level);
    return;
}

// 4. 绘制地图的线状地物图层
void draw_rail(struct layer *pl, int vlevel)
{
    int label_level = 16;
    struct feature *pfeature = NULL;
    if (pl == NULL || pl->features == NULL)
        return;
    if(vlevel < pstyle[0].zoom_min || vlevel > pstyle[0].zoom_max)
        return;
    
    GDI_SETCOLOR(pstyle[0].fill_color);
    GDI_SETBGCOLOR(pstyle[0].border_color);
    GDI_SETPENSIZE(pstyle[0].pen_width[vlevel - pstyle[0].zoom_min]);
    GDI_SETLINESTYLE(pstyle[0].line_type);
    GDI_SETLABELSTYLE(pstyle[0].label_style);
    GDI_SETTEXTCOLOR(pstyle[0].fontcolor);
    GDI_SETFONTSIZE(pstyle[0].font_size);
    GDI_SET_SHOULD_PIXEL_OVERLAP(1);
   
    pfeature =  pl->features;
    draw_line_feature(pfeature, vlevel, label_level);
    return;
}

/* 绘制面状地物
 * pfeature: the pointer to the feature.
 * tk_gdi.rtview: the part of screen which need redraw.
 * vlevel: the map's current zoom_level.
 * label_level: default is 16
 * return 0 if failed
 * return 1 if success
 */
static int drawpolygonfeature(struct feature *pfeature, int vlevel, int label_level)
{
    int isrval = 0;
    struct tk_point *ppts;// = pfeature->points;
    int ptnum = 0;// = pfeature->point_nums;
    int transferred_coord_num = 0;
    int i = 0;

    if (pfeature->name_index >= 0) {
        //如果名字需要显示,在tk_geo_add_label确认
        pfeature->name_index = TK_NAME_DO_NOT_NEED_DISPLAY;//TODO:?
        isrval = judge_feature_env_relation(pfeature, TK_WHOLE_SCREEN_ENV);
            isrval = 3;
        if (isrval == 0)//无交叉
            return 0;

        //tk_gdi.draw_point_buffer中存入屏幕坐标,ptnum更新为tk_gdi.draw_point_buffer点数目,以后的裁减基于tk_gdi.draw_point_buffer
        get_point_from_feature(pfeature, &ptnum);
        ppts = tk_gdi.draw_point_buffer;
        transferred_coord_num = ptnum;
        if (isrval != 3) {//不是被包含，即交叉, 因此需裁剪
            if (clipgeopoints(tk_gdi.draw_point_buffer, transferred_coord_num,
                        vm_screen, 2, &ppts, &ptnum) == 0)
                return 0;
        }
        /*if (ptnum >= 2 && (vlevel >= pstyle->label_min && vlevel <= pstyle->label_max)) {
            //在其中会更改pfeature->name_index以便确认以后是否可能依然需要显示
            tk_geo_add_label(pfeature, ppts, ptnum, vm_screen, 0, UNVALID_IMG_ID, TK_COLOR_WHITE);
        }*/
        ppts = tk_gdi.draw_point_buffer;
    }
    isrval = judge_feature_env_relation(pfeature, TK_CLIP_SCREEN_ENV);
            isrval = 3;
    if (isrval == 0)
        return 0;

    //tk_gdi.draw_point_buffer中存入屏幕坐标,ptnum更新为tk_gdi.draw_point_buffer点数目,以后的裁减基于tk_gdi.draw_point_buffer
    get_point_from_feature(pfeature, &ptnum);
    ppts = tk_gdi.draw_point_buffer;
    transferred_coord_num = ptnum;

    if (isrval != 3) {
        if (clipgeopoints(tk_gdi.draw_point_buffer, transferred_coord_num,
                    tk_gdi.rtview, 2, &ppts, &ptnum) == 0) {
            return 0;
        }
    }
            
    if(is_draw_subway && ptnum >= 2){
        //cairo_set_line_width(cr,1);
        //cairo_set_source_rgb(cr,(double)((TK_GDI_CI_COLOR >> 16)&0xff)/(double)256,\
            (double)((TK_GDI_CI_COLOR >> 8)&0xff)/(double)256,\
            (double)((TK_GDI_CI_COLOR )&0xff)/(double)256);
        //if (ppts[0].y != ppts[ptnum - 1].y)
        //    return 1;

        cairo_move_to(cr, ppts[0].x, ppts[0].y);
        for( i = 1; i < ptnum; i++) {
            //LOG_DBG("move to %d, %d\n", ppts[0].x, ppts[0].y);
            cairo_line_to(cr, ppts[i].x, ppts[i].y);
        }
        //cairo_close_path(cr);
        //cairo_fill(cr);
        //cairo_stroke(cr);
        return 1;
    }
    if (ptnum >= 2) {
        add_polygon(ptnum, ppts);
    }
    return 1;
}

int tk_geo_drawvmlpoly(struct layer *pl, int vlevel) 
{
    int label_level = 16;
    struct feature *pfeature = NULL;
    int p_num = 0;
    int count = 0;
    
    if (pl == NULL || pl->features == NULL)
        return 0;
    if (pstyle == NULL)
        return 0;

    GDI_SETCOLOR(pstyle[0].fill_color);
    GDI_SETBGCOLOR(pstyle[0].border_color);
    GDI_SETPENSIZE(pstyle[0].pen_width[0]);
    GDI_SETLABELSTYLE(pstyle[0].label_style);
    GDI_SETFONTSIZE(pstyle[0].font_size);
    GDI_SETTEXTCOLOR(pstyle[0].fontcolor);
    //begin_area_fill();
    cairo_set_line_width(cr,1);
    cairo_set_source_rgb(cr,(double)((TK_GDI_CI_COLOR >> 16)&0xff)/(double)256,\
            (double)((TK_GDI_CI_COLOR >> 8)&0xff)/(double)256,\
            (double)((TK_GDI_CI_COLOR )&0xff)/(double)256);
    pfeature = pl->features;
    while (pfeature) {
        drawpolygonfeature(pfeature, vlevel, label_level);
        pfeature = pfeature->layer_next;
    }
    if(is_draw_subway) {
        cairo_close_path(cr);
        cairo_set_fill_rule(cr, CAIRO_FILL_RULE_EVEN_ODD);
        cairo_fill(cr);
        //cairo_stroke(cr);
        //LOG_DBG("now cairo_stroke a polygonfeature\n");
        return 1;
    }
    //draw_area();
    //end_area_fill();
    return 1;
}


// 5. 绘制点状地物(poi)
void draw_point_feature(struct feature *pfeature, int icon_id, int vlevel, int label_level, int font_alter)
{
    if(vlevel < pstyle->label_min || vlevel > pstyle->label_max)
        return;
    int isrval;
    struct tk_point point;
    short  tile_bias_x, tile_bias_y;
    short lvl_dif = pfeature->tile->need_move_bits;
    int img_flag = 0;

    if (lvl_dif >= 0) {
        tile_bias_x = (short)(((pfeature->tile->coder_lon) << (8-lvl_dif))
                    -(tk_engine.min_tile_bbox.left>>(16 - vlevel)));
        tile_bias_y = (short)(((pfeature->tile->coder_lat) << (8-lvl_dif))
                -(tk_engine.min_tile_bbox.bottom >> (16 - vlevel)));
        point.x = (pfeature->points->x >> lvl_dif) + tile_bias_x;
        point.y = (pfeature->points->y >> lvl_dif) + tile_bias_y;
    } else {
        tile_bias_x = (short)(((pfeature->tile->coder_lon) << 8 << (-lvl_dif))
                -(tk_engine.min_tile_bbox.left));//<<(-(16 - vlevel))));
        tile_bias_y = (short)(((pfeature->tile->coder_lat) << 8 << (-lvl_dif))
                -(tk_engine.min_tile_bbox.bottom));// << (-(16 - vlevel))));
        point.x = (pfeature->points->x << (-lvl_dif)) + tile_bias_x;
        point.y = (pfeature->points->y << (-lvl_dif)) + tile_bias_y;
    }
    isrval = pointinrect(point.x, point.y, vm_screen.left, vm_screen.top,
            vm_screen.right, vm_screen.bottom);
    //printf("vm %d---%d---%d---%d\n", vm_screen.left, vm_screen.right, vm_screen.top, vm_screen.bottom);
    if (pfeature->type == 38) {
        pfeature->label_priority = 10;
        //if (tk_engine.current_z <= 12 && (unsigned char)pfeature->name[pfeature->name_length - 1] != 248) 
          //  return;
        //if (tk_engine.current_z > 12 && (unsigned char)pfeature->name[pfeature->name_length - 1] == 248)
          //  return;
    }
    
    img_flag = tk_geo_add_label(pfeature, &point, 1, vm_screen, 0, icon_id, TK_COLOR_WHITE, font_alter);
    isrval = pointinrect(point.x, point.y,
            tk_gdi.rtview.left, tk_gdi.rtview.top, tk_gdi.rtview.right, tk_gdi.rtview.bottom);
}

// 6. 绘制地图的点地物图层
void draw_point(struct layer *pl, int vlevel)
{
    int label_level = 15;
    struct feature *pfeature = NULL;
    if (pl == NULL || pl->features == NULL)
        return;
    if (vlevel < pstyle->zoom_min || vlevel > pstyle->zoom_max)
        return;
    //printf("**************************\n"); 
    GDI_SETCOLOR(pstyle->fill_color);
    GDI_SETBGCOLOR(pstyle->border_color);
    GDI_SETPENSIZE(pstyle->pen_width[0]);
    GDI_SETLABELSTYLE(pstyle->label_style);
    GDI_SETFONTSIZE(pstyle[0].font_size);
    GDI_SETTEXTCOLOR(pstyle->fontcolor);

    pfeature = pl->features;
    while(pfeature) {
        draw_point_feature(pfeature, pstyle->icon_id, vlevel, label_level, pstyle->font_alter);
        pfeature = pfeature->layer_next;
    }
    return;
}

void release_points()
{
    free(tk_clip_mem_a);
    free(tk_clip_mem_b);
    free(tk_clip_index);
    free(tk_gdi.draw_point_buffer);
}

void refresh_local_feature(int x, int y, int flag)
{
    int distance = 0;
    int ptnum = 0;
    struct tk_point *ppts = NULL;
    struct layer *pl = NULL;
    int i;
    struct feature *pfeature = NULL;
    int isrval;
    struct tk_point point;
    short int  tile_bias_x, tile_bias_y;

    best_feature.screen_center_coord.x = x;
	best_feature.screen_center_coord.y = y;
	best_feature.screen_center.x = tk_engine.center_x -
		(tk_engine.width >> 1 << (16 - tk_engine.current_z + 2) >> 2) + (x << (16 - tk_engine.current_z + 2) >> 2);
	best_feature.screen_center.y = tk_engine.center_y -
		(tk_engine.height >> 1 << (16 - tk_engine.current_z + 2) >> 2 ) + (y << (16 - tk_engine.current_z + 2) >> 2);
    best_feature.distance_to_center = 512;//set a max value as initial;
    best_feature.name[0] = '\0'; 
    
    for (i = 0; i < tk_engine.ps->layer_num; i++) {
        if (i == 13 || i == 40 || i == 31)
                  continue;
        pl = &tk_engine.layer_list[i];
        if (pl == NULL)
            continue;
        if ((flag == 1) && (tk_engine.ps->feature_type[i] == TKGEO_ENMFTYPE_POINT)) {
            pfeature = pl->features;
            while(pfeature) {
                short int lvl_dif = pfeature->tile->need_move_bits;
                int vlevel = tk_engine.current_z;
                if (lvl_dif >= 0) {
                    tile_bias_x
                        =  (short int)(((pfeature->tile->coder_lon) << (8-lvl_dif))
                                -(tk_engine.min_tile_bbox.left >> (16 - vlevel)));
                    tile_bias_y = (short int)(((pfeature->tile->coder_lat) << (8-lvl_dif))
                            -(tk_engine.min_tile_bbox.bottom >> (16 - vlevel)));
                    point.x = (pfeature->points->x >> lvl_dif) + tile_bias_x;
                    point.y = (pfeature->points->y >> lvl_dif) + tile_bias_y;
                } else {
                    tile_bias_x = (short int)(((pfeature->tile->coder_lon) << 8 << (-lvl_dif))
                            -(tk_engine.min_tile_bbox.left));// <<(-(16 - vlevel))));
                    tile_bias_y = (short int)(((pfeature->tile->coder_lat) << 8 << (-lvl_dif))
                            -(tk_engine.min_tile_bbox.bottom));// << (-(16 - vlevel))));
                    point.x = (pfeature->points->x << (-lvl_dif)) + tile_bias_x;
                    point.y = (pfeature->points->y << (-lvl_dif)) + tile_bias_y;
                }
                isrval = pointinrect(point.x,
                        point.y, vm_screen.left, vm_screen.top,
                        vm_screen.right, vm_screen.bottom);
                if (isrval == 0) {
                    pfeature = pfeature->layer_next;
                    continue;
                }
                distance = point_distance(&point, &best_feature.screen_center_coord);
                if (pfeature->name_length > 0) {
                    if (best_feature.distance_to_center > distance) {
                        //printf("dist:%d\n"min_tileistance);
                        best_feature.type_code = pfeature->type;
                        memcpy(best_feature.name, pfeature->name, pfeature->name_length);
                        best_feature.name[pfeature->name_length] = '\0';
                        best_feature.distance_to_center = distance;
                    }
                }
                pfeature = pfeature->layer_next;
            }
        }
        else if ((flag == 0) && (tk_engine.ps->feature_type[i] == TKGEO_ENMFTYPE_RAIL ||
                tk_engine.ps->feature_type[i] == TKGEO_ENMFTYPE_ROAD)) {
            pfeature = pl->features;
            while(pfeature) {
                get_point_from_feature(pfeature, &ptnum);
                ppts = tk_gdi.draw_point_buffer;
                if (pfeature->name_index >= 0) {
                    distance = point_toLine_distance(ppts, ptnum, &best_feature.screen_center_coord);
                        if (best_feature.distance_to_center > distance) {
                            //printf("dist:%d\n", distance);
                            best_feature.type_code = pfeature->type;
                            memcpy(best_feature.name, pfeature->name, pfeature->name_length);
                            best_feature.name[pfeature->name_length] = '\0';
                            best_feature.distance_to_center = distance;
                        }
                } 
                pfeature = pfeature->layer_next;      
            }
        }
    }
}


/*
 * =====================================================================================
 *
 *       Filename:  tkm_render.h
 *
 *    Description:  
 *
 *        Version:  1.0
 *        Created:  2010年12月28日 17时53分56秒
 *       Revision:  none
 *       Compiler:  gcc
 *
 *         Author:  YOUR NAME (), 
 *        Company:  
 *
 * =====================================================================================
 */
#ifndef __TKM_RENDER_H
#define __TKM_RENDER_H

#include "tkm_mapint.h"

#define TK_WHOLE_SCREEN_ENV 0
#define TK_CLIP_SCREEN_ENV 1

#define WHITE_DASH_LEN 10
#define COLOR_DASH_LEN 10
#define TK_GDI_YMIN -4095
#define TK_GDI_YMAX  4095

#define TK_GEO_LABEL_WIDTH	 ((unsigned int)floor(tk_gdi.font_size *(float)(tk_font_offset))) //(tk_gdi.font_size + tk_font_offset)
#define TK_GEO_LABEL_HEIGHT	 ((unsigned int)floor(tk_gdi.font_size *(float)(tk_font_offset))) //(tk_gdi.font_size + tk_font_offset)
#define TK_GEO_ICON_HEIGHT 12
#define TK_GDI_90DEG  1024
#define TK_GDI_180DEG 2048
#define TK_GDI_360DEG 4096

#define TK_GDI_FPMAXCOUNT 12

#define TK_NEED_SPECIAL_EFF 0
#define TK_DONOT_NEED_SPECIAL 1
#define MAX_POINT_CLIP 256
#define UNVALID_IMG_ID 100

//定义gdi相关信息
struct gdi {
	unsigned char		nTStyle;	//textstyle---TKGdiENTStyle
	unsigned char		nPSize;		//pensize
	unsigned char		nPShape;	//penshape----TKGdiENPShape
	unsigned char		nLStyle;	//line style--TKGdiENLStyle
    gdi_color           txtcol;     //text color
	gdi_color		    bgcolor;   //background color
    gdi_color           color;   
    int                 font_size;
	unsigned char		nSPixel;	//
	tk_pixel            *pd;		//
	unsigned char       nLabelStyle; 
    int             pixel_should_overlap; 
    int             is_road;
    struct envelope rtview;
    struct envelope unuse_view;
    // buffer for changing cooridinates
    struct tk_point *draw_point_buffer;
    // the length of the buffer above
    int draw_point_buffer_len;
};

extern int is_zoomed_to_max;
extern struct envelope vm_screen;
extern struct gdi tk_gdi;

#ifdef TK_BPP_16
#define TK_COLOR2INDEX(Color)   tk_gdi_rgb565(Color)
#define TK_INDEX2COLOR(Index)   tk_gdi_rgb888(Index)
#endif

#ifdef TK_BPP_32
#define TK_COLOR2INDEX(Color)   (Color)
#define TK_INDEX2COLOR(Index)   (Index)
#endif


#define GDI_SETCOLOR(col) do { \
	tk_gdi.color = col; \
} while (0)

#define GDI_SETBGCOLOR(bgcol) do { \
	tk_gdi.bgcolor = bgcol; \
} while (0)

#define GDI_SETFONTSIZE(ft_size) do { \
	tk_gdi.font_size = ft_size; \
} while (0)

#define GDI_SETTEXTCOLOR(txt_col) do { \
	tk_gdi.txtcol = txt_col; \
} while (0)

#define GDI_GETTEXTCOLOR() (tk_gdi.txtcol)

#define GDI_SETPENSIZE(PSize) do { \
	tk_gdi.nPSize = (unsigned char)PSize; \
} while (0)

#define GDI_SETLINESTYLE(enLStyle) do { \
	tk_gdi.nLStyle = enLStyle; \
} while (0)

#define GDI_GETLINESTYLE() (tk_gdi.nLStyle)

#define GDI_SETLABELSTYLE(labelstyle) do { \
    tk_gdi.nLabelStyle = labelstyle; \
} while (0)

#define GDI_GETLABELSTYLE() (tk_gdi.nLabelStyle)

#define GDI_SET_SHOULD_PIXEL_OVERLAP(is_overlap) do { \
    tk_gdi.pixel_should_overlap = is_overlap; \
} while (0)

//TODO: will change these two routines into defines later
extern void tk_add_color(gdi_color color);
extern void tk_gdi_setcolor(gdi_color col);
extern void tk_gdi_setbkcolor(gdi_color bkCol);

extern void tk_init_gdicontext(void);
extern void release_gdicontext(); 
extern void tk_reinit_gdicontext(int w, int h);
extern void draw_bg(short zoom_level, int is_file_not_found_scene);

extern void tk_reset_filter_screen();
extern void tk_set_filter_screen(struct envelope rect);

extern void draw_point(struct layer *pl, int vlevel); 
extern void draw_rail(struct layer *pl, int vlevel);
extern void draw_road(struct layer *pl,  int vlevel);
extern void draw_linelayer(struct layer *pl, int vlevel);
extern int tk_geo_drawvmlpoly(struct layer *pl, int vlevel); 

extern void _tk_mem_blt_16(unsigned char* dest, unsigned short width, short dx, short dy, unsigned short height);
extern void tk_gdi_cleancdata(struct envelope filter, short int zoom_level, int is_file_not_found_scene);

extern void tk_init_label();
extern void tk_destroy_label();
extern int draw_label();
extern void tk_geo_cleanmaplabel(); 
extern void release_points();
extern void refresh_local_feature(int x, int y, int flag);

extern void begin_area_fill();
extern void draw_area();
extern void end_area_fill();
extern int judge_feature_env_relation(struct feature* pft, int scr_env_id);
extern void get_point_from_feature(struct feature* pfeature, int* ptnum);
extern int clipgeopoints(struct tk_point *pPoints, int ptNbr, struct envelope rect, int nGType, struct tk_point **pPts, int *ptNum);
extern int tk_geo_add_label(struct feature* ft, struct tk_point *pPts, int ptNum, struct envelope rect, int label_type, int icon_id, int outline_color, int font_alter);
extern void add_polygon(int n, struct tk_point *pts);
extern int insert_label_buffer(struct envelope *label_rects, struct tk_point_slope *label_point, 
        struct tk_point *pPts, int label_rect_num,char *name, int name_len, int font_size, int icon_id, int outline_color, int icon_x, int icon_y);
extern void get_map_road_name(struct layer *pl, int vlevel);
extern void get_map_polygon_name(struct layer *pl, int vlevel);
#endif

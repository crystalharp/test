/*
 * =====================================================================================
 *
 *       Filename:  tkm_layer.c
 *
 *    Description: functions related with the map's layers such as greenland, water etc. 
 *
 *        Created:  2011年04月22日 10时42分09秒
 *        Company:  
 *
 * =====================================================================================
 */
#include <stdlib.h>

#include "tkm_render.h"
#include "tkm_layer.h"

/* ==============================================
 * layer management operation 
 * ============================================== */

void extend(int l, struct tile *tile) 
{

}

void cut(int l)
{

}

void get_status(int l)
{

}

/* ==============================================
 * layer draw atomic operation 
 * ============================================== */

void set_gdi(int l)
{
    struct style *pstyle;
    pstyle = &(tk_engine.ps->styles[l]);

    GDI_SETCOLOR(pstyle->fill_color);
    GDI_SETBGCOLOR(pstyle->border_color);
    GDI_SETPENSIZE(pstyle->pen_width[0]);
    GDI_SETLABELSTYLE(pstyle->label_style);
    GDI_SETTEXTCOLOR(pstyle->fontcolor);

}

static int get_polygon_edges(struct feature *pfeature) 
{
    int isrval = 0;
    struct tk_point *ppts;
    int ptnum = 0;
    int transferred_coord_num = 0;

    while (pfeature != NULL) {
        if (pfeature->name_index >= 0) {
            /* 如果名字需要显示,在tk_geo_add_label确认 */
            pfeature->name_index = TK_NAME_DO_NOT_NEED_DISPLAY;
            isrval = judge_feature_env_relation(pfeature, TK_WHOLE_SCREEN_ENV);
            if (isrval == 0) {//无交叉
                goto next;
            }

            //tk_gdi.draw_point_buffer中存入屏幕坐标,ptnum更新为tk_gdi.draw_point_buffer点数目,以后的裁减基于tk_gdi.draw_point_buffer
            get_point_from_feature(pfeature, &ptnum);
            ppts = tk_gdi.draw_point_buffer;
            transferred_coord_num = ptnum;
            if (isrval != 3) {//不是被包含，即交叉, 因此需裁剪
                if (clipgeopoints(tk_gdi.draw_point_buffer, transferred_coord_num, vm_screen, 2, &ppts, &ptnum) == 0) {
                    goto next;
                }
            }
            if (ptnum >= 2) {
                //在其中会更改pfeature->name_index以便确认以后是否可能依然需要显示
                tk_geo_add_label(pfeature, ppts, ptnum, vm_screen, 0, -1, 0xffffff,0);
            }
            ppts = tk_gdi.draw_point_buffer;
        }
        isrval = judge_feature_env_relation(pfeature, TK_CLIP_SCREEN_ENV);
        if (isrval == 0) {
            goto next;
        }

        //tk_gdi.draw_point_buffer中存入屏幕坐标,ptnum更新为tk_gdi.draw_point_buffer点数目,以后的裁减基于tk_gdi.draw_point_buffer
        get_point_from_feature(pfeature, &ptnum);
        ppts = tk_gdi.draw_point_buffer;
        transferred_coord_num = ptnum;

        if (isrval != 3) {
            if (clipgeopoints(tk_gdi.draw_point_buffer, transferred_coord_num, tk_gdi.rtview, 2, &ppts, &ptnum) == 0) {
                goto next;
            }
        }
        if (ptnum >= 2) {
            add_polygon(ptnum, ppts);
        }
next:   pfeature = pfeature->layer_next;        
    }   
    return 1;
}

static void draw_polygon(struct feature *pf)
{
    begin_area_fill();
    get_polygon_edges(pf);
    draw_area();
    end_area_fill();
}

/* ==============================================
 * layer draw part
 * ============================================== */

/* Layer 0 ,1 -- GreenLand, Water */
static void draw_greenland_water(int l)
{
    struct feature *pf = NULL;

    if ((pf = GET_LAYER_FEATURES(l)) != NULL) {
        set_gdi(l);
        draw_polygon(pf);
    }

    return;
}

struct draw_layer draw_all[LAYER_MAX] = {
    {1, draw_greenland_water}
};

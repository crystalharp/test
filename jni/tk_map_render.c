//
//  tk_map_render.c
//  tigermap
//
//  Created by Chen Ming on 13-6-7.
//  Copyright (c) 2013年 TigerKnows. All rights reserved.
//

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "tk_error.h"
#include "tk_context.h"
#include "tk_global_info.h"
#include "tk_geo_util.h"
#include "tk_map_render.h"
#include "tk_log.h"
#include "tk_label.h"

#define TK_COLOR_GREY   0xe4e4e4
#define WHITE_DASH_LEN 15
#define COLOR_DASH_LEN 14


static tk_status_t _tk_check_layer_and_set_gdi(tk_context_t *context, tk_layer_t *layer, tk_bool_t need_pen_width) {
    tk_gdi_t *gdi = &context->gdi;
    int pen_width = 0;
    tk_style_t *style = context->style;
    int level = context->zoom;
    if (need_pen_width) {
        pen_width = style->pen_width[level - style->zoom_min];
        if (pen_width == 0)
            return TK_STATUS_NEED_NOT_DRAW;
    }
    else {
        pen_width = 1;
    }
    
    if (level < style->zoom_min || level > style->zoom_max)
        return TK_STATUS_NEED_NOT_DRAW;
    gdi->color = style->fill_color;
    gdi->background_color = style->border_color;
    gdi->pen_size = pen_width;
    gdi->line_style = style->line_type;
    gdi->text_style = style->label_style;
    gdi->text_color = style->fontcolor;
    gdi->font_size = style->font_size;
    gdi->icon_id = style->icon_id;
    if (level < style->label_min || level > style->label_max) {
        gdi->label_priority = 0;
    }
    else {
        gdi->label_priority = style->label_prioritys[level - style->label_min];
    }
    return TK_STATUS_SUCCESS;
};

static tk_rect_relation_t _tk_get_relation_of_feature_and_filter(tk_context_t *context, tk_feature_t *pfeature, tk_bool_t is_line) {
    tk_envelope_t temp_env, filter_rect;
    tk_feature_t *cur_ft = pfeature;
    int relation = TK_RECT_DISJOINT, cur_relation = TK_RECT_DISJOINT;
    if (is_line) {
        filter_rect = context->line_feature_filter_rect;
    }
    else {
        filter_rect = context->polygon_feature_filter_rect;
    }
    while (cur_ft != NULL) {
        temp_env.left = cur_ft->feature->left_top.x;
        temp_env.top = cur_ft->feature->left_top.y;
        temp_env.right = cur_ft->feature->right_bottom.x;
        temp_env.bottom = cur_ft->feature->right_bottom.y;
        cur_relation = tk_geo_get_rect_relation(temp_env, filter_rect);
        if (cur_relation == TK_RECT_INTERSECT || cur_relation == TK_RECT_COVER)
            return TK_RECT_INTERSECT;
        if ((relation == TK_RECT_COVER && cur_relation == TK_RECT_DISJOINT) ||
            (relation == TK_RECT_DISJOINT && cur_relation == TK_RECT_COVER)) {
            return TK_RECT_INTERSECT;
        }
        relation = cur_relation;
        cur_ft = cur_ft ->next;
    }
    return relation;
}

static tk_status_t _tk_get_points_from_line_feature(tk_context_t *context, tk_feature_t *pfeature) {
    tk_feature_t *cur_feature = pfeature;
    int i, points_num;
    tk_point_buf_t *point_buf = &context->feature_point_buf;
    tk_point_t *point = NULL;
    int ref_x = context->center_tile_pix_box.left;
    int ref_y = context->center_tile_pix_box.top;
    while (cur_feature) {
        tk_feature_data_t *feature_data = cur_feature->feature;
        if (!feature_data) {
            cur_feature = cur_feature->next;
            continue;
        }
        if (cur_feature == pfeature) {//第一个才有
            if (!feature_data->points_num) {
                LOG_DBG("NULL feature points");
                cur_feature = cur_feature->next;
                continue;
            }
            point = feature_data->points + 0;//feature中存的点，已经是tile像素坐标，只需要减去左上角坐标（y轴向下）
            if(tk_point_buf_add_one(point_buf, (point->x - ref_x) * context->scale, (point->y - ref_y) * context->scale, 0) == TK_STATUS_NO_MEMORY) {//不需要feature的level_code
                return TK_STATUS_NO_MEMORY;
            }
        }
        points_num = feature_data->points_num;
        for (i = 1; i < points_num; i++) {//中间的feature的两个边界点要去掉一个，但不能两个都去掉，否则会出现错位）
            point = cur_feature->feature->points + i;
            if(tk_point_buf_add_one(point_buf, (point->x - ref_x) * context->scale, (point->y - ref_y) * context->scale, 0) == TK_STATUS_NO_MEMORY) {//不需要feature的level_code
                return TK_STATUS_NO_MEMORY;
            }
        }
//        if (cur_feature->next == NULL) {//最后一个
//            point = feature_data->points + points_num - 1;
//            if(tk_point_buf_add_one(point_buf, (point->x - ref_x) * context->scale, (point->y - ref_y) * context->scale, 0) == TK_STATUS_NO_MEMORY) {//不需要feature的level_code
//                return TK_STATUS_NO_MEMORY;
//            }
//        }
        cur_feature = cur_feature->next;
    }
    context->point_num += context->feature_point_buf.point_num;
    return TK_STATUS_SUCCESS;
}

static tk_status_t _tk_get_points_from_poly_feature(tk_context_t *context, tk_feature_t* pfeature) {
    tk_feature_t *cur_feature = pfeature;
    int i, points_num;
    tk_point_buf_t *point_buf = &context->feature_point_buf;
    tk_point_t *point = NULL;
    int ref_x = context->center_tile_pix_box.left;
    int ref_y = context->center_tile_pix_box.top;
    points_num = cur_feature->feature->points_num;
    for (i = 0; i < points_num; i++) {
        point = cur_feature->feature->points + i;
        if(tk_point_buf_add_one(point_buf, (point->x - ref_x) * context->scale, (point->y - ref_y) * context->scale, 0) == TK_STATUS_NO_MEMORY) {//不需要feature的level_code
            return TK_STATUS_NO_MEMORY;
        }
    }
    context->point_num += context->feature_point_buf.point_num;
    return TK_STATUS_SUCCESS;
}

//Liang-Barsky Line Clipping  Test
static inline int _tk_lb_clip_test(float p, float q, float *u1, float *u2) {
    float r;
    int retval = 1;
    
    if (p < 0.0) {//out->in
        r = q / p;
        if (r > *u2)
            retval = 0;
        else if (r > *u1)
            *u1 = r;
    } else if (p > 0.0) {//in->out
        r = q / p;
        if (r < * u1)
            retval = 0;
        else if (r < *u2)
            *u2 = r;
    } else {
        /* p = 0, so line is parallel to this clipping edge */
        if (q < 0.0)
        /* Line is outside clipping edge */
            retval = 0;
    }
    return retval;
}

static void _tk_clip_line(tk_point_buf_t *clipped_buf, const tk_point_buf_t *src_point_buf, const tk_envelope_t rect) {
    tk_point_t *points = src_point_buf->points;
    int i = 0, points_num = src_point_buf->point_num;
    
    for (i = 0; i < points_num - 1; i++){
        tk_point_t *p1 = points + i;
        tk_point_t *p2 = points + i + 1;
        float u1 = 0.0, u2 = 1.0, dx, dy;
        dx = (float)(p2->x - p1->x);
        if (_tk_lb_clip_test(-dx, (float)(p1->x - rect.left), &u1, &u2)) {
            //right edge
            if (_tk_lb_clip_test(dx, (float)(rect.right - p1->x), &u1, &u2)) {
                dy =(float)( p2->y - p1->y);
                //top edge
                if (_tk_lb_clip_test(-dy,(float)( p1->y - rect.top), &u1, &u2)) {
                    //bottom edge
                    if (_tk_lb_clip_test(dy,(float)( rect.bottom - p1->y), &u1, &u2)) {
                        if (u1 > 0.0) {
                            tk_point_buf_add_one(clipped_buf, (int)(u1 * dx) + p1->x, (int)(u1 * dy) + p1->y, i + 1);
                        }
                        else {
                            tk_point_buf_add_one(clipped_buf, p1->x, p1->y, 0);
                        }
                        
                        if (i == points_num - 2 && u2 >= 1.0) {
                            tk_point_buf_add_one(clipped_buf, p2->x, p2->y, 0);
                            break;
                        }
                        if (u2 < 1.0) {
                            tk_point_buf_add_one(clipped_buf, (int)(u2 * dx) + p1->x, (int)(u2 * dy) + p1->y, i + 1);
                        }
                    }
                }
            }
        }
    }
}

static tk_rect_relation_t _tk_get_line_feature_point(tk_context_t *context, tk_feature_t *line_feature) {
    tk_rect_relation_t relation;
    relation = _tk_get_relation_of_feature_and_filter(context, line_feature, TK_YES);
    if(relation == TK_RECT_DISJOINT) {
        return relation;
    }
    //清空顶点缓存
    tk_contxt_clear_point_buf(context);
    
    //添加feature顶点
    if(_tk_get_points_from_line_feature(context, line_feature) == TK_STATUS_NO_MEMORY) {
        tk_set_result(TK_STATUS_NO_MEMORY);
        return TK_RECT_DISJOINT;
    }
    
    if (relation != TK_RECT_COVER) {//need clip
        _tk_clip_line(&context->clipped_point_buf, &context->feature_point_buf, context->draw_rect);
    }
    return relation;
}

static int _tk_find_index_swcity(int cid) {
    if(cid == -1)
        return -1;
    int i = 0;
    for(i = 0; i < tk_global_info.subway_infos.city_num; ++i){
        if(tk_global_info.subway_infos.city_infos[i].cityid == cid){
            return  i;
        }
    }
    return -1;
}

static void _tk_set_subway_gdi(tk_gdi_t *gdi, int rid, tk_feature_t *subway_feature) {
    int color_index = -1;
    int current_city_id = 0;
    current_city_id = tk_get_cid_by_rid(rid);
    color_index = _tk_find_index_swcity(current_city_id);
    if(color_index != -1) {
        int i = 0, is_found = 0, num = tk_global_info.subway_infos.city_infos[color_index].color_num;
        for(;i < num; i++) {
            tk_subway_color_info_t *color_info = tk_global_info.subway_infos.city_infos[color_index].color_info + i;
            char *name = color_info->subway_name;
            if(strlen(name) == subway_feature->feature->name_length){
                if(strncmp(name, subway_feature->feature->name, subway_feature->feature->name_length) == 0){
                    gdi->color = color_info->color;
                    gdi->text_color = color_info->color;
                    is_found = 1;
                    break;
                }
            }
        }
        if (!is_found) {
            gdi->color = 0x4ABAA5;
        }
    } else {
        gdi->color = 0x4ABAA5;
    }
}

static void _tk_draw_normal_line(tk_context_t *context, tk_point_t *points, int point_num) {
    cairo_t *cr = context->cr;
    cairo_move_to(cr, points[0].x, points[0].y);
    for(int i = 0; i < point_num; i++) {
        cairo_line_to(cr, points[i].x, points[i].y);
    }
}

static void _tk_draw_line_with_boudary(tk_context_t *context, tk_point_t *points, int point_num) {
    int i, last_boundary_idx = 0;
    tk_bool_t is_in_view = TK_YES;
    tk_point_t *boundary_point = points;
    for (i = 1; i < point_num; ++ i) {//从1开始，因为第0个点必然是in
        if (points[i].level_code > 0) {
            if (is_in_view) {
                _tk_draw_normal_line(context, boundary_point, i - last_boundary_idx + 1);
                is_in_view = TK_NO;
            } else {
                boundary_point = points + i;
                last_boundary_idx = i;
                is_in_view = TK_YES;
            }
        }
    }
    if (is_in_view) {
        _tk_draw_normal_line(context, boundary_point, point_num - last_boundary_idx);
    }
}

static tk_status_t _tk_draw_subways(tk_context_t *context, tk_layer_t *subway_layer) {
    tk_feature_t *subway_feature = subway_layer->features;
    tk_rect_relation_t relation;
    tk_point_buf_t *points_buf_to_draw = NULL;
    cairo_t *cr = context->cr;
    tk_gdi_t *gdi = &context->gdi;
    LOG_DBG("to render subway_layer");
    while (subway_feature) {
        relation = _tk_get_line_feature_point(context, subway_feature);
        if (relation == TK_RECT_DISJOINT) {
            if (tk_get_last_result() == TK_STATUS_NO_MEMORY) {//考虑不做检查
                return TK_STATUS_NO_MEMORY;
            }
            goto NEXT;
        }
        
        //set style
        _tk_set_subway_gdi(gdi, subway_feature->feature->tile->region_id, subway_feature);
        
        // add label
        if (context->zoom >= context->style->label_min &&
            context->zoom <= context->style->label_max &&
            gdi->label_priority != 0) {
            tk_add_line_feature_to_labels(context, subway_feature->feature);
        }
        
        //create line path
        if (relation == TK_RECT_COVER) {
            points_buf_to_draw = &context->feature_point_buf;
            if (points_buf_to_draw->point_num >= 2) {
                _tk_draw_normal_line(context, points_buf_to_draw->points, points_buf_to_draw->point_num);
            }
        }
        else {
            points_buf_to_draw = &context->clipped_point_buf;
            if (points_buf_to_draw->point_num >= 2) {
                _tk_draw_line_with_boudary(context, points_buf_to_draw->points, points_buf_to_draw->point_num);
            }
        }
        LOG_DBG("to draw subway_layer with cairo");
        //draw
        cairo_set_line_width(cr, gdi->pen_size + 2);
        cairo_set_line_cap(cr, CAIRO_LINE_CAP_ROUND);
        cairo_set_source_rgb(cr, 1, 1, 1);
        cairo_stroke_preserve(cr);//每条不同的地铁线路都要独立绘制

        cairo_set_line_width(cr, gdi->pen_size);
        cairo_set_line_cap(cr, CAIRO_LINE_CAP_ROUND);
        cairo_set_source_rgb(cr, (double)(GET_RED(gdi->color ))/(double)256,
                             (double)(GET_GREEN(gdi->color))/(double)256,
                             (double)(GET_BLUE(gdi->color))/(double)256);
        cairo_stroke(cr);//每条不同的地铁线路都要独立绘制
    NEXT:
        subway_feature = subway_feature->layer_next;
    }
    LOG_DBG("render subway_layer success");
    return TK_STATUS_SUCCESS;
}

static tk_status_t _tk_draw_railway(tk_context_t *context, tk_layer_t *rail_layer) {
    tk_feature_t *railway_feature = rail_layer->features;
    tk_rect_relation_t relation;
    tk_point_buf_t *points_buf_to_draw = NULL;
    cairo_t *cr = context->cr;
    tk_gdi_t *gdi = &context->gdi;
    double dash_len = WHITE_DASH_LEN;
    LOG_DBG("to render rail_layer");
    while (railway_feature) {
        relation = _tk_get_line_feature_point(context, railway_feature);
        if (relation == TK_RECT_DISJOINT) {
            if (tk_get_last_result() == TK_STATUS_NO_MEMORY) {//考虑不做检查
                return TK_STATUS_NO_MEMORY;
            }
            goto NEXT;
        }
        
        // add label
        if (context->zoom >= context->style->label_min &&
            context->zoom <= context->style->label_max &&
            gdi->label_priority != 0) {
            tk_add_line_feature_to_labels(context, railway_feature->feature);
        }
        
        // to draw black line first
        //create line path
        if (relation == TK_RECT_COVER) {
            points_buf_to_draw = &context->feature_point_buf;
            if (points_buf_to_draw->point_num >= 2) {
                _tk_draw_normal_line(context, points_buf_to_draw->points, points_buf_to_draw->point_num);
            }
        }
        else {
            points_buf_to_draw = &context->clipped_point_buf;
            if (points_buf_to_draw->point_num >= 2) {
                _tk_draw_line_with_boudary(context, points_buf_to_draw->points, points_buf_to_draw->point_num);
            }
        }
        LOG_DBG("to draw rail_layer with cairo");
        //draw black line
        cairo_set_line_width(cr, gdi->pen_size);
        cairo_set_line_cap(cr, CAIRO_LINE_CAP_ROUND);
        cairo_set_source_rgb(cr, (double)(GET_RED(gdi->background_color ))/(double)256,
                             (double)(GET_GREEN(gdi->background_color))/(double)256,
                             (double)(GET_BLUE(gdi->background_color))/(double)256);
        cairo_stroke(cr);
        
        // than draw dash line
        points_buf_to_draw = &context->feature_point_buf;//重建line path，不做裁剪，让cairo裁剪，这样虚线看起来会比较均匀
        if (points_buf_to_draw->point_num >= 2) {
            _tk_draw_normal_line(context, points_buf_to_draw->points, points_buf_to_draw->point_num);
        }
        cairo_set_line_width(cr, gdi->pen_size - 2);
        cairo_set_dash(cr, &dash_len, 1, 0);
        cairo_set_line_cap(cr, CAIRO_LINE_CAP_BUTT);
        cairo_set_source_rgb(cr, (double)(GET_RED(gdi->color ))/(double)256,
                             (double)(GET_GREEN(gdi->color))/(double)256,
                             (double)(GET_BLUE(gdi->color))/(double)256);
        cairo_stroke(cr);
    NEXT:
        cairo_set_dash(cr, &dash_len, 0, 0);//恢复实线模式
        railway_feature = railway_feature->layer_next;
    }
    LOG_DBG("render rail_layer success");
    return TK_STATUS_SUCCESS;

}

static tk_status_t _tk_draw_road(tk_context_t *context, tk_layer_t *road_layer) {
    tk_feature_t *road_feature = road_layer->features;
    tk_rect_relation_t relation;
    tk_point_buf_t *points_buf_to_draw = NULL;
    cairo_t *cr = context->cr;
    tk_gdi_t *gdi = &context->gdi;
    
    while (road_feature) {
        relation = _tk_get_line_feature_point(context, road_feature);
        if (relation == TK_RECT_DISJOINT) {
            if (tk_get_last_result() == TK_STATUS_NO_MEMORY) {//考虑不做检查
                return TK_STATUS_NO_MEMORY;
            }
            goto NEXT;
        }
        
        // add label
        if (context->zoom >= context->style->label_min &&
            context->zoom <= context->style->label_max &&
            gdi->label_priority != 0) {
            tk_add_line_feature_to_labels(context, road_feature->feature);
        }
        
        //create line path
        if (relation == TK_RECT_COVER) {
            points_buf_to_draw = &context->feature_point_buf;
            if (points_buf_to_draw->point_num >= 2) {
                _tk_draw_normal_line(context, points_buf_to_draw->points, points_buf_to_draw->point_num);
            }
        }
        else {
            points_buf_to_draw = &context->clipped_point_buf;
            if (points_buf_to_draw->point_num >= 2) {
                _tk_draw_line_with_boudary(context, points_buf_to_draw->points, points_buf_to_draw->point_num);
            }
        }
    NEXT:
        road_feature = road_feature->layer_next;
    }
    LOG_DBG("to draw road_layer with cairo");
    // draw background
    cairo_set_line_width(cr, gdi->pen_size);
    cairo_set_line_cap(cr, CAIRO_LINE_CAP_ROUND);
    cairo_set_source_rgb(cr, (double)(GET_RED(gdi->background_color ))/(double)256,
                         (double)(GET_GREEN(gdi->background_color))/(double)256,
                         (double)(GET_BLUE(gdi->background_color))/(double)256);
    if (!((road_layer->features->feature->type == 2 && (context->zoom >= 14 && context->zoom <=15))
    		|| (road_layer->features->feature->type == 3 && (context->zoom >= 10 && context->zoom <=13)))) {
        cairo_stroke_preserve(cr);
        // draw foreground
        if (gdi->pen_size > 4) {
            cairo_set_line_width(cr, gdi->pen_size - 3);
        }
        else if (gdi->pen_size == 4){
            cairo_set_line_width(cr, gdi->pen_size - 2);
        }
        else {
            cairo_set_line_width(cr, gdi->pen_size);
        }
        cairo_set_source_rgb(cr, (double)(GET_RED(gdi->color ))/(double)256,
                             (double)(GET_GREEN(gdi->color))/(double)256,
                             (double)(GET_BLUE(gdi->color))/(double)256);
    }
    if((road_layer->features->feature->type == 5 && (context->zoom == 10))) {
    	cairo_set_source_rgb(cr, (double)(GET_RED(gdi->color ))/(double)256,
    	                             (double)(GET_GREEN(gdi->color))/(double)256,
    	                             (double)(GET_BLUE(gdi->color))/(double)256);
    }
    cairo_stroke(cr);
    LOG_DBG("render road_layer success");
    return TK_STATUS_SUCCESS;
}

static tk_status_t _tk_draw_line_layer(tk_context_t *context, tk_layer_t *layer) {
    if (!layer || !layer->features) {
        return TK_STATUS_SUCCESS;
    }
    tk_feature_t *layer_feature = layer->features;
    int feature_type = layer_feature->feature->type;
    if (context->zoom > TK_NATIONAL_LEVEL_A) {
        switch (feature_type) {
            case 8://铁路
                return _tk_draw_railway(context, layer);
            case 9://地铁
                return _tk_draw_subways(context, layer);
            default://其他线路
                return _tk_draw_road(context, layer);
        }
    }
    else {
        switch (feature_type) {
            case 5://铁路
                return _tk_draw_railway(context, layer);
            default://其他线路
                return _tk_draw_road(context, layer);
        }
    }
}

static tk_status_t _tk_draw_polygon_layer(tk_context_t *context, tk_layer_t *layer) {
    cairo_t *cr = context->cr;
    tk_gdi_t *gdi = &context->gdi;
    tk_feature_t *poly_feature = layer->features;
    tk_point_buf_t *points_buf_to_draw = NULL;
    tk_rect_relation_t relation;
    
    while (poly_feature) {
        LOG_DBG("to create path of polygon_layer");
        relation = _tk_get_relation_of_feature_and_filter(context, poly_feature, TK_NO);
        if(relation == TK_RECT_DISJOINT) {
            goto NEXT;
        }
        
        //清空顶点缓存
        tk_contxt_clear_point_buf(context);
        
        //添加feature顶点
        if(_tk_get_points_from_poly_feature(context, poly_feature) == TK_STATUS_NO_MEMORY) {
            return TK_STATUS_NO_MEMORY;
        }
        
        if (poly_feature->feature->has_name &&
            context->zoom >= context->style->label_min &&
            context->zoom <= context->style->label_max &&
            gdi->label_priority != 0) {//添加label
            tk_add_polygon_feature_to_labels(context, poly_feature->feature);
        }
        
        //create line path
        //这里和线型feature不同，不用剪切，让cairo剪切
        points_buf_to_draw = &context->feature_point_buf;
        if (points_buf_to_draw->point_num >= 2) {
            _tk_draw_normal_line(context, points_buf_to_draw->points, points_buf_to_draw->point_num);
        }
    NEXT:
        poly_feature = poly_feature->layer_next;
    }
    LOG_DBG("to draw polygon_layer with cairo");
    cairo_set_line_width(cr, 1);
    cairo_set_source_rgb(cr, (double)(GET_RED(gdi->color))/(double)256,
                         (double)(GET_GREEN(gdi->color))/(double)256,
                         (double)(GET_BLUE(gdi->color))/(double)256);
    cairo_close_path(cr);
    cairo_set_fill_rule(cr, CAIRO_FILL_RULE_EVEN_ODD);
    cairo_fill(cr);
    LOG_DBG("render polygon_layer success");
    return TK_STATUS_SUCCESS;
}

//#define TK_GDI_COLOR_WATER      10007243
//#define TK_GDI_COLOR_BG         0xf7f4f6 //15460060

static void _tk_clean_tile_with_color(tk_context_t *context, unsigned int color) {
    cairo_t *cr = context->cr;
    cairo_set_antialias(cr, CAIRO_ANTIALIAS_NONE);
    cairo_set_source_rgb(cr, (double)(GET_RED(color))/(double)256,
                         (double)(GET_GREEN(color))/(double)256,
                         (double)(GET_BLUE(color))/(double)256);
    cairo_move_to(cr, 0, 0);
    cairo_line_to(cr, 0, tk_global_info.tile_size_pix);
    cairo_line_to(cr, tk_global_info.tile_size_pix, tk_global_info.tile_size_pix);
    cairo_line_to(cr, tk_global_info.tile_size_pix, 0);
    cairo_line_to(cr, 0, 0);
    cairo_close_path(cr);
    cairo_set_fill_rule(cr, CAIRO_FILL_RULE_EVEN_ODD);
    cairo_fill(context->cr);
    cairo_set_antialias(cr, CAIRO_ANTIALIAS_FAST);
}

tk_status_t tk_render_tile_default (int tile_x, int tile_y, int zoom) {
    tk_status_t result = TK_STATUS_SUCCESS;
    tk_context_t *context = tk_get_context();
    int i = 0, layer_idx = 0;
    tk_layer_t *layer;
    int obj_type = 0;
    context->point_num = 0;
    if (context->zoom > TK_NATIONAL_LEVEL_A) {
        _tk_clean_tile_with_color(context, TK_GDI_COLOR_BG);
    }
    else {
        _tk_clean_tile_with_color(context, TK_GDI_COLOR_WATER);
    }
    for(i = 0; i < context->cur_style_buf->layer_num; ++ i){
        layer_idx = context->cur_style_buf->draw_order[i];
        if (layer_idx < 0) {
            continue;
        }
        layer = context->layer_list + layer_idx;
        context->style = context->cur_style_buf->styles + layer_idx;
        if (layer == NULL || layer->features == NULL)
            continue;
        obj_type = context->cur_style_buf->obj_type[layer_idx];
        
        if (tk_global_info.layer_ctl[layer_idx] == 1) {
            switch (obj_type) {
                case TKGEO_ENMFTYPE_POINT:
                    if (_tk_check_layer_and_set_gdi(context, layer, TK_NO) == TK_STATUS_SUCCESS) {
                        if (context->gdi.label_priority != 0) {
                            tk_add_poi_labels(context, layer);
                        }
                    }
                    break;
                case TKGEO_ENMFTYPE_LINE:
                case TKGEO_ENMFTYPE_RAIL:
                case TKGEO_ENMFTYPE_ROAD:
                    if (_tk_check_layer_and_set_gdi(context, layer, TK_YES) == TK_STATUS_SUCCESS) {
                        _tk_draw_line_layer(context, layer);
                    }
                    break;
                case TKGEO_ENMFTYPE_POLY:
                    if (_tk_check_layer_and_set_gdi(context, layer, TK_NO) == TK_STATUS_SUCCESS) {
                        _tk_draw_polygon_layer(context, layer);
                    }
                    break;
                default:
                    break;
            }
        }
    }
    LOG_DBG("tk_render_tile_default success: total point_num: %i", context->point_num);
    return result;
}



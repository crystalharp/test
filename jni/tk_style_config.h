//
//  tk_style_config.h
//  tigermap
//
//  Created by Chen Ming on 13-6-13.
//  Copyright (c) 2013年 TigerKnows. All rights reserved.
//

#ifndef tigermap_tk_style_config_h
#define tigermap_tk_style_config_h

#include "tk_types.h"

#define TK_GDI_COLOR_WATER      11062014
#define TK_GDI_COLOR_BG         0xf7f4f6 //15460060

struct _tk_style {
	tk_color_t	    border_color;
	tk_color_t	    fill_color;
    tk_color_t      fontcolor;//feature's label's color
    int             font_size;
    int             font_alter;
    short           label_priority;
    unsigned char * label_prioritys;
	char			*pen_width;
	char			line_type; //line style's type: 0 for polygons and points, 1 for the lines, and 2 for Zha Dao, Railway, Subway
	char			dash_ratio;
	char			zoom_min;
	char			zoom_max;
    char            label_min;
    char            label_max;
	char			icon_id;
    unsigned char   label_style;
};

struct _tk_styles_buffer {
    int layer_num;
    tk_style_t *styles;
    short *obj_type;
    short *draw_order;
};

#endif

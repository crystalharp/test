/*
 * =====================================================================================
 *
 *       filename:  tkm_layer.h
 *
 *    description: functions related with the map's layers such as greenland, water etc. 
 *
 *        version:  1.0
 *        created:  2011年04月22日 10时39分50秒
 *        company:  
 *
 * =====================================================================================
 */

#ifndef __TKM_LAYER_H
#define __TKM_LAYER_H


enum LAYER_ID {
    LAYER_GREENLAND,
    LAYER_WATER,
    LAYER_HUTONG,
    LAYER_STREET,
    LAYER_SUBMAIN_ROAD,
    LAYER_MAIN_ROAD,
    LAYER_HIGHWAY,
    LAYER_ZHADAO,
    LAYER_RAILWAY,
    LAYER_SUBWAY,
    LAYER_LANDUSE,
    LAYER_TRAIN_STATION,
    LAYER_BUS_STATION,
    LAYER_SUBWAY_STATION,
    LAYER_AIRPORT,
    LAYER_HOSPITAL,
    LAYER_PORT,
    LAYER_HOTEL,
    LAYER_GOVERNMENT,
    LAYER_SPECIAL_GOVERNMENT,
    LAYER_MARKET,
    LAYER_BUILDING,
    LAYER_SCHOOL,
    LAYER_STADIUM,
    LAYER_PARK,
    LAYER_MUSEUM,
    LAYER_SPECIAL_PARK,
    LAYER_THEATER,
    LAYER_CULTURE,
    LAYER_LIBRARY,
    LAYER_BANK,
    LAYER_TV_TOWER,
    LAYER_RESIDENTIAL,
    LAYER_RESEARCH,
    LAYER_HI_TECH_PARK,
    LAYER_GOLF,
    LAYER_BRIDGE,
    LAYER_TV,
    LAYER_VILLAGE,
    LAYER_COUNTY,
    LAYER_MAX
};

struct layer {
    /* the head pointer of this layer's feature list */
	struct feature *features;
	short int fnum;
    /* the tail pointer of this layer's feature list */
    struct feature *ftail;
};

/* structure used to draw misc layer. */
struct draw_layer {
    int enabled;
    void (* draw)(int l);
};

#define GET_LAYER_FEATURES(l) (tk_engine.layer_list[l].features)

/* extend the layer's scope by add features from tile */
extern void extend(int l, struct tile *tile); 

/* remove obsoleted features from layer when it's necessary to truncate the memory */
extern void cut(int l);

/* get the status of the layer */
extern void get_status(int l);
extern struct draw_layer draw_all[];

#endif

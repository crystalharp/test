/*
 * =====================================================================================
 *
 *       Filename:  test_cairo.c
 *
 *    Description:  
 *
 *        Version:  1.0
 *        Created:  2011年07月01日 09时39分37秒
 *       Revision:  none
 *       Compiler:  gcc
 *
 *         Author:  YOUR NAME (), 
 *        Company:  
 *
 * =====================================================================================
 */

#include "tk_cairo.h"
#include "../tkm_mapint.h"
int main(){
    cairo_t *cr;
    int w = 100;
    int h = 100;
    int *pd = (int *)malloc(w*h*4);
    cairo_surface_t *surface; 
    surface = cairo_image_surface_create_for_data ((unsigned char *)pd, CAIRO_FORMAT_ARGB32, w, h, w * 4);
    cr = cairo_create (surface);
    cairo_line_to(cr,10,20);
    cairo_line_to(cr,10,30);
    cairo_stroke(cr);
}


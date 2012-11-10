LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_CFLAGS := -DTK_FB_BPP=32 -std=c99 -I./cairo
LOCAL_LDLIBS := -llog -lz

LOCAL_MODULE := tkengine
LOCAL_SRC_FILES := cairo-array.c cairo-bentley-otmann-rectangular.c cairo-bentley-otmann-rectlinear.c cairo-boxes.c cairo.c cairo-clip.c cairo-color.c cairo-composite-rectangles.c cairo-freelist.c cairo-gstate.c cairo-image-surface.c cairo-matrix.c cairo-misc.c cairo-path-bounds.c cairo-path-fill.c cairo-path-fixed.c cairo-path-stroke.c cairo-pattern.c cairo-pen.c cairo-polygon.c cairo-rectangle.c cairo-rectangular-scan-converter.c cairo-region.c cairo-slope.c cairo-spline.c cairo-stroke-style.c cairo-surface.c cairo-surface-fallback.c cairo-tor-scan-converter.c cairo-traps.c main.c pixman-access-accessors.c pixman-access.c pixman-bits-image.c pixman.c pixman-combine32.c pixman-combine64.c pixman-conical-gradient.c pixman-cpu.c pixman-edge-accessors.c pixman-edge.c pixman-fast-path.c pixman-general.c pixman-gradient-walker.c pixman-image.c pixman-implementation.c pixman-linear-gradient.c pixman-matrix.c pixman-mmx.c pixman-radial-gradient.c pixman-region16.c pixman-region32.c pixman-solid-fill.c pixman-timer.c pixman-trap.c pixman-utils.c tk_log.c tkm_font.c tkm_layer.c tkm_map.c tkm_mapint.c tkm_render.c tks_merger.c tks_suggest.c tk_util.c png.c png_config.h pngerror.c png.h pnglibconf.h pngpread.c pngread.c pngrtran.c  pngset.c pngtest.c pngwio.c pngwtran.c pngconf.h pngdebug.h    pngget.c pnginfo.h pngmem.c pngpriv.h pngrio.c pngrutil.c pngstruct.h  pngtrans.c pngwrite.c pngwutil.c tk_cairo.h
include $(BUILD_SHARED_LIBRARY)

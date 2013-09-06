//
//  tk_list.h
//  tigermap
//
//  Created by Chen Ming on 13-6-9.
//  Copyright (c) 2013年 TigerKnows. All rights reserved.
//

#ifndef tigermap_tk_list_h
#define tigermap_tk_list_h

typedef struct _tk_list {
    struct _tk_list *next, *prev;
} tk_list_t;

#define tk_container_of(ptr, type, member) \
((type *)((char *) (ptr) - (char *) &((type *)0)->member))

#endif

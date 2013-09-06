//
//  tk_file_util.h
//  tigermap
//
//  Created by Chen Ming on 13-6-13.
//  Copyright (c) 2013年 TigerKnows. All rights reserved.
//

#ifndef tigermap_tk_file_util_h
#define tigermap_tk_file_util_h

#include "tk_error.h"

unsigned int tk_get_file_size(const char *file_name);

unsigned char *tk_read_file_content(const char *file_name, unsigned int *size);

char *tk_read_text_file_content(const char *file_name, unsigned int *length);

int tk_mkdir(const char *file_path);

#endif

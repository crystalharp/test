//
//  tk_error.c
//  tigermap
//
//  Created by Chen Ming on 13-6-16.
//  Copyright (c) 2013年 TigerKnows. All rights reserved.
//

#include <stdio.h>
#include "tk_tls.h"
#include "tk_error.h"

/*
 * 如果一个函数返回值需要被调用者使用，但又不能用来判断函数执行状态，可在函数内部设置执行状态，在函数调用后判断执行状态。
 */
TK_DEFINE_THREAD_LOCAL(int, result);

int tk_get_last_result() {
    return *(int *)TK_GET_THREAD_LOCAL(result);
};

void tk_set_result(int result) {
    int *result_ptr = TK_GET_THREAD_LOCAL(result);
    *result_ptr = result;
};

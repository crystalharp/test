//
//  tk_hash.h
//  tigermap
//
//  Created by Chen Ming on 13-6-9.
//  Copyright (c) 2013年 TigerKnows. All rights reserved.
//

#ifndef tigermap_tk_hash_h
#define tigermap_tk_hash_h

#include "tk_error.h"
#include "tk_types.h"

typedef tk_bool_t
(*tk_hash_keys_equal_func_t) (const void *key_a, const void *key_b);

typedef tk_bool_t
(*tk_hash_predicate_func_t) (const void *entry);

typedef void
(*tk_hash_callback_func_t) (void *entry, void *closure);

tk_hash_table_t *
_tk_hash_table_create (tk_hash_keys_equal_func_t keys_equal);

void
_tk_hash_table_destroy (tk_hash_table_t *hash_table);

void *
_tk_hash_table_lookup (tk_hash_table_t  *hash_table,
                          tk_hash_entry_t  *key);

void *
_tk_hash_table_random_entry (tk_hash_table_t	   *hash_table,
                                tk_hash_predicate_func_t predicate);

tk_status_t
_tk_hash_table_insert (tk_hash_table_t *hash_table,
                          tk_hash_entry_t *entry);

void
_tk_hash_table_remove (tk_hash_table_t *hash_table,
                          tk_hash_entry_t *key);

void
_tk_hash_table_foreach (tk_hash_table_t	      *hash_table,
                           tk_hash_callback_func_t  hash_callback,
                           void			      *closure);

#endif

//
//  tk_list_inline.h
//  tigermap
//
//  Created by Chen Ming on 13-6-9.
//  Copyright (c) 2013年 TigerKnows. All rights reserved.
//

#ifndef tigermap_tk_list_inline_h
#define tigermap_tk_list_inline_h

#include "tk_list.h"

/*
 * borrow from cairo
 */

#define tk_list_entry(ptr, type, member) \
tk_container_of(ptr, type, member)

#define tk_list_first_entry(ptr, type, member) \
tk_list_entry((ptr)->next, type, member)

#define tk_list_last_entry(ptr, type, member) \
tk_list_entry((ptr)->prev, type, member)

#define tk_list_foreach(pos, head)			\
for (pos = (head)->next; pos != (head);	pos = pos->next)

#define tk_list_foreach_entry(pos, type, head, member)		\
for (pos = tk_list_entry((head)->next, type, member);\
&pos->member != (head);					\
pos = tk_list_entry(pos->member.next, type, member))

#define tk_list_foreach_entry_safe(pos, n, type, head, member)	\
for (pos = tk_list_entry ((head)->next, type, member),\
n = tk_list_entry (pos->member.next, type, member);\
&pos->member != (head);					\
pos = n, n = tk_list_entry (n->member.next, type, member))

#define tk_list_foreach_entry_reverse(pos, type, head, member)	\
for (pos = tk_list_entry((head)->prev, type, member);\
&pos->member != (head);					\
pos = tk_list_entry(pos->member.prev, type, member))

#define tk_list_foreach_entry_reverse_safe(pos, n, type, head, member)	\
for (pos = tk_list_entry((head)->prev, type, member),\
n = tk_list_entry (pos->member.prev, type, member);\
&pos->member != (head);					\
pos = n, n = tk_list_entry (n->member.prev, type, member))

#ifdef TK_LIST_DEBUG
static inline void
_tk_list_validate (const tk_list_t *link)
{
    assert (link->next->prev == link);
    assert (link->prev->next == link);
}
static inline void
tk_list_validate (const tk_list_t *head)
{
    tk_list_t *link;
    
    tk_list_foreach (link, head)
	_tk_list_validate (link);
}
static inline tk_bool_t
tk_list_is_empty (const tk_list_t *head);
static inline void
tk_list_validate_is_empty (const tk_list_t *head)
{
    assert (head->next == NULL || (tk_list_is_empty (head) && head->next == head->prev));
}
#else
#define _tk_list_validate(link)
#define tk_list_validate(head)
#define tk_list_validate_is_empty(head)
#endif

static inline void
tk_list_init (tk_list_t *entry)
{
    entry->next = entry;
    entry->prev = entry;
}

static inline void
__tk_list_add (tk_list_t *entry,
                  tk_list_t *prev,
                  tk_list_t *next)
{
    next->prev = entry;
    entry->next = next;
    entry->prev = prev;
    prev->next = entry;
}

static inline void
tk_list_add (tk_list_t *entry, tk_list_t *head)
{
    tk_list_validate (head);
    tk_list_validate_is_empty (entry);
    __tk_list_add (entry, head, head->next);
    tk_list_validate (head);
}

static inline void
tk_list_add_tail (tk_list_t *entry, tk_list_t *head)
{
    tk_list_validate (head);
    tk_list_validate_is_empty (entry);
    __tk_list_add (entry, head->prev, head);
    tk_list_validate (head);
}

static inline void
__tk_list_del (tk_list_t *prev, tk_list_t *next)
{
    next->prev = prev;
    prev->next = next;
}

static inline void
tk_list_del (tk_list_t *entry)
{
    __tk_list_del (entry->prev, entry->next);
    tk_list_init (entry);
}

static inline void
tk_list_move (tk_list_t *entry, tk_list_t *head)
{
    tk_list_validate (head);
    __tk_list_del (entry->prev, entry->next);
    __tk_list_add (entry, head, head->next);
    tk_list_validate (head);
}

static inline void
tk_list_move_tail (tk_list_t *entry, tk_list_t *head)
{
    tk_list_validate (head);
    __tk_list_del (entry->prev, entry->next);
    __tk_list_add (entry, head->prev, head);
    tk_list_validate (head);
}

static inline void
tk_list_swap (tk_list_t *entry, tk_list_t *other)
{
    __tk_list_add (entry, other->prev, other->next);
    tk_list_init (other);
}

static inline tk_bool_t
tk_list_is_first (const tk_list_t *entry,
                     const tk_list_t *head)
{
    tk_list_validate (head);
    return entry->prev == head;
}

static inline tk_bool_t
tk_list_is_last (const tk_list_t *entry,
                    const tk_list_t *head)
{
    tk_list_validate (head);
    return entry->next == head;
}

static inline tk_bool_t
tk_list_is_empty (const tk_list_t *head)
{
    tk_list_validate (head);
    return head->next == head;
}

static inline tk_bool_t
tk_list_is_singular (const tk_list_t *head)
{
    tk_list_validate (head);
    return head->next == head || head->next == head->prev;
}


#endif

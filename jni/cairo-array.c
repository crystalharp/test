
#include "cairoint.h"

/**
 * _cairo_array_init:
 *
 * Initialize a new #cairo_array_t object to store objects each of size
 * @element_size.
 *
 * The #cairo_array_t object provides grow-by-doubling storage. It
 * never interprets the data passed to it, nor does it provide any
 * sort of callback mechanism for freeing resources held onto by
 * stored objects.
 *
 * When finished using the array, _cairo_array_fini() should be
 * called to free resources allocated during use of the array.
 **/
void
_cairo_array_init (cairo_array_t *array, int element_size)
{
    array->size = 0;
    array->num_elements = 0;
    array->element_size = element_size;
    array->elements = NULL;

    array->is_snapshot = FALSE;
}

/**
 * _cairo_user_data_array_init:
 * @array: a #cairo_user_data_array_t
 *
 * Initializes a #cairo_user_data_array_t structure for future
 * use. After initialization, the array has no keys. Call
 * _cairo_user_data_array_fini() to free any allocated memory
 * when done using the array.
 **/
void
_cairo_user_data_array_init (cairo_user_data_array_t *array)
{
    _cairo_array_init (array, sizeof (cairo_user_data_slot_t));
}

/**
 * _cairo_array_fini:
 * @array: A #cairo_array_t
 *
 * Free all resources associated with @array. After this call, @array
 * should not be used again without a subsequent call to
 * _cairo_array_init() again first.
 **/
void
_cairo_array_fini (cairo_array_t *array)
{
    if (array->is_snapshot)
	return;

    if (array->elements) {
	free (* array->elements);
	free (array->elements);
    }
}

/**
 * _cairo_array_index:
 * @array: a #cairo_array_t
 * Returns: A pointer to the object stored at @index.
 *
 * If the resulting value is assigned to a pointer to an object of the same
 * element_size as initially passed to _cairo_array_init() then that
 * pointer may be used for further direct indexing with []. For
 * example:
 *
 * <informalexample><programlisting>
 *	cairo_array_t array;
 *	double *values;
 *
 *	_cairo_array_init (&array, sizeof(double));
 *	... calls to _cairo_array_append() here ...
 *
 *	values = _cairo_array_index (&array, 0);
 *      for (i = 0; i < _cairo_array_num_elements (&array); i++)
 *	    ... use values[i] here ...
 * </programlisting></informalexample>
 **/
void *
_cairo_array_index (cairo_array_t *array, unsigned int index)
{
    /* We allow an index of 0 for the no-elements case.
     * This makes for cleaner calling code which will often look like:
     *
     *    elements = _cairo_array_index (array, num_elements);
     *	  for (i=0; i < num_elements; i++) {
     *        ... use elements[i] here ...
     *    }
     *
     * which in the num_elements==0 case gets the NULL pointer here,
     * but never dereferences it.
     */
    if (index == 0 && array->num_elements == 0)
	return NULL;

    assert (index < array->num_elements);

    return (void *) &(*array->elements)[index * array->element_size];
}

/**
 * _cairo_user_data_array_fini:
 * @array: a #cairo_user_data_array_t
 *
 * Destroys all current keys in the user data array and deallocates
 * any memory allocated for the array itself.
 **/
void
_cairo_user_data_array_fini (cairo_user_data_array_t *array)
{
    unsigned int num_slots;

    num_slots = array->num_elements;
 /*   if (num_slots) {
	cairo_user_data_slot_t *slots;

	slots = _cairo_array_index (array, 0);
	do {
	    if (slots->user_data != NULL && slots->destroy != NULL)
		slots->destroy (slots->user_data);
	    slots++;
	} while (--num_slots);
    } */

    _cairo_array_fini (array);
}


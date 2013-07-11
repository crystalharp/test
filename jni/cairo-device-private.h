
#ifndef _CAIRO_DEVICE_PRIVATE_H_
#define _CAIRO_DEVICE_PRIVATE_H_

struct _cairo_device {
    cairo_reference_count_t ref_count;
    cairo_status_t status;
    cairo_user_data_array_t user_data;

    const cairo_device_backend_t *backend;

    cairo_recursive_mutex_t mutex;
    unsigned mutex_depth;

    cairo_bool_t finished;
};

#endif

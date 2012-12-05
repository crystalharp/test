package com.tigerknows.maps;

import com.tigerknows.TKApplication;

public class Ca {
    
    static {
        //System.loadLibrary("tkengine");
        System.load(TKApplication.getInstance().getFilesDir().getParent() + "/lib/libtkengine.so");
    }
    
    /**初始化引擎、销毁引擎、reset引擎、计算bmpBuf大小*/
    private static native int a(String resdir, String mapdir, int w, int h, byte[] buf, int need_opt);
    protected static int tk_init_engine(String resdir, String mapdir, int w, int h, byte[] buf, int need_opt) { return a(resdir, mapdir, w, h, buf, need_opt); }
    private static native void b(int w, int h, byte[] buf);
    protected static void tk_reset_matrix_size(int w, int h, byte[] buf) { b(w, h, buf); }
    private static native void c();
    protected static void tk_destroy_engine() { c(); }
    private static native int d(int w, int h);
    protected static int tk_get_matrix_size(int w, int h) { return d(w, h); }
    
    /**获取当前屏幕图片及缺失的数据*/
    private static native byte[] e();
    protected static byte[] tk_get_lost_tile_info() { return e(); }
    /**获取当前屏幕图片的文字*/
    private static native byte[] ac();
    protected static byte[] get_map_text_info() { return ac(); }
    
    /**获取当前屏幕TILE图片*/
    private static native int ae(int x, int y, int z);
    protected static int tk_get_tile_buffer(int x, int y, int z) { return ae(x, y, z); }
    
    /**地图移动*/
    private static native void f(double lat, double lon, int zoom);
    protected static void tk_move_latlonzoom(double lat, double lon, int zoom) { f(lat, lon, zoom); }
    private static native void g(int dx, int dy);
    protected static void tk_move_delta(int dx, int dy) { g(dx, dy); }
    
    /**地图缩放*/
    private static native int h();
    protected static int tk_zoom_in() { return h(); }
    private static native int i();
    protected static int tk_zoom_out() { return i(); }
    private static native int j();
    protected static int tk_get_zoom() { return j(); }
    private static native int ja(int zoomLevel);
    protected static int tk_set_zoom(int zoomLevel) { return ja(zoomLevel); }

    
    /**经纬度与当前屏幕坐标之间的转换*/
    private static native byte[] k(int x, int y);
    protected static byte[] tk_scr2latlon(int x, int y) { return k(x, y); }
    private static native byte[] l(double lat, double lon);
    protected static byte[] tk_latlon2scr(double lat, double lon) { return l(lat, lon); }

    /**根据经纬度计算cityId、获得当前屏幕中心点cityId*/
    private static native int m(double lat, double lon);
    protected static int tk_get_city_id(double lat, double lon) { return m(lat, lon); }
    private static native int n();
    protected static int tk_get_current_city_id() { return n(); }

    /**计算当前纬度、缩放级别的比例尺需要用几个像素表示,例如200km需要20个象素*/
    private static native int o(double lat, int zoom);
    protected static int tk_scale_in_pixels(double lat, int zoom) { return o(lat, zoom); }
    /**得到zoom对应的比例（以m为单位）*/
    private static native int aa(int zoom);
    protected static int tk_scale_in_meters(int zoom) { return aa(zoom); }

    /**联系词解压、销毁、获取联想词、联想词版本*/
    private static native int p(int citycode, String filepath);
    protected static int tk_decompress(int citycode, String filepath) { return p(citycode, filepath); }
    private static native int q(String filepath, String filepathcommon, int citycode);
    protected static int tk_suggestword_init(String filepath, String filepathcommon, int citycode) { return q(filepath, filepathcommon, citycode); }
    private static native int r();
    protected static int get_SWCOUNT() { return r(); }
    private static native byte[] s(byte[] searchword, int type);
    protected static byte[] tk_getwordslist(byte[] searchword, int type) { return s(searchword, type); }
    private static native void t();
    protected static void tk_suggestword_destroy() { t(); }
    private static native int u(String path, int cityCode);
    protected static int tk_getrevision(String path, int cityCode) { return u(path, cityCode); }
    
    /**生成.dat .chk文件、填充缺失数据*/
    private static native int v(String path, int rid);
    protected static int tk_init_region(String path, int rid) { return v(path, rid); }
    private static native int w(int rid, int offset, int len, byte[] buf);
    protected static int tk_write_region(int rid, int offset, int len, byte[] buf) { return w(rid, offset, len, buf); }
    private static native byte[] x(int rid);
    protected static byte[] tk_get_region_stat(int rid) { return x(rid); }
    
    /**元数据信息：省市列表等*/
    private static native byte[] y();
    protected static byte[] tk_get_data_root() { return y(); }
    private static native byte[] ya(int rid);
    protected static byte[] tk_get_region_path(int rid) { return ya(rid); }
    private static native byte[] yb(int rid);
    protected static byte[] tk_get_provname(int rid) { return yb(rid); }
    private static native int yc(String regionName);
    protected static int tk_get_region_id(String regionName) { return yc(regionName); }
    private static native byte[] yd(int rid);
    protected static byte[] tk_get_region_name(int rid) { return yd(rid); }
    private static native byte[] ye(String provinceName);
    protected static byte[] get_citylist(String provinceName) { return ye(provinceName); }
    private static native int yf(String cityName);
    protected static int tk_get_cityid(String cityName) { return yf(cityName); }
    //char *tk_get_region_info(int rid); 例如："中文名字, English name, file size, city id". 
    private static native byte[] yg(int regionId);
    protected static byte[] tk_get_region_info(int regionId) { return yg(regionId); }
    // "中文名字, English name, latitude, longitude, level, 省中文名字, 省英文名字" 
    //例如："北京,beijing,39.90415599,116.397772995,11, 北京,beijing"
    //char *tk_get_city_info(int cityid);
    private static native byte[] yh(int cityId);
    protected static byte[] tk_get_city_info(int cityId) { return yh(cityId); }
    private static native byte[] yi(int cityId);
    protected static byte[] tk_get_regionlist(int cityId) { return yi(cityId); }
    private static native byte[] yj();
    protected static byte[] tk_get_provincelist() { return yj(); }
    private static native byte[] yk();
    protected static byte[] tk_get_eprovincelist() { return yk(); }
    private static native void yl(String cityName);
    protected static void tk_remove_city_data(String cityName) { yl(cityName); }
    private static native byte[] ym(int regionId);
    protected static byte[] tk_get_region_version(int regionId) { return ym(regionId); }
    private static native void yn(int regionId);
    protected static void tk_remove_region_data(int regionId) { yn(regionId); }
    
    private static native byte[] z(double lat, double lon);
    protected static byte[] tk_latlon_transform(double lat, double lon) { return z(lat, lon); }
    
    //获取离屏幕上某坐标点（x,y)最近的POI名称；
    private static native byte[] abl(double x, double y, int zoom, int flag);
    protected static byte[] tk_get_poi_namel(double x, double y, int zoom, int flag) { return abl(x, y, zoom, flag); }
    
    //整体增减地图上的字体大小；
    private static native void yt(float offset);
    protected static void tk_reset_font_size(float offset) { yt(offset); }
    private static native int ys(double lon, double lat, int w, int h, int zoom);
    protected static int tk_get_screen_label(double lon, double lat, int w, int h, int zoom) { return ys(lon, lat, w, h, zoom); }
    
    //整体增减地图上的字体大小；
    private static native void zb(int offset);
    protected static void tk_reset_icon_size(int offset) { zb(offset); }
    
    //移动地图到指定的区域 左上角经纬度: lat0, lon0  右下角经纬度: lat1, lon1 标识该点的图片(大头针等图片)的宽及高 icWidth, icHeight
    private static native int za(double lat0, double lon0, double lat1, double lon1, int icWidth, int icHeight);
    protected static int tk_set_scope(double lat0, double lon0, double lat1, double lon1, int icWidth, int icHeight) { return za(lat0, lon0, lat1, lon1, icWidth, icHeight); }
    
    private static native int zc(byte[] bmpbuf, byte[] pngbuf);
    protected static int bmp2Png(byte[] bmpbuf, byte[] pngbuf) { return zc(bmpbuf, pngbuf); }

    private static native int yw(int icon_num);
    // tk_init_icon_num
    protected static int tk_init_icon_num(int icon_num) { return yw(icon_num); }
    private static native int yu(int icon_id, int w, int h);
    // tk_set_icon
    protected static int tk_set_icon(int icon_id, int w, int h) { return yu(icon_id, w, h); }
    protected static native int yv(double lat, double lon);
    protected static int tk_get_rid_by_point(double lat, double lon) { return yv(lat, lon); }
}

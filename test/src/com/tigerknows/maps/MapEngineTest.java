package com.tigerknows.maps;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;

import com.tigerknows.Sphinx;
import com.tigerknows.maps.MapEngine.CityInfo;
import com.tigerknows.maps.MapEngine.RegionInfo;
import com.tigerknows.model.MapMetaFileDownload;
import com.tigerknows.util.ByteUtil;

public class MapEngineTest extends EngineTest {

    private static final String TAG = "MapEngineTest";

    //测试获得data root
    public void testGetDataRoot() {
        String dataRoot = engine.getDataRoot();
        Log.v(TAG, "dataRoot..........................:" + dataRoot);
        assertEquals(dataRoot, dataRootFolderPath);
    }
    
    public void testGetProvinceList() {
        String[] provinceList = engine.getProvinceList();
        //[全国, 北京, 上海, 天津, 重庆, 广东, 江苏, 浙江, 福建, 山东, 河南, 河北, 四川, 辽宁, 湖北, 湖南, 安徽, 黑龙江, 山西, 广西, 陕西, 江西, 内蒙古, 云南, 吉林, 新疆, 贵州, 甘肃, 海南, 宁夏, 青海, 西藏, 香港, 澳门]
        Log.v(TAG, "province list:" + provinceList);
        assertEquals(provinceList[1], "全国");
    }
    
    public void testGetCityId() {
        int cityId = engine.getCityId("北京");
        Log.v(TAG, "get cityId of beijing:" + cityId);
        assertEquals(cityId, 1);
        
        assertEquals(engine.getCityId("上海"), 2);
        assertEquals(engine.getCityId("全国"), -3);
    }
    
    public void testGetCityInfo() {
        CityInfo cityInfo = engine.getCityInfo(1);
        Log.v(TAG, "city info of 1 北京 beijing 116.397772995 39.90415599 2 1 11 1 \n:" + cityInfo);
        assertEquals(cityInfo.getLatitude(), 39.904156);
        assertEquals(cityInfo.getLongitude(), 116.397773);
        assertEquals(cityInfo.getCName(), "北京");
        assertEquals(cityInfo.getEName(), "beijing");
        assertEquals(cityInfo.getLevel(), 11);
        assertEquals(cityInfo.getCProvinceName(), "北京");
        assertEquals(cityInfo.getEProvinceName(), "beijing");
    }
    
    public void testGetScaleInPixels() {
        int scaleInPixels = engine.getScaleInPixels(39.904156, 11);
        Log.v(TAG, "scaleInPixels:" + scaleInPixels);
    }
    
    public void testGetRegionFileName() {
        int regionId = 109;
        String regionName = engine.getRegionFileName(regionId);
        ///sdcard/tigermap/map/beijing/beijinga.dat
        Log.d(TAG, "region of 109:" + regionName);
        assertEquals(regionName, "/sdcard/tigermap/map/beijing/beijinga.dat");
    }
    
    public void testGetRegionList() {
        String[] regions = engine.getRegionList(1);
        //beijingc, beijingd, beijinge, beijingf, beijinga, beijingb
        Log.d(TAG, "regionlist of beijing:" + regions);
        assertEquals(regions.length, 6);
    }
    
    public void testGetRegionId() {
        String regionName = "quanguo";
        int regionId = engine.getRegionId(regionName);
        Log.v(TAG, "regionid of quanguo:" + regionId);
        assertEquals(regionId, -3);
    }
    
    public void testGetRegionInfo() {
        int regionId = 109;
        RegionInfo regionInfo = engine.getRegionInfo(regionId);
        //cName: 北京四环内市区     eName: beijinga     fileSize: 776950    cCityName北京
        Log.d(TAG, "region info of 109" + regionInfo);
        assertEquals(regionInfo.getFileSize(), 776950);
    }
    
    public void testGetRegionMapInfo() {
        RegionMapInfo regionMapInfo = engine.getRegionMapInfo(1);
        Log.d(TAG, "region map info:" + regionMapInfo);
    }
}

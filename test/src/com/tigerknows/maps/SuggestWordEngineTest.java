package com.tigerknows.maps;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import com.tigerknows.Sphinx;

import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;

public class SuggestWordEngineTest extends EngineTest{
    
    private static final String TAG = "SuggestWordEngineTest";
    
    public void atestAllSuggest() {
        testPreconditions();
        testDecompressSuggestWordsFile();
        testDecompressDiffSuggestWordsFile();
        testGetWordsList();
    }
    
    public void testGetWordsList() {
        // [北京大学, 北海, 北京站, 北京师范大学, 北苑, 北京电影学院, 北京西站, 北京理工大学, 北京工业大学, 北方工业大学,
        // 北京交通大学, 北方明珠大厦, 北京航空航天大学, 北方佳苑饭店, 北京中医药大学]
        //北京、全国联想词都存在
        // MapEngine.tk_suggestword_init(testFolderPath + "map1/beijing", testFolderPath + "map1/quanguo", 1);
        ArrayList<String> wordsList = engine.getWordsList("北", 0);
        Log.v(TAG, "suggest word********************************* :" + wordsList);
        assertEquals(wordsList.size(), 15);
        assertEquals(wordsList.get(0), "北京大学");
        assertEquals(wordsList.get(1), "北海");
        assertEquals(wordsList.get(2), "北京站");
        wordsList = engine.getWordsList("中山", 2);
        Log.v(TAG, "suggest word :" + wordsList);
        assertEquals(wordsList.get(0), "中山公园");
        assertEquals(wordsList.get(1), "中山医院");
        assertEquals(wordsList.get(2), "中山北大街");
//      assertEquals(engine.getSwVersion(mActivity, 1), 1);
//      assertEquals(engine.getSwVersion(mActivity, 9999), 1);
        engine.suggestwordDestroy();
        //北京、全国联想词都存在 edition 2
        // MapEngine.tk_suggestword_init(testFolderPath + "map2/beijing", testFolderPath + "map2/quanguo", 1);
        wordsList = engine.getWordsList("北", 0);
        Log.v(TAG, "suggest word********************************* :" + wordsList);
        assertEquals(wordsList.size(), 15);
        assertEquals(wordsList.get(0), "北京大学");
        assertEquals(wordsList.get(1), "北海");
        assertEquals(wordsList.get(2), "北京站");
        wordsList = engine.getWordsList("中山", 2);
        Log.v(TAG, "suggest word :" + wordsList);
        assertEquals(wordsList.get(0), "中山公园");
        assertEquals(wordsList.get(1), "中山医院");
        assertEquals(wordsList.get(2), "中山北大街");
//      assertEquals(engine.getSwVersion(mActivity, 1), 1);
//      assertEquals(engine.getSwVersion(mActivity, 9999), 1);
        engine.suggestwordDestroy();
        //全国存在、北京不存在
        clearTempFolder();
        // MapEngine.tk_suggestword_init(tempFolderPath, testFolderPath + "map1/quanguo", 1);
        wordsList = engine.getWordsList("北", 0);
        Log.v(TAG, "suggest word :" + wordsList);
        assertEquals(wordsList.size(), 3);
        assertEquals(wordsList.get(0), "北京华联");
        assertEquals(wordsList.get(1), "北斗手机");
        assertEquals(wordsList.get(2), "北京华联超市");
//      assertEquals(engine.getSwVersion(mActivity, 1), 1);
//      assertEquals(engine.getSwVersion(mActivity, 9999), 0);
        engine.suggestwordDestroy();
        //全国不存在，北京存在
        clearTempFolder();
        // MapEngine.tk_suggestword_init(testFolderPath + "map1/beijing", tempFolderPath, 1);
        wordsList = engine.getWordsList("北", 0);
        Log.v(TAG, "suggest word :" + wordsList);
        assertEquals(wordsList.size(), 15);
        assertEquals(wordsList.get(0), "北京大学");
        assertEquals(wordsList.get(1), "北海");
        assertEquals(wordsList.get(2), "北京站");
//      assertEquals(engine.getSwVersion(mActivity, 1), 0);
//      assertEquals(engine.getSwVersion(mActivity, 9999), 1);
        engine.suggestwordDestroy();
        //Java接口，前提，北京、全国的联想词已经成功解压
        copySWFiles(testFolderPath + "map1/beijing", "sdcard/tigermap/map/beijing", 1);
        copySWFiles(testFolderPath + "map1/quanguo", "sdcard/tigermap/map/quanguo", 9999);
        engine.suggestwordInit(1);
        wordsList = engine.getWordsList("北", 0);
        Log.v(TAG, "suggest word :" + wordsList);
        assertEquals(wordsList.size(), 15);
        assertEquals(wordsList.get(0), "北京大学");
        assertEquals(wordsList.get(1), "北海");
        assertEquals(wordsList.get(2), "北京站");
//      assertEquals(engine.getSwVersion(mActivity, 1), 1);
//      assertEquals(engine.getSwVersion(mActivity, 9999), 1);
        //需要更换城市
//      assertEquals(engine.getCurrentCitySwVersion(), 1);
        engine.suggestwordDestroy();
        //Java接口，前提，上海、全国的联想词已经成功解压
        copySWFiles(testFolderPath + "map1/shanghai", "sdcard/tigermap/map/shanghai", 2);
        copySWFiles(testFolderPath + "map1/quanguo", "sdcard/tigermap/map/quanguo", 9999);
        engine.suggestwordInit(2);
        wordsList = engine.getWordsList("上", 0);
        Log.v(TAG, "suggest word :" + wordsList);
        assertEquals(wordsList.size(), 15);
        assertEquals(wordsList.get(0), "上海交通大学");
        assertEquals(wordsList.get(1), "上大");
        assertEquals(wordsList.get(2), "上海火车站");
//      assertEquals(engine.getSwVersion(mActivity, 1), 2);
//      assertEquals(engine.getSwVersion(mActivity, 9999), 2);
//      assertEquals(engine.getCurrentCitySwVersion(), 1);
        engine.suggestwordDestroy();
    }

    //测试联想词解压
    public void testUpdate() {
      //北京、全国联想词都存在
        // MapEngine.tk_suggestword_init(testFolderPath + "map1/beijing", testFolderPath + "map1/quanguo", 1);
        ArrayList<String> wordsList = engine.getWordsList("北", 0);
        Log.v(TAG, "suggest word********************************* :" + wordsList);
        assertEquals(wordsList.size(), 15);
        assertEquals(wordsList.get(0), "北京大学");
        assertEquals(wordsList.get(1), "北海");
        assertEquals(wordsList.get(2), "北京站");
        wordsList = engine.getWordsList("中山", 2);
        Log.v(TAG, "suggest word :" + wordsList);
        assertEquals(wordsList.get(0), "中山公园");
        assertEquals(wordsList.get(1), "中山医院");
        assertEquals(wordsList.get(2), "中山北大街");
//      assertEquals(engine.getSwVersion(mActivity, 1), 1);
//      assertEquals(engine.getSwVersion(mActivity, 9999), 1);
        engine.suggestwordDestroy();
      //全国联想词升级
        clearTempFolder();
        copyFile(testFolderPath + "map1_diff_2/quanguo/9999", tempFolderPath + "9999");
        copyFile(testFolderPath + "map1/quanguo/sw_9999_index", tempFolderPath + "sw_9999_index");
        copyFile(testFolderPath + "map1/quanguo/sw_9999_l", tempFolderPath + "sw_9999_l");
        copyFile(testFolderPath + "map1/quanguo/sw_9999_s", tempFolderPath + "sw_9999_s");
        engine.decompressSuggestWordsFile(9999, tempFolderPath);
        engine.suggestwordDestroy();
        assertEquals(compareFileContent(tempFolderPath + "sw_9999_index", testFolderPath + "map2/quanguo/sw_9999_index"), 0);
        assertEquals(compareFileContent(tempFolderPath + "sw_9999_l", testFolderPath + "map2/quanguo/sw_9999_l"), 0);
        assertEquals(compareFileContent(tempFolderPath + "sw_9999_s", testFolderPath + "map2/quanguo/sw_9999_s"), 0);
      //北京、全国联想词都存在
        // MapEngine.tk_suggestword_init(testFolderPath + "map1/beijing", testFolderPath + "map2/quanguo", 1);
        wordsList = engine.getWordsList("北", 0);
        Log.v(TAG, "suggest word********************************* :" + wordsList);
        assertEquals(wordsList.size(), 15);
        assertEquals(wordsList.get(0), "北京大学");
        assertEquals(wordsList.get(1), "北海");
        assertEquals(wordsList.get(2), "北京站");
        wordsList = engine.getWordsList("中山", 2);
        Log.v(TAG, "suggest word :" + wordsList);
        assertEquals(wordsList.get(0), "中山公园");
        assertEquals(wordsList.get(1), "中山医院");
        assertEquals(wordsList.get(2), "中山北大街");
//      assertEquals(engine.getSwVersion(mActivity, 1), 1);
//      assertEquals(engine.getSwVersion(mActivity, 9999), 1);
        engine.suggestwordDestroy();
      //北京联想词升级
        clearTempFolder();
        copyFile(testFolderPath + "map1_diff_2/beijing/1", tempFolderPath + "1");
        copyFile(testFolderPath + "map1/beijing/sw_1_index", tempFolderPath + "sw_1_index");
        copyFile(testFolderPath + "map1/beijing/sw_1_l", tempFolderPath + "sw_1_l");
        copyFile(testFolderPath + "map1/beijing/sw_1_s", tempFolderPath + "sw_1_s");
        engine.decompressSuggestWordsFile(1, tempFolderPath);
        engine.suggestwordDestroy();
        assertEquals(compareFileContent(tempFolderPath + "sw_1_index", testFolderPath + "map2/beijing/sw_1_index"), 0);
        assertEquals(compareFileContent(tempFolderPath + "sw_1_l", testFolderPath + "map2/beijing/sw_1_l"), 0);
        assertEquals(compareFileContent(tempFolderPath + "sw_1_s", testFolderPath + "map2/beijing/sw_1_s"), 0);
        
        //北京、全国联想词都存在
        // MapEngine.tk_suggestword_init(testFolderPath + "map2/beijing", testFolderPath + "map2/quanguo", 1);
        wordsList = engine.getWordsList("北", 0);
        Log.v(TAG, "suggest word********************************* :" + wordsList);
        assertEquals(wordsList.size(), 15);
        assertEquals(wordsList.get(0), "北京大学");
        assertEquals(wordsList.get(1), "北海");
        assertEquals(wordsList.get(2), "北京站");
        wordsList = engine.getWordsList("中山", 2);
        Log.v(TAG, "suggest word :" + wordsList);
        assertEquals(wordsList.get(0), "中山公园");
        assertEquals(wordsList.get(1), "中山医院");
        assertEquals(wordsList.get(2), "中山北大街");
//        assertEquals(engine.getSwVersion(mActivity, 1), 1);
//        assertEquals(engine.getSwVersion(mActivity, 9999), 1);
        engine.suggestwordDestroy();
    }

    //测试联想词解压
    public void testDecompressSuggestWordsFile() {
        //全国联想词
        clearTempFolder();
        copyFile(testFolderPath + "map2/quanguo/9999", tempFolderPath + "9999");
        engine.decompressSuggestWordsFile(9999, tempFolderPath);
        engine.suggestwordDestroy();
        assertEquals(compareFileContent(tempFolderPath + "sw_9999_index", testFolderPath + "map2/quanguo/sw_9999_index"), 0);
        assertEquals(compareFileContent(tempFolderPath + "sw_9999_l", testFolderPath + "map2/quanguo/sw_9999_l"), 0);
        assertEquals(compareFileContent(tempFolderPath + "sw_9999_s", testFolderPath + "map2/quanguo/sw_9999_s"), 0);
        //北京联想词
        clearTempFolder();
        copyFile(testFolderPath + "map2/beijing/1", tempFolderPath + "1");
        engine.decompressSuggestWordsFile(1, tempFolderPath);
        engine.suggestwordDestroy();
        assertEquals(compareFileContent(tempFolderPath + "sw_1_index", testFolderPath + "map2/beijing/sw_1_index"), 0);
        assertEquals(compareFileContent(tempFolderPath + "sw_1_l", testFolderPath + "map2/beijing/sw_1_l"), 0);
        assertEquals(compareFileContent(tempFolderPath + "sw_1_s", testFolderPath + "map2/beijing/sw_1_s"), 0);
    }
    
    //测试联想词升级合并
    public void testDecompressDiffSuggestWordsFile() {
        //全国联想词升级
        clearTempFolder();
        copyFile(testFolderPath + "map1_diff_2/quanguo/9999", tempFolderPath + "9999");
        copyFile(testFolderPath + "map1/quanguo/sw_9999_index", tempFolderPath + "sw_9999_index");
        copyFile(testFolderPath + "map1/quanguo/sw_9999_l", tempFolderPath + "sw_9999_l");
        copyFile(testFolderPath + "map1/quanguo/sw_9999_s", tempFolderPath + "sw_9999_s");
        engine.decompressSuggestWordsFile(9999, tempFolderPath);
        engine.suggestwordDestroy();
        assertEquals(compareFileContent(tempFolderPath + "sw_9999_index", testFolderPath + "map2/quanguo/sw_9999_index"), 0);
        assertEquals(compareFileContent(tempFolderPath + "sw_9999_l", testFolderPath + "map2/quanguo/sw_9999_l"), 0);
        assertEquals(compareFileContent(tempFolderPath + "sw_9999_s", testFolderPath + "map2/quanguo/sw_9999_s"), 0);
        //北京联想词升级
        clearTempFolder();
        copyFile(testFolderPath + "map1_diff_2/beijing/1", tempFolderPath + "1");
        copyFile(testFolderPath + "map1/beijing/sw_1_index", tempFolderPath + "sw_1_index");
        copyFile(testFolderPath + "map1/beijing/sw_1_l", tempFolderPath + "sw_1_l");
        copyFile(testFolderPath + "map1/beijing/sw_1_s", tempFolderPath + "sw_1_s");
        engine.decompressSuggestWordsFile(1, tempFolderPath);
        engine.suggestwordDestroy();
        assertEquals(compareFileContent(tempFolderPath + "sw_1_index", testFolderPath + "map2/beijing/sw_1_index"), 0);
        assertEquals(compareFileContent(tempFolderPath + "sw_1_l", testFolderPath + "map2/beijing/sw_1_l"), 0);
        assertEquals(compareFileContent(tempFolderPath + "sw_1_s", testFolderPath + "map2/beijing/sw_1_s"), 0);
        //全国联想词升级
        clearTempFolder();
        copyFile(testFolderPath + "map1_diff_2/quanguo/9999", tempFolderPath + "9999");
        copyFile(testFolderPath + "map1/quanguo/sw_9999_index", tempFolderPath + "sw_9999_index");
        copyFile(testFolderPath + "map1/quanguo/sw_9999_l", tempFolderPath + "sw_9999_l");
        copyFile(testFolderPath + "map1/quanguo/sw_9999_s", tempFolderPath + "sw_9999_s");
        engine.decompressSuggestWordsFile(9999, tempFolderPath);
        engine.suggestwordDestroy();
        assertEquals(compareFileContent(tempFolderPath + "sw_9999_index", testFolderPath + "map2/quanguo/sw_9999_index"), 0);
        assertEquals(compareFileContent(tempFolderPath + "sw_9999_l", testFolderPath + "map2/quanguo/sw_9999_l"), 0);
        assertEquals(compareFileContent(tempFolderPath + "sw_9999_s", testFolderPath + "map2/quanguo/sw_9999_s"), 0);
    }
    
    //比较两个文件内容是否相同，0表示相同，-1表示不同。
    private int compareFileContent(final String srcFilePath, final String targetFilePath) {
        try {
            FileInputStream fi1 = new FileInputStream(new File(srcFilePath));
            FileInputStream fi2 = new FileInputStream(new File(targetFilePath));
            if (fi1.available() != fi2.available()) {
                fi2.close();
                fi1.close();
                return -1;
            }
            byte[] bytes1 = new byte[fi1.available()];
            fi1.read(bytes1);
            
            byte[] bytes2 = new byte[fi2.available()];
            fi2.read(bytes2);
            
            for (int i=0; i<fi1.available(); i++) {
                if (bytes1[i] != bytes2[i]) {
                    fi2.close();
                    fi1.close();
                    return -1;
                }
            }
            fi2.close();
            fi1.close();
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
        return 0;
    }
    
    private void clearTempFolder() {
        File tempFolder = new File(tempFolderPath);
        File[] files = tempFolder.listFiles();
        for (File file : files) {
            if (!file.getName().startsWith(".")) {
                file.delete();
            }
        }
    }
    
    private int copyFile(final String srcFilePath, final String destFilePath) {
        try {
            FileInputStream fi = new FileInputStream(new File(srcFilePath));
            byte[] bytes = new byte[fi.available()];
            fi.read(bytes);
            
            File destFile = new File(destFilePath);
            if (destFile.exists()) {
                destFile.delete();
            }
            FileOutputStream fo = new FileOutputStream(destFile);
            fo.write(bytes);
            fo.close();
            fi.close();
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        } 
        return 1;
    }
    
    private void copySWFiles(final String srcFolderPath, final String destFolderPath, final int cityCode) {
        copyFile(srcFolderPath + "/sw_" + cityCode + "_index", destFolderPath + "/sw_" + cityCode + "_index");
        copyFile(srcFolderPath + "/sw_" + cityCode + "_l", destFolderPath + "/sw_" + cityCode + "_l");
        copyFile(srcFolderPath + "/sw_" + cityCode + "_s", destFolderPath + "/sw_" + cityCode + "_s");
    }
}

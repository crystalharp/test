/*
 * Copyright (C) pengwenyue@tigerknows.com
 */

package com.tigerknows.model;

import com.decarta.android.exception.APIException;
import com.decarta.android.util.LogWrapper;
import com.tigerknows.TKConfig;
import com.tigerknows.map.MapEngine;
import com.tigerknows.model.response.Appendix;
import com.tigerknows.model.response.DataPackage;
import com.tigerknows.model.response.ResponseCode;
import com.tigerknows.util.ByteUtil;
import com.tigerknows.util.ParserUtil;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * @author Peng Wenyue
 */
public class SuggestLexiconDownload extends BaseQuery {
    
    protected static final String VERSION = "2";

    private static final String TAG = "SuggestLexiconDownload";
    
    public static final String ACTION_SUGGEST_LEXICON_DOWNLOAD_COMPLATE = "action_suggest_lexicon_download_complate";

    public static final String CITY_ID = "city_id";

    private static final int VALIDATE_LENGTH = 30;

    private int currentCityId = -1;

    private int oldversion = 0;

    private int revision = 0;

    private int hasDataLength = 0;

    private byte[] hasData = null;

    private String filePath;

    public int getRevision() {
        return revision;
    }

    // 设置下载城市，并初始化文件目录等信息
    private boolean setupCityId(int cityId) {

        oldversion = 0;
        revision = 0;
        hasDataLength = 0;
        hasData = null;

        currentCityId = cityId;
        filePath = MapEngine.cityId2Floder(currentCityId);
        if (TextUtils.isEmpty(filePath)) {
            return false;
        }

        // 检查是否有未下载完成的数据文件
        File segmentFile = new File(filePath
                + String.format(TKConfig.SUGGEST_LEXCION_FILE_TEMP, currentCityId));
        try {
            if (segmentFile.exists()) {
                hasDataLength = (int)segmentFile.length();
                if (hasDataLength <= VALIDATE_LENGTH) {
                    segmentFile.delete();
                    hasDataLength = 0;
                } else {
                    FileInputStream fis = new FileInputStream(segmentFile);
                    try {
                        fis.skip(hasDataLength - VALIDATE_LENGTH);
                        hasData = new byte[VALIDATE_LENGTH];
                        fis.read(hasData, 0, VALIDATE_LENGTH);
                        byte[] versionData = new byte[8];
                        fis.read(versionData, 0, 8);
                        oldversion = ByteUtil.arr2int(versionData, 0);
                        revision = ByteUtil.arr2int(versionData, 4);
                    } finally {
                        if (null != fis) {
                            try {
                                fis.close();
                            } catch (IOException e) {
                                // Ignore
                                Log.e(TAG, "IOException caught while closing stream", e);
                            }
                        }
                    }
                }
            }

        } catch (IOException e) {
            segmentFile.delete();
            oldversion = 0;
            revision = 0;
            hasDataLength = 0;
            e.printStackTrace();
        }

        // 检查是否有完整的数据文件
        if (hasDataLength == 0) {
            if (!MapEngine.hasCorrectSW(currentCityId)) {
                oldversion = 0;
                revision = 0;
            } else {
                File file = new File(filePath
                        + String.format(TKConfig.SUGGEST_LEXCION_FILE_SW_INDEX,
                                currentCityId));
                try {
                    if (file.exists()) {
                        FileInputStream fis = new FileInputStream(file);
                        try {
                            byte[] versionData = new byte[4];
                            fis.read(versionData, 0, 4);
                            oldversion = ByteUtil.arr2int(versionData, 0);
                        } finally {
                            if (null != fis) {
                                try {
                                    fis.close();
                                } catch (IOException e) {
                                    // Ignore
                                    Log.e(TAG, "IOException caught while closing stream", e);
                                }
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else if (!MapEngine.hasCorrectSW(currentCityId) && oldversion > 0) {
            segmentFile.delete();
            oldversion = 0;
            revision = 0;
            hasDataLength = 0;
        }
        return true;
    }

    public SuggestLexiconDownload(Context context) {
        super(context, API_TYPE_SUGGEST_LEXICON_DOWNLOAD, VERSION);
        this.isTranslatePart = true;
    }

    @Override
    public void query() {
        if (setupCityId(cityId) == false) {
            return;
        }
        super.query();
    }
    
    @Override
    protected void checkRequestParameters() throws APIException {
        addCommonParameters(currentCityId);
        addParameter("lr", String.valueOf(oldversion));
        addParameter("rs", String.valueOf(hasDataLength > 0 ? hasDataLength - VALIDATE_LENGTH : 0));
        if (hasDataLength > 0) {
            addParameter("nr", String.valueOf(revision));
        }
    }
    
    @Override
    protected void createHttpClient() {
        super.createHttpClient();
        httpClient.setIsEncrypt(false);
        httpClient.setURL(String.format(TKConfig.getDownloadSuggestUrl(), TKConfig.getDownloadHost()));
    }

    // 保存下载的数据到文件，然后，如果正常下载完成则调用jni接口解压并初始化引擎
    private void saveData(int responseCode, DataPackage suggest) {

        byte[] data;
        ParserUtil util = suggest.getData();
        int length = util.availableDataleng();

        data = new byte[length];
        System.arraycopy(util.getData(), util.getStart(), data, 0, length);
        
        String zipFilePath = filePath + String.format(TKConfig.SUGGEST_LEXCION_FILE_TEMP, currentCityId);

        // 创建文件
        File file = new File(zipFilePath);
        FileOutputStream fout = null;
        boolean writeSuccess = false;
        try {
            if (file.exists() && hasDataLength == 0) {
                if (!file.delete()) {
                    return;
                }
            }

            if (hasDataLength == 0) {
                if (!file.createNewFile()) {
                    Log.e(TAG, "Unable to create new file: " + zipFilePath);
                    return;
                }
            }

            // 如果已经存在下载的数据，则检查30字节的数据是否与服务器上的一致
            if (hasDataLength > VALIDATE_LENGTH) {
                fout = new FileOutputStream(file, true);
                byte[] temp = new byte[VALIDATE_LENGTH];
                System.arraycopy(data, 0, temp, 0, VALIDATE_LENGTH);
                if (Arrays.equals(hasData, temp)) {
                    fout.write(data, VALIDATE_LENGTH, data.length - VALIDATE_LENGTH);
                    writeSuccess = true;
                }
            } else {
                fout = new FileOutputStream(file);
                fout.write(data);
                writeSuccess = true;
            }
            fout.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != fout) {
                try {
                    fout.close();
                } catch (IOException e) {
                    // Ignore
                    Log.e(TAG, "IOException caught while closing stream", e);
                }
            }
        }

        // 如果下载完成则从文件中读取数据并解压文件
        if (writeSuccess) {
            //判断cityId的联想词是否下载成功
            //下载成功，解压联想词
            String suggestWordsZipFilePath = filePath + currentCityId; 
            File suggestWordsZipFile = new File(suggestWordsZipFilePath);
            MapEngine mapEngine = MapEngine.getInstance();
            if (suggestWordsZipFile.exists()) {
                //解压联想词
                mapEngine.decompress(currentCityId, filePath);
                suggestWordsZipFile.delete(); //以备联想词版本升级
            }
            
            // 前台可以进行联想词库初始化了(调用jni解压文件)
            // 发送联想词下载完成信息。
            mapEngine.suggestDownloadEnd(currentCityId);
        } else {
            file.delete();
        }
    }

    @Override
    protected void translateResponseV12(ParserUtil util) throws IOException {
        super.translateResponseV12(util);
        DataPackage dataPackage = null;
        ResponseCode responseCode = null;
        for (Appendix appendix : appendice) {
            if (appendix.type == Appendix.TYPE_DATA_PACKAGE) {
                dataPackage = (DataPackage)appendix;
            } else if (appendix.type == Appendix.TYPE_RESPONSE_CODE) {
                responseCode = (ResponseCode)appendix;
            }
        }
        LogWrapper.d("SuggestLexiconDownload", "translateResponseV12() currentCityId="+currentCityId+", responseCode="+responseCode.getResponseCode());
        if (responseCode != null) {
            if (responseCode.getResponseCode() == ResponseCode.LEXICON_OK && dataPackage != null) {
                saveData(responseCode.getResponseCode(), dataPackage);
            } else {
                String zipFilePath = filePath + String.format(TKConfig.SUGGEST_LEXCION_FILE_TEMP, currentCityId);
                File file = new File(zipFilePath);
                file.delete();
            }
        }
    }
}

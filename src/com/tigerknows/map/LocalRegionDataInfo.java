package com.tigerknows.map;

import java.util.List;

public class LocalRegionDataInfo {

    private int mTotalSize;
    private int mDownloadedSize;
    private int mLostDataNum;
    private List<TileDownload> mLostDatas;
    
    public LocalRegionDataInfo() {
    }
    
    public LocalRegionDataInfo(int totalSize, int downloadedSize, int lostDataNum, List<TileDownload> lostDatas) {
        mTotalSize = totalSize;
        mDownloadedSize = downloadedSize;
        mLostDataNum = lostDataNum;
        mLostDatas = lostDatas;
    }

    public int getLostDataNum() {
        return mLostDataNum;
    }

    public void setLostDataNum(int lostDataNum) {
        mLostDataNum = lostDataNum;
    }

    public int getTotalSize() {
        return mTotalSize;
    }
    public void setTotalSize(final int totalSize) {
        mTotalSize = totalSize;
    }
    public int getDownloadedSize() {
        return mDownloadedSize;
    }
    public void setDownloadSize(final int downloadedSize) {
        mDownloadedSize = downloadedSize;
    }
    
    public List<TileDownload> getLostDatas() {
        return mLostDatas;
    }
    public void setLostDatas(List<TileDownload> lostDatas) {
        mLostDatas = lostDatas;
    }

    @Override
    public String toString() {
        return "mTotalSize: " + mTotalSize + "\tmDownloadedSize: " + mDownloadedSize
                + "\t mLostDataNum: " + mLostDataNum + "\t lostDatas:" + mLostDatas;
    }

}

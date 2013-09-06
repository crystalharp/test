package com.tigerknows.map;

import java.util.List;

public class LocalRegionDataInfo {

    private int mTotalSize;
    private int mDownloadedSize;
    private int mLostDataNum;
    
    public LocalRegionDataInfo() {
    }
    
    public LocalRegionDataInfo(int totalSize, int downloadedSize, int lostDataNum) {
        mTotalSize = totalSize;
        mDownloadedSize = downloadedSize;
        mLostDataNum = lostDataNum;
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

    @Override
    public String toString() {
        return "mTotalSize: " + mTotalSize + "\tmDownloadedSize: " + mDownloadedSize
                + "\t mLostDataNum: " + mLostDataNum;
    }

}

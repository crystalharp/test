package com.tigerknows;

import com.decarta.Globals;
import com.tigerknows.map.MapEngine;
import com.tigerknows.model.FileDownload;

public class StartUpDelayRunnable implements Runnable{

    Sphinx mSphinx;
    
    public StartUpDelayRunnable(Sphinx sphinx) {
        mSphinx = sphinx;
    }
    
    @Override
    public void run() {
        
        int cityId = Globals.getCurrentCityInfo(mSphinx).getId();
        if (MapEngine.checkSupportSubway(cityId) &&
                "WIFI".equals(TKConfig.getConnectivityType(mSphinx)) &&
                !MapEngine.checkSubwayMapValidity(mSphinx, cityId)) {
            FileDownload fileDownload = new FileDownload(mSphinx);
            fileDownload.addParameter(FileDownload.SERVER_PARAMETER_FILE_TYPE, FileDownload.FILE_TYPE_SUBWAY);
            mSphinx.queryStart(fileDownload);
        }
    }

}

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
        
        if (MapEngine.checkSupportSubway(Globals.getCurrentCityInfo().getId()) &&
                "WIFI".equals(TKConfig.getConnectivityType(mSphinx)) &&
                !MapEngine.checkSubwayMapValidity(mSphinx, Globals.getCurrentCityInfo().getId())) {
            FileDownload fileDownload = new FileDownload(mSphinx);
            fileDownload.addParameter(FileDownload.SERVER_PARAMETER_FILE_TYPE, FileDownload.FILE_TYPE_SUBWAY);
            fileDownload.setup(Globals.getCurrentCityInfo().getId());
            mSphinx.queryStart(fileDownload);
        }
    }

}

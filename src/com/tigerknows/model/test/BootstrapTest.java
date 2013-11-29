package com.tigerknows.model.test;

import com.tigerknows.model.BootstrapModel;
import com.tigerknows.model.BootstrapModel.StartupDisplay;
import com.tigerknows.model.xobject.XArray;
import com.tigerknows.model.xobject.XMap;

public class BootstrapTest extends BaseQueryTest{

    public static XMap launchRespnose() {
        XMap xmap = new XMap();
        XArray<XMap> list = new XArray<XMap>();
        list.add(launchStartupDisplay());
        XMap startupDisplayXMap = new XMap();
        startupDisplayXMap.put(BootstrapModel.FIELD_STARTUP_DISPLAY_LIST, list);
        xmap.put(BootstrapModel.FIELD_STARTUP_DISPLAY, startupDisplayXMap);
        return xmap;
    }    

    public static XMap launchStartupDisplay() {
        XMap xmap = new XMap();
        xmap.put(StartupDisplay.FIELD_URL, PIC_URL);
        xmap.put(StartupDisplay.FIELD_BEGIN, "2013-01-01 01:01:01");
        xmap.put(StartupDisplay.FIELD_END, "3013-01-01 01:01:01");
        return xmap;
    }    
}

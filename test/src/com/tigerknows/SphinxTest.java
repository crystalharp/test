/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tigerknows;

import com.tigerknows.view.menu.MenuBuilder;

import android.app.Dialog;
import android.app.Instrumentation;
import android.content.res.Configuration;
import android.os.SystemClock;
import android.test.SingleLaunchActivityTestCase;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Warning: Don't close menu
 * 
 * Make sure that the main launcher activity opens up properly, which will be
 * verified by {@link ActivityInstrumentationTestCase#testActivityTestCaseSetUpProperly}.
 */
public class SphinxTest extends SingleLaunchActivityTestCase<Sphinx> {
    
    private static String TAG = "SphinxTest";
    private static int ProtraitMenuCountForLine = 3; 
    private static int intervalTime = 3 * 1000;
    private static int waitTimes = 5;

    private static int do_type_click = 0;
    private static int do_type_longclick = 1;
    private static int do_type_focus = 2;

    private Sphinx mSphinx;

    private RelativeLayout mTopView;
    private ImageView mLogoIV;
    private EditText mSearchPOIEdt;
    private Button mQueryTrafficBtn;
    private Button mDiscoverBtn;
    Dialog discoverDialog;
    Dialog menuDialog;

    @Override
    public Sphinx getActivity() {
        // TODO Auto-generated method stub
        return super.getActivity();
    }
    
    @Override
    protected void setUp() throws Exception {
        // TODO Auto-generated method stub
        super.setUp();
        mSphinx = getActivity();
        findViews();
    }
    
    private void findViews() {
        mTopView = (RelativeLayout)mSphinx.findViewById(R.id.sphinx_top);
        mLogoIV = (ImageView)mSphinx.findViewById(R.id.sphinx_logo);
        mSearchPOIEdt = (EditText)mSphinx.findViewById(R.id.sphinx_search_poi);
        mQueryTrafficBtn = (Button)mSphinx.findViewById(R.id.sphinx_query_traffic);
        mDiscoverBtn = (Button)mSphinx.findViewById(R.id.sphinx_discover);
        discoverDialog = mSphinx.getDiscoverDialog();
        menuDialog= mSphinx.getMenuDialog();
    }

    /**
     * The first constructor parameter must refer to the package identifier of the
     * package hosting the activity to be launched, which is specified in the AndroidManifest.xml
     * file.  This is not necessarily the same as the java package name of the class - in fact, in
     * some cases it may not match at all.
     */
    public SphinxTest() {
        super("com.tigerknows", Sphinx.class);
    }
    
    public void testDiscoverViaButton(){
        Instrumentation inst = getInstrumentation();
        waitForViewVisibility(mTopView, 2);
        performDo(inst, mDiscoverBtn, do_type_click);
        
        verifyDiscover(inst);
    }
    
    public void testDiscoverViaMenu(){
        Instrumentation inst = getInstrumentation();
        waitForViewVisibility(mTopView, 2);
        selectedMenuViaIndex(inst, 1);
        
        verifyDiscover(inst);
    }
    
    private void verifyDiscover(Instrumentation inst) {

        Dialog dialog = mSphinx.getDiscoverDialog();
        assertDialog(inst, dialog, waitTimes);
        
        LinearLayout linearLayout = (LinearLayout)dialog.findViewById(R.id.discover_scrollview_body);
        int count = linearLayout.getChildCount();
        pressKeyTimes(inst, KeyEvent.KEYCODE_DPAD_RIGHT, count);
        pressKeyTimes(inst, KeyEvent.KEYCODE_DPAD_CENTER, 1);
        pressKeyList(inst, new int[] {KeyEvent.KEYCODE_W, KeyEvent.KEYCODE_F, KeyEvent.KEYCODE_J, KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_CENTER});
        performItemDo(inst, dialog, R.id.discover_hot_listview, 0, do_type_click);
        Dialog dialog1 = mSphinx.getPOIDetailDialog();
        assertDialog(inst, dialog1, waitTimes);
        CheckBox favorite = (CheckBox)dialog1.findViewById(R.id.poi_detail_favorite); 
        if (!favorite.isChecked()) {
            performDo(inst, favorite, 0);
        }
        performDo(inst, dialog1, R.id.poi_detail_map_btn, do_type_click);
        performDo(inst, mSphinx.findViewById(R.id.map_history), do_type_click);
        assertDialog(inst, dialog, waitTimes);
    }
    
    public void testSearchPOIViaMenu(){
        Instrumentation inst = getInstrumentation();
        waitForViewVisibility(mTopView, 2);
        selectedMenuViaIndex(inst, 0);

        verifySearchPOI(inst);
    }
    
    public void testSearchPOIViaButton(){
        Instrumentation inst = getInstrumentation();
        waitForViewVisibility(mTopView, 2);
        performDo(inst, mSearchPOIEdt, do_type_click);

        verifySearchPOI(inst);
    }
    
    private void verifySearchPOI(Instrumentation inst) {
        Dialog dialog = mSphinx.getSearchPOIDialog();
        assertDialog(inst, dialog, waitTimes);
        performDo(inst, dialog, R.id.search_poi_find_edittext, do_type_focus);
        pressKeyList(inst, new int[] {KeyEvent.KEYCODE_W, KeyEvent.KEYCODE_F, KeyEvent.KEYCODE_J, KeyEvent.KEYCODE_DPAD_CENTER});
        performDo(inst, dialog, R.id.search_poi_button, do_type_click);
        Dialog dialog1 = mSphinx.getSearchResultDialog();
        assertDialog(inst, dialog1, waitTimes);
        performItemDo(inst, dialog1, R.id.search_result_listview, 0, do_type_click);
        Dialog dialog2 = mSphinx.getPOIDetailDialog();
        assertDialog(inst, dialog2, waitTimes);
        performDo(inst, dialog2, R.id.poi_detail_map_btn, do_type_click);
        performDo(inst, mSphinx.findViewById(R.id.map_history), do_type_click);
        assertDialog(inst, dialog1, waitTimes);
        pressKeyTimes(inst, KeyEvent.KEYCODE_BACK, 1);
    }
    
    public void testQueryTrafficViaButton(){
        Instrumentation inst = getInstrumentation();
        waitForViewVisibility(mTopView, 2);
        performDo(inst, mQueryTrafficBtn, do_type_click);
        
        verifyQuerTraffic(inst);
    }
    
    public void testQueryTrafficViaMenu(){
        Instrumentation inst = getInstrumentation();
        waitForViewVisibility(mTopView, 2);
        selectedMenuViaIndex(inst, 2);
        
        verifyQuerTraffic(inst);
    }
    
    public void testQueryBuslineViaButton(){
        Instrumentation inst = getInstrumentation();
        waitForViewVisibility(mTopView, 2);
        performDo(inst, mQueryTrafficBtn, do_type_click);
        
        verifyQuerBusline(inst);
    }
    
    public void testQueryBuslineViaMenu(){
        Instrumentation inst = getInstrumentation();
        waitForViewVisibility(mTopView, 2);
        selectedMenuViaIndex(inst, 2); // R.id.menu_traffic
        
        verifyQuerBusline(inst);
    }
    
    private void verifyQuerTraffic(Instrumentation inst) {
        Dialog dialog = mSphinx.getQueryTrafficDialog();
        assertDialog(inst, dialog, waitTimes);
        performDo(inst, dialog, R.id.query_traffic_start_edittext, do_type_focus);
        pressKeyList(inst, new int[] {KeyEvent.KEYCODE_W, KeyEvent.KEYCODE_F, KeyEvent.KEYCODE_J, KeyEvent.KEYCODE_DPAD_CENTER});
        performDo(inst, dialog, R.id.query_traffic_end_edittext, do_type_focus);
        pressKeyList(inst, new int[] {KeyEvent.KEYCODE_Z, KeyEvent.KEYCODE_G, KeyEvent.KEYCODE_C, KeyEvent.KEYCODE_DPAD_CENTER});
        performDo(inst, dialog, R.id.query_traffic_button, do_type_click);
        Dialog dialog1 = mSphinx.getQuerySelectLocationDialog();
        assertDialog(inst, dialog1, waitTimes);
        performItemDo(inst, dialog1, R.id.query_select_location_start_listview, 0, do_type_click);
        performItemDo(inst, dialog1, R.id.query_select_location_end_listview, 0, do_type_click);
        Dialog dialog2 = mSphinx.getQueryResultDialog();
        assertDialog(inst, dialog2, waitTimes);
        performDo(inst, dialog2, R.id.query_result_drive_raiaobutton, do_type_click);
        waitForViewVisibility(dialog2.findViewById(R.id.query_result_for_drive_listview), waitTimes);
        performItemDo(inst, dialog2, R.id.query_result_for_drive_listview, 0, do_type_click);
        performDo(inst, mSphinx.findViewById(R.id.map_history), do_type_click);
        assertDialog(inst, dialog2, waitTimes);
        pressKeyTimes(inst, KeyEvent.KEYCODE_BACK, 1);
    }
    
    private void verifyQuerBusline(Instrumentation inst) {
        Dialog dialog = mSphinx.getQueryTrafficDialog();
        assertDialog(inst, dialog, waitTimes);
        
        performDo(inst, dialog, R.id.tabhost, do_type_focus);
        pressKeyList(inst, new int[] {KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_CENTER});
        
        performDo(inst, dialog, R.id.query_traffic_busline_edittext, do_type_focus);
        pressKeyList(inst, new int[] {KeyEvent.KEYCODE_1, KeyEvent.KEYCODE_DPAD_CENTER});
        
        performDo(inst, dialog, R.id.query_traffic_busline_button, do_type_click);
        
        Dialog dialog2 = mSphinx.getQueryResultDialog();
        assertDialog(inst, dialog2, waitTimes);
        waitForViewVisibility(dialog2.findViewById(R.id.query_result_for_bus_listview), waitTimes);
        performItemDo(inst, dialog2, R.id.query_result_for_bus_listview, 0, do_type_click);
        
        selectedMenuViaIndex(inst, 0);

        performDo(inst, mSphinx.findViewById(R.id.map_history), do_type_click);
        assertDialog(inst, dialog2, waitTimes);
        pressKeyTimes(inst, KeyEvent.KEYCODE_BACK, 1);
    }
    
    public void testFavoriteViaMenu(){
        Instrumentation inst = getInstrumentation();
        waitForViewVisibility(mTopView, 2);
        selectedMenuViaIndex(inst, 3);
        Dialog dialog = mSphinx.getFavoriteDialog();
        assertDialog(inst, dialog, waitTimes);
        
        performDo(inst, dialog, R.id.tabhost, do_type_focus);
        pressKeyList(inst, new int[] {KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_CENTER});
        performItemDo(inst, dialog, R.id.favorite_poi_listview, 0, do_type_click);
        
        Dialog dialog1 = mSphinx.getPOIDetailDialog();
        assertDialog(inst, dialog1, waitTimes);
        performDo(inst, dialog1, R.id.poi_detail_map_btn, do_type_click);
        
        performDo(inst, mSphinx.findViewById(R.id.map_history), do_type_click);
        assertDialog(inst, dialog, waitTimes);
        
        performDo(inst, dialog, R.id.tabhost, 0);
        pressKeyList(inst, new int[] {KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_CENTER});
        performItemDo(inst, dialog, R.id.favorite_traffic_listview, 0, do_type_click);
        Dialog dialog2 = mSphinx.getQueryResultDialog();
        assertDialog(inst, dialog2, waitTimes);
    }
    
    public void testMoreViaMenu(){
        Instrumentation inst = getInstrumentation();
        
        waitForViewVisibility(mTopView, 2);
        selectedMenuViaIndex(inst, 5);
        Dialog dialog = mSphinx.getMoreDialog();
        assertDialog(inst, dialog, waitTimes);

        performItemDo(inst, dialog, R.id.listview, 0, do_type_click);
        assertDialog(inst, mSphinx.getSettingsDialog(), waitTimes);
        pressKeyTimes(inst, KeyEvent.KEYCODE_BACK, 1);
        assertDialog(inst, dialog, waitTimes);

        performItemDo(inst, dialog, R.id.listview, 1, do_type_click);
        assertDialog(inst, mSphinx.getSelectCityDialog(), waitTimes);
        pressKeyTimes(inst, KeyEvent.KEYCODE_BACK, 1);
        assertDialog(inst, dialog, waitTimes);

        performItemDo(inst, dialog, R.id.listview, 2, do_type_click);
        assertDialog(inst, mSphinx.getMapDownloadDialog(), waitTimes);
        pressKeyTimes(inst, KeyEvent.KEYCODE_BACK, 1);
        assertDialog(inst, dialog, waitTimes);

        performItemDo(inst, dialog, R.id.listview, 4, do_type_click);
        assertDialog(inst, mSphinx.getHistoryResultDialog(), waitTimes);
//        performItemClick(inst, mSphinx.getHistoryResultDialog(), R.id.history_result_listview, 0);
        pressKeyTimes(inst, KeyEvent.KEYCODE_BACK, 1);
        assertDialog(inst, dialog, waitTimes);

        performItemDo(inst, dialog, R.id.listview, 5, do_type_click);
        assertDialog(inst, mSphinx.getCompassDialog(), waitTimes);
        pressKeyTimes(inst, KeyEvent.KEYCODE_BACK, 1);
        assertDialog(inst, dialog, waitTimes);

        performItemDo(inst, dialog, R.id.listview, 7, do_type_click);
        assertDialog(inst, mSphinx.getFeedbackDialog(), waitTimes);
        pressKeyTimes(inst, KeyEvent.KEYCODE_BACK, 1);
        assertDialog(inst, dialog, waitTimes);

        performItemDo(inst, dialog, R.id.listview, 9, do_type_click);
        assertDialog(inst, mSphinx.getHelpDialog(), waitTimes);
        pressKeyTimes(inst, KeyEvent.KEYCODE_BACK, 1);
        assertDialog(inst, dialog, waitTimes);

        performItemDo(inst, dialog, R.id.listview, 10, do_type_click);
        assertDialog(inst, mSphinx.getAboutDialog(), waitTimes);
        pressKeyTimes(inst, KeyEvent.KEYCODE_BACK, 1);
        assertDialog(inst, dialog, waitTimes);
        pressKeyTimes(inst, KeyEvent.KEYCODE_BACK, 1);
    }
    
    private void performItemDo(Instrumentation inst, final Dialog dialog, final int id, final int index, final int type) {
        inst.runOnMainSync(new Runnable() {  
            
            @Override  
            public void run() {
                ListView listView = (ListView)dialog.findViewById(id);
                if (type == 0) {
                    listView.performItemClick(null, index, -1);
                } else {
                    listView.performLongClick();
                }
            }  
        });
        SystemClock.sleep(intervalTime);
    }
    
    private void performDo(Instrumentation inst, final View view, final int type) {
        inst.runOnMainSync(new Runnable() {  
            
            @Override  
            public void run() { 
                if (type == 0) {
                    view.performClick();
                } else if (type == 1) {
                    view.performLongClick();
                } else {
                    view.requestFocus();
                }
            }  
        });
        SystemClock.sleep(intervalTime);
    }
    
    private void performDo(Instrumentation inst, final Dialog dialog, final int id, final int type) {
        View view = dialog.findViewById(id);
        performDo(inst, view, type);
    }

    private void pressKeyList(Instrumentation inst, int[] keycodes) {
        for (int keycode : keycodes) {
            inst.sendKeyDownUpSync(keycode);
        }
        SystemClock.sleep(intervalTime);
    }

    private void pressKeyTimes(Instrumentation inst, int keycode, int times) {
        for(int i = 0; i < times; i++) {
            inst.sendKeyDownUpSync(keycode);
        }
        SystemClock.sleep(intervalTime);
    }
    
    private void selectedMenuViaIndex(Instrumentation inst, final int index) {

        boolean isLand = (mSphinx.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE);
        pressKeyList(inst, new int[] {KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_UP});
        if (isLand == false && index >= ProtraitMenuCountForLine) {
            inst.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_DOWN);
            pressKeyTimes(inst, KeyEvent.KEYCODE_DPAD_RIGHT, index % ProtraitMenuCountForLine);
        } else {
            pressKeyTimes(inst, KeyEvent.KEYCODE_DPAD_RIGHT, index);
        }
        inst.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_CENTER);
        
        final Dialog dialog = mSphinx.getMenuDialog();        
        inst.runOnMainSync(new Runnable() {
            
            @Override
            public void run() {
                dialog.dismiss();
            }
        });
        SystemClock.sleep(intervalTime);
    }
    
    private void assertDialog(Instrumentation inst, final Dialog dialog, int timeout) {
        try {
            boolean expected = waitForDialogShowing(dialog, timeout);
            assertEquals("The dialog is showing", expected, true);
        } catch (Exception e){
            assertTrue("Fails to dialog showing", false);
            Log.v(TAG, e.toString());
        }
        SystemClock.sleep(intervalTime);
    }
    
    public boolean waitForViewVisibility(View view, int timeout) {
        try {
            int times = 0;
            while (view.getVisibility() != View.VISIBLE && times < timeout) {
                Thread.sleep(intervalTime);
                times++;
            }
        } catch (Exception e){
            assertTrue("Fails to view visibility", false);
            Log.v(TAG, e.toString());
        }
        return view.getVisibility() == View.VISIBLE;
    }
    
    public boolean waitForDialogShowing(Dialog dialog, int timeout) {
        try {
            int times = 0;
            while (!dialog.isShowing() && times < timeout) {
                Thread.sleep(intervalTime);
                times++;
            }
        } catch (Exception e){
            assertTrue("Fails to show dialog", false);
            Log.v(TAG, e.toString());
        }
        return dialog.isShowing();
    }

    private void selectedMenuViaId(Instrumentation inst, final MenuBuilder menuBuilder, final int id) {

        inst.runOnMainSync(new Runnable() {  
            
            @Override  
            public void run() {
                menuBuilder.performIdentifierAction(id, 0);
            }  
        });
    }
    
    private Object getFieldViaReflect(Class targetClass, Object targetObject, String fieldName) {
        Object object = null;
        try {
            Field field = targetClass.getDeclaredField(fieldName);
            field.setAccessible(true);
            try {
                object = field.get(targetObject);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        return object;
    }
    
    private Method getMethodViaReflect(Class targetClass, Object targetObject, String methodName, Class... parameterTypes) {
        Method method = null;
        try {
            method = targetClass.getDeclaredMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        method.setAccessible(true);
        return method;
    }
    
    private Object invokeMethodViaReflect(Class targetClass, Object targetObject, Method method, Object... args) {
        Object returnValue = null;
        try {
            returnValue = method.invoke(targetObject, args);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return returnValue;
    }
}

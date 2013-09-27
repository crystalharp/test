package com.tigerknows.ui.traffic;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings.ZoomDensity;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.decarta.Globals;
import com.decarta.android.location.Position;
import com.decarta.android.util.LogWrapper;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.TKConfig;
import com.tigerknows.map.MapEngine;
import com.tigerknows.map.MapEngine.CityInfo;
import com.tigerknows.model.LocationQuery;
import com.tigerknows.model.POI;
import com.tigerknows.ui.BaseFragment;
import com.tigerknows.util.Utility;

public class SubwayMapFragment extends BaseFragment {

    WebView mWebWbv;
    String mTitle;
    String mURL;
    String mBaseURL;
    
    static final String TAG = "SubwayMapFragment";
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mRootView = mLayoutInflater.inflate(R.layout.traffic_subway, container, false);
        
        findViews();
        setListener();
        
        return mRootView;

    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        LogWrapper.d(TAG, "mURL="+mURL);
        //FIXME:临时用来解压的，需要加各种判断
//        AssetManager am = mSphinx.getAssets();
//        Utility.unZipFile(am, "beijing-sw.zip", TKConfig.getDataPath(false));
        mTitleBtn.setText(mTitle);
        
        LocationQuery locationQuery = LocationQuery.getInstance(mSphinx);
        Location location = locationQuery.getLocation();
        String s_pos = "";
        if (location != null) {
            MapEngine mapEngine = MapEngine.getInstance();
            Position pos = mapEngine.latlonTransform(new Position(location.getLatitude(), location.getLongitude()));
            double lx = pos.getLon();
            double ly = pos.getLat();
            s_pos = "lx=" + lx + "&ly=" + ly;
        }
        mURL += s_pos;
        
        mWebWbv.loadUrl(mURL);
    }

    public SubwayMapFragment(Sphinx sphinx) {
        super(sphinx);
    }

    private void findViews() {
        mWebWbv = (WebView)mRootView.findViewById(R.id.web_wbv);
        //TODO:还有其他的各种转圈的view
    }
    
    private void setListener() {
        mWebWbv.addJavascriptInterface(new StationHandler(), "station");
        mWebWbv.getSettings().setJavaScriptEnabled(true);
        mWebWbv.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                mWebWbv.loadUrl("javascript:is_android()");
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                mWebWbv.getSettings().setBuiltInZoomControls(true); //显示放大缩小 controler
                mWebWbv.getSettings().setSupportZoom(true); //可以缩放
                mWebWbv.getSettings().setDefaultZoom(ZoomDensity.FAR);//默认缩放模式
                mWebWbv.getSettings().setUseWideViewPort(true);
                mWebWbv.invokeZoomPicker();
            }
            
        });
    }
    
    public void setData(CityInfo cityinfo) {
        //TODO:获取url 
        mWebWbv.stopLoading();
        mWebWbv.clearView();
        mTitle = "地铁图";
//        mURL = "file:///sdcard/" + cityinfo.getEName() + "-sw/index.html?";
        mURL = "file://" + TKConfig.getDataPath(false) + cityinfo.getEName() + "-sw/index.html?";
//        mFinishedUrl = null;
//        mLastUrl = null;
        LogWrapper.d(TAG, "city:" + cityinfo.getEName());

    }
    
    class StationHandler {
        public void show(final String poiid) {
            Runnable run = new Runnable() {

                @Override
                public void run() {
                    POI poi = new POI();
                    poi.setUUID(poiid);
                    poi.ciytId =  Globals.getCurrentCityInfo().getId();
                    mSphinx.getPOIDetailFragment().setData(poi);
                    mSphinx.showView(R.id.view_poi_detail);
                }
            };
            mSphinx.runOnUiThread(run);
        }
        
        public void search(final String poiid, final String x, final String y, final String name) {
            //FIXME:检查参数
            LogWrapper.d(TAG, "x:" + x + " y:" + y);
            Runnable run = new Runnable() {

                @Override
                public void run() {
                    POI poi = new POI();
                    Position pos = new Position(Double.parseDouble(y), Double.parseDouble(x));
                    poi.setUUID(poiid);
                    poi.setName(name);
                    poi.setPosition(pos);
                    mSphinx.getPOINearbyFragment().setData(poi);
                    mSphinx.showView(R.id.view_poi_nearby_search);
                }
                
            };
            mSphinx.runOnUiThread(run);
        }
    }
}

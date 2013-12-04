package com.tigerknows.ui.traffic;

import java.io.File;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebSettings.ZoomDensity;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.decarta.Globals;
import com.decarta.android.util.LogWrapper;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.TKConfig;
import com.tigerknows.android.app.TKActivity;
import com.tigerknows.android.location.Position;
import com.tigerknows.android.os.TKAsyncTask;
import com.tigerknows.common.ActionLog;
import com.tigerknows.map.MapEngine;
import com.tigerknows.map.MapEngine.CityInfo;
import com.tigerknows.model.FileDownload;
import com.tigerknows.model.POI;
import com.tigerknows.model.Response;
import com.tigerknows.ui.BaseActivity;
import com.tigerknows.ui.BaseFragment;
import com.tigerknows.widget.QueryingView;
import com.tigerknows.widget.RetryView;

public class SubwayMapFragment extends BaseFragment implements RetryView.CallBack {

    WebView mWebWbv;
    String mTitle;
    String mURL;
    String subwayPath;
    CityInfo mCityInfo;
    Position mPos;
    boolean needRefresh;
    int mOriginStat;
    
    RetryView mRetryView;
    QueryingView mQueryingView;
    View mEmptyView;
    ImageView mEmptyImg;
    TextView mEmptyTxv;
    
    static final String TAG = "SubwayMapFragment";
    
    static final int STAT_MAP = 0;
    static final int STAT_QUERY = 1;
    static final int STAT_NODATA = 2;
    static final int STAT_QUERY_FAILED = 3;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActionTag = ActionLog.TrafficSubwayMap;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mRootView = mLayoutInflater.inflate(R.layout.traffic_subway, container, false);
        
        findViews();
        setListener();
        
        mWebWbv.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        return mRootView;

    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        
        mTitleBtn.setText(mTitle);
        if (!mDismissed) {
            return;
        }
        subwayPath = MapEngine.getSubwayDataPath(mSphinx, mCityInfo.getId());
        LogWrapper.d(TAG, "subway path:" + subwayPath);
        if (subwayPath == null) {
            setStatus(STAT_QUERY);
        } else {
            String subwayUpdated = TKConfig.getPref(mSphinx, TKConfig.getSubwayMapUpdatedPrefs(mCityInfo.getId()), "");
            if (!TextUtils.isEmpty(subwayUpdated)) {
                mSphinx.showTip(R.string.subway_map_updated, Toast.LENGTH_SHORT);
                TKConfig.setPref(mSphinx, TKConfig.getSubwayMapUpdatedPrefs(mCityInfo.getId()), "");
                mWebWbv.clearCache(false);
            }
            
            setStatus(STAT_MAP);
            mURL = Uri.fromFile(new File(subwayPath)).toString();
            showSubwayMap(mURL);
        }
        FileDownload fileDownload = new FileDownload(mSphinx);
        fileDownload.addParameter(FileDownload.SERVER_PARAMETER_FILE_TYPE, FileDownload.FILE_TYPE_SUBWAY);
        fileDownload.setup(mCityInfo.getId(), this.getId(), this.getId());
        mSphinx.queryStart(fileDownload);
    }

    public SubwayMapFragment(Sphinx sphinx) {
        super(sphinx);
    }

    private void findViews() {
        mWebWbv = (WebView)mRootView.findViewById(R.id.web_wbv);
        mRetryView = (RetryView) mRootView.findViewById(R.id.retry_view);
        mQueryingView = (QueryingView)mRootView.findViewById(R.id.querying_view);
        mEmptyView = mRootView.findViewById(R.id.empty_view);
        mEmptyTxv = (TextView) mEmptyView.findViewById(R.id.empty_txv);
        mEmptyImg = (ImageView) mEmptyView.findViewById(R.id.icon_imv);
        mEmptyTxv.setText(mSphinx.getString(R.string.no_subway_map));
        
        mQueryingView.setText(R.string.loading_subway_map);
        mRetryView.setCallBack(this, mActionTag);
        mEmptyImg.setBackgroundResource(R.drawable.bg_no_subway);
    }
    
    private void showSubwayMap(String url) {
        if (mPos != null) {
            String s_params = "?";
            double lx = mPos.getLon();
            double ly = mPos.getLat();
            s_params += "lx=" + lx + "&ly=" + ly;
            url += s_params;
            mPos = null;
        }
        LogWrapper.d(TAG, "subway path="+url);

        if (needRefresh) {
            mWebWbv.loadUrl(url);
            needRefresh = false;
        }
    }
    
    private void setStatus(int stat) {
        mOriginStat = stat;
        switch (stat) {
        case STAT_MAP:
            mWebWbv.setVisibility(View.VISIBLE);
            mQueryingView.setVisibility(View.GONE);
            mEmptyView.setVisibility(View.GONE);
            mTitle = mCityInfo.getCName() + mTitle;
            mRetryView.setVisibility(View.GONE);
            break;
        case STAT_QUERY:
            mWebWbv.setVisibility(View.GONE);
            mQueryingView.setVisibility(View.VISIBLE);
            mEmptyView.setVisibility(View.GONE);
            mRetryView.setVisibility(View.GONE);
            break;
        case STAT_NODATA:
            mWebWbv.setVisibility(View.GONE);
            mQueryingView.setVisibility(View.GONE);
            mEmptyView.setVisibility(View.VISIBLE);
            mRetryView.setVisibility(View.GONE);
            break;
        case STAT_QUERY_FAILED:
            mWebWbv.setVisibility(View.GONE);
            mQueryingView.setVisibility(View.GONE);
            mEmptyView.setVisibility(View.GONE);
            mRetryView.setVisibility(View.VISIBLE);
            break;
        }
        mTitleBtn.setText(mTitle);
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
        needRefresh = true;
        mWebWbv.stopLoading();
        mWebWbv.clearView();
        mTitle = mSphinx.getString(R.string.subway_map);
        mCityInfo = cityinfo;
        CityInfo locateCityInfo = Globals.g_My_Location_City_Info;
        if (locateCityInfo != null && locateCityInfo.getId() == mCityInfo.getId()) {
            mPos = locateCityInfo.getPosition();
        } else {
            mPos = null;
        }
        LogWrapper.d(TAG, "city:" + cityinfo.getEName());

    }
    
    public class StationHandler {
        public void show(final String poiid) {
            mActionLog.addAction(mActionTag + ActionLog.TrafficSubwayMapClickPOI);
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
            LogWrapper.d(TAG, "x:" + x + " y:" + y);
            mActionLog.addAction(mActionTag + ActionLog.TrafficSubwayMapClickSearch);
            if ((TextUtils.isEmpty(x) || x.length() < 1) ||
                    (TextUtils.isEmpty(y) || y.length() < 1)) {
                mSphinx.showTip(R.string.subway_location_error, Toast.LENGTH_SHORT);
                return;
            }
            
            Runnable run = new Runnable() {

                @Override
                public void run() {
                    POI poi = new POI();
                    Position pos = new Position(Double.parseDouble(y), Double.parseDouble(x));
                    poi.setUUID(poiid);
                    poi.setName(name + mSphinx.getString(R.string.subway_poi_name_suffix));
                    poi.setPosition(pos);
                    poi.setSourceType(POI.SOURCE_TYPE_SUBWAY);
                    poi.setFrom(POI.FROM_LOCAL);
                    mSphinx.getPOINearbyFragment().setData(poi);
                    mSphinx.showView(R.id.view_poi_nearby_search);
                }
                
            };
            mSphinx.runOnUiThread(run);
        }
    }

    @Override
    public void onPostExecute(TKAsyncTask tkAsyncTask) {
        super.onPostExecute(tkAsyncTask);
        LogWrapper.d(TAG, "onPostExecute()");
        FileDownload fileDownload = (FileDownload) tkAsyncTask.getBaseQuery();
        int stat = STAT_QUERY_FAILED;
        Response response = fileDownload.getResponse();
        if (response != null) {
            if (BaseActivity.checkResponseCode(fileDownload, mSphinx, new int[]{953}, TKActivity.SHOW_ERROR_MSG_NO, this, false, true) == false) {
                if (response.getResponseCode() != 953) {
                    subwayPath = MapEngine.getSubwayDataPath(mSphinx, mCityInfo.getId());
                    if (subwayPath != null) {
                        stat = STAT_MAP;
                        mURL = Uri.fromFile(new File(subwayPath)).toString();
                        showSubwayMap(mURL);
                    }
                } else {
                    stat = STAT_NODATA;
                }
            }
        }
        if (mOriginStat != STAT_MAP) {
            setStatus(stat);
        }
    }

    @Override
    public void retry() {
        if (mBaseQuerying != null) {
            for(int i = 0, size = mBaseQuerying.size(); i < size; i++) {
                mBaseQuerying.get(i).setResponse(null);
            }
            mSphinx.queryStart(mBaseQuerying);
        }
        setStatus(STAT_QUERY);

    }

}

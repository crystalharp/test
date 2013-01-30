/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.view;

import com.decarta.Globals;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.TKConfig;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import java.util.Hashtable;

import com.tigerknows.ActionLog;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.DataQuery;
import com.tigerknows.util.TKAsyncTask;

/**
 * @author Peng Wenyue
 */
public class MenuFragment extends BaseFragment implements View.OnClickListener {

    public MenuFragment(Sphinx sphinx) {
        super(sphinx);
        // TODO Auto-generated constructor stub
    }

    public static final int[] MENU_BACKGROUD = {R.drawable.menu_poi_still,
        R.drawable.menu_discover_still,
        R.drawable.menu_traffic_still, 
        R.drawable.menu_more_still}; 
    
    public static final int[] MENU_BACKGROUD_SELECTED = {R.drawable.menu_poi_focused, 
        R.drawable.menu_discover_focused,
        R.drawable.menu_traffic_focused, 
        R.drawable.menu_more_focused}; 
    
    private ImageButton mPOIBtn;
    private ImageButton mDiscoverBtn;
    private ImageButton mTrafficBtn;
    private ImageButton mMoreBtn;
    private ImageView mUpgradeImv;
    private ImageView mDiscvoerImv;
    
    private int mMenuIdSelected;
    private ImageButton[] mMenuBtnList = new ImageButton[4]; 

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        
        mRootView = mLayoutInflater.inflate(R.layout.menu, container, false);
        findViews();
        setListener();
        return mRootView;
    }
    
    @Override
    public void onResume() {
        super.onResume();
        
        mRootView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        mSphinx.getControlView().setPadding(0, 0, 0, getMeasuredHeight());
    }
    
    @Override
    public void onPause() {
        super.onPause();
        mSphinx.getControlView().setPadding(0, 0, 0, 0);
    }

    protected void findViews() {
        mPOIBtn = (ImageButton)mRootView.findViewById(R.id.poi_btn);
        mDiscoverBtn = (ImageButton)mRootView.findViewById(R.id.discover_btn);
        mDiscvoerImv = (ImageView)mRootView.findViewById(R.id.discover_imv);
        mTrafficBtn = (ImageButton)mRootView.findViewById(R.id.traffic_btn);
        mMoreBtn = (ImageButton)mRootView.findViewById(R.id.more_btn);
        mUpgradeImv = (ImageView)mRootView.findViewById(R.id.upgrade_imv);
        mMenuBtnList[0] = mPOIBtn;
        mMenuBtnList[1] = mDiscoverBtn;
        mMenuBtnList[2] = mTrafficBtn;
        mMenuBtnList[3] = mMoreBtn;
    }

    protected void setListener() {
        mPOIBtn.setOnClickListener(this);
        mDiscoverBtn.setOnClickListener(this);
        mTrafficBtn.setOnClickListener(this);
        mMoreBtn.setOnClickListener(this);
    }
    
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.poi_btn:
            	mActionLog.addAction(ActionLog.MenuSearch);
                mSphinx.uiStackClose(new int[]{R.id.view_home});
                mSphinx.showView(R.id.view_home);
                break;
            case R.id.discover_btn:
                mSphinx.uiStackClose(new int[]{R.id.view_discover});
                mActionLog.addAction(ActionLog.MenuDiscover);
                mSphinx.showView(R.id.view_discover);
                mSphinx.getDiscoverFragment().setCurrentItem(0);
                if (mDiscvoerImv.getVisibility() == View.VISIBLE) {
                    mDiscvoerImv.setVisibility(View.GONE);
                    mSphinx.showHint(TKConfig.PREFS_HINT_DISCOVER_HOME, R.layout.hint_discover_home);
                }
                break;
            case R.id.traffic_btn:
                mSphinx.uiStackClose(new int[]{R.id.view_traffic_query});
            	mActionLog.addAction(ActionLog.MenuTraffic);
            	mSphinx.getTrafficQueryFragment().setState(TrafficViewSTT.State.Normal);
            	//正常使用菜单进入交通，默认在起点显示我的位置。
            	mSphinx.getTrafficQueryFragment().setShowStartMyLocation(true);
                mSphinx.showView(R.id.view_traffic_query);
                break;
            case R.id.more_btn:
                mSphinx.uiStackClose(new int[]{R.id.view_more});
            	mActionLog.addAction(ActionLog.MenuMore);
                mSphinx.showView(R.id.view_more);
                break;

            default:
                break;
        }
    }
    
    public void updateMenuStatus(int id) {
        mMenuIdSelected = id;
        
        int i = 0;
        for(ImageButton imageButton : mMenuBtnList) {
            if (imageButton.getId() == mMenuIdSelected) {
                imageButton.setBackgroundResource(MENU_BACKGROUD_SELECTED[i]);
            } else {
                imageButton.setImageBitmap(null);
                imageButton.setBackgroundResource(MENU_BACKGROUD[i]);
            }
            i++;
        }
    }
    
    public void setUpgrade(int visibility) {
        mUpgradeImv.setVisibility(visibility);
    }
    
    public void setDiscover(int visibility) {
        mDiscvoerImv.setVisibility(visibility);
    }
}

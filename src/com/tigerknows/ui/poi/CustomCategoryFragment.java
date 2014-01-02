package com.tigerknows.ui.poi;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import com.decarta.android.util.LogWrapper;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.TKConfig;
import com.tigerknows.ui.BaseFragment;

public class CustomCategoryFragment extends BaseFragment {

	public CustomCategoryFragment(Sphinx sphinx) {
		super(sphinx);
		// TODO Auto-generated constructor stub
	}
	
	private View[] mCustomBtns;
	private String[] mHotNames;
	private String mCurrentState;
	private int[] VIEW_IDS = {
			R.id.custom_00_btn,
			R.id.custom_01_btn,
			R.id.custom_02_btn,
			R.id.custom_03_btn,
			R.id.custom_04_btn,
			R.id.custom_05_btn,
			R.id.custom_06_btn,
			R.id.custom_07_btn,
			R.id.custom_08_btn,
			R.id.custom_09_btn,
			R.id.custom_10_btn,
			R.id.custom_11_btn,
			R.id.custom_12_btn,
			R.id.custom_13_btn,
			R.id.custom_14_btn
	};
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//TODO: mActionTag = 
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, 
			Bundle savedInstanceState) {
		LogWrapper.d(BaseFragment.TAG, "onCreateView()"+mActionTag);
		mRootView = mLayoutInflater.inflate(R.layout.poi_custom_category, container, false);
		
		findViews();
		setListener();
		mHotNames = mContext.getResources().getStringArray(R.array.custom_category);
		return mRootView;
	}
	
	@Override
	protected void findViews(){
		super.findViews();
		mCustomBtns = new View[NearbySearchFragment.NUM_OF_HOT];
		for(int i=0; i<NearbySearchFragment.NUM_OF_HOT; i++){
			mCustomBtns[i] = mRootView.findViewById(VIEW_IDS[i]);
		}
	}
	
	@Override
	protected void setListener(){
		super.setListener();
		for(int i=2; i<NearbySearchFragment.NUM_OF_HOT; i++){
			final int j = i;
			Button btn = (Button) mCustomBtns[j].findViewById(R.id.custom_btn);
			btn.setOnClickListener(new OnClickListener(){

				@Override
				public void onClick(View v) {
					ImageView imv = (ImageView) mCustomBtns[j].findViewById(R.id.icon_imv);
					StringBuilder sb = new StringBuilder(mCurrentState);
					if(mCurrentState.charAt(j) == '1'){
						sb.setCharAt(j, '0');
						mCurrentState = sb.toString();
						imv.setImageResource(R.drawable.btn_fill_in_normal);
					}else{
						sb.setCharAt(j, '1');
						mCurrentState = sb.toString();
						imv.setImageResource(R.drawable.btn_fill_in_focused);
					}
					LogWrapper.d("Trap", mCurrentState);
				}
			});
		}
	}
	
	@Override
	public void onPause(){
		TKConfig.setPref(mContext, TKConfig.PREFS_CUSTOM_CATEGORY, mCurrentState);
		super.onPause();
	}
	
	@Override
	public void onResume(){
		super.onResume();
		mTitleBtn.setText("自定义分类");
		mRightBtn.setVisibility(View.GONE);
		mCurrentState = TKConfig.getPref(mContext, TKConfig.PREFS_CUSTOM_CATEGORY, "111111100000000");
		refreshViewByCurrentState();
	}
	
	private void refreshViewByCurrentState(){
		for(int i=0; i<NearbySearchFragment.NUM_OF_HOT; i++){
			ImageView imv = (ImageView) mCustomBtns[i].findViewById(R.id.icon_imv);
			Button btn = (Button) mCustomBtns[i].findViewById(R.id.custom_btn);
			btn.setText((mHotNames[i].split(";"))[0]);
			if(mCurrentState.charAt(i) == '1'){
				imv.setImageResource(R.drawable.btn_fill_in_focused);
			}else{
				imv.setImageResource(R.drawable.btn_fill_in_normal);
			}
		}
	}
	
	public void setData(){
		
	}

}

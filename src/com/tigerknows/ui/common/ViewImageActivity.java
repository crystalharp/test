package com.tigerknows.ui.common;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.decarta.Globals;
import com.tigerknows.R;
import com.tigerknows.TKConfig;
import com.tigerknows.android.os.TKAsyncTask;
import com.tigerknows.common.ActionLog;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.DataQuery;
import com.tigerknows.model.FileUpload;
import com.tigerknows.model.Response;
import com.tigerknows.model.TKDrawable;
import com.tigerknows.model.DataQuery.PictureResponse;
import com.tigerknows.model.DataQuery.PictureResponse.PictureList;
import com.tigerknows.model.Hotel.HotelTKDrawable;
import com.tigerknows.ui.BaseActivity;
import com.tigerknows.ui.discover.TKGallery;
import com.tigerknows.util.Utility;
import com.tigerknows.widget.QueryingView;
import com.tigerknows.widget.RetryView;

public class ViewImageActivity extends BaseActivity implements RetryView.CallBack, View.OnClickListener {
    
    public static final String EXTRA_CAN_ADD = "EXTRA_CAN_ADD";
    
    public static final String EXTRA_TITLE = "EXTRA_TITLE";
    
    public static final String EXTRA_IMAGE = "EXTRA_IMAGE";
    
    public static final String EXTRA_IMAGE_LIST = "EXTRA_IMAGE_LIST";
    
    public static final String EXTRA_ORIGINAL_IMAGE_LIST = "EXTRA_ORIGINAL_IMAGE_LIST";
    
    public static final String EXTRA_COLUMN_WIDTH = "EXTRA_COLUMN_WIDTH";
    
    private static final int DEFAULT_COLUMN_WIDTH = 70;
    
    /**
     * 网格状态
     */
    private static final int STATE_GRID = 0;
    
    /**
     * 全屏状态
     */
    private static final int STATE_GALLERY = 1;
    
    private int mState =  STATE_GRID;

    private HotelTKDrawable mImage;
    private List<HotelTKDrawable> mImageList = new ArrayList<HotelTKDrawable>();;
    private List<HotelTKDrawable> mOriginalImageList = new ArrayList<HotelTKDrawable>();;
    private GridView mGridView;
    private GridAdapter mGridAdapter;
    private TKGallery mTKGallery;
    private GalleryAdapter mGalleryAdapter;
    private int mPosition;
    
    private String mTitle;
    private String mRefDty;
    private String mRefId;
    private boolean mCanAdd;
    private int mColumnWidth;
    private DataQuery mDataQuery;

    private QueryingView mQueryingView = null;
    
    private TextView mQueryingTxv = null;
    
    private View mEmptyView = null;
    
    private TextView mEmptyTxv = null;
    
    private RetryView mRetryView;
    
    private Runnable mLoadedDrawableRun = new Runnable() {
        
        @Override
        public void run() {
            mHandler.removeCallbacks(mActualLoadedDrawableRun);
            mHandler.post(mActualLoadedDrawableRun);
        }
    };
    
    private Runnable mActualLoadedDrawableRun = new Runnable() {
        
        @Override
        public void run() {
            if (mState == STATE_GRID) {
                mGridAdapter.notifyDataSetChanged();
            } else if (mState == STATE_GALLERY) {
                mGalleryAdapter.notifyDataSetChanged();
            }
        }
    };
    
    void setState(int state) {
        this.mState = state;
        if (this.mState == STATE_GRID) {
            if (mTitleBtn != null) {
                mTitleBtn.setText(mTitle);
            }
            mGridView.setVisibility(View.VISIBLE);
            mTKGallery.setVisibility(View.GONE);
        } else if (this.mState == STATE_GALLERY) {
            if (mTitleBtn != null) {
                if (mImage != null) {
                    mTitleBtn.setText(mTitle);
                } else {
                    mTitleBtn.setText(R.string.picture_detail);
                }
            }
            mGridView.setVisibility(View.GONE);
            mTKGallery.setVisibility(View.VISIBLE);
        }
        mHandler.post(mLoadedDrawableRun);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActionTag = ActionLog.ViewPicture;
        
        setContentView(R.layout.common_view_image);
        
        findViews();
        setListener();
        
        Intent intent = getIntent();
        mTitle = intent.getStringExtra(EXTRA_TITLE);
        mRefDty = intent.getStringExtra(BaseQuery.SERVER_PARAMETER_REF_DATA_TYPE);
        mRefId = intent.getStringExtra(BaseQuery.SERVER_PARAMETER_REF_ID);
        mCanAdd = intent.getBooleanExtra(EXTRA_CAN_ADD, false);
        mColumnWidth = intent.getIntExtra(EXTRA_COLUMN_WIDTH, DEFAULT_COLUMN_WIDTH);
        mImage = intent.getParcelableExtra(EXTRA_IMAGE);
        List<HotelTKDrawable>  imageList = intent.getParcelableArrayListExtra(EXTRA_IMAGE_LIST);
        List<HotelTKDrawable> originalImageList = intent.getParcelableArrayListExtra(EXTRA_ORIGINAL_IMAGE_LIST);
        
        mGridView.setColumnWidth(Utility.dip2px(mThis, mColumnWidth));
        
        boolean noList = (imageList == null || originalImageList == null);
        boolean noRef = (mRefDty == null || mRefId == null);
        
        if (mImage != null) {
            mOriginalImageList.add(mImage);

            if (mCanAdd) {
                mRightBtn.setBackgroundResource(R.drawable.btn_add_picture_title);
                mRightBtn.setOnClickListener(this);
            }

            mState = STATE_GALLERY;
        } else {
            if (noRef == false) {
                mGridView.setVisibility(View.GONE);
                mTKGallery.setVisibility(View.GONE);
                mQueryingView.setVisibility(View.VISIBLE);
                
                mDataQuery = new DataQuery(mThis);
                mDataQuery.addParameter(BaseQuery.SERVER_PARAMETER_DATA_TYPE, BaseQuery.DATA_TYPE_PICTURE);
                mDataQuery.addParameter(BaseQuery.SERVER_PARAMETER_REF_DATA_TYPE, mRefDty);
                mDataQuery.addParameter(BaseQuery.SERVER_PARAMETER_REF_ID, mRefId);
                
                queryStart(mDataQuery);
                
            } else if (noList == false) {
                mImageList.addAll(imageList);
                mOriginalImageList.addAll(originalImageList);
            } else {
                finish();
            }
        }

        mGridAdapter = new GridAdapter(mThis, mImageList);
        mGridView.setAdapter(mGridAdapter);
        
        mGalleryAdapter = new GalleryAdapter(mThis, mOriginalImageList);
        mTKGallery.setAdapter(mGalleryAdapter);

        mLeftBtn.setOnClickListener(new View.OnClickListener() {
            
            @Override
            public void onClick(View v) {
                if (mState == STATE_GALLERY && mImage == null) {
                    setState(STATE_GRID);
                } else {
                    finish();
                }
            }
        });
        mTitleBtn.setText(mTitle);
        
        if (noList == false || mImage != null) {
            setState(mState);
        }
        
        if (mCanAdd && mImage == null) {
            mImageList.add(0, new HotelTKDrawable());
        }
    }
    
    protected void findViews() {
        super.findViews();
        mGridView = (GridView)findViewById(R.id.hotel_grid);
        mTKGallery = (TKGallery)findViewById(R.id.gallery_view);
        
        mQueryingView = (QueryingView)findViewById(R.id.querying_view);
        mEmptyView = findViewById(R.id.empty_view);
        mEmptyTxv = (TextView) mEmptyView.findViewById(R.id.empty_txv);
        mQueryingTxv = (TextView) mQueryingView.findViewById(R.id.loading_txv);
        mRetryView = (RetryView) findViewById(R.id.retry_view);
    }
    
    protected void setListener() {
        super.setListener();
        mGridView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {

                if (mCanAdd) {
                    if (position == 0) {
                        return;
                    }
                    position--;
                }
                setState(STATE_GALLERY);
                mTKGallery.setSelection(position);
                mActionLog.addAction(mActionTag+ActionLog.ListViewItem, position);
                mPosition = position;
            }
        });
        mTKGallery.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long arg3) {
                mActionLog.addAction(mActionTag+ActionLog.ViewPageSelected, position, mPosition);
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                // TODO Auto-generated method stub
                
            }
        });
    }
    
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (mState == STATE_GALLERY && mImage == null) {
                setState(STATE_GRID);
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }
    
    private class GridAdapter extends ArrayAdapter<HotelTKDrawable> {

        public GridAdapter(Context context, List<HotelTKDrawable> list) {
            super(context, 0, list);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;
            if (convertView == null) {
                LinearLayout viewGroup = new LinearLayout(getContext());
                ImageView iconImv = new ImageView(getContext());
                iconImv.setId(R.id.icon_imv);
                iconImv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                iconImv.setBackgroundResource(R.drawable.bg_picture);
                viewGroup.addView(iconImv);
                viewGroup.setGravity(Gravity.CENTER);
                
                ViewGroup.LayoutParams layoutParams = iconImv.getLayoutParams();
                layoutParams.width = Utility.dip2px(mThis, mColumnWidth);
                layoutParams.height = layoutParams.width;
                
                view = viewGroup;
            } else {
                view = convertView;
            }
            
            ImageView iconImv = (ImageView)view.findViewById(R.id.icon_imv);
            
            HotelTKDrawable hotelTKDrawable = getItem(position);
            Drawable image;
            if (mCanAdd && position == 0) {
                image = getResources().getDrawable(R.drawable.btn_add_picture);
                iconImv.setOnClickListener(ViewImageActivity.this);
                iconImv.setClickable(true);
            } else {
                image = hotelTKDrawable.getTKDrawable().loadDrawable(mThis, mLoadedDrawableRun, ViewImageActivity.this.toString());
                iconImv.setOnClickListener(null);
                iconImv.setClickable(false);
            }
            
            if (image != null) {
                iconImv.setImageDrawable(image);
            } else {
                iconImv.setImageDrawable(null);
            }
            
            return view;
        }
        
    }
    
    private class GalleryAdapter extends ArrayAdapter<HotelTKDrawable> {
        
        static final int RES_ID = R.layout.view_image_gallery_item;

        public GalleryAdapter(Context context, List<HotelTKDrawable> list) {
            super(context, RES_ID, list);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;
            if (convertView == null) {
                view = mLayoutInflater.inflate(RES_ID, parent, false);
            } else {
                view = convertView;
            }
            
            ImageView iconImv = (ImageView)view.findViewById(R.id.icon_imv);
            TextView textTxv = (TextView)view.findViewById(R.id.text_txv);
            
            iconImv.setScaleType(ImageView.ScaleType.FIT_CENTER);
            
            HotelTKDrawable hotelTKDrawable = getItem(position);
            
            Drawable image = hotelTKDrawable.getTKDrawable().loadDrawable(mThis, mLoadedDrawableRun, ViewImageActivity.this.toString());
            if (image != null) {
                iconImv.setImageDrawable(image);
            } else {
                Drawable drawable = getResources().getDrawable(R.drawable.bg_picture);
                iconImv.setImageDrawable(drawable);
            }
            String name = hotelTKDrawable.getName();
            textTxv.setText((name != null ? name : "")+" "+(position+1)+"/"+(getCount()));
            return view;
        }
        
    }
    
    @Override    
    public void onPostExecute(TKAsyncTask tkAsyncTask) {
        super.onPostExecute(tkAsyncTask);
        
        DataQuery dataQuery = (DataQuery) tkAsyncTask.getBaseQuery();
        
        if (BaseActivity.checkReLogin(dataQuery, mThis, mSourceUserHome, mId, mId, mId, mCancelLoginListener)) {
            isReLogin = true;
            return;
        } else {
            mQueryingView.setVisibility(View.GONE);
            
            Response response = dataQuery.getResponse();
            if (response != null) {
                int responsCode = response.getResponseCode();
                if (responsCode != Response.RESPONSE_CODE_OK) {
                    mRetryView.setVisibility(View.VISIBLE);
                    return;
                } else {
                    PictureResponse pictureResponse = (PictureResponse) response;
                    PictureList pictureList = pictureResponse.getList();
                    if (pictureList != null) {
                        List<HotelTKDrawable> list = pictureList.getPictureList();
                        if (list != null && list.size() > 0) {
                            mTitle = mTitle + list.size();
                            mTitleBtn.setText(mTitle);
                            mOriginalImageList.addAll(list);
                            for(int i = 0, size = list.size(); i < size; i++) {
                                HotelTKDrawable originalHotelTKDrawable = list.get(i);
                                HotelTKDrawable hotelTKDrawable = new HotelTKDrawable();
                                hotelTKDrawable.setName(originalHotelTKDrawable.getName());
                                if (originalHotelTKDrawable.getTKDrawable() != null) {
                                    TKDrawable tkDrawable = new TKDrawable();
                                    tkDrawable.setUrl(Utility.getPictureUrlByWidthHeight(originalHotelTKDrawable.getTKDrawable().getUrl(), Globals.getPicWidthHeight(TKConfig.PICTURE_HOTEL_LIST)));
                                    hotelTKDrawable.setTkDrawable(tkDrawable);
                                }
                                mImageList.add(hotelTKDrawable);
                            }
                            setState(STATE_GRID);
                            return;
                        }
                    }
                    mEmptyView.setVisibility(View.VISIBLE);
                }
            } else {
                mRetryView.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void retry() {
        
        mDataQuery.setResponse(null);
        queryStart(mDataQuery);
        
        mRetryView.setVisibility(View.GONE);
        mQueryingView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onClick(View v) {
        Intent intent = new Intent();
        intent.putExtra(FileUpload.SERVER_PARAMETER_REF_DATA_TYPE, mRefDty);
        intent.putExtra(FileUpload.SERVER_PARAMETER_REF_ID, mRefId);
        intent.putExtra(AddPictureActivity.EXTRA_SUCCESS_TIP, getIntent().getStringExtra(AddPictureActivity.EXTRA_SUCCESS_TIP));
        Utility.showTakePhotoDialog(mActionTag, mThis, intent);
    }
}

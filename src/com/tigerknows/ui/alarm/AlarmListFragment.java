package com.tigerknows.ui.alarm;

import java.util.ArrayList;
import java.util.List;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.decarta.Globals;
import com.decarta.android.util.LogWrapper;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.android.location.Position;
import com.tigerknows.common.ActionLog;
import com.tigerknows.model.Alarm;
import com.tigerknows.service.AlarmService;
import com.tigerknows.ui.BaseFragment;
import com.tigerknows.ui.more.SettingActivity;
import com.tigerknows.util.ShareTextUtil;
import com.tigerknows.util.Utility;
import com.tigerknows.widget.StringArrayAdapter;

public class AlarmListFragment extends BaseFragment implements View.OnClickListener {

    static final String TAG = "AlarmListFragment";

    private ListView mListView = null;
    
    private MyAdapter mMyAdapter = null;
    
    private List<Alarm> mDataList = null;
    
    private Alarm mAlarm;
    
    private View mLoadingView = null;
    
    private TextView mEmptyView;
    
    private List<String> mRangeList = new ArrayList<String>();
    
    public AlarmListFragment(Sphinx sphinx) {
        super(sphinx);
    }

    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActionTag = ActionLog.AlarmList;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        LogWrapper.d(BaseFragment.TAG, "onCreateView()"+mActionTag);
        
        for(int i =  1; i <= 5; i++) {
            mRangeList.add(getString(R.string.length_str_m, 500*i));
        }
        
        mRootView = mLayoutInflater.inflate(R.layout.traffic_fetch_favorite, container, false);
        findViews();
        setListener();
        
        mListView.setSelector(R.color.transparent);
        mListView.setDividerHeight(0);
        mListView.setPadding(0, Utility.dip2px(mSphinx, 4), 0, 0);
        
        mEmptyView.setText(R.string.alarm_empty_tip);
        
        return mRootView;
    }
    
    @Override
    protected void findViews() {
        super.findViews();
        mListView = (ListView)mRootView.findViewById(R.id.favorite_lsv);
        mLoadingView = (LinearLayout)mRootView.findViewById(R.id.loading_lnl);
        mEmptyView = (TextView)mRootView.findViewById(R.id.empty_txv);
    }

    @Override
    public void onResume() {
        super.onResume();

        mTitleBtn.setText(getString(R.string.manage_alarm));
        mRightBtn.setBackgroundResource(R.drawable.btn_cancel);
        mRightBtn.setText(R.string.add_alarm);
        mRightBtn.setTextColor(getResources().getColor(R.color.black_dark));
        mRightBtn.setVisibility(View.VISIBLE);
        mRightBtn.setOnClickListener(this);
        
        if (mMyAdapter == null) {
            loadData();
        } else {
            mMyAdapter.notifyDataSetChanged();
            refreshEmptyView();
        }
    }
    
    private void refreshEmptyView() {
        if (mDataList.size() == 0) {
            mEmptyView.setVisibility(View.VISIBLE);
        } else {
            mEmptyView.setVisibility(View.GONE);
        }
    }

    @Override
    protected void setListener() {
        super.setListener();
    }

    protected void loadData() {
        LoadThread loadThread = new LoadThread();
        loadThread.start();
        mLoadingView.setVisibility(View.VISIBLE);
    }
    
    Handler mHandler = new Handler(){
        public void handleMessage(Message msg) {
            if (msg.what == 0) {
                mMyAdapter = new MyAdapter(mSphinx, mDataList);
                mListView.setAdapter(mMyAdapter);  
                refreshEmptyView();
                mLoadingView.setVisibility(View.GONE);
            }
        }
    };
    
    class LoadThread extends Thread{
        
        @Override
        public void run(){
            mDataList = Alarm.getAlarmList(mSphinx);
            mHandler.sendEmptyMessage(0);
        }
    }

    private class MyAdapter extends ArrayAdapter<Alarm> implements View.OnClickListener {
        
        int statusBtnWidth = 0;

        public MyAdapter(Context context, List<Alarm> poiList) {
            super(context, R.layout.alarm_item, poiList);
            View view = mLayoutInflater.inflate(R.layout.alarm_item, null, false);
            Button statusBtn = (Button) view.findViewById(R.id.status_btn);
            statusBtn.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            statusBtnWidth = statusBtn.getMeasuredWidth();
            statusBtnWidth += Utility.dip2px(mSphinx, 56);
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {

            View view;
            if (convertView == null) {
                view = mLayoutInflater.inflate(R.layout.alarm_item, parent, false);
            } else {
                view = convertView;
            }
            
            Alarm data = getItem(position);
            
            TextView nameTxv = (TextView) view.findViewById(R.id.name_txv);
            TextView distanceTxv = (TextView) view.findViewById(R.id.distance_txv);
            Button statusBtn = (Button) view.findViewById(R.id.status_btn);
            Button rangeBtn = (Button) view.findViewById(R.id.range_btn);
            Button ringtoneBtn = (Button) view.findViewById(R.id.ringtone_btn);
            Button deleteBtn = (Button) view.findViewById(R.id.delete_btn);
            
            String name = data.getName();
            nameTxv.setText(name);
            Position myLocation = Globals.getMyLocationPosition();
            int distanceTxvWidth = 0;
            if (myLocation != null) {
                distanceTxv.setText(getString(R.string.range) + ShareTextUtil.getPlanLength(mSphinx, Position.distanceBetween(myLocation, data.getPosition())));
                distanceTxv.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                distanceTxvWidth = distanceTxv.getMeasuredWidth();
            } else {
                distanceTxv.setText(null);
            }
            if (data.getStatus() == 0) {
                statusBtn.setBackgroundResource(R.drawable.btn_check_enabled);
            } else {
                statusBtn.setBackgroundResource(R.drawable.btn_check_disabled);
            }
            
            ViewGroup.LayoutParams layoutParams = nameTxv.getLayoutParams();
            layoutParams.width = LayoutParams.WRAP_CONTENT;
            if (distanceTxvWidth > 0) {
                nameTxv.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                int nameTxvWidth = nameTxv.getMeasuredWidth();
                if (Globals.g_metrics.widthPixels - statusBtnWidth - distanceTxvWidth - nameTxvWidth < 0) {
                    layoutParams.width = Globals.g_metrics.widthPixels - statusBtnWidth - distanceTxvWidth;
                }
            }
            nameTxv.setText(name);
            rangeBtn.setText(getString(R.string.length_str_m, String.valueOf(data.getRange())));
            ringtoneBtn.setText(data.getRingtoneName());

            statusBtn.setTag(position);
            rangeBtn.setTag(position);
            ringtoneBtn.setTag(position);
            deleteBtn.setTag(position);

            statusBtn.setOnClickListener(MyAdapter.this);
            rangeBtn.setOnClickListener(MyAdapter.this);
            ringtoneBtn.setOnClickListener(MyAdapter.this);
            deleteBtn.setOnClickListener(MyAdapter.this);
            
            return view;
        }

        @Override
        public void onClick(View v) {
            int id = v.getId();
            int position = (Integer) v.getTag();
            mAlarm = mDataList.get(position);
            if (id == R.id.status_btn) {
                addActionLog(ActionLog.AlarmListStatus, mAlarm.getStatus());
                mAlarm.setStatus(mAlarm.getStatus() == 0 ? 1 : 0);
                Alarm.writeAlarm(mSphinx, mAlarm);
                mMyAdapter.notifyDataSetChanged();
                
                if (mAlarm.getStatus() == 0) {
                    showSettingLocationDialog(mSphinx);
                }
            } else if (id == R.id.range_btn) {
                addActionLog(ActionLog.AlarmListRange, mAlarm.getRange());
                showRangeDialog();
            } else if (id == R.id.ringtone_btn) {
                addActionLog(ActionLog.AlarmListRingtone, mAlarm.getRingtoneName());
                Alarm.pickRingtone(mSphinx, mAlarm.getRingtone(), R.id.view_alarm_list);
            } else if (id == R.id.delete_btn) {
                addActionLog(ActionLog.AlarmListDelete);
                mDataList.remove(mAlarm);
                Alarm.deleteAlarm(mSphinx, mAlarm);
                mMyAdapter.notifyDataSetChanged();
                refreshEmptyView();
            }
        }
        
    }

    @Override
    public void onClick(View v) {
        mSphinx.showView(R.id.view_alarm_add);
    }


    public void setData(Uri uri) {
        String ringtoneName = Alarm.getRingtoneName(mSphinx, uri);
        mAlarm.setRingtone(uri);
        mAlarm.setRingtoneName(ringtoneName);
        Alarm.writeAlarm(mSphinx, mAlarm);
        mMyAdapter.notifyDataSetChanged();
    }

    public void showRangeDialog(){
        final ArrayAdapter<String> adapter = new StringArrayAdapter(mSphinx, mRangeList);
        View alterListView = mSphinx.getLayoutInflater().inflate(R.layout.alert_listview, null, false);
        
        final ListView listView = (ListView) alterListView.findViewById(R.id.listview);
        listView.setAdapter(adapter);
        
        final Dialog dialog = Utility.getChoiceDialog(mSphinx, alterListView, R.style.AlterChoiceDialog);
        
        TextView titleTxv = (TextView)alterListView.findViewById(R.id.title_txv);
        titleTxv.setText(R.string.range);
        
        Button button = (Button)alterListView.findViewById(R.id.confirm_btn);
        button.setVisibility(View.GONE);
        
        dialog.show();
        
        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int which, long arg3){
                listView.setAdapter(adapter);
                mAlarm.setRange(500*(which+1));
                Alarm.writeAlarm(mSphinx, mAlarm);
                mMyAdapter.notifyDataSetChanged();
                AlarmService.start(mSphinx, true);
                dialog.dismiss();
            }
        });
    }
    
    public static void showSettingLocationDialog(final Sphinx activity) {
        if (SettingActivity.checkGPS(activity)) {
            return;
        }
        Utility.showNormalDialog(activity,
                                 activity.getString(R.string.prompt),
                                 activity.getString(R.string.alarm_enable_tip),
                                 activity.getString(R.string.settings),
                                 activity.getString(R.string.cancel),
                                 new DialogInterface.OnClickListener() {
                    
                                     @Override
                                     public void onClick(DialogInterface arg0, int id) {
                                         if (id == DialogInterface.BUTTON_POSITIVE) {
                                             activity.showView(R.id.activity_setting_location);
                                         }
                                     }
                                 }
                                 );
    }
}

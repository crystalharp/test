package com.tigerknows.model.test;

import com.decarta.Globals;
import com.decarta.android.map.MapView;
import com.decarta.android.util.LogWrapper;
import com.tigerknows.BaseActivity;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.TKConfig;
import com.tigerknows.maps.MapEngine;
import com.tigerknows.maps.MapEngine.CityInfo;
import com.tigerknows.model.AccountManage;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.LocationQuery;
import com.tigerknows.model.DataQuery.DiscoverResponse;
import com.tigerknows.model.xobject.XMap;
import com.tigerknows.radar.Alarms;
import com.tigerknows.service.LocationCollectionService;
import com.tigerknows.service.PullService;
import com.tigerknows.service.TKLocationManager;
import com.tigerknows.service.TigerknowsLocationManager;
import com.tigerknows.util.CommonUtils;
import com.tigerknows.view.MoreFragment;
import com.tigerknows.view.StringArrayAdapter;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.provider.Settings;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.tigerknows.widget.Toast;

import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.List;

public class BaseQueryTest {
    
    static final String TAG = "BaseQueryTest";
    
    public static boolean Test = true;
    
    static int RESPONSE_CODE = BaseQuery.STATUS_CODE_NETWORK_OK;

    public static XMap launchResponse() {
        return launchResponse(new XMap());
    }

    public static XMap launchResponse(XMap data) {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        data.put(DiscoverResponse.FIELD_RESPONSE_CODE, RESPONSE_CODE);
        data.put(DiscoverResponse.FIELD_DESCRIPTION, "FIELD_DESCRIPTION");
        return  data;
    }
    
    public static void showSetResponseCode(LayoutInflater layoutInflater, final Activity activity) {
        if (Test == false) {
            return;
        }
        LinearLayout layout = new LinearLayout(activity);
        layout.setOrientation(LinearLayout.VERTICAL);
        final Button editConfigBtn = new Button(activity);
        layout.addView(editConfigBtn, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

        final Button clearDataBtn = new Button(activity);
        layout.addView(clearDataBtn, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        
        final LinearLayout deleteMobileNumLayout = new LinearLayout(activity);
        deleteMobileNumLayout.setOrientation(LinearLayout.HORIZONTAL);
        Button deleteMobileNumBtn = new Button(activity);
        deleteMobileNumLayout.addView(deleteMobileNumBtn);
        final EditText deleteMobileNumEdt = new EditText(activity);
        deleteMobileNumLayout.addView(deleteMobileNumEdt, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
        layout.addView(deleteMobileNumLayout);

        final CheckBox launchTestChb = new CheckBox(activity);
        layout.addView(launchTestChb);
        final LinearLayout lunchTestLayout = new LinearLayout(activity);
        lunchTestLayout.setOrientation(LinearLayout.HORIZONTAL);
        TextView responseCodeTxv = new TextView(activity);
        lunchTestLayout.addView(responseCodeTxv);
        final AutoCompleteTextView responseCodeEdt = new AutoCompleteTextView(activity);
        lunchTestLayout.addView(responseCodeEdt, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
        responseCodeEdt.setAdapter(new StringArrayAdapter(activity, new String[] {"200", "300"}));
        responseCodeEdt.setThreshold(0);
        responseCodeEdt.setHint(String.valueOf(RESPONSE_CODE));
        layout.addView(lunchTestLayout);
        
        final Button clearLocationCacheBtn = new Button(activity);
        layout.addView(clearLocationCacheBtn, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        final CheckBox unallowedLocationChb = new CheckBox(activity);
        layout.addView(unallowedLocationChb);
        final CheckBox locationChb = new CheckBox(activity);
        layout.addView(locationChb);
        final EditText locationEdt = new EditText(activity);
        layout.addView(locationEdt, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));

        final Button updateSoftTip = new Button(activity);
        layout.addView(updateSoftTip, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        final Button diaoyanTip = new Button(activity);
        layout.addView(diaoyanTip, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        final Button commentTip = new Button(activity);
        layout.addView(commentTip, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        final Button updateMapTip = new Button(activity);
        layout.addView(updateMapTip, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        final Button radarPushBtn = new Button(activity);
        layout.addView(radarPushBtn, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        final Button radarLocationBtn = new Button(activity);
        layout.addView(radarLocationBtn, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        final Button readLocationLogBtn = new Button(activity);
        layout.addView(readLocationLogBtn, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        
        editConfigBtn.setText("View or Modify config.txt");
        editConfigBtn.setOnClickListener(new View.OnClickListener() {
            
            @Override
            public void onClick(View arg0) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                File file = new File(TKConfig.getDataPath(true)+"config.txt");
                if (file.exists()) {
                    Uri uri = Uri.fromFile(file);
                    intent.setDataAndType(uri, "text/plain");
                    activity.startActivity(intent);
                } else {
                    Toast.makeText(activity, "config.ini not exists!", Toast.LENGTH_LONG).show();
                }
            }
        });

        clearDataBtn.setText("Clear app data");
        clearDataBtn.setOnClickListener(new View.OnClickListener() {
            
            @Override
            public void onClick(View arg0) {

                Class<?> iPackageDataObserverClass = null;
                try {
                    iPackageDataObserverClass = Class.forName("android.content.pm.IPackageDataObserver");
                } catch (ClassNotFoundException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }

                Class<ActivityManager> activityManagerClass=ActivityManager.class;
                ActivityManager activityManager=(ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE);

                Method clearDataMethod=activityManagerClass.getMethods()[0];

                Object iPackageDataObserverObject = Proxy.newProxyInstance(
                    Settings.class.getClassLoader(), new Class[]{iPackageDataObserverClass},
                                        new InvocationHandler() {

                            public Object invoke(Object proxy, Method method, Object[] args)
                                    throws Throwable {
                                return null;
                            }
                        });

                try {
                    clearDataMethod.invoke(activityManager, activity.getApplicationInfo().packageName, iPackageDataObserverObject);
                } catch (IllegalArgumentException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });

        deleteMobileNumBtn.setText("Delete mobile phone");
        deleteMobileNumEdt.setInputType(InputType.TYPE_CLASS_NUMBER);
        deleteMobileNumEdt.setSingleLine();
        deleteMobileNumBtn.setOnClickListener(new View.OnClickListener() {
            
            @Override
            public void onClick(View arg0) {
                AccountManage accountManage = new AccountManage(activity);
                String phone = deleteMobileNumEdt.getText().toString().trim();
                Hashtable<String, String> criteria = new Hashtable<String, String>();
                criteria.put(BaseQuery.SERVER_PARAMETER_OPERATION_CODE, "du");
                criteria.put(BaseQuery.SERVER_PARAMETER_TELEPHONE, phone);
                accountManage.setup(criteria, Globals.g_Current_City_Info.getId());
                accountManage.setTipText(activity.getString(R.string.query_loading_tip));
                if (activity instanceof BaseActivity) {
                    ((BaseActivity)(activity)).queryStart(accountManage, false);
                } else if (activity instanceof Sphinx) {
                    ((Sphinx)(activity)).queryStart(accountManage);
                }
            }
        });
        
        clearLocationCacheBtn.setText("Clear Location Cache");
        clearLocationCacheBtn.setOnClickListener(new View.OnClickListener() {
            
            @Override
            public void onClick(View arg0) {
                LocationQuery.getInstance(activity).clearCache();
                Globals.g_My_Location = null;
                Globals.g_My_Location_City_Info = null;
                if (activity instanceof Sphinx) {
                    Sphinx sphinx = (Sphinx) activity;
                    sphinx.mLocationListener.onLocationChanged(null);
                }
            }
        });
        unallowedLocationChb.setChecked(TKLocationManager.UnallowedLocation);
        unallowedLocationChb.setText("unallowed location response");
        unallowedLocationChb.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View arg0) {
                TKLocationManager.UnallowedLocation = unallowedLocationChb.isChecked();
            }
        });
        launchTestChb.setText("Launch fake data(DataQuery, DataOperation, AccountManage");
        launchTestChb.setTextColor(0xffffffff);
        launchTestChb.setChecked(BaseQuery.Test);
        launchTestChb.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View arg0) {
                BaseQuery.Test = launchTestChb.isChecked();
                lunchTestLayout.setVisibility(BaseQuery.Test ? View.VISIBLE : View.GONE);
            }
        });
        responseCodeTxv.setText("ResponseCode:");
        responseCodeTxv.setTextColor(0xffffffff);
        responseCodeEdt.setText("");
        responseCodeEdt.setInputType(InputType.TYPE_CLASS_NUMBER);
        responseCodeEdt.setSingleLine();
        lunchTestLayout.setVisibility(BaseQuery.Test ? View.VISIBLE : View.GONE);
        
        locationChb.setText("Specific Location(lat,lon,accuracy)");
        locationChb.setChecked(TKLocationManager.UnallowedLocation);
        locationChb.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View arg0) {
                locationEdt.setVisibility(locationChb.isChecked() ? View.VISIBLE : View.GONE);
            }
        });
        locationEdt.setText("39.904156,116.397764,1000");
        locationEdt.setVisibility(View.GONE);
        
        updateSoftTip.setText("clear updateSoftTip");
        updateSoftTip.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View arg0) {
                if (Globals.g_Bootstrap_Model != null) {
                    Globals.g_Bootstrap_Model.setSoftwareUpdate(null);
                }
            }
        });
        
        diaoyanTip.setText("clear diaoyanTip");
        diaoyanTip.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View arg0) {
            	(((Sphinx) activity).getMoreFragment()).setDiaoyanQueryResponse(null);
            }
        });
        
        commentTip.setText("clear commenTip");
        commentTip.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View arg0) {
                TKConfig.setPref(activity, TKConfig.PREFS_SHOW_UPGRADE_COMMENT_TIP, String.valueOf(MoreFragment.SHOW_COMMENT_TIP_TIMES));
            }
        });
        
        updateMapTip.setText("clear updateMapTip");
        updateMapTip.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View arg0) {
                MoreFragment.CurrentDownloadCity = null;
            }
        });

        radarPushBtn.setText("send a Radar Push in 5s");
        radarPushBtn.setOnClickListener(new View.OnClickListener() {
            
            @Override
            public void onClick(View v) {
                Calendar next = Calendar.getInstance();
                next.setTimeInMillis(System.currentTimeMillis());
                next.add(Calendar.SECOND, 5);
                Alarms.enableAlarm(activity, next, PullService.alarmAction);
                LogWrapper.d(TAG, "Radar Push send in:" + next.getTime().toLocaleString());
            }
        });

        radarLocationBtn.setText("send a Radar location in 5s");
        radarLocationBtn.setOnClickListener(new View.OnClickListener() {
            
            @Override
            public void onClick(View v) {
                Calendar next = Calendar.getInstance();
                next.setTimeInMillis(System.currentTimeMillis());
                next.add(Calendar.SECOND, 5);
                Alarms.enableAlarm(activity, next, LocationCollectionService.alarmAction);
                LogWrapper.d(TAG, "Radar Location send in:" + next.getTime().toLocaleString());
            }
        });

        readLocationLogBtn.setText("read location log");
        readLocationLogBtn.setOnClickListener(new View.OnClickListener() {
            
            @Override
            public void onClick(View v) {
                String locationLog = PullService.queryCollectionLocation(activity);
                CommonUtils.showNormalDialog(activity, locationLog);
            }
        });
        
        ScrollView scrollView = new ScrollView(activity);
        scrollView.addView(layout);
        AlertDialog dialog = new AlertDialog.Builder(activity)
            .setCancelable(false)
            .setTitle("Test")
            .setView(scrollView)
            .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    try {
                        if (launchTestChb.isChecked()) {
                            String str = responseCodeEdt.getEditableText().toString().trim();
                            if (TextUtils.isEmpty(str) == false) {
                                RESPONSE_CODE = Integer.parseInt(str);
                            }
                        }
                        if (locationChb.isChecked()) {
                            String[] str = locationEdt.getEditableText().toString().split(",");
                            Location location = new Location(TigerknowsLocationManager.TIGERKNOWS_PROVIDER);
                            location.setLatitude(Float.parseFloat(str[0]));
                            location.setLongitude(Float.parseFloat(str[1]));
                            location.setAccuracy(Float.parseFloat(str[2]));
                            if (activity instanceof Sphinx) {
                                Sphinx sphinx = (Sphinx) activity;
                                sphinx.mLocationListener.onLocationChanged(location);
                            }
                        }
                    } catch (Exception e) {
                        Toast.makeText(activity, "Parse Error!", Toast.LENGTH_LONG).show();
                    }
                }
            })
            .create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }
    
    public static void throwNewException() {
        try {
            throw new Exception("test1");
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    public static void changeCity(final Sphinx sphinx) {
        
      //自动切换全国各个城市
      new Thread(new Runnable() {
          
          @Override
          public void run() {
              MapEngine mapEngine = MapEngine.getInstance();
              final MapView mapView = sphinx.getMapView();
              List<CityInfo>  sAllCityInfoList = mapEngine.getAllProvinceCityList(sphinx);
              boolean stop = false;
              for(CityInfo cityInfo : sAllCityInfoList) {
                  List<CityInfo> cityInfoList = cityInfo.getCityList();
                  for(final CityInfo c2 : cityInfoList) {
                      if (TextUtils.isEmpty(TKConfig.getPref(sphinx, TKConfig.PREFS_ACQUIRE_WAKELOCK, "")) == false) {
                          stop = true;
                          
                          sphinx.runOnUiThread(new Runnable() {
                              
                              @Override
                              public void run() {
                                  Toast.makeText(sphinx, "Stop!!!", 10*1000).show();
                              }
                          });
                          break;
                      }
                      
                      sphinx.runOnUiThread(new Runnable() {
                          
                          @Override
                          public void run() {
                              sphinx.changeCity(c2.getId());
                          }
                      });
                      try {
                          Thread.sleep(5*1000);
                          sphinx.runOnUiThread(new Runnable() {
                              
                              @Override
                              public void run() {
                                  mapView.zoomTo((int)(mapView.getZoomLevel()+2));
                              }
                          });
                          Thread.sleep(5*1000);
                      } catch (InterruptedException e2) {
                          // TODO Auto-generated catch block
                          e2.printStackTrace();
                      }
                      
                  }
                  if (stop) {
                      break;
                  }
              }
          }
      }).start();
    }
    
    public static void moveView(final Sphinx sphinx, final int offset) {
        
      new Thread(new Runnable() {
          
          @Override
          public void run() {
              final MapView mMapView = sphinx.getMapView();
              boolean stop = false;
              while (stop == false) {
                  if (TextUtils.isEmpty(TKConfig.getPref(sphinx, TKConfig.PREFS_ACQUIRE_WAKELOCK, "")) == false) {
                      stop = true;

                      sphinx.runOnUiThread(new Runnable() {

                          @Override
                          public void run() {
                              Toast.makeText(sphinx, "Stop!!!", 10 * 1000).show();
                          }
                      });
                      break;
                  }
                  try {
                      Thread.sleep(5 * 1000);
                      sphinx.runOnUiThread(new Runnable() {

                          @Override
                          public void run() {
                              mMapView.moveView(0, offset);
                          }
                      });
                      Thread.sleep(5 * 1000);
                  } catch (InterruptedException e2) {
                      // TODO Auto-generated catch block
                      e2.printStackTrace();
                  }
              }
          }
      }).start();
    }
}

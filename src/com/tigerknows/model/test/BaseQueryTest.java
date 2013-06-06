package com.tigerknows.model.test;

import com.decarta.Globals;
import com.decarta.android.map.MapView;
import com.decarta.android.util.LogWrapper;
import com.decarta.android.util.Util;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.TKConfig;
import com.tigerknows.android.location.TKLocationManager;
import android.widget.Toast;
import com.tigerknows.map.MapEngine;
import com.tigerknows.map.MapEngine.CityInfo;
import com.tigerknows.map.MapEngine.RegionMetaVersion;
import com.tigerknows.model.AccountManage;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.LocationQuery;
import com.tigerknows.model.Response;
import com.tigerknows.model.TKWord;
import com.tigerknows.model.xobject.XArray;
import com.tigerknows.model.xobject.XInt;
import com.tigerknows.model.xobject.XMap;
import com.tigerknows.model.xobject.XObject;
import com.tigerknows.model.xobject.XString;
import com.tigerknows.provider.HistoryWordTable;
import com.tigerknows.radar.Alarms;
import com.tigerknows.service.LocationService;
import com.tigerknows.service.PullService;
import com.tigerknows.service.TigerknowsLocationManager;
import com.tigerknows.ui.BaseActivity;
import com.tigerknows.ui.more.MoreHomeFragment;
import com.tigerknows.util.Utility;
import com.tigerknows.widget.StringArrayAdapter;
import com.weibo.sdk.android.WeiboParameters;

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
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

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
    
    private static Activity sActivity;
    
    public static void setActivity(Activity activity) {
        if (TKConfig.ModifyRequestData || TKConfig.ModifyResponseData) {
            sActivity = activity;
        }
    }
    
    public static Activity getActivity() {
        return sActivity;
    }
    
    static int RESPONSE_CODE = BaseQuery.STATUS_CODE_NETWORK_OK;

    public static XMap launchResponse() {
        return launchResponse(new XMap());
    }
    
    public static XMap launchResponse(String fieldDescription) {
    	return launchResponse(new XMap(),fieldDescription);
    }

    public static XMap launchResponse(XMap data) {
        data.put(Response.FIELD_RESPONSE_CODE, RESPONSE_CODE);
        switch(RESPONSE_CODE){
        case 825:
        	data.put(Response.FIELD_DESCRIPTION, "2013-13-32 25:00#中国银行#中国工商银行#交通银行");
        	break;
        case 826:
        	data.put(Response.FIELD_DESCRIPTION, "Error Message from ELONG");
        default:
        	data.put(Response.FIELD_DESCRIPTION, "FIELD_DESCRIPTION"+RESPONSE_CODE);
        	break;
        }
        return data;
    }
    
    public static XMap launchResponse(XMap data, String fieldDescription){
        data.put(Response.FIELD_RESPONSE_CODE, RESPONSE_CODE);
        data.put(Response.FIELD_DESCRIPTION, fieldDescription);
        return  data;
    }
    
    public static ViewGroup getViewByWeiboParameters(final Activity activity, final WeiboParameters parameters) {
        final LinearLayout layout = new LinearLayout(activity);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundResource(R.drawable.list_single);
        for (int loc = 0; loc < parameters.size(); loc++) {
            final String key = parameters.getKey(loc);
            TextView keyTxv = new TextView(activity);
            layout.addView(keyTxv);
            keyTxv.setBackgroundResource(R.drawable.btn_default);
            keyTxv.setText(key);
            final EditText valueEdt = new EditText(activity);
            layout.addView(valueEdt);
            valueEdt.setText(parameters.getValue(key));
            keyTxv.setOnClickListener(new OnClickListener() {
                
                @Override
                public void onClick(View v) {
                    parameters.remove(key);
                    parameters.add(key, valueEdt.getEditableText().toString());
                }
            });
        }
        
        TextView removeTxv = new TextView(activity);
        removeTxv.setText("remove");
        removeTxv.setBackgroundResource(R.drawable.btn_default);
        layout.addView(removeTxv);
        final EditText removeEdt = new EditText(activity);
        layout.addView(removeEdt);
        removeTxv.setOnClickListener(new View.OnClickListener() {
            
            @Override
            public void onClick(View v) {
                parameters.remove(removeEdt.getEditableText().toString().trim());
                layout.removeAllViews();
                layout.addView(getViewByWeiboParameters(activity, parameters));
            }
        });
        
        TextView addTxv = new TextView(activity);
        addTxv.setText("add");
        addTxv.setBackgroundResource(R.drawable.btn_default);
        layout.addView(addTxv);
        final EditText addEdt = new EditText(activity);
        layout.addView(addEdt);
        addTxv.setOnClickListener(new View.OnClickListener() {
            
            @Override
            public void onClick(View v) {
                String input = addEdt.getEditableText().toString();
                try {
                    int index = input.indexOf("=");
                    if (index > 0 && index < input.length()) {
                        parameters.add(input.substring(0, index), input.substring(index+1));
                    }
                    layout.removeAllViews();
                    layout.addView(getViewByWeiboParameters(activity, parameters));

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        
        return layout;
    }
    
    public static ViewGroup getViewByXObject(final Activity activty, final byte key, final XObject value, final int level) {
        final LinearLayout layout = new LinearLayout(activty);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundResource(R.drawable.list_single);
        TextView keyTxv = new TextView(activty);
        keyTxv.setBackgroundResource(R.drawable.btn_default);
        keyTxv.setPadding(level*32, 0, 0, 0);
        layout.addView(keyTxv);
        if (level > 1) {
            return null;
        }
        String type = null;
        if (value instanceof XInt ||
                value instanceof XString) {
            final EditText valueEdt = new EditText(activty);
            if (value instanceof XInt) {
                type = "XInt";
                valueEdt.setInputType(InputType.TYPE_CLASS_NUMBER);
            } else if (value instanceof XString) {
                type = "XString";
            }
            
            valueEdt.setText(value.toString());
            valueEdt.setPadding(level*32, 0, 0, 0);
            keyTxv.setOnClickListener(new OnClickListener() {
                
                @Override
                public void onClick(View v) {
                    if (value instanceof XInt) {
                        ((XInt) value).setValue(Integer.parseInt(valueEdt.getEditableText().toString()));
                    } else if (value instanceof XString) {
                        ((XString) value).setValue(valueEdt.getEditableText().toString());
                    }
                }
            });
            layout.addView(valueEdt);
        } else if (value instanceof XArray) {
            type = "XArray";
            @SuppressWarnings("rawtypes")
            final XArray xArray = (XArray)value;
            for(int i = 0, size = xArray.size(); i < size; i++) {
                View view = getViewByXObject(activty, (byte)i, (XObject) xArray.get(i), level+1);
                if (view != null) {
                    layout.addView(view);
                }
            }
            keyTxv.setOnClickListener(new OnClickListener() {
                
                @Override
                public void onClick(View v) {
                    ViewGroup viewGroup = getViewByXObject(activty, (byte)0, xArray, 0);
                    ScrollView scrollView = new ScrollView(activty);
                    scrollView.addView(viewGroup);
                    Utility.showNormalDialog(activty, scrollView);
                }
            });

            if (layout.getChildCount() > 1) {
                TextView removeTxv = new TextView(activty);
                removeTxv.setText("remove");
                removeTxv.setBackgroundResource(R.drawable.btn_default);
                layout.addView(removeTxv);
                final EditText removeEdt = new EditText(activty);
                layout.addView(removeEdt);
                removeTxv.setOnClickListener(new View.OnClickListener() {
                    
                    @Override
                    public void onClick(View v) {
                        int index = Integer.parseInt(removeEdt.getEditableText().toString());
                        if (index >= 0 && index < xArray.size()) {
                            xArray.remove(index);
                            layout.removeAllViews();
                            layout.addView(getViewByXObject(activty, key, xArray, level));
                        }
                    }
                });

                
                TextView addTxv = new TextView(activty);
                addTxv.setText("add");
                addTxv.setBackgroundResource(R.drawable.btn_default);
                layout.addView(addTxv);
                final EditText addEdt = new EditText(activty);
                layout.addView(addEdt);
                addTxv.setOnClickListener(new View.OnClickListener() {
                    
                    @SuppressWarnings("unchecked")
                    @Override
                    public void onClick(View v) {
                        String input = addEdt.getEditableText().toString();
                        try {
                            if (input.startsWith("int")) {
                                String s = input.substring(3);
                                xArray.add(XInt.valueOf(Integer.parseInt(s)));
                            } else if (input.startsWith("string")) {
                                String s = input.substring(6);
                                xArray.add(XString.valueOf(s));
                            } else if (input.startsWith("array")) {
                                String s = input.substring(5);
                                if (s.startsWith("int")) {
                                    XArray<XInt> x = new XArray<XInt>();
                                    x.add(XInt.valueOf(0));
                                    xArray.add(x);
                                } else if (s.startsWith("string")) {
                                    XArray<XString> x = new XArray<XString>();
                                    x.add(XString.valueOf("0"));
                                    xArray.add(x);                                
                                }
                            } else if (input.startsWith("map")) {
                                XMap xMap1 = new XMap();
                                xMap1.put((byte)255, XInt.valueOf((byte)255));
                                xArray.add(xMap1);
                            }
                            layout.removeAllViews();
                            layout.addView(getViewByXObject(activty, key, xArray, level));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        } else if (value instanceof XMap) {
            type = "XMap";
            final XMap xMap = (XMap)value;
            List<Byte> keyList = xMap.getKeyList();
            for(int i = 0, size = keyList.size(); i < size; i++) {
                XObject xObject = xMap.getXObject(keyList.get(i));
                View view = getViewByXObject(activty, keyList.get(i), xObject, level+1);
                if (view != null) {
                    layout.addView(view);
                }
            }
            keyTxv.setOnClickListener(new OnClickListener() {
                
                @Override
                public void onClick(View v) {
                    final ViewGroup viewGroup = getViewByXObject(activty, (byte)0, xMap, 0);
                    ScrollView scrollView = new ScrollView(activty);
                    scrollView.addView(viewGroup);
                    Utility.showNormalDialog(activty, scrollView);
                }
            });
            
            if (layout.getChildCount() > 1) {
                TextView removeTxv = new TextView(activty);
                removeTxv.setText("remove");
                removeTxv.setBackgroundResource(R.drawable.btn_default);
                layout.addView(removeTxv);
                final EditText removeEdt = new EditText(activty);
                layout.addView(removeEdt);
                removeTxv.setOnClickListener(new View.OnClickListener() {
                    
                    @Override
                    public void onClick(View v) {
                        byte key1 = (byte)Integer.parseInt(removeEdt.getEditableText().toString());
                        xMap.remove(key1);
                        layout.removeAllViews();
                        layout.addView(getViewByXObject(activty, key, xMap, level));
                    }
                });
                
                TextView addTxv = new TextView(activty);
                addTxv.setText("add");
                addTxv.setBackgroundResource(R.drawable.btn_default);
                layout.addView(addTxv);
                final EditText addEdt = new EditText(activty);
                layout.addView(addEdt);
                addTxv.setOnClickListener(new View.OnClickListener() {
                    
                    @Override
                    public void onClick(View v) {
                        String input = addEdt.getEditableText().toString();
                        try {
                            if (input.startsWith("int")) {
                                String s = input.substring(3);
                                String[] arr = s.split("-");
                                byte key1 = (byte)Integer.parseInt(arr[0]);
                                xMap.put(key1, Integer.parseInt(arr[1]));
                            } else if (input.startsWith("string")) {
                                String s = input.substring(6);
                                String[] arr = s.split("-");
                                byte key1 = (byte)Integer.parseInt(arr[0]);
                                xMap.put(key1, arr[1]);
                            } else if (input.startsWith("array")) {
                                String s = input.substring(5);
                                if (s.startsWith("int")) {
                                    XArray<XInt> xArray = new XArray<XInt>();
                                    xArray.add(XInt.valueOf(0));
                                    byte key1 = (byte)Integer.parseInt(s.substring(3));
                                    xMap.put(key1, xArray);
                                } else if (s.startsWith("string")) {
                                    XArray<XString> xArray = new XArray<XString>();
                                    xArray.add(XString.valueOf("0"));
                                    byte key1 = (byte)Integer.parseInt(s.substring(6));
                                    xMap.put(key1, xArray);                                
                                }
                            } else if (input.startsWith("map")) {
                                String s = input.substring(3);
                                byte key1 = (byte)Integer.parseInt(s);
                                XMap xMap1 = new XMap();
                                xMap1.put((byte)255, XInt.valueOf((byte)255));
                                xMap.put(key1, xMap1);
                            }
                            layout.removeAllViews();
                            layout.addView(getViewByXObject(activty, key, xMap, level));

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
            
        }
        keyTxv.setText(Util.byteToHexString(key)+"    ("+key+")    " + type);
        return layout;
    }
    
    public static void showSetResponseCode(LayoutInflater layoutInflater, final Activity activity) {
        if (TKConfig.ShowTestOption == false) {
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

        final CheckBox modifyRequestData = new CheckBox(activity);
        layout.addView(modifyRequestData);
        final CheckBox modifyResponseData = new CheckBox(activity);
        layout.addView(modifyResponseData);
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
        final Button launchHistoryWorkBtn = new Button(activity);
        layout.addView(launchHistoryWorkBtn, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        
        final LinearLayout viewCityMapVersionLayout = new LinearLayout(activity);
        viewCityMapVersionLayout.setOrientation(LinearLayout.HORIZONTAL);
        Button viewCityMapVersionBtn = new Button(activity);
        viewCityMapVersionLayout.addView(viewCityMapVersionBtn);
        final EditText viewCityMapVersionEdt = new EditText(activity);
        viewCityMapVersionLayout.addView(viewCityMapVersionEdt, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
        layout.addView(viewCityMapVersionLayout);
        
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
                } catch (ClassNotFoundException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
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
                accountManage.setup(criteria, Globals.getCurrentCityInfo().getId());
                accountManage.setTipText(activity.getString(R.string.query_loading_tip));
                if (activity instanceof BaseActivity) {
                    ((BaseActivity)(activity)).queryStart(accountManage, false);
                } else if (activity instanceof Sphinx) {
                    ((Sphinx)(activity)).queryStart(accountManage);
                }
            }
        });
        
        viewCityMapVersionBtn.setText("View city map version");
        viewCityMapVersionEdt.setSingleLine();
        viewCityMapVersionBtn.setOnClickListener(new View.OnClickListener() {
            
            @Override
            public void onClick(View arg0) {
                MapEngine mapEngine = MapEngine.getInstance();
                List<Integer> list = mapEngine.getRegionIdList(viewCityMapVersionEdt.getEditableText().toString());
                StringBuilder s = new StringBuilder();
                if (list != null) {
                    for (int j = list.size()-1; j >= 0; j--) {
                        int id = list.get(j);
                        s.append(id);
                        s.append(": ");
                        RegionMetaVersion regionMetaVersion = mapEngine.getRegionMetaVersion(id);
                        if (regionMetaVersion != null) {
                            s.append(regionMetaVersion.toString());
                        }
                        s.append("\n");
                        
                    }
                }
                Utility.showDialogAcitvity(activity, s.toString());
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
        modifyRequestData.setText("Modify request data");
        modifyRequestData.setTextColor(0xffffffff);
        modifyRequestData.setChecked(TKConfig.ModifyRequestData);
        modifyRequestData.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View arg0) {
                TKConfig.ModifyRequestData = modifyRequestData.isChecked();
            }
        });
        modifyResponseData.setText("Modify response data(v13 service)");
        modifyResponseData.setTextColor(0xffffffff);
        modifyResponseData.setChecked(TKConfig.ModifyResponseData);
        modifyResponseData.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View arg0) {
                TKConfig.ModifyResponseData = modifyResponseData.isChecked();
            }
        });
        launchTestChb.setText("Launch fake data(Bootstrap, FeedbackUpload, DataQuery, DataOperation, AccountManage, ProxyQuery, HoteOrderOperation)");
        launchTestChb.setTextColor(0xffffffff);
        launchTestChb.setChecked(TKConfig.LaunchTest);
        launchTestChb.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View arg0) {
                TKConfig.LaunchTest = launchTestChb.isChecked();
                lunchTestLayout.setVisibility(TKConfig.LaunchTest ? View.VISIBLE : View.GONE);
            }
        });
        responseCodeTxv.setText("ResponseCode:");
        responseCodeTxv.setTextColor(0xffffffff);
        responseCodeEdt.setText("");
        responseCodeEdt.setInputType(InputType.TYPE_CLASS_NUMBER);
        responseCodeEdt.setSingleLine();
        lunchTestLayout.setVisibility(TKConfig.LaunchTest ? View.VISIBLE : View.GONE);
        
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
                TKConfig.setPref(activity, TKConfig.PREFS_SHOW_UPGRADE_COMMENT_TIP, String.valueOf(MoreHomeFragment.SHOW_COMMENT_TIP_TIMES));
            }
        });
        
        updateMapTip.setText("clear updateMapTip");
        updateMapTip.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View arg0) {
                MoreHomeFragment.CurrentDownloadCity = null;
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

        launchHistoryWorkBtn.setText("launch History word");
        launchHistoryWorkBtn.setOnClickListener(new View.OnClickListener() {
            
            @Override
            public void onClick(View v) {
                List<CityInfo> allCityInfoList = MapEngine.getInstance().getAllProvinceCityList(activity.getApplicationContext());  
                for(int i = allCityInfoList.size()-1; i >= 0; i--) {
                    CityInfo cityInfo1 = allCityInfoList.get(i);
                    List<CityInfo> childCityInfoList = cityInfo1.getCityList();
                    if (childCityInfoList.size() > 1) {
//                        for(int ii = childCityInfoList.size()-1; ii >= 0; ii--) {
//                            CityInfo cityInfo2 = childCityInfoList.get(ii);
//                            StringBuilder s = new StringBuilder();
//                            for(int l = 0; l < 51; l++) {
//                                s.append(l);
//                                TKWord tkWord = new TKWord(TKWord.ATTRIBUTE_HISTORY, s.toString(), cityInfo2.getPosition());
//                                HistoryWordTable.addHistoryWord(activity, tkWord, cityInfo2.getId(), HistoryWordTable.TYPE_POI);
//                                HistoryWordTable.addHistoryWord(activity, tkWord, cityInfo2.getId(), HistoryWordTable.TYPE_TRAFFIC);
//                                HistoryWordTable.addHistoryWord(activity, tkWord, cityInfo2.getId(), HistoryWordTable.TYPE_BUSLINE);
//                            }
//                        }
                    } else {
                        StringBuilder s = new StringBuilder();
                        for(int l = 0; l < 51; l++) {
                            s.append(l);
                            TKWord tkWord = new TKWord(TKWord.ATTRIBUTE_HISTORY, s.toString(), cityInfo1.getPosition());
                            HistoryWordTable.addHistoryWord(activity, tkWord, cityInfo1.getId(), HistoryWordTable.TYPE_POI);
                            HistoryWordTable.addHistoryWord(activity, tkWord, cityInfo1.getId(), HistoryWordTable.TYPE_TRAFFIC);
                            HistoryWordTable.addHistoryWord(activity, tkWord, cityInfo1.getId(), HistoryWordTable.TYPE_BUSLINE);
                        }
                    }
                }
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
                            LocationService.LOCATION_FOR_TEST = location;
                        } else {
                            LocationService.LOCATION_FOR_TEST = null;
                        }
                        TKConfig.readConfig();
                        setActivity(activity);
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
                              sphinx.changeCity(c2);
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
                      } catch (InterruptedException e) {
                          // TODO Auto-generated catch block
                          e.printStackTrace();
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
                  } catch (InterruptedException e) {
                      // TODO Auto-generated catch block
                      e.printStackTrace();
                  }
              }
          }
      }).start();
    }
}

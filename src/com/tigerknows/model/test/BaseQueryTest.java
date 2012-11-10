package com.tigerknows.model.test;

import com.decarta.Globals;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.LocationQuery;
import com.tigerknows.model.DataQuery.DiscoverResponse;
import com.tigerknows.model.xobject.XMap;
import com.tigerknows.service.TKLocationManager;
import com.tigerknows.service.TigerknowsLocationManager;
import com.tigerknows.view.StringArrayAdapter;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.location.Location;
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
import android.widget.TextView;
import android.widget.Toast;

public class BaseQueryTest {
    
    static int RESPONSE_CODE = BaseQuery.STATUS_CODE_NETWORK_OK;

    public static XMap launchResponse() {
        return launchResponse(new XMap());
    }

    public static XMap launchResponse(XMap data) {
        data.put(DiscoverResponse.FIELD_RESPONSE_CODE, RESPONSE_CODE);
        data.put(DiscoverResponse.FIELD_DESCRIPTION, "FIELD_DESCRIPTION");
        return  data;
    }
    
    public static void showSetResponseCode(LayoutInflater layoutInflater, final Activity activity) {
        LinearLayout layout = new LinearLayout(activity);
        layout.setOrientation(LinearLayout.VERTICAL);
        final Button clearLocationCacheBtn = new Button(activity);
        layout.addView(clearLocationCacheBtn, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        final CheckBox unallowedLocationChb = new CheckBox(activity);
        layout.addView(unallowedLocationChb);
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
        final CheckBox locationChb = new CheckBox(activity);
        layout.addView(locationChb);
        final EditText locationEdt = new EditText(activity);
        layout.addView(locationEdt, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
        TextView textView = new TextView(activity);
        textView.setText(" ");
        layout.addView(textView);
        clearLocationCacheBtn.setText("Clear Location Cache");
        clearLocationCacheBtn.setOnClickListener(new View.OnClickListener() {
            
            @Override
            public void onClick(View arg0) {
                LocationQuery.getInstance(activity).getLocationCache().clear();
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
                if (locationChb.isChecked()) {
                    responseCodeEdt.showDropDown();
                }
            }
        });
        locationEdt.setText("39.904156,116.397764,1000");
        locationEdt.setVisibility(View.GONE);
        
        AlertDialog dialog = new AlertDialog.Builder(activity)
            .setCancelable(false)
            .setTitle("Test")
            .setView(layout)
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
}

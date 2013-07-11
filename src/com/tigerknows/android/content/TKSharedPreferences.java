package com.tigerknows.android.content;

import com.tigerknows.TKConfig;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.text.TextUtils;

public class TKSharedPreferences {

    
    /**
     * remove preference
     * @param context
     * @param name 名称
     */
    public static void removePref(Context context, String name) {
        if (context == null || TextUtils.isEmpty(name)) {
            return;
        }
        SharedPreferences sharedPreferences = context.getSharedPreferences(TKConfig.TIGERKNOWS_PREFS, Context.MODE_WORLD_WRITEABLE);
        sharedPreferences.edit().remove(name).commit();
    }

    /**
     * set preference
     * @param name 名称
     * @param value 值
     */
    public static void setPref(Context context, String name, String value) {
        if (context == null || TextUtils.isEmpty(name)) {
            return;
        }
        SharedPreferences sharedPreferences = context.getSharedPreferences(TKConfig.TIGERKNOWS_PREFS, Context.MODE_WORLD_WRITEABLE);
        try {
            sharedPreferences.edit().putString(name, value).commit();
        } catch (Exception e) {
            Editor editor = sharedPreferences.edit();
            editor.remove(name).commit();
            editor.putString(name, value).commit();
        }
    }

    /**
     * get preference
     * @param name 名称
     */
    public static String getPref(Context context, String name) {
        return getPref(context, name, null);
    }

    /**
     * get preference
     * @param 名称
     * @defaultValue 默认值
     */
    public static String getPref(Context context, String name, String defaultValue) {
        String value = defaultValue;
        if (context == null || TextUtils.isEmpty(name)) {
            return value;
        }
        SharedPreferences sharedPreferences = context.getSharedPreferences(TKConfig.TIGERKNOWS_PREFS, Context.MODE_WORLD_READABLE);
        try {
            value = sharedPreferences.getString(name, defaultValue);
        } catch (Exception e) {
            Editor editor = sharedPreferences.edit();
            editor.remove(name).commit();
            editor.putString(name, value).commit();
        }
        return value;
    }
}
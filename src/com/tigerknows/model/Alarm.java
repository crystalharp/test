package com.tigerknows.model;

import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.android.location.Position;
import com.tigerknows.provider.Tigerknows;
import com.tigerknows.service.AlarmService;
import com.tigerknows.ui.alarm.AlarmShowActivity;
import com.tigerknows.ui.more.SettingActivity;
import com.tigerknows.util.SqliteWrapper;
import com.tigerknows.util.Utility;

import android.app.Activity;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.TextUtils;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class Alarm implements Parcelable {
    
    public static int MIN_DISTANCE = 200;
    
    private static List<Alarm> sAlarmList = new ArrayList<Alarm>();
    private static int sEnabledCount = 0;
    
    private static Alarm sWaitAlarm = null;
    private static int sShowToastResId = 0;
    
    public static int getShowToastResId() {
        return sShowToastResId;
    }
    
    public static Alarm getWaitAlarm() {
        return sWaitAlarm;
    }
    
    public static void resetWaitAlarm() {
        sWaitAlarm = null;
        sShowToastResId = 0;
    }
    
    public static List<Alarm> getAlarmListForNoRead() {
        return sAlarmList;
    }
    
    public static List<Alarm> getAlarmList(Context context) {
        synchronized (sAlarmList) {
            if (sAlarmList.size() == 0) {
                sAlarmList.addAll(readFromDatabases(context));
            }
            checkStatus(context, true);
            return sAlarmList;
            
        }
    }
    
    public static void writeAlarm(Context context, Alarm alarm) {
        writeAlarm(context, alarm, 0);
    }
    
    public static void writeAlarm(Context context, Alarm alarm, int showToastResId) {
    
        synchronized (sAlarmList) {
            List<Alarm> list = getAlarmList(context);
            boolean enabled = (alarm.getStatus() == 0);
            if (enabled) {
                if (SettingActivity.checkGPS(context) == false && context instanceof Sphinx && showToastResId != 0) {
                    alarm.setStatus(1);
                    showSettingLocationDialog((Sphinx) context, alarm, showToastResId);
                    return;
                }
            }
            
            Alarm exist = null;
            for(int i = list.size() - 1; i >= 0; i--) {
                Alarm element = list.get(i);
                if (element == alarm ||
                        (element.getName().equals(alarm.getName()) && Position.distanceBetween(element.position, alarm.position) <= MIN_DISTANCE)) {
                    exist = element;
                }
            }
            
            boolean add = false;
            if (exist != null) {
                if (showToastResId > 0) {
                    deleteAlarm(context, exist);
                    alarm.writeToDatabases(context);
                    list.add(0, alarm);
                    add = true;
                } else {
                    exist.setPosition(alarm.getPosition());
                    exist.setRange(alarm.getRange());
                    exist.setRingtone(alarm.getRingtone());
                    exist.setRingtoneName(alarm.getRingtoneName());
                    exist.setStatus(alarm.getStatus());
                    exist.updateToDatabases(context);
                }
            } else {
                alarm.writeToDatabases(context);
                list.add(0, alarm);
                add = true;
            }
            checkStatus(context, add);

            if (enabled && showToastResId > 0 && context instanceof Sphinx) {
                Toast.makeText(context, showToastResId, Toast.LENGTH_SHORT).show();

                Sphinx sphinx = (Sphinx) context;
                if (showToastResId == R.string.alarm_add_success && sphinx.uiStackContains(R.id.view_alarm_list)) {
                    sphinx.uiStackClearTop(R.id.view_alarm_list);
                }
            }
        }
    }
    
    public static void deleteAlarm(Context context, Alarm alarm) {
        synchronized (sAlarmList) {
            List<Alarm> list = getAlarmList(context);
            list.remove(alarm);
            SqliteWrapper.delete(context, context.getContentResolver(), ContentUris.withAppendedId(Tigerknows.Alarm.CONTENT_URI, alarm.id), null, null);
            checkStatus(context, false);
        }
    }
    
    private static void checkStatus(Context context, boolean recheck) {
        sEnabledCount = 0;
        for(int i = 0, size = sAlarmList.size(); i < size; i++) {
            if (sAlarmList.get(i).getStatus() == 0) {
                sEnabledCount++;
                break;
            }
        }

        if (sEnabledCount > 0) {
            AlarmService.start(context, recheck);
        } else {
            AlarmService.stop(context);
        }
    }
    
    public static void showAlarm(Context context, Position position) {
        if (position == null) {
            return;
        }
        ArrayList<Alarm> list = new ArrayList<Alarm>();
        synchronized (sAlarmList) {
            for(int i = 0, size = sAlarmList.size(); i < size; i++) {
                Alarm alarm = sAlarmList.get(i);
                if (alarm.getStatus() == 0 &&
                        Position.distanceBetween(position, alarm.getPosition()) <= alarm.getRange()) {
                    alarm.setStatus(1);
                    Alarm.writeAlarm(context, alarm);
                    list.add(alarm);
                }
            }
        }
        
        if (list.size() > 0) {
            Intent intent = new Intent(context, AlarmShowActivity.class);
            intent.putParcelableArrayListExtra(AlarmShowActivity.EXTRA_ALARM_LIST, list);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }

    private long id;
    private String name;
    private Position position;
    private int range = 1000;
    private int status = 0;
    private Uri ringtone;
    private String ringtoneName;
    
    public long getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public Position getPosition() {
        return position;
    }
    
    public void setPosition(Position position) {
        this.position = position;
    }
    
    public int getRange() {
        return range;
    }
    
    public void setRange(int range) {
        this.range = range;
    }
    
    public int getStatus() {
        return status;
    }
    
    public void setStatus(int status) {
        this.status = status;
    }
    
    public Uri getRingtone() {
        return ringtone;
    }
    
    public void setRingtone(Uri uri) {
        this.ringtone = uri;
    }

    public String getRingtoneName() {
        return ringtoneName;
    }
    
    public void setRingtoneName(String name) {
        this.ringtoneName = name;
    }

    private Alarm() {
    }

    public Alarm(Context context) {
        ringtone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        ringtoneName = getRingtoneName(context, ringtone);
    }

    private Uri writeToDatabases(Context context) {
        ContentValues values = buildContentValues();
        Uri uri = SqliteWrapper.insert(context, context.getContentResolver(), Tigerknows.Alarm.CONTENT_URI, values);
        id = Long.parseLong(uri.getPathSegments().get(1));
        return uri;
    }

    private int updateToDatabases(Context context) {
        ContentValues values = buildContentValues();
        return SqliteWrapper.update(context, context.getContentResolver(), ContentUris.withAppendedId(Tigerknows.Alarm.CONTENT_URI, id), values, null, null);
    }
    
    private ContentValues buildContentValues() {
        ContentValues values = new ContentValues();
        values.put(Tigerknows.Alarm.NAME, name);
        values.put(Tigerknows.Alarm.POSITION, position.getLat()+","+position.getLon());
        values.put(Tigerknows.Alarm.RANGE, range);
        values.put(Tigerknows.Alarm.RINGTONE, ringtone != null ? ringtone.toString() : null);
        values.put(Tigerknows.Alarm.RINGTONE_NAME, ringtoneName);
        values.put(Tigerknows.Alarm.STATUS, status);
        return values;
    }
    
    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        
        if (null == object) {
            return false;
        }
        
        if (getClass() != object.getClass()) {
            return false;
        }
        
        Alarm other = (Alarm) object;
        
        if(null != name && name.equals(other.name) && 
                null != position && position.equals(other.position)) {
                return true;
        } else {
            return false;
        }
    }
    
    @Override
    public int hashCode() {
        int hash = 3;
        hash = 29 * hash + (this.name != null ? this.name.hashCode() : 0);
        hash = 29 * hash + (this.position != null ? this.position.hashCode() : 0);
        return hash;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeLong(id);
        parcel.writeString(name);
        parcel.writeParcelable(position, flags);
        parcel.writeInt(range);
        parcel.writeParcelable(ringtone, flags);
        parcel.writeString(ringtoneName);
        parcel.writeInt(status);
    }

    public static final Parcelable.Creator<Alarm> CREATOR
            = new Parcelable.Creator<Alarm>() {
        public Alarm createFromParcel(Parcel in) {
            return new Alarm(in);
        }

        public Alarm[] newArray(int size) {
            return new Alarm[size];
        }
    };
    
    private Alarm(Parcel in) {
        id = in.readLong();
        name = in.readString();
        position = in.readParcelable(Position.class.getClassLoader());
        range = in.readInt();
        ringtone = in.readParcelable(Uri.class.getClassLoader());
        ringtoneName = in.readString();
        status = in.readInt();
    }
    
    /**
     * 从数据库中读取Alarm
     * 
     * @param id
     */
    private static Alarm readFromDatabases(Context context, long id) {
        Alarm result = null;
        Cursor c = SqliteWrapper.query(context, context.getContentResolver(), ContentUris
                .withAppendedId(Tigerknows.Alarm.CONTENT_URI, id), null, null, null, null);
        if (c != null) {
            if (c.getCount() > 0) {
                c.moveToFirst();
                result = readFromCursor(context, c);
            }
            c.close();
        }
        return result;
    }

    private static List<Alarm> readFromDatabases(Context context) {
        List<Alarm> list = new ArrayList<Alarm>();
        Cursor c = SqliteWrapper.query(context, context.getContentResolver(),
                Tigerknows.Alarm.CONTENT_URI, null, null, null, Tigerknows.Alarm.DEFAULT_SORT_ORDER);
        if (c != null) {
            if (c.getCount() > 0) {
                c.moveToFirst();
                for (int i = 0; i < c.getCount(); i++) {
                    list.add(readFromCursor(context, c));
                    c.moveToNext();
                }
            }
            c.close();
        }
        return list;
    }

    private static Alarm readFromCursor(Context context, Cursor cursor) {
        Alarm result = new Alarm();
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                result.id = cursor.getLong(cursor.getColumnIndex(Tigerknows.Alarm._ID));
                result.name = cursor.getString(cursor.getColumnIndex(Tigerknows.Alarm.NAME));
                String str = cursor.getString(cursor.getColumnIndex(Tigerknows.Alarm.POSITION));
                if (TextUtils.isEmpty(str) == false) {
                    String[] arr = str.split(",");
                    try {
                        double lat = Double.parseDouble(arr[0]);
                        double lon = Double.parseDouble(arr[1]);
                        result.position = new Position(lat, lon);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                result.range = cursor.getInt(cursor.getColumnIndex(Tigerknows.Alarm.RANGE));
                str = cursor.getString(cursor.getColumnIndex(Tigerknows.Alarm.RINGTONE));
                if (TextUtils.isEmpty(str) == false) {
                    result.ringtone = Uri.parse(str);
                }
                result.ringtoneName = cursor.getString(cursor.getColumnIndex(Tigerknows.Alarm.RINGTONE_NAME));
                result.status = cursor.getInt(cursor.getColumnIndex(Tigerknows.Alarm.STATUS));
            }
        }

        return result;
    }
    
    public static String getRingtoneName(Context context, Uri uri) {
        String summary = null;
        if (uri == null) {
            summary = context.getString(R.string.ringtone_silent);
            return summary;
        }
        Ringtone ringtone = RingtoneManager.getRingtone(context, uri);
        if (ringtone != null) {
            summary = ringtone.getTitle(context);
        }
//        // Fetch the ringtone title from the media provider
//        try {
//            Cursor cursor = context.getContentResolver().query(uri,
//                    new String[] { MediaStore.Audio.Media.TITLE }, null, null, null);
//            if (cursor != null) {
//                if (cursor.moveToFirst()) {
//                    summary = cursor.getString(0);
//                }
//                cursor.close();
//            }
//        } catch (SQLiteException sqle) {
//            // Unknown title for the ringtone
//        }
        
        return summary;
    }
    
    public static Uri getActualDefaultRingtoneUri(Context context) {
        // 根据指定的类型获取Settings类中对应的类型
        String setting = Settings.System.ALARM_ALERT;
        if (setting == null)
            return null;
        // 调用Settings类中静态内部类System中的相应方法
        final String uriString = Settings.System.getString(context.getContentResolver(), setting);
        return uriString != null ? Uri.parse(uriString) : null;
    }
    
    public static void pickRingtone(Activity activity, Uri uri, int requestCode) {
        Intent intent = new Intent();
        intent.setAction(RingtoneManager.ACTION_RINGTONE_PICKER);    
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, Settings.System.DEFAULT_ALARM_ALERT_URI);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, uri);
        activity.startActivityForResult(intent, requestCode);
    }
    
    private static void showSettingLocationDialog(final Sphinx activity, final Alarm alarm, final int showToastResId) {
        Utility.showNormalDialog(activity,
                                 activity.getString(R.string.prompt),
                                 activity.getString(R.string.alarm_enable_tip),
                                 activity.getString(R.string.settings),
                                 activity.getString(R.string.cancel),
                                 new DialogInterface.OnClickListener() {
                    
                                     @Override
                                     public void onClick(DialogInterface arg0, int id) {
                                         if (id == DialogInterface.BUTTON_POSITIVE) {
                                             sWaitAlarm = alarm;
                                             sShowToastResId = showToastResId;
                                             activity.showView(R.id.activity_setting_location);
                                         }
                                     }
                                 }
                                 );
    }
}

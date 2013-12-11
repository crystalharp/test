package com.tigerknows.model;

import android.telephony.TelephonyManager;

/**
 * 基站信息类
 * @author pengwenyue
 *
 */
public class TKCellLocation {
    
    public int phoneType = TelephonyManager.PHONE_TYPE_NONE;
    public int lac = -1;
    public int cid = -1;
    public int signalStrength = Integer.MAX_VALUE;
    private volatile int hashCode = 0;
    
    public TKCellLocation(int phoneType, int lac, int cid, int signalStrength) {
        this.phoneType = phoneType;
        this.lac = lac;
        this.cid = cid;
        this.signalStrength = signalStrength;
    }
    
    public TKCellLocation(String str) {
        if (str != null) {
            String[] arr = str.split(",");
            try {
                phoneType = Integer.parseInt(arr[0]);
                lac = Integer.parseInt(arr[1]);
                cid = Integer.parseInt(arr[2]);
                signalStrength = Integer.parseInt(arr[3]);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    @Override
    public boolean equals(Object object) {
        if (object == null) {
            return false;
        }
        if (object instanceof TKCellLocation) {
            TKCellLocation other = (TKCellLocation) object;
            if (phoneType == other.phoneType
                    && lac == other.lac
                    && cid == other.cid) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        if (hashCode == 0) {
            int result = 17;
            result = 37*result + phoneType;
            result = 37*result + lac;
            result = 37*result + cid;
            hashCode = result;
        }
        return hashCode;
    }
    
    public String toString() {
        return phoneType+","+lac+","+cid+","+signalStrength;
    }

}

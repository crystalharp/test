/**
 * deCarta Android Mapping API
 * deCarta confidential and proprietary.
 * Copyright deCarta. All rights reserved.
 */

package com.decarta.android.location;

import com.tigerknows.model.Zhanlan;
import com.tigerknows.service.TigerknowsLocationManager;

import android.location.Location;
import android.location.LocationManager;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

/**
 * immutable position object
 */
public class Position implements Serializable, Parcelable{

	private static final long serialVersionUID = 1L;
	private double lat;
	private double lon;
    private float accuracy=0;
    private int type = 2; //0代表来源GPS，1代表来源基站或wifi，2代表不知道来源 
	public Position(double lat, double lon){
		this(lat, lon, 0);
	}

    public Position(double lat, double lon, float accuracy){
        lat=(lat+90+180)%180-90;
        lon=(lon+180+360)%360-180;                  
        this.lat=lat;
        this.lon=lon;
        this.accuracy = accuracy;
    }
    
	public Position(String latlon){
		int index1=-1;
		if(latlon.indexOf(",")>-1){
			index1=latlon.indexOf(",");
			
		}else if(latlon.indexOf(" ")>-1){
			index1=latlon.indexOf(" ");
		}
		
		this.lat = Double.parseDouble(latlon.substring(0, index1));
		this.lat = (this.lat + 90 + 180) % 180 - 90;
		this.lon = Double.parseDouble(latlon.substring(index1 + 1));
		this.lon = (this.lon + 180 + 360) % 360 - 180;	
	}

	public double getLat() {
		return lat;
	}

	public double getLon() {
		return lon;
	}
	
	@Override
	public Position clone(){
		return new Position(this.lat,this.lon, this.accuracy);
	}
	@Override
	public String toString(){
		return lat+" "+lon;
	}
	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Position other = (Position) obj;
		if(other.lat==this.lat && other.lon==this.lon)
			return true;
		else
			return false;
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 97 * hash + (int) (Double.doubleToLongBits(this.lat) ^ (Double.doubleToLongBits(this.lat) >>> 32));
		hash = 97 * hash + (int) (Double.doubleToLongBits(this.lon) ^ (Double.doubleToLongBits(this.lon) >>> 32));
		return hash;
	}

    
    public float getAccuracy(){
        return accuracy;
    }
    
    public void setAccuracy(float accuracy){
        this.accuracy = accuracy;
    }
    
    public void setProvider(String provider) {
        if (LocationManager.GPS_PROVIDER.equals(provider)) {
            type = 0;
        } else if (TigerknowsLocationManager.TIGERKNOWS_PROVIDER.equals(provider)) {
            type = 1;
        } else {
            type = 2;
        }
    }
    
    public int getType() {
        return type;
    }
    
    public static int distanceBetween(Position position1, Position position2) {
        if (position1 == null || position2 == null) {
            return Integer.MAX_VALUE;
        }
        float[] results = new float[1];
        Location.distanceBetween(position1.getLat(), position1.getLon(), position2.getLat(), position2.getLon(), results);
        return (int)results[0];
    }
    
    public Position(Parcel in){
		this.lat = in.readDouble();
		this.lon = in.readDouble();
		this.accuracy = in.readFloat();
		this.type = in.readInt();
    }

    public static final Parcelable.Creator<Position> CREATOR
		    = new Parcelable.Creator<Position>() {
		public Position createFromParcel(Parcel in) {
		    return new Position(in);
		}
		
		public Position[] newArray(int size) {
		    return new Position[size];
		}
	};
	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeDouble(this.lat);
		dest.writeDouble(this.lon);
		dest.writeFloat(this.accuracy);
		dest.writeInt(this.type);
	}
	
	
	
	
}

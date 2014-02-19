package com.tigerknows.android.location;

import android.location.Location;

public class TKLocation {

    /**
     * Computes the approximate distance in meters between two
     * locations, and optionally the initial and final bearings of the
     * shortest path between them.  Distance and bearing are defined using the
     * WGS84 ellipsoid.
     *
     * <p> The computed distance is stored in results[0].  If results has length
     * 2 or greater, the initial bearing is stored in results[1]. If results has
     * length 3 or greater, the final bearing is stored in results[2].
     *
     * @param startLatitude the starting latitude
     * @param startLongitude the starting longitude
     * @param endLatitude the ending latitude
     * @param endLongitude the ending longitude
     * @param results an array of floats to hold the results
     *
     * @throws IllegalArgumentException if results is null or has length < 1
     */
    public static void distanceBetween(double startLatitude, double startLongitude,
        double endLatitude, double endLongitude, float[] results) {
        if (results == null || results.length < 1) {
            throw new IllegalArgumentException("results is null or has length < 1");
        }
        Location.distanceBetween(startLatitude, startLongitude, endLatitude, endLongitude, results);
        
        // android.location.Locatoin.computeDistanceAndBearing()方法存在bug,所以采用简单公式计算两点之间的距离
        // earth's mean radius in KM  
        double r = 6378.137;  
        startLatitude = Math.toRadians(startLatitude);  
        startLongitude = Math.toRadians(startLongitude);  
        endLatitude = Math.toRadians(endLatitude);  
        endLongitude = Math.toRadians(endLongitude);  
        double d1 = Math.abs(startLatitude - endLatitude);  
        double d2 = Math.abs(startLongitude - endLongitude);  
        double p = Math.pow(Math.sin(d1 / 2), 2) + Math.cos(startLatitude)  
                * Math.cos(endLatitude) * Math.pow(Math.sin(d2 / 2), 2);  
        double dis = r * 2 * Math.asin(Math.sqrt(p));  
        results[0] = (float)(dis*1000);
    }
}

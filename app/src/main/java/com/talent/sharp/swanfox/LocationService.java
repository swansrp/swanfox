package com.talent.sharp.swanfox;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

public class LocationService extends Service {
    private final String TAG = "OhEunSook_" + LocationService.class.getSimpleName();
    private Handler displayHandler = null;
    private LocalBinder mBinder = null;
    private LocationManager locationManager;
    private TelephonyManager telephonyManager;
    private AlarmManager alarmManager;
    private final String ALARM_GET_GPS_LOCATION = "ALARM_GET_GPS_LOCATION";
    private final String ALARM_GET_NETWORK_LOCATION = "ALARM_GET_NETWORK_LOCATION";
    private final String MSG_GET_GPS_LOCATION = "MSG_GET_GPS_LOCATION";
    private final String MSG_GET_NETWORK_LOCATION = "MSG_GET_NETWORK_LOCATION";
    private Map<String, LocationListener> locationListenerMap = new HashMap<String, LocationListener>();
    private Map<String, Location> curLocationMap = new HashMap<String, Location>();

    private void updateDisplayLocation() {
        for(Map.Entry<String, Location> entry: curLocationMap.entrySet()) {
            updateDisplayLocation(entry.getKey());
        }
    }
    private void updateDisplayLocation(String provider) {
        if(displayHandler!=null) {
            int msgType = -1;
            if(provider.equals(LocationManager.GPS_PROVIDER)) {
                msgType = MainActivity.MSG_UPDATE_GPS_LOCATION;
            } else if(provider.equals(LocationManager.NETWORK_PROVIDER)){
                msgType = MainActivity.MSG_UPDATE_NW_LOCATION;
            }
            displayHandler.sendMessage(displayHandler.obtainMessage(
                    msgType, curLocationMap.get(provider)));
        } else {
          Log.d(TAG, "There is no Display Handler");
        }
    }

    private void requestPermissions(String[] permissions) {
        displayHandler.sendMessage(displayHandler.obtainMessage(
                MainActivity.MSG_REQ_LOCATION_PERMISSION, permissions));
    }

    protected final LocationListener gpsLocationListener = new LocationListener() {

        // 当位置发生变化时，输出位置信息
        public void onLocationChanged(Location location) {
            Log.d(TAG, "GPS Location changed to: " + getLocationInfo(location));
            curLocationMap.put(LocationManager.GPS_PROVIDER, location);
            updateDisplayLocation(LocationManager.GPS_PROVIDER);
        }

        public void onProviderDisabled(String provider) {
            Log.d(TAG, provider + " disabled.");
        }

        public void onProviderEnabled(String provider) {
            Log.d(TAG, provider + " enabled.");
        }

        public void onStatusChanged(String provider, int status,
                                    Bundle extras){
            Log.d(TAG, provider + " status changed.");
        }
    };
    protected final LocationListener nwLocationListener = new LocationListener() {

        // 当位置发生变化时，输出位置信息
        public void onLocationChanged(Location location) {
            Log.d(TAG, "Network Location changed to: " + getLocationInfo(location));
            curLocationMap.put(LocationManager.NETWORK_PROVIDER, location);
            updateDisplayLocation(LocationManager.NETWORK_PROVIDER);
        }

        public void onProviderDisabled(String provider) {
            Log.d(TAG, provider + " disabled.");
        }

        public void onProviderEnabled(String provider) {
            Log.d(TAG, provider + " enabled.");
        }

        public void onStatusChanged(String provider, int status,
                                    Bundle extras){
            Log.d(TAG, provider + " status changed.");
        }
    };

    private void regLocation(String currentProvider) {
        Log.d(TAG, "Start Provider: " + currentProvider);
        // 获取 Provider 最后一个记录的地址信息
        Location lastKnownLocation = null;
        try {
            // 注册监听器接受位置更新
            lastKnownLocation = locationManager.getLastKnownLocation(currentProvider);
            if(lastKnownLocation != null){
                curLocationMap.put(currentProvider, lastKnownLocation);
                updateDisplayLocation(currentProvider);
            }
            LocationListener ll = locationListenerMap.get(currentProvider);
            if(ll != null) {
                locationManager.requestLocationUpdates(currentProvider, 0, 0, ll);
            } else {
                Log.e(TAG, "Warning, this kind of provider [" + currentProvider + "] is not existed");
            }
        } catch (SecurityException e) {
            requestPermissions(new String[] {"android.permission.ACCESS_FINE_LOCATION","android.permission.ACCESS_COARSE_LOCATION"});
        }
    }

    private void deregLocation(String currentProvider) {
        Log.d(TAG, "Stop Provider: " + currentProvider);
        LocationListener ll = locationListenerMap.get(currentProvider);
        if(ll != null) {
            locationManager.removeUpdates(ll);
        } else {
            Log.e(TAG, "Warning, this kind of provider [" + currentProvider + "] is not registered");
        }
        if(currentProvider.equals(LocationManager.GPS_PROVIDER)) {
            cancelAlarm(ALARM_GET_GPS_LOCATION);
        }else if(currentProvider.equals(LocationManager.NETWORK_PROVIDER)) {
            cancelAlarm(ALARM_GET_NETWORK_LOCATION);
        }
    }

    private void requestSingleLocation(String provider) {
        Log.d(TAG, "request Single Location <" + provider + ">");
        try {
            LocationListener ll = locationListenerMap.get(provider);
            if(ll != null) {
                locationManager.requestSingleUpdate(provider, ll, null);
            } else {
                Log.e(TAG, "Warning, this kind of provider [" + provider + "] is not existed");
            }
        } catch (SecurityException e) {
            requestPermissions(new String[] {"android.permission.ACCESS_FINE_LOCATION","android.permission.ACCESS_COARSE_LOCATION"});
        }
    }

    /**
     * 将 Location 对象转换成字符串形式方便显示
     *
     * @param location
     *            Location 对象
     * @return 字符串形式的表示
     */
    private String getLocationInfo(Location location) {
        String info = "";
        info += "Longitude:" + location.getLongitude();
        info += ", Latitude:" + location.getLatitude();
        if (location.hasAltitude()) {
            info += ", Altitude:" + location.getAltitude();
        }
        if (location.hasBearing()) {
            info += ", Bearing:" + location.getBearing();
        }
        return info;
    }

    private void startAlarm(String action, long time){
        Log.d(TAG, "Set Alarm action: " + action + " time: " + time + "ms");
        Intent intent = new Intent(action);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() +
                        time, alarmIntent);
    }

    private void startAlarm(String action, long startTime, long intervalTime){
        Log.d(TAG, "Set Alarm action: " + action + " time: "  + " period: " + intervalTime + "ms");
        Intent intent = new Intent(action);
        if(intervalTime > 0) {
            intent.putExtra("period", intervalTime);
        }
        if(startTime <= 0) {
            startTime = intervalTime;
        }
        PendingIntent alarmIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() +
                        startTime, alarmIntent);
    }

    private void cancelAlarm(String action){
        Log.d(TAG, "Cancel Alarm action: " + action);
        Intent intent = new Intent(action);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.cancel(alarmIntent);
    }

    public LocationService() {
        locationListenerMap.put(LocationManager.NETWORK_PROVIDER, nwLocationListener);
        locationListenerMap.put(LocationManager.GPS_PROVIDER, gpsLocationListener);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // 获取 LocationManager
        locationManager = (LocationManager)getSystemService(LOCATION_SERVICE);
        telephonyManager = (TelephonyManager)getSystemService(TELEPHONY_SERVICE);
        alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);
        mBinder = new LocalBinder();

        registerLocationReceiver();


    }
    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    public class LocalBinder extends Binder {
        LocationService getService() {
            return LocationService.this;
        }
        void regLocationUpdateCallBack(Handler handler) {
            displayHandler = handler;
        }

        void startLocationUpdate(String provider) {
            regLocation(provider);
        }

        void stopLocationUpdate(String provider) {
            deregLocation(provider);
        }

        void setLocationFeq(String provider,int time) {
            Log.d(TAG, "setLocationFeq time: " + time + "s");
            deregLocation(provider);
            if(time == 0) {
                regLocation(provider);
            } else {
                if (provider.equals(LocationManager.GPS_PROVIDER)) {
                    startAlarm(ALARM_GET_GPS_LOCATION, 0,time * 1000);
                } else if (provider.equals(LocationManager.NETWORK_PROVIDER)) {
                    startAlarm(ALARM_GET_NETWORK_LOCATION, 0,time * 1000);
                }
            }
        }

        Location getCurLocation(String provider) {
            return curLocationMap.get(provider);
        }
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent == null) return;
            Log.d(TAG,"Receive: " + intent.getAction());
            switch (intent.getAction())
            {
                case ALARM_GET_GPS_LOCATION:
                    requestSingleLocation(LocationManager.GPS_PROVIDER);
                    if(intent.hasExtra("period")) {
                        startAlarm(ALARM_GET_GPS_LOCATION, 0,intent.getLongExtra("period",0));
                    }
                    break;
                case ALARM_GET_NETWORK_LOCATION:
                    requestSingleLocation(LocationManager.NETWORK_PROVIDER);
                    if(intent.hasExtra("period")) {
                        startAlarm(ALARM_GET_NETWORK_LOCATION, 0,intent.getLongExtra("period",0));
                    }
                    break;
                case "android.net.conn.CONNECTIVITY_CHANGE":
                    Log.d(TAG,"==============");
                    break;
            }
        }
    };


    private void registerLocationReceiver() {

        IntentFilter intentFilter = new IntentFilter();
        //设置接收广播的类型
        intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        intentFilter.addAction(ALARM_GET_GPS_LOCATION);
        intentFilter.addAction(ALARM_GET_NETWORK_LOCATION);
        intentFilter.addAction(MSG_GET_GPS_LOCATION);
        intentFilter.addAction(MSG_GET_NETWORK_LOCATION);
        //调用Context的registerReceiver（）方法进行动态注册
        registerReceiver(mBroadcastReceiver, intentFilter);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mBroadcastReceiver);
        super.onDestroy();
    }
}

package com.talent.sharp.swanfox;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import com.igexin.sdk.PushManager;
import com.umeng.message.IUmengRegisterCallback;
import com.umeng.message.PushAgent;

public class MainActivity extends AppCompatActivity {

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }
    private String TAG = "OhEunSook_" + this.getClass().getSimpleName();
    private Switch swGePush;
    private Switch swJiPush;
    private Switch swUmengPush;
    private EditText etLocationFeq;
    private Button btnLocation;
    private Switch swGps;
    private Switch swNetwork;
    private TextView tvLongitude;
    private TextView tvLatitude;
    private TextView tvCellId;
    private TextView tvLac;
    private TextView tvSysId;
    private TextView tvNwId;
    private TextView tvBaseId;
    private Toolbar toolbar;
    private FloatingActionButton fab;



    private boolean mIsLocationBound = false;
    private LocationService.LocalBinder mLocationBinder = null;



    public final static int MSG_UPDATE_LOCATION = 0;
    public final static int MSG_REQ_LOCATION_PERMISSION = 1;

    private enum EnumPush{
        GE_PUSH,
        J_PUSH,
        UMENG_PUSH
    }
    private void initView() {
        setContentView(R.layout.activity_main);
        swGePush = (Switch) findViewById(R.id.sw_ge_push);
        swJiPush = (Switch) findViewById(R.id.sw_j_push);
        swUmengPush = (Switch) findViewById(R.id.sw_umeng_push);
        etLocationFeq = (EditText) findViewById(R.id.et_location_feq);
        btnLocation = (Button) findViewById(R.id.btn_location_feq);
        swGps = (Switch) findViewById(R.id.sw_gps);
        swNetwork = (Switch) findViewById(R.id.sw_nw);
        tvLongitude = (TextView) findViewById(R.id.tv_longitude);
        tvLatitude = (TextView) findViewById(R.id.tv_latitude);
        tvCellId = (TextView) findViewById(R.id.tv_cellid);
        tvLac = (TextView) findViewById(R.id.tv_lac);
        tvSysId = (TextView) findViewById(R.id.tv_sid);
        tvNwId = (TextView) findViewById(R.id.tv_nid);
        tvBaseId = (TextView) findViewById(R.id.tv_bid);

        swGePush.setChecked(getSwitchPreference(swGePush));
        swJiPush.setChecked(getSwitchPreference(swJiPush));
        swUmengPush.setChecked(getSwitchPreference(swUmengPush));

        swGps.setChecked(getSwitchPreference(swGps));
        swNetwork.setChecked(getSwitchPreference(swNetwork));

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
    }

    private String getPreference(String tag, String defaultValue) {
        SharedPreferences preferences = getSharedPreferences("config",Context.MODE_PRIVATE);
        return preferences.getString(tag, defaultValue);
    }

    private void setPreference(String tag, String value) {
        SharedPreferences preferences = getSharedPreferences("config",Context.MODE_PRIVATE);
        SharedPreferences.Editor editor=preferences.edit();
        editor.putString(tag, value);
        editor.apply();
        editor.commit();
    }

    private boolean getSwitchPreference(Switch sw) {
        SharedPreferences preferences = getSharedPreferences("config",Context.MODE_PRIVATE);
        if(sw == swGePush) {
            return preferences.getBoolean("GE_PUSH", false);
        } else if(sw == swJiPush) {
            return preferences.getBoolean("JI_PUSH", false);
        } else if(sw == swUmengPush) {
            return preferences.getBoolean("UMENG_PUSH", false);
        } else if(sw == swGps) {
            return preferences.getBoolean("GPS_LOCATION", false);
        } else if(sw == swNetwork) {
            return preferences.getBoolean("NETWORK_LOCATION", false);
        }
        return false;
    }
    private void setSwitchPreference(Switch sw, boolean enable) {
        SharedPreferences preferences = getSharedPreferences("config",Context.MODE_PRIVATE);
        SharedPreferences.Editor editor=preferences.edit();
        if(sw == swGePush) {
            editor.putBoolean("GE_PUSH", enable);
        } else if(sw == swJiPush) {
            editor.putBoolean("JI_PUSH", enable);
        } else if(sw == swUmengPush) {
            editor.putBoolean("UMENG_PUSH", enable);
        } else if(sw == swGps) {
            editor.putBoolean("GPS_LOCATION", enable);
        } else if(sw == swNetwork) {
            editor.putBoolean("NETWORK_LOCATION", enable);
        }
        editor.apply();
        editor.commit();
    }
    private void disablePush(EnumPush category) {
        switch(category) {
            case GE_PUSH:
                break;
            case J_PUSH:
                break;
            case UMENG_PUSH:
                break;
            default:
                break;
        }
    }

    private void enablePush(EnumPush category) {
        switch(category) {
            case GE_PUSH:

                break;
            case J_PUSH:
                break;
            case UMENG_PUSH:
                PushAgent mPushAgent = PushAgent.getInstance(getApplication());
                //注册推送服务，每次调用register方法都会回调该接口
                mPushAgent.register(new IUmengRegisterCallback() {
                    @Override
                    public void onSuccess(String deviceToken) {
                        //注册成功会返回device token
                        Log.d(TAG, "My device token is: " + deviceToken);
                    }
                    @Override
                    public void onFailure(String s, String s1) {

                    }
                });
                break;
            default:
                break;
        }
    }

    private void initPushService() {
        swGePush.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setSwitchPreference(swGePush, isChecked);
                if(isChecked) {
                    enablePush(EnumPush.GE_PUSH);
                } else {
                    disablePush(EnumPush.GE_PUSH);
                }
            }
        });
        swJiPush.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setSwitchPreference(swJiPush, isChecked);
                if(isChecked) {
                    enablePush(EnumPush.J_PUSH);
                } else {
                    disablePush(EnumPush.J_PUSH);
                }
            }
        });
        swUmengPush.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setSwitchPreference(swUmengPush, isChecked);
                if(isChecked) {
                    enablePush(EnumPush.UMENG_PUSH);
                } else {
                    disablePush(EnumPush.UMENG_PUSH);
                }
            }
        });
    }

    private void initLocationService() {
        bindLocationService();
        swGps.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setSwitchPreference(swGps, isChecked);
                if(isChecked) {
                    mLocationBinder.startLocationUpdate(LocationManager.GPS_PROVIDER);
                } else {
                    mLocationBinder.stopLocationUpdate(LocationManager.GPS_PROVIDER);
                }
            }
        });
        swNetwork.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setSwitchPreference(swNetwork, isChecked);
                if(isChecked) {
                    mLocationBinder.startLocationUpdate(LocationManager.NETWORK_PROVIDER);
                } else {
                    mLocationBinder.stopLocationUpdate(LocationManager.NETWORK_PROVIDER);
                }
            }
        });
        btnLocation.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String defaultFeqTime = getPreference("FEQ_TIME", "30");
                String feqTime = etLocationFeq.getText().toString();
                if(feqTime.equals("")) {
                    etLocationFeq.setText(defaultFeqTime);
                }
                feqTime = etLocationFeq.getText().toString();
                setPreference("FEQ_TIME",feqTime);
                int time = Integer.valueOf(feqTime);
                if(mLocationBinder!=null) {
                    Location loc = mLocationBinder.getCurLocation();
                    if(loc != null) {
                        mHandler.sendMessage(mHandler.obtainMessage(MSG_UPDATE_LOCATION, loc));
                    }
                    if(swGps.isChecked()) {
                        mLocationBinder.setLocationFeq(LocationManager.GPS_PROVIDER, time);
                    }
                    if(swNetwork.isChecked()) {
                        mLocationBinder.setLocationFeq(LocationManager.NETWORK_PROVIDER, time);
                    }
                }
            }
        });
    }

    private void updateLocationDisplay(){
        if(mLocationBinder == null) {
            return;
        }
        Location loc = mLocationBinder.getCurLocation();
        if (loc != null) {
            mHandler.sendMessage(mHandler.obtainMessage(MSG_UPDATE_LOCATION,loc));
        }
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initView();
        initPushService();
        initLocationService();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateLocationDisplay();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        doUnbindService();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 0) {

            if(permissions.length == grantResults.length) {
                for (int i = 0; i < permissions.length; i++) {
                    Log.d(TAG, "permiisions: " + permissions[i] + " res: " + grantResults[i]);
                    if (permissions[i].equals("")) {

                    }
                }
            }
            if ((grantResults.length == 2 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED)) {

            } else {
                Log.e(TAG, "We highly recommend that you need to grant the special permissions before initializing the SDK, otherwise some "
                        + "functions will not work");
                //PushManager.getInstance().initialize(this.getApplicationContext(), userPushService);
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            Log.d(TAG, "msg.what: " + msg.what);
            switch (msg.what) {
                case MSG_UPDATE_LOCATION:
                    Location loc = (Location)(msg.obj);
                    tvLatitude.setText(String.valueOf(loc.getLatitude()));
                    tvLongitude.setText(String.valueOf(loc.getLongitude()));
                    break;
                case MSG_REQ_LOCATION_PERMISSION:
                    MainActivity.this.requestPermissions((String[])(msg.obj), 0);
                    break;
            }
            return false;
        }
    });

    /**
     * 绑定服务
     */
    void bindLocationService() {
        //Location Service
        bindService(new Intent(MainActivity.this,
                LocationService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsLocationBound = true;
    }

    /**
     * 解除绑定
     */
    void doUnbindService() {
        if (mIsLocationBound) {
            unbindService(mConnection);
            mIsLocationBound = false;
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mLocationBinder =  (LocationService.LocalBinder) service;
            mLocationBinder.regLocationUpdateCallBack(mHandler);
            if(swGps.isChecked()) {
                mLocationBinder.startLocationUpdate(LocationManager.GPS_PROVIDER);
            }
            if(swNetwork.isChecked()) {
                mLocationBinder.startLocationUpdate(LocationManager.NETWORK_PROVIDER);
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            mLocationBinder = null;
        }
    };


    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
}

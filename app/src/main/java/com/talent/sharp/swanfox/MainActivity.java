package com.talent.sharp.swanfox;

import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
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
    private Switch swLocation;
    private TextView tvLongitude;
    private TextView tvLatitude;
    private TextView tvCellId;
    private TextView tvLac;
    private TextView tvSysId;
    private TextView tvNwId;
    private TextView tvBaseId;
    private Toolbar toolbar;
    private FloatingActionButton fab;

    private LocationManager locationManager;
    private TelephonyManager telephonyManager;

    protected final LocationListener locationListener = new LocationListener() {

        // 当位置发生变化时，输出位置信息
        public void onLocationChanged(Location location) {
            Log.d(TAG, "Location changed to: " + getLocationInfo(location));
            mHandler.sendMessage(mHandler.obtainMessage(MSG_UPDATE_LOCTION,location));
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

    private final int MSG_UPDATE_LOCTION = 0;


    private enum EnumPush{
        GE_PUSH,
        J_PUSH,
        UMENG_PUSH;
    }
    private void initView() {
        setContentView(R.layout.activity_main);
        swGePush = (Switch) findViewById(R.id.sw_ge_push);
        swJiPush = (Switch) findViewById(R.id.sw_j_push);
        swUmengPush = (Switch) findViewById(R.id.sw_umeng_push);
        etLocationFeq = (EditText) findViewById(R.id.et_location_feq);
        btnLocation = (Button) findViewById(R.id.btn_location_feq);
        swLocation = (Switch) findViewById(R.id.sw_location);
        tvLongitude = (TextView) findViewById(R.id.tv_longitude);
        tvLatitude = (TextView) findViewById(R.id.tv_latitude);
        tvCellId = (TextView) findViewById(R.id.tv_cellid);
        tvLac = (TextView) findViewById(R.id.tv_lac);
        tvSysId = (TextView) findViewById(R.id.tv_sid);
        tvNwId = (TextView) findViewById(R.id.tv_nid);
        tvBaseId = (TextView) findViewById(R.id.tv_bid);

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
                if(isChecked) {
                    enablePush(EnumPush.UMENG_PUSH);
                } else {
                    disablePush(EnumPush.UMENG_PUSH);
                }
            }
        });
    }

    private void initLocationService() {
        // 获取 LocationManager
        locationManager = (LocationManager)getSystemService(LOCATION_SERVICE);
        telephonyManager = (TelephonyManager)getSystemService(TELEPHONY_SERVICE);
    }

    private void updateLocation(String currentProvider) {
        Log.d(TAG, "CurrentProvider: " + currentProvider);
        // 获取 Provider 最后一个记录的地址信息
        Location lastKnownLocation = null;
        try {
            lastKnownLocation = locationManager.getLastKnownLocation(currentProvider);
            if (lastKnownLocation != null) {
                Log.d(TAG, "LastKnownLocation: "
                        + getLocationInfo(lastKnownLocation));
            } else {
                Log.d(TAG, "Last Location Unkown!");
            }
            // 注册监听器接受位置更新
            locationManager.requestLocationUpdates(currentProvider, 0, 0,
                    locationListener);
        } catch (SecurityException e) {
            this.requestPermissions(new String[] {"android.permission.ACCESS_FINE_LOCATION"},0);
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
        updateLocation(LocationManager.NETWORK_PROVIDER);
        //updateLocation(LocationManager.GPS_PROVIDER);
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
                case MSG_UPDATE_LOCTION:
                    Location loc = (Location)(msg.obj);
                    tvLatitude.setText(String.valueOf(loc.getLatitude()));
                    tvLongitude.setText(String.valueOf(loc.getLongitude()));
                    break;
            }
            return false;
        }
    });


    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
}

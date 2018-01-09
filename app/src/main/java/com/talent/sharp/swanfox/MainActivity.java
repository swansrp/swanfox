package com.talent.sharp.swanfox;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.igexin.sdk.PushManager;
import com.umeng.message.IUmengCallback;
import com.umeng.message.IUmengRegisterCallback;
import com.umeng.message.PushAgent;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;




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
    private TextView tvGpsLongitude;
    private TextView tvGpsLatitude;
    private TextView tvNwLongitude;
    private TextView tvNwLatitude;
    private LinearLayout lyGpsLocation;
    private LinearLayout lyNwLocation;
    private TextView tvCellId;
    private TextView tvLac;
    private TextView tvSysId;
    private TextView tvNwId;
    private TextView tvBaseId;
    private Toolbar toolbar;
    private FloatingActionButton fab;



    private boolean mIsLocationBound = false;
    private LocationService.LocalBinder mLocationBinder = null;
    private AlertDialog mDialog = null;

    String gpsUpdateTime = null;
    String nwUpdateTime = null;


    private final static int REQUEST_CODE_SYSTEM_ALERT = 0;
    private final static int REQUEST_CODE_PERMISSION = 1;
    public final static int MSG_UPDATE_GPS_LOCATION = 0;
    public final static int MSG_UPDATE_NW_LOCATION = 1;
    public final static int MSG_REQ_LOCATION_PERMISSION = 2;

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
        tvGpsLongitude = (TextView) findViewById(R.id.tv_gps_longitude);
        tvGpsLatitude = (TextView) findViewById(R.id.tv_gps_latitude);
        tvNwLongitude = (TextView) findViewById(R.id.tv_nw_longitude);
        tvNwLatitude = (TextView) findViewById(R.id.tv_nw_latitude);

        lyGpsLocation = (LinearLayout) findViewById(R.id.layout_gsp_location);
        lyNwLocation = (LinearLayout) findViewById(R.id.layout_nw_location);;


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

    private void initPermissions() {
        PackageManager pkgManager = getPackageManager();

        // 读写 sd card 权限非常重要, android6.0默认禁止的, 建议初始化之前就弹窗让用户赋予该权限
        boolean sdCardWritePermission =
                pkgManager.checkPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE, getPackageName()) == PackageManager.PERMISSION_GRANTED;

        // read phone state用于获取 imei 设备信息
        boolean phoneSatePermission =
                pkgManager.checkPermission(android.Manifest.permission.READ_PHONE_STATE, getPackageName()) == PackageManager.PERMISSION_GRANTED;

        boolean locationPermission =
                pkgManager.checkPermission(android.Manifest.permission.ACCESS_FINE_LOCATION, getPackageName()) == PackageManager.PERMISSION_GRANTED;

        if (Build.VERSION.SDK_INT >= 23 && !sdCardWritePermission || !phoneSatePermission || !locationPermission) {
            MainActivity.this.requestPermissions(new String[]{
                            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            android.Manifest.permission.READ_PHONE_STATE,
                            android.Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_CODE_PERMISSION);
        } else {
            Log.d(TAG, "permission has been checked");
        }
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
                Log.d(TAG, "disablePush GE_PUSH");
                PushManager.getInstance().stopService(getApplicationContext());
                break;
            case J_PUSH:
                Log.d(TAG, "disablePush J_PUSH");
                break;
            case UMENG_PUSH:
                Log.d(TAG, "disablePush UMENG_PUSH");
                PushAgent mPushAgent = PushAgent.getInstance(getApplication());
                mPushAgent.disable(new IUmengCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "Umeng has been De Registered");
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

    private void enablePush(EnumPush category) {

        switch(category) {
            case GE_PUSH:
                Log.d(TAG, "enablePush GE_PUSH");
                // 注册 intentService 后 PushDemoReceiver 无效, sdk 会使用 DemoIntentService 传递数据,
                // AndroidManifest 对应保留一个即可(如果注册 DemoIntentService, 可以去掉 PushDemoReceiver, 如果注册了
                // IntentService, 必须在 AndroidManifest 中声明)
                PushManager.getInstance().registerPushIntentService(this.getApplicationContext(), GetuiIntentService.class);
                PushManager.getInstance().initialize(this.getApplicationContext(), GetuiPushService.class);
                // cpu 架构
                Log.d(TAG, "cpu arch = " + (Build.VERSION.SDK_INT < 21 ? Build.CPU_ABI : Build.SUPPORTED_ABIS[0]));

                // 检查 so 是否存在
                File file = new File(this.getApplicationInfo().nativeLibraryDir + File.separator + "libgetuiext2.so");
                Log.e(TAG, "libgetuiext2.so exist = " + file.exists());
                break;
            case J_PUSH:
                Log.d(TAG, "enablePush J_PUSH");
                break;
            case UMENG_PUSH:
                Log.d(TAG, "enablePush UMENG_PUSH");
                PushAgent mPushAgent = PushAgent.getInstance(getApplicationContext());
                //注册推送服务，每次调用register方法都会回调该接口
                mPushAgent.register(new IUmengRegisterCallback() {
                    @Override
                    public void onSuccess(String deviceToken) {
                        //注册成功会返回device token
                        Log.e(TAG, "My device token is: " + deviceToken);
                    }
                    @Override
                    public void onFailure(String s, String s1) {
                        Log.d(TAG, "My device register UMENG failed " + s + "  " + s1);
                    }
                });
                break;
            default:
                break;
        }
    }

    private void initPushService() {
        if(getSwitchPreference(swGePush)) {
            enablePush(EnumPush.GE_PUSH);
        } else {
            disablePush(EnumPush.GE_PUSH);
        }
        if(getSwitchPreference(swJiPush)) {
            enablePush(EnumPush.J_PUSH);
        } else {
            disablePush(EnumPush.J_PUSH);
        }
        if(getSwitchPreference(swUmengPush)) {
            enablePush(EnumPush.UMENG_PUSH);
        } else {
            disablePush(EnumPush.UMENG_PUSH);
        }
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
                    tvGpsLatitude.setText("______");
                    tvGpsLongitude.setText("______");
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
                    tvNwLatitude.setText("______");
                    tvNwLongitude.setText("______");
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
                    if(swGps.isChecked()) {
                        mLocationBinder.setLocationFeq(LocationManager.GPS_PROVIDER, time);
                    }
                    if(swNetwork.isChecked()) {
                        mLocationBinder.setLocationFeq(LocationManager.NETWORK_PROVIDER, time);
                    }
                    updateLocationDisplay();
                }
            }
        });
        lyGpsLocation.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), gpsUpdateTime,
                        Toast.LENGTH_LONG).show();
            }
        });
        lyNwLocation.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), nwUpdateTime,
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void updateLocationDisplay(){
        if(mLocationBinder == null) {
            return;
        }
        if(swGps.isChecked()) {
            Location loc = mLocationBinder.getCurLocation(LocationManager.GPS_PROVIDER);
            if(loc != null) {
                mHandler.sendMessage(mHandler.obtainMessage(MSG_UPDATE_GPS_LOCATION, loc));
            }
        }
        if(swNetwork.isChecked()) {
            Location loc = mLocationBinder.getCurLocation(LocationManager.NETWORK_PROVIDER);
            if(loc != null) {
                mHandler.sendMessage(mHandler.obtainMessage(MSG_UPDATE_NW_LOCATION, loc));
            }
        }
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initPermissions();
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
        if (requestCode == REQUEST_CODE_PERMISSION) {
            if(permissions.length == grantResults.length) {
                for (int i = 0; i < permissions.length; i++) {
                    Log.d(TAG, "permissions: " + permissions[i] + " res: " + grantResults[i]);
                    if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                        showSystemDialog();
                    }
                }
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SYSTEM_ALERT) {
            if (Settings.canDrawOverlays(this)) {
                Log.i(TAG, "onActivityResult granted");
            } else {

            }
        }
    }

    private void showSystemDialog() {
        /* create ui */
        if(!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQUEST_CODE_SYSTEM_ALERT);
        }
        if(mDialog!=null && mDialog.isShowing()) {
            return;
        }
        mDialog = new AlertDialog.Builder(MainActivity.this)
                .setTitle("检查权限失败")
                .setMessage("未能获取必备权限")
                .setPositiveButton("关闭应用", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mDialog.dismiss();
                    }
                })
                .create();
        mDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        mDialog.show();
    }

    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            Log.d(TAG, "msg.what: " + msg.what);
            Location loc = null;
            SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
            Date curDate = new Date(System.currentTimeMillis());//获取当前时间
            switch (msg.what) {
                case MSG_UPDATE_GPS_LOCATION:
                    loc = (Location)(msg.obj);
                    tvGpsLatitude.setText(String.valueOf(loc.getLatitude()));
                    tvGpsLongitude.setText(String.valueOf(loc.getLongitude()));
                    gpsUpdateTime = formatter.format(curDate);
                    break;
                case MSG_UPDATE_NW_LOCATION:
                    loc = (Location)(msg.obj);
                    tvNwLatitude.setText(String.valueOf(loc.getLatitude()));
                    tvNwLongitude.setText(String.valueOf(loc.getLongitude()));
                    nwUpdateTime = formatter.format(curDate);
                    break;
                case MSG_REQ_LOCATION_PERMISSION:
                    MainActivity.this.requestPermissions((String[])(msg.obj), REQUEST_CODE_PERMISSION);
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

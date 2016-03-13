package com.videogo.ui.devicelist;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.hikvision.wifi.configuration.BaseUtil;
import com.videogo.R;
import com.videogo.constant.IntentConsts;
import com.videogo.ui.util.Constants;
import com.videogo.util.ConnectionDetector;

/**
 * 一键连接网络配置界面
 * 
 * @author chengjuntao
 * @data 2014-4-9
 */
public class AutoWifiNetConfigActivity extends Activity implements OnClickListener {
    private Button btnNext;

    private TextView tvSSID;

    private EditText edtPassword;

    private String seriaNo;
    
    private String deviceType;

    private View btnBack;

    private View tvReset;

    private String veryCode = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.auto_wifi_net_config);
        init();
        findViews();
        initUI();
        setListener();
    }

    /**
     * 提示需要wifi网络
     * 
     * @see
     * @since V1.8.2
     */
    private void showWifiRequiredDialog() {

        new AlertDialog.Builder(this).setTitle(R.string.auto_wifi_dialog_title_wifi_required)
                .setMessage(R.string.please_open_wifi_network)
                .setNegativeButton(R.string.auto_wifi_dialog_btn_wifi, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int arg1) {

                        dialog.dismiss();
                        // 跳转wifi设置界面
                        if (android.os.Build.VERSION.SDK_INT > 10) {
                            // 3.0以上打开设置界面，也可以直接用ACTION_WIRELESS_SETTINGS打开到wifi界面
                            startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));
                        } else {
                            startActivity(new Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS));
                        }
                    }
                }).setPositiveButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        btnBack.performClick();
                    }
                }).setCancelable(false).create().show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ConnectionDetector.getConnectionType(this) != ConnectionDetector.WIFI) {
            tvSSID.setText(R.string.unknow_ssid);
            showWifiRequiredDialog();
        } else {
            tvSSID.setText(BaseUtil.getWifiSSID(this));
        }
    }

    private void init() {
        Intent intent = getIntent();
        seriaNo = intent.getStringExtra(IntentConsts.EXTRA_DEVICE_ID);
        veryCode = intent.getStringExtra(IntentConsts.EXTRA_DEVICE_CODE);
        deviceType = intent.getStringExtra(IntentConsts.EXTRA_DEVICE_TYPE);
    }

    /**
     * 这里对方法做描述
     * 
     * @see
     * @since V1.0
     */
    private void findViews() {
        btnBack = findViewById(R.id.btnBack);
        btnNext = (Button) findViewById(R.id.btnNext);
        tvSSID = (TextView) findViewById(R.id.tvSSID);
        edtPassword = (EditText) findViewById(R.id.edtPassword);
        tvReset = findViewById(R.id.tvReset);
    }

    /**
     * 这里对方法做描述
     * 
     * @see
     * @since V1.0
     */
    private void initUI() {
        tvSSID.setText(BaseUtil.getWifiSSID(this));
        edtPassword.setText("");
    }

    /**
     * 这里对方法做描述
     * 
     * @see
     * @since V1.0
     */
    private void setListener() {
        btnNext.setOnClickListener(this);
        btnBack.setOnClickListener(this);
        tvReset.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        Intent intent = null;
        switch (v.getId()) {
            case R.id.btnNext:
                // findViewById(R.id.a1_defence_mode).setBackgroundResource(R.drawable.a1_blue);
                if(!TextUtils.isEmpty(seriaNo)) {
                    intent = new Intent(this, AutoWifiConnectingActivity.class);
                } else {
                    //intent = new Intent(this, AutoWifiDiscoveringActivity.class);
                    intent = new Intent(this, DeviceDiscoverActivity.class);
                }
                intent.putExtra(IntentConsts.EXTRA_WIFI_SSID, tvSSID.getText().toString());
                intent.putExtra(IntentConsts.EXTRA_WIFI_PASSWORD, TextUtils.isEmpty(edtPassword.getText().toString()) ? "smile"
                        : edtPassword.getText().toString());
                if(!TextUtils.isEmpty(seriaNo)) {
                    intent.putExtra(IntentConsts.EXTRA_DEVICE_ID, seriaNo);
                    intent.putExtra(IntentConsts.EXTRA_DEVICE_CODE, veryCode);
                    intent.putExtra(IntentConsts.EXTRA_DEVICE_TYPE, deviceType);
                }
                startActivity(intent);
                finish();
                break;
            case R.id.btnBack:
                finish();
                break;
            case R.id.tvReset:
                intent = new Intent(this, ResetIntroduceActivity.class);
                startActivity(intent);
                break;
            default:
                break;
        }
    }
}
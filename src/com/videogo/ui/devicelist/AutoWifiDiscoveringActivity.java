/* 
 * @ProjectName ezviz-openapi-android-demo
 * @Copyright HangZhou Hikvision System Technology Co.,Ltd. All Right Reserved
 * 
 * @FileName AutoWifiDiscoveringActivity.java
 * @Description 这里对文件进行描述
 * 
 * @author chenxingyf1
 * @data 2015-5-12
 * 
 * @note 这里写本文件的详细功能描述和注释
 * @note 历史记录
 * 
 * @warning 这里写本文件的相关警告
 */
package com.videogo.ui.devicelist;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.net.wifi.WifiManager.MulticastLock;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.hikvision.wifi.configuration.BaseUtil;
import com.hikvision.wifi.configuration.DeviceDiscoveryListener;
import com.hikvision.wifi.configuration.DeviceInfo;
import com.hikvision.wifi.configuration.DeviceInfo.DevceState;
import com.hikvision.wifi.configuration.OneStepWifiConfigurationManager;
import com.videogo.R;
import com.videogo.constant.IntentConsts;
import com.videogo.exception.BaseException;
import com.videogo.exception.ErrorCode;
import com.videogo.openapi.EzvizAPI;
import com.videogo.openapi.bean.resp.CameraInfo;
import com.videogo.ui.cameralist.CameraListActivity;
import com.videogo.util.ConnectionDetector;
import com.videogo.util.LogUtil;
import com.videogo.util.Utils;
import com.videogo.widget.SecurityEditText;
import com.videogo.widget.WaitDialog;

import java.util.Timer;
import java.util.TimerTask;

/**
 * 一键添加摄像头界面
 * 
 * @author chengjuntao
 * @data 2014-4-9
 */
public class AutoWifiDiscoveringActivity extends Activity implements OnClickListener {
    private static final String TAG = "AutoWifiDiscoveringActivity";

    /** 一键添加的当前状态 正在连接wifi */
    private static final int STATUS_WIFI_CONNETCTING = 1000;

    /** 一键添加的当前状态 正在进行设备注册 */
    private static final int STATUS_REGISTING = 101;

    /** 一键添加的当前状态 正在添加摄像头 */
    private static final int STATUS_ADDING_CAMERA = 102;

    /** 一键添加的当前状态 添加摄像头 */
    private static final int STATUS_ADD_CAMERA_SUCCESS = 103;

    /** 一键添加错误代号 设备连接wifi失败 */
    private static final int ERROR_WIFI_CONNECT = 1000;

    /** 一键添加错误代号 设备注册失败 */
    private static final int ERROR_REGIST = 1001;

    /** 一键添加错误代号 设备添加摄像头失败 */
    private static final int ERROR_ADD_CAMERA = 1002;

    private static final long OVERTIME_CONNECT_WIFI_REGIST = 60 * 1000;

    // 返回按钮
    private View btnBack;

    // title
    private TextView tvTitle;

    // 添加摄像头的容器
    private View addCameraContainer;

    // 有线连接的容器
    private View lineConnectContainer;

    // 状态变化图
    private ImageView imgStatus;

    // 状态
    private TextView tvStatus;

    // 重试按钮
    private View btnRetry;

    // 有线连接
    private Button btnLineConnect;

    // 线连接成功
    private View btnLineConnetOk;

    // 完成按钮
    private View btnFinish;

    private String serialNo;

    private String wifiPassword = "";

    private String wifiSSID = "";

    /** 当前的错误代码 */
    private int errorStep = 0;

    private ImageView imgAnimation;

    private AnimationDrawable animWaiting;

    private String maskIpAddress;

    private Timer overTimeTimer;

    private OneStepWifiConfigurationManager oneStepWifiConfigurationManager;

    private MulticastLock lock;

    private CameraInfo mCameraInfo = null;

    private DeviceDiscoveryListener deviceDiscoveryListener = new DeviceDiscoveryListener() {
        @Override
        public void onDeviceLost(DeviceInfo deviceInfo) {
        }

        @Override
        public void onDeviceFound(DeviceInfo deviceInfo) {
            Message msg = new Message();
            msg.what = 0;
            msg.obj = deviceInfo;
            defiveFindHandler.sendMessage(msg);
        }
    };

    private boolean isWifiConnected = false;
    private boolean isPlatConnected = false;

    private Handler defiveFindHandler = new Handler() {

        public void handleMessage(Message msg) {
            if (msg.what == 0) {
                DeviceInfo deviceInfo = (DeviceInfo) msg.obj;
                if (deviceInfo == null || deviceInfo.getState() == null) {
                    LogUtil.debugLog(TAG, "接收到无效的bonjour信息 为空");
                    return;
                }

                if (DevceState.WIFI == deviceInfo.getState()) {
                    if (isWifiConnected) {
                        return;
                    }
                    serialNo = deviceInfo.getSerialNo();
                    isWifiConnected = true;
                    LogUtil.debugLog(TAG, "接收到设备连接上wifi信息 " + deviceInfo.toString());
                    stopConfigOnThread();
                    changeStatuss(STATUS_REGISTING);
                } else if (DevceState.PLAT == deviceInfo.getState()) {
                    if (isPlatConnected) {
                        return;
                    }
                    if (serialNo != null && !TextUtils.equals(serialNo, deviceInfo.getSerialNo())) {
                        return;
                    }
                    serialNo = deviceInfo.getSerialNo();
                    isWifiConnected = true;
                    isPlatConnected = true;
                    LogUtil.debugLog(TAG, "接收到设备连接上PLAT信息 " + deviceInfo.toString());
                    cancelOvertimeTimer();
                    changeStatuss(STATUS_ADDING_CAMERA);
                } else if (DevceState.REPORT == deviceInfo.getState()) {
                    serialNo = deviceInfo.getSerialNo();
                    isWifiConnected = true;
                    LogUtil.debugLog(TAG, "接收到设备连接上wifi信息 " + deviceInfo.toString());
                    stopConfigOnThread();
                    changeStatuss(STATUS_REGISTING);    
                    new GetCamersInfoTask().execute();
                }
            }
        };
    };

    private View btnCancel;

    private WaitDialog mWaitDlg;

    private boolean isSupportNetWork = true;

    private boolean isSupportWifi = true;

    private View tvDeviceWifiConfigTip;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.auto_wifi_connecting);
        init();
        findViews();
        initUI();
        setListener();
        changeStatuss(STATUS_WIFI_CONNETCTING);
    }

    private void init() {
        // 唤醒，常亮
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Intent intent = getIntent();
        wifiPassword = intent.getStringExtra(IntentConsts.EXTRA_WIFI_PASSWORD);
        wifiSSID = intent.getStringExtra(IntentConsts.EXTRA_WIFI_SSID);
        LogUtil.debugLog(TAG, "wifiPassword = " + wifiPassword + ",wifiSSID = " + wifiSSID 
                + ",isSupportNetWork " + isSupportNetWork + ",isSupportWifi " + isSupportWifi);

        // 一键安装管理器
        maskIpAddress = BaseUtil.getMaskIpAddress(getApplicationContext());
        oneStepWifiConfigurationManager = new OneStepWifiConfigurationManager(this, maskIpAddress);
        oneStepWifiConfigurationManager.setDeviceDiscoveryListener(deviceDiscoveryListener);
        LogUtil.debugLog(TAG, wifiSSID + " " + wifiPassword + " " + maskIpAddress);
    }

    /**
     * 这里对方法做描述
     * 
     * @see
     * @since V1.0
     */
    private void findViews() {
        btnBack = findViewById(R.id.btnBack);
        btnCancel = findViewById(R.id.cancel_btn);
        tvTitle = (TextView) findViewById(R.id.tvTitle);

        addCameraContainer = findViewById(R.id.addCameraContainer);
        lineConnectContainer = findViewById(R.id.lineConnectContainer);
        imgStatus = (ImageView) findViewById(R.id.imgStatus);
        tvStatus = (TextView) findViewById(R.id.tvStatus);

        btnRetry = (TextView) findViewById(R.id.btnRetry);
        btnLineConnect = (Button) findViewById(R.id.btnLineConnet);
        btnLineConnetOk = findViewById(R.id.btnLineConnetOk);
        imgAnimation = (ImageView) findViewById(R.id.imgAnimation);
        btnFinish = findViewById(R.id.btnFinish);

        tvDeviceWifiConfigTip = findViewById(R.id.tvDeviceWifiConfigTip);
        mWaitDlg = new WaitDialog(this, android.R.style.Theme_Translucent_NoTitleBar);
        mWaitDlg.setCancelable(false);

    }

    /**
     * 这里对方法做描述
     * 
     * @see
     * @since V1.0
     */
    private void initUI() {
        tvTitle.setText(R.string.auto_wifi_title_add_device);
    }

    /**
     * 这里对方法做描述
     * 
     * @see
     * @since V1.0
     */
    private void setListener() {
        btnBack.setOnClickListener(this);
        btnCancel.setOnClickListener(this);
        btnLineConnect.setOnClickListener(this);
        btnLineConnetOk.setOnClickListener(this);
        btnRetry.setOnClickListener(this);
        btnFinish.setOnClickListener(this);
    }

    /**
     * 开始连接wifi状态,发送bonjour信息,配置超时信息
     * 
     * @see
     * @since V1.8.2
     */
    private void start() {
        android.net.wifi.WifiManager wifi = (android.net.wifi.WifiManager) getSystemService(android.content.Context.WIFI_SERVICE);
        lock = wifi.createMulticastLock("videogo_multicate_lock");
        lock.setReferenceCounted(true);
        lock.acquire();
        isWifiConnected = false;
        isPlatConnected = false;
        // 检测
        startOvertimeTimer(OVERTIME_CONNECT_WIFI_REGIST, new Runnable() {
            public void run() {
                stopBonjour();
                if (isWifiConnected && isPlatConnected) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            changeStatuss(STATUS_ADDING_CAMERA);
                        }
                    }); 
                } else {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            if(!isPlatConnected) {
                                addCameraFailed(ERROR_WIFI_CONNECT, 0);
                            } else {
                                startReportBonjour();
                            }
                        }
                    }); 
                }
            }
        });
        new Thread(new Runnable() {
            @Override
            public void run() {
                stopConfigAndBonjour(false);
                if (oneStepWifiConfigurationManager == null) {
                    maskIpAddress = BaseUtil.getMaskIpAddress(getApplicationContext());
                    oneStepWifiConfigurationManager = new OneStepWifiConfigurationManager(
                            AutoWifiDiscoveringActivity.this, maskIpAddress);
                    oneStepWifiConfigurationManager.setDeviceDiscoveryListener(deviceDiscoveryListener);
                    LogUtil.debugLog(TAG, wifiSSID + " " + wifiPassword + " " + maskIpAddress);
                }
                int startSendConfigData = oneStepWifiConfigurationManager.startConfig(wifiSSID, wifiPassword, 5, 200);
                if (startSendConfigData == OneStepWifiConfigurationManager.START_SUCESS) {
                    LogUtil.debugLog(TAG, "开始向网关地址: " + maskIpAddress + " 发送数据: ssid: " + wifiSSID + " key:"
                            + wifiPassword);
                } else if (startSendConfigData == OneStepWifiConfigurationManager.PARAM_ERROR) {
                    LogUtil.debugLog(TAG, "调用发送接口: 参数异常");
                } else if (startSendConfigData == OneStepWifiConfigurationManager.HAS_SENDING) {
                    LogUtil.debugLog(TAG, "正在发送，请稍候...");
                }
                if (!isFinishing()
                        && ConnectionDetector.getConnectionType(AutoWifiDiscoveringActivity.this) == ConnectionDetector.WIFI
                        && oneStepWifiConfigurationManager != null) {
                    oneStepWifiConfigurationManager.startBonjour();
                }
            }
        }).start();
    }

    /**
     * 停止连接wifi，注册设备
     * 
     * @see
     * @since V1.8.2
     */
    private synchronized void stopBonjourOnThread() {
        if (lock != null) {
            lock.release();
            lock = null;
        }
        // 停止配置，停止bonjour服务
        new Thread(new Runnable() {
            @Override
            public void run() {
                long startTime = System.currentTimeMillis();
                stopConfigAndBonjour(false);
                LogUtil.debugLog(TAG, "stopBonjourOnThread .cost time = " + (System.currentTimeMillis() - startTime)
                        + "ms");
            }
        }).start();
        LogUtil.debugLog(TAG, "stopBonjourOnThread ..................");
    }

    /**
     * 停止连接wifi，注册设备
     * 
     * @see
     * @since V1.8.2
     */
    private synchronized void stopConfigOnThread() {
        // 停止配置，停止bonjour服务
        new Thread(new Runnable() {
            @Override
            public void run() {
                long startTime = System.currentTimeMillis();
                stopConfigAndBonjour(true);
                LogUtil.debugLog(TAG, "stopConfigOnThread .cost time = " + (System.currentTimeMillis() - startTime)
                        + "ms");
            }
        }).start();
    }

    /**
     * 这里对方法做描述
     * 
     * @param config
     *            ture just stop Config
     * @see
     * @since V1.8.2
     */
    private synchronized void stopConfigAndBonjour(boolean config) {

        if (oneStepWifiConfigurationManager != null) {
            if (config) {
                oneStepWifiConfigurationManager.stopConfig();
            } else {
                oneStepWifiConfigurationManager.stopConfig();
                oneStepWifiConfigurationManager.stopBonjour();
                oneStepWifiConfigurationManager.stopSmartBonjour();
                oneStepWifiConfigurationManager = null;
            }
            LogUtil.debugLog(TAG, "stopConfigAndBonjour is invoked...");
        }
    }

    /**
     * 停止连接wifi，注册设备
     * 
     * @see
     * @since V1.8.2
     */
    private void stopBonjour() {
        long startTime = System.currentTimeMillis();
        if (lock != null) {
            lock.release();
            lock = null;
        }
        // 停止配置，停止bonjour服务
        stopConfigAndBonjour(false);
        LogUtil.debugLog(TAG, "stopBonjour cost time = " + (System.currentTimeMillis() - startTime) + "ms");
    }

    /**
     * 开始设备状态report,发送bonjour信息,配置超时信息
     * 
     * @see
     * @since V1.8.2
     */
    private void startReportBonjour() {
        android.net.wifi.WifiManager wifi = (android.net.wifi.WifiManager) getSystemService(android.content.Context.WIFI_SERVICE);
        lock = wifi.createMulticastLock("videogo_multicate_lock");
        lock.setReferenceCounted(true);
        lock.acquire();
        isWifiConnected = false;
        isPlatConnected = false;
        // 检测
        startOvertimeTimer(OVERTIME_CONNECT_WIFI_REGIST, new Runnable() {
            public void run() {
                stopBonjour();
                if (isWifiConnected && isPlatConnected) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            changeStatuss(STATUS_ADDING_CAMERA);
                        }
                    }); 
                } else {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            addCameraFailed(ERROR_WIFI_CONNECT, 0);
                        }
                    }); 
                }
            }
        });
        new Thread(new Runnable() {
            @Override
            public void run() {
                stopConfigAndBonjour(false);
                if (oneStepWifiConfigurationManager == null) {
                    maskIpAddress = BaseUtil.getMaskIpAddress(getApplicationContext());
                    oneStepWifiConfigurationManager = new OneStepWifiConfigurationManager(
                            AutoWifiDiscoveringActivity.this, maskIpAddress);
                    oneStepWifiConfigurationManager.setDeviceDiscoveryListener(deviceDiscoveryListener);
                }

                if (!isFinishing()
                        && ConnectionDetector.getConnectionType(AutoWifiDiscoveringActivity.this) == ConnectionDetector.WIFI
                        && oneStepWifiConfigurationManager != null) {
                    oneStepWifiConfigurationManager.startSmartBonjour();
                }
            }
        }).start();
    }
    
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnBack:
                // 设备切换wifi界面来的，且已连接成功
                if (tvDeviceWifiConfigTip.getVisibility() == View.VISIBLE) {
                    finish();
                } else {
                    if (addCameraContainer.getVisibility() == View.VISIBLE && btnRetry.getVisibility() != View.VISIBLE) {
                        showConfirmDialog();
                    } else {
                        finish();
                    }
                }

                break;
            case R.id.cancel_btn:
                cancelOnClick();
                break;
            case R.id.btnRetry:
                retryOnclick();
                break;
            case R.id.btnLineConnet:
                lineConnectClick();
                break;
            case R.id.btnLineConnetOk:
                lineConnectOkClick();
                break;
            case R.id.btnFinish:
                closeActivity();
                break;
            default:
                break;
        }
    }

    private void cancelOnClick() {
        btnCancel.setVisibility(View.GONE);
        lineConnectContainer.setVisibility(View.GONE);
        addCameraContainer.setVisibility(View.VISIBLE);
        tvTitle.setText(R.string.auto_wifi_title_add_device);
    }

    /**
     * 重试按钮点击处理
     * 
     * @see
     * @since V1.8.2
     */
    private void retryOnclick() {
        switch (errorStep) {
            case ERROR_WIFI_CONNECT:
            case ERROR_REGIST:
                changeStatuss(STATUS_WIFI_CONNETCTING);
                break;
            case ERROR_ADD_CAMERA:
                changeStatuss(STATUS_ADDING_CAMERA);
                break;
            default:
                break;
        }
    }

    /**
     * 有线连接按钮点击处理
     * 
     * @see
     * @since V1.8.2
     */
    private void lineConnectClick() {
        btnCancel.setVisibility(View.VISIBLE);
        lineConnectContainer.setVisibility(View.VISIBLE);

        tvTitle.setText(R.string.auto_wifi_network_add_device);
        addCameraContainer.setVisibility(View.GONE);
    }

    /**
     * 已经连接好按钮点击处理
     * 
     * @see
     * @since V1.8.2
     */
    private void lineConnectOkClick() {
        //cancelOnClick();
        btnRetry.setVisibility(View.GONE);
        btnLineConnect.setVisibility(View.GONE);
        changeStatuss(STATUS_ADDING_CAMERA);
    }

    /**
     * 弹出对话确认是否退出
     * 
     * @see
     * @since V1.8.2
     */
    private void showConfirmDialog() {
        new AlertDialog.Builder(this).setMessage(R.string.auto_wifi_dialog_connecting_msg)
                .setPositiveButton(R.string.update_exit, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        finish();
                    }
                }).setNegativeButton(R.string.wait, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).create().show();
    }

    /**
     * 改变状态，改变文字
     * 
     * @param Status
     * @see
     * @since V1.8.2
     */
    private void changeStatuss(int Status) {
        switch (Status) {
            case STATUS_WIFI_CONNETCTING:
                tvStatus.setText(R.string.auto_wifi_connecting_msg1);
                imgStatus.setImageResource(R.drawable.auto_wifi_cicle_bg);
                imgAnimation.setVisibility(View.VISIBLE);
                imgStatus.setVisibility(View.VISIBLE);
                imgAnimation.setImageResource(R.drawable.auto_wifi_wait);
                animWaiting = (AnimationDrawable) imgAnimation.getDrawable();
                animWaiting.start();
                btnRetry.setVisibility(View.GONE);
                btnLineConnect.setVisibility(View.GONE);
                start();
                break;
            case STATUS_REGISTING:
                tvStatus.setText(R.string.auto_wifi_connecting_msg2);
                imgStatus.setImageResource(R.drawable.auto_wifi_cicle_120);
                imgStatus.setVisibility(View.VISIBLE);
                imgAnimation.setImageResource(R.drawable.auto_wifi_wait);
                animWaiting = (AnimationDrawable) imgAnimation.getDrawable();
                animWaiting.start();
                btnRetry.setVisibility(View.GONE);
                btnLineConnect.setVisibility(View.GONE);
                break;
            case STATUS_ADDING_CAMERA:
                tvStatus.setText(R.string.auto_wifi_connecting_msg3);
                imgAnimation.setImageResource(R.drawable.auto_wifi_wait);
                imgStatus.setVisibility(View.VISIBLE);
                imgStatus.setImageResource(R.drawable.auto_wifi_cicle_240);
                animWaiting = (AnimationDrawable) imgAnimation.getDrawable();
                animWaiting.start();
                btnRetry.setVisibility(View.GONE);
                btnLineConnect.setVisibility(View.GONE);
                if(mCameraInfo == null) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            openAddDeviceDialog();
                        }
                    });
                } else {
                    mCameraInfo.setStatus(1);//设备已在线
                    changeStatuss(STATUS_ADD_CAMERA_SUCCESS);
                }
                break;
            case STATUS_ADD_CAMERA_SUCCESS:
                tvStatus.setText(R.string.aotu_wifi_add_device_success);
                imgStatus.setImageResource(R.drawable.auto_wifi_add_success_2);
                imgAnimation.setVisibility(View.GONE);

                btnBack.setVisibility(View.GONE);
                btnFinish.setVisibility(View.VISIBLE);
                break;
            default:
                break;
        }
    }

    /**
     * 开启超时
     * 
     * @param time
     * @see
     * @since V1.8.2
     */
    private void startOvertimeTimer(long time, final Runnable run) {
        if (overTimeTimer != null) {
            overTimeTimer.cancel();
            overTimeTimer = null;
        }
        overTimeTimer = new Timer();
        overTimeTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                LogUtil.debugLog(TAG, "startOvertimeTimer");
                runOnUiThread(run);
            }
        }, time);

    }

    /**
     * 关闭超时
     * 
     * @see
     * @since V1.8.2
     */
    private void cancelOvertimeTimer() {
        if (overTimeTimer != null) {
            overTimeTimer.cancel();
        }
    }

    /**
     * 连接失败的处理
     * 
     * @param errorStep
     * @see
     * @since V1.8.2
     */
    private void addCameraFailed(int errorStep, int errorCode) {
        this.errorStep = errorStep;
        switch (errorStep) {
            case ERROR_WIFI_CONNECT:
                btnRetry.setVisibility(View.VISIBLE);
                // 支持有线连接才显示
                if (isSupportNetWork) {
                    btnLineConnect.setVisibility(View.VISIBLE);
                }
                btnLineConnect.setText(R.string.auto_wifi_line_connect);
                imgAnimation.setImageResource(R.drawable.auto_wifi_failed);
                tvStatus.setText(R.string.auto_wifi_connecting_failed);
                // stopBonjourOnThread();
                break;
            case ERROR_REGIST:
                // stopBonjourOnThread();
                btnRetry.setVisibility(View.VISIBLE);
                btnLineConnect.setVisibility(View.GONE);
                imgAnimation.setImageResource(R.drawable.auto_wifi_failed);
                tvStatus.setText(R.string.auto_wifi_register_failed);
                break;
            case ERROR_ADD_CAMERA:
                btnRetry.setVisibility(View.VISIBLE);
                btnLineConnect.setVisibility(View.GONE);
                imgAnimation.setImageResource(R.drawable.auto_wifi_failed);
                
                if(errorCode == ErrorCode.ERROR_WEB_CAMERA_NO_PERMISSION
                || errorCode == ErrorCode.ERROR_WEB_DIVICE_ADDED
                || errorCode == ErrorCode.ERROR_WEB_DIVICE_ONLINE_ADDED
                || errorCode == ErrorCode.ERROR_WEB_DIVICE_OFFLINE_ADDED) {
                    tvStatus.setText(R.string.scan_device_add_by_others);
                } else if (errorCode == ErrorCode.ERROR_WEB_DEVICE_NOTEXIT
                        || errorCode == ErrorCode.ERROR_WEB_DEVICE_NOT_EXIT) {
                    tvStatus.setText(R.string.device_not_exist);
                } else if (errorCode > 0) {
                    tvStatus.setText(Utils.getErrorTip(this, R.string.auto_wifi_add_device_failed, errorCode));
                } else {
                    tvStatus.setText(R.string.auto_wifi_add_device_failed);
                }
                break;
            default:
                break;
        }
    }

    /**
     * 关闭当前画面，返回主页面
     * 
     * @see
     * @since V1.8.2
     */
    private void closeActivity() {
        Intent intent = new Intent(this, CameraListActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (btnFinish.getVisibility() == View.VISIBLE) {
            closeActivity();
        } else if (btnCancel.getVisibility() == View.VISIBLE) {
            cancelOnClick();
        } else {
            btnBack.performClick();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelOvertimeTimer();
        stopBonjourOnThread();
    }
    
    /**
     * 获取设备信息任务
     */
    private class GetCamersInfoTask extends AsyncTask<Void, Void, CameraInfo> {  
        private int mErrorCode = 0;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected CameraInfo doInBackground(Void... params) {
            if(AutoWifiDiscoveringActivity.this.isFinishing()) {
                return null;
            }
            if (!ConnectionDetector.isNetworkAvailable(AutoWifiDiscoveringActivity.this)) {
                return null;
            }

            try {
                return (CameraInfo)EzvizAPI.getInstance().getCameraInfo(1, serialNo);
            } catch (BaseException e) {
                e.printStackTrace();
                mErrorCode = e.getErrorCode();
                return null;
            }
        }

        @Override
        protected void onPostExecute(CameraInfo result) {
            super.onPostExecute(result);
            if(AutoWifiDiscoveringActivity.this.isFinishing()) {
                return;
            }
            
            if (result != null) { 
                mCameraInfo = result;
                if(mCameraInfo.getStatus() == 1) {//设备在线，已被自己添加
                    changeStatuss(STATUS_ADD_CAMERA_SUCCESS);
                } else {//设备不在线，已被自己添加，只需配置wifi
                    changeStatuss(STATUS_WIFI_CONNETCTING);
                }
            } else {
                onError(mErrorCode);
            }
        }
        
        protected void onError(int errorCode) {
            LogUtil.debugLog(TAG, "GetCamersInfoTask onError:" + errorCode);
            switch (errorCode) {
                case ErrorCode.ERROR_WEB_SESSION_ERROR:
                case ErrorCode.ERROR_WEB_SESSION_EXPIRE:
                case ErrorCode.ERROR_WEB_HARDWARE_SIGNATURE_ERROR:
                    EzvizAPI.getInstance().gotoLoginPage();
                    finish();
                    break;
                case ErrorCode.ERROR_WEB_DEVICE_NOTEXIT:
                case ErrorCode.ERROR_WEB_DEVICE_NOT_EXIT:
                case ErrorCode.ERROR_WEB_DIVICE_ONLINE_NOT_ADD:
                    isWifiConnected = true;
                    isPlatConnected = true;
                    cancelOvertimeTimer();
                    changeStatuss(STATUS_ADDING_CAMERA);
                    break;                     
                case ErrorCode.ERROR_WEB_DIVICE_ADDED:
                case ErrorCode.ERROR_WEB_DIVICE_ONLINE_ADDED:
                case ErrorCode.ERROR_WEB_DIVICE_OFFLINE_ADDED:
                    addCameraFailed(ERROR_ADD_CAMERA, mErrorCode);
                    break;
                case ErrorCode.ERROR_WEB_DIVICE_NOT_ONLINE:
                case ErrorCode.ERROR_WEB_DIVICE_OFFLINE:
                default:
                    if (!isSupportWifi) {
                        lineConnectClick();
                        btnBack.setVisibility(View.VISIBLE);
                        btnCancel.setVisibility(View.GONE);
                    } else {
                        changeStatuss(STATUS_WIFI_CONNETCTING);
                    }
                    break;                     
            }
        }
    }
    
    private void openAddDeviceDialog() {
        final SecurityEditText editText = new SecurityEditText(AutoWifiDiscoveringActivity.this);
        editText.setAction(SecurityEditText.ACTION_ADD_DEVICE, serialNo);
        editText.setHint(R.string.input_device_verify_code);
        new  AlertDialog.Builder(AutoWifiDiscoveringActivity.this)  
        .setTitle(this.getString(R.string.add_device) + serialNo)   
        .setIcon(android.R.drawable.ic_dialog_info)   
        .setView(editText)  
        .setPositiveButton(R.string.certain, new DialogInterface.OnClickListener() {
    
            @Override
            public void onClick(DialogInterface dialog, int which) {
                new AddDeviceTask().execute(editText);
            }
            
        })   
        .setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                LogUtil.infoLog(TAG, "Ignoring key events...");
                return true;
            }
        })
        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
    
            @Override
            public void onClick(DialogInterface dialog, int which) {
                addCameraFailed(ERROR_ADD_CAMERA, 0);
            }
            
        })
        .show(); 
    }
    
    private class AddDeviceTask extends AsyncTask<SecurityEditText, Void, Boolean> {
        private Dialog mWaitDialog;
        private int mErrorCode = 0;
        
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mWaitDialog = new WaitDialog(AutoWifiDiscoveringActivity.this, android.R.style.Theme_Translucent_NoTitleBar);
            mWaitDialog.setCancelable(false);
            mWaitDialog.show();
        }

        @Override
        protected Boolean doInBackground(SecurityEditText... params) {
            if (!ConnectionDetector.isNetworkAvailable(AutoWifiDiscoveringActivity.this)) {
                return null;
            }
            
            SecurityEditText editText = params[0];
            try {
                return editText.submitAction();
            } catch (BaseException e) {
                e.printStackTrace();
                mErrorCode = e.getErrorCode();
                return null;
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            mWaitDialog.dismiss();

            if (result != null && result) {
                Utils.showToast(AutoWifiDiscoveringActivity.this, R.string.add_device_success); 
                changeStatuss(STATUS_ADD_CAMERA_SUCCESS);
            } else {
                Utils.showToast(AutoWifiDiscoveringActivity.this, R.string.add_device_fail, mErrorCode);
                addCameraFailed(ERROR_ADD_CAMERA, mErrorCode);
            }
        }
    }
}

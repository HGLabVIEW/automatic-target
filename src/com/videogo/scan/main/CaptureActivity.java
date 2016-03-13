/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.videogo.scan.main;

import java.io.IOException;
import java.util.Collection;

import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewTreeObserver;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;
import com.videogo.R;
import com.videogo.constant.IntentConsts;
import com.videogo.openapi.EzvizAPI;
import com.videogo.scan.camera.CameraManager;
import com.videogo.ui.devicelist.AutoWifiNetConfigActivity;
import com.videogo.util.LogUtil;
import com.videogo.util.Utils;
import com.videogo.widget.TitleBar;

public final class CaptureActivity extends Activity implements SurfaceHolder.Callback {
    private static final int SERIAL_NO_LENGTH = 9;
    
    // 常量定义区
    private static final String TAG = CaptureActivity.class.getSimpleName();// 打印标识

    private static final String PRODUCT_SEARCH_URL_PREFIX = "http://www.google";

    private static final String PRODUCT_SEARCH_URL_SUFFIX = "/m/products/scan";

    private static final String[] ZXING_URLS = {
        "http://zxing.appspot.com/scan", "zxing://scan/"};

    public static final int HISTORY_REQUEST_CODE = 0x0000bacc;

    private static final float BEEP_VOLUME = 0.10f;// 读取成功后音效大小

    private static final long VIBRATE_DURATION = 200L;// 震动持续时间 单位：微秒

    /** 扫描句柄 */
    private CaptureActivityHandler mHandler = null;

    /** 扫描View变量 */
    private ViewfinderView mViewfinderView = null;

    /** 显示扫描提示变量 */
    private TextView mTxtResult = null;

    /** 定时器变量 */
    private InactivityTimer mInactivityTimer = null;

    /** 播放器句柄 */
    private MediaPlayer mMediaPlayer = null;

    /** 提示信息变量 */
    private Toast mToast = null;

    /** 是否有声音变量 */
    private final boolean mPlayBeep = false;

    /** 是否震动变量 */
    private boolean mVibrate = false;

    /** 序列号结果变量 */
    private String mSerialNo = null;

    private String mSerialVerifyCode = null;

    /** 在页面重绘回调函数中标识是否计算过控件的大小 */
    private boolean mHasMeasured = false;

    private CameraManager cameraManager;

    private CaptureActivityHandler handler;

    private Result savedResultToShow;

    private ViewfinderView viewfinderView;

    private Result lastResult;

    private boolean hasSurface;

    private IntentSource source;

    private String sourceUrl;

    private Collection<BarcodeFormat> decodeFormats;

    private String characterSet;

    private String deviceType = "";

    private TitleBar mTitleBar;

    ViewfinderView getViewfinderView() {
        return viewfinderView;
    }

    public Handler getHandler() {
        return handler;
    }

    CameraManager getCameraManager() {
        return cameraManager;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        // Window window = getWindow();
        // window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.capture_activity);

        hasSurface = false;

        init();
        findViews();
        initTitleBar();
        setListener();
    }

    /**
     * 初始化标题栏
     */
    private void initTitleBar() {
        mTitleBar.setTitle(R.string.scan_title_txt);
        mTitleBar.addBackButton(new OnClickListener() {

            @Override
            public void onClick(View v) {
                finish();
            }
        });
        mTitleBar.addRightButton(R.drawable.common_title_input_selector, new OnClickListener() {

            @Override
            public void onClick(View v) {
                EzvizAPI.getInstance().gotoAddDevicePage();
            }
        });
    }

    private void init() {
        mInactivityTimer = new InactivityTimer(this);
    }

    private void findViews() {
        mTitleBar = (TitleBar) findViewById(R.id.title_bar);
        mViewfinderView = (ViewfinderView) findViewById(R.id.viewfinder_view);
        mTxtResult = (TextView) findViewById(R.id.txtResult);
        mTxtResult = (TextView) findViewById(R.id.txtResult);
    }

    private void setListener() {

        ViewTreeObserver vto = mTxtResult.getViewTreeObserver();

        vto.addOnPreDrawListener(/**
         * @ClassName: 匿名类
         * @Description: 用于监听在重绘前获得控件的大小按照屏幕的比例放置控件的位置
         * @author wangnanayf1
         * @date 2012-12-3 上午9:14:25
         */
        new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                if (mHasMeasured == false) {
                    DisplayMetrics dm = new DisplayMetrics();
                    // 取得窗口属性
                    getWindowManager().getDefaultDisplay().getMetrics(dm);
                    int windowsHeight = dm.heightPixels;
                    int windowsWidth = dm.heightPixels;
                    int moveLength = (int) ((windowsHeight - windowsWidth * 0.83f) / 2 - mTxtResult.getMeasuredHeight() / 2f);
                    LogUtil.debugLog(TAG, "moveLength = " + moveLength);
                    if (moveLength > 0) {
                        // 移动控件的位置
                        mTxtResult.setPadding(0, 0, 0, moveLength);
                    }

                    mHasMeasured = true;
                }
                return true;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        cameraManager = new CameraManager(getApplication());

        viewfinderView = (ViewfinderView) findViewById(R.id.viewfinder_view);
        viewfinderView.setCameraManager(cameraManager);

        handler = null;

        // resetStatusView();

        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
        SurfaceHolder surfaceHolder = surfaceView.getHolder();
        if (hasSurface) {
            // The activity was paused but not stopped, so the surface still
            // exists. Therefore
            // surfaceCreated() won't be called, so init the camera here.
            initCamera();
        } else {
            // Install the callback and wait for surfaceCreated() to init the
            // camera.
            surfaceHolder.addCallback(this);
            surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }

        mInactivityTimer.onResume();

        Intent intent = getIntent();

        source = IntentSource.NONE;
        decodeFormats = null;
        characterSet = null;

        if (intent != null) {

            String action = intent.getAction();
            String dataString = intent.getDataString();

            if (Intents.Scan.ACTION.equals(action)) {

                // Scan the formats the intent requested, and return the result
                // to the calling activity.
                source = IntentSource.NATIVE_APP_INTENT;
                decodeFormats = DecodeFormatManager.parseDecodeFormats(intent);

                if (intent.hasExtra(Intents.Scan.WIDTH) && intent.hasExtra(Intents.Scan.HEIGHT)) {
                    int width = intent.getIntExtra(Intents.Scan.WIDTH, 0);
                    int height = intent.getIntExtra(Intents.Scan.HEIGHT, 0);
                    if (width > 0 && height > 0) {
                        cameraManager.setManualFramingRect(width, height);
                    }
                }

                // String customPromptMessage = intent
                // .getStringExtra(Intents.Scan.PROMPT_MESSAGE);

            } else if (dataString != null && dataString.contains(PRODUCT_SEARCH_URL_PREFIX)
                    && dataString.contains(PRODUCT_SEARCH_URL_SUFFIX)) {

                // Scan only products and send the result to mobile Product
                // Search.
                source = IntentSource.PRODUCT_SEARCH_LINK;
                sourceUrl = dataString;
                decodeFormats = DecodeFormatManager.PRODUCT_FORMATS;

            } else if (isZXingURL(dataString)) {

                // Scan formats requested in query string (all formats if none
                // specified).
                // If a return URL is specified, send the results there.
                // Otherwise, handle it ourselves.
                source = IntentSource.ZXING_LINK;
                sourceUrl = dataString;
                Uri inputUri = Uri.parse(sourceUrl);
                // returnUrlTemplate =
                // inputUri.getQueryParameter(RETURN_URL_PARAM);
                decodeFormats = DecodeFormatManager.parseDecodeFormats(inputUri);

            }

            characterSet = intent.getStringExtra(Intents.Scan.CHARACTER_SET);

        }
    }

    private static boolean isZXingURL(String dataString) {
        if (dataString == null) {
            return false;
        }
        for (String url : ZXING_URLS) {
            if (dataString.startsWith(url)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onPause() {
        if (handler != null) {
            handler.quitSynchronously();
            handler = null;
        }
        mInactivityTimer.onPause();
        cameraManager.closeDriver();
        if (!hasSurface) {
            SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
            SurfaceHolder surfaceHolder = surfaceView.getHolder();
            surfaceHolder.removeCallback(this);
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mInactivityTimer.shutdown();
        super.onDestroy();
    }

    private void decodeOrStoreSavedBitmap(Bitmap bitmap, Result result) {
        // Bitmap isn't used yet -- will be used soon
        if (handler == null) {
            savedResultToShow = result;
        } else {
            if (result != null) {
                savedResultToShow = result;
            }
            if (savedResultToShow != null) {
                Message message = Message.obtain(handler, R.id.decode_succeeded, savedResultToShow);
                handler.sendMessage(message);
            }
            savedResultToShow = null;
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (holder == null) {
            LogUtil.errorLog(TAG, "*** WARNING *** surfaceCreated() gave us a null surface!");
        }
        if (!hasSurface) {
            hasSurface = true;
            initCamera();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        hasSurface = false;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    /**
     * A valid barcode has been found, so give an indication of success and show the results.
     * 
     * @param rawResult
     *            The contents of the barcode.
     * @param barcode
     *            A greyscale bitmap of the camera data which was decoded.
     */
    public void handleDecode(Result rawResult, Bitmap barcode) {
        mInactivityTimer.onActivity();
        playBeepSoundAndVibrate();

        String resultString = rawResult.getText();
        if (resultString == null) {
            LogUtil.errorLog(TAG, "handleDecode-> resultString is null");
            return;
        }
        // 初始化数据
        mSerialNo = "";
        mSerialVerifyCode = "";
        deviceType = "";
        LogUtil.errorLog(TAG, resultString);
        // CS-F1-1WPFR
        // CS-A1-1WPFR
        // CS-C1-1FPFR
        // resultString = "www.xxx.com\n456654855\nABCDEF\nCS-C3-21PPFR\n";

        // 字符集合
        String[] newlineCharacterSet = {
            "\n\r", "\r\n", "\r", "\n"};

        String stringOrigin = resultString;

        // 寻找第一次出现的位置
        int a = -1;
        int firstLength = 1;
        for (String string : newlineCharacterSet) {
            if (a == -1) {
                a = resultString.indexOf(string);
                if (a > stringOrigin.length() - 3) {
                    a = -1;
                }
                if (a != -1) {
                    firstLength = string.length();
                }
            }
        }

        // 扣去第一次出现回车的字符串后，剩余的是第二行以及以后的
        if (a != -1) {
            resultString = TextUtils.substring(resultString, a + firstLength, resultString.length());
        }
        // 寻找最后一次出现的位置
        int b = -1;
        for (String string : newlineCharacterSet) {
            if (b == -1) {
                b = resultString.indexOf(string);
                if (b != -1) {
                    mSerialNo = TextUtils.substring(resultString, 0, b);
                    firstLength = string.length();
                }
            }
        }

        // 寻找遗失的验证码阶段
        if (mSerialNo != null && b != -1 && (b + firstLength) <= resultString.length()) {
            resultString = TextUtils.substring(resultString, b + firstLength, resultString.length());
        }

        // 再次寻找回车键最后一次出现的位置
        int c = -1;
        for (String string : newlineCharacterSet) {
            if (c == -1) {
                c = resultString.indexOf(string);
                if (c != -1) {
                    mSerialVerifyCode = TextUtils.substring(resultString, 0, c);
                }
            }
        }

        // 寻找CS-C2-21WPFR 判断是否支持wifi
        if (mSerialNo != null && c != -1 && (c + firstLength) <= resultString.length()) {
            resultString = TextUtils.substring(resultString, c + firstLength, resultString.length());
        }
        if (resultString != null && resultString.length() > 0) {
            deviceType = resultString;
        }

        if (b == -1) {
            mSerialNo = resultString;
        }

        if (mSerialNo == null) {
            mSerialNo = stringOrigin;
        }
        LogUtil.debugLog(TAG, "mSerialNoStr = " + mSerialNo + ",mSerialVeryCodeStr = " + mSerialVerifyCode
                + ",deviceType = " + deviceType);
        // 判断是不是9位
        isValidate();
    }

    /**
     * 判断是不是合法
     * 
     * @throws
     */
    private void isValidate() {
        if (mSerialNo == null || mSerialNo.length() != SERIAL_NO_LENGTH) {
            handleLocalValidateSerialNoFail();
            return;
        }
//        EzvizAPI.getInstance().gotoAddDevicePage(mSerialNo, mSerialVerifyCode);
        Intent intent = new Intent(this, AutoWifiNetConfigActivity.class);
        intent.putExtra(IntentConsts.EXTRA_DEVICE_ID, mSerialNo);
        intent.putExtra(IntentConsts.EXTRA_DEVICE_CODE, mSerialVerifyCode);
        intent.putExtra(IntentConsts.EXTRA_DEVICE_TYPE, deviceType);
        startActivity(intent);
        finish();
    }

    private void handleLocalValidateSerialNoFail() {
        Utils.showToast(CaptureActivity.this, R.string.serial_number_error);
        onPause();
        onResume();
    }

    private void initCamera() {
        try {

            // 声音效果
            initBeepSound();
            // 是否震动
            mVibrate = true;
            mSerialNo = null;

            SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
            SurfaceHolder surfaceHolder = surfaceView.getHolder();
            cameraManager.openDriver(surfaceHolder);
            // Creating the handler starts the preview, which can also throw a
            // RuntimeException.
            if (handler == null) {
                handler = new CaptureActivityHandler(this, decodeFormats, characterSet, cameraManager);
            }
            decodeOrStoreSavedBitmap(null, null);
        } catch (IOException ioe) {
            LogUtil.warnLog(TAG, ioe);
            displayFrameworkBugMessageAndExit();
        } catch (RuntimeException e) {
            // Barcode Scanner has seen crashes in the wild of this variety:
            // java.?lang.?RuntimeException: Fail to connect to camera service
            LogUtil.warnLog(TAG, "Unexpected error initializing camera", e);
            displayFrameworkBugMessageAndExit();
        }
    }

    private void displayFrameworkBugMessageAndExit() {
        Utils.showToast(CaptureActivity.this, R.string.open_camera_fail);

    }

    public void restartPreviewAfterDelay(long delayMS) {
        if (handler != null) {
            handler.sendEmptyMessageDelayed(R.id.restart_preview, delayMS);
        }
    }

    public void drawViewfinder() {
        viewfinderView.drawViewfinder();
    }

    /**
     * 初始化音效,并设置监听
     */
    private void initBeepSound() {
        if (mPlayBeep) {
            // The volume on STREAM_SYSTEM is not adjustable, and users found it
            // too loud,
            // so we now play on the music stream.
            setVolumeControlStream(AudioManager.STREAM_MUSIC);
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.setOnCompletionListener(beepListener);

            AssetFileDescriptor file = getResources().openRawResourceFd(R.raw.beep);
            try {
                mMediaPlayer.setDataSource(file.getFileDescriptor(), file.getStartOffset(), file.getLength());
                file.close();
                mMediaPlayer.setVolume(BEEP_VOLUME, BEEP_VOLUME);
                mMediaPlayer.prepare();
            } catch (IOException e) {
                mMediaPlayer = null;
            }
        }
    }

    /**
     * 设置声音以及震动
     * 
     * @throws
     */
    private void playBeepSoundAndVibrate() {
        if (mPlayBeep && mMediaPlayer != null) {
            mMediaPlayer.start();
        }
        if (mVibrate) {
            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            vibrator.vibrate(VIBRATE_DURATION);
        }
    }

    /**
     * When the beep has finished playing, rewind to queue up another one.
     */
    private final OnCompletionListener beepListener = new OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mediaPlayer) {
            mediaPlayer.seekTo(0);
        }
    };

    @Override
    public void finish() {
        super.finish();
        this.overridePendingTransition(0, R.anim.fade_down);
    }
}

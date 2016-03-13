/* 
 * @ProjectName VideoGo
 * @Copyright HangZhou Hikvision System Technology Co.,Ltd. All Right Reserved
 * 
 * @FileName RemotePlayBackActivity.java
 * @Description 这里对文件进行描述
 * 
 * @author chenxingyf1
 * @data 2014-7-1
 * 
 * @note 这里写本文件的详细功能描述和注释
 * @note 历史记录
 * 
 * @warning 这里写本文件的相关警告
 */
package com.videogo.ui.remoteplayback;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.DatePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.DatePicker;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.videogo.R;
import com.videogo.constant.Constant;
import com.videogo.constant.IntentConsts;
import com.videogo.exception.BaseException;
import com.videogo.exception.CASClientSDKException;
import com.videogo.exception.ErrorCode;
import com.videogo.exception.HCNetSDKException;
import com.videogo.exception.InnerException;
import com.videogo.exception.RtspClientException;
import com.videogo.openapi.EzvizAPI;
import com.videogo.openapi.bean.req.GetCloudFileList;
import com.videogo.openapi.bean.resp.CameraInfo;
import com.videogo.openapi.bean.resp.CloudFile;
import com.videogo.remoteplayback.RemoteFileInfo;
import com.videogo.remoteplayback.RemotePlayBackHelper;
import com.videogo.remoteplayback.RemotePlayBackManager;
import com.videogo.remoteplayback.RemotePlayBackMsg;
import com.videogo.ui.util.AudioPlayUtil;
import com.videogo.ui.util.VerifySmsCodeUtil;
import com.videogo.util.ConnectionDetector;
import com.videogo.util.LocalInfo;
import com.videogo.util.LogUtil;
import com.videogo.util.RotateViewUtil;
import com.videogo.util.SDCardUtil;
import com.videogo.util.Utils;
import com.videogo.widget.HVScrollView;
import com.videogo.widget.TimeBarHorizontalScrollView;
import com.videogo.widget.WaitDialog;
import com.videogo.widget.TimeBarHorizontalScrollView.TimeScrollBarScrollListener;
import com.videogo.widget.TitleBar;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 时间轴回放界面2.0
 * @author chenxingyf1
 * @data 2014-7-1
 */
public class RemotePlayBackActivity extends Activity implements OnClickListener, SurfaceHolder.Callback, 
    Handler.Callback, OnTouchListener, TimeScrollBarScrollListener, VerifySmsCodeUtil.OnVerifyListener {
    private static final String TAG = "RemotePlayBackActivity";
    //UI消息
    public static final int MSG_PLAY_UI_UPDATE = 100;  
    //搜索文件成功
    public static final int MSG_SEARCH_CLOUD_FILE_SUCCUSS = 101;
    //搜索文件失败
    public static final int MSG_SEARCH_CLOUD_FILE_FAIL = 102;
    
    public static final int ALARM_MAX_DURATION = 120;
    
    //播放状态
    /** 初始状态 */
    public static final int STATUS_INIT = 0;
    /** 链接状态 */
    public static final int STATUS_START = 1;
    /** 停止状态 */
    public static final int STATUS_STOP = 2;
    /** 播放状态 */
    public static final int STATUS_PLAY = 3;
    /** 暂停状态 */
    public static final int STATUS_PAUSE = 4;
    
    /** 报警发生时间 */
    private CameraInfo mCameraInfo = null;
    private List<CloudFile> mCloudFileList = null;
    private List<RemoteFileInfo> mDeviceFileList = null;
    private Calendar mStartTime = null;
    private Calendar mEndTime = null;
    private Calendar mAlarmStartTime = null;
    private Calendar mAlarmStopTime = null;
    
    /** 实时预览控制对象 */
    private RemotePlayBackManager mRemotePlayBackMgr = null;
    private RemotePlayBackHelper mRemotePlayBackHelper = null;
    private AudioPlayUtil mAudioPlayUtil = null;
    private LocalInfo mLocalInfo = null;
    private Handler mHandler = null;
    
    private float mRealRatio = Constant.LIVE_VIEW_RATIO;
    /** 标识是否正在播放 */
    private int mStatus = STATUS_INIT;
    private boolean mIsOnStop = false;
    /** 屏幕当前方向 */
    private int mOrientation = Configuration.ORIENTATION_PORTRAIT;
    /** 存放上一次的流量 */
    private long mStreamFlow = 0;
    private long mTotalStreamFlow = 0;
    private Rect mRemotePlayBackRect = null;
    
    private RelativeLayout mRemotePlayBackPageLy = null;
    private TitleBar mTitleBar = null;
    
    private SurfaceView mRemotePlayBackSv = null;
    private SurfaceHolder mRemotePlayBackSh = null;
    private GestureDetector mPlaySvGestureDetector = null;
    
    private LinearLayout mRemotePlayBackLoadingLy = null;
    private LinearLayout mRemotePlayBackLoadingPbLy = null;
    private TextView mRemotePlayBackLoadingTv = null;    
    private TextView mRemotePlayBackTipTv = null;
    private ImageButton mRemotePlayBackReplayBtn = null;
    private ImageButton mRemotePlayBackLoadingPlayBtn = null;

    private RelativeLayout mRemotePlayBackControlRl = null;
    private ImageButton mRemotePlayBackBtn = null;
    private ImageButton mRemotePlayBackSoundBtn = null;
    private TextView mRemotePlayBackFlowTv = null;
    private int mControlDisplaySec = 0;
    private long mPlayTime = 0;
    
    private LinearLayout mRemotePlayBackProgressLy = null;
    private TextView mRemotePlayBackBeginTimeTv = null;
    private TextView mRemotePlayBackEndTimeTv = null;
    private SeekBar mRemotePlayBackSeekBar = null;
    private ProgressBar mRemotePlayBackProgressBar = null;
    
    private RelativeLayout mRemotePlayBackCaptureRl = null;
    private RelativeLayout.LayoutParams mRemotePlayBackCaptureRlLp = null;
    private ImageView mRemotePlayBackCaptureIv = null;
    private ImageView mRemotePlayBackCaptureWatermarkIv = null;
    private int mCaptureDisplaySec = 0;
    private LinearLayout mRemotePlayBackRecordLy = null;
    private ImageView mRemotePlayBackRecordIv = null;
    private TextView mRemotePlayBackRecordTv = null;
    
    /** 录像文件路径 */
    private String mRecordFilePath = null;
    private String mRecordTime = null;
    /** 录像时间 */
    private int mRecordSecond = 0;
    
    private LinearLayout mRemotePlayBackOperateBar = null;
    private ImageButton mRemotePlayBackCaptureBtn = null;
    private ImageButton mRemotePlayBackRecordBtn = null;
    private ImageButton mRemotePlayBackRecordStartBtn = null;
    private View mRemotePlayBackRecordContainer = null;
    private RotateViewUtil mRecordRotateViewUtil = null;
    
    private ImageButton mRemotePlayBackSmallRecordBtn = null;
    private ImageButton mRemotePlayBackSmallRecordStartBtn = null;
    private View mRemotePlayBackSmallRecordContainer = null;   
    private ImageButton mRemotePlayBackSmallCaptureBtn = null;
    
    private RelativeLayout mRemotePlayBackFullOperateBar = null;
    private ImageButton mRemotePlayBackFullPlayBtn = null;
    private ImageButton mRemotePlayBackFullSoundBtn = null;
    private ImageButton mRemotePlayBackFullCaptureBtn = null;
    private ImageButton mRemotePlayBackFullRecordBtn = null;
    private ImageButton mRemotePlayBackFullRecordStartBtn = null;
    private View mRemotePlayBackFullRecordContainer = null;
    
    private ImageButton mRemotePlayBackFullDownBtn = null;
    private LinearLayout mRemotePlayBackFullFlowLy = null;
    private TextView mRemotePlayBackFullRateTv = null;
    private TextView mRemotePlayBackFullFlowTv = null;
    private TextView mRemotePlayBackRatioTv = null;
    
    private RelativeLayout mRemotePlayBackTimeBarRl = null;
    private TimeBarHorizontalScrollView mRemotePlayBackTimeBar = null;
    private RemoteFileTimeBar mRemoteFileTimeBar = null;
    private TextView mRemotePlayBackTimeTv = null;
    
    /** 监听锁屏解锁的事件 */
    private ScreenBroadcastReceiver mScreenBroadcastReceiver = null;
    /** 定时器 */
    private Timer mUpdateTimer = null;
    /** 定时器执行的任务 */
    private TimerTask mUpdateTimerTask = null;

    private HVScrollView mHVScrollView;
    
    private WaitDialog mWaitDialog = null;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        initData();
        initView();
    }
    
    //初始化数据对象
    private void initData() {
        Intent intent = getIntent();
        if (intent != null) {
            mAlarmStartTime = Utils.parseTimeToCalendar(intent.getStringExtra(IntentConsts.EXTRA_ALARM_TIME));
            mCameraInfo = (CameraInfo)intent.getParcelableExtra(IntentConsts.EXTRA_CAMERA_INFO);           
        }
        
        if(mAlarmStartTime != null) {
            mAlarmStartTime.add(Calendar.SECOND, -5);   
            mAlarmStopTime = (Calendar)mAlarmStartTime.clone();
            mAlarmStopTime.add(Calendar.SECOND, ALARM_MAX_DURATION); 
        } else {
            mStartTime = Calendar.getInstance();
            mStartTime.set(Calendar.AM_PM, 0);
            mStartTime.set(mStartTime.get(Calendar.YEAR), mStartTime.get(Calendar.MONTH), 
                    mStartTime.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
            mEndTime = Calendar.getInstance();
            mEndTime.set(Calendar.AM_PM, 0);
            mEndTime.set(mEndTime.get(Calendar.YEAR), mEndTime.get(Calendar.MONTH), 
                    mEndTime.get(Calendar.DAY_OF_MONTH), 23, 59, 59);
        }
        // 获取本地信息
        Application application = (Application) getApplication();
        mRemotePlayBackHelper = RemotePlayBackHelper.getInstance(application);
        mAudioPlayUtil = AudioPlayUtil.getInstance(application);
        // 获取配置信息操作对象
        mLocalInfo = LocalInfo.getInstance();
        // 获取屏幕参数
        DisplayMetrics metric = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metric);
        mLocalInfo.setScreenWidthHeight(metric.widthPixels, metric.heightPixels);
        mLocalInfo.setNavigationBarHeight((int) Math.ceil(25 * getResources().getDisplayMetrics().density));
        
        mHandler = new Handler(this);   
        mRecordRotateViewUtil = new RotateViewUtil();
        
        mScreenBroadcastReceiver = new ScreenBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_USER_PRESENT);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(mScreenBroadcastReceiver, filter);
    }
    
    //初始化界面
    private  void initView() {
        setContentView(R.layout.remote_playback_page);
        // 保持屏幕常亮
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        mTitleBar = (TitleBar)findViewById(R.id.title_bar);
        mTitleBar.addBackButton(new OnClickListener() {                                           
            
            @Override                                           
            public void onClick(View v) {                                           
                if(mStatus != STATUS_STOP) {
                    stopRemotePlayBack();
                }
                finish();
            }                                           
        });        
        if(mAlarmStartTime == null) {
            mTitleBar.setTitle(Utils.date2String(mStartTime.getTime()));
            mTitleBar.addTitleButton(R.drawable.remote_cal_selector, new OnClickListener() {
                @Override
                public void onClick(View v) {
                    showDatePicker();
                }
            });
            mTitleBar.setOnTitleClickListener(new OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    showDatePicker();
                }
            });
        }
        
        mRemotePlayBackPageLy = (RelativeLayout)findViewById(R.id.remoteplayback_page_ly);
        /** 测量状态栏高度 **/
        ViewTreeObserver viewTreeObserver = mRemotePlayBackPageLy.getViewTreeObserver();
        viewTreeObserver.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (mRemotePlayBackRect == null) {
                    // 获取状况栏高度
                    mRemotePlayBackRect = new Rect();
                    getWindow().getDecorView().getWindowVisibleDisplayFrame(mRemotePlayBackRect);
                }
            }
        });
        mHVScrollView = (HVScrollView) findViewById(R.id.remoteplayback_sv_view);
        mRemotePlayBackSv = (SurfaceView) findViewById(R.id.remoteplayback_sv);
        mRemotePlayBackSv.getHolder().addCallback(this);
        mRemotePlayBackSv.setOnTouchListener(this);
        mPlaySvGestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                onRemotePlayBackSvClick();
                return true;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                onRemotePlayBackSvDoubleClick(e);
                return true;
            }            
        });
        mRemotePlayBackLoadingLy = (LinearLayout)findViewById(R.id.remoteplayback_loading_ly);
        mRemotePlayBackLoadingPbLy = (LinearLayout)findViewById(R.id.remoteplayback_loading_pb_ly);
        mRemotePlayBackLoadingTv = (TextView) findViewById(R.id.remoteplayback_loading_tv);
        mRemotePlayBackTipTv = (TextView)findViewById(R.id.remoteplayback_tip_tv);
        mRemotePlayBackReplayBtn = (ImageButton)findViewById(R.id.remoteplayback_replay_btn);
        mRemotePlayBackLoadingPlayBtn = (ImageButton) findViewById(R.id.remoteplayback_loading_play_btn);
        
        mRemotePlayBackControlRl = (RelativeLayout)findViewById(R.id.remoteplayback_control_rl);
        mRemotePlayBackBtn = (ImageButton)findViewById(R.id.remoteplayback_play_btn);
        mRemotePlayBackSoundBtn = (ImageButton) findViewById(R.id.remoteplayback_sound_btn);
        mRemotePlayBackFlowTv = (TextView)findViewById(R.id.remoteplayback_flow_tv);
        mRemotePlayBackFlowTv.setText("0k/s 0MB");
        
        mRemotePlayBackProgressLy = (LinearLayout)findViewById(R.id.remoteplayback_progress_ly);
        mRemotePlayBackBeginTimeTv = (TextView)findViewById(R.id.remoteplayback_begin_time_tv);
        mRemotePlayBackEndTimeTv = (TextView)findViewById(R.id.remoteplayback_end_time_tv);
        mRemotePlayBackSeekBar = (SeekBar)findViewById(R.id.remoteplayback_progress_seekbar);
        mRemotePlayBackProgressBar = (ProgressBar)findViewById(R.id.remoteplayback_progressbar);
        
        mRemotePlayBackCaptureRl = (RelativeLayout)findViewById(R.id.remoteplayback_capture_rl);
        mRemotePlayBackCaptureRlLp = (RelativeLayout.LayoutParams) mRemotePlayBackCaptureRl.getLayoutParams();
        mRemotePlayBackCaptureIv = (ImageView)findViewById(R.id.remoteplayback_capture_iv); 
        mRemotePlayBackCaptureWatermarkIv = (ImageView)findViewById(R.id.remoteplayback_capture_watermark_iv); 
        mRemotePlayBackRecordLy = (LinearLayout)findViewById(R.id.remoteplayback_record_ly);
        mRemotePlayBackRecordIv = (ImageView) findViewById(R.id.remoteplayback_record_iv); 
        mRemotePlayBackRecordTv = (TextView)findViewById(R.id.remoteplayback_record_tv);
        
        mRemotePlayBackOperateBar = (LinearLayout)findViewById(R.id.remoteplayback_operate_bar);
        mRemotePlayBackCaptureBtn = (ImageButton)findViewById(R.id.remoteplayback_previously_btn);
        mRemotePlayBackRecordBtn = (ImageButton)findViewById(R.id.remoteplayback_video_btn);
        mRemotePlayBackRecordContainer = findViewById(R.id.remoteplayback_video_container);
        mRemotePlayBackRecordStartBtn = (ImageButton) findViewById(R.id.remoteplayback_video_start_btn);

        mRemotePlayBackSmallCaptureBtn = (ImageButton)findViewById(R.id.remoteplayback_small_previously_btn);
        mRemotePlayBackSmallRecordBtn = (ImageButton)findViewById(R.id.remoteplayback_small_video_btn);
        mRemotePlayBackSmallRecordContainer = findViewById(R.id.remoteplayback_small_video_container);
        mRemotePlayBackSmallRecordStartBtn = (ImageButton) findViewById(R.id.remoteplayback_small_video_start_btn);
        
        mRemotePlayBackFullOperateBar = (RelativeLayout)findViewById(R.id.remoteplayback_full_operate_bar);
        mRemotePlayBackFullPlayBtn = (ImageButton)findViewById(R.id.remoteplayback_full_play_btn);
        mRemotePlayBackFullSoundBtn = (ImageButton) findViewById(R.id.remoteplayback_full_sound_btn);
        mRemotePlayBackFullCaptureBtn = (ImageButton)findViewById(R.id.remoteplayback_full_previously_btn);
        mRemotePlayBackFullRecordBtn = (ImageButton)findViewById(R.id.remoteplayback_full_video_btn);
        mRemotePlayBackFullRecordContainer = findViewById(R.id.remoteplayback_full_video_container);
        mRemotePlayBackFullRecordStartBtn = (ImageButton) findViewById(R.id.remoteplayback_full_video_start_btn);
        mRemotePlayBackFullDownBtn = (ImageButton)findViewById(R.id.remoteplayback_full_down_btn);
        
        mRemotePlayBackFullFlowLy = (LinearLayout)findViewById(R.id.remoteplayback_full_flow_ly);
        mRemotePlayBackFullRateTv = (TextView)findViewById(R.id.remoteplayback_full_rate_tv);
        mRemotePlayBackFullFlowTv = (TextView)findViewById(R.id.remoteplayback_full_flow_tv);
        mRemotePlayBackRatioTv = (TextView)findViewById(R.id.remoteplayback_ratio_tv); 
        mRemotePlayBackFullRateTv.setText("0k/s");
        mRemotePlayBackFullFlowTv.setText("0MB");  
        
        mRemotePlayBackTimeBarRl = (RelativeLayout) findViewById(R.id.remoteplayback_timebar_rl);
        mRemotePlayBackTimeBar = (TimeBarHorizontalScrollView) findViewById(R.id.remoteplayback_timebar);
        mRemotePlayBackTimeBar.setTimeScrollBarScrollListener(this);
        mRemotePlayBackTimeBar.smoothScrollTo(0, 0);
        mRemoteFileTimeBar = (RemoteFileTimeBar) findViewById(R.id.remoteplayback_file_time_bar);
        mRemoteFileTimeBar.setX(0, mLocalInfo.getScreenWidth() * 6);
        mRemotePlayBackTimeTv = (TextView) findViewById(R.id.remoteplayback_time_tv);
        mRemotePlayBackTimeTv.setText("00:00:00");
        
        mWaitDialog = new WaitDialog(this, android.R.style.Theme_Translucent_NoTitleBar);
        mWaitDialog.setCancelable(false);
        
        setRemotePlayBackSvLayout(1);
        
        if(mAlarmStartTime != null) {
            mRemotePlayBackTimeBarRl.setVisibility(View.GONE);
            mRemotePlayBackProgressLy.setVisibility(View.VISIBLE);
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
            mRemotePlayBackBeginTimeTv.setText(sdf.format(mAlarmStartTime.getTimeInMillis()));
            mRemotePlayBackEndTimeTv.setText(sdf.format(mAlarmStopTime.getTimeInMillis()));
            mRemotePlayBackProgressBar.setMax(ALARM_MAX_DURATION);
            mRemotePlayBackProgressBar.setProgress(0);
            mRemotePlayBackSeekBar.setMax(ALARM_MAX_DURATION);
            mRemotePlayBackSeekBar.setProgress(0);
            mRemotePlayBackSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
                /**
                 * 拖动条停止拖动的时候调用
                 */
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    int progress = seekBar.getProgress();
                    if(mStatus != STATUS_STOP) {
                        stopRemotePlayBack();
                    }
                    Calendar seletedTime = (Calendar)mAlarmStartTime.clone();
                    seletedTime.add(Calendar.SECOND, progress); 
                    mPlayTime = seletedTime.getTimeInMillis();
                    mRemotePlayBackProgressBar.setProgress(progress);
                    startRemotePlayBack(seletedTime);
                }

                /**
                 * 拖动条开始拖动的时候调用
                 */
                @Override
                public void onStartTrackingTouch(SeekBar arg0) {
                }

                /**
                 * 拖动条进度改变的时候调用
                 */
                @Override
                public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
                }
            });
        } else {
            mRemotePlayBackTimeBarRl.setVisibility(View.VISIBLE);
        }
    }
    
    private void initUI() {
        if(mCameraInfo == null) {
            return;
        }
        if(mAlarmStartTime != null) {
            mTitleBar.setTitle(mCameraInfo.getCameraName());
        }
    }
    
    private void showDatePicker() {
        DatePickerDialog dpd = new DatePickerDialog(this, null, mStartTime.get(Calendar.YEAR),
                mStartTime.get(Calendar.MONTH), mStartTime.get(Calendar.DAY_OF_MONTH));

        dpd.setCancelable(true);
        dpd.setTitle(R.string.select_date);
        dpd.setCanceledOnTouchOutside(true);
        dpd.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.certain),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dg, int which) {
                        DatePicker dp = null;
                        Field[] fields = dg.getClass().getDeclaredFields();
                        for (Field field : fields) {
                            field.setAccessible(true);
                            if (field.getName().equals("mDatePicker")) {
                                try {
                                    dp = (DatePicker) field.get(dg);
                                } catch (IllegalArgumentException e) {
                                    e.printStackTrace();
                                } catch (IllegalAccessException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        dp.clearFocus();

                        if(mStatus != STATUS_STOP) {
                            stopRemotePlayBack();
                        }
                        
                        mCloudFileList = null;
                        mPlayTime = 0;
                        mStartTime = Calendar.getInstance();
                        mStartTime.set(Calendar.AM_PM, 0);
                        mStartTime.set(dp.getYear(), dp.getMonth(), dp.getDayOfMonth(), 0, 0, 0);
                        mEndTime = Calendar.getInstance();
                        mEndTime.set(Calendar.AM_PM, 0);
                        mEndTime.set(dp.getYear(), dp.getMonth(), dp.getDayOfMonth(), 23, 59, 59);
                        mTitleBar.setTitle(Utils.date2String(mStartTime.getTime()));

                        startRemotePlayBack(null);
                    }
                });
        dpd.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.cancel),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        LogUtil.debugLog("Picker", "Cancel!");
                        if (!isFinishing()) {
                            dialog.dismiss();
                        }

                    }
                });

        dpd.show();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        mRemotePlayBackSv.setVisibility(View.VISIBLE);
        if(mCameraInfo == null) {
            return;
        }
        initUI();
        if(mCameraInfo.getStatus() != 1) {
            if(mStatus != STATUS_STOP) {
                stopRemotePlayBack();
            }
            setRemotePlayBackFailUI(getString(R.string.camera_not_online));
        } else {        
            if (mStatus == STATUS_INIT || mStatus == STATUS_PAUSE) { 
                // 开始播放
                startRemotePlayBack(getTimeBarSeekTime());
            } else if(mIsOnStop) {
                if(mStatus != STATUS_STOP) {
                    stopRemotePlayBack();
                }
                // 开始播放
                startRemotePlayBack(getTimeBarSeekTime());                
            }
        }
        mIsOnStop = false;
    }
    
    /**
     * 获取时间轴定位时间
     * 
     * @see
     * @since V1.0
     */

    private Calendar getTimeBarSeekTime() {
        if(mAlarmStartTime != null) {
            int progress = mRemotePlayBackSeekBar.getProgress();
            Calendar seletedTime = (Calendar)mAlarmStartTime.clone();
            if(progress < ALARM_MAX_DURATION) {
                seletedTime.add(Calendar.SECOND, progress); 
            }
            return seletedTime;
        } else {
            return mRemoteFileTimeBar.pos2Calendar(mPlayTime == 0?0:mRemotePlayBackTimeBar.getScrollX(), mOrientation);
        }
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        if(mCameraInfo == null) {
            return;
        }
        if(mStatus != STATUS_STOP) {
            mIsOnStop = true;
            stopRemotePlayBack();
            setRemotePlayBackStopUI();
        }
        mRemotePlayBackSv.setVisibility(View.INVISIBLE);
    }
    
    /*
     * (non-Javadoc)
     * @see android.app.Activity#onDestroy()
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mScreenBroadcastReceiver != null) {
            // 取消锁屏广播的注册
            unregisterReceiver(mScreenBroadcastReceiver);
        }
        mRemotePlayBackHelper.clearCacheData();
    }
    
    /**
     * screen状态广播接收者
     */
    private class ScreenBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                if(mStatus != STATUS_STOP) {
                    if(mStatus == STATUS_PLAY) {
                        pauseRemotePlayBack();
                    } else {
                        stopRemotePlayBack();
                    }
                    setRemotePlayBackStopUI();
                }
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        mOrientation = newConfig.orientation;
        onOrientationChanged();
        super.onConfigurationChanged(newConfig);
    }

    private void updateSoundUI() {
        if (mLocalInfo.isSoundOpen()) {
            mRemotePlayBackSoundBtn.setBackgroundResource(R.drawable.remote_list_soundon_btn_selector);
            mRemotePlayBackFullSoundBtn.setBackgroundResource(R.drawable.play_full_soundon_btn_selector);
        } else {
            mRemotePlayBackSoundBtn.setBackgroundResource(R.drawable.remote_list_soundoff_btn_selector);
            mRemotePlayBackFullSoundBtn.setBackgroundResource(R.drawable.play_full_soundoff_btn_selector);
        }
    }

    private void updateTimeBarUI() {
        if(mAlarmStartTime != null) {
            if(mRemotePlayBackControlRl.getVisibility() == View.VISIBLE) {
                RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams)mRemotePlayBackProgressLy.getLayoutParams();
                lp.setMargins(0, 0, 0, Utils.dip2px(this, 40));
                mRemotePlayBackProgressLy.setLayoutParams(lp);
            } else {
                RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams)mRemotePlayBackProgressLy.getLayoutParams();
                lp.setMargins(0, 0, 0, 0);
                mRemotePlayBackProgressLy.setLayoutParams(lp);               
            }
        } else {
            float pos = mRemoteFileTimeBar.getScrollPosByPlayTime(mPlayTime, mOrientation);
            mRemotePlayBackTimeBar.smoothScrollTo((int)pos, 0);               
        }
    }
    
    private void updateOperatorUI() {
        if(mOrientation == Configuration.ORIENTATION_PORTRAIT) {
            mRemotePlayBackPageLy.setBackgroundColor(getResources().getColor(R.color.common_bg));
            mTitleBar.setVisibility(View.VISIBLE);
            mRemotePlayBackOperateBar.setVisibility(View.VISIBLE);
            mRemotePlayBackFullOperateBar.setVisibility(View.GONE);
            mRemotePlayBackControlRl.setVisibility(View.VISIBLE);
            if(mAlarmStartTime != null) {
                if (mStatus != STATUS_PLAY) {
                    mRemotePlayBackProgressLy.setVisibility(View.GONE);
                } else {
                    mRemotePlayBackProgressLy.setVisibility(View.VISIBLE);
                }
                mRemotePlayBackProgressBar.setVisibility(View.GONE);
            }
            mRemotePlayBackSmallCaptureBtn.setVisibility(View.GONE);
            mRemotePlayBackSmallRecordContainer.setVisibility(View.GONE);
            if(mAlarmStartTime == null) {
                mRemoteFileTimeBar.setX(0, mLocalInfo.getScreenWidth() * 6);
                mRemotePlayBackTimeBarRl.setBackgroundColor(getResources().getColor(R.color.transparent));
                mRemotePlayBackTimeBarRl.setVisibility(View.VISIBLE);
            }
            if(mRecordFilePath != null) {
                mRemotePlayBackRecordBtn.setVisibility(View.GONE);
                mRemotePlayBackRecordStartBtn.setVisibility(View.VISIBLE);
            } else {
                mRemotePlayBackRecordBtn.setVisibility(View.VISIBLE);
                mRemotePlayBackRecordStartBtn.setVisibility(View.GONE);                
            }
        } else {
            mRemotePlayBackPageLy.setBackgroundColor(getResources().getColor(R.color.black_bg));
            mTitleBar.setVisibility(View.GONE);
            mRemotePlayBackOperateBar.setVisibility(View.GONE);
            if(mAlarmStartTime == null) {
                mRemotePlayBackFullOperateBar.setVisibility(View.GONE);
                mRemotePlayBackControlRl.setVisibility(View.GONE);
                mRemoteFileTimeBar.setX(0, mLocalInfo.getScreenHeight() * 6);
                mRemotePlayBackTimeBarRl.setBackgroundColor(getResources().getColor(R.color.play_translucent_bg));
                mRemotePlayBackTimeBarRl.setVisibility(View.GONE);
                mRemotePlayBackFullDownBtn.setBackgroundResource(R.drawable.palyback_full_up);
                mRemotePlayBackFullOperateBar.setPadding(0, 0, 0, Utils.dip2px(this, 5));
            } else {
                mRemotePlayBackControlRl.setVisibility(View.VISIBLE);
                mRemotePlayBackProgressLy.setVisibility(View.VISIBLE);
                mRemotePlayBackProgressBar.setVisibility(View.GONE);
                mRemotePlayBackSmallCaptureBtn.setVisibility(View.VISIBLE);
                mRemotePlayBackSmallRecordContainer.setVisibility(View.VISIBLE);
            }
            if(mRecordFilePath != null) {
                if(mAlarmStartTime != null) {
                    mRemotePlayBackSmallRecordBtn.setVisibility(View.GONE);
                    mRemotePlayBackSmallRecordStartBtn.setVisibility(View.VISIBLE);                
                } else {
                    mRemotePlayBackFullRecordBtn.setVisibility(View.GONE);
                    mRemotePlayBackFullRecordStartBtn.setVisibility(View.VISIBLE);
                }
            } else {
                if(mAlarmStartTime != null) {
                    mRemotePlayBackSmallRecordBtn.setVisibility(View.VISIBLE);
                    mRemotePlayBackSmallRecordStartBtn.setVisibility(View.GONE);                
                } else {
                    mRemotePlayBackFullRecordBtn.setVisibility(View.VISIBLE);
                    mRemotePlayBackFullRecordStartBtn.setVisibility(View.GONE); 
                }               
            }
        }
    }
    
    private void onOrientationChanged() {
        mRemotePlayBackSv.setVisibility(View.INVISIBLE);
        setRemotePlayBackSvLayout(1);
        mRemotePlayBackSv.setVisibility(View.VISIBLE);
        
        updateOperatorUI();
        updateCaptureUI();
        updateTimeBarUI();
    }
    
    /* (non-Javadoc)
     * @see android.view.View.OnTouchListener#onTouch(android.view.View, android.view.MotionEvent)
     */
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch(v.getId()) {
            case R.id.remoteplayback_sv:
                mPlaySvGestureDetector.onTouchEvent(event); 
                break;
            default:
                break;
        }
        return true;
    }
    
    private void updateLoadingProgress(final int progress) {
        mRemotePlayBackLoadingTv.setText(progress + "%");
        mHandler.postDelayed(new Runnable() {

            @Override
            public void run() {
                Random r = new Random();
                mRemotePlayBackLoadingTv.setText((progress + r.nextInt(20)) + "%");
            }
            
        }, 500);
    }
    
    /* (non-Javadoc)
     * @see android.os.Handler.Callback#handleMessage(android.os.Message)
     */
    @Override
    public boolean handleMessage(Message msg) {
        LogUtil.infoLog(TAG, "handleMessage:" + msg.what);
        switch (msg.what) {
            case RemotePlayBackMsg.MSG_REMOTEPLAYBACK_PLAY_START:
                updateLoadingProgress(60);
                break;
            case RemotePlayBackMsg.MSG_REMOTEPLAYBACK_CONNECTION_START:
                break;
            case RemotePlayBackMsg.MSG_REMOTEPLAYBACK_CONNECTION_SUCCESS:
                updateLoadingProgress(80);
                break;                
            case RemotePlayBackMsg.MSG_REMOTEPLAYBACK_PLAY_FINISH:
                handlePlayFinish();
                break;
            case RemotePlayBackMsg.MSG_REMOTEPLAYBACK_RATIO_CHANGED:
                if (msg.arg1 != 0) {
                    mRealRatio = (float) msg.arg2 / msg.arg1;
                }
                setRemotePlayBackSvLayout(1);
                break;
            case RemotePlayBackMsg.MSG_REMOTEPLAYBACK_CONNECTION_EXCEPTION:
                handleConnectionException(msg.arg1);
                break;
            case RemotePlayBackMsg.MSG_REMOTEPLAYBACK_PLAY_SUCCUSS:
                handlePlaySuccess(msg);
                break;
            case RemotePlayBackMsg.MSG_REMOTEPLAYBACK_PLAY_FAIL:
            case RemotePlayBackMsg.MSG_REMOTEPLAYBACK_SEARCH_FILE_FAIL:
                handlePlayFail(msg.arg1);
                break;                         
            case RemotePlayBackMsg.MSG_REMOTEPLAYBACK_PASSWORD_ERROR:
                // 处理播放密码错误
                handlePasswordError(R.string.realplay_password_error_title, 
                        R.string.realplay_password_error_message3,
                        R.string.realplay_password_error_message1);
                break;
            case RemotePlayBackMsg.MSG_REMOTEPLAYBACK_ENCRYPT_PASSWORD_ERROR:
                //处理加密密码错误
                handlePasswordError(R.string.realplay_encrypt_password_error_title,
                        R.string.realplay_encrypt_password_error_message, 0);
                break;                            
            case RemotePlayBackMsg.MSG_START_RECORD_SUCCESS:
                handleStartRecordSuccess((String) msg.obj);
                break;
            case RemotePlayBackMsg.MSG_START_RECORD_FAIL:
                Utils.showToast(this, R.string.remoteplayback_record_fail);
                break;
            case RemotePlayBackMsg.MSG_CAPTURE_PICTURE_SUCCESS:
                handleCapturePictureSuccess((String)msg.obj);
                break;
            case RemotePlayBackMsg.MSG_CAPTURE_PICTURE_FAIL:
                // 提示抓图失败
                Utils.showToast(this, R.string.remoteplayback_capture_fail);
                break;            
            case MSG_SEARCH_CLOUD_FILE_SUCCUSS:
                updateLoadingProgress(20);
                handleSearchCloudFileSuccess((Calendar)msg.obj);
                break;               
            case MSG_SEARCH_CLOUD_FILE_FAIL:
                handleSearchCloudFileFail(msg.arg1);
                break;    
            case RemotePlayBackMsg.MSG_REMOTEPLAYBACK_SEARCH_FILE_SUCCUSS:
                updateLoadingProgress(40);
                handleSearchDeviceFileSuccess();
                break;      
            case RemotePlayBackMsg.MSG_REMOTEPLAYBACK_SEARCH_NO_FILE:
                handleSearchNoFile();
                break;                
            case MSG_PLAY_UI_UPDATE:
                updateRemotePlayBackUI();
                break;
            default:
                break;                
        }
        return false;
    }
    
    private void handleSearchDeviceFileSuccess() {
        mDeviceFileList = mRemotePlayBackMgr.getRemoteFileInfoList();
        if((mCloudFileList != null && mCloudFileList.size() > 0) || (mDeviceFileList != null && mDeviceFileList.size() > 0)) {
            if(mAlarmStartTime != null) {
                if(mRemotePlayBackBeginTimeTv.getTag() == null) {
                    Calendar fileStartTime = getFileStartTime();
                    Calendar beginTime = (fileStartTime != null && fileStartTime.getTimeInMillis() > mAlarmStartTime.getTimeInMillis()) ? fileStartTime:mAlarmStartTime;
                    Calendar lastStopTime = getFileStopTime();
                    Calendar endTime = (lastStopTime != null && lastStopTime.getTimeInMillis()  < mAlarmStopTime.getTimeInMillis()) ?lastStopTime:mAlarmStopTime;
                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
                    mRemotePlayBackBeginTimeTv.setText(sdf.format(beginTime.getTimeInMillis()));
                    mRemotePlayBackEndTimeTv.setText(sdf.format(endTime.getTimeInMillis()));
                    int duration = (int)(endTime.getTimeInMillis() - beginTime.getTimeInMillis())/1000;
                    mRemotePlayBackProgressBar.setMax(duration);
                    mRemotePlayBackSeekBar.setMax(duration);
                    mRemotePlayBackBeginTimeTv.setTag(mAlarmStartTime);
                }
            } else {
                mRemoteFileTimeBar.drawFileLayout(mDeviceFileList, mCloudFileList, mStartTime, mEndTime);
            }  
        } else {
            handleSearchNoFile();
        }
    }
    
    private Calendar getFileStartTime() {
        Calendar cloudStartTime = null;
        if(mCloudFileList != null && mCloudFileList.size() > 0) {
            cloudStartTime = Utils.convert19Calender(mCloudFileList.get(0).getStartTime());
        }
        Calendar deviceStartTime = null;
        if(mDeviceFileList != null && mDeviceFileList.size() > 0) {
            deviceStartTime = mDeviceFileList.get(0).getStartTime();
        }
        
        if(cloudStartTime != null && deviceStartTime != null) {
            return (cloudStartTime.getTimeInMillis() > deviceStartTime.getTimeInMillis()) ? deviceStartTime:cloudStartTime;
        } else if(cloudStartTime != null) {
            return cloudStartTime;
        } else {
            return deviceStartTime;
        }
    }

    private Calendar getFileStopTime() {
        Calendar cloudStopTime = null;
        if(mCloudFileList != null && mCloudFileList.size() > 0) {
            cloudStopTime = Utils.convert19Calender(mCloudFileList.get(mCloudFileList.size()-1).getEndTime());
        }
        Calendar deviceStopTime = null;
        if(mDeviceFileList != null && mDeviceFileList.size() > 0) {
            deviceStopTime = mDeviceFileList.get(mDeviceFileList.size()-1).getStartTime();
        }
        
        if(cloudStopTime != null && deviceStopTime != null) {
            return (cloudStopTime.getTimeInMillis() > deviceStopTime.getTimeInMillis()) ? cloudStopTime:deviceStopTime;
        } else if(cloudStopTime != null) {
            return cloudStopTime;
        } else {
            return deviceStopTime;
        }
    }
    
    private void handleSearchNoFile() {
        stopRemotePlayBack();
        
        if (mAlarmStartTime != null) {
            setRemotePlayBackFailUI(getString(R.string.remoteplayback_norecordfile_alarm));
        } else {
            setRemotePlayBackFailUI(getString(R.string.remoteplayback_norecordfile));
        }
    }
    
    private void handleSearchCloudFileSuccess(Calendar seletedTime) {        
        mRemotePlayBackMgr = new RemotePlayBackManager(this);
        mRemotePlayBackMgr.setHandler(mHandler);
        mRemotePlayBackMgr.setPlaySurface(mRemotePlayBackSh);
        
        if(mAlarmStartTime != null) {
            mRemotePlayBackHelper.startRemotePlayBackTask(mRemotePlayBackMgr, mCameraInfo.getCameraId(), 
                    seletedTime!=null?seletedTime:mAlarmStartTime, mAlarmStopTime);
        } else {
            mRemotePlayBackHelper.startRemotePlayBackTask(mRemotePlayBackMgr, mCameraInfo.getCameraId(), 
                    seletedTime!=null?seletedTime:mStartTime, mEndTime);
        }
    }
    
    // 搜索文件异常处理
    private void handleSearchCloudFileFail(int errorCode) {        
        LogUtil.debugLog(TAG, "handleSearchFileFail:" + errorCode);

        stopRemotePlayBack();

        String txt = null;
        // 判断返回的错误码
        switch (errorCode) {
            case ErrorCode.ERROR_WEB_SESSION_ERROR:
            case ErrorCode.ERROR_WEB_SESSION_EXPIRE:
            case ErrorCode.ERROR_WEB_HARDWARE_SIGNATURE_ERROR:
            case ErrorCode.ERROR_CAS_VERIFY_SESSION_ERROR:          
                EzvizAPI.getInstance().gotoLoginPage();
                return;                
            default:
                txt = Utils.getErrorTip(this, R.string.remoteplayback_searchfile_fail_for_device, errorCode);
                break;
        }

        setRemotePlayBackFailUI(txt);
    }
    
    private void handleConnectionException(int errorCode) {
        LogUtil.debugLog(TAG, "handleConnectionException:" + errorCode);
        Calendar startTime = Calendar.getInstance();
        
        Calendar seekTime = getTimeBarSeekTime();
        if(seekTime == null) {
            handlePlayFail(errorCode);
            return;            
        }
        
        if (mPlayTime == 0) {
            Calendar osdTime = mRemotePlayBackMgr.getOSDTime();
            if(osdTime != null) {
                mPlayTime = osdTime.getTimeInMillis() + 5000;
            } else {
                mPlayTime = seekTime.getTimeInMillis() + 5000;
            }
        } else {
            mPlayTime = mPlayTime + 5000;
        }

        startTime.setTimeInMillis(mPlayTime);
        LogUtil.debugLog(TAG, "handleConnectionException replay:" + startTime.toString());
        stopRemotePlayBack();
        startRemotePlayBack(startTime);   
    }
    
    /* (non-Javadoc)
     * @see android.view.SurfaceHolder.Callback#surfaceChanged(android.view.SurfaceHolder, int, int, int)
     */
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // TODO Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see android.view.SurfaceHolder.Callback#surfaceCreated(android.view.SurfaceHolder)
     */
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (mRemotePlayBackMgr != null) {
            mRemotePlayBackMgr.setPlaySurface(holder);
        }
        mRemotePlayBackSh = holder;
    }

    /* (non-Javadoc)
     * @see android.view.SurfaceHolder.Callback#surfaceDestroyed(android.view.SurfaceHolder)
     */
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (mRemotePlayBackMgr != null) {
            mRemotePlayBackMgr.setPlaySurface(null);
        }
        mRemotePlayBackSh = null;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if(mStatus != STATUS_STOP) {
                stopRemotePlayBack();
            }
            finish();
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }
    
    /* (non-Javadoc)
     * @see android.view.View.OnClickListener#onClick(android.view.View)
     */
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.remoteplayback_loading_play_btn:
                startRemotePlayBack(getTimeBarSeekTime());
                break;
            case R.id.remoteplayback_play_btn:
            case R.id.remoteplayback_full_play_btn:
                if(mStatus == STATUS_START || mStatus == STATUS_PLAY) {
                    if(mStatus == STATUS_PLAY) {
                        pauseRemotePlayBack();
                    } else {
                        stopRemotePlayBack();
                        mRemotePlayBackLoadingPlayBtn.setVisibility(View.VISIBLE);
                    }
                    setRemotePlayBackStopUI();
                } else {
                    startRemotePlayBack(getTimeBarSeekTime());
                }
                break;
            case R.id.remoteplayback_replay_btn:
                if(mStatus != STATUS_STOP) {
                    stopRemotePlayBack();
                }			
                startRemotePlayBack(null);
                break;
            case R.id.remoteplayback_sound_btn:
            case R.id.remoteplayback_full_sound_btn:
                onSoundBtnClick();
                break;
            case R.id.remoteplayback_previously_btn:
            case R.id.remoteplayback_full_previously_btn:
            case R.id.remoteplayback_small_previously_btn:
                onCapturePicBtnClick();
                break;
            case R.id.remoteplayback_capture_rl:
                onCaptureRlClick();
                break;
            case R.id.remoteplayback_video_btn:
            case R.id.remoteplayback_full_video_btn:
            case R.id.remoteplayback_small_video_btn:
            case R.id.remoteplayback_video_start_btn:
            case R.id.remoteplayback_full_video_start_btn:
            case R.id.remoteplayback_small_video_start_btn:                
                onRecordBtnClick();
                break;
            case R.id.remoteplayback_full_down_btn:
                onTimeBarDownBtnClick();
                break;
            default:
                break;
        }
    }

    private void onTimeBarDownBtnClick() {
        if(mRemotePlayBackTimeBarRl.getVisibility() == View.VISIBLE) {
            mRemotePlayBackTimeBarRl.setVisibility(View.GONE);
            mRemotePlayBackFullDownBtn.setBackgroundResource(R.drawable.palyback_full_up);
            mRemotePlayBackFullOperateBar.setPadding(0, 0, 0, Utils.dip2px(this, 5));
        } else {
            mRemotePlayBackTimeBarRl.setVisibility(View.VISIBLE);
            mRemotePlayBackFullDownBtn.setBackgroundResource(R.drawable.palyback_full_down);
            mRemotePlayBackFullOperateBar.setPadding(0, 0, 0, Utils.dip2px(this, 92));
        }
        updateCaptureUI();
    }
    
    /**
     * 开始录像
     * 
     * @see
     * @since V1.0
     */
    private void onRecordBtnClick() {
        mControlDisplaySec = 0;   
        if(mRecordFilePath != null) {
            stopRemotePlayBackRecord();
            return;
        }

        if (!SDCardUtil.isSDCardUseable()) {
            // 提示SD卡不可用
            Utils.showToast(this, R.string.remoteplayback_SDCard_disable_use);
            return;
        }

        if (SDCardUtil.getSDCardRemainSize() < SDCardUtil.PIC_MIN_MEM_SPACE) {
            // 提示内存不足
            Utils.showToast(this, R.string.remoteplayback_record_fail_for_memory);
            return;
        }
        
        if(mRemotePlayBackMgr != null) {
            mCaptureDisplaySec = 4;
            updateCaptureUI();
            mAudioPlayUtil.playAudioFile(AudioPlayUtil.RECORD_SOUND);
            mRemotePlayBackHelper.startRecordTask(mRemotePlayBackMgr, RemotePlayBackActivity.this.getResources(), R.drawable.video_file_watermark);
        }
    }
    
    /**
     * 停止录像
     * 
     * @see
     * @since V1.0
     */
    private void stopRemotePlayBackRecord() {
        if(mRecordFilePath == null) {
            return;
        }
        
        // 设置录像按钮为check状态
        if (mOrientation == Configuration.ORIENTATION_PORTRAIT) {
            if(!mIsOnStop) {
                mRecordRotateViewUtil.applyRotation(mRemotePlayBackRecordContainer, mRemotePlayBackRecordStartBtn, 
                        mRemotePlayBackRecordBtn, 0, 90);
            } else {
                mRemotePlayBackRecordBtn.setVisibility(View.VISIBLE);
                mRemotePlayBackRecordStartBtn.setVisibility(View.GONE);     
            }
            if(mAlarmStartTime != null) {
                mRemotePlayBackSmallRecordBtn.setVisibility(View.VISIBLE);
                mRemotePlayBackSmallRecordStartBtn.setVisibility(View.GONE);               
            } else {
                mRemotePlayBackFullRecordBtn.setVisibility(View.VISIBLE);
                mRemotePlayBackFullRecordStartBtn.setVisibility(View.GONE);
            }
        } else {
            if(mAlarmStartTime != null) {
                if(!mIsOnStop) {
                    mRecordRotateViewUtil.applyRotation(mRemotePlayBackSmallRecordContainer,
                            mRemotePlayBackSmallRecordStartBtn, mRemotePlayBackSmallRecordBtn, 0, 90);
                } else {
                    mRemotePlayBackSmallRecordBtn.setVisibility(View.VISIBLE);
                    mRemotePlayBackSmallRecordStartBtn.setVisibility(View.GONE);                        
                }
                mRemotePlayBackSmallRecordBtn.setVisibility(View.VISIBLE);
                mRemotePlayBackSmallRecordStartBtn.setVisibility(View.GONE);   
            } else {
                if(!mIsOnStop) {
                    mRecordRotateViewUtil.applyRotation(mRemotePlayBackFullRecordContainer,
                            mRemotePlayBackFullRecordStartBtn, mRemotePlayBackFullRecordBtn, 0, 90);
                } else {
                    mRemotePlayBackFullRecordBtn.setVisibility(View.VISIBLE);
                    mRemotePlayBackFullRecordStartBtn.setVisibility(View.GONE);                   
                }
                mRemotePlayBackRecordBtn.setVisibility(View.VISIBLE);
                mRemotePlayBackRecordStartBtn.setVisibility(View.GONE);                
            }
        }
        
        mAudioPlayUtil.playAudioFile(AudioPlayUtil.RECORD_SOUND);
        mRemotePlayBackHelper.stopRecordTask(mRemotePlayBackMgr);

        // 计时按钮不可见
        mRemotePlayBackRecordLy.setVisibility(View.GONE);        
        mRemotePlayBackCaptureRl.setVisibility(View.VISIBLE);
        mCaptureDisplaySec = 0;
        try {
            mRemotePlayBackCaptureIv.setImageURI(Uri.parse(mRecordFilePath));
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
        }    
        mRemotePlayBackCaptureWatermarkIv.setTag(mRecordFilePath);
        mRecordFilePath = null;
        updateCaptureUI();
    }

    /**
     * 进入图像管理页面
     * 
     * @see
     * @since V2.0
     */
    private void onCaptureRlClick() {
        mRemotePlayBackCaptureRl.setVisibility(View.GONE);
        mRemotePlayBackCaptureIv.setImageURI(null);
        mRemotePlayBackCaptureWatermarkIv.setTag(null);
        mRemotePlayBackCaptureWatermarkIv.setVisibility(View.GONE);
    }

    /**
     * <p>
     * 声音控制
     * </p>
     * 
     * @author hanlieng 2014-8-5 下午6:57:41
     */
    private void onSoundBtnClick() {
        if (mLocalInfo.isSoundOpen()) {
            mLocalInfo.setSoundOpen(false);
            mRemotePlayBackSoundBtn.setBackgroundResource(R.drawable.remote_list_soundoff_btn_selector);
            mRemotePlayBackFullSoundBtn.setBackgroundResource(R.drawable.play_full_soundoff_btn_selector);
        } else {
            mLocalInfo.setSoundOpen(true);
            mRemotePlayBackSoundBtn.setBackgroundResource(R.drawable.remote_list_soundon_btn_selector);
            mRemotePlayBackFullSoundBtn.setBackgroundResource(R.drawable.play_full_soundon_btn_selector);
        }
        
        setRemotePlaySound();
    }
    
    private void setRemotePlaySound() {
        if (mRemotePlayBackMgr != null) {
            if (mLocalInfo.isSoundOpen()) {
                mRemotePlayBackMgr.openSound();
            } else {
                mRemotePlayBackMgr.closeSound();
            }
        }
    }
    
    /**
     * 抓拍按钮响应函数
     * 
     * @since V1.0
     */
    private void onCapturePicBtnClick() {
        mControlDisplaySec = 0;    
        if (!SDCardUtil.isSDCardUseable()) {
            // 提示SD卡不可用
            Utils.showToast(this, R.string.remoteplayback_SDCard_disable_use);
            return;
        }

        if (SDCardUtil.getSDCardRemainSize() < SDCardUtil.PIC_MIN_MEM_SPACE) {
            // 提示内存不足
            Utils.showToast(this, R.string.remoteplayback_capture_fail_for_memory);
            return;
        }
        
        if (mRemotePlayBackMgr != null) {
            mCaptureDisplaySec = 4;
            updateCaptureUI();
            mRemotePlayBackHelper.capturePictureTask(mRemotePlayBackMgr);
        }
    }
    
    private void onRemotePlayBackSvClick() {
        if(mRemotePlayBackRatioTv.getVisibility() == View.VISIBLE) {
            return;
        }
        if(mOrientation == Configuration.ORIENTATION_PORTRAIT || mAlarmStartTime != null) {
            if(mRemotePlayBackControlRl.getVisibility() == View.VISIBLE) {
                mRemotePlayBackControlRl.setVisibility(View.GONE);
                if(mAlarmStartTime != null) {
                    mRemotePlayBackProgressLy.setVisibility(View.GONE);
                    mRemotePlayBackProgressBar.setVisibility(View.VISIBLE);
                }
            } else {
                mRemotePlayBackControlRl.setVisibility(View.VISIBLE);
                if(mAlarmStartTime != null) {
                    if (mStatus != STATUS_PLAY) {
                        mRemotePlayBackProgressLy.setVisibility(View.GONE);
                    } else {
                        mRemotePlayBackProgressLy.setVisibility(View.VISIBLE);
                    }
                    mRemotePlayBackProgressBar.setVisibility(View.GONE);
                }
                mControlDisplaySec = 0;
            }
            updateTimeBarUI();
        } else {
            if(mRemotePlayBackFullOperateBar.getVisibility() == View.VISIBLE) {
                mRemotePlayBackFullOperateBar.setVisibility(View.GONE);
            } else {
                mRemotePlayBackFullOperateBar.setVisibility(View.VISIBLE);
                mControlDisplaySec = 0;
            }            
        }
    }
    
    private void onRemotePlayBackSvDoubleClick(MotionEvent e) {
        if(mStatus == STATUS_PLAY && mOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            if(mRemotePlayBackRatioTv.getVisibility() == View.VISIBLE) {
                setRemotePlayBackSvLayout(1);
            } else {
                final int touchX = (int) (e.getX() - (mLocalInfo.getScreenHeight() - mHVScrollView.getWidth()) / 2) * 2;
                final int touchY = (int) (e.getY() - mLocalInfo.getNavigationBarHeight() - ((mLocalInfo
                        .getScreenWidth() - mLocalInfo.getNavigationBarHeight()) / 2 - mHVScrollView.getHeight() / 2)) * 2;
                setRemotePlayBackSvLayout(2.0f);

                mHVScrollView.post(new Runnable() {
                    @Override
                    public void run() {
                        int centerX = mLocalInfo.getScreenHeight() / 2;
                        int centerY = (mLocalInfo.getScreenWidth() - mLocalInfo.getNavigationBarHeight()) / 2;
                        int scrollX = touchX > centerX ? (touchX - centerX) : 0;
                        int scrollY = touchY > centerY ? (touchY - centerY) : 0;
                        mHVScrollView.scrollTo(scrollX, scrollY);
                    }
                });
                mRemotePlayBackFullOperateBar.setVisibility(View.GONE);
            }            
        }
    }
    
    private void searchCloudFileList(final Calendar seletedTime) {
        new Thread() {
            @Override
            public void run() {
                try {
                    GetCloudFileList getCloudFileList = new GetCloudFileList();
                    getCloudFileList.setCameraId(mCameraInfo.getCameraId());
                    if(mAlarmStartTime != null) {
                        getCloudFileList.setStartTime(Utils.calendar2String(mAlarmStartTime));
                        getCloudFileList.setEndTime(Utils.calendar2String(mAlarmStopTime));
                    } else {
                        getCloudFileList.setStartTime(Utils.calendar2String(mStartTime));
                        getCloudFileList.setEndTime(Utils.calendar2String(mEndTime));
                    }
                    getCloudFileList.setPageSize(10000);
                    getCloudFileList.setPageStart(0);
                    mCloudFileList = EzvizAPI.getInstance().getCloudFileList(getCloudFileList);
                    sendMessage(MSG_SEARCH_CLOUD_FILE_SUCCUSS, 0, seletedTime);   
                } catch (BaseException e) {
                    e.printStackTrace();
                    sendMessage(MSG_SEARCH_CLOUD_FILE_FAIL, e.getErrorCode()); 
                }
            }
        }.start();
    }
    
    /**
     * 开始回放
     * 
     * @see
     * @since V2.0
     */
    private void startRemotePlayBack(Calendar seletedTime) {
        LogUtil.debugLog(TAG, "startRemotePlayBack:" + seletedTime);
        
        if(mStatus == STATUS_START || mStatus == STATUS_PLAY) {
            return;
        }
        
        // 检查网络是否可用
        if (!ConnectionDetector.isNetworkAvailable(this)) {
            // 提示没有连接网络
            setRemotePlayBackFailUI(getString(R.string.remoteplayback_searchfile_fail_for_network));
            return;
        }
        
        if(mRemotePlayBackMgr != null && mStatus == STATUS_PAUSE) {
            resumeRemotePlayBack();
            setRemotePlayBackSuccessUI();
            return;
        }
        
        mStatus = STATUS_START;
        setRemotePlayBackLoadingUI();
        updateLoadingProgress(0);
        
        if(mCloudFileList == null) {
            searchCloudFileList(seletedTime);
            return;
        }
        
        mRemotePlayBackMgr = new RemotePlayBackManager(this);
        mRemotePlayBackMgr.setHandler(mHandler);
        mRemotePlayBackMgr.setPlaySurface(mRemotePlayBackSh);
        
        if(mAlarmStartTime != null) {
            mRemotePlayBackHelper.startRemotePlayBackTask(mRemotePlayBackMgr, mCameraInfo.getCameraId(), 
                    seletedTime!=null?seletedTime:mAlarmStartTime, mAlarmStopTime);
        } else {
            mRemotePlayBackHelper.startRemotePlayBackTask(mRemotePlayBackMgr, mCameraInfo.getCameraId(), 
                    seletedTime!=null?seletedTime:mStartTime, mEndTime);
        }
    }
    
    /**
     * 停止回放
     * 
     * @see
     * @since V1.0
     */
    private void stopRemotePlayBack() {
        LogUtil.debugLog(TAG, "stopRemotePlayBack");
        mStatus = STATUS_STOP;
        
        stopUpdateTimer();
        if (mRemotePlayBackMgr != null) {
            stopRemotePlayBackRecord();

            mRemotePlayBackHelper.stopRemotePlayBackTask(mRemotePlayBackMgr);
            mTotalStreamFlow += mRemotePlayBackMgr.getStreamFlow();
        }
        mStreamFlow = 0;
    }
    
    /**
     * 暂停回放
     * 
     * @see
     * @since V1.0
     */
    private void pauseRemotePlayBack() {
        LogUtil.debugLog(TAG, "pauseRemotePlayBack");
        mStatus = STATUS_PAUSE;
        
        if (mRemotePlayBackMgr != null) {
            stopRemotePlayBackRecord();

            mRemotePlayBackHelper.pauseRemotePlayBackTask(mRemotePlayBackMgr);
        }
    }

    /**
     * 暂停回放
     * 
     * @see
     * @since V1.0
     */
    private void resumeRemotePlayBack() {
        LogUtil.debugLog(TAG, "resumeRemotePlayBack");
        mStatus = STATUS_PLAY;
        
        if (mRemotePlayBackMgr != null) {
            mRemotePlayBackMgr.openSound();
            mRemotePlayBackHelper.resumeRemotePlayBackTask(mRemotePlayBackMgr);
        }
    }

    
    private void setRemotePlayBackLoadingUI() {
        mRemotePlayBackSv.setVisibility(View.INVISIBLE);
        mRemotePlayBackSv.setVisibility(View.VISIBLE);
        mRemotePlayBackTipTv.setVisibility(View.GONE);
        mRemotePlayBackReplayBtn.setVisibility(View.GONE);
        mRemotePlayBackLoadingPlayBtn.setVisibility(View.GONE);

        mRemotePlayBackLoadingLy.setVisibility(View.VISIBLE);
        mRemotePlayBackLoadingPbLy.setVisibility(View.VISIBLE);

        if (mAlarmStartTime != null) {
            mRemotePlayBackProgressLy.setVisibility(View.GONE);
            mRemotePlayBackProgressBar.setVisibility(View.VISIBLE);
        }
        mRemotePlayBackBtn.setBackgroundResource(R.drawable.remote_list_pause_btn_selector);

        if (mOrientation == Configuration.ORIENTATION_PORTRAIT) {
            mRemotePlayBackControlRl.setVisibility(View.VISIBLE);
            mRemotePlayBackFullOperateBar.setVisibility(View.GONE);
        } else {
            mRemotePlayBackFullOperateBar.setVisibility(View.GONE);
            mRemotePlayBackControlRl.setVisibility(View.GONE);
        }
        mRemotePlayBackCaptureBtn.setEnabled(false);
        mRemotePlayBackRecordBtn.setEnabled(false);       
        mRemotePlayBackSmallCaptureBtn.setEnabled(false);
        mRemotePlayBackSmallRecordBtn.setEnabled(false);   
        
        mRemotePlayBackFullPlayBtn.setEnabled(false);
        mRemotePlayBackFullCaptureBtn.setEnabled(false); 
        mRemotePlayBackFullRecordBtn.setEnabled(false); 
        mRemotePlayBackFullFlowLy.setVisibility(View.GONE);

        updateSoundUI();

        updateTimeBarUI();
    }

    private void setRemotePlayBackStopUI() {
        stopUpdateTimer();
        setRemotePlayBackSvLayout(1);
        mRemotePlayBackTipTv.setVisibility(View.GONE);
        mRemotePlayBackReplayBtn.setVisibility(View.GONE);
        mRemotePlayBackLoadingLy.setVisibility(View.GONE);
        if (mTotalStreamFlow > 0) {
            mRemotePlayBackFlowTv.setVisibility(View.VISIBLE);
            mRemotePlayBackFullFlowLy.setVisibility(View.VISIBLE);    
            updateRemotePlayBackFlowTv(mStreamFlow);
        } else {
            mRemotePlayBackFlowTv.setVisibility(View.GONE);
            mRemotePlayBackFullFlowLy.setVisibility(View.GONE);
        }
        if(mOrientation == Configuration.ORIENTATION_PORTRAIT || mAlarmStartTime != null) {
            mRemotePlayBackControlRl.setVisibility(View.VISIBLE);
            if(mAlarmStartTime != null) {
                if (mStatus == STATUS_PAUSE) {
                    mRemotePlayBackProgressLy.setVisibility(View.VISIBLE);
                } else {
                    mRemotePlayBackProgressLy.setVisibility(View.GONE);
                }
                mRemotePlayBackProgressBar.setVisibility(View.GONE);
            }
        } else {
            mRemotePlayBackFullOperateBar.setVisibility(View.VISIBLE);
        }
        mRemotePlayBackBtn.setBackgroundResource(R.drawable.remote_list_play_btn_selector);
        
        mRemotePlayBackCaptureBtn.setEnabled(false);
        mRemotePlayBackRecordBtn.setEnabled(false);
        mRemotePlayBackSmallCaptureBtn.setEnabled(false);
        mRemotePlayBackSmallRecordBtn.setEnabled(false);   
        
        mRemotePlayBackFullPlayBtn.setEnabled(true);
        mRemotePlayBackFullPlayBtn.setBackgroundResource(R.drawable.play_full_play_selector);
        mRemotePlayBackFullCaptureBtn.setEnabled(false); 
        mRemotePlayBackFullRecordBtn.setEnabled(false); 
        updateSoundUI();

        updateTimeBarUI();
    }
    
    private void setRemotePlayBackFailUI(String errorStr) {        
        stopUpdateTimer();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        
        if(TextUtils.isEmpty(errorStr)) {
            mRemotePlayBackTipTv.setVisibility(View.GONE);
            mRemotePlayBackReplayBtn.setVisibility(View.VISIBLE);
        } else {
            mRemotePlayBackTipTv.setVisibility(View.VISIBLE);
            mRemotePlayBackTipTv.setText(errorStr);
            mRemotePlayBackReplayBtn.setVisibility(View.GONE);
        }

        mRemotePlayBackLoadingLy.setVisibility(View.GONE);
        mRemotePlayBackFlowTv.setVisibility(View.GONE);
        mRemotePlayBackFullFlowLy.setVisibility(View.GONE);
        if(mOrientation == Configuration.ORIENTATION_PORTRAIT || mAlarmStartTime != null) {
            mRemotePlayBackControlRl.setVisibility(View.VISIBLE);
            if(mAlarmStartTime != null) {
                mRemotePlayBackProgressLy.setVisibility(View.VISIBLE);
                mRemotePlayBackProgressBar.setVisibility(View.GONE);
            }
        } else {
            mRemotePlayBackFullOperateBar.setVisibility(View.VISIBLE);
        }    
        mRemotePlayBackBtn.setBackgroundResource(R.drawable.remote_list_play_btn_selector);
              
        mRemotePlayBackCaptureBtn.setEnabled(false);
        mRemotePlayBackRecordBtn.setEnabled(false);
        mRemotePlayBackSmallCaptureBtn.setEnabled(false);
        mRemotePlayBackSmallRecordBtn.setEnabled(false);   
        
        mRemotePlayBackFullPlayBtn.setEnabled(true);
        mRemotePlayBackFullPlayBtn.setBackgroundResource(R.drawable.play_full_play_selector);
        mRemotePlayBackFullCaptureBtn.setEnabled(false); 
        mRemotePlayBackFullRecordBtn.setEnabled(false);

        updateSoundUI();

        updateTimeBarUI();
    }
 
    /**
     * 这里对方法做描述
     * @see 
     * @since V1.8.2
     */
    /**
     * 这里对方法做描述
     * @see 
     * @since V1.8.2
     */
    private void setRemotePlayBackSuccessUI() {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR); 
        mRemotePlayBackTipTv.setVisibility(View.GONE);
        mRemotePlayBackReplayBtn.setVisibility(View.GONE);
        mRemotePlayBackLoadingPlayBtn.setVisibility(View.GONE);

        mRemotePlayBackLoadingLy.setVisibility(View.GONE);
        mRemotePlayBackFlowTv.setVisibility(View.VISIBLE);
        mRemotePlayBackFullFlowLy.setVisibility(View.VISIBLE);
        mRemotePlayBackBtn.setBackgroundResource(R.drawable.remote_list_pause_btn_selector);

        mRemotePlayBackCaptureBtn.setEnabled(true);
        mRemotePlayBackRecordBtn.setEnabled(true);
        mRemotePlayBackSmallCaptureBtn.setEnabled(true);
        mRemotePlayBackSmallRecordBtn.setEnabled(true);   
        
        mRemotePlayBackFullPlayBtn.setEnabled(true);
        mRemotePlayBackFullPlayBtn.setBackgroundResource(R.drawable.play_full_pause_selector);
        mRemotePlayBackFullCaptureBtn.setEnabled(true); 
        mRemotePlayBackFullRecordBtn.setEnabled(true);
        updateTimeBarUI();

        updateSoundUI();

        startUpdateTimer();
    }
    
    /**
     * 更新流量统计
     * 
     * @param str
     * @see
     * @since V1.0
     */
    private void checkRemotePlayBackFlow() {
        if (mRemotePlayBackMgr != null && mRemotePlayBackFlowTv.getVisibility() == View.VISIBLE) {
            // 更新流量数据
            long streamFlow = mRemotePlayBackMgr.getStreamFlow();

            updateRemotePlayBackFlowTv(streamFlow);
        }
    }
    
    private void updateRemotePlayBackFlowTv(long streamFlow) {
        long streamFlowUnit = streamFlow - mStreamFlow;
        if (streamFlowUnit < 0)
            streamFlowUnit = 0;
        float fKBUnit = (float) streamFlowUnit / (float) Constant.KB;
        String descUnit = String.format("%.2f k/s ", fKBUnit);
        String desc = null;
        float fMB = 0;
        if (streamFlow >= Constant.GB) {
            float fGB = (float) streamFlow / (float) Constant.GB;
            fMB = 1024 * fGB;
            desc = String.format("%.2f GB ", fGB);
        } else {
            fMB = (float) streamFlow / (float) Constant.MB;
            desc = String.format("%.2f MB ", fMB);
        }

        // 显示流量
        mRemotePlayBackFlowTv.setText(descUnit + " " + desc);
        mRemotePlayBackFullRateTv.setText(descUnit);
        mRemotePlayBackFullFlowTv.setText(desc);
        mStreamFlow = streamFlow;
    }
    
    
    /**
     * 开始录像成功
     * 
     * @param errorCode
     * @see
     * @since V2.0
     */
    private void handleStartRecordSuccess(String recordFilePath) {
        // 设置录像按钮为check状态
        if (mOrientation == Configuration.ORIENTATION_PORTRAIT) {
            if(!mIsOnStop) {
                mRecordRotateViewUtil.applyRotation(mRemotePlayBackRecordContainer, mRemotePlayBackRecordBtn, 
                        mRemotePlayBackRecordStartBtn, 0, 90);
            } else {
                mRemotePlayBackRecordBtn.setVisibility(View.GONE);
                mRemotePlayBackRecordStartBtn.setVisibility(View.VISIBLE);
            }
            if(mAlarmStartTime != null) {
                mRemotePlayBackSmallRecordBtn.setVisibility(View.GONE);
                mRemotePlayBackSmallRecordStartBtn.setVisibility(View.VISIBLE);                
            } else {
                mRemotePlayBackFullRecordBtn.setVisibility(View.GONE);
                mRemotePlayBackFullRecordStartBtn.setVisibility(View.VISIBLE);
            }
        } else {
            if(mAlarmStartTime != null) {
                if(!mIsOnStop) {
                    mRecordRotateViewUtil.applyRotation(mRemotePlayBackSmallRecordContainer,
                            mRemotePlayBackSmallRecordBtn, mRemotePlayBackSmallRecordStartBtn, 0, 90);
                } else {
                    mRemotePlayBackSmallRecordBtn.setVisibility(View.GONE);
                    mRemotePlayBackSmallRecordStartBtn.setVisibility(View.VISIBLE); 
                }
            } else {
                if(!mIsOnStop) {
                    mRecordRotateViewUtil.applyRotation(mRemotePlayBackFullRecordContainer,
                            mRemotePlayBackFullRecordBtn, mRemotePlayBackFullRecordStartBtn, 0, 90);
                } else {
                    mRemotePlayBackFullRecordBtn.setVisibility(View.GONE);
                    mRemotePlayBackFullRecordStartBtn.setVisibility(View.VISIBLE);
                }
            }
            mRemotePlayBackRecordBtn.setVisibility(View.GONE);
            mRemotePlayBackRecordStartBtn.setVisibility(View.VISIBLE);
        }

        mRecordFilePath = recordFilePath;
        // 计时按钮可见
        mRemotePlayBackRecordLy.setVisibility(View.VISIBLE);
        mRemotePlayBackRecordTv.setText("00:00");
        mRecordSecond = 0;
    }
    
    /**
     * 抓图成功，返回图片文件路径
     * 
     * @param errorCode
     * @see
     * @since V2.0
     */
    private void handleCapturePictureSuccess(String filePath) {
        if(TextUtils.isEmpty(filePath)) {
            return;
        }
        // 播放抓拍音频
        mAudioPlayUtil.playAudioFile(AudioPlayUtil.CAPTURE_SOUND);
        
        mRemotePlayBackCaptureRl.setVisibility(View.VISIBLE);   
        mCaptureDisplaySec = 0;
        try {
            mRemotePlayBackCaptureIv.setImageURI(Uri.parse(filePath));
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
        }
        updateCaptureUI();
    }
    
    private void updateRemotePlayBackUI() {
        if (isFinishing()) {
            return;
        }
        if (mControlDisplaySec == 5) {
            mControlDisplaySec = 0;
            mRemotePlayBackControlRl.setVisibility(View.GONE);
            if(mAlarmStartTime != null) {
                mRemotePlayBackProgressLy.setVisibility(View.GONE);
                mRemotePlayBackProgressBar.setVisibility(View.VISIBLE);
            }
            mRemotePlayBackFullOperateBar.setVisibility(View.GONE);
            updateTimeBarUI();
        }
        
        updateCaptureUI();
        
        if(mRecordFilePath != null) {
            updateRecordTime();
        }
        
        checkRemotePlayBackFlow();
        
        Calendar OSDTime = mRemotePlayBackMgr.getOSDTime();
        if(OSDTime != null) {
            mPlayTime = OSDTime.getTimeInMillis();
            if(mAlarmStartTime != null) {
                mRemotePlayBackProgressBar.setProgress((int)(mPlayTime - mAlarmStartTime.getTimeInMillis())/1000);
                mRemotePlayBackSeekBar.setProgress((int)(mPlayTime - mAlarmStartTime.getTimeInMillis())/1000);
                if(mPlayTime + 1000 > mAlarmStopTime.getTimeInMillis()) {
                    handlePlayFinish();
                }
            } else {
                float pos = mRemoteFileTimeBar.getScrollPosByPlayTime(mPlayTime, mOrientation);
                mRemotePlayBackTimeBar.smoothScrollTo((int)pos, 0);
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
                mRemotePlayBackTimeTv.setText(sdf.format(mPlayTime));
            }
        }
    }
    
    //更新抓图/录像显示UI
    private void updateCaptureUI() {
        if (isFinishing()) {
            return;
        }
        if(mRemotePlayBackCaptureRl.getVisibility() == View.VISIBLE) {
            if(mOrientation == Configuration.ORIENTATION_PORTRAIT || mRemotePlayBackTimeBarRl.getVisibility() == View.GONE) {
                if(mRemotePlayBackControlRl.getVisibility() == View.VISIBLE) {
                    mRemotePlayBackCaptureRlLp.setMargins(0, 0, 0, Utils.dip2px(this, mAlarmStartTime != null?60:40));
                } else {
                    mRemotePlayBackCaptureRlLp.setMargins(0, 0, 0, mAlarmStartTime != null?Utils.dip2px(this, 2):0);
                }
                mRemotePlayBackCaptureRl.setLayoutParams(mRemotePlayBackCaptureRlLp);
            } else {
                if(mAlarmStartTime != null) {
                    if(mRemotePlayBackControlRl.getVisibility() == View.VISIBLE) {
                        mRemotePlayBackCaptureRlLp.setMargins(0, 0, 0, Utils.dip2px(this, 60));
                    } else {
                        mRemotePlayBackCaptureRlLp.setMargins(0, 0, 0, Utils.dip2px(this, 2));
                    }
                } else {
                    mRemotePlayBackCaptureRlLp.setMargins(0, 0, 0, Utils.dip2px(this, 87));
                }
                mRemotePlayBackCaptureRl.setLayoutParams(mRemotePlayBackCaptureRlLp);
            }
            if(mRemotePlayBackCaptureWatermarkIv.getTag() != null) {
                mRemotePlayBackCaptureWatermarkIv.setVisibility(View.VISIBLE);
                mRemotePlayBackCaptureWatermarkIv.setTag(null);
            }
        }
        if(mCaptureDisplaySec >= 4) {
            mCaptureDisplaySec = 0;
            mRemotePlayBackCaptureRl.setVisibility(View.GONE);
            mRemotePlayBackCaptureIv.setImageURI(null);
            mRemotePlayBackCaptureWatermarkIv.setTag(null);
            mRemotePlayBackCaptureWatermarkIv.setVisibility(View.GONE);
        }
    }
    
    /**
     * 更新录像时间
     * 
     * @see
     * @since V1.0
     */
    private void updateRecordTime() {
        if(mRemotePlayBackRecordIv.getVisibility() == View.VISIBLE) {
            mRemotePlayBackRecordIv.setVisibility(View.INVISIBLE);
        } else {
            mRemotePlayBackRecordIv.setVisibility(View.VISIBLE);
        }
        // 计算分秒
        int leftSecond = mRecordSecond % 3600;
        int minitue = leftSecond / 60;
        int second = leftSecond % 60;

        // 显示录像时间
        String recordTime = String.format("%02d:%02d", minitue, second);
        mRemotePlayBackRecordTv.setText(recordTime);
    }
    
    // 处理密码错误
    private void handlePasswordError(int title_resid, int msg1_resid, int msg2_resid) {
        stopRemotePlayBack();
        setRemotePlayBackStopUI();
        
        if(mStatus == STATUS_START || mStatus == STATUS_PLAY) {
            return;
        }
        
        // 检查网络是否可用
        if (!ConnectionDetector.isNetworkAvailable(this)) {
            // 提示没有连接网络
            setRemotePlayBackFailUI(getString(R.string.remoteplayback_searchfile_fail_for_network));
            return;
        }
        
        if(mRemotePlayBackMgr != null && mStatus == STATUS_PAUSE) {
            resumeRemotePlayBack();
            setRemotePlayBackSuccessUI();
            return;
        }
        
        mStatus = STATUS_START;
        setRemotePlayBackLoadingUI();
        updateLoadingProgress(0);
        
        Calendar seletedTime = getTimeBarSeekTime();
        LogUtil.debugLog(TAG, "startRemotePlayBack:" + seletedTime);
        
        if(mCloudFileList == null) {
            searchCloudFileList(seletedTime);
            return;
        }
        
        mRemotePlayBackMgr = new RemotePlayBackManager(this);
        mRemotePlayBackMgr.setHandler(mHandler);
        mRemotePlayBackMgr.setPlaySurface(mRemotePlayBackSh);
        
        if(mAlarmStartTime != null) {
            mRemotePlayBackHelper.startEncryptRemotePlayBackTask(this, mRemotePlayBackMgr, mCameraInfo.getCameraId(), 
                    seletedTime!=null?seletedTime:mAlarmStartTime, mAlarmStopTime, title_resid, msg1_resid, msg2_resid);
        } else {
            mRemotePlayBackHelper.startEncryptRemotePlayBackTask(this, mRemotePlayBackMgr, mCameraInfo.getCameraId(), 
                    seletedTime!=null?seletedTime:mStartTime, mEndTime, title_resid, msg1_resid, msg2_resid);
        }
    }
    
    /**
     * 处理播放成功的情况
     * 
     * @see
     * @since V1.0
     */
    private void handlePlaySuccess(Message msg) {
        LogUtil.debugLog(TAG, "handlePlaySuccess:" + msg.arg1);
        mStatus = STATUS_PLAY;
        
        if (msg.arg1 != 0) {
            mRealRatio = (float) msg.arg2 / msg.arg1;
        } else {
            mRealRatio = Constant.LIVE_VIEW_RATIO;
        }
        setRemotePlayBackSvLayout(1);
        
        setRemotePlayBackSuccessUI();
        setRemotePlaySound();
    }
    
    private void setRemotePlayBackSvLayout(float screenRatio) {
        // 设置播放窗口位置
        final int screenWidth = (mOrientation == Configuration.ORIENTATION_PORTRAIT) ? mLocalInfo.getScreenWidth()
                : (mLocalInfo.getScreenWidth() - mLocalInfo.getNavigationBarHeight());
        final int screenHeight = (mOrientation == Configuration.ORIENTATION_PORTRAIT) ? (mLocalInfo.getScreenHeight() - mLocalInfo
                .getNavigationBarHeight()) : mLocalInfo.getScreenHeight();
        final RelativeLayout.LayoutParams remoteplaybackSvlp = Utils.getPlayViewLp(mRealRatio, mOrientation,
                mLocalInfo.getScreenWidth(), (int) (mLocalInfo.getScreenWidth() * Constant.LIVE_VIEW_RATIO),
                (int) (screenWidth * screenRatio), (int) (screenHeight * screenRatio));

        FrameLayout.LayoutParams remoteplaybackSvRlLp = new FrameLayout.LayoutParams(remoteplaybackSvlp.width, remoteplaybackSvlp.height);
        findViewById(R.id.remoteplayback_sv_rl).setLayoutParams(remoteplaybackSvRlLp);
        RelativeLayout.LayoutParams svLp = new RelativeLayout.LayoutParams(remoteplaybackSvlp.width, remoteplaybackSvlp.height);
        mRemotePlayBackSv.setLayoutParams(svLp);
        if (screenRatio == 1) {
            mRemotePlayBackRatioTv.setVisibility(View.GONE);
            FrameLayout.LayoutParams remotePlayBackRlLp = (FrameLayout.LayoutParams) mRemotePlayBackPageLy
                    .getLayoutParams();
            remotePlayBackRlLp.width = LayoutParams.MATCH_PARENT;
            remotePlayBackRlLp.height = LayoutParams.MATCH_PARENT;
            mRemotePlayBackPageLy.setLayoutParams(remotePlayBackRlLp);

            mHVScrollView.setLayoutParams(remoteplaybackSvlp);
        } else {
            RelativeLayout.LayoutParams remotePlayBackRatioTvLp = (RelativeLayout.LayoutParams) mRemotePlayBackRatioTv
                    .getLayoutParams();
            remotePlayBackRatioTvLp.setMargins(Utils.dip2px(this, 70), Utils.dip2px(this, 20), 0, 0);
            mRemotePlayBackRatioTv.setLayoutParams(remotePlayBackRatioTvLp);
            mRemotePlayBackRatioTv.setVisibility(View.VISIBLE);

            RelativeLayout.LayoutParams svViewLp = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT,
                    LayoutParams.MATCH_PARENT);
            svViewLp.addRule(RelativeLayout.CENTER_IN_PARENT);
            mHVScrollView.setLayoutParams(svViewLp);
        }

    }
    
    /**
     * 处理播放失败的情况
     * 
     * @param errorCode
     *            - 错误码
     * @see
     * @since V1.0
     */
    private void handlePlayFail(int errorCode) {
        LogUtil.debugLog(TAG, "handlePlayFail:" + errorCode);

        stopRemotePlayBack();
        
        String txt = null;
        // 判断返回的错误码
        switch (errorCode) {
            case HCNetSDKException.NET_DVR_PASSWORD_ERROR:
            case HCNetSDKException.NET_DVR_NOENOUGHPRI:
	     case ErrorCode.ERROR_DVR_LOGIN_USERID_ERROR:
                // 弹出密码输入框
                handlePasswordError(R.string.realplay_password_error_title,
                        R.string.realplay_password_error_message3, 
                        R.string.realplay_password_error_message1);
                return;
            case ErrorCode.ERROR_WEB_SESSION_ERROR:
            case ErrorCode.ERROR_WEB_SESSION_EXPIRE:
            case ErrorCode.ERROR_CAS_PLATFORM_CLIENT_NO_SIGN_RELEATED:
            case ErrorCode.ERROR_WEB_HARDWARE_SIGNATURE_ERROR:
            case ErrorCode.ERROR_CAS_VERIFY_SESSION_ERROR:
            case ErrorCode.ERROR_RTSP_SESSION_NOT_EXIST:                        
                EzvizAPI.getInstance().gotoLoginPage();
                return;
                // case HCNetSDKException.NET_DVR_RTSP_DESCRIBESERVERERR:
            case RtspClientException.RTSPCLIENT_DEVICE_CONNECTION_LIMIT:
            case HCNetSDKException.NET_DVR_RTSP_OVER_MAX_CHAN:
            case RtspClientException.RTSPCLIENT_OVER_MAXLINK:
            case HCNetSDKException.NET_DVR_OVER_MAXLINK:
            case CASClientSDKException.CASCLIENT_MSG_PU_NO_RESOURCE:
                txt = getString(R.string.remoteplayback_over_link);
                break;
            case ErrorCode.ERROR_WEB_DIVICE_NOT_ONLINE:
            case ErrorCode.ERROR_RTSP_NOT_FOUND:
            case ErrorCode.ERROR_CAS_PLATFORM_CLIENT_REQUEST_NO_PU_FOUNDED:
                txt = getString(R.string.realplay_fail_device_not_exist);
                break;
            case ErrorCode.ERROR_WEB_DIVICE_SO_TIMEOUT:
                txt = getString(R.string.realplay_fail_connect_device);
                break;
            case HCNetSDKException.NET_DVR_RTSP_PRIVACY_STATUS:
            case RtspClientException.RTSPCLIENT_PRIVACY_STATUS:
                txt = getString(R.string.realplay_set_fail_status);
                break;
            case InnerException.INNER_DEVICE_NOT_EXIST:
                txt = getString(R.string.camera_not_online);
                break;   
            case HCNetSDKException.NET_DVR_NETWORK_FAIL_CONNECT:
                txt = getString(R.string.remoteplayback_connect_device_error);
                break;
            case ErrorCode.ERROR_CAS_CONNECT_FAILED:
                txt = getString(R.string.remoteplayback_connect_server_error);
                break;
            case ErrorCode.ERROR_INNER_DEVICE_ENCRYPT_PASSWORD_IS_NULL:
                txt = null;
                break;        
            case ErrorCode.ERROR_WEB_CODE_ERROR:
                VerifySmsCodeUtil.openSmsVerifyDialog(Constant.SMS_VERIFY_LOGIN, this, this);
                txt = Utils.getErrorTip(this, R.string.check_feature_code_fail, errorCode);
                break;
            case ErrorCode.ERROR_WEB_HARDWARE_SIGNATURE_OP_ERROR:
                VerifySmsCodeUtil.openSmsVerifyDialog(Constant.SMS_VERIFY_HARDWARE, this, this);
                txt = Utils.getErrorTip(this, R.string.check_feature_code_fail, errorCode);
                break;               
            default:
                txt = Utils.getErrorTip(this, R.string.remoteplayback_fail, errorCode);
                break;
        }

        setRemotePlayBackFailUI(txt);
    }
    
    /**
     * 处理播放失败的情况
     * 
     * @param errorCode
     *            - 错误码
     * @see
     * @since V1.0
     */
    private void handlePlayFinish() {
        LogUtil.debugLog(TAG, "handlePlayFinish");

        stopRemotePlayBack();
        
        if(mAlarmStartTime != null) {
            mRemotePlayBackProgressBar.setProgress(mRemotePlayBackProgressBar.getMax());
            mRemotePlayBackSeekBar.setProgress(mRemotePlayBackSeekBar.getMax());
            setRemotePlayBackFailUI(null);
        } else {
            setRemotePlayBackFailUI(null);
        }
    }
    
    /**
     * 启动定时器
     * 
     * @see
     * @since V1.0
     */
    private void startUpdateTimer() {
        stopUpdateTimer();
        // 开始录像计时
        mUpdateTimer = new Timer();
        mUpdateTimerTask = new TimerTask() {
            @Override
            public void run() {      
                if((mRemotePlayBackControlRl.getVisibility() == View.VISIBLE || mRemotePlayBackFullOperateBar.getVisibility() == View.VISIBLE)
                    && mControlDisplaySec < 5) {
                    mControlDisplaySec++;
                }
                if(mRemotePlayBackCaptureRl.getVisibility() == View.VISIBLE && mCaptureDisplaySec < 4) {
                    mCaptureDisplaySec++;
                }

                // 更新录像时间
                if (mRecordFilePath != null) {
                    // 更新录像时间
                    Calendar OSDTime = mRemotePlayBackMgr.getOSDTime();
                    if (OSDTime != null) {
                        String playtime = Utils.OSD2Time(OSDTime);
                        if (!TextUtils.equals(playtime, mRecordTime)) {
                            mRecordSecond++;
                            mRecordTime = playtime;
                        }
                    }
                }  
                
                sendMessage(MSG_PLAY_UI_UPDATE, 0);               
            }
        };
        // 延时1000ms后执行，1000ms执行一次
        mUpdateTimer.schedule(mUpdateTimerTask, 1000, 1000);
    }
    
    /**
     * 停止定时器
     * 
     * @see
     * @since V1.0
     */
    private void stopUpdateTimer() {
        mCaptureDisplaySec = 4;
        updateCaptureUI();
        mHandler.removeMessages(MSG_PLAY_UI_UPDATE);
        // 停止录像计时
        if (mUpdateTimer != null) {
            mUpdateTimer.cancel();
            mUpdateTimer = null;
        }

        if (mUpdateTimerTask != null) {
            mUpdateTimerTask.cancel();
            mUpdateTimerTask = null;
        }
    }
    
    /**
     * 发送消息
     * 
     * @param msg
     *            - 消息对象
     * @param arg1
     *            - 错误码
     * @since V1.0
     */
    public void sendMessage(int msg, int arg1) {
        if (mHandler != null) {
            Message message = Message.obtain();
            message.what = msg;
            message.arg1 = arg1;
            mHandler.sendMessage(message);
        }
    }
    
    public void sendMessage(int msg, int arg1, Object obj) {
        if (mHandler != null) {
            Message message = Message.obtain();
            message.what = msg;
            message.arg1 = arg1;
            message.obj = obj;
            mHandler.sendMessage(message);
        }
    }
    
    private void dismissPopDialog(AlertDialog popDialog) {
        if (popDialog != null && popDialog.isShowing() && !isFinishing()) {
            try {
                popDialog.dismiss();
            } catch (Exception e) {
                // TODO: handle exception
            }
        }
    }

    /* (non-Javadoc)
     * @see com.videogo.widget.TimeBarHorizontalScrollView.TimeScrollBarScrollListener#onScrollChanged(int, int, int, int, android.widget.HorizontalScrollView)
     */
    @Override
    public void onScrollChanged(int left, int top, int oldLeft, int oldTop, HorizontalScrollView scrollView) {   
        Calendar startCalendar = mRemoteFileTimeBar.pos2Calendar(left, mOrientation);
        if(startCalendar != null) {
            mPlayTime = startCalendar.getTimeInMillis();
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
            mRemotePlayBackTimeTv.setText(sdf.format(mPlayTime));
        }                  
    }

    /* (non-Javadoc)
     * @see com.videogo.widget.TimeBarHorizontalScrollView.TimeScrollBarScrollListener#onScrollStart(android.widget.HorizontalScrollView)
     */
    @Override
    public void onScrollStart(HorizontalScrollView scrollView) {
        if(mStatus != STATUS_STOP) {
            stopRemotePlayBack();
        }
    }

    /* (non-Javadoc)
     * @see com.videogo.widget.TimeBarHorizontalScrollView.TimeScrollBarScrollListener#onScrollStop(android.widget.HorizontalScrollView)
     */
    @Override
    public void onScrollStop(HorizontalScrollView scrollView) {
        if(mStatus != STATUS_STOP) {
            stopRemotePlayBack();
        }
        startRemotePlayBack(getTimeBarSeekTime());
    }

    /* (non-Javadoc)
     * @see com.videogo.ui.util.VerifySmsCodeUtil.OnVerifyListener#onVerify(int, int)
     */
    @Override
    public void onVerify(int type, int result) {
        if(result == 0) {
            startRemotePlayBack(null);
        }
    }
}

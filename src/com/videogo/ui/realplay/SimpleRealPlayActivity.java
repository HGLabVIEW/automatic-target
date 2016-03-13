/* 
 * @ProjectName VideoGo
 * @Copyright HangZhou Hikvision System Technology Co.,Ltd. All Right Reserved
 * 
 * @FileName RealPlayerActivity.java
 * @Description 杩欓噷瀵规枃浠惰繘琛屾弿杩�
 * 
 * @author Dengshihua
 * @data 2012-8-20
 * 
 * @note 杩欓噷鍐欐湰鏂囦欢鐨勮缁嗗姛鑳芥弿杩板拰娉ㄩ噴
 * @note 鍘嗗彶璁板綍
 * 
 * @warning 杩欓噷鍐欐湰鏂囦欢鐨勭浉鍏宠鍛�
 */
package com.videogo.ui.realplay;

import java.util.ArrayList;
import java.util.Random;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.videogo.R;
import com.videogo.constant.Constant;
import com.videogo.constant.IntentConsts;
import com.videogo.exception.ErrorCode;
import com.videogo.exception.HCNetSDKException;
import com.videogo.exception.InnerException;
import com.videogo.exception.RtspClientException;
import com.videogo.openapi.EzvizAPI;
import com.videogo.openapi.bean.resp.CameraInfo;
import com.videogo.realplay.RealPlayMsg;
import com.videogo.realplay.RealPlayStatus;
import com.videogo.realplay.RealPlayerHelper;
import com.videogo.realplay.RealPlayerManager;
import com.videogo.ui.devicelist.DeviceDiscoverInfo;
import com.videogo.ui.realplay.SimpleRealPlayerAdapter.RealPlayerHolder;
import com.videogo.ui.util.VerifySmsCodeUtil;
import com.videogo.util.ConnectionDetector;
import com.videogo.util.LogUtil;
import com.videogo.util.Utils;
import com.videogo.widget.PagesGallery;
import com.videogo.widget.WaitDialog;

/**
 * 绠�鍗曞疄鏃堕瑙堢晫闈�
 * 
 */
public class SimpleRealPlayActivity extends Activity implements
		OnClickListener, SurfaceHolder.Callback, OnItemSelectedListener,
		Handler.Callback, OnTouchListener, VerifySmsCodeUtil.OnVerifyListener {
	private Mat mat1;
	private Mat mat2;
	private Mat mat3;
	private Mat mat4;
	private Mat mat5;
	private Bitmap bitmap1;
	private Bitmap bitmap2;
	private Bitmap bitmap3;

	/** 鎵撳嵃鏍囩 */
	private static final String TAG = "EzvizRealPlayActivity";
	private static String path;
	/** 瀹炴椂棰勮鎺у埗瀵硅薄 */
	private RealPlayerManager mRealPlayMgr = null;
	/** 棰勮鍙栨祦浠诲姟澶勭悊瀵硅薄 */
	private RealPlayerHelper mRealPlayerHelper = null;

	/** 璁惧淇℃伅 */
	private ArrayList<CameraInfo> mCameraInfoList = null;
	/** 閫変腑閫氶亾淇℃伅 */
	private CameraInfo mCameraInfo = null;
	private DeviceDiscoverInfo mDeviceDiscoverInfo = null;

	private RelativeLayout mDisplayView = null;
	// 鏍囬鏍忔帶浠�
	/** 鏍囬鏍忓尯鍩� */
	private RelativeLayout mTitleArea = null;
	/** 鐣岄潰杩斿洖鎸夐挳 */
	private ImageButton mBackBtn = null;
	/** 鏍囬鏂囨湰鎺т欢 */
	private TextView mTitleTv = null;
	private TextView mcapTextView = null;
	private ImageView mImageView1 = null;
	private ImageView mImageView2 = null;
	private ImageView mImageView3 = null;
	/** 鎾斁鍖哄煙甯冨眬 */
	private RelativeLayout mPlayArea = null;
	/** 鎾斁鐣岄潰 */
	private SurfaceView mSurfaceView = null;
	private SurfaceHolder mSurfaceHolder = null;
	private RelativeLayout mControlArea = null;
	/** 鏆傚仠鎸夐挳 */
	private ImageButton mStopBtn = null;
	/** 鎾斁鎸夐挳 */
	private ImageButton mPlayBtn = null;
	private Button btncap1;
	private Button btncanny;
	private Button btndif;
	private Button btnhoughlines;
	// 瀹炴椂棰勮婊戝姩鏄剧ず鎺т欢
	private PagesGallery mPagesGallery = null;
	private SimpleRealPlayerAdapter mPagesAdapter = null;
	private RealPlayerHolder mSelectedRealPlayerHolder = null;
	private int mPosition = 0;
	/** 灞忓箷褰撳墠鏂瑰悜 */
	private int mOrientation = Configuration.ORIENTATION_PORTRAIT;

	/** 鏍囪瘑鏄惁姝ｅ湪鎾斁 */
	private boolean mIsPlaying = false;
	/** 瑙嗛绐楀彛鍙互鏄剧ず鐨勫尯鍩� */
	private Rect mCanDisplayRect = null;
	/** 绔栧睆鏃剁殑瀹藉害 */
	private int mDisplayWidth = 0;
	/** 绔栧睆鏃剁殑楂樺害 */
	private int mDisplayHeight = 0;
	private double mPlayRatio = 0;

	/** 娑堟伅鍙ユ焺 */
	private Handler mHandler = null;
	/** 鍙橀噺/甯搁噺璇存槑 */
	private GestureDetector mGestureDetector;
	/** 鐩戝惉閿佸睆瑙ｉ攣鐨勪簨浠� */
	private ScreenBroadcastReceiver mScreenBroadcastReceiver = null;

	private AlertDialog mPlayFailDialog = null;
	private WaitDialog mWaitDialog = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.realplayer_page);

		// 鍒濆鍖栨暟鎹�
		initData();

		// 鍒濆鍖栨帶浠�
		initViews();
		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this,
				mLoaderCallback);
	}

	/**
	 * 鍒濆鍖栨暟鎹�
	 * 
	 * @see
	 * @since V2.0
	 */
	private void initData() {

		// 鑾峰彇鎾斁璁惧淇℃伅
		Intent intent = getIntent();
		if (intent != null) {
			mCameraInfo = (CameraInfo) intent
					.getParcelableExtra(IntentConsts.EXTRA_CAMERA_INFO);
			mCameraInfoList = intent
					.getParcelableArrayListExtra(IntentConsts.EXTRA_CAMERA_INFO_LIST);
			mDeviceDiscoverInfo = (DeviceDiscoverInfo) intent
					.getParcelableExtra("DeviceDiscoverInfo");
			if (mCameraInfo != null && mCameraInfoList != null) {
				CameraInfo cameraInfo = null;
				for (int i = 0; i < mCameraInfoList.size(); i++) {
					cameraInfo = mCameraInfoList.get(i);
					if (cameraInfo != null
							&& cameraInfo.getCameraId().equals(
									mCameraInfo.getCameraId())) {
						mPosition = i;
						break;
					}
				}
			} else if (mCameraInfo != null) {
				mCameraInfoList = new ArrayList<CameraInfo>();
				mCameraInfoList.add(mCameraInfo);
				mPosition = 0;
			} else if (mDeviceDiscoverInfo != null) {
				mCameraInfo = new CameraInfo();
				mCameraInfo.setCameraName(mDeviceDiscoverInfo.deviceName);
				mCameraInfo.setDeviceId(mDeviceDiscoverInfo.deviceID);
				mCameraInfoList = new ArrayList<CameraInfo>();
				mCameraInfoList.add(mCameraInfo);
				mPosition = 0;
			} else {
				finish();
				return;
			}
		}

		mRealPlayerHelper = RealPlayerHelper.getInstance(getApplication());
		mHandler = new Handler(this);
		mCanDisplayRect = new Rect(0, 0, 0, 0);

		mScreenBroadcastReceiver = new ScreenBroadcastReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_USER_PRESENT);
		filter.addAction(Intent.ACTION_SCREEN_OFF);
		registerReceiver(mScreenBroadcastReceiver, filter);
	}

	@Override
	protected void onResume() {
		super.onResume();

		mSurfaceView.setVisibility(View.VISIBLE);
		if (mCameraInfo == null) {
			return;
		}

		if (mSelectedRealPlayerHolder != null) {
			// 寮�濮嬫挱鏀�
			startRealPlay();

		}

	}

	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			if (status == LoaderCallbackInterface.SUCCESS) {
				// now we can call opencv code !
				btncanny.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						// TODO 鑷姩鐢熸垚鐨勬柟娉曞瓨鏍�
						mat1 = new Mat();
						mat2 = new Mat();
						mat3 = new Mat();
						mat4 = new Mat();
						mat5 = new Mat();
						bitmap1 = drawableToBitmap(mImageView1.getDrawable());
						bitmap2 = drawableToBitmap(mImageView2.getDrawable());
						bitmap3 = drawableToBitmap(mImageView2.getDrawable());
						mImageView3.setImageBitmap(bitmap3);
						org.opencv.android.Utils.bitmapToMat(bitmap1, mat1);
						org.opencv.android.Utils.bitmapToMat(bitmap2, mat2);
						/*
						 * Imgproc.cvtColor(mat1, mat3,
						 * Imgproc.COLOR_BGRA2GRAY); Imgproc.cvtColor(mat2,
						 * mat4, Imgproc.COLOR_BGRA2GRAY);
						 */

						Imgproc.Canny(mat1, mat3, 30, 40);
						Imgproc.Canny(mat2, mat4, 30, 40);
						org.opencv.android.Utils.matToBitmap(mat3, bitmap1);
						org.opencv.android.Utils.matToBitmap(mat4, bitmap2);
						mImageView1.setImageBitmap(bitmap1);
						mImageView2.setImageBitmap(bitmap2);

					}
				});
				btnhoughlines.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						// TODO 鑷姩鐢熸垚鐨勬柟娉曞瓨鏍�
						Mat lines = new Mat();
						Scalar color = new Scalar(255, 0, 0);
						Imgproc.HoughLinesP(mat3, lines, 1, Math.PI / 180, 35,
								10, 10);
						for (int i = 0; i < lines.cols(); i++) {
							double[] points = lines.get(0, i);
							double x1 = points[0];
							double y1 = points[1];
							double x2 = points[2];
							double y2 = points[3];
							Point l1 = new Point(x1, y1);
							Point l2 = new Point(x2, y2);
							Imgproc.line(mat3, l1, l2, color, 3);
						}
						org.opencv.android.Utils.matToBitmap(mat3, bitmap1);
						mImageView1.setImageBitmap(bitmap1);
					}
				});
				btndif.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						// TODO 鑷姩鐢熸垚鐨勬柟娉曞瓨鏍�
						Core.absdiff(mat3, mat4, mat5);
						org.opencv.android.Utils.matToBitmap(mat5, bitmap3);
						mImageView3.setImageBitmap(bitmap3);
					}
				});

			} else {
				super.onManagerConnected(status);
			}
		}
	};

	/**
	 * 鍒濆鍖栨帶浠�
	 * 
	 * @see
	 * @since V1.0
	 */
	private void initViews() {
		// 淇濇寔灞忓箷甯镐寒
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		// 鑾峰彇灞忓箷闀垮
		DisplayMetrics metric = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metric);
		mDisplayWidth = metric.widthPixels;
		mDisplayHeight = metric.heightPixels;
		mGestureDetector = new GestureDetector(SimpleRealPlayActivity.this,
				onGestureListener);

		mDisplayView = (RelativeLayout) findViewById(R.id.realplay_display_view);
		// 鏍囬鏍忔帶浠�
		mTitleArea = (RelativeLayout) findViewById(R.id.realplay_top_bar);
		mBackBtn = (ImageButton) findViewById(R.id.realplay_back_btn);
		mTitleTv = (TextView) findViewById(R.id.realplay_title_tv);
		if (mCameraInfo != null) {
			mTitleTv.setText(mCameraInfo.getCameraName());
			mTitleTv.setText("基于Android的视频数据获取及图处理技术研究");
		}

		// 鎾斁鍖哄煙
		mPlayArea = (RelativeLayout) findViewById(R.id.realplay_area);
		mSurfaceView = (SurfaceView) findViewById(R.id.realplay_wnd_sv);
		mSurfaceView.getHolder().addCallback(this);
		mSurfaceView.setVisibility(View.INVISIBLE);

		// 鎺у埗鏍忔帶浠�
		mControlArea = (RelativeLayout) findViewById(R.id.realplay_control_bar);
		mStopBtn = (ImageButton) findViewById(R.id.realplay_stop_btn);
		mPlayBtn = (ImageButton) findViewById(R.id.realplay_play_btn);
		btncap1 = (Button) findViewById(R.id.btncap1);
		btncanny = (Button) findViewById(R.id.btncanny);
		btndif = (Button) findViewById(R.id.btndif);
		btnhoughlines = (Button) findViewById(R.id.btnhoughlines);
		mImageView1 = (ImageView) findViewById(R.id.capimg1);
		mImageView2 = (ImageView) findViewById(R.id.capimg2);
		mImageView3 = (ImageView) findViewById(R.id.capimg3);
		mStopBtn.setOnClickListener(this);
		mPlayBtn.setOnClickListener(this);
		mBackBtn.setOnClickListener(this);
		btncap1.setOnClickListener(this);
		mTitleArea.setOnClickListener(this);
		mControlArea.setOnClickListener(this);
		mTitleTv.setOnClickListener(this);

		mPagesAdapter = new SimpleRealPlayerAdapter(this);
		mPagesAdapter.setOrientation(mOrientation);
		mPagesAdapter.setDisplayWidthHeight(mDisplayWidth, mDisplayHeight);
		mPagesAdapter.setCameraList(mCameraInfoList);

		mPagesGallery = (PagesGallery) findViewById(R.id.realplay_pages_gallery);
		mPagesGallery.setVerticalFadingEdgeEnabled(false);
		mPagesGallery.setHorizontalFadingEdgeEnabled(false);
		mPagesGallery.setOnItemSelectedListener(this);
		mPagesGallery.setAdapter(mPagesAdapter);
		mPagesGallery.setSelection(mPosition);
		mPagesGallery.setOnTouchListener(this);

		mWaitDialog = new WaitDialog(SimpleRealPlayActivity.this,
				android.R.style.Theme_Translucent_NoTitleBar);
		mWaitDialog.setCancelable(false);
	}

	@Override
	protected void onStop() {
		super.onStop();

		stopRealPlay(false);
		mSurfaceView.setVisibility(View.INVISIBLE);
	}

	private void setPlayLoadingUI() {
		mSurfaceView.setVisibility(View.VISIBLE);
		mPlayBtn.setVisibility(View.GONE);
		mStopBtn.setVisibility(View.VISIBLE);

		if (mSelectedRealPlayerHolder != null) {
			mSelectedRealPlayerHolder.mFigureIv.setVisibility(View.VISIBLE);
			mSelectedRealPlayerHolder.mLoadingRL.setVisibility(View.VISIBLE);
			mSelectedRealPlayerHolder.mPlayIv.setVisibility(View.GONE);
		}
	}

	private void setPlayStopUI() {
		mIsPlaying = false;
		mSurfaceView.setVisibility(View.INVISIBLE);
		mPlayBtn.setVisibility(View.VISIBLE);
		mStopBtn.setVisibility(View.GONE);

		if (mSelectedRealPlayerHolder != null) {
			mSelectedRealPlayerHolder.mFigureIv.setVisibility(View.VISIBLE);
			mSelectedRealPlayerHolder.mLoadingRL.setVisibility(View.GONE);
			mSelectedRealPlayerHolder.mPlayIv.setVisibility(View.VISIBLE);
		}
	}

	private void setPlayStartUI() {
		mIsPlaying = true;
		mSurfaceView.setVisibility(View.VISIBLE);
		mPlayBtn.setVisibility(View.GONE);
		mStopBtn.setVisibility(View.VISIBLE);

		if (mSelectedRealPlayerHolder != null) {
			mSelectedRealPlayerHolder.mFigureIv.setVisibility(View.GONE);
			mSelectedRealPlayerHolder.mLoadingRL.setVisibility(View.GONE);
			mSelectedRealPlayerHolder.mPlayIv.setVisibility(View.GONE);
		}
	}

	/**
	 * 鍋滄鎾斁
	 * 
	 * @see
	 * @since V2.0
	 */
	private void stopRealPlay(boolean onScroll) {
		LogUtil.debugLog(TAG, "stopRealPlay");

		if (mRealPlayMgr != null) {
			// 鍋滄棰勮浠诲姟
			mRealPlayerHelper.stopRealPlayTask(mRealPlayMgr);
		}

		setPlayStopUI();

		if (onScroll) {
			if (mOrientation == Configuration.ORIENTATION_PORTRAIT) {
				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
			}
		} else {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onDestroy()
	 */
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mScreenBroadcastReceiver != null) {
			// 鍙栨秷閿佸睆骞挎挱鐨勬敞鍐�
			unregisterReceiver(mScreenBroadcastReceiver);
		}

		if (mPagesAdapter != null) {
			mPagesAdapter.setCameraList(null);
			mPagesAdapter.notifyDataSetChanged();
			mPagesAdapter = null;
		}
		if (mPagesGallery != null) {
			mSelectedRealPlayerHolder = null;
			mPagesGallery.setAdapter(null);
			mPagesGallery = null;
		}
	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
	}

	@Override
	public void onItemSelected(AdapterView<?> parent, View v, int position,
			long id) {
		if (null == mPagesAdapter || null == mPagesGallery || null == v) {
			return;
		}

		LogUtil.debugLog(TAG, "onItemSelected");

		if (mSelectedRealPlayerHolder == null) {
			mSelectedRealPlayerHolder = (RealPlayerHolder) v.getTag();
			if (mSelectedRealPlayerHolder != null) {
				// 寮�濮嬫挱鏀�
				startRealPlay();
			}
			return;
		}

		if (mPosition == position) {
			return;
		}

		if (position < 0 || position > (mPagesAdapter.getCount() - 1)) {
			return;
		}

		// 濡傛灉褰撳墠瑙嗛杩樺湪鎾斁锛岃繕闇�瑕佸仠姝㈡挱鏀�
		stopRealPlay(true);

		mSelectedRealPlayerHolder = (RealPlayerHolder) v.getTag();
		mPosition = position;

		CameraInfo cameraInfo = mCameraInfoList.get(mPosition);
		if (cameraInfo == null) {
			return;
		}
		mPlayRatio = 0;
		mCameraInfo = cameraInfo;

		mTitleTv.setText(mCameraInfo.getCameraName());

		startRealPlay();
	}

	/**
	 * 瀹氫箟灞忓箷鐨勭偣鍑讳簨浠�
	 * 
	 * @author fangzhihua
	 * @data 2013-3-19
	 */
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		return mGestureDetector.onTouchEvent(event);
	}

	/**
	 * 瀹氫箟宸﹀彸婊戝姩鐨勭洃鍚�
	 */
	private final GestureDetector.OnGestureListener onGestureListener = new GestureDetector.SimpleOnGestureListener() {
		@Override
		public boolean onSingleTapUp(MotionEvent e) {
			// TODO Auto-generated method stub
			onSurfaceViewClick();
			return true;
		}

		@Override
		public boolean onDoubleTap(MotionEvent e) {
			return true;
		}
	};

	/**
	 * 寮�濮嬫挱鏀�
	 * 
	 * @see
	 * @since V1.0
	 */
	private void startRealPlay() {
		LogUtil.debugLog(TAG, "startRealPlay");

		// 妫�鏌ョ綉缁滄槸鍚﹀彲鐢�
		if (!ConnectionDetector.isNetworkAvailable(this)) {
			// 鎻愮ず娌℃湁杩炴帴缃戠粶
			showPlayFailDialog(getString(R.string.realplay_play_fail_becauseof_network));
			setPlayStopUI();
			return;
		}

		setPlayLoadingUI();

		mRealPlayMgr = new RealPlayerManager(this);
		// 璁剧疆Handler锛屾帴鏀跺鐞嗘秷鎭�
		mRealPlayMgr.setHandler(mHandler);
		// 璁剧疆鎾斁Surface
		mRealPlayMgr.setPlaySurface(mSurfaceHolder);
		// 寮�鍚瑙堜换鍔�
		if (TextUtils.isEmpty(mCameraInfo.getCameraId())) {
			if (!TextUtils.isEmpty(EzvizAPI.getInstance().getUserCode())) {
				mRealPlayerHelper.startLocalRealPlayTask(mRealPlayMgr,
						mCameraInfo.getDeviceId(), mDeviceDiscoverInfo.localIP,
						null);
			} else {
				mRealPlayerHelper.startEncryptLocalRealPlayTask(this,
						mRealPlayMgr, mCameraInfo.getDeviceId(),
						mDeviceDiscoverInfo.localIP,
						R.string.realplay_password_error_title,
						R.string.realplay_login_password_msg, 0);
			}
		} else {
			mRealPlayerHelper.startRealPlayTask(mRealPlayMgr,
					mCameraInfo.getCameraId());
		}

		updateLoadingProgress(0);
	}

	private void updateLoadingProgress(final int progress) {
		if (mSelectedRealPlayerHolder == null
				|| mSelectedRealPlayerHolder.mWaittingTv == null) {
			return;
		}
		mSelectedRealPlayerHolder.mWaittingTv.setText(progress + "%");
		mHandler.postDelayed(new Runnable() {

			@Override
			public void run() {
				if (mSelectedRealPlayerHolder == null
						|| mSelectedRealPlayerHolder.mWaittingTv == null) {
					return;
				}
				Random r = new Random();
				mSelectedRealPlayerHolder.mWaittingTv.setText((progress + r
						.nextInt(20)) + "%");
			}

		}, 500);
	}

	@Override
	public boolean handleMessage(Message msg) {
		switch (msg.what) {
		case RealPlayMsg.MSG_REALPLAY_PLAY_SUCCESS:
			// 澶勭悊棰勮鎴愬姛
			handlePlaySuccess(msg);
			break;
		case RealPlayMsg.MSG_REALPLAY_PLAY_FAIL:
			// 澶勭悊棰勮澶辫触
			handlePlayFail(msg.arg1);
			break;
		case RealPlayMsg.MSG_REALPLAY_PASSWORD_ERROR:
			// 澶勭悊鎾斁瀵嗙爜閿欒
			if (TextUtils.isEmpty(mCameraInfo.getCameraId())) {
				Utils.showToast(this, R.string.realplay_login_password_error,
						msg.arg1);
				handlePasswordError(R.string.realplay_password_error_title,
						R.string.realplay_login_password_msg, 0, false);
			} else {
				handlePasswordError(R.string.realplay_password_error_title,
						R.string.realplay_password_error_message3,
						R.string.realplay_password_error_message1, false);
			}
			break;
		case RealPlayMsg.MSG_REALPLAY_ENCRYPT_PASSWORD_ERROR:
			// 澶勭悊鎾斁瀵嗙爜閿欒
			handlePasswordError(R.string.realplay_encrypt_password_error_title,
					R.string.realplay_encrypt_password_error_message, 0, true);
			break;

		case RealPlayMsg.MSG_REALPLAY_PLAY_START:
			updateLoadingProgress(40);
			break;
		case RealPlayMsg.MSG_REALPLAY_CONNECTION_START:
			updateLoadingProgress(60);
			break;
		case RealPlayMsg.MSG_REALPLAY_CONNECTION_SUCCESS:
			updateLoadingProgress(80);
			break;
		case RealPlayMsg.MSG_GET_CAMERA_INFO_SUCCESS:
			updateLoadingProgress(20);
			break;
		case RealPlayMsg.MSG_CAPTURE_PICTURE_SUCCESS:
			path = (String) msg.obj;
			if (mImageView1.getDrawable() == null) {
				mImageView1.setImageURI(Uri.parse(path));
			} else {
				mImageView2.setImageURI(Uri.parse(path));
			}
			break;
		default:
			break;
		}
		return false;
	}

	/**
	 * 澶勭悊鎾斁鎴愬姛鐨勬儏鍐�
	 * 
	 * @see
	 * @since V1.0
	 */
	private void handlePlaySuccess(Message msg) {
		if (msg.arg1 != 0) {
			mPlayRatio = Math.min((double) msg.arg2 / msg.arg1,
					Constant.LIVE_VIEW_RATIO);
		}
		if (mPlayRatio != 0) {
			setPlayViewPosition();
		}

		setPlayStartUI();

		// 寮�鍚棆杞紶鎰熷櫒
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
	}

	/**
	 * 澶勭悊鎾斁澶辫触鐨勬儏鍐�
	 * 
	 * @param errorCode
	 *            - 閿欒鐮�
	 * @see
	 * @since V1.0
	 */
	private void handlePlayFail(int errorCode) {
		LogUtil.debugLog(TAG, "handlePlayFail:" + errorCode);
		stopRealPlay(false);

		String msg = null;
		// 鍒ゆ柇杩斿洖鐨勯敊璇爜
		switch (errorCode) {
		case HCNetSDKException.NET_DVR_PASSWORD_ERROR:
		case HCNetSDKException.NET_DVR_NOENOUGHPRI:
		case ErrorCode.ERROR_DVR_LOGIN_USERID_ERROR:
			// 寮瑰嚭瀵嗙爜杈撳叆妗�
			handlePasswordError(R.string.realplay_password_error_title,
					R.string.realplay_password_error_message3,
					R.string.realplay_password_error_message1, false);
			return;
		case ErrorCode.ERROR_WEB_SESSION_ERROR:
		case ErrorCode.ERROR_WEB_SESSION_EXPIRE:
		case ErrorCode.ERROR_CAS_PLATFORM_CLIENT_NO_SIGN_RELEATED:
		case ErrorCode.ERROR_WEB_HARDWARE_SIGNATURE_ERROR:
			EzvizAPI.getInstance().gotoLoginPage();
			return;
		case RtspClientException.RTSPCLIENT_DEVICE_CONNECTION_LIMIT:
		case HCNetSDKException.NET_DVR_RTSP_OVER_MAX_CHAN:
		case RtspClientException.RTSPCLIENT_OVER_MAXLINK:
		case HCNetSDKException.NET_DVR_OVER_MAXLINK:
			msg = getString(R.string.remoteplayback_over_link);
			break;
		case ErrorCode.ERROR_WEB_DIVICE_NOT_ONLINE:
		case ErrorCode.ERROR_RTSP_NOT_FOUND:
		case ErrorCode.ERROR_CAS_PLATFORM_CLIENT_REQUEST_NO_PU_FOUNDED:
			msg = getString(R.string.realplay_fail_device_not_exist);
			break;
		case ErrorCode.ERROR_WEB_DIVICE_SO_TIMEOUT:
			msg = getString(R.string.realplay_fail_connect_device);
			break;
		case HCNetSDKException.NET_DVR_RTSP_PRIVACY_STATUS:
		case RtspClientException.RTSPCLIENT_PRIVACY_STATUS:
			msg = getString(R.string.realplay_set_fail_status);
			break;
		case InnerException.INNER_DEVICE_NOT_EXIST:
			msg = getString(R.string.camera_not_online);
			break;
		case ErrorCode.ERROR_WEB_CODE_ERROR:
			VerifySmsCodeUtil.openSmsVerifyDialog(Constant.SMS_VERIFY_LOGIN,
					this, this);
			msg = Utils.getErrorTip(this, R.string.check_feature_code_fail,
					errorCode);
			break;
		case ErrorCode.ERROR_WEB_HARDWARE_SIGNATURE_OP_ERROR:
			VerifySmsCodeUtil.openSmsVerifyDialog(Constant.SMS_VERIFY_HARDWARE,
					this, this);
			msg = Utils.getErrorTip(this, R.string.check_feature_code_fail,
					errorCode);
			break;
		default:
			msg = Utils.getErrorTip(this, R.string.realplay_play_fail,
					errorCode);
			break;
		}

		if (msg != null) {
			showPlayFailDialog(msg);
		}
	}

	// 澶勭悊瀵嗙爜閿欒
	private void handlePasswordError(int title_resid, int msg1_resid,
			int msg2_resid, final boolean isEncrypt) {
		stopRealPlay(false);
		setPlayStopUI();

		LogUtil.debugLog(TAG, "startRealPlay");

		// 妫�鏌ョ綉缁滄槸鍚﹀彲鐢�
		if (!ConnectionDetector.isNetworkAvailable(this)) {
			// 鎻愮ず娌℃湁杩炴帴缃戠粶
			showPlayFailDialog(getString(R.string.realplay_play_fail_becauseof_network));
			return;
		}

		setPlayLoadingUI();

		mRealPlayMgr = new RealPlayerManager(this);
		// 璁剧疆Handler锛屾帴鏀跺鐞嗘秷鎭�
		mRealPlayMgr.setHandler(mHandler);
		// 璁剧疆鎾斁Surface
		mRealPlayMgr.setPlaySurface(mSurfaceHolder);
		// 寮�鍚姞瀵嗛瑙堜换鍔�
		if (TextUtils.isEmpty(mCameraInfo.getCameraId())) {
			mRealPlayerHelper.startEncryptLocalRealPlayTask(this, mRealPlayMgr,
					mCameraInfo.getDeviceId(), mDeviceDiscoverInfo.localIP,
					title_resid, msg1_resid, msg2_resid);
		} else {
			mRealPlayerHelper.startEncryptRealPlayTask(this, mRealPlayMgr,
					mCameraInfo.getCameraId(), title_resid, msg1_resid,
					msg2_resid);
		}

		updateLoadingProgress(0);
	}

	/**
	 * 璁剧疆鎾斁绐楀彛浣嶇疆
	 * 
	 * @see
	 * @since V1.0
	 */
	private void setPlayViewPosition() {
		int canDisplayHeight = mCanDisplayRect.height();
		int canDisplayWidth = mCanDisplayRect.width();
		if (canDisplayHeight == 0 || canDisplayWidth == 0) {
			return;
		}

		// 璁剧疆鎾斁绐楀彛浣嶇疆
		RelativeLayout.LayoutParams lp = Utils.getPlayViewLp(mPlayRatio,
				mOrientation, canDisplayWidth, canDisplayHeight, mDisplayWidth,
				mDisplayHeight);
		// mSurfaceView.setLayoutParams(lp);
		if (mPagesAdapter != null) {
			mPagesAdapter.setOrientation(mOrientation);
			if (mSelectedRealPlayerHolder != null) {
				mPagesAdapter.updateFigureIvLayoutParams(
						mSelectedRealPlayerHolder.mFigureIv, lp.height + 2);
			}
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		mOrientation = newConfig.orientation;
		onOrientation();
		super.onConfigurationChanged(newConfig);

	}

	/**
	 * 灞忓箷鏃嬭浆鍝嶅簲
	 * 
	 * @see
	 * @since V1.0
	 */
	private void onOrientation() {
		setPlayViewPosition();

		if (mOrientation == Configuration.ORIENTATION_LANDSCAPE) {
			// 妯睆澶勭悊
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
			getWindow().clearFlags(
					WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);

			mDisplayView.setBackgroundColor(getResources().getColor(
					R.color.black));

			mTitleArea.setVisibility(View.GONE);
			mControlArea.setVisibility(View.GONE);
		} else {
			// 绔栧睆澶勭悊
			getWindow().addFlags(
					WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

			mDisplayView.setBackgroundColor(getResources().getColor(
					R.color.application_bg));
			mTitleArea.setVisibility(View.VISIBLE);
			mControlArea.setVisibility(View.VISIBLE);

			if (!mIsPlaying) {
				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.view.View.OnClickListener#onClick(android.view.View)
	 */

	@Override
	public void onClick(View view) {
		if (view.getId() == R.id.realplay_back_btn) {
			finish();
		} else if (view.getId() == R.id.realplay_stop_btn) {
			stopRealPlay(false);
		} else if (view.getId() == R.id.realplay_play_btn) {
			startRealPlay();
		} else if (view.getId() == R.id.realplay_play_iv) {
			startRealPlay();
		} else if (view.getId() == R.id.btncap1) {
			mRealPlayerHelper.capturePictureTask(mRealPlayMgr);

		}
	}

	/**
	 * 鎾斁绐楀彛浜嬩欢
	 * 
	 * @since V1.0
	 */
	private void onSurfaceViewClick() {
		if (mOrientation == Configuration.ORIENTATION_LANDSCAPE) {
			if (mTitleArea.getVisibility() == View.VISIBLE) {
				// 鏄剧ず绐楀彛
				mTitleArea.setVisibility(View.VISIBLE);
				mControlArea.setVisibility(View.VISIBLE);
			} else {
				mTitleArea.setVisibility(View.GONE);
				mControlArea.setVisibility(View.GONE);
			}
		}
	}

	/**
	 * screen鐘舵�佸箍鎾帴鏀惰��
	 */
	private class ScreenBroadcastReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
				stopRealPlay(false);
			}
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		// TODO Auto-generated method stub
	}

	private void showPlayFailDialog(String msg) {
		dismissDialog(mPlayFailDialog);
		mPlayFailDialog = null;
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(msg);
		builder.setPositiveButton(R.string.confirm, null);
		if (!isFinishing()) {
			mPlayFailDialog = builder.show();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.view.SurfaceHolder.Callback#surfaceCreated(android.view.SurfaceHolder
	 * )
	 */
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// 鑾峰彇surfaceView鍙幇瀹炲尯鍩熺殑澶у皬
		if (mCanDisplayRect.width() <= 0 || mCanDisplayRect.height() <= 0) {
			int left = 0;
			int right = mPlayArea.getWidth();
			int top = mTitleArea.getHeight();
			int bottom = mPlayArea.getHeight() - mControlArea.getHeight();
			mCanDisplayRect.set(left, top, right, bottom);
		}

		setPlayViewPosition();

		if (mRealPlayMgr != null) {
			// 璁剧疆鎾斁Surface
			mRealPlayMgr.setPlaySurface(holder);
		}
		mSurfaceHolder = holder;

		LogUtil.debugLog(TAG, "surfaceCreated");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.view.SurfaceHolder.Callback#surfaceDestroyed(android.view.
	 * SurfaceHolder)
	 */
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		LogUtil.debugLog(TAG, "surfaceDestroyed");
		if (mRealPlayMgr != null) {
			// 璁剧疆鎾斁Surface
			mRealPlayMgr.setPlaySurface(holder);
		}
		mSurfaceHolder = null;
	}

	private void dismissDialog(AlertDialog popDialog) {
		if (popDialog != null && popDialog.isShowing() && !isFinishing()) {
			try {
				popDialog.dismiss();
			} catch (Exception e) {
				// TODO: handle exception
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.videogo.ui.util.VerifySmsCodeUtil.OnVerifyListener#onVerify(int,
	 * int)
	 */
	@Override
	public void onVerify(int type, int result) {
		if (result == 0) {
			// 寮�濮嬫挱鏀�
			startRealPlay();
		}
	}

	public static Bitmap drawableToBitmap(Drawable drawable) {
		int width = drawable.getIntrinsicWidth();
		int height = drawable.getIntrinsicHeight();
		Bitmap bitmap = Bitmap.createBitmap(width, height, drawable
				.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888
				: Bitmap.Config.RGB_565);
		Canvas canvas = new Canvas(bitmap);
		drawable.setBounds(0, 0, width, height);
		drawable.draw(canvas);
		return bitmap;

	}

}

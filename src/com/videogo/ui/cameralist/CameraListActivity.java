/* 
 * @ProjectName VideoGoJar
 * @Copyright HangZhou Hikvision System Technology Co.,Ltd. All Right Reserved
 * 
 * @FileName CameraListActivity.java
 * @Description 这里对文件进行描述
 * 
 * @author chenxingyf1
 * @data 2014-7-14
 * 
 * @note 这里写本文件的详细功能描述和注释
 * @note 历史记录
 * 
 * @warning 这里写本文件的相关警告
 */
package com.videogo.ui.cameralist;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.AlertDialog.Builder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.ClipboardManager;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.RelativeLayout.LayoutParams;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.download.DecryptFileInfo;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.FailReason.FailType;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;
import com.nostra13.universalimageloader.core.listener.ImageLoadingProgressListener;
import com.videogo.R;
import com.videogo.constant.Constant;
import com.videogo.constant.IntentConsts;
import com.videogo.exception.BaseException;
import com.videogo.exception.ErrorCode;
import com.videogo.openapi.EzvizAPI;
import com.videogo.openapi.bean.req.GetCameraInfoList;
import com.videogo.openapi.bean.resp.CameraInfo;
import com.videogo.openapi.bean.resp.VideoInfo;
import com.videogo.scan.main.CaptureActivity;
import com.videogo.ui.devicelist.AutoWifiNetConfigActivity;
import com.videogo.ui.discovery.SquareColumnActivity;
import com.videogo.ui.discovery.VideoSquareActivity;
import com.videogo.ui.message.MessageActivity;
import com.videogo.ui.realplay.RealPlayActivity;
import com.videogo.ui.realplay.SimpleRealPlayActivity;
import com.videogo.ui.remoteplayback.RemotePlayBackActivity;
import com.videogo.ui.util.Constants;
import com.videogo.util.ConnectionDetector;
import com.videogo.util.DevPwdUtil;
import com.videogo.util.LogUtil;
import com.videogo.util.Utils;
import com.videogo.widget.MenuAdapter;
import com.videogo.widget.PullToRefreshFooter;
import com.videogo.widget.PullToRefreshHeader;
import com.videogo.widget.TitleBar;
import com.videogo.widget.TitleMenuItem;
import com.videogo.widget.WaitDialog;
import com.videogo.widget.PullToRefreshFooter.Style;
import com.videogo.widget.pulltorefresh.LoadingLayout;
import com.videogo.widget.pulltorefresh.PullToRefreshBase;
import com.videogo.widget.pulltorefresh.IPullToRefresh.Mode;
import com.videogo.widget.pulltorefresh.IPullToRefresh.OnRefreshListener;
import com.videogo.widget.pulltorefresh.PullToRefreshBase.LoadingLayoutCreator;
import com.videogo.widget.pulltorefresh.PullToRefreshBase.Orientation;
import com.videogo.widget.pulltorefresh.PullToRefreshListView;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 摄像头列表
 * @author chenxingyf1
 * @data 2014-7-14
 */
public class CameraListActivity extends Activity implements View.OnClickListener  {
    protected static final String TAG = "CameraListActivity";
    /** 删除设备 */
    private final static int SHOW_DIALOG_DEL_DEVICE = 1;
    
    private EzvizAPI mEzvizAPI = null;
    private BroadcastReceiver mReceiver = null;
    
    private TitleBar mTitleBar = null;
    private Button mAddButton;
    private PullToRefreshListView mListView = null;
    private View mNoMoreView;
    private CameraListAdapter mAdapter = null;
    private CameraInfo mCameraInfo = null;
    
    private LinearLayout mNoCameraTipLy = null;
    private LinearLayout mGetCameraFailTipLy = null;
    private TextView mCameraFailTipTv = null;
    private boolean mHasShowInputDialog;
    
    private List<TitleMenuItem> mTitleMenuItems = null;
    private PopupWindow mPopupWindow;
    private MenuAdapter mMenuAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cameralist_page);
        
        initData();
        initView();        
        Utils.clearAllNotification(this);
    }
    
    private void initView() {
        mTitleBar = (TitleBar) findViewById(R.id.title_bar);
        mTitleBar.setTitle(R.string.cameras_txt);
        initPopupMenu();
        mAddButton = mTitleBar.addRightButton(R.drawable.my_add, new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (mPopupWindow.isShowing())
                    mPopupWindow.dismiss();
                else
                    mPopupWindow.showAsDropDown(mAddButton, -Utils.dip2px(CameraListActivity.this, 95), 0);
            }
        });
        
        mTitleBar.addBackButton(new OnClickListener() {

            @Override
            public void onClick(View v) {
                popLogoutDialog();
            }
        });
        
        mNoMoreView = getLayoutInflater().inflate(R.layout.no_device_more_footer, null);
        
        mAdapter = new CameraListAdapter(this);
        mAdapter.setOnClickListener(new CameraListAdapter.OnClickListener() {

            @Override
            public void onPlayClick(BaseAdapter adapter, View view, int position) {
                CameraInfo cameraInfo = mAdapter.getItem(position);
                // Intent intent = new Intent(CameraListActivity.this, RealPlayActivity.class);
                Intent intent = new Intent(CameraListActivity.this, SimpleRealPlayActivity.class);
                intent.putExtra(IntentConsts.EXTRA_CAMERA_INFO, cameraInfo);
                startActivity(intent);
            }

            @Override
            public void onRemotePlayBackClick(BaseAdapter adapter, View view, int position) {
                CameraInfo cameraInfo = mAdapter.getItem(position);
                Intent intent = new Intent(CameraListActivity.this, RemotePlayBackActivity.class);
                intent.putExtra(IntentConsts.EXTRA_CAMERA_INFO, cameraInfo);
                startActivity(intent);
            }

            @Override
            public void onSetDeviceClick(BaseAdapter adapter, View view, int position) {
                CameraInfo cameraInfo = mAdapter.getItem(position);
                mEzvizAPI.gotoSetDevicePage(cameraInfo.getDeviceId(), cameraInfo.getCameraId());
            }

            @Override
            public void onDeleteClick(BaseAdapter adapter, View view, int position) {
                mCameraInfo = mAdapter.getItem(position);
                showDialog(SHOW_DIALOG_DEL_DEVICE);
            }

            @Override
            public void onAlarmListClick(BaseAdapter adapter, View view, int position) {
                CameraInfo cameraInfo = mAdapter.getItem(position);
                Intent intent = new Intent(CameraListActivity.this, MessageActivity.class);
                intent.putExtra(IntentConsts.EXTRA_CAMERA_INFO, cameraInfo);
                startActivity(intent);
            }

            @Override
            public void onDevicePictureClick(BaseAdapter adapter, View view, int position) {
                final CameraInfo cameraInfo = mAdapter.getItem(position);
                final EditText editText = new EditText(CameraListActivity.this);
                editText.setText("7069370639e8495e9f04d2453e53cb27");//7fda76bd1c1d4b8c9b286921ff7e7651
                new  AlertDialog.Builder(CameraListActivity.this)  
                .setTitle(R.string.input_device_picture_uuid)   
                .setIcon(android.R.drawable.ic_dialog_info)   
                .setView(editText)  
                .setPositiveButton(R.string.certain, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final EditText editText = new EditText(CameraListActivity.this);
                        editText.setText("640");
                        new  AlertDialog.Builder(CameraListActivity.this)  
                        .setTitle(R.string.input_device_picture_size)   
                        .setIcon(android.R.drawable.ic_dialog_info)   
                        .setView(editText)  
                        .setPositiveButton(R.string.certain, new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                new GetDevicePictureTask(cameraInfo, editText.getText().toString()).execute();
                            }
                            
                        })   
                        .setNegativeButton(R.string.cancel, null)
                        .show();  
                    }
                    
                })   
                .setNegativeButton(R.string.cancel, null)
                .show();  
            }

            @Override
            public void onDeviceVideoClick(BaseAdapter adapter, View view, int position) {
                final CameraInfo cameraInfo = mAdapter.getItem(position);
                final EditText editText = new EditText(CameraListActivity.this);
                editText.setText("f8a95ad7e69143038fa7233df6339094");//7fda76bd1c1d4b8c9b286921ff7e7651
                new  AlertDialog.Builder(CameraListActivity.this)  
                .setTitle(R.string.input_device_video_uuid)   
                .setIcon(android.R.drawable.ic_dialog_info)   
                .setView(editText)  
                .setPositiveButton(R.string.certain, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new GetDeviceVideoTask(cameraInfo, editText.getText().toString()).execute();
                    }
                    
                })   
                .setNegativeButton(R.string.cancel, null)
                .show(); 
            }
            
        });
        mListView = (PullToRefreshListView) findViewById(R.id.camera_listview);
        mListView.setLoadingLayoutCreator(new LoadingLayoutCreator() {

            @Override
            public LoadingLayout create(Context context, boolean headerOrFooter, Orientation orientation) {
                if (headerOrFooter)
                    return new PullToRefreshHeader(context);
                else
                    return new PullToRefreshFooter(context, Style.EMPTY_NO_MORE);
            }
        });
        mListView.setMode(Mode.BOTH);
        mListView.setOnRefreshListener(new OnRefreshListener<ListView>() {

            @Override
            public void onRefresh(PullToRefreshBase<ListView> refreshView, boolean headerOrFooter) {
                getCameraInfoList(headerOrFooter);
            }
        });                
        mListView.getRefreshableView().addFooterView(mNoMoreView);
        mListView.setAdapter(mAdapter);
        mListView.getRefreshableView().removeFooterView(mNoMoreView);
        
        mNoCameraTipLy = (LinearLayout) findViewById(R.id.no_camera_tip_ly);
        mGetCameraFailTipLy = (LinearLayout) findViewById(R.id.get_camera_fail_tip_ly);
        mCameraFailTipTv = (TextView) findViewById(R.id.get_camera_list_fail_tv);
    }
    
    private void initData() {
        mEzvizAPI = EzvizAPI.getInstance();
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                LogUtil.debugLog(TAG, "onReceive:" + action);
                if (action.equals(Constant.ADD_DEVICE_SUCCESS_ACTION)) {
                    refreshButtonClicked();
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constant.ADD_DEVICE_SUCCESS_ACTION);
        registerReceiver(mReceiver, filter);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if(mAdapter != null && mAdapter.getCount() == 0) {
            refreshButtonClicked();
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        if(mAdapter != null) {
            mAdapter.shutDownExecutorService();
            mAdapter.clearImageCache();
        }
    }
    
    /**
     * 从服务器获取最新事件消息
     */
    private void getCameraInfoList(boolean headerOrFooter) {
        if(this.isFinishing()) {
            return;
        }
        new GetCamersInfoListTask(headerOrFooter).execute();
    }
    
    /**
     * 获取事件消息任务
     */
    private class GetCamersInfoListTask extends AsyncTask<Void, Void, List<CameraInfo>> {
        private boolean mHeaderOrFooter;
        private int mErrorCode = 0;

        public GetCamersInfoListTask(boolean headerOrFooter) {
            mHeaderOrFooter = headerOrFooter;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mListView.setFooterRefreshEnabled(true);
            mListView.getRefreshableView().removeFooterView(mNoMoreView);
        }

        @Override
        protected List<CameraInfo> doInBackground(Void... params) {
            if(CameraListActivity.this.isFinishing()) {
                return null;
            }
            if (!ConnectionDetector.isNetworkAvailable(CameraListActivity.this)) {
                mErrorCode = ErrorCode.ERROR_WEB_NET_EXCEPTION;
                return null;
            }

            try {
                GetCameraInfoList getCameraInfoList = new GetCameraInfoList();
                getCameraInfoList.setPageSize(10);
                if(mHeaderOrFooter) {
                    getCameraInfoList.setPageStart(0);
                } else {
                    getCameraInfoList.setPageStart(mAdapter.getCount()/10);
                }
                List<CameraInfo> result = mEzvizAPI.getCameraInfoList(getCameraInfoList);
                return result;

            } catch (BaseException e) {
                mErrorCode = e.getErrorCode();
                return null;
            }
        }

        @Override
        protected void onPostExecute(List<CameraInfo> result) {
            super.onPostExecute(result);
            mListView.onRefreshComplete();
            if(CameraListActivity.this.isFinishing()) {
                return;
            }
            
            if (result != null) {               
                if (mHeaderOrFooter) {
                    CharSequence dateText = DateFormat.format("yyyy-MM-dd kk:mm:ss", new Date());
                    for (LoadingLayout layout : mListView.getLoadingLayoutProxy(true, false).getLayouts()) {
                        ((PullToRefreshHeader) layout).setLastRefreshTime(":" + dateText);
                    }
                    mAdapter.clearItem();
                }
                if(mAdapter.getCount() == 0 && result.size() == 0) {
                    mListView.setVisibility(View.GONE);
                    mNoCameraTipLy.setVisibility(View.VISIBLE);
                    mGetCameraFailTipLy.setVisibility(View.GONE);
                    mListView.getRefreshableView().removeFooterView(mNoMoreView);
                 } else if(result.size() < 10){
                    mListView.setFooterRefreshEnabled(false);
                    mListView.getRefreshableView().addFooterView(mNoMoreView);
                } else if(mHeaderOrFooter) {
                    mListView.setFooterRefreshEnabled(true);
                    mListView.getRefreshableView().removeFooterView(mNoMoreView);
                }
                addCameraList(result);
                mAdapter.notifyDataSetChanged();
            }

            if (mErrorCode != 0) {
                onError(mErrorCode);
            }
        }

        protected void onError(int errorCode) {
            switch (errorCode) {
                case ErrorCode.ERROR_WEB_SESSION_ERROR:
                case ErrorCode.ERROR_WEB_SESSION_EXPIRE:
                case ErrorCode.ERROR_WEB_HARDWARE_SIGNATURE_ERROR:
                    mEzvizAPI.gotoLoginPage();
                    break;
                default:
                    if(mAdapter.getCount() == 0) {
                        mListView.setVisibility(View.GONE);
                        mNoCameraTipLy.setVisibility(View.GONE);
                        mCameraFailTipTv.setText(Utils.getErrorTip(CameraListActivity.this, R.string.get_camera_list_fail,  errorCode));
                        mGetCameraFailTipLy.setVisibility(View.VISIBLE);
                    } else {
                        Utils.showToast(CameraListActivity.this, R.string.get_camera_list_fail,  errorCode);
                    }
                    break;
            }
        }
    }
    
    /**
     * 删除事件消息任务
     */
    private class DeleteDeviceTask extends AsyncTask<CameraInfo, Void, CameraInfo> {

        private Dialog mWaitDialog;
        private int mErrorCode = 0;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mWaitDialog = new WaitDialog(CameraListActivity.this, android.R.style.Theme_Translucent_NoTitleBar);
            mWaitDialog.setCancelable(false);
            mWaitDialog.show();
        }

        @Override
        protected CameraInfo doInBackground(CameraInfo... params) {
            if (!ConnectionDetector.isNetworkAvailable(CameraListActivity.this)) {
                mErrorCode = ErrorCode.ERROR_WEB_NET_EXCEPTION;
                return null;
            }

            try {
                CameraInfo cameraInfo = params[0];
                mEzvizAPI.deleteDevice(cameraInfo.getDeviceId());
                return cameraInfo;

            } catch (BaseException e) {
                mErrorCode = e.getErrorCode();
                return null;
            }
        }

        @Override
        protected void onPostExecute(CameraInfo result) {
            super.onPostExecute(result);
            mWaitDialog.dismiss();

            if (result != null) {
                mAdapter.removeItem(result);
                List<CameraInfo> cameraInfoList = new ArrayList<CameraInfo>();
                CameraInfo cameraInfo = null;
                for(int i = 0; i < mAdapter.getCount(); i++) {
                    cameraInfo = mAdapter.getItem(i);
                    if(cameraInfo.getDeviceId() != null && result.getDeviceId().equalsIgnoreCase(cameraInfo.getDeviceId())) {
                        cameraInfoList.add(cameraInfo);
                    }
                }
                for(int i = 0; i < cameraInfoList.size(); i++) {
                    cameraInfo = cameraInfoList.get(i);
                    mAdapter.removeItem(cameraInfo);
                }

                // 如果删除到最后会重新获取的
                if (mAdapter.getCount() == 0) {
                    mListView.setRefreshing();
                }
                mAdapter.notifyDataSetChanged();
                Utils.showToast(CameraListActivity.this, getString(R.string.detail_del_device_success));
            }

            if (mErrorCode != 0)
                onError(mErrorCode);
        }

        protected void onError(int errorCode) {
            switch (errorCode) {
                case ErrorCode.ERROR_WEB_SESSION_ERROR:
                case ErrorCode.ERROR_WEB_SESSION_EXPIRE:
                case ErrorCode.ERROR_WEB_HARDWARE_SIGNATURE_ERROR:
                    mEzvizAPI.gotoLoginPage();
                    break;

                case ErrorCode.ERROR_WEB_NET_EXCEPTION:
                    Utils.showToast(CameraListActivity.this, R.string.alarm_message_del_fail_network_exception);
                    break;
                default:
                    Utils.showToast(CameraListActivity.this, R.string.alarm_message_del_fail_txt, mErrorCode);
                    break;
            }
        }
    }
    
    private class GetDevicePictureTask extends AsyncTask<Void, Void, String> {
        private CameraInfo mCameraInfo;
        private String mUuid;
        private Dialog mWaitDialog;
        
        public GetDevicePictureTask(CameraInfo cameraInfo, String uuid) {
            mCameraInfo = cameraInfo;
            mUuid = uuid;
        }
        
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mWaitDialog = new WaitDialog(CameraListActivity.this, android.R.style.Theme_Translucent_NoTitleBar);
            mWaitDialog.setCancelable(false);
            mWaitDialog.show();
        }

        @Override
        protected String doInBackground(Void... params) {
            if (!ConnectionDetector.isNetworkAvailable(CameraListActivity.this)) {
                return null;
            }

            try {
                return EzvizAPI.getInstance().getDevicePicture(mUuid, 1280);
            } catch (BaseException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(final String picUrl) {
            super.onPostExecute(picUrl);
            
            if (TextUtils.isEmpty(picUrl)) {
                mWaitDialog.dismiss();
                Utils.showToast(CameraListActivity.this, R.string.get_device_picture_fail);
            } else {
                displayMessageImage(picUrl);
            }
        }
        
        private void displayMessageImage(final String picUrl) {
            String checkSum = Utils.getUrlValue(picUrl, "checkSum=", "&");
            DisplayImageOptions options = new DisplayImageOptions.Builder()
                    .cacheInMemory(false).cacheOnDisk(true)
                    .needDecrypt(!TextUtils.isEmpty(checkSum)).considerExifParams(true)
                    .extraForDownloader(new DecryptFileInfo(mCameraInfo.getDeviceId(), checkSum)).build();
            
            final ImageView img =  new ImageView(CameraListActivity.this); 
            ImageLoader.getInstance().displayImage(picUrl, img, options,
                    new ImageLoadingListener() {

                        @Override
                        public void onLoadingStarted(String imageUri, View view) {
                        }

                        @Override
                        public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
                            mWaitDialog.dismiss();
                            if (failReason.getType() == FailType.DECRYPT_ERROR) {
                                if (mHasShowInputDialog) {
                                    new AlertDialog.Builder(CameraListActivity.this)
                                            .setMessage(R.string.common_passwd_error)
                                            .setPositiveButton(R.string.cancel, null)
                                            .setNegativeButton(R.string.retry, new DialogInterface.OnClickListener() {

                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    showInputSafePassword(picUrl);
                                                }
                                            }).show();
                                } else {
                                    showInputSafePassword(picUrl);
                                }
                            } else {
                                Utils.showToast(CameraListActivity.this, R.string.get_device_picture_fail);
                            }
                        }

                        @Override
                        public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                            mWaitDialog.dismiss();
                            new  AlertDialog.Builder(CameraListActivity.this)  
                            .setTitle(R.string.device_picture)   
                            .setView(img)   
                            .setPositiveButton(R.string.certain,  null)   
                            .show();  
                        }

                        @Override
                        public void onLoadingCancelled(String imageUri, View view) {
                            mWaitDialog.dismiss();
                        }

                    }, new ImageLoadingProgressListener() {

                        @Override
                        public void onProgressUpdate(String imageUri, View view, int current, int total) {
                        }
                    });                
        }
        
        // 处理密码错误
        private void showInputSafePassword(final String picUrl) {
            mHasShowInputDialog = true;
            // 从布局中加载视图
            LayoutInflater factory = LayoutInflater.from(CameraListActivity.this);
            final View passwordErrorLayout = factory.inflate(R.layout.password_error_layout, null);
            final EditText passwordInput = (EditText) passwordErrorLayout.findViewById(R.id.new_password);
            passwordInput.setFilters(new InputFilter[] {new InputFilter.LengthFilter(Constant.PSW_MAX_LENGTH)});

            final TextView message1 = (TextView) passwordErrorLayout.findViewById(R.id.message1);
            final TextView message2 = (TextView) passwordErrorLayout.findViewById(R.id.message2);
            message1.setText(R.string.message_encrypt_inputpsw_tip_title);
            message2.setVisibility(View.GONE);

            // 使用布局中的视图创建AlertDialog
            new AlertDialog.Builder(CameraListActivity.this).setView(passwordErrorLayout)
                    .setTitle(R.string.realplay_encrypt_password_error_title)
                    .setPositiveButton(R.string.cancel, null)
                    .setNegativeButton(R.string.confirm, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // 使用新密码
                            String newPassword = passwordInput.getText().toString();
                            DevPwdUtil.savePwd(mCameraInfo.getDeviceId(), newPassword);

                            displayMessageImage(picUrl);
                        }

                    }).show();

            InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.showSoftInput(passwordInput, 0);
        }        
    }
    
    private class GetDeviceVideoTask extends AsyncTask<Void, Void, VideoInfo> {
        private CameraInfo mCameraInfo;
        private String mUuid;
        private Dialog mWaitDialog;
        
        public GetDeviceVideoTask(CameraInfo cameraInfo, String uuid) {
            mCameraInfo = cameraInfo;
            mUuid = uuid;
        }
        
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mWaitDialog = new WaitDialog(CameraListActivity.this, android.R.style.Theme_Translucent_NoTitleBar);
            mWaitDialog.setCancelable(false);
            mWaitDialog.show();
        }

        @Override
        protected VideoInfo doInBackground(Void... params) {
            if (!ConnectionDetector.isNetworkAvailable(CameraListActivity.this)) {
                return null;
            }

            try {
                return EzvizAPI.getInstance().getDeviceVideoInfo(mUuid);
            } catch (BaseException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(VideoInfo videoInfo) {
            super.onPostExecute(videoInfo);
            mWaitDialog.dismiss();

            if (videoInfo != null) {
                Intent intent = new Intent(CameraListActivity.this, RemotePlayBackActivity.class);
                intent.putExtra(IntentConsts.EXTRA_CAMERA_INFO, mCameraInfo);
                intent.putExtra(IntentConsts.EXTRA_ALARM_TIME, videoInfo.getStartTime());
                startActivity(intent);                
            } else {
                Utils.showToast(CameraListActivity.this, R.string.get_device_picture_fail);
            }
        }
    }
    
    private void addCameraList(List<CameraInfo> cameraInfoList) {
        int count = cameraInfoList.size();
        CameraInfo item = null;
        for (int i = 0; i < count; i++) {
           item = cameraInfoList.get(i);
           mAdapter.addItem(item);
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        if (mReceiver != null) {
            unregisterReceiver(mReceiver);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.camera_list_refresh_btn:
            case R.id.no_camera_tip_ly:
                refreshButtonClicked();
                break;
            case R.id.camera_list_gc_ly:
                Intent intent = new Intent(CameraListActivity.this, SquareColumnActivity.class);
                startActivity(intent);
                break;
            default:
                break;
        }
    }
    
    /**
     * 刷新点击
     */
    private void refreshButtonClicked() {
        mListView.setVisibility(View.VISIBLE);
        mNoCameraTipLy.setVisibility(View.GONE);
        mGetCameraFailTipLy.setVisibility(View.GONE);
        mListView.setRefreshing();
    }
    
    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog = null;
        switch (id) {
            case SHOW_DIALOG_DEL_DEVICE:
                dialog = new AlertDialog.Builder(this).setMessage(getString(R.string.detail_del_device_btn_tip))
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        }).setPositiveButton(R.string.certain, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if(mCameraInfo != null) {
                                    new DeleteDeviceTask().execute(mCameraInfo);
                                    mCameraInfo = null;
                                }
                            }
                        }).create();
                break;
        }
        return dialog;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 1, 1, R.string.update_exit).setIcon(R.drawable.exit_selector);
        return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        if (dialog != null) {
            removeDialog(id);
            TextView tv = (TextView) dialog.findViewById(android.R.id.message);
            tv.setGravity(Gravity.CENTER);
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {// 得到被点击的item的itemId
            case 1:// 对应的ID就是在add方法中所设定的Id
                popLogoutDialog();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }
    
    /**
     * 弹出登出对话框
     * 
     * @see
     * @since V1.0
     */
    private void popLogoutDialog() {
        Builder exitDialog = new AlertDialog.Builder(CameraListActivity.this);
        exitDialog.setTitle(R.string.exit);
        exitDialog.setMessage(R.string.exit_tip);
        exitDialog.setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                new LogoutTask().execute();
            }
        });
        exitDialog.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        exitDialog.show();
    }
    
    private class LogoutTask extends AsyncTask<Void, Void, Void> {
        private Dialog mWaitDialog;
        
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mWaitDialog = new WaitDialog(CameraListActivity.this, android.R.style.Theme_Translucent_NoTitleBar);
            mWaitDialog.setCancelable(false);
            mWaitDialog.show();
        }

        @Override
        protected Void doInBackground(Void... params) {
            mEzvizAPI.logout();
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            mWaitDialog.dismiss();
            mEzvizAPI.gotoLoginPage(false);
            finish();
        }
    }
    
    private void createDefaultTitleMenu() {
        if (mTitleMenuItems == null) {
            mTitleMenuItems = new ArrayList<TitleMenuItem>();
            mTitleMenuItems.add(new TitleMenuItem(Constants.ADD_DEVICE_SCAN, null, null, getString(R.string.add_device_scan)));
            mTitleMenuItems.add(new TitleMenuItem(Constants.ADD_DEVICE_DISCOVER, null, null, getString(R.string.add_device_discover)));
            mTitleMenuItems.add(new TitleMenuItem(Constants.ADD_DEVICE_INPUT, null, null, getString(R.string.add_device_input)));
        }
    }
    
    private void initPopupMenu() {
        createDefaultTitleMenu();
        ListView menuListView = new ListView(this);
        menuListView.setBackgroundResource(R.drawable.title_menu_bg);
        menuListView.setDivider(new ColorDrawable(0xff414141));
        menuListView.setDividerHeight(Utils.dip2px(this, 0.5f));
        menuListView.setSelector(new ColorDrawable(Color.TRANSPARENT));
        menuListView.setPadding(0, Utils.dip2px(this, 7.5f), 0, 0);

        mPopupWindow = new PopupWindow(menuListView, Utils.dip2px(this, 150), LayoutParams.WRAP_CONTENT);
        mPopupWindow.setTouchable(true);
        mPopupWindow.setOutsideTouchable(true);
        mPopupWindow.setFocusable(true);
        mPopupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        mPopupWindow.setTouchInterceptor(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
                    mPopupWindow.dismiss();
                    return true;
                }
                return false;
            }
        });

        mMenuAdapter = new MenuAdapter(this, mTitleMenuItems);
        menuListView.setAdapter(mMenuAdapter);
        menuListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                TitleMenuItem menuItem = (TitleMenuItem) parent.getItemAtPosition(position);
                if (Constants.ADD_DEVICE_SCAN.equals(menuItem.getType())) {
                    Intent intent = new Intent(CameraListActivity.this, CaptureActivity.class);
                    startActivity(intent);
                } else if (Constants.ADD_DEVICE_DISCOVER.equals(menuItem.getType())) {
                    Intent intent = new Intent(CameraListActivity.this, AutoWifiNetConfigActivity.class);
                    startActivity(intent);
                } else if (Constants.ADD_DEVICE_INPUT.equals(menuItem.getType())) {
                    mEzvizAPI.gotoAddDevicePage();
                }
                mPopupWindow.dismiss();
            }
        });
    }
}

package com.videogo.ui.message;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.DatePicker;
import android.widget.ListView;
import android.widget.TextView;

import com.videogo.R;
import com.videogo.alarm.AlarmLogInfoManager;
import com.videogo.constant.IntentConsts;
import com.videogo.exception.BaseException;
import com.videogo.exception.ErrorCode;
import com.videogo.openapi.EzvizAPI;
import com.videogo.openapi.bean.req.GetAlarmInfoList;
import com.videogo.openapi.bean.resp.AlarmInfo;
import com.videogo.openapi.bean.resp.CameraInfo;
import com.videogo.ui.androidpn.NotifierUtils;
import com.videogo.ui.realplay.RealPlayActivity;
import com.videogo.ui.remoteplayback.RemotePlayBackActivity;
import com.videogo.util.ConnectionDetector;
import com.videogo.util.LogUtil;
import com.videogo.util.Utils;
import com.videogo.widget.PullToRefreshFooter;
import com.videogo.widget.PullToRefreshFooter.Style;
import com.videogo.widget.PullToRefreshHeader;
import com.videogo.widget.TitleBar;
import com.videogo.widget.WaitDialog;
import com.videogo.widget.pulltorefresh.IPullToRefresh.Mode;
import com.videogo.widget.pulltorefresh.IPullToRefresh.OnRefreshListener;
import com.videogo.widget.pulltorefresh.LoadingLayout;
import com.videogo.widget.pulltorefresh.PullToRefreshBase;
import com.videogo.widget.pulltorefresh.PullToRefreshBase.LoadingLayoutCreator;
import com.videogo.widget.pulltorefresh.PullToRefreshBase.Orientation;
import com.videogo.widget.pulltorefresh.PullToRefreshListView;

public class MessageActivity extends Activity {
    /** 标题栏 */
    private TitleBar mTitleBar;
    /** 消息列表 */
    private PullToRefreshListView mMessageListView;
    /** 没有消息的布局 */
    private ViewGroup mNoMessageLayout;
    /** 加载失败的时候的刷新布局 */
    private ViewGroup mRefreshLayout;
    /** 加载失败的时候的刷新按钮 */
    private ViewGroup mRefreshButton;
    /** 加载失败的时候的刷新按钮上面的提示 */
    private TextView mRefreshTipView;
    /** 没有更多 */
    private View mNoMoreView;

    /** 消息管理 */
    private MessageListAdapter mAdapter;
    /** 消息 */
    private List<AlarmInfo> mMessageList;
    
    private CameraInfo mCameraInfo = null;
    private Calendar mStartTime = null;
    private Calendar mEndTime = null;
    private EzvizAPI mEzvizAPI = null;
    private int mPushFlag = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.message_page);
        
        NotifierUtils.clearAllNotification(this);
        
        findViews();
        initData();
        initTitleBar();
        initViews();
        setListner();
    }
    
    /**
     * 控件关联
     */
    private void findViews() {
        mTitleBar = (TitleBar) findViewById(R.id.title_bar);
        mMessageListView = (PullToRefreshListView) findViewById(R.id.message_list);
        mNoMessageLayout = (ViewGroup) findViewById(R.id.no_message_layout);
        mRefreshLayout = (ViewGroup) findViewById(R.id.refresh_layout);
        mRefreshButton = (ViewGroup) findViewById(R.id.refresh_button);
        mRefreshTipView = (TextView) findViewById(R.id.refresh_tip);
    }

    /**
     * 初始化数据
     */
    private void initData() {
        Intent intent = getIntent();
        mPushFlag = intent.getIntExtra(IntentConsts.EXTRA_PUSH, 0);
        mCameraInfo = (CameraInfo)intent.getParcelableExtra(IntentConsts.EXTRA_CAMERA_INFO); 
        mStartTime = Calendar.getInstance();
        mStartTime.set(Calendar.AM_PM, 0);
        mStartTime.set(mStartTime.get(Calendar.YEAR), mStartTime.get(Calendar.MONTH), 
                mStartTime.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
        mEndTime = Calendar.getInstance();
        mEndTime.set(Calendar.AM_PM, 0);
        mEndTime.set(mEndTime.get(Calendar.YEAR), mEndTime.get(Calendar.MONTH), 
                mEndTime.get(Calendar.DAY_OF_MONTH), 23, 59, 59);
        mEzvizAPI = EzvizAPI.getInstance();
        if(mPushFlag != 0) {
            mMessageList = AlarmLogInfoManager.getInstance().getAlarmInfoListFromPush(this, mCameraInfo.getCameraId(), mPushFlag==1);
        } else {
            mMessageList = new ArrayList<AlarmInfo>();
        }
        mAdapter = new MessageListAdapter(this, mMessageList);
    }
    
    /**
     * 初始化标题栏
     */
    private void initTitleBar() {
        mTitleBar.addBackButton(new OnClickListener() {

            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        if(mPushFlag != 0) {
            mTitleBar.setTitle(R.string.push_out_event_alarm_title);
        } else {
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

                        mStartTime = Calendar.getInstance();
                        mStartTime.set(Calendar.AM_PM, 0);
                        mStartTime.set(dp.getYear(), dp.getMonth(), dp.getDayOfMonth(), 0, 0, 0);
                        mEndTime = Calendar.getInstance();
                        mEndTime.set(Calendar.AM_PM, 0);
                        mEndTime.set(dp.getYear(), dp.getMonth(), dp.getDayOfMonth(), 23, 59, 59);
                        mTitleBar.setTitle(Utils.date2String(mStartTime.getTime()));
                        
                        refreshButtonClicked();
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
    
    /**
     * 初始化控件
     */
    private void initViews() {
        mNoMoreView = getLayoutInflater().inflate(R.layout.no_msg_more_footer, null);
        ((TextView) mNoMoreView.findViewById(R.id.no_more_hint)).setText(R.string.no_more_alarm_tip);

        mMessageListView.setLoadingLayoutCreator(new LoadingLayoutCreator() {

            @Override
            public LoadingLayout create(Context context, boolean headerOrFooter, Orientation orientation) {
                if (headerOrFooter)
                    return new PullToRefreshHeader(context);
                else
                    return new PullToRefreshFooter(context, Style.EMPTY_NO_MORE);
            }
        });
        mMessageListView.setMode(mPushFlag!=0?Mode.DISABLED:Mode.BOTH);
        mMessageListView.setOnRefreshListener(new OnRefreshListener<ListView>() {

            @Override
            public void onRefresh(PullToRefreshBase<ListView> refreshView, boolean headerOrFooter) {
                getAlarmMessageList(true, headerOrFooter);
            }
        });
        getAlarmMessageList(false, true);
        mMessageListView.setAdapter(mAdapter);
    }

    /**
     * 设置监听
     */
    private void setListner() {
        OnClickListener clickListener = new OnClickListener() {

            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.refresh_button:
                        refreshButtonClicked();
                        break;

                    case R.id.no_message_layout:
                        refreshButtonClicked();
                        break;
                }
            }
        };

        mRefreshButton.setOnClickListener(clickListener);
        mNoMessageLayout.setOnClickListener(clickListener);

        mAdapter.setOnClickListener(new MessageListAdapter.OnClickListener() {

            @Override
            public void onVideoButtonClick(BaseAdapter adapter, View view, int position) {
                AlarmInfo alarmInfo = (AlarmInfo) adapter.getItem(position);
                
                Intent intent = new Intent(MessageActivity.this, RemotePlayBackActivity.class);
                intent.putExtra(IntentConsts.EXTRA_CAMERA_INFO, mCameraInfo);
                intent.putExtra(IntentConsts.EXTRA_ALARM_TIME, alarmInfo.getAlarmStart());
                startActivity(intent);
            }

            @Override
            public void onPlayButtonClick(BaseAdapter adapter, View view, int position) {
                Intent intent = new Intent(MessageActivity.this, RealPlayActivity.class);
                intent.putExtra(IntentConsts.EXTRA_CAMERA_INFO, mCameraInfo);
                startActivity(intent);
            }

            @Override
            public void onItemClick(BaseAdapter adapter, View view, int position) {
                AlarmInfo alarmInfo = (AlarmInfo) adapter.getItem(position);
                
                new SetReadTask().execute(alarmInfo.getAlarmId());
            }

            @Override
            public void onImageClick(BaseAdapter adapter, View view, int position) {
                // TODO Auto-generated method stub
                
            }
        });
    }

    private class SetReadTask extends AsyncTask<String, Void, Boolean> {
        private Dialog mWaitDialog;
        
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mWaitDialog = new WaitDialog(MessageActivity.this, android.R.style.Theme_Translucent_NoTitleBar);
            mWaitDialog.setCancelable(false);
            mWaitDialog.show();
        }

        @Override
        protected Boolean doInBackground(String... params) {
            try {
                return mEzvizAPI.setAlarmRead(params[0]);
            } catch (BaseException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            mWaitDialog.dismiss();
        }
    }
    
    /**
     * 刷新点击
     */
    private void refreshButtonClicked() {
        mNoMessageLayout.setVisibility(View.GONE);
        mRefreshLayout.setVisibility(View.GONE);
        getAlarmMessageList(false, true);
    }

    /**
     * 从服务器获取最新事件消息
     */
    private void getAlarmMessageList(boolean pullOrClick, boolean headerOrFooter) {
        if (pullOrClick) {
            String lastTime = "";
            if (!headerOrFooter) {
                if (mMessageList != null && mMessageList.size() > 0)
                    lastTime = mMessageList.get(mMessageList.size() - 1).getAlarmStart();
            }
            new GetAlarmMessageTask(headerOrFooter).execute(lastTime);
        } else {
            mMessageListView.setRefreshing();
        }
    }
    
    /**
     * 获取事件消息任务
     */
    private class GetAlarmMessageTask extends AsyncTask<String, Void, List<AlarmInfo>> {
        private boolean mHeaderOrFooter;
        private int mErrorCode = 0;

        public GetAlarmMessageTask(boolean headerOrFooter) {
            mHeaderOrFooter = headerOrFooter;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected List<AlarmInfo> doInBackground(String... params) {
            if (!ConnectionDetector.isNetworkAvailable(MessageActivity.this)) {
                mErrorCode = ErrorCode.ERROR_WEB_NET_EXCEPTION;
                return null;
            }

            try {
                GetAlarmInfoList getAlarmInfoList = new GetAlarmInfoList();
                getAlarmInfoList.setCameraId(mCameraInfo.getCameraId());
                getAlarmInfoList.setStartTime(Utils.calendar2String(mStartTime));
                getAlarmInfoList.setEndTime(Utils.calendar2String(mEndTime));
                getAlarmInfoList.setStatus(2);
                getAlarmInfoList.setAlarmType(-1);
                
                getAlarmInfoList.setPageSize(10);
                if(mHeaderOrFooter) {
                    getAlarmInfoList.setPageStart(0);
                } else {
                    getAlarmInfoList.setPageStart(mMessageList.size()/10);
                }
                return mEzvizAPI.getAlarmInfoList(getAlarmInfoList);

            } catch (BaseException e) {
                mErrorCode = e.getErrorCode();
                return null;
            }
        }

        @Override
        protected void onPostExecute(List<AlarmInfo> result) {
            super.onPostExecute(result);
            mMessageListView.onRefreshComplete();

            if (result != null) {
                if (mHeaderOrFooter) {
                    CharSequence dateText = DateFormat.format("yyyy-MM-dd kk:mm:ss", new Date());
                    for (LoadingLayout layout : mMessageListView.getLoadingLayoutProxy(true, false).getLayouts()) {
                        ((PullToRefreshHeader) layout).setLastRefreshTime(":" + dateText);
                    }
                    mMessageList.clear();
                    mMessageListView.getRefreshableView().removeFooterView(mNoMoreView);
                }
                if (mMessageList.size() == 0 && result.size() == 0) {
                    mNoMessageLayout.setVisibility(View.VISIBLE);
                    mRefreshLayout.setVisibility(View.GONE);
                    mNoMessageLayout.setVisibility(View.VISIBLE);
                } else if(result.size() < 10) {
                    mNoMessageLayout.setVisibility(View.GONE);
                    mRefreshLayout.setVisibility(View.GONE);
                    mMessageListView.setFooterRefreshEnabled(false);
                    mMessageListView.getRefreshableView().addFooterView(mNoMoreView);                        
                } else if(mHeaderOrFooter) {
                    mMessageListView.setFooterRefreshEnabled(true);
                }
                mMessageList.addAll(result);
                
                mAdapter.notifyDataSetChanged();
            }

            if (mErrorCode != 0) {
                onError(mErrorCode);
            }
        }

        protected void onError(int errorCode) {
            switch (errorCode) {
                case ErrorCode.ERROR_WEB_PARAM_ERROR:
                case ErrorCode.ERROR_WEB_SESSION_ERROR:
                case ErrorCode.ERROR_WEB_SESSION_EXPIRE:
                case ErrorCode.ERROR_WEB_HARDWARE_SIGNATURE_ERROR:
                    mEzvizAPI.gotoLoginPage();
                    break;

                case ErrorCode.ERROR_WEB_SERVER_EXCEPTION:
                    showError(getText(R.string.message_refresh_fail_server_exception));
                    break;

                case ErrorCode.ERROR_WEB_NET_EXCEPTION:
                    showError(getText(R.string.message_refresh_fail_network_exception));
                    break;

                default:
                    showError(Utils.getErrorTip(MessageActivity.this, R.string.get_message_fail_service_exception, errorCode));
                    break;
            }
        }

        private void showError(CharSequence text) {
            if (mHeaderOrFooter) {
                mRefreshTipView.setText(text);
                mRefreshLayout.setVisibility(View.VISIBLE);
            } else {
                Utils.showToast(MessageActivity.this, text.toString());
            }
        }
    }
}
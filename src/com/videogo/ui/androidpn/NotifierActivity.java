/* 
 * @ProjectName VideoGo
 * @Copyright HangZhou Hikvision System Technology Co.,Ltd. All Right Reserved
 * 
 * @FileName NotifierActivity.java
 * @Description 这里对文件进行描述
 * 
 * @author fangzhihua
 * @data 2013-3-20
 * 
 * 报警推送页面
 * 
 * @warning 这里写本文件的相关警告
 */
package com.videogo.ui.androidpn;

import java.util.List;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.videogo.R;
import com.videogo.alarm.AlarmLogInfoEx;
import com.videogo.alarm.AlarmLogInfoManager;
import com.videogo.constant.Constant;
import com.videogo.constant.IntentConsts;
import com.videogo.ui.androidpn.NotifierAdapter.OnIconClickListener;
import com.videogo.ui.message.MessageActivity;
import com.videogo.util.LogUtil;

/**
 * 报警推送页面
 * 
 * @author fangzhihua
 * @data 2013-3-20
 */
public class NotifierActivity extends Activity implements OnIconClickListener {
    private final static String TAG = "NotifierActivity";

    /** 竖屏时的高度 */
    private int displayHeight = 0;

    /** 报警列表容器 */
    private NotifierAdapter mAlarmAdapter = null;

    /** 监控点列表控件 */
    private ListView mAlarmLv = null;

    private LinearLayout myRelativeLayout = null;

    private LinearLayout mAlarmListLy = null;

    /** 按钮 */
    private Button mCloseBtn = null;

    private AlarmLogInfoManager mAlarmLogInfoManager = null;

    private BroadcastReceiver mReceiver = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 隐藏title
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.notifier_page);

        findView();
        initData();

        updateAlarmLogInfoDisplayList();

        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                LogUtil.debugLog(TAG, "onReceive:" + intent.getAction());
                if (intent.getAction().equals(Constant.NOTIFIER_ALARM_LIST_CHANGE_ACTION)) {
                    updateAlarmLogInfoDisplayList();
                }
            }
        };
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constant.NOTIFIER_ALARM_LIST_CHANGE_ACTION);
        registerReceiver(mReceiver, intentFilter);
        
        NotifierUtils.clearAllNotification(this);
    }

    private void findView() {
        myRelativeLayout = (LinearLayout) findViewById(R.id.myRelativeLayout);
        myRelativeLayout.getBackground().setAlpha(70);
        // myRelativeLayout.setBackgroundColor(Color.argb(0, 0, 150, 0));
        myRelativeLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        mAlarmListLy = (LinearLayout) findViewById(R.id.alarmlist_ly);
        mCloseBtn = (Button) findViewById(R.id.alarm_close_btn);
        mCloseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAlarmLogInfoManager.clearAlarmListFromNotifier();
                mAlarmLogInfoManager.clearDeviceOfflineAlarmList();
                finish();
            }
        });
        mAlarmLv = (ListView) findViewById(R.id.alarmlist_lv);
    }

    private void initData() {
        displayHeight = getWindowManager().getDefaultDisplay().getHeight();
        mAlarmLogInfoManager = AlarmLogInfoManager.getInstance();

        mAlarmAdapter = new NotifierAdapter(NotifierActivity.this);
        mAlarmLv.setAdapter(mAlarmAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void updateAlarmLogInfoDisplayList() {
        // 更新列表

        final List<AlarmLogInfoEx> alarmInfoList = mAlarmLogInfoManager.getNotifierDsiplayAlarmInfoList();
        if (alarmInfoList.size() > 0) {
            if (mAlarmAdapter != null && mAlarmLv != null) {
                mAlarmListLy.setVisibility(View.VISIBLE);
                // 如果报警数量超过2条，就以半屏显示
                if (alarmInfoList.size() > 2) {
                    LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT,
                            displayHeight / 3);
                    mAlarmLv.setLayoutParams(lp2);
                }
                mAlarmAdapter.setCameraList(alarmInfoList);
                mAlarmAdapter.notifyDataSetChanged();
            }
        }

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        updateAlarmLogInfoDisplayList();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        onOrientation(newConfig.orientation);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LogUtil.debugLog(TAG, "onDestroy");

        if (mReceiver != null) {
            unregisterReceiver(mReceiver);
        }
        if (mAlarmAdapter != null) {
            mAlarmAdapter.setCameraList(null);
            mAlarmAdapter.notifyDataSetChanged();
        }
        if (mAlarmLv != null) {
            mAlarmLv.setAdapter(null);
            mAlarmLv = null;
        }
    }

    /**
     * 屏幕旋转响应
     * 
     * @see
     * @since V1.0
     */
    private void onOrientation(int orientation) {
        displayHeight = getWindowManager().getDefaultDisplay().getHeight();
        // 如果报警数量超过2条，就以半屏显示
        if (mAlarmAdapter.getCount() > 2) {
            LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, displayHeight / 3);
            mAlarmLv.setLayoutParams(lp2);
            mAlarmAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onIconClick(AlarmLogInfoEx alarmInfo) {
        onItemClick(alarmInfo, 0);
    }

    @Override
    public void onItemLongClick(int position) {
    }

    @Override
    public void onItemClick(AlarmLogInfoEx alarmInfo, int position) {
        Intent intent = null;
        switch (alarmInfo.getNotifyType()) {
            case AlarmLogInfoEx.ALARMTYPE:
                intent = new Intent(NotifierActivity.this, MessageActivity.class);
                intent.putExtra(IntentConsts.EXTRA_PUSH, 1);
                intent.putExtra(IntentConsts.EXTRA_CAMERA_INFO, alarmInfo.getCameraInfo());
                startActivity(intent);
                break;
            case AlarmLogInfoEx.MESSAGETYPE:
                break;
            case AlarmLogInfoEx.DEVICETYPE:
                mAlarmLogInfoManager.clearDeviceOfflineAlarmList();
                break;
            default:
                break;
        }
        finish();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            mAlarmLogInfoManager.clearAlarmListFromNotifier();
            mAlarmLogInfoManager.clearDeviceOfflineAlarmList();
            finish();
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onCheckBtnClick() {
    }

    @Override
    public void onDelBtnClick(int position) {
    }
}

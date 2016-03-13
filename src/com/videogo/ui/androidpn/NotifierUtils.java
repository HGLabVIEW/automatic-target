/* 
 * @ProjectName ezviz-openapi-android-sdk
 * @Copyright HangZhou Hikvision System Technology Co.,Ltd. All Right Reserved
 * 
 * @FileName NotifierUtils.java
 * @Description 这里对文件进行描述
 * 
 * @author chenxingyf1
 * @data 2014-7-30
 * 
 * @note 这里写本文件的详细功能描述和注释
 * @note 历史记录
 * 
 * @warning 这里写本文件的相关警告
 */
package com.videogo.ui.androidpn;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;

import com.videogo.R;
import com.videogo.alarm.AlarmLogInfoEx;
import com.videogo.alarm.AlarmLogInfoManager;
import com.videogo.alarm.AnalyzeMsgUtils;
import com.videogo.alarm.BaseMessageInfo;
import com.videogo.alarm.NoticeInfo;
import com.videogo.androidpn.Constants;
import com.videogo.constant.IntentConsts;
import com.videogo.ui.cameralist.CameraListActivity;
import com.videogo.ui.message.MessageActivity;
import com.videogo.util.LocalInfo;
import com.videogo.util.LogUtil;
import com.videogo.util.Utils;

/**
 * 在此对类做相应的描述
 * @author chenxingyf1
 * @data 2014-7-30
 */
public class NotifierUtils {
    private static final String TAG = "NotifierUtils";
    
    public static void showNotification(final Context context, final Intent intent) {
        new Thread() {
            @Override
            public void run() {
                BaseMessageInfo baseMessageInfo = AnalyzeMsgUtils.getAlarmLogInfoFromPushMsg(intent);
                AlarmLogInfoManager alarmLogInfoManager = AlarmLogInfoManager.getInstance();

                // 判断消息是否已经存在 如果已经存在了 就不再显示（相当于过滤服务器过来的重复数据）。
                if (baseMessageInfo == null || (baseMessageInfo instanceof AlarmLogInfoEx 
                    && alarmLogInfoManager.isAlarmLogInfoExist((AlarmLogInfoEx)baseMessageInfo))) {
                    LogUtil.debugLog(TAG, "push 该消息已经存在,或者消息解析失败....不再显示...直接废弃....");
                    return;
                }

                int messageType = baseMessageInfo.getNotifyType();

                Intent showIntent = null;
                // 应用内推送
                if (Utils.isTopActivity(context)) {
                    switch (messageType) {
                        case AlarmLogInfoEx.ALARMTYPE:
                        case AlarmLogInfoEx.MESSAGETYPE:
                        case AlarmLogInfoEx.DEVICETYPE:
                            alarmLogInfoManager.insertNewAlarmLogInfo(context, (AlarmLogInfoEx)baseMessageInfo, true);
                            showIntent = new Intent(context, NotifierActivity.class);
                            showIntent.putExtra(Constants.NOTIFICATION_TYPE, messageType);
                            showIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            context.startActivity(showIntent);
                            break;
                        case AlarmLogInfoEx.SYSTEMTYPE:
                            LocalInfo.getInstance().setNoticeInfo((NoticeInfo)baseMessageInfo);
                            break;
                        default:
                            break;
                    }
                } else {
                    alarmLogInfoManager.insertNewAlarmLogInfo(context, (AlarmLogInfoEx)baseMessageInfo, false);
                    switch (messageType) {              
                        case AlarmLogInfoEx.ALARMTYPE:
                            showIntent = new Intent(context, MessageActivity.class);
                            showIntent.putExtra(IntentConsts.EXTRA_PUSH, 2);
                            showIntent.putExtra(IntentConsts.EXTRA_CAMERA_INFO, ((AlarmLogInfoEx)baseMessageInfo).getCameraInfo());
                            break;
                        case AlarmLogInfoEx.MESSAGETYPE:
                            showIntent = new Intent(context, CameraListActivity.class);
                            break;
                        case AlarmLogInfoEx.SYSTEMTYPE:
                            LocalInfo.getInstance().setNoticeInfo((NoticeInfo)baseMessageInfo);
                            showIntent = new Intent(context, CameraListActivity.class);
                            break;
                        case AlarmLogInfoEx.DEVICETYPE:
                            alarmLogInfoManager.clearDeviceOfflineAlarmList();
                            showIntent = new Intent(context, CameraListActivity.class);
                            break;                      
                        default:
                            break;
                    }

                    if (showIntent != null) {
                        showIntent.putExtra(Constants.NOTIFICATION_TYPE, messageType);
                        showIntent.putExtra(Constants.NOTIFICATION_MESSAGE,
                                intent.getStringExtra(Constants.NOTIFICATION_MESSAGE));
                        
                        showIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        showIntent.setFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                        showIntent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                        showIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        showIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                        notifyNotification(context, null, showIntent);
                    }
                }                
            }
        }.start();
    }

    public static void notifyNotification(Context context, Class<?> cls, Intent intent) {
        if (intent == null) {
            return;
        }

        NotificationCompat.Builder notifyBuilder = new NotificationCompat.Builder(context);
        notifyBuilder.setSmallIcon(R.drawable.shipin7_alarm_msg_new);
        notifyBuilder.setContentTitle(context.getString(R.string.app_name));
        notifyBuilder.setOngoing(true);
        notifyBuilder.setAutoCancel(false);
        notifyBuilder.setDefaults(Notification.DEFAULT_ALL);
        int requestCode = (int) SystemClock.uptimeMillis();
        PendingIntent contentIntent = PendingIntent.getActivity(context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        int size = AlarmLogInfoManager.getInstance().getAllOutsideAlarmList().size();
        if (size > 1) {
            notifyBuilder.setContentText(context.getString(R.string.push_event_get) + " " + size + " "
                    + context.getString(R.string.push_event_get_count));
        } else {
            notifyBuilder.setContentText(intent.getStringExtra(Constants.NOTIFICATION_MESSAGE));
            String msg = intent.getStringExtra(Constants.NOTIFICATION_MESSAGE);
            if(!TextUtils.isEmpty(msg)) {
                notifyBuilder.setContentText(intent.getStringExtra(Constants.NOTIFICATION_MESSAGE));
            } else {
                notifyBuilder.setContentText(context.getString(R.string.push_event_get) + " 1 "
                        + context.getString(R.string.push_event_get_count));                
            }            
        }
        notifyBuilder.setContentIntent(contentIntent);

        // 创建一个NotificationManager的引用
        NotificationManager notificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        // 把Notification传递给NotificationManager
        notificationManager.notify(0, notifyBuilder.getNotification());
    }
    
    public static void clearAllNotification(Context context) {
        NotificationManager notificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
    }
}

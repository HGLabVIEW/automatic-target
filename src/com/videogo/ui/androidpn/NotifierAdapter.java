/* 
 * @ProjectName VMSNetSDKDemo
 * @Copyright HangZhou Hikvision System Technology Co.,Ltd. All Right Reserved
 * 
 * @FileName NotifierAdapter.java
 * @Description 这里对文件进行描述
 * 
 * @author fangzhihua
 * @data 2013-3-1
 * 
 * @note 这里写本文件的详细功能描述和注释
 * @note 历史记录
 * 
 * @warning 这里写本文件的相关警告
 */
package com.videogo.ui.androidpn;

import java.util.Date;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.download.DecryptFileInfo;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;
import com.nostra13.universalimageloader.core.listener.ImageLoadingProgressListener;
import com.videogo.R;
import com.videogo.alarm.AlarmLogInfoEx;
import com.videogo.ui.message.AlarmType;
import com.videogo.util.LogUtil;

/**
 * 在此对类做相应的描述
 * 
 * @author fangzhihua
 * @data 2013-3-1
 */
public class NotifierAdapter extends BaseAdapter {
    private String TAG = "NotifierAdapter";
    /** item列表 */
    private List<AlarmLogInfoEx> mItemDataList = null;

    private Context mContext = null;

    /** 视图容器 */
    private LayoutInflater mListContainer = null;

    /** 图标按下监听对象 */
    private OnIconClickListener mListener = null;

    /** 异步图片加载器 */
    private ImageLoader mImageLoader = null;

    private LayoutParams mLayoutParams = null;

    /**
     * 自定义控件集合
     * 
     * @author dengsh
     * @data 2012-6-25
     */
    public static class ViewHolder {
        public ImageView iconIv;

        public Button cameraItemBtn;

        public TextView alarmName;

        public TextView fromTv;

        public TextView alarmTime;

        public TextView messNumTv;

        private ImageView offlineIv;

        private LinearLayout cameraItemRl = null;

        private RelativeLayout cameraItemRlt;

        private ProgressBar progressBar;
    }

    /**
     * 默认构造函数
     */
    public NotifierAdapter() {
    }

    /**
     * 带参数的构造函数
     * 
     * @param context
     * @param itemList
     * @param thumbnailPath
     * @param playingIdList
     */
    public NotifierAdapter(Context context) {
        // 创建视图容器并设置上下文
        mListener = (OnIconClickListener) context;
        mListContainer = LayoutInflater.from(context);
        mContext = context;
        mImageLoader = ImageLoader.getInstance();
    }

    /**
     * 设置监控点列表
     * 
     * @param mCameraList
     * @see
     * @since V1.0
     */
    public void setCameraList(List<AlarmLogInfoEx> cameraList) {
        mItemDataList = cameraList;
    }

    @Override
    public int getCount() {
        if (mItemDataList != null) {
            return mItemDataList.size();
        } else {
            return 0;
        }
    }

    /*
     * (non-Javadoc)
     * @see android.widget.Adapter#getItem(int)
     */
    @Override
    public Object getItem(int position) {
        return position;
    }

    /*
     * (non-Javadoc)
     * @see android.widget.Adapter#getItemId(int)
     */
    @Override
    public long getItemId(int position) {
        return position;
    }

    /*
     * (non-Javadoc)
     * @see android.widget.Adapter#getView(int, android.view.View, android.view.ViewGroup)
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LogUtil.debugLog(TAG, "position = " + position);

        // 自定义视图
        final ViewHolder viewHolder;
        // 获取数据
        final AlarmLogInfoEx alarmLogInfo = mItemDataList.get(position);

        if (convertView == null) {
            viewHolder = new ViewHolder();

            // 获取list_item布局文件的视图
            convertView = mListContainer.inflate(R.layout.notifier_alarmloginfo_list_item, null);

            // 获取控件对象
            viewHolder.iconIv = (ImageView) convertView.findViewById(R.id.item_icon);
            viewHolder.iconIv.setDrawingCacheEnabled(false);
            viewHolder.iconIv.setWillNotCacheDrawing(true);
            viewHolder.cameraItemBtn = (Button) convertView.findViewById(R.id.camera_item_btn);
            viewHolder.alarmName = (TextView) convertView.findViewById(R.id.name_tv);
            viewHolder.fromTv = (TextView) convertView.findViewById(R.id.from_tv);
            viewHolder.alarmTime = (TextView) convertView.findViewById(R.id.time_tv);
            viewHolder.messNumTv = (TextView) convertView.findViewById(R.id.message_num_tv);
            viewHolder.offlineIv = (ImageView) convertView.findViewById(R.id.item_offline);
            viewHolder.progressBar = (ProgressBar) convertView.findViewById(R.id.watting_pb);

            viewHolder.cameraItemRl = (LinearLayout) convertView.findViewById(R.id.camera_item_rl);

            viewHolder.cameraItemRlt = (RelativeLayout) convertView.findViewById(R.id.item_icon_area);

            viewHolder.cameraItemBtn.setOnClickListener(mOnClickListener);

            // 设置控件集到convertView
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        viewHolder.iconIv.setImageResource(R.drawable.notify_bg);
        viewHolder.iconIv.setTag(null);
        mImageLoader.cancelDisplayTask(viewHolder.iconIv);
        viewHolder.cameraItemBtn.setTag(Integer.valueOf(position));
        if (alarmLogInfo != null) {
            if (alarmLogInfo.getNotifyType() == AlarmLogInfoEx.DEVICETYPE) {
                int site = alarmLogInfo.getObjectName().lastIndexOf("(");
                viewHolder.fromTv.setText("");
                viewHolder.alarmName.setMaxLines(2);
                if (site > -1) {
                    viewHolder.alarmName.setText(alarmLogInfo.getObjectName().substring(0, site));
                } else {
                    viewHolder.alarmName.setText(alarmLogInfo.getObjectName());
                }
            } else {
                setAlarmName(viewHolder, alarmLogInfo);
            }
            String time = alarmLogInfo.getAlarmOccurTime();
            if (time.length() > 11) {
                // 如果是今天的日期，则显示“今天”
                if (alarmLogInfo.getNotifyType() == AlarmLogInfoEx.DEVICETYPE) {
                    if (time.substring(0, 10).trim().equals(getDate())) {
                        viewHolder.fromTv.setText(mContext.getString(R.string.today) + " " + time.substring(11));
                    } else {
                        viewHolder.fromTv.setText(time);
                    }
                    viewHolder.alarmTime.setText("");
                } else {
                    if (time.substring(0, 10).trim().equals(getDate())) {
                        viewHolder.alarmTime.setText(mContext.getString(R.string.today) + " " + time.substring(11));
                    } else {
                        viewHolder.alarmTime.setText(time);
                    }
                }
            }
            mLayoutParams = viewHolder.cameraItemRlt.getLayoutParams();
            if (alarmLogInfo.getNotifyType() == AlarmLogInfoEx.DEVICETYPE) {
                viewHolder.alarmName.setTextColor(Color.parseColor("#A75F03"));
                viewHolder.alarmTime.setTextColor(Color.parseColor("#CCA067"));
                viewHolder.fromTv.setTextColor(Color.parseColor("#A75F03"));
                viewHolder.cameraItemBtn.setBackgroundColor(Color.parseColor("#FFF8DF"));
                mLayoutParams.height = mLayoutParams.width * 8 / 14;
                viewHolder.cameraItemRlt.invalidate();
            } else {
                viewHolder.alarmName.setTextColor(Color.BLACK);
                viewHolder.alarmTime.setTextColor(Color.GRAY);
                viewHolder.fromTv.setTextColor(Color.BLACK);
                viewHolder.cameraItemBtn.setBackgroundResource(R.drawable.cameralist_item_selector);
                mLayoutParams.height = mLayoutParams.width * 8 / 11;
                viewHolder.cameraItemRlt.invalidate();
            }
            if (alarmLogInfo.getAlarmNum() > 1) {
                viewHolder.messNumTv.setVisibility(View.VISIBLE);
                viewHolder.messNumTv.setText(alarmLogInfo.getAlarmNum() + "");
            } else {
                viewHolder.messNumTv.setVisibility(View.GONE);
            }
            viewHolder.offlineIv.setVisibility(View.GONE);
            if (alarmLogInfo.getNotifyType() == AlarmLogInfoEx.ALARMTYPE) {
                viewHolder.progressBar.setVisibility(View.VISIBLE);
                AlarmType alarmType = AlarmType.getAlarmTypeById(alarmLogInfo.getAlarmType());
                if (alarmType.hasCamera()) {

                    DisplayImageOptions options = new DisplayImageOptions.Builder()
                            .cacheInMemory(true)
                            .cacheOnDisk(true)
                            .needDecrypt(alarmLogInfo.getAlarmEncryption())
                            .considerExifParams(true)
                            .showImageForEmptyUri(R.drawable.notify_bg)
                            .showImageOnFail(R.drawable.event_list_fail_pic)
                            .showImageOnDecryptFail(R.drawable.alarm_encrypt_image_mid)
                            .extraForDownloader(
                                    new DecryptFileInfo(alarmLogInfo.getDeviceSerial(), alarmLogInfo.getCheckSum()))
                            .build();

                    mImageLoader.displayImage(null/*alarmLogInfo.getAlarmPicUrl()*/, viewHolder.iconIv, options,
                            new ImageLoadingListener() {

                                @Override
                                public void onLoadingStarted(String imageUri, View view) {
                                    viewHolder.progressBar.setProgress(0);
                                    viewHolder.progressBar.setVisibility(View.VISIBLE);
                                }

                                @Override
                                public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
                                    viewHolder.progressBar.setVisibility(View.GONE);
                                }

                                @Override
                                public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                                    viewHolder.progressBar.setVisibility(View.GONE);
                                }

                                @Override
                                public void onLoadingCancelled(String imageUri, View view) {
                                    viewHolder.progressBar.setVisibility(View.GONE);
                                }

                            }, new ImageLoadingProgressListener() {

                                @Override
                                public void onProgressUpdate(String imageUri, View view, int current, int total) {
                                    viewHolder.progressBar.setProgress(current * 100 / total);
                                }
                            });
                } else {
                    viewHolder.iconIv.setImageResource(alarmType.getDrawableResId());
                    viewHolder.progressBar.setVisibility(View.GONE);
                    viewHolder.iconIv.setVisibility(View.VISIBLE);
                }
                viewHolder.cameraItemRl.setBackgroundColor(Color.WHITE);
            } else {
                viewHolder.progressBar.setVisibility(View.GONE);
                if (alarmLogInfo.getNotifyType() == AlarmLogInfoEx.DEVICETYPE) {
                    viewHolder.iconIv.setImageResource(R.drawable.device_offline);
                    viewHolder.cameraItemRl.setBackgroundColor(Color.parseColor("#FFF8DF"));
                    viewHolder.iconIv.setVisibility(View.VISIBLE);
                } else {
                    viewHolder.cameraItemRl.setBackgroundColor(Color.WHITE);
                    viewHolder.iconIv.setImageResource(R.drawable.notify_bg);
                    viewHolder.iconIv.setVisibility(View.VISIBLE);
                }
            }
        }

        return convertView;
    }

    private void setAlarmName(ViewHolder viewHolder, final AlarmLogInfoEx alarmLogInfo) {
        // 留言类型
        if (alarmLogInfo.getNotifyType() == AlarmLogInfoEx.MESSAGETYPE) {
            // 2表示视频留言
            if (alarmLogInfo.getAlarmType() == 2) {
                viewHolder.alarmName.setText(R.string.receice_leave_msg_video);
                // 1表示语音留言
            } else if (alarmLogInfo.getAlarmType() == 1) {
                viewHolder.alarmName.setText(R.string.receice_leave_msg_audio);
            } else {
                viewHolder.alarmName.setText(alarmLogInfo.getObjectName());
            }
            // 报警类型
        } else {
            AlarmType alarmType = AlarmType.getAlarmTypeById(alarmLogInfo.getAlarmType());
            String alarmTypeName = null;
            if (alarmType != AlarmType.UNKNOWN) {
                alarmTypeName = mContext.getResources().getString(alarmType.getTextResId());
            }
            if (alarmTypeName != null) {
                viewHolder.alarmName.setText(alarmTypeName);
            } else {
                viewHolder.alarmName.setText(alarmLogInfo.getObjectName());
            }
        }

        viewHolder.fromTv.setText(mContext.getString(R.string.push_event_from) + alarmLogInfo.getObjectName());
    }

    private OnClickListener mOnClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mListener != null && v != null && v.getTag() != null && mItemDataList != null
                    && mItemDataList.size() > 0) {
                int position = ((Integer) v.getTag()).intValue();
                mListener.onItemClick(mItemDataList.get(position), position);
            }
        }
    };

    private String getDate() {
        return DateFormat.format("yyyy-MM-dd", new Date()).toString();
    }
    
    public interface OnIconClickListener {
        public void onIconClick(AlarmLogInfoEx alarmInfo);

        public void onItemLongClick(int position);

        public void onItemClick(AlarmLogInfoEx alarmInfo, int position);

        public void onCheckBtnClick();
        
        public void onDelBtnClick(int position);
    }
}
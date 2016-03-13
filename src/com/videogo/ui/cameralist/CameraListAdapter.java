/* 
 * @ProjectName VideoGoJar
 * @Copyright HangZhou Hikvision System Technology Co.,Ltd. All Right Reserved
 * 
 * @FileName CameraListAdapter.java
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

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;
import com.videogo.R;
import com.videogo.constant.Constant;
import com.videogo.exception.BaseException;
import com.videogo.openapi.EzvizAPI;
import com.videogo.openapi.bean.resp.CameraInfo;
import com.videogo.util.LogUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * 摄像头列表适配器
 * @author chenxingyf1
 * @data 2014-7-14
 */
public class CameraListAdapter extends BaseAdapter {
    private static final String TAG = "CameraListAdapter";

    private Context mContext = null;
    private Handler mHandler = null;
    private List<CameraInfo> mCameraInfoList = null;
    /** 监听对象 */
    private OnClickListener mListener;
    private ImageLoader mImageLoader;
    private ExecutorService mExecutorService = null;// 线程池
    public Map<String, CameraInfo> mExecuteItemMap = null;
    
    /**
     * 自定义控件集合
     * 
     * @author dengsh
     * @data 2012-6-25
     */
    public static class ViewHolder {
        public ImageView iconIv;

        public ImageView playBtn;

        public ImageView offlineBtn;

        public TextView cameraNameTv;
        
        public ImageButton cameraDelBtn;

        public ImageButton alarmListBtn;
        
        public ImageButton remoteplaybackBtn;

        public ImageButton setDeviceBtn;

        public View itemIconArea;

        public ImageView offlineBgBtn;
        
        public ImageButton deleteBtn;
        
        public ImageButton devicePicBtn;
        
        public ImageButton deviceVideoBtn;
    }
    
    public CameraListAdapter(Context context) {
        mContext = context;
        mHandler = new Handler();
        mCameraInfoList = new ArrayList<CameraInfo>();
        mImageLoader = ImageLoader.getInstance();
        mExecuteItemMap = new HashMap<String, CameraInfo>();
    }
    
    public void clearImageCache() {
        mImageLoader.clearMemoryCache();
    }
    
    public void setOnClickListener(OnClickListener l) {
        mListener = l;
    }
    
    public void addItem(CameraInfo item) {
        mCameraInfoList.add(item);
    }

    public void removeItem(CameraInfo item) {
        for(int i = 0; i < mCameraInfoList.size(); i++) {
            if(item == mCameraInfoList.get(i)) {
                mCameraInfoList.remove(i);
            }
        }
    }
    
    public void clearItem() {
        //mExecuteItemMap.clear();
        mCameraInfoList.clear();
    }
    
    /* (non-Javadoc)
     * @see android.widget.Adapter#getCount()
     */
    @Override
    public int getCount() {
        return mCameraInfoList.size();
    }

    /* (non-Javadoc)
     * @see android.widget.Adapter#getItem(int)
     */
    @Override
    public CameraInfo getItem(int position) {
        CameraInfo item = null;
        if (position >= 0 && getCount() > position) {
            item = mCameraInfoList.get(position);
        }
        return item;
    }

    /* (non-Javadoc)
     * @see android.widget.Adapter#getItemId(int)
     */
    @Override
    public long getItemId(int position) {
        // TODO Auto-generated method stub
        return 0;
    }

    /* (non-Javadoc)
     * @see android.widget.Adapter#getView(int, android.view.View, android.view.ViewGroup)
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // 自定义视图
        ViewHolder viewHolder = null;
        if (convertView == null) {
            viewHolder = new ViewHolder();

            // 获取list_item布局文件的视图
            convertView = LayoutInflater.from(mContext).inflate(R.layout.cameralist_small_item, null);

            // 获取控件对象
            viewHolder.iconIv = (ImageView) convertView.findViewById(R.id.item_icon);
            viewHolder.iconIv.setDrawingCacheEnabled(false);
            viewHolder.iconIv.setWillNotCacheDrawing(true);
            viewHolder.playBtn = (ImageView) convertView.findViewById(R.id.item_play_btn);

            viewHolder.offlineBtn = (ImageView) convertView.findViewById(R.id.item_offline);
            viewHolder.cameraNameTv = (TextView) convertView.findViewById(R.id.camera_name_tv);
            viewHolder.cameraDelBtn = (ImageButton) convertView.findViewById(R.id.camera_del_btn);
            viewHolder.alarmListBtn = (ImageButton) convertView.findViewById(R.id.tab_alarmlist_btn);            
            viewHolder.remoteplaybackBtn = (ImageButton) convertView.findViewById(R.id.tab_remoteplayback_btn);
            viewHolder.setDeviceBtn = (ImageButton) convertView.findViewById(R.id.tab_setdevice_btn);
            viewHolder.offlineBgBtn = (ImageView) convertView.findViewById(R.id.offline_bg);
            viewHolder.itemIconArea = convertView.findViewById(R.id.item_icon_area);
            viewHolder.deleteBtn = (ImageButton) convertView.findViewById(R.id.camera_del_btn);
            viewHolder.devicePicBtn = (ImageButton) convertView.findViewById(R.id.tab_devicepicture_btn);
            viewHolder.deviceVideoBtn = (ImageButton) convertView.findViewById(R.id.tab_devicevideo_btn);
            
            // 设置点击图标的监听响应函数
            viewHolder.playBtn.setOnClickListener(mOnClickListener);

            // 设置删除的监听响应函数
            viewHolder.cameraDelBtn.setOnClickListener(mOnClickListener);

            // 设置报警列表的监听响应函数
            viewHolder.alarmListBtn.setOnClickListener(mOnClickListener);
            
            // 设置历史回放的监听响应函数
            viewHolder.remoteplaybackBtn.setOnClickListener(mOnClickListener);

            // 设置设备设置的监听响应函数
            viewHolder.setDeviceBtn.setOnClickListener(mOnClickListener);
            
            viewHolder.deleteBtn.setOnClickListener(mOnClickListener);
            
            viewHolder.devicePicBtn.setOnClickListener(mOnClickListener);
            
            viewHolder.deviceVideoBtn.setOnClickListener(mOnClickListener);
            
            // 设置控件集到convertView
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        
        // 设置position
        viewHolder.playBtn.setTag(position);
        viewHolder.remoteplaybackBtn.setTag(position);
        viewHolder.alarmListBtn.setTag(position);
        viewHolder.setDeviceBtn.setTag(position);
        viewHolder.deleteBtn.setTag(position);
        viewHolder.devicePicBtn.setTag(position);
        viewHolder.deviceVideoBtn.setTag(position);
        
        CameraInfo cameraInfo = getItem(position);
        if(cameraInfo != null) {
            if (cameraInfo.getStatus() == 0) {
                viewHolder.offlineBtn.setVisibility(View.VISIBLE);
                viewHolder.offlineBgBtn.setVisibility(View.VISIBLE);
                viewHolder.playBtn.setVisibility(View.GONE);
            } else {
                viewHolder.offlineBtn.setVisibility(View.GONE);
                viewHolder.offlineBgBtn.setVisibility(View.GONE);
                viewHolder.playBtn.setVisibility(View.VISIBLE);
            }

            viewHolder.cameraNameTv.setText(cameraInfo.getCameraName());   
            viewHolder.iconIv.setVisibility(View.INVISIBLE);
            
            String snapshotPath = EzvizAPI.getInstance().getSnapshotPath(cameraInfo.getCameraId());
            File snapshotFile = new File(snapshotPath);
            String imageUri = null; 
            if(snapshotFile.exists()) {
                imageUri = "file://" + snapshotPath;
            } else {
                imageUri = cameraInfo.getPicUrl();
            }
            if(!TextUtils.isEmpty(imageUri)) {
                DisplayImageOptions options = new DisplayImageOptions.Builder()
                .cacheInMemory(true)//设置下载的图片是否缓存在内存中  
                .cacheOnDisk(true)//设置下载的图片是否缓存在SD卡中
                .considerExifParams(true)  //是否考虑JPEG图像EXIF参数（旋转，翻转）
                .build();//构建完成                              
                // 依次从内存和sd中获取，如果没有则网络下载
                mImageLoader.displayImage(imageUri, viewHolder.iconIv, options, mImgLoadingListener);
            }                
            if(cameraInfo.getStatus() == 1 && TextUtils.isEmpty(snapshotPath)) {
                getCameraSnapshotTask(cameraInfo);
            }
        }
        
        return convertView;
    }
    
    private final ImageLoadingListener mImgLoadingListener = new ImageLoadingListener() {

        @Override
        public void onLoadingStarted(String imageUri, View view) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
            LogUtil.errorLog(TAG, "onLoadingFailed: " + failReason.toString());
        }

        @Override
        public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
            if (view != null && view instanceof ImageView && loadedImage != null) {
                ImageView imgView = (ImageView) view;
                imgView.setImageBitmap(loadedImage);
                imgView.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onLoadingCancelled(String imageUri, View view) {
            // TODO Auto-generated method stub

        }
    };
    
    private void getCameraSnapshotTask(final CameraInfo cameraInfo) {
        synchronized (mExecuteItemMap) {
            if (mExecuteItemMap.containsKey(cameraInfo.getCameraId())) {
                return;
            }
            mExecuteItemMap.put(cameraInfo.getCameraId(), cameraInfo);
        }
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    String snapshotPath = EzvizAPI.getInstance().getCameraSnapshot(cameraInfo.getCameraId());
                    LogUtil.infoLog(TAG, "getCameraSnapshotTask:" + snapshotPath);
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            CameraListAdapter.this.notifyDataSetChanged();
                        }
                    });                    
                } catch (BaseException e) {
                    e.printStackTrace();
                }
                synchronized (mExecuteItemMap) {
                    mExecuteItemMap.remove(cameraInfo.getCameraId());
                }
            }
        };
        if (mExecutorService == null) {
            // 线程个数
            mExecutorService = Executors.newSingleThreadExecutor();
        }
        Future<?> ret = mExecutorService.submit(runnable);
    }
    
    public void shutDownExecutorService() {
        if (mExecutorService != null) {
            if (!mExecutorService.isShutdown()) {
                mExecutorService.shutdown();
            }
            mExecutorService = null;
        }
    }
    
    private View.OnClickListener mOnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            if (mListener != null) {
                int position = (Integer) v.getTag();
                switch (v.getId()) {
                    case R.id.item_play_btn:
                        mListener.onPlayClick(CameraListAdapter.this, v, position);
                        break;

                    case R.id.tab_remoteplayback_btn:
                        mListener.onRemotePlayBackClick(CameraListAdapter.this, v, position);
                        break;

                    case R.id.tab_alarmlist_btn:
                        mListener.onAlarmListClick(CameraListAdapter.this, v, position);
                        break;
                        
                    case R.id.tab_setdevice_btn:
                        mListener.onSetDeviceClick(CameraListAdapter.this, v, position);
                        break;
                        
                    case R.id.camera_del_btn: 
                        mListener.onDeleteClick(CameraListAdapter.this, v, position);
                        break;
                        
                    case R.id.tab_devicepicture_btn: 
                        mListener.onDevicePictureClick(CameraListAdapter.this, v, position);
                        break;
                        
                    case R.id.tab_devicevideo_btn: 
                        mListener.onDeviceVideoClick(CameraListAdapter.this, v, position);
                        break;                        
                }
            }
        }
    };
    
    public interface OnClickListener {

        public void onPlayClick(BaseAdapter adapter, View view, int position);

        public void onDeleteClick(BaseAdapter adapter, View view, int position);
        
        public void onAlarmListClick(BaseAdapter adapter, View view, int position);
        
        public void onRemotePlayBackClick(BaseAdapter adapter, View view, int position);
        
        public void onSetDeviceClick(BaseAdapter adapter, View view, int position);
        
        public void onDevicePictureClick(BaseAdapter adapter, View view, int position);
        
        public void onDeviceVideoClick(BaseAdapter adapter, View view, int position);
    }
}

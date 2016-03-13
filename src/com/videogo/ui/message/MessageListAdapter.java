package com.videogo.ui.message;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.videogo.R;
import com.videogo.openapi.bean.resp.AlarmInfo;

public class MessageListAdapter extends ArrayAdapter<AlarmInfo> {
    private class ViewHolder {
        TextView timeText;
        ImageView image;
        TextView from;
        TextView type;
        ViewGroup contentLayout;
        ViewGroup videoLayout;
        ViewGroup playLayout;
        ViewGroup playVideoLayout;
    }

    /** 监听对象 */
    private OnClickListener mListener;

    public MessageListAdapter(Context context, List<AlarmInfo> alarmInfoList) {
        super(context, 0, alarmInfoList);
    }

    public void setOnClickListener(OnClickListener l) {
        mListener = l;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ViewHolder viewHolder;

        if (convertView == null) {
            viewHolder = new ViewHolder();

            convertView = LayoutInflater.from(getContext()).inflate(R.layout.message_list_item, parent, false);

            // 获取控件对象
            viewHolder.timeText = (TextView) convertView.findViewById(R.id.message_time);
            viewHolder.contentLayout = (ViewGroup) convertView.findViewById(R.id.message_content);
            viewHolder.image = (ImageView) convertView.findViewById(R.id.message_image);
            viewHolder.from = (TextView) convertView.findViewById(R.id.message_from);
            viewHolder.type = (TextView) convertView.findViewById(R.id.message_type);
            viewHolder.playLayout = (ViewGroup) convertView.findViewById(R.id.message_play_layout);
            viewHolder.videoLayout = (ViewGroup) convertView.findViewById(R.id.message_video_layout);
            viewHolder.playVideoLayout = (ViewGroup) convertView.findViewById(R.id.play_video_layout);

            // 防止获得缓存
            viewHolder.image.setDrawingCacheEnabled(false);
            viewHolder.image.setWillNotCacheDrawing(true);

            // 内容区域的点击响应
            viewHolder.contentLayout.setOnClickListener(mOnClickListener);
            // 点击图片区域
            viewHolder.image.setOnClickListener(mOnClickListener);
            viewHolder.videoLayout.setOnClickListener(mOnClickListener);
            viewHolder.playLayout.setOnClickListener(mOnClickListener);

            // 设置控件集到convertView
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        // 设置position
        viewHolder.contentLayout.setTag(position);
        viewHolder.image.setTag(position);
        viewHolder.videoLayout.setTag(position);
        viewHolder.playLayout.setTag(position);

        // 获取数据
        AlarmInfo alarmLogInfo = getItem(position);

        if (alarmLogInfo != null) {
            // 消息类型
            // TODO 后面将在赋值时就转换AlarmType
            AlarmType alarmType = AlarmType.getAlarmTypeById(alarmLogInfo.getAlarmType());

            viewHolder.type.setText(getContext().getString(alarmType.getTextResId()));

            // 消息来源
            viewHolder.from.setText(alarmLogInfo.getAlarmName());

            // 消息时间
            viewHolder.timeText.setText(alarmLogInfo.getAlarmStart());

            // 消息图片
            if (alarmType.hasCamera()) {
                viewHolder.image.setImageResource(R.drawable.my_cover);
            } else {
                viewHolder.image.setImageResource(alarmType.getDrawableResId());
            }

            if (alarmType.hasCamera()) {
                viewHolder.playLayout.setEnabled(true);
                viewHolder.videoLayout.setEnabled(true);
                viewHolder.playVideoLayout.setVisibility(View.VISIBLE);
            } else {
                viewHolder.playVideoLayout.setVisibility(View.GONE);
            }
        }

        return convertView;
    }

    private View.OnClickListener mOnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            if (mListener != null) {
                int position;
                switch (v.getId()) {
                    case R.id.message_video_layout:
                        position = (Integer) v.getTag();
                        mListener.onVideoButtonClick(MessageListAdapter.this, v, position);
                        break;

                    case R.id.message_play_layout:
                        position = (Integer) v.getTag();
                        mListener.onPlayButtonClick(MessageListAdapter.this, v, position);
                        break;

                    case R.id.message_content:
                        position = (Integer) v.getTag();
                        mListener.onItemClick(MessageListAdapter.this, v, position);
                        break;

                    case R.id.message_image:
                        position = (Integer) v.getTag();
                        AlarmInfo alarmLogInfo = getItem(position);
                        if (alarmLogInfo != null && AlarmType.getAlarmTypeById(alarmLogInfo.getAlarmType()).hasCamera()) {
                            mListener.onImageClick(MessageListAdapter.this, v, position);
                        }
                        break;
                }
            }
        }
    };

    public interface OnClickListener {
        public void onItemClick(BaseAdapter adapter, View view, int position);

        public void onImageClick(BaseAdapter adapter, View view, int position);

        public void onVideoButtonClick(BaseAdapter adapter, View view, int position);

        public void onPlayButtonClick(BaseAdapter adapter, View view, int position);
    }
}
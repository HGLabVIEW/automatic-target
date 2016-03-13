package com.videogo.ui.discovery;

import com.videogo.R;
import com.videogo.openapi.bean.resp.SquareColumnInfo;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class SquareColumnAdapter extends VideoGoBaseAdapter<SquareColumnInfo> {
	public SquareColumnAdapter(Context context) {
		super(context);
	}
	@Override
	public long getItemId(int position) {
		if (position < 0 || position >= mList.size()) {
			return 0;
		} else {
			return Integer.valueOf(mList.get(position).getChannelCode());
		}
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder viewHolder = null;
		if (convertView == null) {
			convertView = LayoutInflater.from(context).inflate(R.layout.square_column_item, null);
			viewHolder = new ViewHolder(convertView);
			convertView.setTag(viewHolder);
		} else {
			viewHolder = (ViewHolder) convertView.getTag();
		}
		viewHolder.render(position);
		return convertView;
	}

	private class ViewHolder {
		private TextView textView;
		
		public ViewHolder(View view) {
			textView = (TextView) view.findViewById(R.id.squareColumnItem);
		}
		
		public void render(int position) {
			textView.setText(mList.get(position).getChannelName());
		}
	}
}

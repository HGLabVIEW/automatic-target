package com.videogo.ui.discovery;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

public abstract class VideoGoBaseAdapter<T> extends BaseAdapter {
	protected List<T> mList = new ArrayList<T>();
	protected Context context;
	
	public VideoGoBaseAdapter(Context context) {
		this.context = context;
	}
	
	public VideoGoBaseAdapter() {}
	
	@Override
	public int getCount() {
		int count = 0;
		if (mList != null) {
			count = mList.size();
		}
		return count;
	}

	@Override
	public T getItem(int position) {
		if (mList != null) {
			return position < mList.size() ? mList.get(position) : null;
		}
		return null;
	}
	
	public List<T> getList() {
		return mList;
	}

	abstract public View getView(int position, View convertView, ViewGroup parent);

	public void setList(List<T> list) {
		if (list == null) {
			list = new ArrayList<T>();
		}
		this.mList = list;
		notifyDataSetChanged();
	}
	
	public void appendData(List<T> list) {
		if (list == null) {
			return;
		}
		mList.addAll(list);
		notifyDataSetChanged();
	}
	public void add(T object){
		if(object == null)
			return;
			mList.add(object);
			notifyDataSetChanged();
	}
	public void add(int position,T object){
		if(object == null)
			return;
		
		synchronized (mList) {
			mList.add(position,object);
		}
	}
	
	public void clear() {
		mList.clear();
		notifyDataSetChanged();
	}
}
package com.videogo.asynctask;

import com.videogo.exception.BaseException;
import com.videogo.widget.WaitDialog;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.AsyncTask;

public abstract class AsyncTaskBase<Params, Progress, Result> extends AsyncTask<Params, Progress, Result> {
	protected Context context;
	private WaitDialog waitDialog;
	protected int mErrorCode = 0;
    protected String msg;
	
	public AsyncTaskBase(Context context) {
		this(context, false);
	}
	
	public AsyncTaskBase(Context context, boolean needDialog) {
		this.context = context;
		if (needDialog) {
			waitDialog = new WaitDialog(context, android.R.style.Theme_Translucent_NoTitleBar);
			waitDialog.setCanceledOnTouchOutside(false);
		}
	}
	
	protected abstract Result realDoInBackground(Params... params) throws BaseException;
	
	protected abstract void realOnPostExecute(Result result);
	
	protected void realOnCancelled() {}
	
	protected void realOnProgressUpdate(Progress... values) {}
	
	protected abstract void onError(int mErrorCode);
	
	private boolean isActivityFinishing() {
		return context == null || (context instanceof Activity && ((Activity) context).isFinishing());
	}
	
	@Override
	@Deprecated
	protected Result doInBackground(Params... params) {
		try {
			return realDoInBackground(params);
		} catch (BaseException e) {
			e.printStackTrace();
            msg = e.getMessage();
			mErrorCode = ((BaseException) e).getErrorCode();
			return null;
		}
	}
	
	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		if (waitDialog != null) {
			waitDialog.setOnCancelListener(new OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					if (getStatus() != AsyncTask.Status.FINISHED) {
						cancel(true);
					}
				}
			});
			if (isActivityFinishing()) {
				cancel(true);
			} else {
				waitDialog.show();
			}
		}
	}
	
	@Override
	@Deprecated
	protected void onPostExecute(Result result) {
		super.onPostExecute(result);
		
		if (isActivityFinishing()) {
			return ;
		}
		
		if (waitDialog != null && waitDialog.isShowing()) {
			waitDialog.dismiss();
		}
		
		if (mErrorCode != 0) {
			onError(mErrorCode);
		} else {
			realOnPostExecute(result);
		}
	}
	
	@Override
	@Deprecated
	protected void onProgressUpdate(Progress... values) {
		super.onProgressUpdate(values);
		
		if (isActivityFinishing()) {
			return;
		}
		realOnProgressUpdate(values);
	}
	
	@Override
	@Deprecated
	protected void onCancelled() {
		super.onCancelled();
		
		if (isActivityFinishing()) {
			return;
		}
		
		if (waitDialog != null && waitDialog.isShowing()) {
			waitDialog.dismiss();
		}
		
		realOnCancelled();
	}
}

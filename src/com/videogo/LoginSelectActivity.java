/* 
 * @ProjectName ezviz-openapi-android-demo
 * @Copyright HangZhou Hikvision System Technology Co.,Ltd. All Right Reserved
 * 
 * @FileName LoginSelectActivity.java
 * @Description 这里对文件进行描述
 * 
 * @author chenxingyf1
 * @data 2014-12-6
 * 
 * @note 这里写本文件的详细功能描述和注释
 * @note 历史记录
 * 
 * @warning 这里写本文件的相关警告
 */
package com.videogo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;

import com.videogo.androidpn.AndroidpnUtils;
import com.videogo.constant.Constant;
import com.videogo.openapi.EzvizAPI;
import com.videogo.ui.cameralist.CameraListActivity;
import com.videogo.ui.util.VerifySmsCodeUtil;

/**
 * 登录选择演示
 * 
 * @author chenxingyf1
 * @data 2014-12-6
 */
public class LoginSelectActivity extends Activity implements OnClickListener,
		VerifySmsCodeUtil.OnVerifyListener {
	private EzvizAPI mEzvizAPI = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.login_page);

		initData();
		initView();
	}

	private void initData() {
		mEzvizAPI = EzvizAPI.getInstance();
	}

	private void initView() {

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.view.View.OnClickListener#onClick(android.view.View)
	 */
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.web_login_btn:
			mEzvizAPI.gotoLoginPage(false);
			break;

		default:
			break;
		}
	}

	private void openPlatformLoginDialog() {
		final EditText editText = new EditText(this);
		new AlertDialog.Builder(this)
				.setTitle(R.string.please_input_platform_accesstoken_txt)
				.setIcon(android.R.drawable.ic_dialog_info)
				.setView(editText)
				.setPositiveButton(R.string.certain,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								// String getAccessTokenSign =
								// SignUtil.getGetAccessTokenSign();

								mEzvizAPI.setAccessToken(editText.getText()
										.toString());
								Intent toIntent = new Intent(
										LoginSelectActivity.this,
										CameraListActivity.class);
								toIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
								LoginSelectActivity.this
										.startActivity(toIntent);
								AndroidpnUtils
										.startPushServer(LoginSelectActivity.this);
							}

						}).setNegativeButton(R.string.cancel, null).show();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.videogo.ui.util.VerifySmsCodeUtil.OnVerifyListener#onVerify(int,
	 * int)
	 */
	@Override
	public void onVerify(int type, int result) {
		// TODO Auto-generated method stub

	}
}

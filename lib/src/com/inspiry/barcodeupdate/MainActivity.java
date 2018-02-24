package com.inspiry.barcodeupdate;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.view.WindowManager;


@TargetApi(19)
public class MainActivity extends Activity implements View.OnClickListener,  OnEventAvailableListener{
	public static boolean PhoneTest = true;
	private static String TAG = "MainActivity";
	private final String[] baudrate = { "300", "600", "1200", "2400", "4800",
			"9600", "19200", "38400", "43000", "56000", "57600", "115200" };
			
	private String extern_usb = "/mnt/usb_storage";
	private int i=1;
	private boolean mAutoUpdate = false;
	private CheckBox mAutoupdate;
	private Spinner mBaudrate;
	private Button mConnect_btn, mDisconnect_btn, mGetversion_btn,
			mLoadfile_btn1, mLoadfile_btn2;
	private Handler mCoreHandler;
	private ProgressBar mLoadprogres;
	private TextView mLoadprogres_tv;
	private EditText mDetaShow_tv;
	private StringBuffer mLog;
	private EditText mPath;
	private Runnable mRunnable;
	private TimeCount mTimeFirst;
	private BarCodeSerialUpdateWrapper mUpdateWrapper;
	private Button mUpdate_btn;
	private String mUri1 = "/data/inspiry/SmartReader";
	private String mUri2 = "/data/inspiry/SmartReader.md5";
	private Intent mIntent;
	public static final int MESSAGE_CONNECT =10;
	public static final int MESSAGE_GETVER =11;
	public static final int MESSAGE_DISCONNECT =12;
	public static final int MESSAGE_UPDATE =13;
	public static final int MESSAGE_UPDATE_PROGRESS =14;

	protected void onCreate(Bundle paramBundle) {
		super.onCreate(paramBundle);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,  WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);  
		setContentView(R.layout.activity_main);
		mUpdateWrapper = BarCodeSerialUpdateWrapper.getInstance();
		mTimeFirst = new TimeCount(15000L, 1000L);
		
		mIntent = new Intent(Intent.ACTION_GET_CONTENT);
		mIntent.setType("*/*");
		mPath = (EditText) findViewById(R.id.path);
		mLoadprogres_tv = (TextView) findViewById(R.id.loadprogres_tv);
		mLoadprogres = (ProgressBar) findViewById(R.id.loadprogres);
		mBaudrate = (Spinner) findViewById(R.id.baudrate);
		mBaudrate.setAdapter(new ArrayAdapter(this, R.layout.spinner_item,baudrate));
		mDetaShow_tv = (EditText) findViewById(R.id.detashow);
		mConnect_btn = (Button) findViewById(R.id.connect);
		mDisconnect_btn = (Button) findViewById(R.id.disconnect);
		mGetversion_btn = (Button) findViewById(R.id.getversion);
		mLoadfile_btn1 = (Button) findViewById(R.id.loadfile1);
		mLoadfile_btn2 = (Button) findViewById(R.id.loadfile2);
		mUpdate_btn = (Button) findViewById(R.id.update);
		mAutoupdate = (CheckBox)findViewById(R.id.autoupdate);
		mAutoupdate.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
					public void onCheckedChanged(CompoundButton paramAnonymousCompoundButton,boolean paramAnonymousBoolean) {
						mAutoUpdate = paramAnonymousBoolean;
						
					}
				});
		mConnect_btn.setOnClickListener(this);
		mDisconnect_btn.setOnClickListener(this);
		mGetversion_btn.setOnClickListener(this);
		mLoadfile_btn1.setOnClickListener(this);
		mLoadfile_btn2.setOnClickListener(this);
		mUpdate_btn.setOnClickListener(this);
		mLog = new StringBuffer();
		mDetaShow_tv.addTextChangedListener(new TextWatcher() {
			public void afterTextChanged(Editable paramAnonymousEditable) {
			}

			public void beforeTextChanged(
					CharSequence paramAnonymousCharSequence,
					int paramAnonymousInt1, int paramAnonymousInt2,
					int paramAnonymousInt3) {
			}

			public void onTextChanged(CharSequence paramAnonymousCharSequence,
					int paramAnonymousInt1, int paramAnonymousInt2,
					int paramAnonymousInt3) {
				mDetaShow_tv.setSelection(paramAnonymousInt3);
			}
		});
		
		mLoadfile_btn1.setText("更新包:" + mUri1);
		mLoadfile_btn2.setText("MD5:" + mUri2);

		
		mCoreHandler = new Handler(){  
			public void handleMessage(Message msg) {  
				int what = msg.what;  
				switch (what) {  
				case MESSAGE_CONNECT:
					connect();
					break;  
				case MESSAGE_GETVER: 
					getVersion();
					break;
				case MESSAGE_DISCONNECT:  
					disConnect();
					break;
				case MESSAGE_UPDATE:
					update();
					break;
				case MESSAGE_UPDATE_PROGRESS:
					mLoadprogres.setProgress(msg.arg1);
					mLoadprogres_tv.setText(msg.arg1+ "%");
					Log.d(TAG,"Event EventTypeUpgradeProgress "+ msg.arg1+ "%");
					break;
				default:  
					break;  
				}  
			};
		};		
	}

	protected void onResume() {
		mPath.setSelection(mPath.getText().length());
		for (int i = 0; i < baudrate.length; i++) {
			if (baudrate[i].equals("9600")) {
				mBaudrate.setSelection(i);
			}
		}
		if (mUpdateWrapper.isConnected()) {
			mDisconnect_btn.setEnabled(true);
			mGetversion_btn.setEnabled(true);
			mLoadfile_btn1.setEnabled(false);
			mLoadfile_btn2.setEnabled(false);
			mUpdate_btn.setEnabled(true);

		} else {
			mDisconnect_btn.setEnabled(false);
			mGetversion_btn.setEnabled(false);
			mLoadfile_btn1.setEnabled(false);
			mLoadfile_btn2.setEnabled(false);
			mUpdate_btn.setEnabled(false);
		}
		super.onResume();
	}
	public void OnEventAvailable(Message msg) {
		switch (msg.what) {
		case BarCodeSerialUpdateWrapper.EventTypeUpgradeSuccess:
			Toast.makeText(MainActivity.this,"升级完成", 0).show();
			mLog.append("升级完成！"+ System.getProperty("line.separator"));
			mDetaShow_tv.setText(mLog);
			Log.d(TAG,"Event EventTypeUpgradeSuccess");
			i++;
			if(mAutoUpdate){
				mTimeFirst.start();
			}
			break;
		case BarCodeSerialUpdateWrapper.EventTypeUpgradeFail:
			mLog.append("设备连接失败！!!!"+ System.getProperty("line.separator"));
			mDetaShow_tv.setText(mLog);
			Log.d(TAG,"Event EventTypeUpgradeFail!!!");
			break;
		case BarCodeSerialUpdateWrapper.EventTypeUpgradeProgress:
			Message message1 = mCoreHandler.obtainMessage();  
			message1.what = MESSAGE_UPDATE_PROGRESS;  
			message1.arg1 = msg.arg1;
			mCoreHandler.sendMessage(message1);
			break;
		case BarCodeSerialUpdateWrapper.EventTypeTargetConnected:
			Toast.makeText(MainActivity.this,"设备连接成功！", 0).show();
			mLog.append("设备连接成功！"+ System.getProperty("line.separator"));
			mDetaShow_tv.setText(mLog);
			mDisconnect_btn.setEnabled(true);
			mGetversion_btn.setEnabled(true);
			mLoadfile_btn1.setEnabled(false);
			mLoadfile_btn2.setEnabled(false);
			mUpdate_btn.setEnabled(true);
			Log.d(TAG,"Event EventTypeTargetConnected");
			if(mAutoUpdate){
				Message message = mCoreHandler.obtainMessage();  
				message.what = MESSAGE_UPDATE; 
				mCoreHandler.sendMessage(message);
			}
			break;
		case BarCodeSerialUpdateWrapper.EventTypeTargetDisConnected:
			mLoadprogres.setProgress(0);
			mLoadprogres_tv.setText(0 + "%");
			mLog.append("设备已断开"+ System.getProperty("line.separator"));
			mDetaShow_tv.setText(mLog);
			mConnect_btn.setEnabled(true);
			mDisconnect_btn.setEnabled(false);
			mGetversion_btn.setEnabled(false);
			mLoadfile_btn1.setEnabled(false);
			mLoadfile_btn2.setEnabled(false);
			Log.d(TAG,"Event EventTypeTargetDisConnected");
			break;
		}
	}
	private void connect(){
		if (mUpdateWrapper.isConnected()) {
			mLog.append("设备已连接！" + System.getProperty("line.separator"));
			mDetaShow_tv.setText(mLog);
			Toast.makeText(MainActivity.this, "设备已连接！", 0).show();
			return;
		}
		if (!mPath.getText().toString().trim().equals("")&& 
			!mBaudrate.getSelectedItem().toString().trim().equals("")) {
			try {
				mUpdateWrapper.setDeviceName(mPath.getText().toString());
				mUpdateWrapper.setBaudrate(Integer.valueOf(mBaudrate.getSelectedItem().toString()));
				mUpdateWrapper.connectTarget();
				mUpdateWrapper.setOnEventAvailableListener(this);
			
			} catch (Exception e) {
				// TODO Auto-generated catch block
				mLog.append("连接失败，请确认设备节点和手机root情况"+ System.getProperty("line.separator"));
				mDetaShow_tv.setText(mLog);
				Toast.makeText(MainActivity.this, "连接失败！", 0).show();
				e.printStackTrace();
			}
			
		} else {
			mLog.append("请填写连接信息"+ System.getProperty("line.separator"));
			mDetaShow_tv.setText(mLog);
			Toast.makeText(this, "请填写连接信息", Toast.LENGTH_SHORT).show();
		}
	}
	private void disConnect(){
		if(!mUpdateWrapper.isConnected()){
			mLog.append("设备未连接！" + System.getProperty("line.separator"));
			mDetaShow_tv.setText(mLog);
			Toast.makeText(this, "设备未连接!", Toast.LENGTH_SHORT).show();
			return ;
		}
		
		try {
			if (mUpdateWrapper.disConnectTarget()) {
				mLog.append("断开成功！"+ System.getProperty("line.separator"));
				mDetaShow_tv.setText(mLog);
				mDisconnect_btn.setEnabled(false);
				mGetversion_btn.setEnabled(false);
				mLoadfile_btn1.setEnabled(false);
				mLoadfile_btn2.setEnabled(false);
				mUpdate_btn.setEnabled(false);
				Toast.makeText(MainActivity.this, "断开成功！", 0).show();
			} else {
				mLog.append("断开失败！"+ System.getProperty("line.separator"));
				mDetaShow_tv.setText(mLog);
				Toast.makeText(MainActivity.this, "断开失败！", 0).show();
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	private void getVersion(){
		if(!mUpdateWrapper.isConnected()){
			mLog.append("请连接设备！" + System.getProperty("line.separator"));
			mDetaShow_tv.setText(mLog);
			Toast.makeText(this, "设备未连接", Toast.LENGTH_SHORT).show();
			return;
		}
		Toast.makeText(MainActivity.this,
				"当前版本：" + mUpdateWrapper.getTargetVersion(),
				Toast.LENGTH_SHORT).show();
		mLog.append("应用版本：" + mUpdateWrapper.getTargetVersion()
				+ System.getProperty("line.separator"));
		mLog.append("硬件名称："+mUpdateWrapper.getTargetHardwareName()
				+ System.getProperty("line.separator"));
		mDetaShow_tv.setText(mLog);
				
	}
	private void update(){
		if(!mUpdateWrapper.isConnected()){
			mLog.append("请连接设备！" + System.getProperty("line.separator"));
			mDetaShow_tv.setText(mLog);
			Toast.makeText(this, "设备未连接", Toast.LENGTH_SHORT).show();
			return ;
		}
		if (null == mUri1 || null == mUri2){
			if (null == mUri1) {
				Toast.makeText(this, "请选择安装包", Toast.LENGTH_SHORT).show();
				mLog.append("安装包未选择"+ System.getProperty("line.separator"));
				mDetaShow_tv.setText(mLog);
			} else if (null == mUri2) {
				mLog.append("效验包未选择"+ System.getProperty("line.separator"));
				mDetaShow_tv.setText(mLog);
				Toast.makeText(this, "请选择效验包", Toast.LENGTH_SHORT).show();
			} else {
				mLog.append("安装包、效验包未选择"+ System.getProperty("line.separator"));
				mDetaShow_tv.setText(mLog);
				Toast.makeText(this, "请选择安装包、效验包",Toast.LENGTH_SHORT).show();
			}
			return;
		}
		if (!mUpdateWrapper.checkPckage(mUri1, mUri2)){
			mLog.append("安装包、效验包不匹配！"+ System.getProperty("line.separator"));
			mDetaShow_tv.setText(mLog);
			Toast.makeText(this, "安装包、效验包不匹配！",Toast.LENGTH_SHORT).show();
			return ;		
		}
		try {
			if (mUpdateWrapper.upgradeTarget(mUri1, mUri2)) {
				mLog.append("正在升级..."+ System.getProperty("line.separator"));
				Toast.makeText(this, "正在升级",Toast.LENGTH_SHORT).show();
				mConnect_btn.setEnabled(false);
				mDisconnect_btn.setEnabled(false);
				mGetversion_btn.setEnabled(false);
				mLoadfile_btn1.setEnabled(false);
				mLoadfile_btn2.setEnabled(false);
				mUpdate_btn.setEnabled(false);
			} else {
				mLog.append("升级命令失败"+ System.getProperty("line.separator"));
				Toast.makeText(this, "升级命令失败!!!",Toast.LENGTH_SHORT).show();
				mConnect_btn.setEnabled(true);
				mDisconnect_btn.setEnabled(true);
				mGetversion_btn.setEnabled(true);
				mLoadfile_btn1.setEnabled(false);
				mLoadfile_btn2.setEnabled(false);
				mUpdate_btn.setEnabled(true);
			}
			mDetaShow_tv.setText(mLog);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

	public void onClick(View v) {
		Message msg1 = mCoreHandler.obtainMessage(); 
		msg1.what = 0;
		if (ClickLong.isClick(1500)) {
			return;
		} else {
			switch (v.getId()) {
			case R.id.connect: 
				msg1.what = MESSAGE_CONNECT;  
				break;
			case R.id.disconnect:
				msg1.what = MESSAGE_DISCONNECT;  
				break;
			case R.id.getversion:
				msg1.what = MESSAGE_GETVER;
				break;
			case R.id.update:  
				msg1.what = MESSAGE_UPDATE;
				break;
			}
			mCoreHandler.sendMessage(msg1);
		}
	}

	
	protected void onDestroy() {
		mTimeFirst.cancel();
		super.onDestroy();
		}
				
	static class ClickLong {
		private static long lastClickTime;

		public static boolean isClick(float paramFloat) {
			long l1 = System.currentTimeMillis();
			long l2 = l1 - lastClickTime;
			if ((0L < l2) && ((float) l2 < paramFloat))
				return true;
			lastClickTime = l1;
			return false;
	}
	}

	class TimeCount extends CountDownTimer {
		public TimeCount(long arg2, long arg4) {
			super(arg2, arg4);
	}

		public void onFinish() {
			mLog.delete(0,mLog.length()-1);
			mLog.append("马上第"+i+"次升级请不要断电！" + System.getProperty("line.separator"));
			mDetaShow_tv.setText(mLog);
			Message message = mCoreHandler.obtainMessage();  
			message.what = MESSAGE_CONNECT;  
			mCoreHandler.sendMessage(message);
	}

		public void onTick(long paramLong) {
			mLog.append("距离第"+i+"次升级还有" + paramLong / 1000L + "s"
					+ System.getProperty("line.separator"));
			mDetaShow_tv.setText(mLog);

	}
	}
}

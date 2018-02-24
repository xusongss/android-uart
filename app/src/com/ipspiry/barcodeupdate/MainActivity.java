package com.ipspiry.barcodeupdate;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.inspiry.barcodeupdate.BarCodeSerialUpdateWrapper;
import com.inspiry.barcodeupdate.OnEventAvailableListener;

@TargetApi(19)
public class MainActivity extends Activity implements View.OnClickListener {
	public static boolean PhoneTest = true;
	private static String TAG = "MainActivity";
	private final String[] baudrate = { "300", "600", "1200", "2400", "4800",
			"9600", "19200", "38400", "43000", "56000", "57600", "115200" };
	private String extern_usb = "/mnt/usb_storage";
	private int i;
	private boolean mAutoUpdate = false;
	private boolean mAutoConnect = true;
	private CheckBox mAutoupdate;
	private Spinner mBaudrate;
	private Button mConnect_btn, mDisconnect_btn, mGetversion_btn,
			mLoadfile_btn1, mLoadfile_btn2;
	private Handler mHandler;
	private ProgressBar mLoadprogres;
	private TextView mLoadprogres_tv;
	private EditText mDetaShow_tv;
	private StringBuffer mLog;
	private EditText mPath;
	private Runnable mRunnable;
	private TimeCount mTimeFirst;
	private BarCodeSerialUpdateWrapper mUpdateWrapper;
	private Button mUpdate_btn;
	private String mUri1 = null;
	private String mUri2 = null;
	private Intent mIntent;
	private ScaleAnimation mAanimation;

	protected void onCreate(Bundle paramBundle) {
		super.onCreate(paramBundle);
		setContentView(R.layout.activity_main);
		mUpdateWrapper = BarCodeSerialUpdateWrapper.getInstance();
		mTimeFirst = new TimeCount(30000L, 1000L);
		mHandler = new Handler();
		mRunnable = new Runnable() {
			public void run() {
				i++;
				getActionBar()
						.setTitle(
								getString(R.string.app_name)
										+ Html.fromHtml("<font color='#ff0000'>&nbsp;&nbsp;连续升级次数"
												+ i + "</font>"));
				mLog.append("连续升级次数" + i + System.getProperty("line.separator"));
				mDetaShow_tv.setText(mLog);
				mConnect_btn.performClick();
				new Handler().postDelayed(new Runnable() {
					public void run() {
						mUpdate_btn.performClick();
					}
				}, 5000);
			}
		};
		mIntent = new Intent(Intent.ACTION_GET_CONTENT);
		mIntent.setType("*/*");
		mAanimation = new ScaleAnimation(1f, 1.5f, 1f, 1.5f,
				Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
				0.5f);
		mPath = (EditText) findViewById(R.id.path);
		mLoadprogres_tv = (TextView) findViewById(R.id.loadprogres_tv);
		mLoadprogres = (ProgressBar) findViewById(R.id.loadprogres);
		mBaudrate = (Spinner) findViewById(R.id.baudrate);
		mBaudrate.setAdapter(new ArrayAdapter(this, R.layout.spinner_item,
				baudrate));
		mDetaShow_tv = (EditText) findViewById(R.id.detashow);
		mConnect_btn = (Button) findViewById(R.id.connect);
		mDisconnect_btn = (Button) findViewById(R.id.disconnect);
		mGetversion_btn = (Button) findViewById(R.id.getversion);
		mLoadfile_btn1 = (Button) findViewById(R.id.loadfile1);
		mLoadfile_btn2 = (Button) findViewById(R.id.loadfile2);
		mUpdate_btn = (Button) findViewById(R.id.update);
		mAutoupdate = (CheckBox) findViewById(R.id.autoupdate);
		mAutoupdate
				.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
					public void onCheckedChanged(
							CompoundButton paramAnonymousCompoundButton,
							boolean paramAnonymousBoolean) {
						if (paramAnonymousBoolean) {
							mAutoUpdate = paramAnonymousBoolean;
						} else {
							mAutoUpdate = paramAnonymousBoolean;
							mHandler.removeCallbacks(mRunnable);
							mAanimation.cancel();
							mTimeFirst.cancel();
						}
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
			mLoadfile_btn1.setEnabled(true);
			mLoadfile_btn2.setEnabled(true);
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

	private static String getDataColumn(Context context, Uri uri,
			String selection, String[] selectionArgs) {
		Cursor cursor = null;
		String column = MediaStore.Images.Media.DATA;
		String[] projection = { column };
		try {
			cursor = context.getContentResolver().query(uri, projection,
					selection, selectionArgs, null);
			if (cursor != null && cursor.moveToFirst()) {
				int index = cursor.getColumnIndexOrThrow(column);
				return cursor.getString(index);
			}
		} finally {
			if (cursor != null && !cursor.isClosed())
				cursor.close();
		}
		return null;
	}

	private String getPhotoPathFromContentUri(Context context, Uri uri) {
		String photoPath = "";
		MyLog("ccccccccc  111111111" + photoPath);
		if (context == null || uri == null) {
			return photoPath;
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT
				&& DocumentsContract.isDocumentUri(context, uri)) {
			String docId = DocumentsContract.getDocumentId(uri);
			if (isExternalStorageDocument(uri)) {

				String[] split = docId.split(":");
				if (split.length >= 2) {
					String type = split[0];
					if ("primary".equalsIgnoreCase(type)) {
						photoPath = Environment.getExternalStorageDirectory()
								+ "/" + split[1];
					} else {
						// MyLog(
						// "Environment.getExternalUsbStorageDirectory().getPath()"+Environment.getExternalUsbStorageDirectory().getPath());
						photoPath = extern_usb + "/" + split[1];
						MyLog("getPathFromContentUri" + photoPath);
					}
					// }
				}
			} else if (isDownloadsDocument(uri)) {
				Uri contentUri = ContentUris.withAppendedId(
						Uri.parse("content://downloads/public_downloads"),
						Long.valueOf(docId));
				photoPath = getDataColumn(context, contentUri, null, null);
			} else if (isMediaDocument(uri)) {
				String[] split = docId.split(":");
				if (split.length >= 2) {
					String type = split[0];
					Uri contentUris = null;
					if ("image".equals(type)) {
						contentUris = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
					} else if ("video".equals(type)) {
						contentUris = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
					} else if ("audio".equals(type)) {
						contentUris = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
					}
					String selection = MediaStore.Images.Media._ID + "=?";
					String[] selectionArgs = new String[] { split[1] };
					photoPath = getDataColumn(context, contentUris, selection,
							selectionArgs);
				}
			}
		} else if ("file".equalsIgnoreCase(uri.getScheme())) {
			photoPath = uri.getPath();
			MyLog("ccccccccc  44444444444444444" + photoPath);
		} else {
			photoPath = getDataColumn(context, uri, null, null);
			MyLog("ccccccccc 5555555555 " + photoPath);
		}

		return photoPath;
	}

	private static boolean isDownloadsDocument(Uri paramUri) {
		return "com.android.providers.downloads.documents".equals(paramUri
				.getAuthority());
	}

	private static boolean isExternalStorageDocument(Uri paramUri) {
		return "com.android.externalstorage.documents".equals(paramUri
				.getAuthority());
	}

	private static boolean isMediaDocument(Uri paramUri) {
		return "com.android.providers.media.documents".equals(paramUri
				.getAuthority());
	}

	public void onClick(View v) {
		if (ClickLong.isClick(1000)) {
			return;
		} else {
			switch (v.getId()) {
			case R.id.connect:
				if (mUpdateWrapper.isConnected()) {
					mLog.append("设备已连接！" + System.getProperty("line.separator"));
					mDetaShow_tv.setText(mLog);
					Toast.makeText(MainActivity.this, "设备已连接！", 0).show();
				} else {
					if (!mPath.getText().toString().trim().equals("")
							&& !mBaudrate.getSelectedItem().toString().trim()
									.equals("")) {

						try {
							mUpdateWrapper.setDeviceName(mPath.getText()
									.toString());
							mUpdateWrapper.setBaudrate(Integer
									.valueOf(mBaudrate.getSelectedItem()
											.toString()));
							mUpdateWrapper.connectTarget();
							mAutoupdate.setEnabled(true);
							{
								mUpdateWrapper
										.setOnEventAvailableListener(new OnEventAvailableListener() {
											public void OnEventAvailable(
													final Message msg) {
												switch (msg.what) {
												case BarCodeSerialUpdateWrapper.EventTypeUpgradeSuccess:
													Toast.makeText(
															MainActivity.this,
															"升级完成！", 0).show();

													mLog.append("升级完成！"
															+ System.getProperty("line.separator"));
													mDetaShow_tv.setText(mLog);
													if (mAutoUpdate) {
														Toast.makeText(
																MainActivity.this,
																"30s后连续升级", 0)
																.show();
														mTimeFirst.start();
													} else {
														mHandler.removeCallbacks(mRunnable);
													}
													Log.d(TAG,
															"Event EventTypeUpgradeSuccess");
													break;
												case BarCodeSerialUpdateWrapper.EventTypeUpgradeFail:
													mLog.append("设备连接失败!!!"
															+ System.getProperty("line.separator"));
													mDetaShow_tv.setText(mLog);
													mConnect_btn.performClick();
													mLog.append("正在尝试重新连接..."
															+ System.getProperty("line.separator"));
													mDetaShow_tv.setText(mLog);
													new Handler().postDelayed(
															new Runnable() {

																@Override
																public void run() {
																	mUpdate_btn
																			.performClick();
																}
															}, 5000);
													Log.d(TAG,
															"Event EventTypeUpgradeFail!!!");
													break;
												case BarCodeSerialUpdateWrapper.EventTypeUpgradeProgress:
													mLoadprogres
															.setProgress(msg.arg1);
													mLoadprogres_tv
															.setText(msg.arg1
																	+ "%");
													Log.d(TAG,
															"Event EventTypeUpgradeProgress "
																	+ msg.arg1
																	+ "%");
													break;
												case BarCodeSerialUpdateWrapper.EventTypeTargetConnected:
													Toast.makeText(
															MainActivity.this,
															"设备连接成功！", 0)
															.show();
													mLog.append("设备连接成功！"
															+ System.getProperty("line.separator"));
													mDetaShow_tv.setText(mLog);
													mBaudrate.setEnabled(true);
													mPath.setEnabled(true);
													mDisconnect_btn
															.setEnabled(true);
													mGetversion_btn
															.setEnabled(true);
													mLoadfile_btn1
															.setEnabled(true);
													mLoadfile_btn2
															.setEnabled(true);
													mUpdate_btn
															.setEnabled(true);
													Log.d(TAG,
															"Event EventTypeTargetConnected");
													break;
												case BarCodeSerialUpdateWrapper.EventTypeTargetDisConnected:
													mLoadprogres.setProgress(0);
													mLoadprogres_tv
															.setText(0 + "%");
													mHandler.removeCallbacks(mRunnable);
													mLog.append("设备已断开"
															+ System.getProperty("line.separator"));
													mDetaShow_tv.setText(mLog);
													mBaudrate.setEnabled(false);
													mPath.setEnabled(false);
													mConnect_btn
															.setEnabled(true);
													mDisconnect_btn
															.setEnabled(false);
													mGetversion_btn
															.setEnabled(false);
													mLoadfile_btn1
															.setEnabled(false);
													mLoadfile_btn2
															.setEnabled(false);
													Log.d(TAG,
															"Event EventTypeTargetDisConnected");
													break;
												}
											}
										});
							}
						} catch (Exception e) {
							// TODO Auto-generated catch block
							mLog.append("连接失败，请确认设备节点和手机root情况"
									+ System.getProperty("line.separator"));
							mDetaShow_tv.setText(mLog);
							Toast.makeText(MainActivity.this, "连接失败！", 0)
									.show();
							e.printStackTrace();
						}

					} else {
						mLog.append("请填写连接信息"
								+ System.getProperty("line.separator"));
						mDetaShow_tv.setText(mLog);
						Toast.makeText(this, "请填写连接信息", Toast.LENGTH_SHORT)
								.show();
					}
				}
				break;
			case R.id.disconnect:
				if (mUpdateWrapper.isConnected()) {
					try {
						if (mUpdateWrapper.disConnectTarget()) {
							mLog.append("断开成功！"
									+ System.getProperty("line.separator"));
							mDetaShow_tv.setText(mLog);
							mBaudrate.setEnabled(true);
							mPath.setEnabled(true);
							mConnect_btn.setEnabled(true);
							mDisconnect_btn.setEnabled(false);
							mGetversion_btn.setEnabled(false);
							mLoadfile_btn1.setEnabled(false);
							mLoadfile_btn2.setEnabled(false);
							mUpdate_btn.setEnabled(false);
							Toast.makeText(MainActivity.this, "断开成功！", 0)
									.show();
						} else {
							mLog.append("断开失败！"
									+ System.getProperty("line.separator"));
							mDetaShow_tv.setText(mLog);
							Toast.makeText(MainActivity.this, "断开失败！", 0)
									.show();
						}
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else {
					mLog.append("设备未连接！" + System.getProperty("line.separator"));
					mDetaShow_tv.setText(mLog);
					Toast.makeText(this, "设备未连接!", Toast.LENGTH_SHORT).show();
				}
				break;
			case R.id.getversion:
				if (mUpdateWrapper.isConnected()) {
					Toast.makeText(MainActivity.this,
							"当前版本：" + mUpdateWrapper.getTargetVersion(),
							Toast.LENGTH_SHORT).show();
					mLog.append("应用版本：" + mUpdateWrapper.getTargetVersion()
							+ System.getProperty("line.separator"));
					mDetaShow_tv.setText(mLog);
					mLog.append("硬件名称："
							+ mUpdateWrapper.getTargetHardwareName()
							+ System.getProperty("line.separator"));
					mDetaShow_tv.setText(mLog);
				} else {
					mLog.append("请连接设备！" + System.getProperty("line.separator"));
					mDetaShow_tv.setText(mLog);
					Toast.makeText(this, "设备未连接", Toast.LENGTH_SHORT).show();
				}
				break;
			case R.id.loadfile1:
				if (mUpdateWrapper.isConnected()) {
					startActivityForResult(mIntent, 100);
				} else {
					mLog.append("请连接设备！" + System.getProperty("line.separator"));
					mDetaShow_tv.setText(mLog);
					Toast.makeText(this, "设备未连接", Toast.LENGTH_SHORT).show();
				}
				break;
			case R.id.loadfile2:
				if (mUpdateWrapper.isConnected()) {

					startActivityForResult(mIntent, 200);
				} else {
					mLog.append("请连接设备！" + System.getProperty("line.separator"));
					mDetaShow_tv.setText(mLog);
					Toast.makeText(this, "设备未连接", Toast.LENGTH_SHORT).show();
				}
				break;
			case R.id.update:
				if (mUpdateWrapper.isConnected()) {
					if (null != mUri1 && null != mUri2) {
						if (mUpdateWrapper.checkPckage(mUri1, mUri2)) {
							try {
								if (mUpdateWrapper.upgradeTarget(mUri1, mUri2)) {
									mLog.append("正在升级..."
											+ System.getProperty("line.separator"));
									Toast.makeText(this, "正在升级",
											Toast.LENGTH_SHORT).show();
									mBaudrate.setEnabled(false);
									mPath.setEnabled(false);
									mConnect_btn.setEnabled(false);
									mDisconnect_btn.setEnabled(false);
									mGetversion_btn.setEnabled(false);
									mLoadfile_btn1.setEnabled(false);
									mLoadfile_btn2.setEnabled(false);
									mUpdate_btn.setEnabled(false);
								} else {
									mLog.append("升级命令失败"
											+ System.getProperty("line.separator"));
									Toast.makeText(this, "升级命令失败!!!",
											Toast.LENGTH_SHORT).show();
									mBaudrate.setEnabled(true);
									mPath.setEnabled(true);
									mConnect_btn.setEnabled(true);
									mDisconnect_btn.setEnabled(true);
									mGetversion_btn.setEnabled(true);
									mLoadfile_btn1.setEnabled(true);
									mLoadfile_btn2.setEnabled(true);
									mUpdate_btn.setEnabled(true);
								}
								mDetaShow_tv.setText(mLog);

							} catch (Exception e) {
								e.printStackTrace();
							}
						} else {
							mLog.append("安装包、效验包不匹配！"
									+ System.getProperty("line.separator"));
							mDetaShow_tv.setText(mLog);
							Toast.makeText(this, "安装包、效验包不匹配！",
									Toast.LENGTH_SHORT).show();
						}

					} else {
						if (null == mUri1) {
							Toast.makeText(this, "请选择安装包", Toast.LENGTH_SHORT)
									.show();
							mLog.append("安装包未选择"
									+ System.getProperty("line.separator"));
							mDetaShow_tv.setText(mLog);
						} else if (null == mUri2) {
							mLog.append("效验包未选择"
									+ System.getProperty("line.separator"));
							mDetaShow_tv.setText(mLog);
							Toast.makeText(this, "请选择效验包", Toast.LENGTH_SHORT)
									.show();
						} else {
							mLog.append("安装包、效验包未选择"
									+ System.getProperty("line.separator"));
							mDetaShow_tv.setText(mLog);
							Toast.makeText(this, "请选择安装包、效验包",
									Toast.LENGTH_SHORT).show();
						}
						mHandler.removeCallbacks(mRunnable);
					}
				} else {
					if (mAutoConnect) {
						new Handler().postDelayed(new Runnable() {
							@Override
							public void run() {
								mHandler.removeCallbacks(mRunnable);
								mLog.append("设备未连接，已帮你自动连接，下次记得先连接~_~"
										+ System.getProperty("line.separator"));
								mDetaShow_tv.setText(mLog);
								Toast.makeText(MainActivity.this, "设备未连接",
										Toast.LENGTH_SHORT).show();
								mConnect_btn.performClick();
								new Handler().postDelayed(new Runnable() {

									@Override
									public void run() {
										mUpdate_btn.performClick();
									}
								}, 8000);
							}
						}, 200);
						mAutoConnect = true;
					} else {
						mLog.append("设备未连接"
								+ System.getProperty("line.separator"));
						mDetaShow_tv.setText(mLog);
						Toast.makeText(MainActivity.this, "设备未连接",
								Toast.LENGTH_SHORT).show();
					}

				}
				break;
			}
		}
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (null != data) {
			switch (requestCode) {
			case 100:
				mUri1 = getPhotoPathFromContentUri(MainActivity.this,
						data.getData());
				mLoadfile_btn1.setText("更新包已选择！" + mUri1);
				break;
			case 200:
				mUri2 = getPhotoPathFromContentUri(MainActivity.this,
						data.getData());
				mLoadfile_btn2.setText("效验包已选择！" + mUri2);
				break;
			default:
				break;
			}

		}
	}

	private long exitTime = 0;

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK
				&& event.getAction() == KeyEvent.ACTION_DOWN) {
			if ((System.currentTimeMillis() - exitTime) > 2000) {
				Toast.makeText(getApplicationContext(), "再按一次退出程序",
						Toast.LENGTH_SHORT).show();
				exitTime = System.currentTimeMillis();
			} else {
				mHandler.removeCallbacks(mRunnable);
				mAanimation.cancel();
				mTimeFirst.cancel();
				finish();
				System.exit(0);
			}
			return true;
		}
		return super.onKeyDown(keyCode, event);
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
			mAutoupdate.setEnabled(true);
			mDetaShow_tv.startAnimation(new Animation() {
			});
			mLog.append("马上升级请不要断电！" + System.getProperty("line.separator"));
			mDetaShow_tv.setText(mLog);
			mDetaShow_tv.setGravity(Gravity.TOP);
			mDetaShow_tv.setTextSize(15);
			mHandler.postDelayed(mRunnable, 0);
		}

		public void onTick(final long paramLong) {
			mAutoupdate.setEnabled(false);
			// mLog.append("距下次升级还有    " + paramLong / 1000L + "s"
			// + System.getProperty("line.separator"));
			new Handler().postDelayed(new Runnable() {

				@Override
				public void run() {
					mDetaShow_tv.setText(Html.fromHtml("距下次升级还有    "
							+ "<font color='#ff0000' size='25sp'>"
							+ "<font color='#ff0000' size='30sp'>" + "<b>"
							+ paramLong / 1000L + "s" + "</b>" + "</font>"
							+ "</font>"));
					mDetaShow_tv.setGravity(Gravity.CENTER);
					mAanimation.setDuration(900);
					mAanimation.setFillAfter(true);
					mAanimation.setInterpolator(new AccelerateInterpolator());
					mDetaShow_tv.startAnimation(mAanimation);
				}
			}, 200);

		}
	}

	private void MyLog(String msg) {
		boolean MyLog_IsShow = false;
		if (MyLog_IsShow) {
			Log.i(TAG, msg);
		}

	}
}
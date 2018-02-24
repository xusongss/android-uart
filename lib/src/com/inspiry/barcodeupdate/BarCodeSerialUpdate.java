package com.inspiry.barcodeupdate;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.math.BigInteger;
import java.security.MessageDigest;

/**
 * Created by xuss on 2016/11/20.
 */
public class BarCodeSerialUpdate {
	private static String TAG = "BarCodeSerialUpdate";
	private static BarCodeSerialUpdate mUpdate = null;
	//
	// set isCheckAuthorization to false for Test, set it to "true" for
	// realization
	//
	private static boolean mIsCheckAuthorization = true;

	private SerialConfig mConfig;
	private EventHandler mEventHandler;
	private OnEventAvailableListener mOnEventAvailableListener;

	private class EventHandler extends Handler {
		public EventHandler(){
			super();
		}
		@Override
		public void handleMessage(Message msg) {
			if (mOnEventAvailableListener != null) {
				Log.d(TAG, "BarCodeSerialUpdate:EventHandler rev msg");
				mOnEventAvailableListener.OnEventAvailable(msg);
			}
		}
	}
	/**
	 * 
	 * @param strFilePath
	 * @return
	 */
	private  static String getFileMD5(String  strFilePath) {
		File file = new File(strFilePath);
	    if (!file.isFile() || !file.exists()) {
	      return null;
	    }
	    MessageDigest digest = null;
	    FileInputStream in = null;
	    byte buffer[] = new byte[1024];
	    int len;
	    try {
	      digest = MessageDigest.getInstance("MD5");
	      in = new FileInputStream(file);
	      while ((len = in.read(buffer, 0, 1024)) != -1) {
	        digest.update(buffer, 0, len);
	      }
	      in.close();
	    } catch (Exception e) {
	      e.printStackTrace();
	      return null;
	    }
	    return bytesToHexString(digest.digest());
	  }
	private static String bytesToHexString(byte[] src) {
		StringBuilder stringBuilder = new StringBuilder("");
		if (src == null || src.length <= 0) {
			return null;
		}
		for (int i = 0; i < src.length; i++) {
			int v = src[i] & 0xFF;
			String hv = Integer.toHexString(v);
			if (hv.length() < 2) {
				stringBuilder.append(0);
			}
			stringBuilder.append(hv);
		}
		return stringBuilder.toString();
	  }
	/**
	 * 获取文件中的能内容
	 * @param strFilePath
	 * @return
	 */
	  private  static String ReadTxtFile(String strFilePath) {
	    String path = strFilePath;
	    String content = ""; // 文件内容字符串
	    // 打开文件
	    File file = new File(path);
	    
	    if (!file.isFile() || !file.exists()) {
		     return null;
		 }
	    // 如果path是传递过来的参数，可以做一个非目录的判断
	    
	      try {
	        InputStream instream = new FileInputStream(file);
	        if (instream != null) {
	          InputStreamReader inputreader = new InputStreamReader(
	              instream);
	          BufferedReader buffreader = new BufferedReader(inputreader);
	          String line;
	          // 分行读取
	          while ((line = buffreader.readLine()) != null) {
	            content += line;
	          }
	          instream.close();
	        }
	      } catch (java.io.FileNotFoundException e) {
	        Log.d("TestFile", "The File doesn't not exist.");
	      } catch (IOException e) {
	        Log.d("TestFile", e.getMessage());
	      }
	    
	    return content;
	  }
	//
	// Static Method
	//
	public static boolean checkFile(String path, String md5Path) {
		Log.d(TAG, "checkFile: "+path);
		Log.d(TAG, "checkFile: "+md5Path);
		String md5 = getFileMD5(path);
		String md5check = ReadTxtFile(md5Path);
		if(md5 == null || md5check == null)
		{
			return false;
		}
		Log.d(TAG, "md5:"+md5);
		Log.d(TAG, "md5check:"+md5check);
		return md5.equals(md5check);
	}

	public static BarCodeSerialUpdate getInstance() {
		if (mUpdate != null) {
			Log.e(TAG, "Another Instance Is Opening, Close It Before buildInstance!!!");
			return null;
		}
		mUpdate = new BarCodeSerialUpdate();
		return mUpdate;
	}

	public static boolean checkSecurity(String path) throws SecurityException,
			IOException, Exception {
		Log.v(TAG, "checkSecurity path:" + path);
		if (!mIsCheckAuthorization) {
			Log.v(TAG, "checkSecurity successful mIsCheckAuthorization is false");
			return true;
		}
		File device = new File(path);
		if (!device.exists()) {
			Log.v(TAG, "" + path + " is not exists");
			throw new Exception();
		}
		if (!device.canRead() || !device.canWrite()) {
			try {
				Process su;
				su = Runtime.getRuntime().exec("su");
				String cmd = "chmod 666 " + device.getAbsolutePath() + "\n"
						+ "exit\n";
				su.getOutputStream().write(cmd.getBytes());
				if ((su.waitFor() != 0) || !device.canRead()
						|| !device.canWrite()) {
					Log.e(TAG, "checkSecurity su error!!!");
					throw new SecurityException();
				}
			} catch (Exception e) {
				e.printStackTrace();
				throw new SecurityException();
			}
		}
		Log.v(TAG, "checkSecurity successful");
		return true;
	}

	public void setConfig(SerialConfig config) {
		mConfig = config;
	}
	/*
	 * Object Method
	 */
	private BarCodeSerialUpdate() {
		/*
		Looper looper;
		if ((looper = Looper.myLooper()) != null) {
			Log.v(TAG, "mEventHandler inited x1");
			mEventHandler = new EventHandler(looper);
		} else if ((looper = Looper.getMainLooper()) != null) {
			mEventHandler = new EventHandler(looper);
			Log.v(TAG, "mEventHandler inited by myLooper x2");
		} else {
			Log.v(TAG, "mEventHandler null");
			mEventHandler = null;
		}
		*/
		mEventHandler = new EventHandler();
	}

	public boolean open() {
		int ret = 0;
		ret = openNative(new WeakReference<BarCodeSerialUpdate>(this),
				mConfig.getPath(), mConfig.getBaudrate(), mConfig.getParity(),
				mConfig.getStop(), mConfig.getBits());
		if (ret == 0) {
			return true;
		} else {
			return false;
		}
	}

	public boolean close() {
		int ret = 0;
		ret = closeNative();
		if (ret == 0) {
			return true;
		} else {
			return false;
		}
	}

	public String getVersion() {
		return getVersionNative();
	}
	public String getTargetHardwareName(){
		return getTargetHardwareNameNative();
	}

	public boolean update(String path, String md5Path) {
		int ret = updateNative(path, md5Path);
		if (ret == 0) {
			return true;
		} else {
			return false;
		}

	}

	public void setOnEventAvailableListener(OnEventAvailableListener l) {
		mOnEventAvailableListener = l;
	}

	
	/**
	 * Do Not Modify This Method, It Is Used By Native Code!!!
	 */
	@SuppressWarnings({ "UnusedDeclaration" })
	private static void postEventFromNative(int what, int arg1, int arg2,
			Object selfRef) {
		WeakReference weakSelf = (WeakReference) selfRef;
		BarCodeSerialUpdate update = (BarCodeSerialUpdate) weakSelf.get();
		Log.d(TAG, "postEventFromNative:Receive event:" + what);
		if (update == null) {
			return;
		}
		if (update.mEventHandler != null) {
			Message m = update.mEventHandler.obtainMessage();
			m.what = what;
			m.arg1 = arg1;
			m.arg2 = arg2;
			update.mEventHandler.sendMessage(m);
		}
	}

	/*
	 * Native Method
	 */
	private native String getVersionNative();
	
	private native String getTargetHardwareNameNative();

	private native int updateNative(String path, String md5Path);

	private native int openNative(Object weakSelf, String device, int baudrate,
			int parity, int stop, int bits);

	private native int closeNative();

	/**
	 * Called at the time of the class is load
	 */
	private static native void nativeClassInit();

	//
	// Do Not Modify This Field, It Is Used By Native Code!!!
	//
	private long mDeviceNativePointer;
	private long mDeviceNativeCallBackPointer;
	
	static {
		System.loadLibrary("inspiry_update");
		nativeClassInit();
	}

}

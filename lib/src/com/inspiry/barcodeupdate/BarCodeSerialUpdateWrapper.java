package com.inspiry.barcodeupdate;

import android.os.Message;
import android.util.Log;

/**
 * Created by xuss on 2016/11/24.
 */
/**
 * @author xuss
 * @version 1.0.1
 * 
 * For example:
 * <pre>
 * 		private String mDeviceName="ttyS1";
 * 		private int mBaudrate=9600;
 * 		private String mUpgradePackage="/data/upgrade.1.0.5.bin"
 * 		private String mUpgradePackageMD5="/data/upgrade.1.0.5.md5"; 
 * 		private BarCodeSerialUpdateWrapper mUpdatewrapper;
 *  	mUpdatewrapper = BarCodeSerialUpdateWrapper.getInstance();
 *  	mUpdatewrapper.setDeviceName(mDeviceName);
 *  	mUpdatewrapper.setBaudrate(mBaudrate);
 *  	mUpdatewrapper.setOnEventAvailableListener(new OnEventAvailableListener(){
 * 		public void OnEventAvailable(final Message msg){
 * 				switch (msg.what)
 * 					case BarCodeSerialUpdateWrapper.EventTypeUpgradeSuccess:
 * 						...
 * 						break;
 * 					case BarCodeSerialUpdateWrapper.EventTypeUpgradeFail:
 * 						...
 * 						break;
 * 					case BarCodeSerialUpdateWrapper.EventTypeTargetConnected:
 * 						...
 * 						break;
 * 					case BarCodeSerialUpdateWrapper.EventTypeTargetDisConnected:
 * 						...
 * 						break;
 * 					case BarCodeSerialUpdateWrapper.EventTypeUpgradeProgress:
 * 						...
 * 						break;
 * 					...
 *  			}
 *  		});
 *  	mUpdatewrapper.connectTarget();
 *  	...
 *  	mUpdatewrapper.checkPckage(mUpgradePackage, mUpgradePackageMD5);
 *  	...
 *  	mUpdatewrapper.upgradeTarget(mUpgradePackage, mUpgradePackageMD5);
 *  	...
 *  	mUpdatewrapper.getTargetVersion();
 *  	...
 * </pre>
 */

public class BarCodeSerialUpdateWrapper {
	
	//Event define
	/**
	 * what =EventTypeUpgradeSuccess<br/>
	 * arg1 = 0 <br/>
	 * arg2 = 0
	 */
	public static final int EventTypeUpgradeSuccess = 0x01000001;
	/**
	 * what = EventTypeUpgradeFail<br/> 
	 * arg1 = error code<br/> 
	 * arg2 = 0
	 */

	public static final int EventTypeUpgradeFail = 0x01000002;
	/**
	 * what = EventTYpeUpgradeProgress<br/>
	 * arg1 = (int)percent * 100 <br/> 
	 * arg2 = 0 
	 */
	public static final int EventTypeUpgradeProgress = 0x01000003;
	/**
	 * what = EventTypeTargetDisConnected<br/>
	 * arg1 =0<br/>
	 * arg2 = 0
	 */
	public static final int EventTypeTargetDisConnected = 0x01100000;
	
	/**
	 * what = EventTypeTargetConnected<br/>
	 * arg1 = 0<br/>
	 * arg2 = 0
	 */
	public static final int EventTypeTargetConnected = 0x01100001;
	
	
	public static final String gApiVersion = "1.0.1";
	
	private static String TAG = "BarCodeSerialUpdatewrapper";
	private  SerialConfig mConfig;
	private  BarCodeSerialUpdate mUpdate;
	private  OnEventAvailableListener mListener = null;
	private  boolean mConnected = false;
	private  boolean mIsBusy = false;
	
	
	private static BarCodeSerialUpdateWrapper mInstance = null;
	/**
	 * 
	 * 查询BarCodeSerialUpdatewrapper API 版本信息
	 */
	public static String getApiVersion()
	{
		return gApiVersion;
	}
	/**
	 * 获取实例对象
	 * @return 获取实例对象
	 */
	public static BarCodeSerialUpdateWrapper getInstance()
	{
		synchronized (BarCodeSerialUpdateWrapper.class)
		{
			if(mInstance == null)
			{
				mInstance = new BarCodeSerialUpdateWrapper();
				
			}
			return mInstance;
		}
	}
	private BarCodeSerialUpdateWrapper()
	{
		  mConfig = SerialConfig.getInstance();
		  mUpdate = BarCodeSerialUpdate.getInstance();
	}
	/**
	 * 
	 * 查询串口是否和下位机连接
	 */
	public  boolean isConnected() {
		return mConnected;
	}
	/**
	 * 
	 * 查询串口是否正在传输数据，比如传输升级数据
	 */
	public boolean isBusy() {
		return mIsBusy;
	}

	/**
	 * 设置波特率<br/>
	 * 注意:只有在断开和下位机连接时才可以设置波特率
	 * @see #isConnected()
	 * @see #connectTarget()
	 * @param baudrate
	 * @return true 设置成功， false 设置失败
	 */
	public  boolean setBaudrate(int baudrate) {
		synchronized (this) {
			if (mConnected) {
				Log.e(TAG,
						"Can not set baud rate after the device is connected");
				return false;
			} else {
				mConfig.setBaudrate(baudrate);
				Log.d(TAG, "Set baud rate(" + baudrate + ") sucess");
				return true;
			}
		}
	}

	/**
	 * 设置串口设备节点<br/>
	 * 注意:只有在断开和下位机连接时才可以设置串口设备节点
	 * @see #isConnected()
	 * @see #connectTarget()
	 * @param path
	 * @return
	 */
	public  boolean setDeviceName(String path) {
		synchronized (this) {
			if (mConnected) {
				Log.e(TAG,
						"Can not set device path after the device is connected");
				return false;
			} else {
				mConfig.setPath(path);
				Log.d(TAG, "Set device path(" + path + ") sucess");
				return true;
			}
		}
	}
	/**
	 * 获取串口默认设备节点
	 */
	public  String getDeviceName()
	{
		return mConfig.getPath();
	}
	/**
	 * 获取默认波特率
	 */
	public  int getBaudrate()
	{
		return mConfig.getBaudrate();
	}
	/**
	 * 尝试和下位机建立连接
	 * @see #disConnectTarget()
	 * @return true 连接成功 false 失败
	 * @throws Exception 在设备节点缺失或者没有权限读写的情况下会抛出异常
	 */
	public  boolean connectTarget() throws Exception {
		synchronized (this) {
			if (mConnected) {
				Log.e(TAG, "already connected");
				return true;
			}
			mUpdate.setConfig(mConfig);
			BarCodeSerialUpdate.checkSecurity(mConfig.getPath());
			if (!mUpdate.open()) {
				Log.e(TAG, "Connect error!!!");
				mConnected = false;
				throw new Exception();
			} else {
				Log.d(TAG, "Connect target success");
				mConnected = true;
				
				mUpdate.setOnEventAvailableListener(new OnEventAvailableListener() {
					public void OnEventAvailable(Message msg) {
						synchronized (this) {
							if (msg.what <= EventTypeUpgradeFail) {
								mIsBusy = false;
							}
						}
						if (mListener != null)
						{
							mListener.OnEventAvailable(msg);
						}
						if(msg.what == EventTypeUpgradeSuccess ||
								msg.what ==  EventTypeUpgradeFail)
						{
							try {
								Log.d(TAG, "Upgrade finished close target");
								disConnectTarget();
							} catch (Exception e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}
				});
			}
		}
		return mConnected;
	}
	/**
	 * 尝试断开和下位机的连接
	 * @see #connectTarget()
	 * @return true 成功 false 失败
	 * @throws Exception 比如下位机正在升级，会禁止断开连接
	 */
	public boolean disConnectTarget() throws Exception {
		synchronized (this) {
			if(!mConnected)
			{
				Log.d(TAG,"already disConnected");
				return true;
			}
			if (mIsBusy) {
				Log.d(TAG,
						"Can not disconnect target, the target is busy");
				return false;
			}
			if (!mUpdate.close()) {
				throw new Exception();
			}
			mConnected = false;
			mIsBusy = false;
			return true;
		}
	}
	/**
	 * 设置监听器
	 * @param l
	 */
	public void setOnEventAvailableListener(OnEventAvailableListener l) {
		synchronized (this) {
			mListener = l;
		}
	}
	/**
	 * 开始更新下位机软件<br/>
	 * 注意:该方法异步，需要通过事件监听器判断升级结果
	 * @see #setOnEventAvailableListener(OnEventAvailableListener)
	 * @param packagefile 升级包文件
	 * @param md5file 升级包md5文件
	 * @return true 开始升级 false 升级动作失败
	 * @throws Exception
	 */
	public boolean upgradeTarget(String packagefile, String md5file)
			throws Exception {
		synchronized (this) {
			
			if(!mConnected)
			{
				Log.d(TAG,
						"Can not upgrade target,  the target is not connected ");
				return false;
			}
			if ( mIsBusy) {
				Log.d(TAG,"the target is upgrading");
				return true;
			} else {
				Log.d(TAG, "upgrad target packagefile " + packagefile
						+ " md5file" + md5file);
				if (!mUpdate.update(packagefile, md5file)) {
					throw new Exception();
				}
				mIsBusy = true;
			}
			return true;
		}
	}
	
	/**
	 * 获取下位机版本信息
	 * @see #isConnected()
	 * @return 下位机当前版本信息
	 */
	public String getTargetVersion() {
		synchronized (this) {
			if (!mConnected) {
				Log.d(TAG, "Can not get target version for it is disconnected");
				return null;
			} else {
				return mUpdate.getVersion();
			}
		}
	}
	/**
	 *获取下位机硬件名称
	 *@see #isConnected()
	 *@return 下位机硬件名称
	 */
	 public String getTargetHardwareName(){
		synchronized (this) {
			if (!mConnected) {
				Log.d(TAG, "Can not get target HardwareName for it is disconnected");
				return null;
			} else {
				return mUpdate.getTargetHardwareName();
			}
		}
	 }
	
	/**
	 *  校验升级包
	 * @param packagefile 升级包文件
	 * @param md5file 升级包md5文件
	 * @return true 校验成功 false 校验失败
	 */
	public  boolean checkPckage(String packagefile, String md5file) {
		return BarCodeSerialUpdate.checkFile(packagefile, md5file);
	}

}

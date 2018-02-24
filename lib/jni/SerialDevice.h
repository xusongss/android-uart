//
// Created by xuss on 2016/11/20.
//

#ifndef BARCODEUPDATE_SERIALDEVICE_H
#define BARCODEUPDATE_SERIALDEVICE_H

#include "EventListener.h"
#include "Mutex.h"
#include "UpdateThread.h"
#include "Uart.h"

class SerialDevice {
public:
    SerialDevice(const char * device, int baudrate, int parity, int stop, int bits);
    /**
     * All of those method is used by JNI
     */
    int openDevice();
    int closeDevice();
    int upgrade(const char * path, const char * md5path);
    const char * getTargetVersion();
	const char * getProduceName();
    int setEventListener( EventListener * listener);
    const char * getDeviceName();

public:
    /**
     * used for EventListener
     */
    typedef enum
    {
        EventTypeUpgradeSuccess=0x01000001,
        EventTypeUpgradeFail,
        /**
         * arg1 是升级的百分比，是个大概值
         */
        EventTypeUpgradeProgress,
        EventTypeTargetDisConnected = 0x01100000,
        EventTypeTargetConnected = 0x01100001

    }EventType;
private:
    friend class Uart;
    void onEvent( int what, int arg1, int arg2);
private:
    friend class UpdateThread;
    /**
    * All of thos method is used by UpdateThread
    */

   /**
    * upgradeImp
    * this function is always return 0
    */
    int upgradeImp();

private:
    char mDeviceName[128];
    const int mBaudrate;
    const int mParity;
    const int mStop;
    const int mBits;
    EventListener * mListener;
private:
    UpdateThread mUpdateThread;
    Mutex mLock;
    bool mIsUpdateing;
private:
    Uart mUart;
    char mPackagePath[128];
    char mMd5Path[128];
};


#endif //BARCODEUPDATE_SERIALDEVICE_H

//
// Created by xuss on 2016/11/22.
//
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>
#include <string.h>
#include <errno.h>
#ifdef _LINUX_X86_HOST_
    #include <termios.h>//com
#else
	/**
	 * it is inline function, we copy it from android ndk
	 */
    #include "termios.h"//com
#endif
#include "Timer.h"
#include "Com.h"
#include "InspiryLog.h"
#define LOG_TAG "COM"
Com::Com(const char * name ): mName(name),mHandle(-1)
{
}
Com::Com(const char *name, int baud, int parity, int stop, int bits) :
        mName(name),
        mBaudrate(baud),
        mParity(parity),
        mBits(bits)
{
}
int Com::open() {
    LOGD(LOG_TAG, "com %s open...", mName);

    Mutex::Autolock _l(mLock);
    mHandle = ::open(mName, O_RDWR |O_NONBLOCK |O_NOCTTY | O_NDELAY);
    if (mHandle < 0)
    {
        LOGE(LOG_TAG, "com %s open failed", mName);
        return -1;
    }
    LOGD(LOG_TAG, "com %s open success mHandle(%d)", mName, mHandle);

    setConfig(Com::ComDefaultBaudrate,
                Com::ComDefaultBits,
                Com::ComDefaultStop,
                Com::ComDefaultParity);

    return 0;
}
int Com::close() {
    int ret = 0;
    LOGD(LOG_TAG, "com %s closed!!!", mName);

    Mutex::Autolock _l(mLock);
    ret = ::close(mHandle);
    mHandle = -1;
    return ret;
}

int Com::isOpen() {
    Mutex::Autolock _l(mLock);
    return mHandle != -1;
}

BYTE Com::ReadCom(int  handle, BYTE *pBuffer, DWORD len, int time)
{
    int ret = 0;
    //const int waitTime = 500*1000;
    int remainLen = UART_PACKET_SIZE;
    int readLen = 0;
    BYTE *buffer = pBuffer;
	
    Mutex::Autolock _l(mLock);

    do{
        struct timeval timeout={time,0};
        FD_ZERO(&mFdset);
        FD_SET(mHandle, &mFdset);
		
	usecs_t begin_time = systemTime();
		
        ret = select(mHandle+1,&mFdset,NULL,NULL,&timeout);
        if(ret == -1){
            LOGE(LOG_TAG, "ReadCom error select error");
            return false;
        }else if(ret == 0){
            LOGE(LOG_TAG, "ReadCom error time off(%ds) remainLen(%d) time consumed(%lld us)", time, remainLen, systemTime()-begin_time);
            return false;
        } else{
            if(!FD_ISSET(mHandle,&mFdset))
            {
                LOGE(LOG_TAG, "ReadCom error mHandle is out of FDSET");
                return false;
            }
			usecs_t current_time = systemTime();
			if((current_time - begin_time) > 3000000){
				LOGW(LOG_TAG, "some thing consuming time > 3000000 us");
			}
            ret = read(mHandle, buffer, remainLen);
            if(ret < 0)
            {
				if(errno ==  EAGAIN)
				{
					LOGE(LOG_TAG,"ReadCom mHandle(%d) ret(%d) errno(%d EAGAIN) ", mHandle, ret, errno);
					usleep(150*1000);
				}
				else
				{
					LOGE(LOG_TAG,"ReadCom mHandle(%d) ret(%d) errno(%d) ", mHandle, ret, errno);
					return false;
				}
            }
	if(ret == 0){
		LOGE(LOG_TAG,"Fuck this big error! why did u read 0 byte! errno(%d) ",errno);
	}
            if(ret > 0)
            {
            	//LOGD(LOG_TAG, "ReadCom len %d", readLen);
                buffer += ret;
                readLen += ret;
                remainLen -= ret;
            }
            if(readLen >= 4 &&
               pBuffer[0] != 0x55 &&
               pBuffer[1] != 0xaa &&
               pBuffer[2] != 0x5a &&
               pBuffer[3] != 0xa5)
            {
                LOGE(LOG_TAG, "ReadCom error(first btyes 0x%x, 0x%x, 0x%x, 0x%x,)",
                     pBuffer[0],pBuffer[1],pBuffer[2],pBuffer[3]);
                return false;
            }
        }
    }while(remainLen);

    return true;
}
BYTE Com::WriteCom(int  handle, BYTE *pBuffer, DWORD len)
{
    Mutex::Autolock _l(mLock);
    int ret = 0;
    const int waitTime = 500*1000;
    do{
            struct timeval timeout={3,waitTime};
            FD_ZERO(&mFdset);
            FD_SET(mHandle, &mFdset);
            ret = select(mHandle+1,NULL,&mFdset,NULL,&timeout);
            if(ret == -1){
                LOGE(LOG_TAG, "WriteCom error select error");
                return false;
            }else if(ret == 0){
                LOGE(LOG_TAG, "WriteCom error time off(%d)", waitTime);
                return false;
            } else{
                if(!FD_ISSET(mHandle,&mFdset))
                {
                    LOGE(LOG_TAG, "WriteCom error mHandle is out of FDSET");
                    return false;
                }
                ret = write(mHandle, pBuffer, len);
                if(ret < 0)
				{
                	LOGE(LOG_TAG,"WriteCom mHandle(%d) ret(%d) errno(%d) ", mHandle, ret, errno);
				}
                if(ret > 0)
                {
                     pBuffer += ret;
                     len -= ret;
                }
            }
    }while(len);
    //LOGD(LOG_TAG,"Write UartOK %d", ret);
    return true;
}

BYTE Com::WriteComDevice(BYTE *DataBuf, int len) {
    BYTE g_TempBuf[512 + 4] = {0};
    DWORD WritenLen = 0;
    DataBuf[0] = VENDOR_OUT_ID;
    memcpy(&g_TempBuf[4], DataBuf, len);
    g_TempBuf[0] = 0XAA;
    g_TempBuf[1] = 0X55;
    g_TempBuf[2] = 0XA5;
    g_TempBuf[3] = 0X5A;
    if(WriteCom(mHandle, g_TempBuf, len + 4) != true)
    {
        return false;
    }
    usleep(500);
    return true;
}

BYTE Com::ReadComDevice(BYTE *DataBuf, int len, int timeout) {
    int ret = true;
    BYTE g_TempBuf[512 + 4] = {0};
    DWORD RcvLen = len + 4;
    memset(DataBuf, 0, RcvLen);
    DataBuf[0] = VENDOR_IN_ID;

    if (ReadCom(mHandle, g_TempBuf, RcvLen, timeout) != true)
    {
        ret = false;
    }
    memcpy(DataBuf, &g_TempBuf[4], len);
    return ret;
}

int Com::setConfig(int baudrate, int bits, int stop,int parity ) {
    struct termios new_cfg;
    struct termios old_cfg;
    int speed;
    if (tcgetattr(mHandle, &old_cfg) != 0)
    {
        LOGE(LOG_TAG, "tcgetattr error!!!");
        return -1;
    }
    //设置字符大小
	
    new_cfg = old_cfg;
    cfmakeraw(&new_cfg);
    new_cfg.c_cflag &= ~CSIZE;

    speed = covBaudrate(baudrate);

    cfsetispeed(&new_cfg, speed);
    cfsetospeed(&new_cfg, speed);

    //设置奇偶校验
	
    switch (parity)
    {
		//奇校
		
		
        case 1:       
		{
            new_cfg.c_cflag |= PARENB;
            new_cfg.c_cflag |= PARODD;
            new_cfg.c_iflag |= (INPCK | ISTRIP);
            break;
        }
		//偶校
		
		
        case 2:
		{
            new_cfg.c_iflag |= (INPCK | ISTRIP);
            new_cfg.c_cflag |= PARENB;
            new_cfg.c_cflag &= ~PARODD;
            break;
        }
		//无奇偶校验位
		
		
        case 0:
        {
            new_cfg.c_cflag &= ~PARENB;
            break;
        }
		//无奇偶校验位
		
		
        default:
        {
            new_cfg.c_cflag &= ~PARENB;
            break;
        }
    }
    //设置停止

	switch (stop)
    {
        case 1:
        {
            new_cfg.c_cflag &= ~CSTOPB;
            break;
        }
        case 2:
        {
            new_cfg.c_cflag |= CSTOPB;
            break;
        }
        default:
        {
            new_cfg.c_cflag &= ~CSTOPB;
            break;
        }
    }
    //设置数据   
	
	switch (bits)
    {
        case 5:
        {
            new_cfg.c_cflag |= CS5;
            break;
        }
        case 6:
        {
            new_cfg.c_cflag |= CS6;
            break;
        }
        case 7:
        {
            new_cfg.c_cflag |= CS7;
            break;
        }
        case 8:
        {
            new_cfg.c_cflag |= CS8;
            break;
        }
        default:
        {
            new_cfg.c_cflag |= CS8;
            break;
        }
    }
    //设置等待时间和最小接收字

    new_cfg.c_cc[VTIME] = 1;
    new_cfg.c_cc[VMIN] = 1;

    //处理未接收字   
	
	tcflush(mHandle, TCIFLUSH);
	
    //激活新配置
	
	
    if ((tcsetattr(mHandle, TCSANOW, &new_cfg)) != 0)
    {
        LOGE(LOG_TAG, "tcsetattr error!!!");
        return -1;
    }
    this->mBaudrate = baudrate;
    this->mParity = parity;
    this->mStop = stop;
    this->mBits = bits;
    LOGD(LOG_TAG, "set setConfig mBaudrate(%d) mBits(%d) mStop(%d) mParity(%d)",
    		this->mBaudrate,
    		this->mBits,
    		this->mStop,
    		this->mParity);
    return 0;
}

int Com::getBaudrate() {
    return this->mBaudrate;
}
int Com::covBaudrate(int baudrate) {
    int speed;
	
    //cov波特

	
    switch (baudrate)
    {
        case 110:
        {
            speed = B110;
            break;
        }
        case 300:
        {
            speed = B300;
            break;
        }
        case 600:
        {
            speed = B600;
            break;
        }
        case 1200:
        {
            speed = B1200;
            break;
        }
        case 2400:
        {
            speed = B2400;
            break;
        }
        case 4800:
        {
            speed = B4800;
            break;
        }
        case 9600:
        {
            speed = B9600;
            break;
        }
		
#if 0
		
        case 14400:
        {
            speed = B14400;
            break;
        }
		
#endif
		
        case 19200:
        {
            speed = B19200;
            break;
        }
        case 38400:
        {
            speed = B38400;
            break;
        }
		
#if 0

        case 56000:
        {
            speed = B56000;
            break;
        }
		
#endif
		
        case 57600:
        {
            speed = B57600;
            break;
        }
        case 115200:
        {
            speed = B115200;
            break;
        }
#if 0

        case 128000:
        {
            speed = B128000;
            break;
        }
        case 256000:
        {
            speed = B256000;
            break;
        }
#endif

        default:
        {
            speed = B57600;
            break;
        }
    }
    return speed;
}

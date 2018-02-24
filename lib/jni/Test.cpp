#include <unistd.h>
#include <string.h>
#include <sys/resource.h>
#include "SerialDevice.h"
#include "InspiryLog.h"

//
// Created by xuss on 2016/11/23.
//
#define LOG_TAG "test"
class EventListenerTest:public EventListener
{
public:
    EventListenerTest()
    {
        mLock.lock();
    }
    void wait()
    {
        Mutex::Autolock _l(mLock);
    }
    void onEvent( int what, int arg1, int arg2)
    {
        if(what == SerialDevice::EventTypeUpgradeSuccess)
        {
            LOGD(LOG_TAG,"SerialDevice::EventTypeUpgradeSuccess");
            mLock.unlock();
        }
        else if(what == SerialDevice::EventTypeUpgradeFail)
        {
            LOGD(LOG_TAG,"SerialDevice::EventTypeUpgradeFail");
			exit(1);
            mLock.unlock();
        }
        else if(what == SerialDevice::EventTypeUpgradeProgress)
        {
            LOGD(LOG_TAG, "upgrade progress =%d%%", arg1);
        }

    }
private:
    Mutex mLock;
};


const char * gFileName = NULL;
const char * gDevName = NULL;

class TestThread :public  Thread
{
public:
    TestThread(const char * name);
    virtual bool        threadLoop();
private:
    const char * mName;
};
TestThread::TestThread(const char *name)
:mName(name)
{

}
bool TestThread::threadLoop() {
    const char * version = NULL;
    EventListenerTest *mListener;
    SerialDevice * mSerialDevice;
    mListener= new EventListenerTest();
    mSerialDevice = new SerialDevice(gDevName, 9600, 0,1,8);
    LOGD(LOG_TAG, "%s   Upgrade test!", mName);
    if(mSerialDevice == NULL)
    {
        LOGE(LOG_TAG, "%s   mSerialDevice init error!!!", mName);
        return 0;
    }
    if(mSerialDevice->openDevice() != 0)
    {
        LOGE(LOG_TAG, "%s   mSerialDevice open error!!!",mName);
        return 0;
    }
    mSerialDevice->setEventListener(mListener);
    if((version = mSerialDevice->getTargetVersion()) == NULL)
    {
        LOGE(LOG_TAG, "%s   mSerialDevice getTargetVersion error!!!", mName);
    }
    else
    {
        LOGD(LOG_TAG, "%s   get version %s",mName, version);
    }
    mSerialDevice->upgrade(gFileName, gFileName);
    mListener->wait();
    delete mListener;
    delete mSerialDevice;
    return false;
}
int main(int argc, const char * argv[]) {


    int i = 0;
    char  testname[128];
    if(argc < 3)
    {
        LOGE(LOG_TAG, "upgrade file name");
        LOGE(LOG_TAG, "serail dev node");
        return 0;
    }
    gFileName = argv[1];
    gDevName = argv[2];


	struct rlimit coredump;
	memset(&coredump, 0, sizeof(struct rlimit));
	coredump.rlim_cur = RLIM_INFINITY;
	coredump.rlim_max = RLIM_INFINITY;
	setrlimit(RLIMIT_CORE, &coredump);

    while (++i) {

        sprintf(testname, ">>>  Test-%d  <<<<<", i);

        LOGD(LOG_TAG, "%s",testname);

        TestThread mTest1(testname);

        mTest1.run();

        mTest1.join();

        sleep(15);
    }


    return 0;
}


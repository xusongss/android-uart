LOCAL_SRC_FILES :=$(patsubst com_inspiry_barcodeupdate%,,$(wildcard *.cpp))
LOCAL_BIN_OUT := ../bin/x86_host
all:
	mkdir -p $(LOCAL_BIN_OUT)
	g++ -D_LINUX_X86_HOST_  -fpermissive -g $(LOCAL_SRC_FILES)  -o $(LOCAL_BIN_OUT)/update_x86_host -lpthread
clean:
	-rm $(LOCAL_BIN_OUT)/update_x86_host

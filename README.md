#  编译和使用说明

---------
##  编译使用ant
```
 ant
```
##  编译输出在out目录
```
    apk 文件用于sdk测试
    tar 文件是sdk
```
##  apk测试
```
android 设备需要root
安装apk
升级文件命名为SmartReader 校验文件命名为SmartReader.md5
将升级文件放到android /data/inspiry 目录
将android /data/inspiry 权限设置为所有用户均可以读写（chmod 777 /data/inspiry  -R）
打开apk，选择好tty 和 波特率 点击连接，成功后点击升级按钮进行升级
```

##   查看log
```
adb logcat -s UpdateThread:V UartCmd:V Uart:V SerialDevice:V BarCodeSerialUpdateNative:V COM:V test:v BarCodeSerialUpdatewrapper:V BarCodeSerialUpdate:V MainActivity:V

```

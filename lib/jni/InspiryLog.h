//
// Created by xuss on 2016/11/22.
//

#ifndef UPGRADE_INSPIRYLOG_H
#define UPGRADE_INSPIRYLOG_H


#ifdef LOGD
#undef LOGD
#endif
#ifdef LOGE
#undef LOGE
#endif

#ifndef _LINUX_X86_HOST_
#include <android/log.h>
#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, __VA_ARGS__);
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, __VA_ARGS__);
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, __VA_ARGS__);
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, __VA_ARGS__);
#else
#include <stdarg.h>
#include <stdio.h>
#define LOGV(tag,  fmt, ...) printf("[VERBOSE] "tag"  "fmt"\n", ##__VA_ARGS__)
#define LOGD(tag,  fmt, ...) printf("[DEBUG] "tag"  "fmt"\n", ##__VA_ARGS__)
#define LOGW(tag,  fmt, ...) printf("[WARN] "tag"  "fmt"\n", ##__VA_ARGS__)
#define LOGE(tag,  fmt, ...) printf("[ERROR] "tag"  "fmt"\n", ##__VA_ARGS__)
#endif





#endif //UPGRADE_INSPIRYLOG_H

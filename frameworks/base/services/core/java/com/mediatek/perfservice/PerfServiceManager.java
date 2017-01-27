/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

package com.mediatek.perfservice;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.os.RemoteException;
import android.util.Log;
import com.mediatek.xlog.Xlog;

import dalvik.system.VMRuntime;

public class PerfServiceManager implements IPerfServiceManager{
    private static final String TAG = "PerfServiceManager";
    private HandlerThread mHandlerThread;
    private PerfServiceThreadHandler mHandler;
    private Context mContext;
    final List<Integer> mTimeList;

    private boolean bDuringTouch;
    private boolean bDuringTouchBoost;
    private static final int TOUCH_BOOST_DURATION_MS = 3000;
    private static final int UI_UPDATE_DURATION_MS = 500;
    private static final int RENDER_BIT = 0x800000;
    private static final float HEAP_UTILIZATION_DURING_FRAME_UPDATE = 0.5f;

    private int mDisplayType;
    private VMRuntime mRuntime;
    private float mDefaultUtilization;

    public static native int nativePerfBoostEnable(int scenario);
    public static native int nativePerfBoostDisable(int scenario);
    public static native int nativePerfNotifyAppState(String packName, String className, int state);
    public static native int nativePerfUserScnReg(int scn_core, int scn_freq, int pid, int tid);
    public static native int nativePerfUserScnRegBigLittle(int scn_core_big, int scn_freq_big, int scn_core_little, int scn_freq_little, int pid, int tid);
    public static native int nativePerfUserScnUnreg(int handle);
    public static native int nativePerfUserGetCapability(int cmd);
    public static native int nativePerfUserRegScn(int pid, int tid);
    public static native int nativePerfUserRegScnConfig(int handle, int cmd, int param_1, int param_2, int param_3, int param_4);
    public static native int nativePerfUserUnregScn(int handle);
    public static native int nativePerfUserScnEnable(int handle);
    public static native int nativePerfUserScnDisable(int handle);
    public static native int nativePerfUserScnResetAll();
    public static native int nativePerfUserScnDisableAll();
    public static native int nativePerfUserScnRestoreAll();
    public static native int nativePerfDumpAll();
    public static native int nativePerfSetFavorPid(int pid);
    public static native int nativePerfRestorePolicy(int pid);
    public static native int nativePerfLevelBoost(int level);

    public class PerfServiceAppState {
        private String mPackName;
        private String mClassName;
        private int mState;

        PerfServiceAppState(String packName, String className, int state) {
            mPackName = packName;
            mClassName = className;
            mState = state;
        }
    }

    //static
    //{
    //    Log.w(TAG, "load libperfservice_jni.so");
    //    System.loadLibrary("perfservice_jni");
    //}

    public PerfServiceManager(Context context) {
        super();
        mContext = context;
        mHandlerThread = new HandlerThread("PerfServiceManager", Process.THREAD_PRIORITY_FOREGROUND);
        mHandlerThread.start();
        Looper looper = mHandlerThread.getLooper();
        if(looper != null) {
            mHandler = new PerfServiceThreadHandler(looper);
        }
        mTimeList = new ArrayList<Integer>();
        bDuringTouch = false;
        bDuringTouchBoost = false;
        mDisplayType = DISPLAY_TYPE_OTHERS;
        mRuntime = VMRuntime.getRuntime();
        mDefaultUtilization = mRuntime.getTargetHeapUtilization();
        log("Created and started PerfService thread");
    }

    public void systemReady() {
        log("systemReady, register ACTION_BOOT_COMPLETED");
    }

    public void boostEnable(int scenario) {
        //log("boostEnable:" + scenario);
        if(IPerfServiceWrapper.SCN_APP_TOUCH == scenario) {
            if(mDisplayType == DISPLAY_TYPE_GAME) {
                nativePerfBoostDisable(scenario);
                return;
            }

            bDuringTouch = true;
            bDuringTouchBoost = true;
            mHandler.stopCheckTouchBoostTimer();
            //mHandler.stopCheckTimer(IPerfServiceWrapper.SCN_SW_FRAME_UPDATE);
            //nativePerfBoostDisable(SCN_SW_FRAME_UPDATE);
        }
        mHandler.stopCheckTimer(scenario);

        Message msg = mHandler.obtainMessage();
        msg.what = PerfServiceThreadHandler.MESSAGE_BOOST_ENABLE;
        msg.arg1 = scenario;
        msg.sendToTarget();
    }

    public void boostDisable(int scenario) {
        //log("boostDisable:" + scenario);
        if(IPerfServiceWrapper.SCN_APP_TOUCH == scenario) {
            bDuringTouch = false;
            mHandler.startCheckTouchBoostTimerMs(TOUCH_BOOST_DURATION_MS);
        }
        mHandler.stopCheckTimer(scenario);

        Message msg = mHandler.obtainMessage();
        msg.what = PerfServiceThreadHandler.MESSAGE_BOOST_DISABLE;
        msg.arg1 = scenario;
        msg.sendToTarget();
    }

    public void boostEnableTimeout(int scenario, int timeout) {
        //log("boostEnableTimeout");
        mHandler.stopCheckTimer(scenario);

        Message msg = mHandler.obtainMessage();
        msg.what = PerfServiceThreadHandler.MESSAGE_BOOST_ENABLE_TIMEOUT;
        msg.arg1 = scenario;
        msg.arg2 = timeout;
        msg.sendToTarget();
    }

    public void boostEnableTimeoutMs(int scenario, int timeout_ms) {
        //log("boostEnableTimeoutMs");
        mHandler.stopCheckTimer(scenario);

        Message msg = mHandler.obtainMessage();
        msg.what = PerfServiceThreadHandler.MESSAGE_BOOST_ENABLE_TIMEOUT_MS;
        msg.arg1 = scenario;
        msg.arg2 = timeout_ms;
        msg.sendToTarget();
    }

    public void notifyAppState(String packName, String className, int state) {
        //log("notifyAppState");
        Message msg = mHandler.obtainMessage();
        msg.what = PerfServiceThreadHandler.MESSAGE_NOTIFY_APP_STATE;
        msg.obj = new PerfServiceAppState(packName, className, state);
        msg.sendToTarget();
    }

    public int userReg(int scn_core, int scn_freq, int pid, int tid) {
        return nativePerfUserScnReg(scn_core, scn_freq, pid, tid);
    }

    public int userRegBigLittle(int scn_core_big, int scn_freq_big, int scn_core_little, int scn_freq_little, int pid, int tid) {
        return nativePerfUserScnRegBigLittle(scn_core_big, scn_freq_big, scn_core_little, scn_freq_little, pid, tid);
    }

    public void userUnreg(int handle) {
        nativePerfUserScnUnreg(handle);
    }

    public int userGetCapability(int cmd) {
        return nativePerfUserGetCapability(cmd);
    }

    public int userRegScn(int pid, int tid) {
        return nativePerfUserRegScn(pid, tid);
    }

    public void userRegScnConfig(int handle, int cmd, int param_1, int param_2, int param_3, int param_4) {
        nativePerfUserRegScnConfig(handle, cmd, param_1, param_2, param_3, param_4);
    }

    public void userUnregScn(int handle) {
        Message msg = mHandler.obtainMessage();
        msg.what = PerfServiceThreadHandler.MESSAGE_USER_UNREG_SCN;
        msg.arg1 = handle;
        msg.sendToTarget();
    }

    public void userEnable(int handle) {
        mHandler.stopCheckUserTimer(handle);

        Message msg = mHandler.obtainMessage();
        msg.what = PerfServiceThreadHandler.MESSAGE_USER_ENABLE;
        msg.arg1 = handle;
        msg.sendToTarget();
    }

    public void userDisable(int handle) {
        mHandler.stopCheckUserTimer(handle);

        Message msg = mHandler.obtainMessage();
        msg.what = PerfServiceThreadHandler.MESSAGE_USER_DISABLE;
        msg.arg1 = handle;
        msg.sendToTarget();
    }

    public void userEnableTimeout(int handle, int timeout) {
        mHandler.stopCheckUserTimer(handle);

        Message msg = mHandler.obtainMessage();
        msg.what = PerfServiceThreadHandler.MESSAGE_USER_ENABLE_TIMEOUT;
        msg.arg1 = handle;
        msg.arg2 = timeout;
        msg.sendToTarget();
    }

    public void userEnableTimeoutMs(int handle, int timeout_ms) {
        mHandler.stopCheckUserTimer(handle);

        Message msg = mHandler.obtainMessage();
        msg.what = PerfServiceThreadHandler.MESSAGE_USER_ENABLE_TIMEOUT_MS;
        msg.arg1 = handle;
        msg.arg2 = timeout_ms;
        msg.sendToTarget();
    }

    public void userResetAll() {
        Message msg = mHandler.obtainMessage();
        msg.what = PerfServiceThreadHandler.MESSAGE_USER_RESET_ALL;
        msg.sendToTarget();
    }

    public void userDisableAll() {
        Message msg = mHandler.obtainMessage();
        msg.what = PerfServiceThreadHandler.MESSAGE_USER_DISABLE_ALL;
        msg.sendToTarget();
    }

    public void userRestoreAll() {
        Message msg = mHandler.obtainMessage();
        msg.what = PerfServiceThreadHandler.MESSAGE_USER_RESTORE_ALL;
        msg.sendToTarget();
    }

    public void dumpAll() {
        nativePerfDumpAll();
    }

    public void setFavorPid(int pid) {
        nativePerfSetFavorPid(pid);
    }

    public void restorePolicy(int pid) {
        nativePerfRestorePolicy(pid);
    }

    public void notifyFrameUpdate(int level) {
        log("notifyFrameUpdate - bDuringTouchBoost:" + bDuringTouchBoost);
        if(level != 0)
            nativePerfSetFavorPid(level | RENDER_BIT);

        if(bDuringTouchBoost == false)
            return;

        mHandler.stopCheckTimer(IPerfServiceWrapper.SCN_SW_FRAME_UPDATE);

        Message msg = mHandler.obtainMessage();
        msg.what = PerfServiceThreadHandler.MESSAGE_BOOST_ENABLE_TIMEOUT_MS;
        msg.arg1 = IPerfServiceWrapper.SCN_SW_FRAME_UPDATE;
        msg.arg2 = UI_UPDATE_DURATION_MS;
        msg.sendToTarget();
    }

    public void notifyDisplayType(int type) {
        log("notifyDisplayType:" + type);
        mDisplayType = type;
    }

    public void notifyUserStatus(int type, int status) {
        log("notifyUserStatus - type:" + type + " status:" + status);

        // TODO
    }

    private class PerfServiceThreadHandler extends Handler {
        private static final int MESSAGE_BOOST_ENABLE               = 0;
        private static final int MESSAGE_BOOST_DISABLE              = 1;
        private static final int MESSAGE_BOOST_ENABLE_TIMEOUT       = 2;
        private static final int MESSAGE_BOOST_ENABLE_TIMEOUT_MS    = 3;
        private static final int MESSAGE_NOTIFY_APP_STATE           = 4;
        private static final int MESSAGE_TIMER_SCN_APP_SWITCH       = 5;
        private static final int MESSAGE_TIMER_SCN_APP_ROTATE       = 6;
        private static final int MESSAGE_TIMER_SCN_SW_CODEC         = 7;
        private static final int MESSAGE_TIMER_SCN_SW_CODEC_BOOST   = 8;
        private static final int MESSAGE_TIMER_SCN_APP_TOUCH        = 9;
        private static final int MESSAGE_TIMER_DONT_USE1            = 10;
        private static final int MESSAGE_TIMER_SCN_SW_FRAME_UPDATE  = 11;
        private static final int MESSAGE_TIMER_TOUCH_BOOST_DURATION = 12;
        private static final int MESSAGE_TIMER_SCN_APP_LAUNCH       = 13;

        private static final int MESSAGE_USER_REG                = 20;
        private static final int MESSAGE_USER_REG_BIG_LITTLE     = 21;
        private static final int MESSAGE_USER_UNREG              = 22;
        private static final int MESSAGE_USER_GET_CAPABILITY     = 23;
        private static final int MESSAGE_USER_REG_SCN            = 24;
        private static final int MESSAGE_USER_REG_SCN_CONFIG     = 25;
        private static final int MESSAGE_USER_UNREG_SCN          = 26;
        private static final int MESSAGE_USER_ENABLE             = 27;
        private static final int MESSAGE_USER_ENABLE_TIMEOUT     = 28;
        private static final int MESSAGE_USER_ENABLE_TIMEOUT_MS  = 29;
        private static final int MESSAGE_USER_DISABLE            = 30;
        private static final int MESSAGE_USER_RESET_ALL          = 31;
        private static final int MESSAGE_USER_DISABLE_ALL        = 32;
        private static final int MESSAGE_USER_RESTORE_ALL        = 33;
        private static final int MESSAGE_DUMP_ALL                = 34;
        private static final int MESSAGE_SET_FAVOR_PID           = 35;
        private static final int MESSAGE_NOTIFY_FRAME_UPDATE     = 36;
        private static final int MESSAGE_SW_FRAME_UPDATE_TIMEOUT = 37;
        private static final int MESSAGE_TOUCH_BOOST_DURATION    = 38;

        private static final int MESSAGE_TIMER_SCN_USER_BASE     = 64; // it should be the last message

        public PerfServiceThreadHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            try {
                switch (msg.what) {
                    case MESSAGE_BOOST_ENABLE:
                    {
                        //log("MESSAGE_BOOST_ENABLE:" + msg.arg1);
                         nativePerfBoostEnable(msg.arg1);
                        break;
                    }

                    case MESSAGE_BOOST_DISABLE:
                    {
                        //log("MESSAGE_BOOST_DISABLE");
                        nativePerfBoostDisable(msg.arg1);
                        break;
                    }

                    case MESSAGE_BOOST_ENABLE_TIMEOUT:
                    {
                        //log("MESSAGE_BOOST_ENABLE_TIMEOUT");
                        nativePerfBoostEnable(msg.arg1);
                        startCheckTimer(msg.arg1, msg.arg2);
                        break;
                    }

                    case MESSAGE_BOOST_ENABLE_TIMEOUT_MS:
                    {
                        if(msg.arg1 == IPerfServiceWrapper.SCN_SW_FRAME_UPDATE) {
                            //log("MESSAGE_BOOST_ENABLE_TIMEOUT_MS [SCN_SW_FRAME_UPDATE] bDuringTouchBoost:" + bDuringTouchBoost);
                            if(bDuringTouchBoost == false)
                                break;

                            // adjust heap utilization
                            mRuntime.setTargetHeapUtilization(HEAP_UTILIZATION_DURING_FRAME_UPDATE);
                            //float utilization = mRuntime.getTargetHeapUtilization();
                            //log("set utilization:" + utilization);
                        }

                        nativePerfBoostEnable(msg.arg1);
                        startCheckTimerMs(msg.arg1, msg.arg2);
                        break;
                    }

                    case MESSAGE_NOTIFY_APP_STATE:
                    {
                        PerfServiceAppState passedObject = (PerfServiceAppState) msg.obj;
                        log("MESSAGE_NOTIFY_APP_STATE");
                        nativePerfNotifyAppState(passedObject.mPackName, passedObject.mClassName, passedObject.mState);
                        passedObject = null;
                        msg.obj = null;
                        break;
                    }

                    case MESSAGE_TIMER_SCN_APP_SWITCH:
                    {
                        log("MESSAGE_TIMER_SCN_APP_SWITCH");
                        nativePerfBoostDisable(SCN_APP_SWITCH);
                        break;
                    }

                    case MESSAGE_TIMER_SCN_APP_ROTATE:
                    {
                        log("MESSAGE_TIMER_SCN_APP_ROTATE");
                        nativePerfBoostDisable(SCN_APP_ROTATE);
                        break;
                    }

                    case MESSAGE_TIMER_SCN_SW_CODEC:
                    {
                        log("MESSAGE_TIMER_SCN_SW_CODEC");
                        nativePerfBoostDisable(SCN_SW_CODEC);
                        break;
                    }

                    case MESSAGE_TIMER_SCN_SW_CODEC_BOOST:
                    {
                        log("MESSAGE_TIMER_SCN_SW_CODEC_BOOST");
                        nativePerfBoostDisable(SCN_SW_CODEC_BOOST);
                        break;
                    }

                    case MESSAGE_TIMER_SCN_APP_TOUCH:
                    {
                        log("MESSAGE_TIMER_SCN_APP_TOUCH");
                        nativePerfBoostDisable(SCN_APP_TOUCH);
                        break;
                    }

                    case MESSAGE_TIMER_SCN_SW_FRAME_UPDATE:
                    {
                        log("MESSAGE_TIMER_SCN_SW_FRAME_UPDATE");
                        //startCheckTouchBoostTimerMs(0);
                        if(bDuringTouch == false)
                            bDuringTouchBoost = false;

                        // adjust heap utilization
                        mRuntime.setTargetHeapUtilization(mDefaultUtilization);
                        float utilization = mRuntime.getTargetHeapUtilization();
                        log("set utilization:" + utilization);

                        nativePerfBoostDisable(SCN_SW_FRAME_UPDATE);
                        stopCheckTouchBoostTimer();
                        break;
                    }

                    case MESSAGE_TIMER_TOUCH_BOOST_DURATION:
                    {
                        log("MESSAGE_TIMER_TOUCH_BOOST_DURATION");
                        bDuringTouchBoost = false;
                        nativePerfBoostDisable(SCN_SW_FRAME_UPDATE);
                        break;
                    }

                    case MESSAGE_USER_ENABLE:
                    {
                        log("MESSAGE_USER_ENABLE: " + msg.arg1);
                        nativePerfUserScnEnable(msg.arg1);
                        break;
                    }

                    case MESSAGE_USER_DISABLE:
                    {
                        log("MESSAGE_USER_DISABLE: " + msg.arg1);
                        nativePerfUserScnDisable(msg.arg1);
                        break;
                    }

                    case MESSAGE_USER_ENABLE_TIMEOUT:
                    {
                        log("MESSAGE_USER_ENABLE_TIMEOUT: " + msg.arg1 + ", " + msg.arg2);
                        nativePerfUserScnEnable(msg.arg1);
                        startCheckUserTimer(msg.arg1, msg.arg2);
                        break;
                    }

                    case MESSAGE_USER_ENABLE_TIMEOUT_MS:
                    {
                        log("MESSAGE_USER_ENABLE_TIMEOUT_MS: " + msg.arg1 + ", " + msg.arg2);
                        nativePerfUserScnEnable(msg.arg1);
                        startCheckUserTimerMs(msg.arg1, msg.arg2);
                        break;
                    }

                    case MESSAGE_USER_UNREG_SCN:
                    {
                        log("MESSAGE_USER_UNREG_SCN: " + msg.arg1);
                        nativePerfUserUnregScn(msg.arg1);
                        break;
                    }

                    case MESSAGE_USER_RESET_ALL:
                    {
                        log("MESSAGE_USER_RESET_ALL");
                        stopAllUserTimer();
                        removeAllUserTimerList();
                        nativePerfUserScnResetAll();
                        break;
                    }

                    case MESSAGE_USER_DISABLE_ALL:
                    {
                        log("MESSAGE_USER_DISABLE_ALL");
                        stopAllUserTimer();
                        nativePerfUserScnDisableAll();
                        break;
                    }

                    case MESSAGE_USER_RESTORE_ALL:
                    {
                        log("MESSAGE_USER_RESTORE_ALL");
                        nativePerfUserScnRestoreAll();
                        break;
                    }

                    case MESSAGE_TIMER_SCN_APP_LAUNCH:
                    {
                        log("MESSAGE_TIMER_SCN_APP_LAUNCH");
                        nativePerfBoostDisable(SCN_APP_LAUNCH);
                        break;
                    }

                    //case MESSAGE_LEVEL_BOOST:
                    //{
                    //    //log("MESSAGE_LEVEL_BOOST");
                    //    startCheckTimerMs(msg.arg1, msg.arg2);
                    //    break;
                    //}

                    default:
                    {
                        log("MESSAGE_TIMER_SCN_USER_BASE:" + msg.what);
                        if(msg.what >= MESSAGE_TIMER_SCN_USER_BASE) {
                            nativePerfUserScnDisable(msg.arg1);
                        }
                        break;
                    }
                }
            } catch (NullPointerException e) {
                loge("Exception in PerfServiceThreadHandler.handleMessage: " + e);
            }
        }

        private void startCheckTimer(int scenario, int timeout) {
            Message msg = this.obtainMessage();
            if((msg.what = getScenarioTimer(scenario)) != -1)
                this.sendMessageDelayed(msg, timeout * 1000);
        }

        private void startCheckTimerMs(int scenario, int timeout_ms) {
            Message msg = this.obtainMessage();
            if((msg.what = getScenarioTimer(scenario)) != -1)
                this.sendMessageDelayed(msg, timeout_ms);
        }

        private void stopCheckTimer(int scenario) {
            int timer = getScenarioTimer(scenario);
            if(timer != -1)
                this.removeMessages(timer);
        }

        private void startCheckTouchBoostTimerMs(int timeout_ms) {
            Message msg = this.obtainMessage();
            msg.what = MESSAGE_TIMER_TOUCH_BOOST_DURATION;
            this.sendMessageDelayed(msg, timeout_ms);
        }

        private void stopCheckTouchBoostTimer() {
            this.removeMessages(MESSAGE_TIMER_TOUCH_BOOST_DURATION);
        }

        private void startCheckUserTimer(int handle, int timeout) {
            Message msg = this.obtainMessage();
            msg.what = MESSAGE_TIMER_SCN_USER_BASE + handle;
            msg.arg1 = handle;
            this.sendMessageDelayed(msg, timeout * 1000);

            if (!mTimeList.contains(handle)) {
                mTimeList.add(handle);
                //log("Add to mTimeList:" + handle);
            }
        }

        private void startCheckUserTimerMs(int handle, int timeout_ms) {
            Message msg = this.obtainMessage();
            msg.what = MESSAGE_TIMER_SCN_USER_BASE + handle;
            msg.arg1 = handle;
            this.sendMessageDelayed(msg, timeout_ms);

            if (!mTimeList.contains(handle)) {
                mTimeList.add(handle);
                //log("Add to mTimeList:" + handle);
            }
        }

        private void stopCheckUserTimer(int handle) {
            int timer = MESSAGE_TIMER_SCN_USER_BASE + handle;
            this.removeMessages(timer);
        }

        private void stopAllUserTimer() {
            for (int i = 0; i < mTimeList.size(); i++) {
                int handle = mTimeList.get(i);
                int timer = MESSAGE_TIMER_SCN_USER_BASE + handle;
                this.removeMessages(timer);
                //log("Stop mTimeList:" + handle);
            }
        }

        private void removeAllUserTimerList() {
            for (int i = mTimeList.size()-1; i >= 0; i--) {
                mTimeList.remove(i);
                //log("Remove mTimeList:" + i);
            }
            //int size = mTimeList.size();
            //log("mTimeList size:" + size);
        }

        private int getScenarioTimer(int scenario) {
            switch(scenario) {
                case SCN_APP_SWITCH:
                    return MESSAGE_TIMER_SCN_APP_SWITCH;

                case SCN_APP_ROTATE:
                    return MESSAGE_TIMER_SCN_APP_ROTATE;

                case SCN_SW_CODEC:
                    return MESSAGE_TIMER_SCN_SW_CODEC;

                case SCN_SW_CODEC_BOOST:
                    return MESSAGE_TIMER_SCN_SW_CODEC_BOOST;

                case SCN_APP_TOUCH:
                    return MESSAGE_TIMER_SCN_APP_TOUCH;

                case SCN_SW_FRAME_UPDATE:
                    return MESSAGE_TIMER_SCN_SW_FRAME_UPDATE;

		case SCN_APP_LAUNCH:
			return MESSAGE_TIMER_SCN_APP_LAUNCH;

                default:
                    return -1;
            }
        }
    }

    private void log(String info) {
        Xlog.d(TAG, "[PerfService] " + info + " ");
    }

    private void loge(String info) {
        Xlog.e(TAG, "[PerfService] ERR: " + info + " ");
    }

}


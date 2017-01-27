package com.mediatek.internal.telephony;

import android.app.ActivityManagerNative;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.os.UserHandle;
import android.telephony.Rlog;
import android.telephony.TelephonyManager;

import com.android.internal.telephony.CommandsInterface.RadioState;
import com.android.internal.telephony.PhoneFactory;

import java.util.concurrent.atomic.AtomicBoolean;

import com.mediatek.internal.telephony.RadioManager;
import com.mediatek.internal.telephony.ltedc.LteDcPhoneProxy;
import com.mediatek.internal.telephony.ltedc.svlte.SvlteModeController;
import com.mediatek.internal.telephony.ltedc.svlte.SvltePhoneProxy;
import com.mediatek.internal.telephony.ltedc.svlte.SvlteRatController;

public class AirplaneRequestHandler extends Handler {
    private static final String LOG_TAG = "AirplaneRequestHandler";
    private Context mContext;
    private Boolean mPendingAirplaneModeRequest;
    private int mPhoneCount;
    private boolean mNeedIgnoreMessageForChangeDone;
    private static final int EVENT_LTE_RADIO_CHANGE_FOR_OFF = 100;
    private static final int EVENT_CDMA_RADIO_CHANGE_FOR_OFF = 101;
    private static final int EVENT_GSM_RADIO_CHANGE_FOR_OFF = 102;
    private static final int EVENT_LTE_RADIO_CHANGE_FOR_AVALIABLE = 103;
    private static final int EVENT_CDMA_RADIO_CHANGE_FOR_AVALIABLE = 104;
    private static final int EVENT_GSM_RADIO_CHANGE_FOR_AVALIABLE = 105;
    private static final String INTENT_ACTION_AIRPLANE_CHANGE_DONE =
            "com.mediatek.intent.action.AIRPLANE_CHANGE_DONE";
    private static final String EXTRA_AIRPLANE_MODE = "airplaneMode";

    private static AtomicBoolean mInSwitching = new AtomicBoolean(false);

    protected boolean allowSwitching() {
        if (mInSwitching.get()) {
            return false;
        }
        return true;
    }

    protected void pendingAirplaneModeRequest(boolean enabled){
        mPendingAirplaneModeRequest = new Boolean(enabled);
    }
    /**
     * The Airplane mode change request handler.
     */
    public AirplaneRequestHandler(Context context) {
        mContext = context;
    }

    protected void monitorAirplaneChangeDone(boolean power, int phoneCount) {
        mNeedIgnoreMessageForChangeDone = false;
        log("monitorAirplaneChangeDone, power = " + power + " mNeedIgnoreMessageForChangeDone = " + mNeedIgnoreMessageForChangeDone);
        mInSwitching.set(true);
        mPhoneCount= phoneCount;
        int phoneId = 0;
        for (int i = 0; i < phoneCount; i++) {
            phoneId = i;
            if (power) {
                if (phoneId == SvlteModeController.getActiveSvlteModeSlotId()) {
                    ((SvltePhoneProxy) PhoneFactory.getPhone(phoneId))
                            .getLtePhone().mCi.registerForRadioStateChanged(this,
                            EVENT_LTE_RADIO_CHANGE_FOR_AVALIABLE, null);
                    ((SvltePhoneProxy) PhoneFactory.getPhone(phoneId))
                            .getNLtePhone().mCi.registerForRadioStateChanged(this,
                            EVENT_CDMA_RADIO_CHANGE_FOR_AVALIABLE, null);
                } else {
                    ((SvltePhoneProxy) PhoneFactory.getPhone(phoneId))
                            .getLtePhone().mCi.registerForRadioStateChanged(this,
                            EVENT_GSM_RADIO_CHANGE_FOR_AVALIABLE, null);
                }
            } else {
                if (phoneId == SvlteModeController.getActiveSvlteModeSlotId()) {
                    ((SvltePhoneProxy) PhoneFactory.getPhone(phoneId))
                            .getLtePhone().mCi.registerForRadioStateChanged(this,
                            EVENT_LTE_RADIO_CHANGE_FOR_OFF, null);
                    ((SvltePhoneProxy) PhoneFactory.getPhone(phoneId))
                            .getNLtePhone().mCi.registerForRadioStateChanged(this,
                            EVENT_CDMA_RADIO_CHANGE_FOR_OFF, null);
                } else {
                    ((SvltePhoneProxy) PhoneFactory.getPhone(phoneId))
                            .getLtePhone().mCi.registerForRadioStateChanged(this,
                            EVENT_GSM_RADIO_CHANGE_FOR_OFF, null);
                }
            }
        }
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
        case EVENT_CDMA_RADIO_CHANGE_FOR_OFF:
        case EVENT_LTE_RADIO_CHANGE_FOR_OFF:
        case EVENT_GSM_RADIO_CHANGE_FOR_OFF:
            if (!mNeedIgnoreMessageForChangeDone) {
                if (msg.what == EVENT_CDMA_RADIO_CHANGE_FOR_OFF) {
                    log("handle EVENT_CDMA_RADIO_CHANGE_FOR_OFF");
                } else if (msg.what == EVENT_LTE_RADIO_CHANGE_FOR_OFF) {
                    log("handle EVENT_LTE_RADIO_CHANGE_FOR_OFF");
                } else if (msg.what == EVENT_GSM_RADIO_CHANGE_FOR_OFF) {
                    log("handle EVENT_GSM_RADIO_CHANGE_FOR_OFF");
                }
                for (int i = 0; i < mPhoneCount; i++) {
                    int phoneId = i;
                    if (!isRadioOff(phoneId)) {
                        log("radio state change, radio not off, phoneId = "
                                + phoneId);
                        return;
                    }
                }
                log("All radio off");
                unMonitorAirplaneChangeDone(true);
                mInSwitching.set(false);
                checkPendingRequest();
            }
            break;
        case EVENT_LTE_RADIO_CHANGE_FOR_AVALIABLE:
        case EVENT_CDMA_RADIO_CHANGE_FOR_AVALIABLE:
        case EVENT_GSM_RADIO_CHANGE_FOR_AVALIABLE:
            if (!mNeedIgnoreMessageForChangeDone) {
                if (msg.what == EVENT_LTE_RADIO_CHANGE_FOR_AVALIABLE) {
                    log("handle EVENT_LTE_RADIO_CHANGE_FOR_AVALIABLE");
                } else if (msg.what == EVENT_CDMA_RADIO_CHANGE_FOR_AVALIABLE) {
                    log("handle EVENT_CDMA_RADIO_CHANGE_FOR_AVALIABLE");
                } else if (msg.what == EVENT_GSM_RADIO_CHANGE_FOR_AVALIABLE) {
                    log("handle EVENT_GSM_RADIO_CHANGE_FOR_AVALIABLE");
                }
                for (int i = 0; i < mPhoneCount; i++) {
                    int phoneId = i;
                    if (!isRadioAvaliable(phoneId)) {
                        log("radio state change, radio not avaliable, phoneId = "
                                + phoneId);
                        return;
                    }
                }
                log("All radio avaliable");
                unMonitorAirplaneChangeDone(false);
                mInSwitching.set(false);
                checkPendingRequest();
            }
            break;
        }
    }

    private boolean isRadioAvaliable(int phoneId) {
        if (phoneId == SvlteModeController.getActiveSvlteModeSlotId()) {
            log("phoneId = " + phoneId + " , in svlte mode "
                    + " , lte radio state = "
                    + ((SvltePhoneProxy) PhoneFactory.getPhone(phoneId))
                            .getLtePhone().mCi.getRadioState()
                    + " , cdma radio state = "
                    + ((SvltePhoneProxy) PhoneFactory.getPhone(phoneId))
                            .getNLtePhone().mCi.getRadioState());
            return ((SvltePhoneProxy) PhoneFactory.getPhone(phoneId))
                    .getLtePhone().mCi.getRadioState() != RadioState.RADIO_UNAVAILABLE
                    && ((SvltePhoneProxy) PhoneFactory.getPhone(phoneId))
                            .getNLtePhone().mCi.getRadioState() != RadioState.RADIO_UNAVAILABLE;
        } else {
            log("phoneId = " + phoneId + " , in csfb mode, lte radio state = "
                    + ((SvltePhoneProxy) PhoneFactory.getPhone(phoneId))
                            .getLtePhone().mCi.getRadioState());
            return ((SvltePhoneProxy) PhoneFactory.getPhone(phoneId))
                    .getLtePhone().mCi.getRadioState() != RadioState.RADIO_UNAVAILABLE;
        }
    }

    private boolean isRadioOff(int phoneId) {
        if (phoneId == SvlteModeController.getActiveSvlteModeSlotId()) {
            log("phoneId = " + phoneId + " , in svlte mode "
                    + " , lte radio state = "
                    + ((SvltePhoneProxy) PhoneFactory.getPhone(phoneId))
                            .getLtePhone().mCi.getRadioState()
                    + " , cdma radio state = "
                    + ((SvltePhoneProxy) PhoneFactory.getPhone(phoneId))
                            .getNLtePhone().mCi.getRadioState());
            return ((SvltePhoneProxy) PhoneFactory.getPhone(phoneId))
                    .getLtePhone().mCi.getRadioState() == RadioState.RADIO_OFF
                    && ((SvltePhoneProxy) PhoneFactory.getPhone(phoneId))
                            .getNLtePhone().mCi.getRadioState() == RadioState.RADIO_OFF;
        } else {
            log("phoneId = " + phoneId + ", in csfb mode, lte radio state = "
                    + ((SvltePhoneProxy) PhoneFactory.getPhone(phoneId))
                            .getLtePhone().mCi.getRadioState());
            return ((SvltePhoneProxy) PhoneFactory.getPhone(phoneId))
                    .getLtePhone().mCi.getRadioState() == RadioState.RADIO_OFF;
        }
    }

    private void checkPendingRequest() {
        log("checkPendingRequest, mPendingAirplaneModeRequest = " + mPendingAirplaneModeRequest);
        if (mPendingAirplaneModeRequest != null) {
            Boolean pendingAirplaneModeRequest = mPendingAirplaneModeRequest;
            mPendingAirplaneModeRequest = null;
            RadioManager.getInstance().notifyAirplaneModeChange(
                    pendingAirplaneModeRequest.booleanValue());
        }
    }

    protected void unMonitorAirplaneChangeDone(boolean airplaneMode) {
        mNeedIgnoreMessageForChangeDone = true;
        Intent intent = new Intent(INTENT_ACTION_AIRPLANE_CHANGE_DONE);
        intent.putExtra(EXTRA_AIRPLANE_MODE, airplaneMode);
        mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        int phoneId = 0;
        for (int i = 0; i < mPhoneCount; i++) {
            phoneId = i;
            if (phoneId == SvlteModeController.getActiveSvlteModeSlotId()) {
                ((SvltePhoneProxy) PhoneFactory.getPhone(phoneId))
                        .getLtePhone().mCi.unregisterForRadioStateChanged(this);
                ((SvltePhoneProxy) PhoneFactory.getPhone(phoneId))
                        .getNLtePhone().mCi
                        .unregisterForRadioStateChanged(this);
                log("unMonitorAirplaneChangeDone, for svlte phone,  phoneId = " + phoneId);
            } else {
                ((SvltePhoneProxy) PhoneFactory.getPhone(phoneId))
                        .getLtePhone().mCi.unregisterForRadioStateChanged(this);
                log("unMonitorAirplaneChangeDone, for csfb phone,  phoneId = " + phoneId);
            }

        }
    }

    private static void log(String s) {
        Rlog.d(LOG_TAG, "[RadioManager] " + s);
    }
}
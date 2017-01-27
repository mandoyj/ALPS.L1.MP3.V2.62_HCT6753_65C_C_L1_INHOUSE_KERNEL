/*
* Copyright (C) 2011-2014 MediaTek Inc.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.mediatek.internal.telephony.gsm;

import android.view.Surface;
import android.telecom.VideoProfile;

import com.mediatek.internal.telephony.gsm.IGsmVideoCallCallback;

/**
 * Internal remote interface for Gsm's video call provider.
 *
 * At least initially, this aidl mirrors telecom's {@link IVideoCallProvider}. We created a
 * separate aidl interface even though the methods and parameters are same because the
 * {@link IVideoCallProvider} was specifically created as a binder for inter-process communication
 * between Telecomm and Telephony.
 *
 * We don't want to use the same aidl in other places for communication, namely communication
 * between Telephony and the Gsm Service, even if that communication may be for similar methods.
 * This decouples the communication among these processes. Similarly, third parties implementing a
 * video call provider will not have the benefit of accessing the internal
 * {@link IVideoCallProvider} aidl for interprocess communication.
 *
 * @see android.telecom.internal.IVideoCallProvider
 * @see android.telecom.VideoCallProvider
 * @hide
 */
oneway interface IGsmVideoCallProvider {
    void setCallback(IGsmVideoCallCallback callback);

    void setCamera(String cameraId);

    void setPreviewSurface(in Surface surface);

    void setDisplaySurface(in Surface surface);

    void setDeviceOrientation(int rotation);

    void setZoom(float value);

    void sendSessionModifyRequest(in VideoProfile reqProfile);

    void sendSessionModifyResponse(in VideoProfile responseProfile);

    void requestCameraCapabilities();

    void requestCallDataUsage();

    void setPauseImage(String uri);

     /// M: For 3G VT only @{
    void setUIMode(int mode);

    void setVTOpen();

    void setVTReady();

    void setVTConnected();

    void setVTClose();

    void onDisconnected();

    void setDisplay(in Surface local, in Surface peer);

    void switchDisplaySurface();

    void setLocalView(int videoType, String path);

    void setPeerView(int bEnableReplacePeerVideo, String sReplacePeerVideoPicturePath);

    void switchCamera();

    void setVTVisible(boolean isVisible);

    void onUserInput(String input);
    /// @}

}

/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2013. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

package com.mediatek.mms.plugin;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.res.Resources;

import com.mediatek.common.PluginImpl;
import com.mediatek.mms.ext.DefaultStorageLowExt;
import com.mediatek.op09.plugin.R;
import com.mediatek.xlog.Xlog;

/**
 * M: Op09StorageLowExt.
 */
@PluginImpl(interfaceName = "com.mediatek.mms.ext.IStorageLowExt")
public class Op09StorageLowExt extends DefaultStorageLowExt {
    private static final String TAG = "Mms/OP09StorageLowExt";
    private static final int STORAGE_LOW_NOTIFICATION_ID = 240;
    private Resources mResources = null;

    /**
     * M: Constructor.
     * @param context the Context.
     */
    public Op09StorageLowExt(Context context) {
        super(context);
        mResources = getResources();
    }

    @Override
    public String getNotificationTitle() {
        Xlog.d(TAG, "OP09StorageLowExt.getNotificationTitle()");
        return mResources.getString(R.string.storage_low_title);
    }

    @Override
    public String getNotificationBody() {
        Xlog.d(TAG, "OP09StorageLowExt.getNotificationBody()");
        return mResources.getString(R.string.storage_low_body);
    }

    @Override
    public void showNotification(NotificationManager notificationMgr, Notification notification) {
        Xlog.d(TAG, "OP09StorageLowExt.showNotification()");
        notificationMgr.notify(STORAGE_LOW_NOTIFICATION_ID, notification);
    }

    @Override
    public void cancelNotification(NotificationManager notificationMgr) {
        Xlog.d(TAG, "OP09StorageLowExt.cancelNotification()");
        notificationMgr.cancel(STORAGE_LOW_NOTIFICATION_ID);
    }
}
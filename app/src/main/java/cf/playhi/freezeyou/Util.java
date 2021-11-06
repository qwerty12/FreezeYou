/*
 * Copyright (C) 2016 The Android Open Source Project
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

package cf.playhi.freezeyou;

import android.annotation.TargetApi;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;

import cf.playhi.freezeyou.utils.DevicePolicyManagerUtils;

/**
 * Common utility functions.
 */
public class Util {
    // TODO(b/179160578): change check once S SDK is launched
    private static final boolean IS_RUNNING_S =
        VERSION.CODENAME.length() == 1 && VERSION.CODENAME.charAt(0) == 'S';

    /**
     * A replacement for {@link VERSION.SDK_INT} that is compatible with pre-release SDKs
     *
     * <p>This will be set to the version SDK, or {@link VERSION_CODES.CUR_DEVELOPMENT} if the SDK
     * int is not yet assigned.
     **/
    public static final int SDK_INT =
        IS_RUNNING_S ? VERSION_CODES.CUR_DEVELOPMENT : VERSION.SDK_INT;

    @TargetApi(VERSION_CODES.LOLLIPOP)
    public static boolean isDeviceOwner(Context context) {
        final DevicePolicyManager dpm = getDevicePolicyManager(context);
        return dpm.isDeviceOwnerApp(context.getPackageName());
    }

    private static DevicePolicyManager getDevicePolicyManager(Context context) {
        return DevicePolicyManagerUtils.getDevicePolicyManager(context);
    }

    public static boolean hasDelegation(Context context, String delegation) {
        if (Util.SDK_INT < VERSION_CODES.O) {
            return false;
        }
        DevicePolicyManager dpm = context.getSystemService(DevicePolicyManager.class);
        return dpm.getDelegatedScopes(null, context.getPackageName()).contains(delegation);
    }
}

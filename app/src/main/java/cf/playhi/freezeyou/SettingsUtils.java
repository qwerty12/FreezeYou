package cf.playhi.freezeyou;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;

import net.grandcentrix.tray.AppPreferences;

import static cf.playhi.freezeyou.fuf.FUFSinglePackage.API_FREEZEYOU_LEGACY_AUTO;
import static cf.playhi.freezeyou.fuf.FUFSinglePackage.API_FREEZEYOU_MROOT_DPM;
import static cf.playhi.freezeyou.fuf.FUFSinglePackage.API_FREEZEYOU_ROOT_DISABLE_ENABLE;
import static cf.playhi.freezeyou.fuf.FUFSinglePackage.API_FREEZEYOU_ROOT_UNHIDE_HIDE;
import static cf.playhi.freezeyou.fuf.FUFSinglePackage.API_FREEZEYOU_SHIZUKU_ENABLE_DISABLE_USER;
import static cf.playhi.freezeyou.fuf.FUFSinglePackage.API_FREEZEYOU_SYSTEM_APP_ENABLE_DISABLE;
import static cf.playhi.freezeyou.fuf.FUFSinglePackage.API_FREEZEYOU_SYSTEM_APP_ENABLE_DISABLE_UNTIL_USED;
import static cf.playhi.freezeyou.fuf.FUFSinglePackage.API_FREEZEYOU_SYSTEM_APP_ENABLE_DISABLE_USER;
import static cf.playhi.freezeyou.utils.AccessibilityUtils.isAccessibilitySettingsOn;
import static cf.playhi.freezeyou.utils.AccessibilityUtils.openAccessibilitySettings;
import static cf.playhi.freezeyou.utils.DevicePolicyManagerUtils.checkAndSetOrganizationName;
import static cf.playhi.freezeyou.utils.DevicePolicyManagerUtils.getDevicePolicyManager;
import static cf.playhi.freezeyou.utils.DevicePolicyManagerUtils.isDeviceOwner;
import static cf.playhi.freezeyou.utils.DevicePolicyManagerUtils.openDevicePolicyManager;
import static cf.playhi.freezeyou.utils.FUFUtils.checkRootPermission;
import static cf.playhi.freezeyou.utils.FUFUtils.isSystemApp;
import static cf.playhi.freezeyou.utils.ServiceUtils.startService;
import static cf.playhi.freezeyou.utils.Support.checkLanguage;
import static cf.playhi.freezeyou.utils.ToastUtils.showToast;

import cf.playhi.freezeyou.utils.ShizukuUtils;

final class SettingsUtils {
    static void syncAndCheckSharedPreference(Context context, Activity activity,
                                             SharedPreferences sharedPreferences, String s,
                                             AppPreferences appPreferences) {
        switch (s) {
            case "firstIconEnabled":
                if (sharedPreferences.getBoolean(s, true)) {
                    context.getPackageManager().setComponentEnabledSetting(new ComponentName(context, "cf.playhi.freezeyou.FirstIcon"),
                            PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
                } else {
                    context.getPackageManager().setComponentEnabledSetting(new ComponentName(context, "cf.playhi.freezeyou.FirstIcon"),
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
                }
                showToast(activity, R.string.ciFinishedToast);
                break;
            case "secondIconEnabled":
                if (sharedPreferences.getBoolean(s, true)) {
                    context.getPackageManager().setComponentEnabledSetting(new ComponentName(context, "cf.playhi.freezeyou.SecondIcon"),
                            PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
                } else {
                    context.getPackageManager().setComponentEnabledSetting(new ComponentName(context, "cf.playhi.freezeyou.SecondIcon"),
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
                }
                showToast(activity, R.string.ciFinishedToast);
                break;
            case "thirdIconEnabled":
                if (sharedPreferences.getBoolean(s, true)) {
                    context.getPackageManager().setComponentEnabledSetting(new ComponentName(context, "cf.playhi.freezeyou.ThirdIcon"),
                            PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
                } else {
                    context.getPackageManager().setComponentEnabledSetting(new ComponentName(context, "cf.playhi.freezeyou.ThirdIcon"),
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
                }
                showToast(activity, R.string.ciFinishedToast);
                break;
            case "shortCutOneKeyFreezeAdditionalOptions":
                appPreferences.put(s, sharedPreferences.getString(s, "nothing"));
                if (!"nothing".equals(sharedPreferences.getString(s, "nothing"))) {
                    DevicePolicyManager devicePolicyManager = getDevicePolicyManager(context);
                    if (devicePolicyManager != null && !devicePolicyManager.isAdminActive(
                            new ComponentName(context, DeviceAdminReceiver.class))) {
                        openDevicePolicyManager(activity);
                    }
                }
                break;
            case "uiStyleSelection":
            case "allowFollowSystemAutoSwitchDarkMode":
                showToast(activity, R.string.willTakeEffectsNextLaunch);
                activity.recreate();
                break;
            case "onekeyFreezeWhenLockScreen":
                appPreferences.put(s, sharedPreferences.getBoolean(s, false));
                if (sharedPreferences.getBoolean(s, false)) {
                    startService(
                            context,
                            new Intent(context, ScreenLockOneKeyFreezeService.class));
                } else {
                    context.stopService(new Intent(context, ScreenLockOneKeyFreezeService.class));
                }
                break;
            case "freezeOnceQuit":
            case "avoidFreezeForegroundApplications":
            case "tryToAvoidUpdateWhenUsing":
                appPreferences.put(s, sharedPreferences.getBoolean(s, false));
                if (sharedPreferences.getBoolean(s, false) && !isAccessibilitySettingsOn(context)) {
                    showToast(activity, R.string.needActiveAccessibilityService);
                    openAccessibilitySettings(context);
                }
                break;
            case "useForegroundService":// 使用前台服务
            case "openImmediately":// 解冻后立即打开
            case "openAndUFImmediately":// 快捷方式立即解冻并启动
            case "notificationBarDisableSlideOut":// 禁止滑动移除
            case "notificationBarDisableClickDisappear":// 禁止点击消失
            case "lesserToast":// 更少的 Toast
            case "debugModeEnabled":// 启用 调试模式
            case "tryDelApkAfterInstalled"://安装完成后尝试删除安装包
                appPreferences.put(s, sharedPreferences.getBoolean(s, false));
                break;
            case "notificationBarFreezeImmediately":
            case "showInRecents":
            case "notAllowInstallWhenIsObsd":
            case "createQuickFUFNotiAfterUnfrozen":
                appPreferences.put(s, sharedPreferences.getBoolean(s, true));
                break;
            case "organizationName":
                checkAndSetOrganizationName(context, sharedPreferences.getString(s, null));
                break;
            case "avoidFreezeNotifyingApplications":
                appPreferences.put(s, sharedPreferences.getBoolean(s, false));
                if (Build.VERSION.SDK_INT >= 21) {//这项不兼容 5.0 以下了
                    String enabledNotificationListeners = Settings.Secure.getString(context.getContentResolver(), "enabled_notification_listeners");
                    if (enabledNotificationListeners != null && !enabledNotificationListeners.contains("cf." + "playhi." + "freezeyou")) {
                        try {
                            activity.startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
                        } catch (Exception e) {
                            showToast(activity, R.string.failed);
                        }
                    }
                }
                break;
            case "enableInstallPkgFunc":
                if (sharedPreferences.getBoolean(s, true)) {
                    context.getPackageManager().setComponentEnabledSetting(new ComponentName(context, "cf.playhi.freezeyou.InstallPackagesActivity"),
                            PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
                } else {
                    context.getPackageManager().setComponentEnabledSetting(new ComponentName(context, "cf.playhi.freezeyou.InstallPackagesActivity"),
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
                }
                break;
            case "languagePref":
                checkLanguage(activity);
                showToast(activity, R.string.willTakeEffectsNextLaunch);
                break;
            case "selectFUFMode":
                appPreferences.put(s, Integer.parseInt(sharedPreferences.getString(s, "0")));
                switch (appPreferences.getInt("selectFUFMode", 0)) {
                    case API_FREEZEYOU_MROOT_DPM:
                        if (!isDeviceOwner(context)) {
                            showToast(context, R.string.noMRootPermission);
                        }
                        break;
                    case API_FREEZEYOU_ROOT_DISABLE_ENABLE:
                    case API_FREEZEYOU_ROOT_UNHIDE_HIDE:
                        if (!checkRootPermission()) {
                            showToast(context, R.string.noRootPermission);
                        }
                        break;
                    case API_FREEZEYOU_LEGACY_AUTO:
                        if (!(checkRootPermission() || isDeviceOwner(context))) {
                            showToast(context, R.string.insufficientPermission);
                        }
                        break;
                    case API_FREEZEYOU_SYSTEM_APP_ENABLE_DISABLE_UNTIL_USED:
                    case API_FREEZEYOU_SYSTEM_APP_ENABLE_DISABLE_USER:
                    case API_FREEZEYOU_SYSTEM_APP_ENABLE_DISABLE:
                        if (!(isSystemApp(context))) {
                            showToast(context, R.string.insufficientPermission);
                        }
                        break;
                    case API_FREEZEYOU_SHIZUKU_ENABLE_DISABLE_USER:
                        if (!ShizukuUtils.isShizukuInstalled(context)) {
                            showToast(context, "Shizuku not installed");
                            if (!ShizukuUtils.supportsShizuku())
                                showToast(context, "Android version too old for Shizuku");
                        } else {
                            try {
                                ShizukuUtils.requestPermission();
                            } catch (IllegalStateException e) {
                                showToast(context, "Shizuku not running");
                            }
                        }
                        break;
                    default:
                        showToast(context, R.string.unknown);
                        break;
                }
                break;
            case "mainActivityPattern":
                showToast(activity, R.string.willTakeEffectsNextLaunch);
                break;
            default:
                break;
        }
    }
}

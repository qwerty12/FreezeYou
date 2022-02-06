package cf.playhi.freezeyou.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.view.SubMenu;
import android.view.View;
import android.widget.EditText;
import android.widget.PopupMenu;

import net.grandcentrix.tray.AppPreferences;

import java.util.HashMap;
import java.util.Locale;

import cf.playhi.freezeyou.DeviceAdminReceiver;
import cf.playhi.freezeyou.ForceStop;
import cf.playhi.freezeyou.Freeze;
import cf.playhi.freezeyou.InstallPackagesActivity;
import cf.playhi.freezeyou.R;

import static cf.playhi.freezeyou.LauncherShortcutUtils.checkSettingsAndRequestCreateShortcut;
import static cf.playhi.freezeyou.utils.AlertDialogUtils.buildAlertDialog;
import static cf.playhi.freezeyou.utils.ApplicationIconUtils.getApplicationIcon;
import static cf.playhi.freezeyou.utils.ClipboardUtils.copyToClipboard;
import static cf.playhi.freezeyou.utils.OneKeyListUtils.addToOneKeyList;
import static cf.playhi.freezeyou.utils.OneKeyListUtils.existsInOneKeyList;
import static cf.playhi.freezeyou.utils.OneKeyListUtils.removeFromOneKeyList;
import static cf.playhi.freezeyou.utils.ToastUtils.showToast;

public final class Support {

    private static void makeDialog(final String title, final String message, final Context context, final ApplicationInfo applicationInfo, final String pkgName, final String target, final String tasks, final boolean enabled, final Activity activity, final boolean finish) {
        AlertDialog.Builder builder =
                buildAlertDialog(context, getApplicationIcon(context, pkgName, applicationInfo, true), message, title)
                        .setNeutralButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                FUFUtils.checkAndDoActivityFinish(activity, finish);
                            }
                        })
                        .setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialogInterface) {
                                FUFUtils.checkAndDoActivityFinish(activity, finish);
                            }
                        });
        if (enabled) {
            builder.setPositiveButton(R.string.launch, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    FUFUtils.checkAndStartApp(context, pkgName, target, tasks, activity, finish);
                }
            });
            builder.setNegativeButton(R.string.freeze, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    FUFUtils.processFreezeAction(context, pkgName, target, tasks, true, activity, finish);
                }
            });
        } else {
            builder.setPositiveButton(R.string.unfreeze, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    FUFUtils.processUnfreezeAction(context, pkgName, target, tasks, true, false, activity, finish);
                }
            });
        }
        builder.create().show();
    }

    public static void shortcutMakeDialog(Context context, String title, String message, final Activity activity, final ApplicationInfo applicationInfo, final String pkgName, String target, String tasks, int ot, boolean auto, boolean finish) {
        if (new AppPreferences(context).getBoolean("openAndUFImmediately", false) && auto) {
            if (ot == 2) {
                FUFUtils.checkAndStartApp(context, pkgName, target, tasks, activity, finish);
            } else {
                FUFUtils.processUnfreezeAction(context, pkgName, target, tasks, true, true, activity, finish);//ot==1
            }
        } else {
            makeDialog(title, message, context, applicationInfo, pkgName, target, tasks, ot == 2, activity, finish);
        }
    }

    public static void checkAddOrRemove(Context context, String pkgNames, String pkgName, String oneKeyName) {
        if (existsInOneKeyList(pkgNames, pkgName)) {
            showToast(context,
                    removeFromOneKeyList(context,
                            oneKeyName,
                            pkgName) ? R.string.removed : R.string.removeFailed);
        } else {
            showToast(context,
                    addToOneKeyList(context,
                            oneKeyName,
                            pkgName) ? R.string.added : R.string.addFailed);
            if (context.getString(R.string.sFreezeOnceQuit).equals(oneKeyName)) {
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
                if (!preferences.getBoolean("freezeOnceQuit", false)) {
                    preferences.edit().putBoolean("freezeOnceQuit", true).apply();
                    new AppPreferences(context).put("freezeOnceQuit", true);
                }
                AccessibilityUtils.checkAndRequestIfAccessibilitySettingsOff(context);
            }
        }
    }

    public static void showChooseActionPopupMenu(final Context context, Activity activity, View view, final String pkgName, final String name) {
        showChooseActionPopupMenu(context, activity, view, pkgName, name, false, null);
    }

    public static void showChooseActionPopupMenu(final Context context, Activity activity, View view, final String pkgName, final String name, boolean canRemoveItem, final SharedPreferences folderPkgListSp) {
        generateChooseActionPopupMenu(context, activity, view, pkgName, name, canRemoveItem, folderPkgListSp).show();
    }

    private static PopupMenu generateChooseActionPopupMenu(final Context context, final Activity activity, View view, final String pkgName, final String name, final boolean canRemoveItem, final SharedPreferences folderPkgListSp) {
        PopupMenu popup = new PopupMenu(context, view);
        popup.inflate(R.menu.main_single_choose_action_menu);

        SubMenu vmUserDefinedSubMenu = popup.getMenu().findItem(R.id.main_sca_userDefined).getSubMenu();
        vmUserDefinedSubMenu.clear();
        vmUserDefinedSubMenu.add(
                R.id.main_sca_menu_userDefined_menuGroup,
                R.id.main_sca_menu_userDefined_newClassification,
                0,
                R.string.newClassification
        ); // 加入“新建分类”

        final HashMap<Integer, String> userDefinedCategoriesHashMap = new HashMap<>();

        SQLiteDatabase vmUserDefinedDb = context.openOrCreateDatabase("userDefinedCategories", Context.MODE_PRIVATE, null);
        vmUserDefinedDb.execSQL(
                "create table if not exists categories(_id integer primary key autoincrement,label varchar,packages varchar)"
        );
        Cursor cursor = vmUserDefinedDb.query("categories", new String[]{"label", "_id", "packages"}, null, null, null, null, null);
        if (cursor.moveToFirst()) {
            for (int i = 0; i < cursor.getCount(); i++) {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("_id"));
                String title = cursor.getString(cursor.getColumnIndexOrThrow("label"));
                userDefinedCategoriesHashMap.put(id, cursor.getString(cursor.getColumnIndexOrThrow("packages")));
                vmUserDefinedSubMenu.add(R.id.main_sca_menu_userDefined_menuGroup, id, id, new String(Base64.decode(title, Base64.DEFAULT)));
                cursor.moveToNext();
            }
        }
        cursor.close();
        vmUserDefinedDb.close();

        final AppPreferences sharedPreferences = new AppPreferences(context);

        final String pkgNames = sharedPreferences.getString(context.getString(R.string.sAutoFreezeApplicationList), "");
        if (existsInOneKeyList(pkgNames, pkgName)) {
            popup.getMenu().findItem(R.id.main_sca_menu_addToOneKeyList).setTitle(R.string.removeFromOneKeyList);
        }

        final String FreezeOnceQuitPkgNames = sharedPreferences.getString(context.getString(R.string.sFreezeOnceQuit), "");
        if (existsInOneKeyList(FreezeOnceQuitPkgNames, pkgName)) {
            popup.getMenu().findItem(R.id.main_sca_menu_addToFreezeOnceQuit).setTitle(R.string.removeFromFreezeOnceQuit);
        }

        final String UFPkgNames = sharedPreferences.getString(context.getString(R.string.sOneKeyUFApplicationList), "");
        if (existsInOneKeyList(UFPkgNames, pkgName)) {
            popup.getMenu().findItem(R.id.main_sca_menu_addToOneKeyUFList).setTitle(R.string.removeFromOneKeyUFList);
        }

        final String ONPkgNames = sharedPreferences.getString(context.getString(R.string.sOngoingNotificationCheckList), "");
        if (existsInOneKeyList(ONPkgNames, pkgName)) {
            popup.getMenu().findItem(R.id.main_sca_menu_addToOngoingNotificationCheck).setTitle(R.string.removeFromOngoingNotificationCheck);
        }

        final String IgnoreNotificationPkgNames = sharedPreferences.getString(context.getString(R.string.sIgnoreNotificationList), "");
        if (existsInOneKeyList(IgnoreNotificationPkgNames, pkgName)) {
            popup.getMenu().findItem(R.id.main_sca_menu_addToIgnoreNotificationList).setTitle(R.string.removeFromIgnoreNotificationList);
        }

        if (FUFUtils.realGetFrozenStatus(context, pkgName, null)) {
            popup.getMenu().findItem(R.id.main_sca_menu_disableAEnable).setTitle(R.string.UfSlashRun);
        } else {
            popup.getMenu().findItem(R.id.main_sca_menu_disableAEnable).setTitle(R.string.freezeSlashRun);
        }

        if (!canRemoveItem) {
            popup.getMenu().removeItem(R.id.main_sca_menu_removeFromTheList);
        }

        final boolean isDeviceOwner = DevicePolicyManagerUtils.isDeviceOwner(context);
        if (isDeviceOwner) {
            try {
                final boolean suspended = DevicePolicyManagerUtils.getDevicePolicyManager(context).isPackageSuspended(DeviceAdminReceiver.getComponentName(context), pkgName);
                final MenuItem item = popup.getMenu().findItem(R.id.main_sca_menu_un_suspend);
                item.setTitle(suspended ? "Unsuspend app" : "Suspend app");
                item.setVisible(true);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }

        if (isDeviceOwner) {
            try {
                final PackageManager packageManager = context.getPackageManager();
                final PackageInfo pkgInfo = packageManager.getPackageInfo(pkgName, PackageManager.MATCH_UNINSTALLED_PACKAGES);
                boolean show = (pkgInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM || (pkgInfo.applicationInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == ApplicationInfo.FLAG_UPDATED_SYSTEM_APP;

                /* if (show) {
                    List<ApplicationInfo> enabledApps = packageManager.getInstalledApplications(0);
                    for (ApplicationInfo applicationInfo : enabledApps) {
                        if (applicationInfo.packageName.equals(pkgName)) {
                            show = false;
                            break;
                        }
                    }
                } */

                if (show)
                    popup.getMenu().findItem(R.id.main_sca_menu_enable_sys_app).setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        popup.setOnMenuItemClickListener(item -> {
            switch (item.getGroupId()) {
                case R.id.main_sca_menu_userDefined_menuGroup:
                    switch (item.getItemId()) {
                        case R.id.main_sca_menu_userDefined_newClassification:
                            final EditText vmUserDefinedNameAlertDialogEditText = new EditText(activity);
                            AlertDialog.Builder vmUserDefinedNameAlertDialog = new AlertDialog.Builder(activity);
                            vmUserDefinedNameAlertDialog.setTitle(R.string.label);
                            vmUserDefinedNameAlertDialog.setView(vmUserDefinedNameAlertDialogEditText);
                            vmUserDefinedNameAlertDialog.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    String label = Base64.encodeToString(vmUserDefinedNameAlertDialogEditText.getText().toString().getBytes(), Base64.DEFAULT);
                                    if ("".equals(label)) {
                                        showToast(activity, R.string.emptyNotAllowed);
                                    } else {
                                        boolean alreadyExists = false;
                                        SQLiteDatabase vmUserDefinedDb1 = context.openOrCreateDatabase("userDefinedCategories", Context.MODE_PRIVATE, null);
                                        vmUserDefinedDb1.execSQL(
                                                "create table if not exists categories(_id integer primary key autoincrement,label varchar,packages varchar)"
                                        );
                                        Cursor cursor1 = vmUserDefinedDb1.query("categories", new String[]{"label"}, null, null, null, null, null);
                                        if (cursor1.moveToFirst()) {
                                            for (int i = 0; i < cursor1.getCount(); i++) {
                                                if (label.equals(cursor1.getString(cursor1.getColumnIndexOrThrow("label")))) {
                                                    alreadyExists = true;
                                                    break;
                                                }
                                                cursor1.moveToNext();
                                            }
                                        }
                                        cursor1.close();
                                        if (alreadyExists) {
                                            showToast(activity, R.string.alreadyExist);
                                        } else {
                                            vmUserDefinedDb1.execSQL(
                                                    "replace into categories(_id,label,packages) VALUES ( "
                                                            + null + ",'"
                                                            + label + "','')"
                                            );
                                        }
                                        vmUserDefinedDb1.close();
                                    }
                                }
                            });
                            vmUserDefinedNameAlertDialog.setNegativeButton(R.string.cancel, null);
                            vmUserDefinedNameAlertDialog.show();
                            break;
                        default:
                            int itemId = item.getItemId();
                            if (userDefinedCategoriesHashMap.containsKey(itemId)) {
                                SQLiteDatabase vmUserDefinedDb1 = context.openOrCreateDatabase("userDefinedCategories", Context.MODE_PRIVATE, null);
                                vmUserDefinedDb1.execSQL(
                                        "create table if not exists categories(_id integer primary key autoincrement,label varchar,packages varchar)"
                                );
                                String pkgs = userDefinedCategoriesHashMap.get(itemId);
                                if (pkgs == null) {
                                    pkgs = "";
                                }
                                boolean existed = existsInOneKeyList(pkgs, pkgName);
                                if (existed) {
                                    pkgs = pkgs.replace(pkgName + ",", "");
                                } else {
                                    pkgs = pkgs + pkgName + ",";
                                }
                                vmUserDefinedDb1.execSQL(
                                        "UPDATE categories SET packages = '"
                                                + pkgs
                                                + "' WHERE _id = "
                                                + itemId
                                                + ";"
                                );
                                vmUserDefinedDb1.close();
                                showToast(activity, existed ? R.string.removed : R.string.added);
                            }
                            break;
                    }
                default:
                    switch (item.getItemId()) {
                        case R.id.main_sca_menu_forceStop:
                            if (!(context.getString(R.string.notAvailable).equals(name))) {
                                context.startActivity(new Intent(context, ForceStop.class).putExtra("pkgName", pkgName));
                            }
                            break;
                        case R.id.main_sca_menu_addToFreezeOnceQuit:
                            Support.checkAddOrRemove(context, FreezeOnceQuitPkgNames, pkgName, context.getString(R.string.sFreezeOnceQuit));
                            break;
                        case R.id.main_sca_menu_addToOneKeyList:
                            Support.checkAddOrRemove(context, pkgNames, pkgName, context.getString(R.string.sAutoFreezeApplicationList));
                            break;
                        case R.id.main_sca_menu_addToOneKeyUFList:
                            Support.checkAddOrRemove(context, UFPkgNames, pkgName, context.getString(R.string.sOneKeyUFApplicationList));
                            break;
                        case R.id.main_sca_menu_addToOngoingNotificationCheck:
                            Support.checkAddOrRemove(context, ONPkgNames, pkgName, context.getString(R.string.sOngoingNotificationCheckList));
                            break;
                        case R.id.main_sca_menu_addToIgnoreNotificationList:
                            Support.checkAddOrRemove(context, IgnoreNotificationPkgNames, pkgName, context.getString(R.string.sIgnoreNotificationList));
                            break;
                        case R.id.main_sca_menu_appDetail:
                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            Uri uri = Uri.fromParts("package", pkgName, null);
                            intent.setData(uri);
                            try {
                                context.startActivity(intent);
                            } catch (Exception e) {
                                e.printStackTrace();
                                showToast(context, e.getLocalizedMessage());
                            }
                            break;
                        case R.id.main_sca_menu_copyPkgName:
                            showToast(context, copyToClipboard(context, pkgName) ? R.string.success : R.string.failed);
                            break;
                        case R.id.main_sca_menu_disableAEnable:
                            if (!(context.getString(R.string.notAvailable).equals(name))) {
                                context.startActivity(new Intent(context, Freeze.class).putExtra("pkgName", pkgName).putExtra("auto", false));
                            }
                            break;
                        case R.id.main_sca_menu_createDisEnableShortCut:
                            checkSettingsAndRequestCreateShortcut(
                                    name,
                                    pkgName,
                                    getApplicationIcon(
                                            context,
                                            pkgName,
                                            ApplicationInfoUtils.getApplicationInfoFromPkgName(pkgName, context),
                                            false),
                                    Freeze.class,
                                    "FreezeYou! " + pkgName,
                                    context);
                            break;
                        case R.id.main_sca_menu_removeFromTheList:
                            if (folderPkgListSp != null) {
                                String folderPkgs = folderPkgListSp.getString("pkgS", "");
                                if (existsInOneKeyList(folderPkgs, pkgName)) {
                                    folderPkgListSp.edit()
                                            .putString("pkgS", folderPkgs.replace(pkgName + ",", ""))
                                            .apply();
                                }
                            }
                            break;
                        case R.id.main_sca_menu_gotoStore:
                            Intent gotoStoreIntent =
                                    new Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse("market://details/?id=" + pkgName));
                            String title = activity.getString(R.string.plsSelect);
                            Intent chooser = Intent.createChooser(gotoStoreIntent, title);
                            if (gotoStoreIntent.resolveActivity(activity.getPackageManager()) != null) {
                                activity.startActivity(chooser);
                            }
                            break;
                        case R.id.main_sca_menu_uninstall:
                            if (!(context.getString(R.string.notAvailable).equals(name)) &&
                                    context.getPackageManager()
                                            .getComponentEnabledSetting(
                                                    new ComponentName("cf.playhi.freezeyou", "cf.playhi.freezeyou.InstallPackagesActivity"))
                                            == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                                activity.startActivity(
                                        new Intent(
                                                Intent.ACTION_DELETE,
                                                Uri.parse("package:" + pkgName),
                                                activity,
                                                InstallPackagesActivity.class)

                                );
                            } else {
                                activity.startActivity(
                                        new Intent(
                                                Intent.ACTION_DELETE,
                                                Uri.parse("package:" + pkgName)
                                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                );
                            }
                            break;
                        case R.id.main_sca_menu_un_suspend:
                            try {
                                final boolean suspended = DevicePolicyManagerUtils.getDevicePolicyManager(context).isPackageSuspended(DeviceAdminReceiver.getComponentName(context), pkgName);
                                DevicePolicyManagerUtils.getDevicePolicyManager(context).setPackagesSuspended(DeviceAdminReceiver.getComponentName(context), new String[]{pkgName}, !suspended);
                            } catch (Exception e) {
                                ToastUtils.showToast(context, e.getMessage());
                            }
                            break;
                        case R.id.main_sca_menu_enable_sys_app:
                            try {
                                DevicePolicyManagerUtils.getDevicePolicyManager(context).enableSystemApp(DeviceAdminReceiver.getComponentName(context), pkgName);
                            } catch (Exception e) {
                                ToastUtils.showToast(context, e.getMessage());
                            }
                            break;
                        default:
                            break;
                    }
            }
            return true;
        });

        return popup;
    }

    public static void checkLanguage(Context context) {
        Resources resources = context.getResources();
        DisplayMetrics dm = resources.getDisplayMetrics();
        Configuration config = resources.getConfiguration();

        config.locale = getLocal(context);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            config.setLayoutDirection(config.locale);
        }

        resources.updateConfiguration(config, dm);
    }

    private static Locale getLocal(Context context) {
        String s = PreferenceManager.getDefaultSharedPreferences(context).getString("languagePref", "Default");

        if (s == null) {
            s = "Default";
        }

        switch (s) {
            case "en":
                return new Locale("en");
            case "en-US":
                return new Locale("en", "US");
            case "ru-RU":
                return new Locale("ru", "RU");
            case "uk-UA":
                return new Locale("uk", "UA");
            case "zh-CN":
                return new Locale("zh", "CN");
            case "zh-TW":
                return new Locale("zh", "TW");
            case "Default":
            default:
                return Locale.getDefault();
        }
    }

}

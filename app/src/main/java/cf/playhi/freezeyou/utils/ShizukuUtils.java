package cf.playhi.freezeyou.utils;

import android.content.Context;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Singleton;

import rikka.shizuku.Shizuku;
import rikka.shizuku.ShizukuBinderWrapper;
import rikka.shizuku.SystemServiceHelper;

public final class ShizukuUtils {
    public static boolean supportsShizuku() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }

    public static boolean isShizukuInstalled(Context context) {
        try {
            return context.getPackageManager().getApplicationInfo("moe.shizuku.privileged.api", 0) != null;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public static void requestPermission() throws IllegalStateException {
        if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            Shizuku.requestPermission(93270);
        }
    }

    private static final Singleton<IPackageManager> PACKAGE_MANAGER = new Singleton<IPackageManager>() {
        @Override
        protected IPackageManager create() {
            return IPackageManager.Stub.asInterface(new ShizukuBinderWrapper(SystemServiceHelper.getSystemService("package")));
        }
    };

    public static IPackageManager getPackageManager() {
        return PACKAGE_MANAGER.get();
    }
}

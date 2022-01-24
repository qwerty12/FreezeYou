package cf.playhi.freezeyou.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;

import moe.shizuku.api.BinderContainer;
import rikka.shizuku.Shizuku;
import rikka.shizuku.ShizukuProvider;

public class ShizukuProvider2 extends ShizukuProvider {
    private static final String TAG = "ShizukuProvider2";
    private static final String EXTRA_BINDER = "moe.shizuku.privileged.api.intent.extra.BINDER";

    @SuppressLint("RestrictedApi")
    public static void requestBinderForNonProviderProcess(@NonNull Context context) {
        Log.d(TAG, "request binder in non-provider process");

        Bundle reply;
        try {
            reply = context.getContentResolver().call(Uri.parse("content://" + context.getPackageName() + ".shizuku"),
                    ShizukuProvider.METHOD_GET_BINDER, null, new Bundle());
        } catch (Throwable tr) {
            reply = null;
        }

        if (reply != null) {
            reply.setClassLoader(BinderContainer.class.getClassLoader());

            BinderContainer container = reply.getParcelable(EXTRA_BINDER);
            if (container != null && container.binder != null) {
                Log.i(TAG, "Binder received from other process");
                Shizuku.onBinderReceived(container.binder, context.getPackageName());
            }
        }
    }
}
package cf.playhi.freezeyou;

import static android.app.admin.DevicePolicyManager.KEYGUARD_DISABLE_TRUST_AGENTS;
import static android.os.UserManager.DISALLOW_CONFIG_DATE_TIME;
import static android.os.UserManager.DISALLOW_CONFIG_PRIVATE_DNS;
import static android.os.UserManager.DISALLOW_CONFIG_VPN;
import static android.os.UserManager.DISALLOW_CONFIG_WIFI;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ProxyInfo;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceDataStore;
import androidx.preference.PreferenceFragmentCompat;

import com.catchingnow.delegatedscopesmanager.centerApp.CenterApp;

import java.util.Set;

import cf.playhi.freezeyou.utils.DevicePolicyManagerUtils;
import cf.playhi.freezeyou.utils.ToastUtils;

class SharedPreferenceDataStore extends PreferenceDataStore {

    private final Context mContext;
    private final SharedPreferences mSharedPreferences;

    public SharedPreferenceDataStore(@NonNull Context context) {
        mContext = context;
        mSharedPreferences = context.getApplicationContext().getSharedPreferences("MiniDPC", Context.MODE_PRIVATE);
    }

    @NonNull
    public SharedPreferences getSharedPreferences() {
        return mSharedPreferences;
    }

    private DevicePolicyManager getDevicePolicyManager() { return DevicePolicyManagerUtils.getDevicePolicyManager(mContext); }
    private ComponentName getAdminComponentName() { return DeviceAdminReceiver.getComponentName(mContext); }

    private void setGlobalProxy(String value) {
        if (TextUtils.isEmpty(value)) {
            getDevicePolicyManager().setRecommendedGlobalProxy(getAdminComponentName(), null);
            return;
        }

        final String[] port_n_host = value.split(":");
        if (port_n_host.length != 2) {
            ToastUtils.showToast(mContext, "Missing host/port");
            return;
        }

        final int port = Integer.parseInt(port_n_host[1]);
        if (port > 65535) {
            ToastUtils.showToast(mContext, "Port out of range");
            return;
        }

        getDevicePolicyManager().setRecommendedGlobalProxy(getAdminComponentName(), ProxyInfo.buildDirectProxy(port_n_host[0], port));
    }

    private boolean getUserRestriction(String key, boolean defaultValue) {
        return getDevicePolicyManager().getUserRestrictions(getAdminComponentName()).getBoolean(key, defaultValue);
    }

    private void setUserRestriction(String key, boolean value) {
        if (value)
            getDevicePolicyManager().addUserRestriction(getAdminComponentName(), key);
        else
            getDevicePolicyManager().clearUserRestriction(getAdminComponentName(), key);
    }

    @Override
    public void putString(String key, @Nullable String value) {
        switch (key) {
            case "LockScreenMessage":
                getDevicePolicyManager().setDeviceOwnerLockScreenInfo(getAdminComponentName(), value);
                break;
            case "SetGlobalHttpProxy":
                setGlobalProxy(value);
                break;
            case "SetProfileName":
                if (!TextUtils.isEmpty(value)) {
                    try {
                        getDevicePolicyManager().setProfileName(getAdminComponentName(), value);
                    } catch (final Exception e) {
                        e.printStackTrace();
                        break;
                    }
                } else {
                    ToastUtils.showToast(mContext, "Missing profile name");
                    break;
                }
                // fall through
            default:
                mSharedPreferences.edit().putString(key, value).apply();
        }
    }

    @Override
    public void putStringSet(String key, @Nullable Set<String> values) {
        mSharedPreferences.edit().putStringSet(key, values).apply();
    }

    @Override
    public void putInt(String key, int value) {
        mSharedPreferences.edit().putInt(key, value).apply();
    }

    @Override
    public void putLong(String key, long value) {
        mSharedPreferences.edit().putLong(key, value).apply();
    }

    @Override
    public void putFloat(String key, float value) {
        mSharedPreferences.edit().putFloat(key, value).apply();
    }

    @Override
    public void putBoolean(String key, boolean value) {
        switch (key) {
            case "DisableCamera":
                getDevicePolicyManager().setCameraDisabled(getAdminComponentName(), value);
                break;
            case "SetAutoTime":
                getDevicePolicyManager().setAutoTimeEnabled(getAdminComponentName(), value);
                break;
            case "SetAutoTimeZone":
                getDevicePolicyManager().setAutoTimeZoneEnabled(getAdminComponentName(), value);
                break;
            case "KeyguardDisableTrustAgents":
                int keyguardDisabledFeatures = getDevicePolicyManager().getKeyguardDisabledFeatures(getAdminComponentName());
                if (value) {
                    keyguardDisabledFeatures |= KEYGUARD_DISABLE_TRUST_AGENTS;
                } else {
                    keyguardDisabledFeatures &= ~KEYGUARD_DISABLE_TRUST_AGENTS; // = 0
                }
                getDevicePolicyManager().setKeyguardDisabledFeatures(getAdminComponentName(), keyguardDisabledFeatures);
                break;
            case "DisallowConfigPrivateDns":
                setUserRestriction(DISALLOW_CONFIG_PRIVATE_DNS, value);
                break;
            case "DisallowConfigDateTime":
                setUserRestriction(DISALLOW_CONFIG_DATE_TIME, value);
                break;
            case "DisallowConfigVpn":
                setUserRestriction(DISALLOW_CONFIG_VPN, value);
                break;
            case "DisallowConfigWifi":
                setUserRestriction(DISALLOW_CONFIG_WIFI, value);
                break;
            case "EnableBackupService":
                getDevicePolicyManager().setBackupServiceEnabled(getAdminComponentName(), value);
                break;
            default:
                mSharedPreferences.edit().putBoolean(key, value).apply();
        }
    }

    @Nullable
    @Override
    public String getString(String key, @Nullable String defValue) {
        switch (key) {
            case "LockScreenMessage":
                final CharSequence cs = getDevicePolicyManager().getDeviceOwnerLockScreenInfo();
                if (cs != null) {
                    final String ret = cs.toString();
                    if (!TextUtils.isEmpty(ret))
                        return ret;
                }
                return defValue;
            case "SetGlobalHttpProxy":
                final String host = System.getProperty("http.proxyHost");
                if (!TextUtils.isEmpty(host)) {
                    return host + ":" + System.getProperty("http.proxyPort");
                }
                return defValue;
            case "SetProfileName":
            default:
                return mSharedPreferences.getString(key, defValue);
        }
    }

    @Nullable
    @Override
    public Set<String> getStringSet(String key, @Nullable Set<String> defValues) {
        return mSharedPreferences.getStringSet(key, defValues);
    }

    @Override
    public int getInt(String key, int defValue) {
        return mSharedPreferences.getInt(key, defValue);
    }

    @Override
    public long getLong(String key, long defValue) {
        return mSharedPreferences.getLong(key, defValue);
    }

    @Override
    public float getFloat(String key, float defValue) {
        return mSharedPreferences.getFloat(key, defValue);
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
        switch (key) {
            case "DisableCamera":
                return getDevicePolicyManager().getCameraDisabled(getAdminComponentName());
            case "SetAutoTime":
                return getDevicePolicyManager().getAutoTimeEnabled(getAdminComponentName());
            case "SetAutoTimeZone":
                return getDevicePolicyManager().getAutoTimeZoneEnabled(getAdminComponentName());
            case "KeyguardDisableTrustAgents":
                return (getDevicePolicyManager().getKeyguardDisabledFeatures(getAdminComponentName()) & KEYGUARD_DISABLE_TRUST_AGENTS) == KEYGUARD_DISABLE_TRUST_AGENTS;
            case "DisallowConfigPrivateDns":
                return getUserRestriction(DISALLOW_CONFIG_PRIVATE_DNS, defValue);
            case "DisallowConfigDateTime":
                return getUserRestriction(DISALLOW_CONFIG_DATE_TIME, defValue);
            case "DisallowConfigVpn":
                return getUserRestriction(DISALLOW_CONFIG_VPN, defValue);
            case "DisallowConfigWifi":
                return getUserRestriction(DISALLOW_CONFIG_WIFI, defValue);
            case "EnableBackupService":
                return getDevicePolicyManager().isBackupServiceEnabled(getAdminComponentName());
            default:
                return mSharedPreferences.getBoolean(key, defValue);
        }
    }
}

public class MiniDPCFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceClickListener {
    // Tag for creating this fragment. This tag can be used to retrieve this fragment.
    public static final String FRAGMENT_TAG = "MiniDPCFragment";

    SharedPreferenceDataStore spds = null;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        if (spds == null)
            spds = new SharedPreferenceDataStore(requireContext());
        getPreferenceManager().setPreferenceDataStore(spds);
        setPreferencesFromResource(R.xml.minidpc, rootKey);

        Preference pref = findPreference("ManagedConfigurations");
        if (pref != null)
            pref.setOnPreferenceClickListener(this);
        pref = findPreference("PrivateDnsMode");
        if (pref != null)
            pref.setOnPreferenceClickListener(this);
        pref = findPreference("ManageDSM");
        if (pref != null)
            pref.setOnPreferenceClickListener(this);

        final EditTextPreference textPref = findPreference("SetGlobalHttpProxy");
        if (textPref != null)
            textPref.setOnBindEditTextListener(editText -> editText.setHint("host:port (empty to remove)"));
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().setTitle("MiniDPC");
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        String key = preference.getKey();
        switch (key) {
            case "ManagedConfigurations":
                showFragment(new ManagedConfigurationsFragment());
                return true;
            case "PrivateDnsMode":
                showFragment(new PrivateDnsModeFragment());
                return true;
            case "ManageDSM":
                startActivity(new Intent(CenterApp.ACTION_APP_LIST).setPackage(requireContext().getPackageName()));
                return true;
        }
        return false;
    }

    private void showFragment(final Fragment fragment) {
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction().addToBackStack(MiniDPCFragment.class.getName())
                .replace(R.id.container, fragment).commit();
    }
}

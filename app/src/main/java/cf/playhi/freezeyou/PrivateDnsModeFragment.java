/*
 * Copyright (C) 2018 The Android Open Source Project
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

import androidx.fragment.app.Fragment;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;

import cf.playhi.freezeyou.utils.DevicePolicyManagerUtils;
import cf.playhi.freezeyou.utils.ToastUtils;

public class PrivateDnsModeFragment extends Fragment implements View.OnClickListener,
        RadioGroup.OnCheckedChangeListener {
    private static final String TAG = "PDNS_FRAG";

    private DevicePolicyManager mDpm;
    private RadioGroup mPrivateDnsModeSelection;
    private Button mSetButton;
    private EditText mCurrentResolver;
    // The mode that is currently selected in the radio group.
    private int mSelectedMode;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDpm = DevicePolicyManagerUtils.getDevicePolicyManager(requireContext());
        mSelectedMode = DevicePolicyManager.PRIVATE_DNS_MODE_UNKNOWN;
    }

    @Override
    public void onClick(View view) {
        String resolver = mCurrentResolver.getText().toString();
        setPrivateDnsMode(mSelectedMode, resolver);
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        updateSelectedMode(checkedId);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.private_dns_mode, null);
        mSetButton = view.findViewById(R.id.private_dns_mode_apply);
        mSetButton.setOnClickListener(this);

        mPrivateDnsModeSelection = view.findViewById(R.id.private_dns_mode_selection);
        int currentMode = getPrivateDnsMode();
        switch (currentMode) {
            case DevicePolicyManager.PRIVATE_DNS_MODE_OFF:
                mPrivateDnsModeSelection.check(R.id.private_dns_mode_off);
                break;
            case DevicePolicyManager.PRIVATE_DNS_MODE_OPPORTUNISTIC:
                mPrivateDnsModeSelection.check(R.id.private_dns_mode_automatic);
                break;
            case DevicePolicyManager.PRIVATE_DNS_MODE_PROVIDER_HOSTNAME:
                mPrivateDnsModeSelection.check(R.id.private_dns_mode_specific_host);
                break;
            default:
                mPrivateDnsModeSelection.check(R.id.private_dns_mode_unknown);
                break;
        }
        mPrivateDnsModeSelection.setOnCheckedChangeListener(this);
        updateSelectedMode(mPrivateDnsModeSelection.getCheckedRadioButtonId());

        mCurrentResolver = view.findViewById(R.id.private_dns_resolver);
        mCurrentResolver.setText(getPrivateDnsHost());
        return view;
    }

    private void updateSelectedMode(int checkedId) {
        switch (checkedId) {
            case R.id.private_dns_mode_off:
                mSelectedMode = DevicePolicyManager.PRIVATE_DNS_MODE_OFF;
                break;
            case R.id.private_dns_mode_automatic:
                mSelectedMode = DevicePolicyManager.PRIVATE_DNS_MODE_OPPORTUNISTIC;
                break;
            case R.id.private_dns_mode_specific_host:
                mSelectedMode = DevicePolicyManager.PRIVATE_DNS_MODE_PROVIDER_HOSTNAME;
                break;
            case R.id.private_dns_mode_unknown:
            default:
                mSelectedMode = DevicePolicyManager.PRIVATE_DNS_MODE_UNKNOWN;
                break;
        }
    }

    private int getPrivateDnsMode() {
        try {
            return mDpm.getGlobalPrivateDnsMode(
                    DeviceAdminReceiver.getComponentName(getActivity()));
        } catch (SecurityException e) {
            Log.w(TAG, "Failure getting current mode", e);
        }

        return DevicePolicyManager.PRIVATE_DNS_MODE_UNKNOWN;
    }

    private String getPrivateDnsHost() {
        try {
            return mDpm.getGlobalPrivateDnsHost(
                    DeviceAdminReceiver.getComponentName(getActivity()));
        } catch (SecurityException e) {
            Log.w(TAG, "Failure getting host", e);
        }

        return "<error getting resolver>";
    }

    private void setPrivateDnsMode(int mode, String resolver) {
        Log.w(TAG, String.format("Setting mode %d host %s", mSelectedMode, resolver));

        final ComponentName component = DeviceAdminReceiver.getComponentName(getActivity());
        new SetPrivateDnsTask(
                mDpm, component, mode, resolver, (int msgId, Object... args) -> {
                    ToastUtils.showLongToast(getContext(), getString(msgId, args));
                }).execute(new Void[0]);
    }
}

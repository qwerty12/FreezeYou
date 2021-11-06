// Copied from https://github.com/percula/LensCap

package cf.playhi.freezeyou;

import android.content.BroadcastReceiver;
import android.graphics.drawable.Icon;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import cf.playhi.freezeyou.utils.DevicePolicyManagerUtils;
import cf.playhi.freezeyou.utils.ToastUtils;

/**
 * Quick Settings tile service for Android 7.0+
 */

public class LensCapTileService extends TileService {

    @Override
    public void onClick() {
        final boolean newState = !isCameraDisabled();
        setCameraDisabled(newState);
        updateTile();

        ToastUtils.showShortToast(this, newState ? "Lens Cap on" : "Lens Cap off");
    }

    @Override
    public void onStartListening() {
        updateTile();
    }

    private void updateTile() {
        Tile tile = getQsTile();

        if (isCameraDisabled()) {
            tile.setIcon(Icon.createWithResource(this, R.drawable.qs_tile_disabled));
            tile.setLabel("Enable Camera");
            tile.setState(Tile.STATE_INACTIVE);
        } else {
            tile.setIcon(Icon.createWithResource(this, R.drawable.qs_tile_enabled));
            tile.setLabel("Disable Camera");
            tile.setState(Tile.STATE_ACTIVE);
        }

        tile.updateTile();
    }

    private boolean isCameraDisabled() {
        if (DevicePolicyManagerUtils.isDeviceOwner(this))
            return DevicePolicyManagerUtils.getDevicePolicyManager(this).getCameraDisabled(DeviceAdminReceiver.getComponentName(this));
        return false;
    }

    private void setCameraDisabled(boolean disabled) {
        if (DevicePolicyManagerUtils.isDeviceOwner(this))
            DevicePolicyManagerUtils.getDevicePolicyManager(this).setCameraDisabled(DeviceAdminReceiver.getComponentName(this), disabled);
    }
}

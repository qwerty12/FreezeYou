package cf.playhi.freezeyou;

import android.os.Build;
import android.os.Bundle;

import net.grandcentrix.tray.AppPreferences;

import androidx.appcompat.app.AppCompatActivity;

import static cf.playhi.freezeyou.ThemeUtils.processSetTheme;

public class MiniDPCActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        processSetTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.container, new MiniDPCFragment(), MiniDPCFragment.FRAGMENT_TAG)
                    .commit();
        }
    }

    @Override
    public void finish() {
        if (Build.VERSION.SDK_INT >= 21 && !(new AppPreferences(this).getBoolean("showInRecents", true))) {
            finishAndRemoveTask();
        }
        super.finish();
    }
}


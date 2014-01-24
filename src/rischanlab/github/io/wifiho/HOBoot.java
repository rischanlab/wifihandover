package rischanlab.github.io.wifiho;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class HOBoot extends BroadcastReceiver {
	private final static String TAG ="WifiHandover";

	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		Log.i(TAG, "Boot Intent Receiver: " +intent);
		if(intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)){
			SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
			if(pref.getBoolean("autostart", true)){
				context.startService(new Intent("rischanlab.github.io.wifiho.HOService"));
			}
		}
		
	}

}

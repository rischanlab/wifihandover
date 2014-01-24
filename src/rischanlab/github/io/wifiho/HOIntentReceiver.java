package rischanlab.github.io.wifiho;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class HOIntentReceiver extends BroadcastReceiver {
	private final static String TAG="WifiHandover";
	
	private HOActivity activity;
	public HOIntentReceiver(HOActivity activity){
		super();
		this.activity = activity;
	}
	
	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		Log.i(TAG, "HOIntentReceiver: "+ intent);
		
		String action = intent.getAction();
		if(action.equals(HOService.AP_LIST_UPDATE_ACTION)){
			activity.updateAPListUI();
		}else if(action.equals(HOService.WIFI_UPDATE_ACTION)){
			activity.updateStatusUI();
		}
		

	}

}

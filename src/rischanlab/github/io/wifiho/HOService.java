package rischanlab.github.io.wifiho;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

class Utils {
	public static String join(Object[] items) {
		StringBuilder buf = new StringBuilder();
		for (Object o : items) {
			buf.append(o);
			buf.append(",");
		}
		if (0 < buf.length()) {
			buf.deleteCharAt(buf.length() - 1);
		}
		return buf.toString();
	}
}

class ScanResultComparator implements Comparator<ScanResult> {
	public int compare(ScanResult r1, ScanResult r2) {
		return WifiManager.compareSignalLevel(r2.level, r1.level);
	}
}

class AccessPoint {
	private String ssid;
	private int level;
	private boolean isSelected;
	
	public AccessPoint(String ssid, int level, boolean isSelected) {
		this.ssid = ssid;
		this.level = level;
		this.isSelected = isSelected;
	}
	
	public String getSSID() {
		return ssid;
	}
	
	public int getLevel() {
		return level;
	}
	
	public boolean isSelected() {
		return isSelected;
	}
	
	public void setSelected(boolean selected) {
		isSelected = selected;
	}
}

public class HOService extends Service {
	private final static String TAG = "WifiHandover";
	
	public final static String AP_LIST_UPDATE_ACTION ="rischanlab.github.io.wifiho.AP_LIST_UPDATE_ACTION";
	public final static String WIFI_UPDATE_ACTION ="rischanlab.github.io.wifiho.WIFI_UPDATE_ACTION";
	
	private static boolean mIsRunning = false;
	
	public static boolean isRunning() {
		return mIsRunning;
	}
	
    public class LocalBinder extends Binder {
    	HOService getService() {
            return HOService.this;
        }
    }
    
    private final LocalBinder mBinder = new LocalBinder();
    
	private BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.i(TAG, "broadcast received: " + intent);
			
			String action = intent.getAction();
			if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
				HOService.this.updateAPList();
				context.sendBroadcast(new Intent(AP_LIST_UPDATE_ACTION));
			} else if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION) || action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
				HOService.this.updateStatus();
				context.sendBroadcast(new Intent(WIFI_UPDATE_ACTION));
			}
		}
	};
	
	private WifiManager mWifi;
	private boolean mAutoHandover;
	private String mCurrentAP;
	
	private Set<String> mSelected;
	private List<AccessPoint> mOrder;
	private HashMap<String, Integer> mItems;

    @Override
    public void onCreate() {
    	Log.i(TAG, "service created");
    	
    	HOService.mIsRunning = true;

    	mWifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
    	
    	mCurrentAP = null;
    	mSelected = new HashSet<String>();
        mOrder = new ArrayList<AccessPoint>();
        mItems = new HashMap<String, Integer>();
        
        // restore preferences
        SharedPreferences pref = this.getSharedPreferences(TAG, MODE_PRIVATE);
        
        mAutoHandover = pref.getBoolean("autoHandover", false);
        
        String rawSelected = pref.getString("selectedAPList", "");
        for (String ssid : rawSelected.split(",")) {
        	mSelected.add(ssid.trim());
        }

        // enable intent filter to receive broadcasts
        registerReceiver(receiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        registerReceiver(receiver, new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "service started: " + intent);
        
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "service stopped");
        
        HOService.mIsRunning = false;
    }
	
	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}
	
	public void setHandoverEnabled(boolean status) {
		mAutoHandover = status;
		
		// save preference
		SharedPreferences pref = this.getSharedPreferences(TAG, MODE_PRIVATE);
		SharedPreferences.Editor editor = pref.edit();
		editor.putBoolean("autoHandover", mAutoHandover);
		editor.commit();
	}
	
	public boolean isHandoverEnabled() {
		return mAutoHandover;
	}
	
	public boolean registerAP(String ssid) {
		if (isRegisteredAP(ssid)) {
			return false;
		}
		
		if (mSelected.add(ssid)) {
			// sync with AP in mOrder
			for (AccessPoint ap : mOrder) {
				if (ap.getSSID() == ssid) {
					ap.setSelected(true);
					break;
				}
			}
			
			// save preference
			SharedPreferences pref = this.getSharedPreferences(TAG, MODE_PRIVATE);
			SharedPreferences.Editor editor = pref.edit();
			editor.putString("selectedAPList", Utils.join(mSelected.toArray()));
			editor.commit();
			
			return true;
		}
		
		return false;
	}
	
	public boolean unregisterAP(String ssid) {
		if (!isRegisteredAP(ssid)) {
			return false;
		}
		
		if (mSelected.remove(ssid)) {
			// sync with AP in mOrder
			for (AccessPoint ap : mOrder) {
				if (ap.getSSID() == ssid) {
					ap.setSelected(false);
					break;
				}
			}
			
			// save preference
			SharedPreferences pref = this.getSharedPreferences(TAG, MODE_PRIVATE);
			SharedPreferences.Editor editor = pref.edit();
			editor.putString("selectedAPList", Utils.join(mSelected.toArray()));
			editor.commit();
			
			return true;
		}
		
		return false;
	}
	
	public boolean isRegisteredAP(String ssid) {
		return mSelected.contains(ssid);
	}
	
	public List<String> getRegisteredAPList() {
		return new ArrayList<String>(mSelected);
	}
	
	public List<AccessPoint> getAvailableAPList() {
		return mOrder;
	}
	
	public String getCurrentAP() {
		return mCurrentAP;
	}
	
	public boolean isConnected() {
		return mWifi.isWifiEnabled() && mCurrentAP != null;
	}
	
	public boolean isPowered() {
		return mWifi.isWifiEnabled();
	}
	
	public void startScan() {
		mWifi.startScan();
	}
	
	public void updateAPList() {
		mOrder.clear();
		mItems.clear();
		
		// get available networks
		List<ScanResult> ssid = mWifi.getScanResults();
		Collections.sort(ssid, new ScanResultComparator());
		
		for (ScanResult result : ssid) {
			mOrder.add(new AccessPoint(result.SSID, result.level, mSelected.contains(result.SSID)));
			mItems.put(result.SSID, mOrder.size() - 1);
		}
		
		
		if (mAutoHandover) {
			handover();
		}
	}
	
	public void updateStatus() {
		WifiInfo info = mWifi.getConnectionInfo();
		mCurrentAP = info.getSSID();
	}
	
	public void handover() {
		// fetch threshold setting from 
		SharedPreferences pref = this.getSharedPreferences(TAG, MODE_PRIVATE);
		int threshold = pref.getInt("handoverThreshold", 6);
		
		// search current AP
		AccessPoint currentAP = null;
		if (mCurrentAP != null) {
			for (AccessPoint ap : mOrder) {
				if (ap.getSSID().equals(mCurrentAP)) {
					currentAP = ap;
				}
			}
		}
		
		
		for (AccessPoint ap : mOrder) {
			if (!mSelected.contains(ap.getSSID())) {
				continue;
			}
			
			for (WifiConfiguration conf : mWifi.getConfiguredNetworks()) {
				if (conf.SSID.equals("\"" + ap.getSSID() + "\"")) {
					if (conf.SSID.equals("\"" + mCurrentAP + "\"")) {
						Log.i(TAG, "Already connected to " + conf.SSID);
						return;
					}
					if (currentAP != null && ap.getLevel() < currentAP.getLevel() + threshold) {
						Log.i(TAG, "Below threshold: threshold=" + threshold + ", diff=" + (ap.getLevel() - currentAP.getLevel()));
						return;
					}
					if (mWifi.enableNetwork(conf.networkId, true)) {
						Log.i(TAG, "Connected to " + conf.SSID);
						Toast.makeText(this, "Connected to " + conf.SSID, Toast.LENGTH_SHORT).show();
						sendBroadcast(new Intent(WIFI_UPDATE_ACTION));
						return;
					}
				}
			}
		}
		
		Log.i(TAG, "No applicable AP");
	}
}

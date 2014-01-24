package rischanlab.github.io.wifiho;

import android.os.Bundle;
import android.os.IBinder;
import android.app.Activity;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class HOActivity extends Activity {
	
	private final String TAG="WifiHandover";
	HOIntentReceiver receiver;
	ArrayAdapter<String> data;
	HOButtonAdapter regData;
	TextView textStatus;
	ListView listAP;
	ListView listRegAP;
	RadioButton tabAvailable;
	RadioButton tabRegistered;
	
	private HOService mService = null;
	private ServiceConnection mConn = new ServiceConnection() {
		
		@Override
		public void onServiceDisconnected(ComponentName name) {
			// TODO Auto-generated method stub
			Log.i(TAG, "ON service Disconected: " + name);
			HOActivity.this.mService = null;
		}
		
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			// TODO Auto-generated method stub
			Log.i(TAG, "ON sERVICE connected " + name);
			HOActivity.this.mService = ((HOService.LocalBinder) service).getService();
			HOActivity.this.updateUI();
		}
	};
	
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ho);
        
        textStatus = (TextView) findViewById(R.id.textStatus);
        tabAvailable = (RadioButton) findViewById(R.id.tabAvailable);
        tabRegistered = (RadioButton) findViewById(R.id.tabRegistered);
        listAP = (ListView) findViewById(R.id.listAP);
        listRegAP = (ListView) findViewById(R.id.listRegAP);
        
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.ho, menu);
        return true;
    }
    
    public void updateUI(){
    	if (mService == null) {
    		return;
    	}
    	
    	mService.isHandoverEnabled();
    	
    	updateAPListUI();
    	updateStatusUI();
    }
    
    public void updateAPListUI() {
    	if (mService == null) {
    		return;
    	}
    	
    	// switch ListView to be shown
    	listAP.setVisibility(tabAvailable.isChecked() ? View.VISIBLE : View.INVISIBLE);
    	listRegAP.setVisibility(tabRegistered.isChecked() ? View.VISIBLE : View.INVISIBLE);
    	
    	// fill ListView with items
    	if (tabAvailable.isChecked()) {
    		
        	listAP.clearChoices();
        	data.clear();
        	
        	for (AccessPoint ap : mService.getAvailableAPList()) {
        		data.add(ap.getSSID() + " [" + ap.getLevel() + "]");
        		listAP.setItemChecked(data.getCount() - 1, ap.isSelected());
        	}
        	
        	boolean status = 0 < data.getCount();
        	if (!status) {
        		data.add("Not Available");
        	}
        	listAP.setEnabled(status);
        	
    	} else if (tabRegistered.isChecked()) {
    		
    		listRegAP.clearChoices();
    		regData.clear();
    		
    		for (String ssid : mService.getRegisteredAPList()) {
    			regData.add(ssid);
    		}
    		
    		if (regData.getCount() == 0) {
    			regData.add("No Registered AP");
    		}
    		
    	}
    }
    
    public void updateStatusUI() {
    	// update SSID
    	String ap = "None";
    	if (mService != null && mService.isConnected()) {
    		ap = mService.getCurrentAP();
    	}
    	textStatus.setText("Connecting to: " + ap);
    	
    	// update Wifi status
    	LinearLayout ll = (LinearLayout) findViewById(R.id.linearBottom);
		RelativeLayout.LayoutParams params = null;
    	if (mService.isPowered()) {
    		params = new RelativeLayout.LayoutParams(
    				RelativeLayout.LayoutParams.MATCH_PARENT, 0);
    	} else {
    		params = new RelativeLayout.LayoutParams(
    				RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
    	}
    	params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
    	ll.setLayoutParams(params);
    }
    
}

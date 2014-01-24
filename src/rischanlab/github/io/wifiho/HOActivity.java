package rischanlab.github.io.wifiho;

import java.util.List;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.view.Menu;


public class HOActivity extends Activity {
	
	private final String TAG="WifiHandover";
	HOIntentReceiver receiver;
	ArrayAdapter<String> data;
	HOButtonAdapter regData;
	TextView textStatus;
	ListView listAP;
	ListView listRegAP;
	ImageButton buttonRefresh;
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
        buttonRefresh = (ImageButton) findViewById(R.id.buttonRefresh);
        tabAvailable = (RadioButton) findViewById(R.id.tabAvailable);
        tabRegistered = (RadioButton) findViewById(R.id.tabRegistered);
        listAP = (ListView) findViewById(R.id.listAP);
        listRegAP = (ListView) findViewById(R.id.listRegAP);
        
        buttonRefresh.setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if (mService != null){
					mService.startScan();
				}
			}
		});
        
        OnCheckedChangeListener listener = new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				// TODO Auto-generated method stub
				if (isChecked){
					if (buttonView.getId() != tabAvailable.getId()){
						tabAvailable.setChecked(false);
					}
					if(buttonView.getId() != tabRegistered.getId()){
						tabRegistered.setChecked(false);
					} 
					
					HOActivity.this.updateAPListUI();
				}
			}
		};
        
        tabAvailable.setOnCheckedChangeListener(listener);
        tabRegistered.setOnCheckedChangeListener(listener);
        
        data = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_multiple_choice);
        listAP.setAdapter(data);
        listAP.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        listAP.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View v, int pos,
					long id) {
				// TODO Auto-generated method stub
				List<AccessPoint> list = mService.getAvailableAPList();
				String ssid = list.get(pos).getSSID();
				SparseBooleanArray checked = listAP.getCheckedItemPositions();
				if (checked.get(pos)){
					mService.registerAP(ssid);
				}else {
					mService.unregisterAP(ssid);
				}
			}
        	
		});
        registerForContextMenu(listAP);
        // setup Registered ListView
        regData = new HOButtonAdapter(this, R.layout.row);
        regData.setOnItemRemoved(new OnItemRemoved() {
			public void onItemRemoved(String item) {
				if (mService == null) {
        			return;
        		}
				
				mService.unregisterAP(item);
			}
        });
        
        listRegAP.setAdapter(regData);
        
        // listen internal intents from core service
        receiver = new HOIntentReceiver(this);
		registerReceiver(receiver, new IntentFilter(HOService.AP_LIST_UPDATE_ACTION));
		registerReceiver(receiver, new IntentFilter(HOService.WIFI_UPDATE_ACTION));
       
		
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	
    	if (!HOService.isRunning()) {
    		Log.i(TAG, "onResume: service is not running");
    		startService(new Intent(this, HOService.class));
    	}
    	
    	if (mService == null) {
    		Log.i(TAG, "onResume: service is not binded");
            bindService(new Intent(this, HOService.class), mConn, Context.BIND_AUTO_CREATE);
    	} else {
    		updateUI();
    	}
    }
    
    @Override
    public void onStart() {
    	super.onStart();
    	
    	if (mService == null) {
    		return;
    	}
    	
    	Log.i(TAG, "onStart: activity is shown, refresh AP list");
    	mService.startScan();
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo info) {
    	menu.setHeaderTitle("Wifi Handover");
    	
    	menu.add("Connect");
    	menu.add("Ignore");
    	
    	super.onCreateContextMenu(menu, v, info);
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
    	return super.onContextItemSelected(item);
    }

    
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.ho, menu);
        return true;
    }
    
    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
    	super.onOptionsItemSelected(item);
        Intent intent = new Intent(HOActivity.this, HOAbout.class);
        /*Here ActivityA is current Activity and ColourActivity is the target Activity.*/
        startActivity(intent);
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

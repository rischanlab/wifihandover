package rischanlab.github.io.wifiho;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

interface OnItemRemoved {
	public void onItemRemoved(String item);
}

public class HOButtonAdapter extends ArrayAdapter<String> {
	private OnItemRemoved mRemovedCallback = null;

	public HOButtonAdapter(Context context, int textViewResourceId) {
		super(context, textViewResourceId);
	}
	
	@Override
	public View getView(int position, View v, ViewGroup parent) {
		String ssid = getItem(position);
		
        if (v == null) {
            LayoutInflater li = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = li.inflate(R.layout.row, null);
            
            if (ssid != null) {
	            ImageButton button = (ImageButton) v.findViewById(R.id.button);
	            button.setTag(ssid);
	            button.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						HOButtonAdapter.this.remove(v.getTag().toString());
					}
	            });
            }
        }
        
        if (ssid != null) {
                TextView text = (TextView) v.findViewById(R.id.text);
                if (text != null) {
                      text.setText(ssid);
                }
                
                ImageButton button = (ImageButton) v.findViewById(R.id.button);
                if (button != null) {
                	button.setTag(ssid);
                }
        }
        
		return v;
	}
	
	@Override
	public void remove(String item) {
		super.remove(item);
		
		if (mRemovedCallback != null) {
			mRemovedCallback.onItemRemoved(item);
		}
	}
	
	public void setOnItemRemoved(OnItemRemoved callback) {
		mRemovedCallback = callback;
	}

}

package com.gimranov.zandy.client;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.TextView.BufferType;

import com.gimranov.zandy.client.data.Item;
import com.gimranov.zandy.client.task.ZoteroAPITask;

/**
 * This Activity handles displaying and editing tags. It works almost the same as
 * ItemDataActivity, using a simple ArrayAdapter on Bundles with the tag info.
 * 
 * @author ajlyon
 *
 */
public class TagActivity extends ListActivity {

	private static final String TAG = "com.gimranov.zandy.client.TagActivity";
	
	static final int DIALOG_TAG = 3;
	static final int DIALOG_CONFIRM_NAVIGATE = 4;	
	
	private Item item;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
                
        /* Get the incoming data from the calling activity */
        // XXX Note that we don't know what to do when there is no key assigned
        String itemKey = getIntent().getStringExtra("com.gimranov.zandy.client.itemKey");
        Item item = Item.load(itemKey);
        this.item = item;
        
        ArrayList<Bundle> rows = item.tagsToBundleArray();
        
        /* 
         * We use the standard ArrayAdapter, passing in our data as a Bundle.
         * Since it's no longer a simple TextView, we need to override getView, but
         * we can do that anonymously.
         */
        setListAdapter(new ArrayAdapter<Bundle>(this, R.layout.list_data, rows) {
        	@Override
        	public View getView(int position, View convertView, ViewGroup parent) {
        		View row;
        		
                // We are reusing views, but we need to initialize it if null
        		if (null == convertView) {
                    LayoutInflater inflater = getLayoutInflater();
        			row = inflater.inflate(R.layout.list_data, null);
        		} else {
        			row = convertView;
        		}
         
        		/* Our layout has just two fields */
        		TextView tvLabel = (TextView) row.findViewById(R.id.data_label);
        		TextView tvContent = (TextView) row.findViewById(R.id.data_content);
        		
        		if (getItem(position).getInt("type") == 1)
        			tvLabel.setText("Auto");
        		else
        			tvLabel.setText("User");
        		tvContent.setText(getItem(position).getString("tag"));
         
        		return row;
        	}
        });
        
        ListView lv = getListView();
        lv.setTextFilterEnabled(true);
        lv.setOnItemClickListener(new OnItemClickListener() {
        	// Warning here because Eclipse can't tell whether my ArrayAdapter is
        	// being used with the correct parametrization.
        	@SuppressWarnings("unchecked")
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        		// If we have a click on an entry, do something...
        		ArrayAdapter<Bundle> adapter = (ArrayAdapter<Bundle>) parent.getAdapter();
        		Bundle row = adapter.getItem(position);
        		
/* TODO Rework this logic to open an ItemActivity showing tagged items
        		if (row.getString("label").equals("url")) {
        			row.putString("url", row.getString("content"));
        			removeDialog(DIALOG_CONFIRM_NAVIGATE);
        			showDialog(DIALOG_CONFIRM_NAVIGATE, row);
        			return;
        		}
        		
        		if (row.getString("label").equals("DOI")) {
        			String url = "http://dx.doi.org/"+Uri.encode(row.getString("content"));
        			row.putString("url", url);
        			removeDialog(DIALOG_CONFIRM_NAVIGATE);
        			showDialog(DIALOG_CONFIRM_NAVIGATE, row);
        			return;
        		}
 */       		
				Toast.makeText(getApplicationContext(), row.getString("tag"), 
        				Toast.LENGTH_SHORT).show();
        	}
        });
        
        /*
         * On long click, we bring up an edit dialog.
         */
        lv.setOnItemLongClickListener(new OnItemLongClickListener() {
        	/*
        	 * Same annotation as in onItemClick(..), above.
        	 */
        	@SuppressWarnings("unchecked")
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
     			// If we have a long click on an entry, show an editor
        		ArrayAdapter<Bundle> adapter = (ArrayAdapter<Bundle>) parent.getAdapter();
        		Bundle row = adapter.getItem(position);
        		
    			removeDialog(DIALOG_TAG);
        		showDialog(DIALOG_TAG, row);
        		return true;
          }
        });

    }
    
	protected Dialog onCreateDialog(int id, Bundle b) {
		@SuppressWarnings("unused")
		final int type = b.getInt("type");
		final String tag = b.getString("tag");
		final String itemKey = b.getString("itemKey");
		AlertDialog dialog;
		
		switch (id) {
		/* Simple editor for a single tag */
		case DIALOG_TAG:			
			final EditText input = new EditText(this);
			input.setText(tag, BufferType.EDITABLE);
			
			dialog = new AlertDialog.Builder(this)
	    	    .setTitle("Edit Tag")
	    	    .setView(input)
	    	    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
	    	        @SuppressWarnings("unchecked")
					public void onClick(DialogInterface dialog, int whichButton) {
	    	            Editable value = input.getText();
	    	            Item.setTag(itemKey, tag, value.toString(), 0);
	    	            Item item = Item.load(itemKey);
	    	            ArrayAdapter<Bundle> la = (ArrayAdapter<Bundle>) getListAdapter();
	    	            la.clear();
	    	            for (Bundle b : item.tagsToBundleArray()) {
	    	            	la.add(b);
	    	            }
	    	            la.notifyDataSetChanged();
	    	        }
	    	    }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
	    	        public void onClick(DialogInterface dialog, int whichButton) {
	    	        	// do nothing
	    	        }
	    	    }).create();
			return dialog;
		case DIALOG_CONFIRM_NAVIGATE:
/*			dialog = new AlertDialog.Builder(this)
		    	    .setTitle("View this online?")
		    	    .setPositiveButton("View", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
		        			// The behavior for invalid URIs might be nasty, but
		        			// we'll cross that bridge if we come to it.
		        			Uri uri = Uri.parse(content);
		        			startActivity(new Intent(Intent.ACTION_VIEW)
		        							.setData(uri));
		    	        }
		    	    }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
		    	        public void onClick(DialogInterface dialog, int whichButton) {
		    	        	// do nothing
		    	        }
		    	    }).create();
			return dialog;*/
			return null;
		default:
			Log.e(TAG, "Invalid dialog requested");
			return null;
		}
	}
               
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.zotero_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case R.id.do_sync:
        	if (!ServerCredentials.check(getApplicationContext())) {
            	Toast.makeText(getApplicationContext(), "Log in to sync", 
        				Toast.LENGTH_SHORT).show();
            	return true;
        	}
        	Log.d(TAG, "Preparing sync requests");
        	new ZoteroAPITask(getBaseContext()).execute();
        	Toast.makeText(getApplicationContext(), "Started syncing...", 
    				Toast.LENGTH_SHORT).show();
        	return true;
        case R.id.do_new:
    		Bundle row = new Bundle();
    		row.putString("tag", null);
    		row.putString("itemKey", this.item.getKey());
    		row.putInt("type", 0);
			removeDialog(DIALOG_TAG);
    		showDialog(DIALOG_TAG, row);
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
}
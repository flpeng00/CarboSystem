package org.secmem.headtrackerdemo;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends ActionBarActivity {
	
	public static final int MSG_SOCKET_CONNECTED = 1;
	public static final int MSG_SOCKET_READWRITE = 2;
	public static final int MSG_SOCKET_DISCONNECTED = 3;
    
    private Messenger serviceMessenger;
    private TextView panValue;
    private TextView tilValue;
    private TextView rolValue;
    private TextView textViewPan;
    private TextView textViewTil;
    private TextView textViewRol;
    private TextView textViewDir; 
    private EditText editTextIP;
    private EditText editTextCam;
    private Button buttonSetDir;
    private Button buttonConnect;
    private Button buttonCamera;
    private SharedPreferences pref;
    private SharedPreferences.Editor prefEdit;
    private String ipAddress;
    
    private String TAG = "SocketTask";
    private String PREF_NAME = "HTDemoPref";
    private String PREF_KEY = "ServerAddress";
    private String PREF_CAM = "CamAddress";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bindService(new Intent(this, SensorService.class), conn, Context.BIND_AUTO_CREATE);
        
        panValue = (TextView)findViewById(R.id.textView4);
        tilValue = (TextView)findViewById(R.id.textView5);
        rolValue = (TextView)findViewById(R.id.textView6);
        textViewPan = (TextView)findViewById(R.id.textView1);
        textViewRol = (TextView)findViewById(R.id.textView2);
        textViewTil = (TextView)findViewById(R.id.textView3);
        textViewDir = (TextView)findViewById(R.id.textView_dir);
        editTextIP = (EditText)findViewById(R.id.edittext_ip);
        editTextCam = (EditText)findViewById(R.id.edittext_cam);
        buttonSetDir = (Button)findViewById(R.id.button_setdir);
        buttonConnect = (Button)findViewById(R.id.button_connect);
        buttonCamera = (Button)findViewById(R.id.button_camera);
        buttonConnect.setOnClickListener(onClickListener);
        buttonSetDir.setOnClickListener(onClickListener);
        buttonCamera.setOnClickListener(onClickListener);
        pref = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        editTextIP.setText(pref.getString(PREF_KEY, ""));
        editTextCam.setText(pref.getString(PREF_CAM, ""));
    }
    
	private ServiceConnection conn = new ServiceConnection(){
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			serviceMessenger = new Messenger(service);
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			serviceMessenger = null;
		}
	};

    View.OnClickListener onClickListener = new View.OnClickListener(){

        @Override
        public void onClick(View v) {
        	switch(v.getId()){
        	case R.id.button_setdir:
	        	try {
					serviceMessenger.send(Message.obtain(null, SensorService.MSG_SET_DIR));
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	        	break;
        	case R.id.button_connect:
        		ipAddress = editTextIP.getText().toString();
    			Log.d(TAG, ipAddress);
    			prefEdit = pref.edit();
    			prefEdit.putString(PREF_KEY, ipAddress);
    			prefEdit.commit();
            	try {
    				serviceMessenger.send(Message.obtain(null, SensorService.MSG_FPV_CONNECT, ipAddress));
    			} catch (RemoteException e) {
    				// TODO Auto-generated catch block
    				e.printStackTrace();
    			}
            	break;
        	case R.id.button_camera:
        		ipAddress = editTextIP.getText().toString();
        		prefEdit = pref.edit();
    			prefEdit.putString(PREF_CAM, ipAddress);
    			prefEdit.commit();
        		Intent intent = new Intent(MainActivity.this, CameraActivity.class);
        		intent.putExtra("CAMADDR", ipAddress);
        		MainActivity.this.startActivity(intent);
        	}
        }
    };
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    
    
    
	@Override
	protected void onPause() {
		unregisterReceiver(serviceConnReceiver);
		super.onPause();
	}

	@Override
	protected void onResume() {
		IntentFilter filter = new IntentFilter();
    	filter.addAction(HeadTrackerIntent.ACTION_GET_SENSORVALUES);
    	registerReceiver(serviceConnReceiver, filter);
		super.onResume();
	}

	private BroadcastReceiver serviceConnReceiver = new BroadcastReceiver(){
		
		@SuppressLint("NewApi")
		@Override
        public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if(HeadTrackerIntent.ACTION_GET_SENSORVALUES.equals(action)){
				textViewPan.setText("azi : " + intent.getIntExtra(HeadTrackerIntent.VALUE_CALCPAN, -1));
                textViewTil.setText("til : " + intent.getIntExtra(HeadTrackerIntent.VALUE_TIL, -1));
                textViewRol.setText("rol : " + intent.getIntExtra(HeadTrackerIntent.VALUE_ROL, -1));
                textViewDir.setText("Dir : " + intent.getIntExtra(HeadTrackerIntent.VALUE_DIR, -1));
                panValue.setText("pan : " + intent.getIntExtra(HeadTrackerIntent.VALUE_SERVOPAN, -1));
                tilValue.setText("til : " + intent.getIntExtra(HeadTrackerIntent.VALUE_SERVOTIL, -1));
                rolValue.setText("rol : " + intent.getIntExtra(HeadTrackerIntent.VALUE_SERVOROL, -1));
			}
        }
	};

}

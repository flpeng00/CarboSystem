package org.secmem.carbo;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

@SuppressLint("ShowToast")
public class MainActivity extends ActionBarActivity {
	
	private final String TAG = "MainActivity";
    private final String PREF_NAME = "CarboAppPref";
    private final String PREF_KEY = "DrvAddress";
	
	private boolean isDrvConnected = false;
	private boolean isFpvConnected = false;
	
	//private TextView txtContents;
	private Button btnDrvSocket;
	private Button btnFpvSocket;
	private Button btnExtSocket;
	private EditText editLocal;
	private EditText editDrv;
	private EditText editSerial;
	private SharedPreferences pref;
	private Messenger serviceMessenger = null;
	//private SurfacePreview mSurface;
	
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

	@SuppressLint("InlinedApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		bindService(new Intent(this, MainService.class), conn, Context.BIND_AUTO_CREATE);
		
		setContentView(R.layout.activity_main);
		editLocal = (EditText)findViewById(R.id.edit_localaddr);
		editDrv = (EditText)findViewById(R.id.edit_drvaddr);
		editSerial = (EditText)findViewById(R.id.edit_serial);
		btnDrvSocket = (Button)findViewById(R.id.btn_drvsocket);
		btnFpvSocket = (Button)findViewById(R.id.btn_fpvsocket);
		btnExtSocket = (Button)findViewById(R.id.btn_extsocket);
		btnDrvSocket.setOnClickListener(l);
		btnFpvSocket.setOnClickListener(l);
		btnExtSocket.setOnClickListener(l);
		pref = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
		editLocal.setText(getLocalIpAddress());
		editDrv.setText(pref.getString(PREF_KEY, ""));
		//mSurface = (SurfacePreview)findViewById(R.id.previewFrame);
		
	}
	
	

	@Override
	protected void onDestroy() {
		unbindService(conn);
		super.onDestroy();
	}



	private String getLocalIpAddress()
	 {
	  final String IP_NONE = "N/A";
	  final String WIFI_DEVICE_PREFIX = "eth";
	  
	  String LocalIP = IP_NONE;
	  try {
	         for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
	             NetworkInterface intf = en.nextElement();           
	             for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
	                 InetAddress inetAddress = enumIpAddr.nextElement();
	                 if (!inetAddress.isLoopbackAddress()) {
	                	 if(inetAddress instanceof Inet4Address){
	                	 if( LocalIP.equals(IP_NONE) )
	                	  LocalIP = inetAddress.getHostAddress().toString();
	                  	else if( intf.getName().startsWith(WIFI_DEVICE_PREFIX) )
	                	  LocalIP = inetAddress.getHostAddress().toString();
	                 	}
	                 }
	             }
	         }
	     } catch (SocketException e) {
	         Log.e(TAG, e.toString());
	     }
	     return LocalIP;
	 }


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	private OnClickListener l = new OnClickListener(){

		@SuppressLint("NewApi")
		@Override
		public void onClick(View v) {
			
			switch(v.getId()){
			
			case R.id.btn_drvsocket:
				if(!isDrvConnected){
					String addr = editDrv.getText().toString();
					SharedPreferences.Editor prefEdit = pref.edit();
	    			prefEdit.putString(PREF_KEY, addr);
	    			prefEdit.commit();
					try {
						serviceMessenger.send(Message.obtain(null, MainService.MSG_DRVSOCKET_CONNECT));
						btnDrvSocket.setText("DRV : Connecting...");
					} catch (RemoteException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else{
					try {
						serviceMessenger.send(Message.obtain(null, MainService.MSG_DRVSOCKET_DISCONNECT));
					} catch (RemoteException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				break;
			case R.id.btn_fpvsocket:
				Log.d(TAG, "isFpcConnected : " + isFpvConnected);
				if(!isFpvConnected){
					try {
						Log.d(TAG, "CONNECT");
						serviceMessenger.send(Message.obtain(null, MainService.MSG_FPVSOCKET_CONNECT));
						btnFpvSocket.setText("FPV : Connecting...");
					} catch (RemoteException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else{
					try {
						Log.d(TAG, "DISCONNECT");
						serviceMessenger.send(Message.obtain(null, MainService.MSG_FPVSOCKET_DISCONNECT));
					} catch (RemoteException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				break;
			case R.id.btn_extsocket:
				Log.d(TAG, "CAM On");
				Intent ipwebcam = 
					new Intent()
					.setClassName("com.pas.webcam.pro", "com.pas.webcam.Rolling")
					.putExtra("cheats", new String[] { 
							"reset(Port)",
							"set(HtmlPath,/sdcard/html/)",
							})
					.putExtra("Awake", true)
					.putExtra("hidebtn1", true);
				startActivity(ipwebcam);
				break;
			}
		}
	};
	
	private BroadcastReceiver serviceConnReceiver = new BroadcastReceiver(){
		
		@SuppressLint("NewApi")
		@Override
        public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if(MainServiceIntent.ACTION_DRV_CONNECTED.equals(action)){
				Log.d(TAG, "BR : DRV Connected");
				isDrvConnected = true;
				btnDrvSocket.setText("DRV : Connected");
			} else if(MainServiceIntent.ACTION_DRV_DISCONNECTED.equals(action)){
				Log.d(TAG, "BR : DRV Disconnected");
				isDrvConnected = false;
				btnDrvSocket.setText("DRV : Disconnected");
			} else if(MainServiceIntent.ACTION_FPV_CONNECTED.equals(action)){
				Log.d(TAG, "BR : FPV Connected");
				isFpvConnected = true;
				btnFpvSocket.setText("FPV : Connected");
			} else if(MainServiceIntent.ACTION_FPV_DISCONNECTED.equals(action)){
				Log.d(TAG, "BR : FPV Disconnected");
				isFpvConnected = false;
				btnFpvSocket.setText("FPV : Disconnected");
			} else if(MainServiceIntent.ACTION_SRL_CONNECTED.equals(action)){
				Log.d(TAG, "BR : Serial Connected");
				editSerial.setText("Connected");
			} else if(MainServiceIntent.ACTION_SRL_DISCONNECTED.equals(action)){
				Log.d(TAG, "BR : Serial Disconnected");
				editSerial.setText("Disconnected");
			}
        }
	};
	
    @Override
    protected void onPause() {
    	unregisterReceiver(serviceConnReceiver);
        super.onPause();
    }

    @Override
    protected void onResume() {
    	IntentFilter filter = new IntentFilter();
    	filter.addAction(MainServiceIntent.ACTION_DRV_CONNECTED);
    	filter.addAction(MainServiceIntent.ACTION_DRV_DISCONNECTED);
    	filter.addAction(MainServiceIntent.ACTION_FPV_CONNECTED);
    	filter.addAction(MainServiceIntent.ACTION_FPV_DISCONNECTED);
    	filter.addAction(MainServiceIntent.ACTION_SRL_CONNECTED);
    	filter.addAction(MainServiceIntent.ACTION_SRL_DISCONNECTED);
	    registerReceiver(serviceConnReceiver, filter);

        super.onResume();
    }
    
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return super.onOptionsItemSelected(item);
	}
	
}
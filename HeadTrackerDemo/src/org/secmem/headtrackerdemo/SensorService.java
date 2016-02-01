package org.secmem.headtrackerdemo;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

public class SensorService extends Service implements SensorEventListener{
	
	private static String TAG = "SensorService";
	
	private static final int SAMPLE_SIZE = 10;
	private static final int PAN_RANGE = 2000;
	private static final int TIL_RANGE = 1600;
	private static final int ROL_RANGE = 1600;
	private static final int PAN_NEUTRAL = 1500;
	private static final int TIL_NEUTRAL = 1500;
	private static final int ROL_NEUTRAL = 1500;
	public static final int MSG_FPV_CONNECT = 1; 
	public static final int MSG_FPV_DISCONNECT = 2;
	public static final int MSG_SET_DIR = 3;

	private static double pan;
	private static double rol;
	private static double til;
	private static int servoPan;
	private static int servoTil;
	private static int servoRol;
	private static int panSum = 0;
	private static int tilSum = 0;
	private static int rolSum = 0;
	
	private int servo_i = 1;
	private static double dir = 0;
	double calcPan;
	private String ipAddress;
    
    private SensorManager sensorManager;
    private Sensor acceleroSensor;
    private Sensor magnetoSensor;
    private float[] gravity = null;
    private float[] geomagnetic = null;
    private FpvSocketTask fpvSocketTask;
    private ExtSocketTask extSocketTask;
    private ServiceHandler mHandler = new ServiceHandler();
    private Messenger messenger = new Messenger(mHandler);
    
    private KalmanFilter kfPan;
    private KalmanFilter kfTil;
    private KalmanFilter kfRol;
    
    @SuppressLint("HandlerLeak")
	private class ServiceHandler extends Handler{
		
		@SuppressLint("NewApi")
		@Override
		public void handleMessage(Message msg) {
			switch(msg.what)
			{
			case MSG_FPV_CONNECT:
				ipAddress = (String)msg.obj;
				if(fpvSocketTask == null || fpvSocketTask.isCancelled()){
					fpvSocketTask = new FpvSocketTask();
					fpvSocketTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
				}
				if(extSocketTask == null || extSocketTask.isCancelled()){
					extSocketTask = new ExtSocketTask();
					extSocketTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
				}
				break;
			case MSG_FPV_DISCONNECT:
				break;
			case MSG_SET_DIR:
				dir = pan;
				break;
			}
		}
    };

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            gravity = event.values.clone();
        }

        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            geomagnetic = event.values.clone();
        }

        if (gravity != null && geomagnetic != null) {
            float R[] = new float[9];
            float I[] = new float[9];
            boolean success = SensorManager.getRotationMatrix(R, I, gravity, geomagnetic);

            if (success) {
                float orientation[] = new float[3];
                SensorManager.getOrientation(R, orientation);
                this.calcServoValue(orientation);
            }
        }
    }
    
    private void sendData(){
    	
    	Intent intent = new Intent(HeadTrackerIntent.ACTION_GET_SENSORVALUES);
    	intent.putExtra(HeadTrackerIntent.VALUE_DIR, (int)dir);
    	intent.putExtra(HeadTrackerIntent.VALUE_CALCPAN, (int)calcPan);
    	intent.putExtra(HeadTrackerIntent.VALUE_ROL, (int)rol);
    	intent.putExtra(HeadTrackerIntent.VALUE_TIL, (int)til);
    	intent.putExtra(HeadTrackerIntent.VALUE_SERVOPAN, servoPan);
    	intent.putExtra(HeadTrackerIntent.VALUE_SERVOROL, servoRol);
    	intent.putExtra(HeadTrackerIntent.VALUE_SERVOTIL, servoTil);
    	sendBroadcast(intent);
    }
    
    private double calcPanValue(double azi){
    	
    	double result = 0;
    	double sym = 0;
    	
    	if(dir < 0)
    		sym = dir + PAN_RANGE;
    	else
    		sym = dir - PAN_RANGE;
    	
    	if(dir < sym){
    		if(azi > sym)
    			result = ((-2*PAN_RANGE) - dir + azi); //(-2000 - dir) - (2000 - azimuth));
    		else
    			result = (azi - dir);
    	} else{
    		if(azi < sym)
    			result = ((2*PAN_RANGE) - dir + azi); // - dir - (-2000 - azimuth));
    		else
    			result = (azi - dir);
    	}
    	
    	if(result > (PAN_RANGE / 2))
    		return (PAN_RANGE / 2);
    	else if(result < (PAN_RANGE / -2))
    		return (PAN_RANGE / -2);
    	else
    		return result;
    		
    }
    
    private void calcServoValue(float ort[])
    {
    	int filteredPan;
    	int filteredTil;
    	int filteredRol;
    												// Range :
        pan = (ort[0] / Math.PI) * PAN_RANGE;		// -2000 to 2000
        calcPan = calcPanValue(pan);				// -1000 to 1000
        til = (ort[2] / Math.PI + 0.5) * TIL_RANGE;	// -800 to 800
        rol = (ort[1] / Math.PI) * ROL_RANGE;       // -800 to 800
        
        filteredPan = (int)kfPan.update(PAN_NEUTRAL - calcPan);
        filteredTil = (int)kfTil.update(TIL_NEUTRAL - til);
        filteredRol = (int)kfRol.update(ROL_NEUTRAL - rol);
        
        panSum = panSum + filteredPan;
        tilSum = tilSum + filteredTil;
        rolSum = rolSum + filteredRol;
        servo_i++;
        
        if(servo_i > SAMPLE_SIZE)
        {
        	servoPan = panSum / SAMPLE_SIZE;
        	servoTil = tilSum / SAMPLE_SIZE - 200;
        	servoRol = rolSum / SAMPLE_SIZE;
        	sendData();
        	servo_i = 1;
        	panSum = 0;
        	tilSum = 0;
        	rolSum = 0;
        	
        }
    }
    
    public class FpvSocketTask extends AsyncTask<Void, Void, Void>{
    	
    	private final int SERVER_PORT = 8001;
    	private Socket socket;
    	
    	@Override
    	protected Void doInBackground(Void... params) {
    		try {
    			Log.d(TAG, ipAddress);
    			socket = new Socket(ipAddress, SERVER_PORT);
    			DataOutputStream doStream = new DataOutputStream(socket.getOutputStream());
    			Log.d(TAG, "FPV Socket Connected");
    			
    			while(true)
    			{
    				doStream.writeInt(39);
    				doStream.writeInt(servoPan);
    				doStream.writeInt(servoTil);
    				doStream.writeInt(servoRol);    				
    				
    				try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						Log.d(TAG, e.toString());
						e.printStackTrace();
					}
    			}
    		} catch (UnknownHostException e) {
    			Log.d(TAG, e.toString());
    			e.printStackTrace();
    		} catch (IOException e) {
    			Log.d(TAG, e.toString());
    			e.printStackTrace();
    		}
    		return null;
    	}
    	
    	public void disconnect(){
    		try { 
    			if(socket != null){
    				socket.close();
					socket = null;
					Log.d(TAG, "FPV Socket Disconnected");
    			}
			} catch (IOException e) {
				Log.d(TAG, e.toString());
				e.printStackTrace();
			}
    		socket = null;
    	}
    }
    
    public class ExtSocketTask extends AsyncTask<Void, Void, Void>{
    	
    	private final int EXT_PORT = 8002;
    	private Socket socket;
    	private ServerSocket serverSocket;
    	private byte[] buffer = new byte[10];
   
    	
    	@Override
    	protected Void doInBackground(Void... params) {
    		while(true){
    			try {
    				Log.d(TAG, "Ext Socket Open");
    				if(serverSocket == null)
    					serverSocket = new ServerSocket(EXT_PORT);
    				if(socket == null)
    					socket = serverSocket.accept();
    				DataInputStream diStream = new DataInputStream(socket.getInputStream());	    			
	    			Log.d(TAG, "Ext Socket connected");
	    			diStream.read(buffer);
	    			if(buffer[0] == 's'){
						dir = pan;
	    			}
	    			diStream.close();
	    			diStream = null;
	    			disconnect();
				} catch (IOException e1) {
					disconnect();
					e1.printStackTrace();
				}
    		}
    	}
    	
    	public void disconnect(){
    		try {
    			if(socket != null){
    				socket.close();
					socket = null;
    			}
    			if(serverSocket != null){
    				serverSocket.close();
    				serverSocket = null;
    			}
    			Log.d(TAG, "EXT Socket Disconnected");
			} catch (IOException e) {
				Log.d(TAG, e.toString());
				e.printStackTrace();
			}
    		socket = null;
    	}
    }
 
    class KalmanFilter {
		private double Q = 0.00001; 
		private double R = 0.001;
		private double X = 0, P = 1, K;
		
		KalmanFilter(double initValue) {
			X = initValue;
		}
		
		private void measurementUpdate(){
			K = (P + Q) / (P + Q + R);
			P = R * (P + Q) / (R + P + Q);
		}
			 
		public double update(double measurement){
			measurementUpdate();
			X = X + (measurement - X) * K;
			 
			return X;
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public IBinder onBind(Intent intent) {
		Log.d(TAG, "Binding");
        sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        acceleroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetoSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        kfPan = new KalmanFilter(0.0f);
        kfTil = new KalmanFilter(0.0f);
        kfRol = new KalmanFilter(0.0f);
        sensorManager.registerListener(this, acceleroSensor, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, magnetoSensor, SensorManager.SENSOR_DELAY_FASTEST);
        return messenger.getBinder();
	}

	
	@Override
	public boolean onUnbind(Intent intent) {
		Log.d(TAG, "Unbinding");
		if(fpvSocketTask != null){
			fpvSocketTask.cancel(true);
			fpvSocketTask.disconnect();
		}
		if(extSocketTask != null){
			extSocketTask.cancel(true);
			extSocketTask.disconnect();
		}
		sensorManager.unregisterListener(this, acceleroSensor);
        sensorManager.unregisterListener(this, magnetoSensor);
		return super.onUnbind(intent);
	}
	
	

}

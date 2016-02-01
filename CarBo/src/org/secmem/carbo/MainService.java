package org.secmem.carbo;

import java.io.IOException;
import java.nio.charset.Charset;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

public class MainService extends Service{
	
	private static String TAG = "MainService";
	public static final int MSG_SERIAL_CONNECT = 1;
	public static final int MSG_SERIAL_DISCONNECT = 2;
	public static final int MSG_SERIAL_CONNECTED = 3;
	public static final int MSG_SERIAL_DISCONNECTED = 4;
	public static final int MSG_SERIAL_WRITE = 5;
	public static final int MSG_DRVSOCKET_CONNECT = 11;
	public static final int MSG_DRVSOCKET_DISCONNECT = 12;
	public static final int MSG_DRVSOCKET_CONNECTED = 13;
	public static final int MSG_DRVSOCKET_DISCONNECTED = 14;
	public static final int MSG_DRVSOCKET_READ = 10;
	public static final int MSG_FPVSOCKET_CONNECT = 21;
	public static final int MSG_FPVSOCKET_DISCONNECT = 22;
	public static final int MSG_FPVSOCKET_CONNECTED = 23;
	public static final int MSG_FPVSOCKET_DISCONNECTED = 24;
	public static final int MSG_FPVSOCKET_READ = 20;
	
	private SocketTask drvSocketTask = null;
	private SocketTask fpvSocketTask = null;
	private SerialTask serialTask = null;
	private ServiceHandler mHandler = new ServiceHandler();
	private Messenger messenger = new Messenger(mHandler);
	
	@Override
	public IBinder onBind(Intent intent) {
		Log.d(TAG, "Binding Service");
		serialTask = new SerialTask(this.getApplicationContext(), mHandler);
		serialTask.resume();
		return messenger.getBinder();
	}
	
	@Override
	public boolean onUnbind(Intent intent) {
		try {
			if(drvSocketTask != null)
			{
				drvSocketTask.cancel(true);
				drvSocketTask.disconnect();
			}
			if(fpvSocketTask != null)
			{
				fpvSocketTask.cancel(true);
				fpvSocketTask.disconnect();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return super.onUnbind(intent);
	}

	@Override
	public void onDestroy() {
		serialTask.pause();
		serialTask = null;
		super.onDestroy();
	}

	@SuppressLint("HandlerLeak")
	private class ServiceHandler extends Handler{
		
		@SuppressLint("NewApi")
		@Override
		public void handleMessage(Message msg) {
			String m;
			byte[] b;
			switch(msg.what)
			{
			case MSG_DRVSOCKET_CONNECT:
				if(drvSocketTask == null){
	    			drvSocketTask = new SocketTask(mHandler, SocketTask.DRV_SOCKET);
	    			drvSocketTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
				} else{
					try {
						drvSocketTask.disconnect();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				break;
			case MSG_DRVSOCKET_DISCONNECT:
				try {
					drvSocketTask.disconnect();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;
			case MSG_DRVSOCKET_CONNECTED:
				sendBroadcast(new Intent(MainServiceIntent.ACTION_DRV_CONNECTED));
				break;
			case MSG_DRVSOCKET_DISCONNECTED:
				m = "012501500";
				b = m.getBytes(Charset.forName("US-ASCII"));
				if(serialTask.isConnected())
					serialTask.write(makeDrvBuffer(b));
				drvSocketTask = null;
				sendBroadcast(new Intent(MainServiceIntent.ACTION_DRV_DISCONNECTED));
				break;
			case MSG_DRVSOCKET_READ:
				if(serialTask.isConnected())
					serialTask.write(makeDrvBuffer(msg.obj));				
				break;
			case MSG_FPVSOCKET_CONNECT:
				if(fpvSocketTask == null){
					fpvSocketTask = new SocketTask(mHandler, SocketTask.FPV_SOCKET);
					fpvSocketTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
				} else{
					try {
						fpvSocketTask.disconnect();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				break;
			case MSG_FPVSOCKET_DISCONNECT:
				try {
					fpvSocketTask.disconnect();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;
			case MSG_FPVSOCKET_CONNECTED:
				sendBroadcast(new Intent(MainServiceIntent.ACTION_FPV_CONNECTED));
				break;
			case MSG_FPVSOCKET_DISCONNECTED:
				if(serialTask.isConnected())
					serialTask.write(makeFpvBuffer(1500, 1500, 1500));
				fpvSocketTask = null;
				sendBroadcast(new Intent(MainServiceIntent.ACTION_FPV_DISCONNECTED));
				break;
			case MSG_FPVSOCKET_READ:
				if(serialTask.isConnected())
					serialTask.write(makeFpvBuffer(msg.arg1, msg.arg2, (Integer)msg.obj));
				break;
			case MSG_SERIAL_CONNECTED:
				sendBroadcast(new Intent(MainServiceIntent.ACTION_SRL_CONNECTED));
				break;
			case MSG_SERIAL_DISCONNECTED:
				sendBroadcast(new Intent(MainServiceIntent.ACTION_SRL_DISCONNECTED));
				break;				
			}
		}
		
	};
	
	private byte[] makeDrvBuffer(Object msg){
		
		byte[] socketBuffer = new byte[9];
		byte[] serialBuffer = new byte[11];
		socketBuffer = (byte[])msg;
		
		serialBuffer[0] = 0x02;
		serialBuffer[10] = 0x03;
		for(int i=1;i<10;i++){
			serialBuffer[i] = socketBuffer[i-1];
		}
		
		return serialBuffer;
	}
	
	private byte[] makeFpvBuffer(int pan, int til, int rol){
		
		byte[] serialBuffer = new byte[14];
		char[] temp;
		int i=0, j=0;
		
		serialBuffer[0] = 0x04;
		serialBuffer[13] = 0x05;
		
		temp = Integer.toString(pan).toCharArray();
		j = 0;
		for(i=0;i<4;i++){
			if(i<(4-temp.length))
				serialBuffer[1+i] = '0';
			else
				serialBuffer[1+i] = (byte) temp[j++];
		}
		
		temp = Integer.toString(til).toCharArray();
		j = 0;
		for(i=0;i<4;i++){
			if(i<(4-temp.length))
				serialBuffer[5+i] = '0';
			else
				serialBuffer[5+i] = (byte) temp[j++];
		}
		
		temp = Integer.toString(rol).toCharArray();
		j = 0;
		for(i=0;i<4;i++){
			if(i<(4-temp.length))
				serialBuffer[9+i] = '0';
			else
				serialBuffer[9+i] = (byte) temp[j++];
		}
		
		return serialBuffer;
	}
}

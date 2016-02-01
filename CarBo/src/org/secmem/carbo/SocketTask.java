package org.secmem.carbo;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.Charset;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class SocketTask extends AsyncTask<Void, Void, Void>{
	
	private final String TAG = "SocketTask";
	public static final int DRV_SOCKET = 0;
	public static final int FPV_SOCKET = 1;
	private int port;
	private ServerSocket serverSocket = null;
	private Socket socket = null;
	private Handler mHandler;
	private int mode;
	private int buffsize;
	private byte[] buffer;
	
	SocketTask(Handler h, int m)
	{
		mHandler = h;
		mode = m;
		if(mode == DRV_SOCKET){
			port = 8000;
			buffer = new byte[9];
		} else if(mode == FPV_SOCKET){
			port = 8001;
			buffer = new byte[11];
		} else{
			Log.d(TAG, mode + " SocketTask is not exist");
		}
	}
	
	
	@Override
	protected Void doInBackground(Void... params) {
		if(mode == DRV_SOCKET){
			try {
				Log.d(TAG, "DRV Server : Open");
				serverSocket = new ServerSocket(port);
				socket = serverSocket.accept();
				Log.d(TAG, "DRV Server : Connected");
				InputStream inputStream = socket.getInputStream();
				BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
				
				Log.d(TAG, "DRV : Connected");
				Message msg = Message.obtain(mHandler, MainService.MSG_DRVSOCKET_CONNECTED);
				mHandler.sendMessage(msg);
				
				while((buffsize = bufferedInputStream.read(buffer)) > 0)
				{
					if(buffsize == 9){	
						Log.d(TAG, new String(buffer));
						msg = Message.obtain(mHandler, MainService.MSG_DRVSOCKET_READ, buffer);
						mHandler.sendMessage(msg);
					}
				}
			} catch (UnknownHostException e) {
				Log.d(TAG, e.toString());
				e.printStackTrace();
			} catch (IOException e) {
				Log.d(TAG, e.toString());
				e.printStackTrace();
			}
			try {
				disconnect();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		} else if(mode == FPV_SOCKET){
			try {
				Log.d(TAG, "FPV Server : Open");
				int[] htValue = new int[3];
				serverSocket = new ServerSocket(port);
				socket = serverSocket.accept();
				Log.d(TAG, "FPV Server : Connected");
				InputStream inputStream = socket.getInputStream();
				DataInputStream diStream = new DataInputStream(inputStream);
				
				Message msg = Message.obtain(mHandler, MainService.MSG_FPVSOCKET_CONNECTED);
				mHandler.sendMessage(msg);
				
				while((diStream.readInt()) == 39)
				{
					for(int i=0;i<3;i++){
						htValue[i] = diStream.readInt();
					}
					msg = Message.obtain(mHandler, MainService.MSG_FPVSOCKET_READ, htValue[0], htValue[1], htValue[2]);
					mHandler.sendMessage(msg);
				}
			} catch (UnknownHostException e) {
				Log.d(TAG, e.toString());
				e.printStackTrace();
			} catch (IOException e) {
				Log.d(TAG, e.toString());
				e.printStackTrace();
				
				
			}
			try {
				disconnect();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Log.d(TAG, "Mode : " + mode + " asynctask exit");
			return null;
		} else return null;
	}
	
	public void disconnect() throws IOException
	{
		Log.d(TAG, "Mode : " + mode + " disconnecting");
		Message msg;
		
		if(socket != null)
			socket.close();
		socket = null;
		
		if(serverSocket != null)
			serverSocket.close();
		serverSocket = null;
		
		if(mode == DRV_SOCKET){
			msg = Message.obtain(mHandler, MainService.MSG_DRVSOCKET_DISCONNECTED);
			mHandler.sendMessage(msg);
		} else if(mode == FPV_SOCKET){
			msg = Message.obtain(mHandler, MainService.MSG_FPVSOCKET_DISCONNECTED);
			mHandler.sendMessage(msg);
		}
		
		Log.d(TAG, "Mode : " + mode + " disconnected");
	}
	
}

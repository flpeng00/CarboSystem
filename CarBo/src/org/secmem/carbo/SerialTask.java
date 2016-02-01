package org.secmem.carbo;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

public class SerialTask {
	
	private UsbManager usbManager = null;
	private UsbSerialDriver usbSerialDriver = null;
	private SerialInputOutputManager serialIOManager;
	private final String TAG = "SerialTask";
	private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
	private Handler mHandler;
	
	
	@SuppressLint("InlinedApi")
	SerialTask(Context c, Handler h){
		usbManager = (UsbManager) c.getSystemService(Context.USB_SERVICE);
		usbSerialDriver = UsbSerialProber.acquire(usbManager);
		mHandler = h;
	}
	
	public void pause(){
		if (usbSerialDriver != null) {
            try {
                usbSerialDriver.close();
            } catch (IOException e) {
                // Ignore.
            }
            usbSerialDriver = null;
        }
	}
	
	public void resume(){
        Log.d(TAG, "Resumed, usbSerialDriver=" + usbSerialDriver);
        if (usbSerialDriver == null) {
        	Message msg = Message.obtain(mHandler, MainService.MSG_SERIAL_DISCONNECTED);
    		mHandler.sendMessage(msg);
        } else {
            try {
                usbSerialDriver.open();
                usbSerialDriver.setBaudRate(115200);
            } catch (IOException e) {
                Log.e(TAG, "Error setting up device: " + e.getMessage(), e);
                try {
                    usbSerialDriver.close();
                } catch (IOException e2) {
                    // Ignore.
                }
                usbSerialDriver = null;
                Message msg = Message.obtain(mHandler, MainService.MSG_SERIAL_DISCONNECTED);
        		mHandler.sendMessage(msg);
                return;
            }
        }
        onDeviceStateChange();
	}
	
	public boolean isConnected(){
		if(usbSerialDriver == null)
			return false;
		else
			return true;
	}
	
    public void onDeviceStateChange() {
        stopIoManager();
        startIoManager();
    }
	
	public void openUsbSerial() {
		if(usbSerialDriver != null) {
			try {
				Log.d(TAG, "openUsbSerial...");
				usbSerialDriver.open();
				usbSerialDriver.setBaudRate(115200);
				Message msg = Message.obtain(mHandler, MainService.MSG_SERIAL_CONNECTED);
        		mHandler.sendMessage(msg);
			} catch (IOException e) {
				e.printStackTrace();
				Log.d(TAG, "openUsbSerial Failed");
			}
		}
	}
	
	public void closeUsbSerial() {
		try {
			
			Log.d(TAG, "Stop USB Serial");
			usbSerialDriver.close();
			Message msg = Message.obtain(mHandler, MainService.MSG_SERIAL_DISCONNECTED);
			mHandler.sendMessage(msg);
		} catch (IOException e) {
			e.printStackTrace();
			Log.d(TAG, "stopUsbSerial Failed");
		}
	}
	
    private void stopIoManager() {
        if (serialIOManager != null) {
            Log.i(TAG, "Stopping I/O manager");
            serialIOManager.stop();
            serialIOManager = null;
        }
    }

    private void startIoManager() {
        if (usbSerialDriver != null) {
            Log.i(TAG, "Starting io manager ..");
            serialIOManager = new SerialInputOutputManager(usbSerialDriver, serialIOListener);
            mExecutor.submit(serialIOManager);
        }
    }
    
    private final SerialInputOutputManager.Listener serialIOListener = 
    		new SerialInputOutputManager.Listener() {
    	@Override
    	public void onRunError(Exception e) {
    	}
    	
    	@Override
    	public void onNewData(final byte[] data) {
    		Message msg = Message.obtain(mHandler, MainService.MSG_SERIAL_WRITE, data);
    		mHandler.sendMessage(msg);
		}
			
	};
	
	public int write(byte[] buffer){
		if(usbSerialDriver == null)
			return -1;
		
		try {
			usbSerialDriver.write(buffer, buffer.length);
			return 0;
		} catch (IOException e) {
			e.printStackTrace();
			return 1;
		}
	}
}

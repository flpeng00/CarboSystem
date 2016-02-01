package com.example.cameratest2;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.LinkedList;


import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {
	Button mShutter;
	SurfacePreview mSurface;
	String mRootPath;


	static final String PICFOLDER = "CameraTest";

    @Override   
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        
        mSurface = (SurfacePreview)findViewById(R.id.previewFrame);
        
    }
}


class SurfacePreview extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback{
	 int count=0;
    SurfaceHolder holder;   //서피스홀더
    Camera cam=null;        //카메라
    byte[] imgdata={0,0};   //프레임 버퍼 변수
    ServerSocket listener;
    boolean running=false;
	String ipAddress;
	BufferedReader in;
	BufferedOutputStream out;
	TextView tv;
    public static LinkedList<Socket> clientList = new LinkedList<Socket>();
    
    boolean socketon=false;
    
    public SurfacePreview(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);

    }
    public SurfacePreview(Context context) {
        super(context);
        init(context);
    }
    public void init(Context context){
       
        holder=getHolder();
        holder.addCallback(this);
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
            getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
       
    }
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
            int height) {
        cam.setDisplayOrientation(90);      //카메라 각도를 포트레이트로(90도)
        cam.startPreview();                 //프리뷰 시작
    }
    @SuppressWarnings("deprecation")
	@Override
    public void surfaceCreated(SurfaceHolder holder) {

        cam=Camera.open();//카메라 객체를 오픈(퍼미션 되어있어야 됨)
    	// soc.Connect("112.108.39.250", 8002);
    	try{
            cam.setPreviewDisplay(holder);  //프리뷰를 홀더로
        }catch(Exception e){
            e.printStackTrace();
        }
     //  params.setPreviewFormat(ImageFormat.JPEG);
    	open();
        cam.setPreviewCallback(this);
    }
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        cam.stopPreview();
        cam.setPreviewCallback(null); 
        cam.release();                      //카메라 죽이기
       
        cam=null;
      
    }

    public void open() {
    	//soc= new clientSocketChannel("112.108.39.163",8002);
    	//웹서버 쓰레드 생성
    	ipAddress=getLocalIpAddress();
    	startServer();
    }
    
    //서버 시작
    public void startServer() {
		Thread worker = new Thread() {
		public void run() {
			try {
				InetAddress ipadr = InetAddress.getByName(ipAddress);
				 listener = new ServerSocket(8080,0,ipadr);
				 running=true;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		};
		worker.start();
		
		Thread worker2 = new Thread() {
		public void run() {
			while( true ) {
				if(running) {
					try {
						Socket client = listener.accept();
					    
						Log.d("ldh","connect");
						
						clientList.add(client);
						
						 in = new BufferedReader(new InputStreamReader(client.getInputStream()));
						
						 in.readLine().trim();

						 out = new BufferedOutputStream(client.getOutputStream());
						 
				    	  out.write(imgdata);
				    	  out.flush();
						  clientList.remove(client);  
						  client.close();

						 
					} catch (IOException e) {
						Log.e("Webserver", e.getMessage());
					}
				}
			}
		}
		};
		worker2.start();
    }
    
    //아이피 가져오기
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
	         
	     }
	     return LocalIP;
	 }
    
	@Override
	public void onPreviewFrame(byte[] data, Camera camera) {		
		Camera.Parameters parameters = camera.getParameters();
		//parameters.setJpegQuality(50);
	    int width = parameters.getPreviewSize().width;
	    int height = parameters.getPreviewSize().height;
	    YuvImage yuv = new YuvImage(data, parameters.getPreviewFormat(), width, height, null);
	    ByteArrayOutputStream out = new ByteArrayOutputStream();
	    
	 //   YUVtoRBG(null, data, width, height);
	    yuv.compressToJpeg(new Rect(0, 0, width, height), 50, out);
	   
	    byte[] bytes = out.toByteArray();

	    Bitmap image = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
	    
	    Bitmap resized = Bitmap.createScaledBitmap(image, 460, 320, true);
	    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
	    resized.compress(Bitmap.CompressFormat.JPEG, 30, byteArrayOutputStream);
	    bytes=byteArrayOutputStream.toByteArray();
	    imgdata=bytes;
	}
}  


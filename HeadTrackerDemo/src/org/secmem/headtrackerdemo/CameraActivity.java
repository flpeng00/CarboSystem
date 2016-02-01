package org.secmem.headtrackerdemo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;


public class CameraActivity extends Activity {

	private String TAG = "CameraActivity";
	private WebView webView;
	private int panValue;

	@SuppressLint("SetJavaScriptEnabled")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Window win = getWindow();
		win.requestFeature(Window.FEATURE_NO_TITLE);
		win.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

		setContentView(R.layout.activity_camera);
		
		Bundle extras = getIntent().getExtras();
		String addr = extras.getString("CAMADDR");
		
		webView = (WebView)findViewById(R.id.webView);
		webView.addJavascriptInterface(new WebAppInterface(this), "Android"); 
	    WebSettings webSettings = webView.getSettings();
	    webSettings.setJavaScriptEnabled(true);
	    webSettings.setSupportMultipleWindows(false);
	    webView.setWebChromeClient(new WebChromeClient() {});
	    webView.loadUrl("http://" + addr + ":8080");
	}
	
    public class WebAppInterface {
        Context mContext;

        /** Instantiate the interface and set the context */
        WebAppInterface(Context c) {
            mContext = c;
        }

        /** Show a toast from the web page */
        @JavascriptInterface
        public void getFromJS(String toast) { 
           if(toast.equals("close"))
           {
              finish();
           }
        }
    }

	@Override
	protected void onResume() {
		IntentFilter filter = new IntentFilter();
    	filter.addAction(HeadTrackerIntent.ACTION_GET_SENSORVALUES);
    	registerReceiver(serviceConnReceiver, filter);
		super.onResume();
	}
	
	
	@Override
	protected void onPause() {
		unregisterReceiver(serviceConnReceiver);
		super.onPause();
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
	}
	
	private BroadcastReceiver serviceConnReceiver = new BroadcastReceiver(){
		
		@SuppressLint("NewApi")
		@Override
        public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if(HeadTrackerIntent.ACTION_GET_SENSORVALUES.equals(action)){
				int panValue = intent.getIntExtra(HeadTrackerIntent.VALUE_SERVOPAN, 1500);
			}
        }
	};


}

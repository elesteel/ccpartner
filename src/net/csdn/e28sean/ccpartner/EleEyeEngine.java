package net.csdn.e28sean.ccpartner;

import android.util.Log;
import net.csdn.e28sean.ccpartner.R;

public class EleEyeEngine {
	private static final String TAG = "ChessBoard";
	
	// should be called in a service thread.
	// this function will not return
	public static native void run();
	
	// run in main thread. the native code will handle the multi-thread 
	public static native void send( String cmd );
	
	// recv is a callback function call by native code in service thread.
	public static void recv( String response ) {
		Log.v(TAG,"get response: " + response );
	}
	
	static {
		System.loadLibrary("eleeye");
	}
}

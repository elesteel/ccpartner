package net.csdn.e28sean.ccpartner;


import java.io.File;
import java.io.InputStream;
import net.csdn.e28sean.ccpartner.R;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;


public class EleEyeService extends Service {

	private final static String TAG = "ccpartner";
	private final static String ENGINE_FILE = "/data/data/net.csdn.e28sean.ccpartner/eleeye";
	
	private final IBinder mBinder = new LocalBinder();
	private Engine mEngine = null;
	
	
	
	public class LocalBinder extends Binder {
		EleEyeService getService() {
			return EleEyeService.this;
		}
	}
	
	@Override
	public IBinder onBind(Intent arg0) {
		return mBinder;
	}
	
	
	@Override
	public void onCreate() {
		super.onCreate();

		int ret = 0;
		File bookFile = new File("/data/data/net.csdn.e28sean.ccpartner/BOOK.DAT");
		if( !bookFile.exists() ) {
			try {
				InputStream is = getAssets().open("book.dat");
				ret = CcUtil.writeToFile(is, bookFile);
			} catch ( Exception e ) {
			}
		}
		
		File engineFile = new File("/data/data/net.csdn.e28sean.ccpartner/eleeye");
		
		if( !engineFile.exists() ) {
			try {
				InputStream is = getAssets().open("eleeye");
				ret = CcUtil.writeToFile(is, engineFile);
			} catch ( Exception e ) {
				ret = -1;
			}
		}
		
		if( ret != 0 )
			return;
		
		mEngine = new Engine(ENGINE_FILE);
		mEngine.open();		
	}

	@Override
	public void onDestroy() {
		if( mEngine != null ) {
			mEngine.close();
		}
		super.onDestroy();
	}
	
	public Engine getEngine() {
		return mEngine;
	}
}

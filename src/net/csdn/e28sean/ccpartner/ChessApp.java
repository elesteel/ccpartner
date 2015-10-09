package net.csdn.e28sean.ccpartner;

import android.app.Application;
import android.content.Context;
import android.content.res.Resources;
import android.media.AudioManager;
import android.media.SoundPool;
import android.util.SparseIntArray;
import net.csdn.e28sean.ccpartner.R;



public class ChessApp extends Application implements SoundPool.OnLoadCompleteListener {
	
	private static Context mContext;
	private static int[] soundEffects;
	private int loadIndex = 0;
	private SoundPool mSoundPool = null;
	private SparseIntArray mSoundMap = null;


	@Override
	public void onCreate() {

		super.onCreate();		
		mContext = this;
		mSoundMap = new SparseIntArray();
		soundEffects = new int[8];
		soundEffects[0] = R.raw.click;
		soundEffects[1] = R.raw.move;
		soundEffects[2] = R.raw.capture;
		soundEffects[3] = R.raw.check;
		soundEffects[4] = R.raw.illegal;
		soundEffects[5] = R.raw.win;
		soundEffects[6]	= R.raw.draw;
		soundEffects[7] = R.raw.loss;
		
		loadIndex = 0;
		mSoundPool = new SoundPool(3, AudioManager.STREAM_MUSIC, 100);
		mSoundPool.setOnLoadCompleteListener(this);
		mSoundPool.load(this,soundEffects[loadIndex],1);
	}
	
	
	public static Context getContext() {
		return mContext;
	}
	
	public static Resources res() {
		return mContext.getResources();
	}
	
	public static int getBookResId() {
		return R.raw.book;
	}
	
	public void playSoundEffect( int id ) {
		int sound = mSoundMap.get(id, -2);
		if( sound != -2 ) {
			mSoundPool.play(sound, 1.0f, 1.0f, 0, 0, 1.0f);
		}
	}

	@Override
	public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
		if( status == 0 ) {
			mSoundMap.put(soundEffects[loadIndex], sampleId);
		}
		loadIndex++;
		if( loadIndex < soundEffects.length ) {
			soundPool.load(this, soundEffects[loadIndex], 1);
		}
	}
	
}

package net.csdn.e28sean.ccpartner;

import net.csdn.e28sean.ccpartner.Engine.Callback;
import net.csdn.e28sean.ccpartner.R;

public class SearchRequest {
	String mPosition;
	boolean mIsDraw;
	int mTime;
	Callback mCallback;
	
    SearchRequest( String position, int time, boolean isDraw, Callback listener ) {
    	mPosition = position;
    	mIsDraw = isDraw;
    	mTime = time;
    	mCallback = listener;
    }
}

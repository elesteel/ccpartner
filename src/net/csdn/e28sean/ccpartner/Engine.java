package net.csdn.e28sean.ccpartner;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;


public class Engine {
	private final static String TAG = "ccpartner";
	
	public static final int IDLE_UNLOAD = 0; // 没有加载引擎
	public static final int IDLE_READY = 1;  // 引擎待命
	public static final int IDLE_REST = 2;   // 引擎休息
	public static final int IDLE_PONDER = 3; // 后台思考结束，等待对手走完一步，再做相应处理
	public static final int BUSY_WAIT = 4;   // 等待引擎停止思考
	public static final int BUSY_ANALYZE = 5;// 引擎以分析方式思考
	public static final int BUSY_THINK = 6;  // 引擎正常思考
	public static final int BUSY_PONDER = 7; // 引擎后台思考

	
	private String mEngineFile = null;
	private Process mEngineProcess = null;
	private OutputStreamWriter mWriter = null;
	private BufferedReader mReader = null;
	private ReadThread mReadThread = null;
	private ReadHandler mReadHandler = null;
	private HandlerThread mWriteThread = null;
	private WriteHandler mWriteHandler = null;
	
	private SearchRequest mCurrentSearchRequest = null;
	private SearchRequest mPendSearchRequest = null;
	
	
	private int mStatus = IDLE_UNLOAD;
	private boolean mIsClosed = false;
	
	public interface Callback {
		public void onThinkResult(String result);
	}

	
	private final class ReadHandler extends Handler {
		public ReadHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) {
			if( msg.what == 1 ) {
				String line = (String) msg.obj;
				handleLine(line);
			}
		}
		
	}
	
	private final class WriteHandler extends Handler {
		private final String newline = System.getProperty("line.separator");
		
		public WriteHandler(Looper looper) {
			super(looper);

		}

		@Override
		public void handleMessage(Message msg) {
			
			if( msg.what == 1 ) {
				if( mWriter != null ) {
					try {
						String cmd = (String) msg.obj;

						mWriter.write(cmd);
						mWriter.flush();
					} catch ( Exception e ) {

					}
				}
			}
		}
		
		public void sendCommand( String command ) {
			Log.d(TAG, "engine <-- " + command);
			Message msg = obtainMessage(1, command + newline);
			sendMessage(msg);
		}
	}

	
	private final class ReadThread extends Thread {

		@Override
		public void run() {

			if( mReader == null ) {
				return;
			}
			
			String line = null;
			try {
				while( (line = mReader.readLine()) != null ) {

					mReadHandler.sendMessage( mReadHandler.obtainMessage(1, line) );
				}

			} catch ( Exception e ) {

			} finally {
				try {
				    mReader.close();
				} catch ( Exception e ) {
				}
			}
		}
	}
	
	Engine( String file ) {
		mEngineFile = file;
	}
	
	
	public int open() {
		if( mStatus != IDLE_UNLOAD ) {
			return -1;
		}
		
		if( mEngineFile == null ) {
			return -1;
		}
		
		int ret = CcUtil.makeFileExecutable( new File( mEngineFile) );
		if( ret != 0 ) {
			return -1;
		}
		
		try {
			mIsClosed = false;
			mEngineProcess = new ProcessBuilder().command( mEngineFile ).start();
			mWriter = new OutputStreamWriter( mEngineProcess.getOutputStream() );
			mReader = new BufferedReader( new InputStreamReader( mEngineProcess.getInputStream()) );
			mReadThread = new ReadThread();
			mReadThread.start();
			mReadHandler = new ReadHandler(Looper.getMainLooper());
			mWriteThread = new HandlerThread("EleeyeWriter");
			mWriteThread.start();
			mWriteHandler = new WriteHandler(mWriteThread.getLooper());
			mWriteHandler.sendCommand("ucci");
		} catch ( Exception e ) {
		}
		
		return 0;
	}
	
	public void close() {
		mIsClosed = true;
		if( mStatus == IDLE_READY ) {
			mWriteHandler.sendCommand("quit");
		} else if ( mStatus == BUSY_THINK ) {
			mWriteHandler.sendCommand("stop");
		} else if ( mStatus == IDLE_UNLOAD ){
			clean();
		}
	}
	
	private void clean() {
		if( mReader != null ) {
			try {
				mReader.close();
			} catch ( Exception e ) {

			}
			mReader = null;
		}

		if( mWriter != null ) {
			try {
				mWriter.close();
			} catch ( Exception e ) {

			}
			mWriter = null;
		}
		mWriteThread.quit();
		mEngineProcess.destroy();
	}
	
	public void cancelSearch() {
		mCurrentSearchRequest = null;
		mPendSearchRequest = null;
		
		if( mStatus == BUSY_THINK ) {
			mWriteHandler.sendCommand("stop");
		}
	}
	
	public int requestSearch( SearchRequest request ) {
		if( mStatus != IDLE_READY ) {
			mPendSearchRequest = request;
			return 1;
		}
		
		mCurrentSearchRequest = request;
		goSearch();
		return 0;
	}
	
	public int requestSearch( String position, int time, boolean isDraw, Callback listener ) {
		SearchRequest request = new SearchRequest(position, time, isDraw, listener);
		return requestSearch( request );
	}
	
	private void goSearch() {
		SearchRequest request = mCurrentSearchRequest;
		mWriteHandler.sendCommand("position " + request.mPosition);
		StringBuilder go = new StringBuilder();
		go.append("go ");
		if( request.mIsDraw ) {
			go.append("draw ");
		}
		//go.append("time ").append( request.mTime * 1000);
		go.append("depth ").append(request.mTime + 3);
		mWriteHandler.sendCommand(go.toString());
		mStatus = BUSY_THINK;
	}
	
	
	
	
	private void handleLine( String line ) {
		Log.d(TAG, "engine --> " + line);
		if( mStatus == IDLE_UNLOAD ) {
			handleInitLine( line );
		} else if( mStatus == IDLE_READY ) {
			handleIdleLine( line );
		} else if( mStatus == BUSY_THINK ) {
			handleBusyThinkLine(line);
		}
	}
	
	private void handleIdleLine( String line ) {
		if( line.startsWith("bye") ) {
			mStatus = IDLE_UNLOAD;
			clean();
		}
	}
	
	private void handleInitLine( String line ) {
		if( line.startsWith("ucciok")) {
			mStatus = IDLE_READY;
			if( mIsClosed ) {
				mWriteHandler.sendCommand("quit");
				return;
			}
			
			if( mPendSearchRequest != null ) {
				mCurrentSearchRequest = mPendSearchRequest;
				mPendSearchRequest = null;
				goSearch();
			}
		}
	}
	
	private void handleBusyThinkLine( String line ) {
		if( line.startsWith("bestmove") || line.startsWith("nobestmove") ) {
			mStatus = IDLE_READY;
			
			if( mCurrentSearchRequest != null ) {
				if( mCurrentSearchRequest.mCallback != null ) {
					mCurrentSearchRequest.mCallback.onThinkResult( line );
				}
			} else {
				// This search request must have been cancelled! Do nothing.
			}
			
			if( mIsClosed ) {
				mWriteHandler.sendCommand("quit");
				return;
			}
			
			if( mPendSearchRequest != null ) {
				mCurrentSearchRequest = mPendSearchRequest;
				mPendSearchRequest = null;
				goSearch();
			}
		} 
	}
}

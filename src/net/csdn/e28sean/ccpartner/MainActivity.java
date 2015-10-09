package net.csdn.e28sean.ccpartner;


import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Stack;

import net.csdn.e28sean.ccpartner.EleEyeService.LocalBinder;
import net.csdn.e28sean.ccpartner.R;
import xqwlight.Position;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends FragmentActivity implements ChessBoard.EventListener, Engine.Callback {
	//private static final String TAG = "ccpartner";
	
	public static final int BIT_DRAW = 1;
	public static final int BIT_REPEAT = 2;
	public static final int BIT_RED_WIN = 4;
	public static final int BIT_BLACK_WIN = 8;
	
	public static final int RESULT_TBD = 0;
	public static final int RESULT_RED_WIN = 4;
	public static final int RESULT_RED_LONG_CHECK_LOSS = 10;
	public static final int RESULT_BLACK_WIN = 8;
	public static final int RESULT_BLACK_LONG_CHECK_LOSS = 6;
	public static final int RESULT_DRAW = 3;
	
    public static final int MOVE_MODE_COMPUTER_BLACK = 0;
    public static final int MOVE_MODE_COMPUTER_RED = 1;
    public static final int MOVE_MODE_COMPUTER_NONE = 2;
	
	private ChessBoard mChessBoard = null;
	private Position mPosition = new Position();
	private Stack<Integer> mHistory = null;
	private int mResult = RESULT_TBD;
	private int mMoveMode = MOVE_MODE_COMPUTER_BLACK;
	private int mLevel = 0;
	private String mStartupFen = Position.STARTUP_FEN[0];
	
	private int mScoreRedWin = 0;
	private int mScoreRedDraw = 0;
	private int mScoreRedLoss = 0;
	private int mScoreBlackWin = 0;
	private int mScoreBlackDraw = 0;
	private int mScoreBlackLoss = 0;
	
	private Button mButtonSurrender = null;
	private Button mButtonDraw = null;
	private Button mButtonNewGame = null;
	private TextView mViewResult = null;
	
	private Engine mEngine = null;
	private EleEyeService mEleeyeService = null;
	private boolean mEleeyeBinded = false;
	private SearchRequest mSearchRequest = null;
	private boolean mIsRequestDraw = false;
	
	public static class RestartDialogFragment extends DialogFragment {

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setMessage("确定要认输吗？")
			       .setPositiveButton("是的", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						((MainActivity)getActivity()).surrender(false);
					}
				}).setNegativeButton("再想想", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						
					}
				});
			return builder.create();
		}
		
	}
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.main);
	    
	    setVolumeControlStream(AudioManager.STREAM_MUSIC);
	    
	    mButtonSurrender = (Button) findViewById(R.id.button_surrender);
	    mButtonDraw = (Button) findViewById(R.id.button_draw);
	    mButtonNewGame = (Button) findViewById(R.id.button_newgame);
	    
	    SharedPreferences sp = getPreferences( MODE_PRIVATE );
	    mScoreRedWin = sp.getInt("SCORE_RED_WIN", 0);
	    mScoreRedDraw = sp.getInt("SCORE_RED_DRAW", 0);
	    mScoreRedLoss = sp.getInt("SCORE_RED_LOSS", 0);
	    mScoreBlackWin = sp.getInt("SCORE_BLACK_WIN", 0);
	    mScoreBlackDraw = sp.getInt("SCORE_BLACK_DRAW", 0);
	    mScoreBlackLoss = sp.getInt("SCORE_BLACK_LOSS", 0);
	    
	    // Load from file
	    try {
	        FileInputStream fis = openFileInput("aspect");
	        ObjectInputStream ois = new ObjectInputStream( fis );
	        mLevel = (Integer) ois.readObject();
	        mMoveMode = (Integer) ois.readObject();
	        mStartupFen = (String)ois.readObject();
	        mHistory = (Stack<Integer>)ois.readObject();
	        
	        mPosition.fromFen( mStartupFen );
	        for( Integer i : mHistory ) {
	        	mPosition.makeMove( i & 0xffff);
	        	if( mPosition.captured() ) {
	        		mPosition.setIrrev();
	        	}
	        }
	    } catch ( Exception e ) {
	    	mLevel = sp.getInt("PREF_LEVEL", 0);
	    	mMoveMode = sp.getInt("PREF_MOVE_MODE", MOVE_MODE_COMPUTER_BLACK);
	    	mStartupFen = Position.STARTUP_FEN[0];
	    	mPosition.fromFen(mStartupFen);
	    	mHistory = new Stack<Integer>();
	    }
	    
	    mViewResult = (TextView) findViewById(R.id.view_result);
        
	    mChessBoard = (ChessBoard) findViewById(R.id.board);
	    mChessBoard.setOnMoveListener(this);
	    mChessBoard.load( mPosition, mMoveMode );
	    if( !mHistory.empty() ) {
	    	int i = mHistory.peek();
	    	mChessBoard.setLastMove( i & 0xffff );
	    }
	    
	    Button regret = (Button) findViewById(R.id.button_regret);
	    regret.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				regret();
			}
		});
	    
	    
	    mButtonSurrender.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				DialogFragment dialog = new RestartDialogFragment();
				dialog.show(getSupportFragmentManager(), "RestartDialogFragment");
			}
		});
	    
	    
	    mButtonNewGame.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mViewResult.setVisibility(View.GONE);
				mButtonNewGame.setVisibility(View.GONE);
				mButtonDraw.setVisibility(View.VISIBLE);
				mButtonSurrender.setVisibility(View.VISIBLE);
				restart();
			}
		});
	    
	    mButtonDraw.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				requestDraw();
			}
		});
	}
	
	private ServiceConnection mEleeyeConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName arg0, IBinder arg1) {
			LocalBinder binder = (LocalBinder) arg1;
			mEleeyeService = binder.getService();
			mEleeyeBinded = true;
			if( mSearchRequest != null ) {
				mEleeyeService.getEngine().requestSearch( mSearchRequest );
				mSearchRequest = null;
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			// TODO Auto-generated method stub
			mEleeyeBinded = false;
			mEleeyeService = null;
		}
		
	};

	@Override
	protected void onStart() {
		super.onStart();
		
		// TODO: bind service
		Intent intent = new Intent(this, EleEyeService.class);
		bindService(intent, mEleeyeConnection, Context.BIND_AUTO_CREATE);
		
		if( (mPosition.sdPlayer == 0 && mMoveMode == MOVE_MODE_COMPUTER_RED)
				|| (mPosition.sdPlayer == 1 && mMoveMode == MOVE_MODE_COMPUTER_BLACK) ) {
			mChessBoard.setClickable(false);
			computerMove();
		}
	}

	@Override
	protected void onStop() {

		if( mEleeyeBinded ) {
			unbindService(mEleeyeConnection);
			mEleeyeBinded = false;
		}
		
		if( mEngine != null ) {
			mEngine.cancelSearch();
		}
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		if( mResult != RESULT_TBD ) {
			prepareNewGame();
		}
		
		// Save to file
		try {
			FileOutputStream fos = openFileOutput("aspect",  MODE_PRIVATE);
			ObjectOutputStream oos = new ObjectOutputStream( fos );
			oos.writeObject(Integer.valueOf(mLevel));
			oos.writeObject(Integer.valueOf(mMoveMode));
			oos.writeObject(mStartupFen);
			oos.writeObject(mHistory);
		} catch ( Exception e ) {
		}
		
		super.onDestroy();
	}

	
	@Override
	public void onMove(int mv) {
		ChessApp app = (ChessApp) getApplicationContext();
		int pc = mPosition.pcList[ mPosition.moveNum - 1];
		int tmp = (pc << 16) + mv;
		mHistory.push(tmp);
		
		// get result
		mResult = getResult();
		if( mResult != RESULT_TBD ) {
			String result = "";
			switch( mResult ) {
			case RESULT_RED_WIN:
			case RESULT_BLACK_LONG_CHECK_LOSS:
				result = "红方胜";
				break;
			case RESULT_BLACK_WIN:
			case RESULT_RED_LONG_CHECK_LOSS:
				result = "黑方胜";
				break;
			case RESULT_DRAW:
				result = "和棋";
				break;
			}
			Toast toast = Toast.makeText(this, result, Toast.LENGTH_SHORT);
			toast.show();
			onResult( mResult );
			return;
		}
		
		// play sound effect
		if( mPosition.checked() ) {
			app.playSoundEffect(R.raw.check);
		} else if( mPosition.captured() ) {
			app.playSoundEffect(R.raw.capture);
		} else {
			app.playSoundEffect(R.raw.move);
		}
		
		if( mPosition.captured() ) {
			mPosition.setIrrev();
		}
		
		// if computer need response, 
		if( (mPosition.sdPlayer == 0 && mMoveMode == MOVE_MODE_COMPUTER_RED)
				|| (mPosition.sdPlayer == 1 && mMoveMode == MOVE_MODE_COMPUTER_BLACK) ) {
			mChessBoard.setClickable(false);
			computerMove();
		}
	}

	private String getResultString( int result ) {
		switch(result) {
		case RESULT_RED_WIN:
		case RESULT_BLACK_LONG_CHECK_LOSS:
			return "红方胜";
			
		case RESULT_BLACK_WIN:
		case RESULT_RED_LONG_CHECK_LOSS:
			return "黑方胜";
		
		case RESULT_DRAW:
			return "和棋";
			
		default:
			return null;
		}
	}
	
	private void onResult( int result ) {
		// 玩家可能在电脑思考的时候认输，所以需要cancel computer think
		cancelComputeThink();
		
		mChessBoard.setClickable(false);
		
		mViewResult.setText(getResultString(result));
		mViewResult.setVisibility(View.VISIBLE);
		
		// 修改底部button
		mButtonNewGame.setVisibility(View.VISIBLE);
		mButtonDraw.setVisibility(View.GONE);
		mButtonSurrender.setVisibility(View.GONE);
		
		// play sound
		ChessApp app = (ChessApp) getApplicationContext();
		if( (result & RESULT_DRAW) != 0 ) {
		    app.playSoundEffect(R.raw.draw);
		} else if( isComputerWin() ) {
			app.playSoundEffect(R.raw.loss);
		} else {
			app.playSoundEffect(R.raw.win);
		}
	}
	
	private int getResult() {
    	if( mPosition.isMate() ) {
    		return mPosition.sdPlayer == 0 ? RESULT_BLACK_WIN : RESULT_RED_WIN ;
    	}
    	
    	int vlRep = mPosition.repStatus(3);
    	if( vlRep > 0 ) {
    		vlRep = -mPosition.repValue(vlRep);
    		if( vlRep > Position.WIN_VALUE ) {
    			return mPosition.sdPlayer == 0 ? RESULT_RED_LONG_CHECK_LOSS : RESULT_BLACK_LONG_CHECK_LOSS;
    		} else {
    			return RESULT_DRAW;
    		}
    	}
    	
    	return RESULT_TBD;
    }
	
	private void regret() {
		// if computer make the last move, pop 2 moves
		// if user make the last move, pop 1 move
		if( (mPosition.sdPlayer == 0 && mMoveMode == MOVE_MODE_COMPUTER_BLACK) 
			|| (mPosition.sdPlayer == 1 && mMoveMode == MOVE_MODE_COMPUTER_RED) ) {
			// pop 2 moves
			if( mHistory.size() >= 2 ) {
				cancelComputeThink();
				regretOneStep();
				regretOneStep();
				afterRegret();
			}
		} else {
			if( !mHistory.empty() ) {
				cancelComputeThink();
				regretOneStep();	
				afterRegret();
			}
		}
	}
	
	private void regretOneStep() {
		int step = mHistory.pop();
		int mv = step & 0xffff;
		int src = Position.SRC(mv);
		int dst = Position.DST(mv);
		int pc = mPosition.squares[dst];
		mPosition.delPiece(dst, pc);
		mPosition.addPiece(src, pc);
		pc = step >> 16;
		if( pc != 0 ) {
			mPosition.addPiece(dst, pc);
		}
		mPosition.sdPlayer = 1 - mPosition.sdPlayer;
	}
	
	private void afterRegret() {
		mChessBoard.setClickable(true);
		if( mHistory.empty() ) {
			mChessBoard.setLastMove(0);
		} else {
			int lastMove = mHistory.peek();
			lastMove &= 0xffff;
			mChessBoard.setLastMove(lastMove);
		}
		mChessBoard.invalidate();
		if( mResult != RESULT_TBD ) {
			mResult = RESULT_TBD;
			mViewResult.setVisibility(View.GONE);
			mButtonNewGame.setVisibility(View.GONE);
			mButtonDraw.setVisibility(View.VISIBLE);
			mButtonSurrender.setVisibility(View.VISIBLE);
		}
	}
	
	private void prepareNewGame() {
		// 记录上一局的棋谱
		// for debug
//		SimpleDateFormat sDateFormat = new SimpleDateFormat("yyyyMMddHHmm", Locale.CHINA);
//		String filename = sDateFormat.format(new Date()) + ".mvs";
//		
//		File file = new File( getExternalFilesDir(null), filename );
//		Log.d(TAG, "record last game's movements to file: " + file.getAbsolutePath());
//		FileOutputStream fos;
//		try {
//			fos = new FileOutputStream(file);
//			ObjectOutputStream oos = new ObjectOutputStream( fos );
//			oos.writeObject(Integer.valueOf(mLevel));
//			oos.writeObject(Integer.valueOf(mMoveMode));
//			oos.writeObject(mStartupFen);
//			oos.writeObject(mHistory);
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}

		// 记录上一局的结果
		recordResult();

		// 根据上一局的结果调整设置
		if( isComputerWin() ) {
			if( mLevel > 0 ) {
				mLevel -= 1;
			}
		} else if( mResult != RESULT_DRAW ) {
			if( mLevel < 7 ) {
				mLevel += 1;
			}
		}
		
		mHistory.clear();
    	mStartupFen = Position.STARTUP_FEN[0];
		mPosition.fromFen(mStartupFen);
		if( mMoveMode == MOVE_MODE_COMPUTER_RED ) {
			mMoveMode = MOVE_MODE_COMPUTER_BLACK;
		} else {
			mMoveMode = MOVE_MODE_COMPUTER_RED;
		}
	}
	
	private void restart() {

		prepareNewGame();
 
		mChessBoard.load( mPosition, mMoveMode );
		mChessBoard.setLastMove(0);
		mChessBoard.invalidate();
		
		cancelComputeThink();
		
		if( (mPosition.sdPlayer == 0 && mMoveMode == MOVE_MODE_COMPUTER_RED)
				|| (mPosition.sdPlayer == 1 && mMoveMode == MOVE_MODE_COMPUTER_BLACK) ) {
			mChessBoard.setClickable(false);
			computerMove();
		} else {
			mChessBoard.setClickable(true);
		}

		mResult = RESULT_TBD;
	}

	@Override
	public void onSelected() {
		ChessApp app = (ChessApp) getApplicationContext();
		app.playSoundEffect(R.raw.click);
	}
	
	@Override
	public void onIllegalMove() {
		ChessApp app = (ChessApp) getApplicationContext();
		app.playSoundEffect(R.raw.illegal);
	}

	@Override
	public void onThinkResult(String result) {	
		if( mIsRequestDraw ) {
			// It's the request draw result
			mIsRequestDraw = false;
			if( result.contains("draw") ) {
				Toast toast = Toast.makeText(this, "电脑接受和棋", Toast.LENGTH_SHORT);
				toast.show();
				mResult = RESULT_DRAW;
				onResult( mResult );
			} else {
				onDrawRefused( );	
			}
			return;
		}
		
		if( result.startsWith("nobestmove") ) {
			surrender(true);
			return;
		}
		
		if( result.startsWith("bestmove") ) { 
			// Compute made a move
			String coord = result.substring(9, 13);
			int mv = CcUtil.coord2Move(coord);
			//mChessBoard.onComputerMove(result);
			int src = Position.SRC(mv);
			int dst = Position.DST(mv);
			mPosition.makeMove(mv);
			mChessBoard.setClickable(true);
			mChessBoard.setLastMove(mv);
			mChessBoard.startAnimate(src, mPosition.squares[dst], dst, (byte)mPosition.pcList[mPosition.moveNum-1]);
			mChessBoard.invalidate();
			onMove( mv );
		} else {
			mChessBoard.setClickable(true);
		}
	}
	
	private void computerMove() {
		computerMove( false, true );
	}
	
	private void computerMove(boolean isDraw, boolean useLastMove) {
//		ChessApp app = (ChessApp) getApplicationContext();
//		if( mEngine == null ) {
//			mEngine = app.getEngine();
//			if( mEngine == null ) {
//				Log.e(TAG, "Failed to get Engine!");
//			}
//		}
		
		mChessBoard.setClickable(false);
		
		StringBuilder position = new StringBuilder();
		position.append( "startpos");
		if( !mHistory.empty() ) {
			position.append(" moves");
			int moves = mHistory.size();
			if( !useLastMove ) {
				moves -= 1;
			}
			int mv;
			for( int i=0; i<moves; i++ ) {
				mv = mHistory.get(i);
				position.append(" ");
				position.append( CcUtil.move2Coord( mv & 0xffff));
			}
		}
		
		// int time = isDraw ? 1 : (1 << mLevel);
		int time = isDraw ? 1 : mLevel;
		
		SearchRequest request = new SearchRequest( position.toString(), time, isDraw, this);
		if( mEleeyeBinded ) {
			if( mEleeyeService.getEngine() != null ) {
				mEleeyeService.getEngine().requestSearch(request);
			}
		} else {
			// will send this request when eleeye service is binded.
			mSearchRequest = request;
		}
	}
	
	private void requestDraw() {
		if( mIsRequestDraw ) {
			return;
		}
		
		if( mHistory.size() < 10 ) {
			onDrawRefused();
			return;
		}
		
		cancelComputeThink();
		// If it's computer turn to make next move.
		// We should include the last move to request draw
		// NOTE: Please remember to let computer make next move if it refuse draw
		boolean useLastMove = isComputerTurn();
		computerMove( true, useLastMove );
		mIsRequestDraw = true;
	}
	
	private void onDrawRefused( ) {
		Toast toast = Toast.makeText(this, "电脑不接受和棋", Toast.LENGTH_SHORT);
		toast.show();
		if( isComputerTurn() ) {
			computerMove();
		} else {
			mChessBoard.setClickable(true);
		}
	}
	
	private void cancelComputeThink() {
		if( mEngine != null ) {
			mEngine.cancelSearch();
		}
		
		mIsRequestDraw = false;
		mChessBoard.setClickable(true);
	}
	
	private boolean isComputerTurn() {
		return (mPosition.sdPlayer == 0 && mMoveMode == MOVE_MODE_COMPUTER_RED)
				|| (mPosition.sdPlayer == 1 && mMoveMode == MOVE_MODE_COMPUTER_BLACK);
	}	
	
	private void surrender( boolean isComputer ) {
		String info = isComputer ? "电脑认输" : "玩家认输";
		Toast toast = Toast.makeText(this, info, Toast.LENGTH_SHORT);
		toast.show();
		
		if( isComputer ) {
			mResult = mMoveMode == MOVE_MODE_COMPUTER_BLACK ? RESULT_RED_WIN : RESULT_BLACK_WIN;
		} else {
			mResult = mMoveMode == MOVE_MODE_COMPUTER_BLACK ? RESULT_BLACK_WIN : RESULT_RED_WIN;
		}
		onResult( mResult );
	}
	
	private boolean isComputerWin() {
		if( mMoveMode == MOVE_MODE_COMPUTER_RED ) {
			return (mResult & BIT_RED_WIN) != 0;
		} else if( mMoveMode == MOVE_MODE_COMPUTER_BLACK ) {
			return (mResult & BIT_BLACK_WIN ) != 0;
		}
		
		return false;
	}
	
	private void recordResult() {
		if( mResult == RESULT_TBD ) {
			return;
		}
		
		int result = mResult;
		SharedPreferences sp = getPreferences( MODE_PRIVATE );
		SharedPreferences.Editor editor = sp.edit();
		if( mMoveMode == MOVE_MODE_COMPUTER_BLACK ) {
			if( (result & BIT_RED_WIN) != 0 ) {
				mScoreRedWin++;
				editor.putInt("SCORE_RED_WIN", mScoreRedWin);
			} else if ( (result & BIT_DRAW) != 0) {
				mScoreRedDraw++;
				editor.putInt("SCORE_RED_DRAW", mScoreRedDraw);
			} else {
				mScoreRedLoss++;
				editor.putInt("SCORE_RED_LOSS", mScoreRedLoss);
			}
		} else {
			if( (result & BIT_BLACK_WIN) != 0 ) {
				mScoreBlackWin++;
				editor.putInt("SCORE_BLACK_WIN", mScoreBlackWin);
			} else if( (result & BIT_DRAW) != 0) {
				mScoreBlackDraw++;
				editor.putInt("SCORE_BLACK_DRAW", mScoreBlackDraw);
			} else {
				mScoreBlackLoss++;
				editor.putInt("SCORE_BLACK_LOSS", mScoreBlackLoss);
			}
		}
		editor.commit();
	}
}

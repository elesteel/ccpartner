package net.csdn.e28sean.ccpartner;


import net.csdn.e28sean.ccpartner.R;
import xqwlight.Position;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;


public class ChessBoard extends View {
	public interface EventListener {
		public void onSelected();
		public void onIllegalMove();
		public void onMove(int mv);
	}

	
	private static final float SPAN = 105.0f;
	private static final float DESIRE_WIDTH = 921.0f + 20.0f;
	private static final float DESIRE_HEIGHT = 1024.0f + 20.0f;
	private static final float XSTART = 40.0f + 10;
	private static final float YSTART = 40.0f + 10;
	private static final float CHESS_R = 49.0f;
	
	private static final long ANIMATION_DURATION = 200;

	private static final Bitmap _dst = Bitmap.createBitmap((int)DESIRE_WIDTH, (int)DESIRE_HEIGHT, Bitmap.Config.ARGB_8888);
	private final Canvas _cvs = new Canvas(_dst);
	private float _xstart = 0.0f;
	private float _ystart = 0.0f;
	private float _span = 105.0f;
	
	private static Bitmap _board;
	private static Bitmap _pcBm[][];
	private Paint _paint;
	
	private float _zoom = 1.0f;
	private Matrix _matrix = new Matrix();
	
	private Position _position = null;

	private long mAnimStartTime = 0;
	private int mAnimSrc = 0;
	private int mAnimDst = 0;
	private byte mAnimSrcPc = 0;
	private byte mAnimDstPc = 0;
	
	public static final int COMPUTER_BLACK = 0;
	public static final int COMPUTER_RED = 1;
	public static final int COMPUTER_NONE = 2;
	
	
	private int mMoveMode = COMPUTER_NONE;
	private int mSqSelected = 0;
	private int mLastMove = 0;
	
	private EventListener mListener = null;
	private boolean mClickable = false;
	
	static {
		_board = BitmapFactory.decodeResource(ChessApp.res(), R.drawable.board);
		_pcBm = new Bitmap[2][7];
		
		_pcBm[0][Position.PIECE_KING] = BitmapFactory.decodeResource(ChessApp.res(), R.drawable.rk);
		_pcBm[0][Position.PIECE_ADVISOR] = BitmapFactory.decodeResource(ChessApp.res(), R.drawable.ra);
		_pcBm[0][Position.PIECE_BISHOP] = BitmapFactory.decodeResource(ChessApp.res(), R.drawable.rb);
		_pcBm[0][Position.PIECE_KNIGHT] = BitmapFactory.decodeResource(ChessApp.res(), R.drawable.rn);
		_pcBm[0][Position.PIECE_ROOK] = BitmapFactory.decodeResource(ChessApp.res(), R.drawable.rr);
		_pcBm[0][Position.PIECE_CANNON] = BitmapFactory.decodeResource(ChessApp.res(), R.drawable.rc);
		_pcBm[0][Position.PIECE_PAWN] = BitmapFactory.decodeResource(ChessApp.res(), R.drawable.rp);
		
		_pcBm[1][Position.PIECE_KING] = BitmapFactory.decodeResource(ChessApp.res(), R.drawable.bk);
		_pcBm[1][Position.PIECE_ADVISOR] = BitmapFactory.decodeResource(ChessApp.res(), R.drawable.ba);
		_pcBm[1][Position.PIECE_BISHOP] = BitmapFactory.decodeResource(ChessApp.res(), R.drawable.bb);
		_pcBm[1][Position.PIECE_KNIGHT] = BitmapFactory.decodeResource(ChessApp.res(), R.drawable.bn);
		_pcBm[1][Position.PIECE_ROOK] = BitmapFactory.decodeResource(ChessApp.res(), R.drawable.br);
		_pcBm[1][Position.PIECE_CANNON] = BitmapFactory.decodeResource(ChessApp.res(), R.drawable.bc);
		_pcBm[1][Position.PIECE_PAWN] = BitmapFactory.decodeResource(ChessApp.res(), R.drawable.bp);
		
	}
	
	public ChessBoard(Context context) {
		super(context);
		initChessBoard();
	}



	public ChessBoard(Context context, AttributeSet attrs) {
		super(context, attrs);
		initChessBoard();
	}

	public void setOnMoveListener( EventListener l ) {
		mListener = l;
	}
	
	public void setClickable(boolean clickable) {
		mClickable = clickable;
	}
	
	public void setLastMove(int mv) {
		mLastMove = mv;
	}
	
	private void initChessBoard() {
		_paint = new Paint();
		_paint.setAntiAlias(true);
		_paint.setColor(Color.BLUE);
		_paint.setStrokeWidth(3);
		_paint.setStyle(Style.STROKE);
		_matrix = new Matrix();
		
	}
	
	public void load(Position pos, int mode) {
		_position = pos;
		mMoveMode = mode;
		if( (_position.sdPlayer == 0 && mMoveMode == COMPUTER_RED) ||
				( _position.sdPlayer == 1 && mMoveMode == COMPUTER_BLACK )) {
			mClickable = false;
		} else {
			mClickable = true;
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		
		int w_result = 0;
		int h_result = 0;
		int w_mode = MeasureSpec.getMode(widthMeasureSpec);
		int w_size = MeasureSpec.getSize(widthMeasureSpec);
		int h_mode = MeasureSpec.getMode(heightMeasureSpec);
		int h_size = MeasureSpec.getSize(heightMeasureSpec);

		
		if( w_mode == MeasureSpec.EXACTLY ) {
			w_result = w_size;
			if( h_mode == MeasureSpec.EXACTLY ) {
				h_result = h_size;
			} else {
				h_result = (int) (DESIRE_HEIGHT / DESIRE_WIDTH * w_result);
				if( h_mode == MeasureSpec.AT_MOST ) {
					h_result = Math.min(h_result, h_size);
				}
			}
		} else if ( h_mode == MeasureSpec.EXACTLY ) {
			h_result = h_size;
			w_result = (int) ( DESIRE_WIDTH / DESIRE_HEIGHT * h_result);
			if( w_mode == MeasureSpec.AT_MOST ) {
				w_result = Math.min( w_result, w_size );
			}
		} else {
			w_result = (w_mode == MeasureSpec.AT_MOST ? w_size : (int)DESIRE_WIDTH);
			h_result = (h_mode == MeasureSpec.AT_MOST ? h_size : (int)DESIRE_HEIGHT);
			float k = Math.min((float) w_result / DESIRE_WIDTH, (float)h_result / DESIRE_HEIGHT);
			w_result = (int)(k * DESIRE_WIDTH);
			h_result = (int)(k * DESIRE_HEIGHT);
		}
		setMeasuredDimension( w_result, h_result);
	}
	
	
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		float zoomx = w / DESIRE_WIDTH;
		float zoomy = h / DESIRE_HEIGHT;
		_zoom = Math.min(zoomx, zoomy);
		_xstart = (w - _zoom * DESIRE_WIDTH)/2;
		_ystart = (h - _zoom * DESIRE_HEIGHT)/2;
		_span = _zoom * SPAN;
		
		_matrix.reset();
		_matrix.postScale(_zoom, _zoom);
		_matrix.postTranslate(_xstart, _ystart);
		
		super.onSizeChanged(w, h, oldw, oldh);
	}

	
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if(event.getAction() == MotionEvent.ACTION_DOWN ) {
		    int x = (int) ((event.getX() - _xstart) / _span);
		    int y = (int) (( event.getY() - _ystart) / _span);
		    onClickSquare( x, y);
		}
		return super.onTouchEvent(event);
	}

    private void onClickSquare(int x, int y) {
    	if( !mClickable ) {
    		return;
    	}
    	
    	int sq = Position.COORD_XY( x + Position.FILE_LEFT, y + Position.RANK_TOP);
    	// Need flip if computer take red. Because computer take red means user take black.
    	// In "Position" the black is always at the top side of the board. But in the game,
    	// User should always in the bottom side of the board. So we need flip the square!
    	if( mMoveMode == COMPUTER_RED ) {
    		sq = Position.SQUARE_FLIP(sq);
    	}
    	
    	int pc = _position.squares[sq];
    	
    	
    	if ((pc & Position.SIDE_TAG(_position.sdPlayer)) != 0) {
    		// Select a piece
			mSqSelected = sq;
			if( mListener != null ) {
				mListener.onSelected();
			}
			invalidate();
		} else {
			
			if( mSqSelected > 0 && addMove(Position.MOVE(mSqSelected, sq)) ) {
				// User make a move success
				mLastMove = Position.MOVE(mSqSelected, sq);
				startAnimate(mSqSelected, _position.squares[sq], sq, (byte) _position.pcList[_position.moveNum - 1]);
				invalidate();
				mSqSelected = 0;
				if( mListener != null ) {
					mListener.onMove(mLastMove);
				}
			}
		}
    }
    
    public void startAnimate(int src, byte src_pc, int dst, byte dst_pc) {
    	mAnimStartTime = System.currentTimeMillis();
    	mAnimSrc = src;
    	mAnimSrcPc = src_pc;
    	mAnimDst = dst;
    	mAnimDstPc = dst_pc;
    	invalidate();
    }
    
    
    private boolean addMove(int mv) {
    	if( ! _position.legalMove(mv) ) {
    		return false;
    	}
    	
    	if( !_position.makeMove(mv) ) {
    		if( mListener != null ) {
				mListener.onIllegalMove();
				return false;
			}
    	}
    	
    	return true;
    }
    

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		_cvs.drawColor(Color.WHITE);
		_cvs.drawBitmap(_board, 10, 10, null);
		byte pc;
		for( int sq = 0; sq < 256; sq ++ ) {
			if( Position.IN_BOARD(sq) && sq != mAnimSrc && sq != mAnimDst ) {
				pc = _position.squares[sq];
				if( pc > 0 ) {
					drawSquare(_cvs, pc, sq);
				}
			}
		}
		
		drawLastMove( _cvs );
		drawAnimation( _cvs );
		
		canvas.drawBitmap(_dst, _matrix, null);
	}
	

	
	private void drawAnimation( Canvas canvas ) {
		if( mAnimStartTime == 0 )
			return;
		
		long now = System.currentTimeMillis();
		if( now - mAnimStartTime > ANIMATION_DURATION ) {
			// draw last frame 
			drawSquare( canvas, mAnimSrcPc, mAnimDst);
			// stop the animation
			mAnimStartTime = 0;
			mAnimSrc = 0;
			mAnimSrcPc = 0;
			mAnimDst = 0;
			mAnimDstPc = 0;
		} else {
			if( mAnimDstPc != 0 ) {
				drawSquare( canvas, mAnimDstPc, mAnimDst);
			}

			int sq_src = mAnimSrc;
			int sq_dst = mAnimDst;
			if( mMoveMode == COMPUTER_RED ) {
				sq_src = Position.SQUARE_FLIP(sq_src);
				sq_dst = Position.SQUARE_FLIP(sq_dst);
			}
			
			float xSrc = XSTART + (Position.FILE_X(sq_src) - Position.FILE_LEFT) * SPAN;
			float ySrc = YSTART + (Position.RANK_Y(sq_src) - Position.RANK_TOP) * SPAN;
			float xDst = XSTART + (Position.FILE_X(sq_dst) - Position.FILE_LEFT) * SPAN;
			float yDst = YSTART + (Position.RANK_Y(sq_dst) - Position.RANK_TOP) * SPAN;
			float k = (now - mAnimStartTime) * 1.0f / ANIMATION_DURATION;
			float left = ( xDst - xSrc ) * k + xSrc - CHESS_R;
			float top = ( yDst - ySrc ) * k + ySrc - CHESS_R;
			Bitmap bm = getPieceBitmap(mAnimSrcPc);
			canvas.drawBitmap(bm, left, top, null);
			
			invalidate();
		}
		
	}
	
	private void drawLastMove( Canvas canvas ) {
		if( mLastMove == 0 )
			return;
		
		// draw src
		int sq = Position.SRC(mLastMove);
		if( mMoveMode == COMPUTER_RED )
			sq = Position.SQUARE_FLIP(sq);
		
		float left; 
		float top;
		float w = CHESS_R/3;
		
		left = XSTART - CHESS_R + ( Position.FILE_X(sq) - Position.FILE_LEFT ) * SPAN;
		top = YSTART - CHESS_R + ( Position.RANK_Y(sq) - Position.RANK_TOP ) * SPAN;
		
		canvas.drawLine(left, top, left + w, top, _paint);
		canvas.drawLine(left, top, left, top + w, _paint);
		canvas.drawLine(left, top + SPAN, left, top + SPAN - w, _paint);
		canvas.drawLine(left, top + SPAN, left + w, top + SPAN, _paint);
		canvas.drawLine(left + SPAN, top, left + SPAN, top + w, _paint);
		canvas.drawLine(left + SPAN, top, left + SPAN - w, top, _paint);
		canvas.drawLine(left + SPAN, top + SPAN, left + SPAN, top + SPAN - w, _paint);
		canvas.drawLine(left + SPAN, top + SPAN, left + SPAN - w, top + SPAN, _paint);

		// draw dst
		sq = Position.DST(mLastMove);
		if( mMoveMode == COMPUTER_RED )
			sq = Position.SQUARE_FLIP(sq);
		
		left = XSTART - CHESS_R + ( Position.FILE_X(sq) - Position.FILE_LEFT ) * SPAN;
		top = YSTART - CHESS_R + ( Position.RANK_Y(sq) - Position.RANK_TOP ) * SPAN;
		canvas.drawLine(left, top, left + w, top, _paint);
		canvas.drawLine(left, top, left, top + w, _paint);
		canvas.drawLine(left, top + SPAN, left, top + SPAN - w, _paint);
		canvas.drawLine(left, top + SPAN, left + w, top + SPAN, _paint);
		canvas.drawLine(left + SPAN, top, left + SPAN, top + w, _paint);
		canvas.drawLine(left + SPAN, top, left + SPAN - w, top, _paint);
		canvas.drawLine(left + SPAN, top + SPAN, left + SPAN, top + SPAN - w, _paint);
		canvas.drawLine(left + SPAN, top + SPAN, left + SPAN - w, top + SPAN, _paint);

		// draw selected
		sq = mSqSelected;
		if( mMoveMode == COMPUTER_RED )
			sq = Position.SQUARE_FLIP(sq);
		
		left = XSTART - CHESS_R + ( Position.FILE_X(sq) - Position.FILE_LEFT ) * SPAN;
		top = YSTART - CHESS_R + ( Position.RANK_Y(sq) - Position.RANK_TOP ) * SPAN;
		canvas.drawLine(left, top, left + w, top, _paint);
		canvas.drawLine(left, top, left, top + w, _paint);
		canvas.drawLine(left, top + SPAN, left, top + SPAN - w, _paint);
		canvas.drawLine(left, top + SPAN, left + w, top + SPAN, _paint);
		canvas.drawLine(left + SPAN, top, left + SPAN, top + w, _paint);
		canvas.drawLine(left + SPAN, top, left + SPAN - w, top, _paint);
		canvas.drawLine(left + SPAN, top + SPAN, left + SPAN, top + SPAN - w, _paint);
		canvas.drawLine(left + SPAN, top + SPAN, left + SPAN - w, top + SPAN, _paint);
	}
	
	private void drawSquare(Canvas canvas, byte pc, int sq) {
	    Bitmap bm = getPieceBitmap(pc);
	    int sqFlipped = ((mMoveMode == COMPUTER_RED) ? Position.SQUARE_FLIP(sq) : sq);
	    float left = XSTART - CHESS_R + ( Position.FILE_X(sqFlipped) - Position.FILE_LEFT ) * SPAN;
	    float top = YSTART - CHESS_R + ( Position.RANK_Y(sqFlipped) - Position.RANK_TOP ) * SPAN;
	    canvas.drawBitmap(bm, left, top, null);
	}
	
	private Bitmap getPieceBitmap(byte pc) {
		return _pcBm[(pc >> 3) -1][pc & 7];
	}

}

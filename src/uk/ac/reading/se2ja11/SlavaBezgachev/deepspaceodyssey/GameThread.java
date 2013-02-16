package uk.ac.reading.se2ja11.SlavaBezgachev.deepspaceodyssey;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.FloatMath;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;

public class GameThread extends Thread {

	// Different mMode states
	public static final int STATE_LOSE = 1;
	public static final int STATE_PAUSE = 2;
	public static final int STATE_READY = 3;
	public static final int STATE_RUNNING = 4;
	public static final int STATE_WIN = 5;

	// Control variable for the mode of the game (e.g. STATE_WIN)
	private int mMode = 1;

	// Control of the actual running inside run()
	private boolean mRun = false;

	// The surface this thread (and only this thread) writes upon
	private SurfaceHolder mSurfaceHolder;

	// the message handler to the View/Activity thread
	private Handler mHandler;

	// Android Context - this stores almost all we need to know
	private Context mContext;

	// The view
	public GameView mGameView;

	// We might want to extend this call - therefore protected
	protected int mCanvasWidth = 1;
	protected int mCanvasHeight = 1;

	// Last time we updated the game physics
	protected long mLastTime = 0;

	protected Bitmap mBackgroundImage;

	private float score = 0;

	// All *WE* need for sprite, but in a real game.......
	private Bitmap mBall;
	private float mBallX = 0;
	private float mBallY = 0;
	private float mBallDX = 0;
	private float mBallDY = 0;
	private float distanceTravelled = 0;

	public GameThread(GameView gameView) {
		mGameView = gameView;

		mSurfaceHolder = gameView.getHolder();
		mHandler = gameView.getmHandler();
		mContext = gameView.getContext();

		mBackgroundImage = BitmapFactory.decodeResource(gameView.getContext()
				.getResources(), R.drawable.background_space);
		mBall = BitmapFactory.decodeResource(gameView.getContext()
				.getResources(), R.drawable.spaceship_icon_game);
	}

	public GameThread(GameView gameView, GameThread oldThread) {
		mGameView = gameView;

		mSurfaceHolder = gameView.getHolder();
		mHandler = gameView.getmHandler();
		mContext = gameView.getContext();

		// Transfer the old values
		mMode = oldThread.mMode;
		mRun = oldThread.mRun;
		mCanvasWidth = oldThread.mCanvasWidth;
		mCanvasHeight = oldThread.mCanvasHeight;
		mLastTime = oldThread.mLastTime;
		mBackgroundImage = oldThread.mBackgroundImage;
		score = oldThread.score;

		mBall = oldThread.mBall;
		mBallX = oldThread.mBallX;
		mBallY = oldThread.mBallY;
		mBallDX = oldThread.mBallDX;
		mBallDY = oldThread.mBallDY;
	}

	/*
	 * Called when app is destroyed, so not really that important here But if
	 * (later) the game involves more thread, we might need to stop a thread,
	 * and then we would need this Dare I say memory leak...
	 */
	public void cleanup() {
		this.mContext = null;
		this.mGameView = null;
		this.mHandler = null;
		this.mSurfaceHolder = null;
	}

	// Pre-begin a game
	public void setupBeginning() {
		mBallDX = 0;
		mBallDY = 0;

		mBallX = mCanvasWidth / 2;
		mBallY = mCanvasHeight / 2;
	}

	// Starting up the game
	public void doStart() {
		synchronized (mSurfaceHolder) {

			setupBeginning();

			mLastTime = System.currentTimeMillis() + 100;

			setState(STATE_RUNNING);

			setScore(0);
		}
	}

	/*
	 * Restore/save state (do not confuse with mMode states) of a game, i.e. all
	 * needed variable. Doing this and we can pause the game and recieve calls.
	 */
	public synchronized void restoreState(Bundle savedState) {
		synchronized (mSurfaceHolder) {
			setState(STATE_PAUSE);

			setScore(savedState.getLong("score"));

			mBallX = savedState.getFloat("mIconX");
			mBallY = savedState.getFloat("mIconY");
			mBallDX = savedState.getFloat("mIconDX");
			mBallDY = savedState.getFloat("mIconDY");

			Integer gameMode = savedState.getInt("gameMode");

			if (gameMode == GameThread.STATE_LOSE
					| gameMode == GameThread.STATE_WIN) {
				setState(gameMode);
			}
		}
	}

	public Bundle saveState(Bundle map) {
		synchronized (mSurfaceHolder) {
			if (map != null) {
				map.putFloat("score", score);
				map.putInt("gameMode", mMode);

				map.putFloat("mIconX", mBallX);
				map.putFloat("mIconY", mBallY);
				map.putFloat("mIconDX", mBallDX);
				map.putFloat("mIconDY", mBallDY);
			}
		}
		return map;
	}

	// The thread start
	@Override
	public void run() {
		Canvas canvasRun;
		while (mRun) {
			canvasRun = null;
			try {
				canvasRun = mSurfaceHolder.lockCanvas(null);
				synchronized (mSurfaceHolder) {
					if (mMode == STATE_RUNNING) {
						updatePhysics();
					}
					doDraw(canvasRun);
				}
			} finally {
				if (canvasRun != null) {
					if (mSurfaceHolder != null)
						mSurfaceHolder.unlockCanvasAndPost(canvasRun);
				}
			}
		}
	}

	/*
	 * Surfaces and drawing
	 */
	public void setSurfaceSize(int width, int height) {
		synchronized (mSurfaceHolder) {
			mCanvasWidth = width;
			mCanvasHeight = height;

			// don't forget to resize the background image
			mBackgroundImage = Bitmap.createScaledBitmap(mBackgroundImage,
					width, height, true);
		}
	}

	protected void doDraw(Canvas canvas) {

		if (canvas == null)
			return;

		if (mBackgroundImage != null)
			canvas.drawBitmap(mBackgroundImage, 0, 0, null);

		canvas.drawBitmap(mBall, mBallX, mBallY, null);
	}

	protected void updatePhysics() {
		long now = System.currentTimeMillis();

		float elapsed = (now - mLastTime) / 1000.0f;

		mBallX += elapsed * mBallDX;
		mBallY += elapsed * mBallDY;

		distanceTravelled = this.distanceTravelled
				+ ((FloatMath.sqrt((mBallDX * mBallDX)
						+ (mBallDY * mBallDY))));

		if (distanceTravelled > 10000) {
			distanceTravelled = distanceTravelled / 100000;
		}
		this.setScore(score + distanceTravelled);

		// if((mBallX <= 0 & mBallDX < 0) | (mBallX >= mCanvasWidth -
		// mBall.getWidth() & mBallDX > 0) ) {
		// mBallDX = -mBallDX;
		// updateScore(1);
		// }

		// if (mBallY <= 0)
		// setState(GameThread.STATE_LOSE);
		// if (mBallY >= mCanvasHeight - mBall.getHeight())
		// setState(GameThread.STATE_WIN);

		if (mBallY <= 0) {
			setState(GameThread.STATE_LOSE);
		} else if (mBallX <= 0) {
			setState(GameThread.STATE_LOSE);
		} else if (mBallX >= mCanvasWidth - mBall.getWidth()) {
			setState(GameThread.STATE_LOSE);
		} else if (mBallY >= mCanvasHeight - mBall.getHeight()) {
			setState(GameThread.STATE_LOSE);
		}

		mLastTime = now;
	}

	/*
	 * Control functions
	 */

	// Finger touches the screen
	public boolean onTouch(MotionEvent e) {
		if (e.getAction() != MotionEvent.ACTION_DOWN)
			return false;

		if (mMode == STATE_READY || mMode == STATE_LOSE || mMode == STATE_WIN) {
			doStart();
			return true;
		}

		if (mMode == STATE_PAUSE) {
			unpause();
			return true;
		}

		synchronized (mSurfaceHolder) {
			this.actionOnTouch(e);
		}

		return false;
	}

	private void actionOnTouch(MotionEvent e) {
		// TODO do something
	}

	// The Accellerometer has changed
	public void onSensorChanged(SensorEvent event) {
		synchronized (mSurfaceHolder) {
			if (event.sensor.getType() == Sensor.TYPE_ORIENTATION) {
				mBallDX -= 1.5f * event.values[2];
				mBallDY -= 1.5f * event.values[1];
			}
		}
	}

	/*
	 * Game states
	 */
	public void pause() {
		synchronized (mSurfaceHolder) {
			if (mMode == STATE_RUNNING)
				setState(STATE_PAUSE);
		}
	}

	public void unpause() {
		// Move the real time clock up to now
		synchronized (mSurfaceHolder) {
			mLastTime = System.currentTimeMillis();
		}
		setState(STATE_RUNNING);
	}

	// Send messages to View/Activity thread
	public void setState(int mode) {
		synchronized (mSurfaceHolder) {
			setState(mode, null);
		}
	}

	public void setState(int mode, CharSequence message) {
		synchronized (mSurfaceHolder) {
			mMode = mode;

			if (mMode == STATE_RUNNING) {
				Message msg = mHandler.obtainMessage();
				Bundle b = new Bundle();
				b.putString("text", "");
				b.putInt("viz", View.INVISIBLE);
				b.putBoolean("showAd", false);
				msg.setData(b);
				mHandler.sendMessage(msg);
			} else {
				Message msg = mHandler.obtainMessage();
				Bundle b = new Bundle();

				Resources res = mContext.getResources();
				CharSequence str = "";
				if (mMode == STATE_READY)
					str = res.getText(R.string.mode_ready);
				else if (mMode == STATE_PAUSE)
					str = res.getText(R.string.mode_pause);
				else if (mMode == STATE_LOSE)
					str = res.getText(R.string.mode_lose);
				else if (mMode == STATE_WIN) {
					str = res.getText(R.string.mode_win);
				}

				if (message != null) {
					str = message + "\n" + str;
				}

				b.putString("text", str.toString());
				b.putInt("viz", View.VISIBLE);

				msg.setData(b);
				mHandler.sendMessage(msg);
			}
		}
	}

	/*
	 * Getter and setter
	 */
	public void setSurfaceHolder(SurfaceHolder h) {
		mSurfaceHolder = h;
	}

	public boolean isRunning() {
		return mRun;
	}

	public void setRunning(boolean running) {
		mRun = running;
	}

	public int getMode() {
		return mMode;
	}

	public void setMode(int mMode) {
		this.mMode = mMode;
	}

	/* ALL ABOUT SCORES */

	// Send a score to the View to view
	// Would it be better to do this inside this thread writing it manually on
	// the screen?
	public void setScore(float score) {

		this.score = score;

		synchronized (mSurfaceHolder) {
			Message msg = mHandler.obtainMessage();
			Bundle b = new Bundle();
			b.putBoolean("score", true);
			b.putString("text", getScoreString().toString());
			msg.setData(b);
			mHandler.sendMessage(msg);
		}
	}

	public float getScore() {
		return score;
	}

	public void updateScore(long score) {
		this.setScore(this.score + score);
	}

	protected CharSequence getScoreString() {
		return Long.toString(Math.round(this.score));
	}

}
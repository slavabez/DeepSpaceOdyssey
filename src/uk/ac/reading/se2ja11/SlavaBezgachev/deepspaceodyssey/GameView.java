package uk.ac.reading.se2ja11.SlavaBezgachev.deepspaceodyssey;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;

public class GameView extends SurfaceView implements SurfaceHolder.Callback {

	private volatile GameThread thread;

	private SensorEventListener sensorAccelerometer;

	//Handle communication from the GameThread to the View/Activity Thread
	private Handler mHandler;
	
	//Pointers to the views
	private TextView mScoreView;
	private TextView mStatusView;


	public GameView(Context context, AttributeSet attrs) {
		super(context, attrs);

		//Get the holder of the screen and register interest
		SurfaceHolder holder = getHolder();
		holder.addCallback(this);
		
		//Set up a handler for messages from GameThread
		mHandler = new Handler() {
			@Override
			public void handleMessage(Message m) {
				if(m.getData().getBoolean("score")) {
					mScoreView.setText(m.getData().getString("text"));
				}
				else {		
					//So it is a status
					mStatusView.setVisibility(m.getData().getInt("viz"));
					mStatusView.setText(m.getData().getString("text"));
				}
 			}
		};
	}
	
	//Used to release any resources.
	public void cleanup() {
		this.thread.setRunning(false);
		this.thread.cleanup();
		
		this.removeCallbacks(thread);
		thread = null;
		
		this.setOnTouchListener(null);
		sensorAccelerometer = null;
		
		SurfaceHolder holder = getHolder();
		holder.removeCallback(this);
	}
	
	/*
	 * Setters and Getters
	 */

	public void setThread(GameThread newThread) {

		thread = newThread;

		setOnTouchListener(new View.OnTouchListener() {

			public boolean onTouch(View v, MotionEvent event) {
				if(thread!=null) {
					return thread.onTouch(event);
				}
				else return false;
			}

		});

		this.sensorAccelerometer = new SensorEventListener() {

			public void onAccuracyChanged(Sensor arg0, int arg1) {
				// not needed
			}

			public void onSensorChanged(SensorEvent event) {
				if(thread!=null) {
					if (thread.isAlive()) {
						thread.onSensorChanged(event);
					}
				}
			}
		};

		setClickable(true);
		setFocusable(true);
	}
	
	public GameThread getThread() {
		return thread;
	}

	public TextView getStatusView() {
		return mStatusView;
	}

	public void setStatusView(TextView mStatusView) {
		this.mStatusView = mStatusView;
	}
	
	public TextView getScoreView() {
		return mScoreView;
	}

	public void setScoreView(TextView mScoreView) {
		this.mScoreView = mScoreView;
	}
	

	public Handler getmHandler() {
		return mHandler;
	}

	public void setmHandler(Handler mHandler) {
		this.mHandler = mHandler;
	}
	
	
	/*
	 * Screen functions
	 */
	
	//ensure that we go into pause state if we go out of focus
	@Override
	public void onWindowFocusChanged(boolean hasWindowFocus) {
		if(thread!=null) {
			if (!hasWindowFocus)
				thread.pause();
		}
	}

	public void surfaceCreated(SurfaceHolder holder) {
		if(thread!=null) {
			thread.setRunning(true);
			
			if(thread.getState() == Thread.State.NEW){
				//Just start the new thread
				thread.start();
			}
			else {
				if(thread.getState() == Thread.State.TERMINATED){
					//Set up and start a new thread with the old thread as seed
					thread = new GameThread(this, thread);
					thread.setRunning(true);
					thread.start();
				}
			}
		}
	}
	
	//Always called once after surfaceCreated. Tell the GameThread the actual size
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		if(thread!=null) {
			thread.setSurfaceSize(width, height);			
		}
	}

	/*
	 * Need to stop the GameThread if the surface is destroyed
	 * Remember this doesn't need to happen when app is paused on even stopped.
	 */
	public void surfaceDestroyed(SurfaceHolder arg0) {
		
		boolean retry = true;
		if(thread!=null) {
			thread.setRunning(false);
		}
		
		//join the thread with this thread
		while (retry) {
			try {
				if(thread!=null) {
					thread.join();
				}
				retry = false;
			} 
			catch (InterruptedException e) {
				//naugthy, ought to do something...
			}
		}
	}
	
	/*
	 * Accelerometer
	 */

	public void startSensor(SensorManager sm) {
		sm.registerListener(this.sensorAccelerometer, 
				sm.getDefaultSensor(Sensor.TYPE_ORIENTATION),	
				SensorManager.SENSOR_DELAY_GAME);
	}
	
	public void removeSensor(SensorManager sm) {
		sm.unregisterListener(this.sensorAccelerometer);
		this.sensorAccelerometer = null;
	}
	
}
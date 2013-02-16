package uk.ac.reading.se2ja11.SlavaBezgachev.deepspaceodyssey;

import android.app.Activity;
import android.content.Context;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.TextView;

public class GameActivity extends Activity {
	
    private static final int MENU_RESUME = 1;
    private static final int MENU_START = 2;
    private static final int MENU_STOP = 3;

    private GameThread mGameThread;
    private GameView mGameView;	
	
	/** Called when the activity is first created. */
    @Override
    protected void onCreate(Bundle savedInstanceState) { 
        super.onCreate(savedInstanceState);
        
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        setContentView(R.layout.activity_game);
        
        mGameView = (GameView)findViewById(R.id.gamearea);
        mGameView.setStatusView((TextView)findViewById(R.id.text));
        mGameView.setScoreView((TextView)findViewById(R.id.score));
              	
        this.startGame(mGameView, null, savedInstanceState);
    }
    
	private void startGame(GameView gView, GameThread gThread, Bundle savedInstanceState) {    	
        if (savedInstanceState == null) {
            // we were just launched: set up a new game
        	mGameThread = new GameThread(mGameView);
        	mGameView.setThread(mGameThread);
            mGameThread.setState(GameThread.STATE_READY);
        } 
        else {
        	if(mGameThread != null) {
        		//Thread is lives, just restart it!
        		mGameThread.restoreState(savedInstanceState);
        		if(mGameThread.getMode() == GameThread.STATE_RUNNING) {
        			mGameThread.setState(GameThread.STATE_PAUSE);
        		}
        	}
        	else {
        		//make a new thread with the values from savedInstanceState
        		gThread = new GameThread(mGameView);
        		mGameView.setThread(gThread);
        		mGameThread = mGameView.getThread();
        		mGameThread.restoreState(savedInstanceState);
        		mGameThread.setState(GameThread.STATE_READY);
        	}
        }
        
        mGameView.startSensor((SensorManager)getSystemService(Context.SENSOR_SERVICE));
    }
    
	/*
	 * Activity state functions
	 */
	
    @Override
    protected void onPause() {
        super.onPause();
        
        if(mGameThread.getMode() == GameThread.STATE_RUNNING) {
        	mGameThread.setState(GameThread.STATE_PAUSE);
        }
    }

    
    @Override
	protected void onDestroy() {
		super.onDestroy();
    	
    	mGameView.cleanup();
        mGameView.removeSensor((SensorManager)getSystemService(Context.SENSOR_SERVICE));
        
        mGameThread = null;
        mGameView = null;
	}    
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // just have the View's thread save its state into our Bundle
        super.onSaveInstanceState(outState);

        if(mGameThread != null) {
        	mGameThread.saveState(outState);
        }
    }    
    
    
    /*
     * UI Functions
     */
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        
        menu.add(0, MENU_START, 0, R.string.menu_start);
        menu.add(0, MENU_STOP, 0, R.string.menu_stop);
        menu.add(0, MENU_RESUME, 0, R.string.menu_resume);

        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_START:
                mGameThread.doStart();
                return true;
            case MENU_STOP:    			
    			mGameThread.setState(GameThread.STATE_LOSE, 
    								getText(R.string.message_stopped));
                return true;
            case MENU_RESUME:
                mGameThread.unpause();
                return true;
        }

        return false;
    }

	public void onNothingSelected(AdapterView<?> arg0) {
		// Do nothing if nothing is selected
	}
}
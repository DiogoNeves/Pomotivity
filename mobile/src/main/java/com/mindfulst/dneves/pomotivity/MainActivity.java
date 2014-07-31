package com.mindfulst.dneves.pomotivity;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


/**
 * Some description.
 */
public class MainActivity extends Activity {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    // TODO: confirm if this can't cause a bug when we leave the application while the pomodoro is running and
    // come back after it has finished.
    PomodoroApi api = PomodoroApi.getInstance();
    api.load(this, getPreferences(Context.MODE_PRIVATE));

    findViewById(R.id.start_button).setOnClickListener(mStartButtonListener);
    findViewById(R.id.stop_button).setOnClickListener(mStopButtonListener);
    findViewById(R.id.pause_button).setOnClickListener(mPauseButtonListener);

    api.setPomodoroListener(new PomodoroApi.PomodoroEventListener() {
      @Override
      public void pomodoroStarted(final PomodoroApi.PomodoroEvent event) {
        ((TextView) findViewById(R.id.last_action)).setText("started");
      }

      @Override
      public void pomodoroTicked(final PomodoroApi.PomodoroEvent event) {
        runOnUiThread(new Runnable() {
          @Override
          public void run() {
            ((TextView) findViewById(R.id.current_time)).setText(String.valueOf(event.currentTime));
          }
        });
      }

      @Override
      public void pomodoroEnded(final PomodoroApi.PomodoroEvent event) {
        runOnUiThread(new Runnable() {
          @Override
          public void run() {
            ((TextView) findViewById(R.id.last_action)).setText("ended");
          }
        });
      }

      @Override
      public void breakStarted(final PomodoroApi.PomodoroEvent event) {
        runOnUiThread(new Runnable() {
          @Override
          public void run() {
            String breakName = event.currentState == PomodoroApi.PomodoroState.LONG_BREAK ? "long" : "short";
            ((TextView) findViewById(R.id.last_action)).setText(breakName + " break started");
          }
        });
      }

      @Override
      public void pomodoroFinished(final PomodoroApi.PomodoroEvent event) {
        runOnUiThread(new Runnable() {
          @Override
          public void run() {
            if (event.currentTime > 0) {
              ((TextView) findViewById(R.id.last_action)).setText("stopped");
            }
            else {
              ((TextView) findViewById(R.id.last_action)).setText("finished");
            }
          }
        });
      }

      @Override
      public void paused(PomodoroApi.PomodoroEvent event) {
        runOnUiThread(new Runnable() {
          @Override
          public void run() {
            ((TextView) findViewById(R.id.last_action)).setText("paused");
          }
        });
      }

      @Override
      public void resumed(PomodoroApi.PomodoroEvent event) {
        runOnUiThread(new Runnable() {
          @Override
          public void run() {
            ((TextView) findViewById(R.id.last_action)).setText("resumed");
          }
        });
      }
    });
  }

  @Override
  protected void onPause() {
    super.onPause();
    SharedPreferences preferences = getPreferences(Context.MODE_PRIVATE);
    SharedPreferences.Editor editor = preferences.edit();
    PomodoroApi.getInstance().save(this, editor);
    editor.apply();
  }

  View.OnClickListener mStartButtonListener = new View.OnClickListener() {

    @Override
    public void onClick(View view) {
      try {
        PomodoroApi.getInstance().start();
      }
      catch (PomodoroApi.AlreadyRunningException e) {
        e.printStackTrace();
      }
    }
  };

  View.OnClickListener mStopButtonListener = new View.OnClickListener() {

    @Override
    public void onClick(View view) {
      PomodoroApi.getInstance().stop();
    }
  };

  View.OnClickListener mPauseButtonListener = new View.OnClickListener() {

    @Override
    public void onClick(View view) {
      Button buttonView = ((Button) view);
      if (buttonView.getText() == getString(R.string.pause_button)) {
        PomodoroApi.getInstance().pause();
        buttonView.setText(getString(R.string.resume_button));
      }
      else {
        PomodoroApi.getInstance().resume();
        buttonView.setText(getString(R.string.pause_button));
      }
    }
  };
}

package com.mindfulst.dneves.pomotivity;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;


/**
 * Some description.
 */
public class MainActivity extends Activity {
  private static final String DEBUG_TAG = "pomoui";

  private SoundPool mPlayer = null;

  private static int mTickSoundId  = 0;
  private static int mTickStreamId = 0;
  private static int mAlarmSoundId = 0;

  private PeriodFormatter mFormatter = null;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    PomodoroApi api = PomodoroApi.getInstance();
    api.load(this, getPreferences(Context.MODE_PRIVATE));

    // We need 2 channels, 1 for the tick the other for the end alarm
    if (mPlayer == null) {
      mPlayer = new SoundPool(2, AudioManager.STREAM_MUSIC, 0);
      mTickSoundId = mPlayer.load(this, R.raw.tick_sound, 1);
      mAlarmSoundId = mPlayer.load(this, R.raw.alarm_sound, 1);
    }

    findViewById(R.id.start_button).setOnClickListener(mStartButtonListener);
    findViewById(R.id.stop_button).setOnClickListener(mStopButtonListener);
    findViewById(R.id.pause_button).setOnClickListener(mPauseButtonListener);
    findViewById(R.id.resume_button).setOnClickListener(mResumeButtonListener);

    mFormatter =
        new PeriodFormatterBuilder().printZeroAlways().minimumPrintedDigits(2).appendMinutes().appendSeparator(":")
                                    .printZeroAlways().minimumPrintedDigits(2).appendSeconds().toFormatter();
    Period period = new Period(PomodoroApi.POMODORO_DURATION * 1000);
    ((TextView) findViewById(R.id.current_time)).setText(mFormatter.print(period));

    api.setPomodoroListener(new PomodoroApi.PomodoroEventListener() {
      @Override
      public void pomodoroStarted(final PomodoroApi.PomodoroEvent event) {
        ((TextView) findViewById(R.id.last_action)).setText("started");
        mTickStreamId = mPlayer.play(mTickSoundId, 1.0f, 1.0f, 1, -1, 1.0f);
        if (mTickStreamId == 0) {
          Log.e(DEBUG_TAG, "Oops, failed to play the tick sound");
        }

        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.setStreamMute(AudioManager.STREAM_NOTIFICATION, true);
        audioManager.setStreamMute(AudioManager.STREAM_RING, true);

        resetButtonsVisibility(false);
      }

      @Override
      public void pomodoroTicked(final PomodoroApi.PomodoroEvent event) {
        runOnUiThread(new Runnable() {
          @Override
          public void run() {
            Period period = new Period(event.currentTime * 1000);
            ((TextView) findViewById(R.id.current_time)).setText(mFormatter.print(period));
          }
        });
      }

      @Override
      public void pomodoroEnded(final PomodoroApi.PomodoroEvent event) {
        runOnUiThread(new Runnable() {
          @Override
          public void run() {
            ((TextView) findViewById(R.id.last_action)).setText("ended");
            mPlayer.play(mAlarmSoundId, 1.0f, 1.0f, 2, 0, 1.0f);
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            audioManager.setStreamMute(AudioManager.STREAM_NOTIFICATION, false);
            audioManager.setStreamMute(AudioManager.STREAM_RING, false);
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
            resetButtonsVisibility(true);

            if (event.currentTime > 0) {
              ((TextView) findViewById(R.id.last_action)).setText("stopped");
              AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
              audioManager.setStreamMute(AudioManager.STREAM_NOTIFICATION, false);
              audioManager.setStreamMute(AudioManager.STREAM_RING, false);
            }
            else {
              ((TextView) findViewById(R.id.last_action)).setText("finished");
              mPlayer.play(mAlarmSoundId, 1.0f, 1.0f, 2, 0, 1.0f);
            }
            if (mTickStreamId != 0) {
              mPlayer.stop(mTickStreamId);
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
            if (mTickStreamId != 0) {
              mPlayer.pause(mTickStreamId);
            }

            setButtonsVisibility(true);
          }
        });
      }

      @Override
      public void resumed(PomodoroApi.PomodoroEvent event) {
        runOnUiThread(new Runnable() {
          @Override
          public void run() {
            ((TextView) findViewById(R.id.last_action)).setText("resumed");
            if (mTickStreamId != 0) {
              mPlayer.resume(mTickStreamId);
            }

            setButtonsVisibility(false);
          }
        });
      }
    });
  }

  private void resetButtonsVisibility(boolean isFinishing) {
    if (isFinishing) {
      findViewById(R.id.start_button).setVisibility(View.VISIBLE);
      findViewById(R.id.pause_button).setVisibility(View.INVISIBLE);
      findViewById(R.id.stop_button).setVisibility(View.INVISIBLE);
      findViewById(R.id.resume_button).setVisibility(View.INVISIBLE);
    }
    else {
      // Starting
      findViewById(R.id.start_button).setVisibility(View.INVISIBLE);
      findViewById(R.id.pause_button).setVisibility(View.VISIBLE);
    }
  }

  private void setButtonsVisibility(boolean isPaused) {
    if (isPaused) {
      findViewById(R.id.pause_button).setVisibility(View.INVISIBLE);
      findViewById(R.id.resume_button).setVisibility(View.VISIBLE);
      findViewById(R.id.stop_button).setVisibility(View.VISIBLE);
    }
    else {
      findViewById(R.id.pause_button).setVisibility(View.VISIBLE);
      findViewById(R.id.resume_button).setVisibility(View.INVISIBLE);
      findViewById(R.id.stop_button).setVisibility(View.INVISIBLE);
    }
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
      PomodoroApi.getInstance().pause();
    }
  };

  View.OnClickListener mResumeButtonListener = new View.OnClickListener() {

    @Override
    public void onClick(View view) {
      PomodoroApi.getInstance().resume();
    }
  };
}

package com.mindfulst.dneves.pomotivity;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.widget.ViewSwitcher;

import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;


/**
 * Some description.
 */
public class MainActivity extends Activity {
  private static final String DEBUG_TAG = "pomoui";

  private ViewSwitcher mSwitcher = null;

  private SoundPool mPlayer = null;

  private static int mTickSoundId  = 0;
  private static int mTickStreamId = 0;
  private static int mAlarmSoundId = 0;

  private PeriodFormatter mFormatter = null;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    // TODO: Remove this!!!
    PomodoroApi testApi = PomodoroApi.getInstance();
    testApi.runTest();

    mSwitcher = (ViewSwitcher) findViewById(R.id.view_switcher);

    PomodoroApi api = PomodoroApi.getInstance();
    api.load(this, getPreferences(Context.MODE_PRIVATE));

    // We need 2 channels, 1 for the tick the other for the end alarm
    if (mPlayer == null) {
      mPlayer = new SoundPool(2, AudioManager.STREAM_MUSIC, 0);
      mTickSoundId = mPlayer.load(this, R.raw.tick_sound, 1);
      mAlarmSoundId = mPlayer.load(this, R.raw.alarm_sound, 1);
    }

    ArrayAdapter adapter =
        ArrayAdapter.createFromResource(this, R.array.project_options, android.R.layout.simple_list_item_1);
    ((Spinner) findViewById(R.id.current_project)).setAdapter(adapter);

    findViewById(R.id.start_button).setOnClickListener(mStartButtonListener);
    findViewById(R.id.stop_button).setOnClickListener(mStopButtonListener);
    findViewById(R.id.pause_button).setOnClickListener(mPauseButtonListener);
    findViewById(R.id.resume_button).setOnClickListener(mResumeButtonListener);

    ToggleButton autoStartToggle = (ToggleButton) findViewById(R.id.auto_start_toggle);
    autoStartToggle.setChecked(api.getAutoStart());
    autoStartToggle.setOnCheckedChangeListener(mAutoStartToggleListener);

    mFormatter =
        new PeriodFormatterBuilder().printZeroAlways().minimumPrintedDigits(2).appendMinutes().appendSeparator(":")
                                    .printZeroAlways().minimumPrintedDigits(2).appendSeconds().toFormatter();
    Period period = new Period(PomodoroApi.POMODORO_DURATION * 1000);
    ((TextView) findViewById(R.id.current_time)).setText(mFormatter.print(period));

    api.setPomodoroListener(new PomodoroApi.PomodoroEventListener() {
      @Override
      public void pomodoroStarted(final PomodoroApi.PomodoroEvent event) {
        runOnUiThread(new Runnable() {
          @Override
          public void run() {
            mTickStreamId = mPlayer.play(mTickSoundId, 1.0f, 1.0f, 1, -1, 1.0f);
            if (mTickStreamId == 0) {
              Log.e(DEBUG_TAG, "Oops, failed to play the tick sound");
            }

            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            audioManager.setStreamMute(AudioManager.STREAM_NOTIFICATION, true);
            audioManager.setStreamMute(AudioManager.STREAM_RING, true);

            resetButtonsVisibility(false, event.autoStart);
          }
        });
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
            mSwitcher.showNext();
            Period period = new Period(event.currentTime * 1000);
            ((TextView) findViewById(R.id.current_time)).setText(mFormatter.print(period));
          }
        });
      }

      @Override
      public void pomodoroFinished(final PomodoroApi.PomodoroEvent event) {
        runOnUiThread(new Runnable() {
          @Override
          public void run() {
            resetButtonsVisibility(true, event.autoStart);

            if (event.currentTime > 0) {
              AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
              audioManager.setStreamMute(AudioManager.STREAM_NOTIFICATION, false);
              audioManager.setStreamMute(AudioManager.STREAM_RING, false);
            }
            else {
              mPlayer.play(mAlarmSoundId, 1.0f, 1.0f, 2, 0, 1.0f);
            }
            if (mTickStreamId != 0) {
              mPlayer.stop(mTickStreamId);
            }

            if (event.currentState != PomodoroApi.PomodoroState.POMODORO) {
              mSwitcher.showPrevious();
            }
            Period period = new Period(PomodoroApi.POMODORO_DURATION * 1000);
            ((TextView) findViewById(R.id.current_time)).setText(mFormatter.print(period));
          }
        });
      }

      @Override
      public void paused(PomodoroApi.PomodoroEvent event) {
        runOnUiThread(new Runnable() {
          @Override
          public void run() {
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
            if (mTickStreamId != 0) {
              mPlayer.resume(mTickStreamId);
            }

            setButtonsVisibility(false);
          }
        });
      }
    });
  }

  private void resetButtonsVisibility(boolean isFinishing, boolean isAutoStart) {
    if (isFinishing && !isAutoStart) {
      findViewById(R.id.start_button).setVisibility(View.VISIBLE);
      findViewById(R.id.pause_button).setVisibility(View.GONE);
    }
    else {
      // Starting
      findViewById(R.id.start_button).setVisibility(View.GONE);
      findViewById(R.id.pause_button).setVisibility(View.VISIBLE);
    }
    findViewById(R.id.stop_button).setVisibility(View.GONE);
    findViewById(R.id.resume_button).setVisibility(View.GONE);
  }

  private void setButtonsVisibility(boolean isPaused) {
    findViewById(R.id.start_button).setVisibility(View.GONE);
    if (isPaused) {
      findViewById(R.id.pause_button).setVisibility(View.GONE);
      findViewById(R.id.resume_button).setVisibility(View.VISIBLE);
      findViewById(R.id.stop_button).setVisibility(View.VISIBLE);
    }
    else {
      findViewById(R.id.pause_button).setVisibility(View.VISIBLE);
      findViewById(R.id.resume_button).setVisibility(View.GONE);
      findViewById(R.id.stop_button).setVisibility(View.GONE);
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

  CompoundButton.OnCheckedChangeListener mAutoStartToggleListener = new CompoundButton.OnCheckedChangeListener() {
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
      PomodoroApi.getInstance().setAutoStart(isChecked);
    }
  };
}

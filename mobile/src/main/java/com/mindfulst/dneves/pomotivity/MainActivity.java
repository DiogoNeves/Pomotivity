package com.mindfulst.dneves.pomotivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.widget.ViewSwitcher;

import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;


/**
 * Some description.
 */
public class MainActivity extends Activity {
  private static final String DEBUG_TAG = "pomoui";

  private ViewSwitcher mSwitcher = null;

  private Set<String>          mProjectSet       = null;
  private ArrayAdapter<String> mProjectAdapter   = null;
  private AlertDialog          mAddProjectDialog = null;

  private SoundPool mPlayer = null;

  private static int mTickSoundId  = 0;
  private static int mTickStreamId = 0;
  private static int mAlarmSoundId = 0;

  private PeriodFormatter mFormatter = null;

  private AlertDialog createProjectDialog() {
    AlertDialog.Builder alert = new AlertDialog.Builder(this);

    alert.setTitle(R.string.project_dialog_title);
    final EditText input = new EditText(this);
    input.setInputType(InputType.TYPE_TEXT_FLAG_CAP_WORDS);
    input.setHint(R.string.project_dialog_hint);
    alert.setView(input);

    alert.setPositiveButton(R.string.project_dialog_ok, new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int whichButton) {
        String newProjectName = input.getText().toString();
        input.setText("");
        if (newProjectName != null && !newProjectName.isEmpty() && !mProjectSet.contains(newProjectName)) {
          mProjectSet.add(newProjectName);
          mProjectAdapter.insert(newProjectName, 0);
          mProjectAdapter.notifyDataSetChanged();
          ((Spinner) findViewById(R.id.current_project)).setSelection(0);
        }
      }
    });

    alert.setNegativeButton(R.string.project_dialog_cancel, new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int whichButton) {
      }
    });
    return alert.create();
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    mSwitcher = (ViewSwitcher) findViewById(R.id.view_switcher);

    final PomodoroApi api = PomodoroApi.getInstance();
    api.load(this, getPreferences(Context.MODE_PRIVATE));

    // We need 2 channels, 1 for the tick the other for the end alarm
    if (mPlayer == null) {
      mPlayer = new SoundPool(2, AudioManager.STREAM_MUSIC, 0);
      mTickSoundId = mPlayer.load(this, R.raw.tick_sound, 1);
      mAlarmSoundId = mPlayer.load(this, R.raw.alarm_sound, 1);
    }

    mProjectAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item) {

      @Override
      public View getView(int position, View convertView, ViewGroup parent) {
        View v = super.getView(position, convertView, parent);
        if (position == getCount()) {
          ((TextView) v.findViewById(android.R.id.text1)).setText("");
          ((TextView) v.findViewById(android.R.id.text1)).setHint(getResources().getString(R.string.project_hint));
        }
        return v;
      }

      @Override
      public int getCount() {
        return super.getCount() - 1;
      }

    };
    Collection<String> allProjects = api.getAllProjects();
    mProjectSet = new HashSet<String>(allProjects);
    mProjectAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    mProjectAdapter.addAll(allProjects);
    // Always keep both of this last ;)
    mProjectAdapter.add(getResources().getString(R.string.project_add));
    mProjectAdapter.add(getResources().getString(R.string.project_hint));
    mAddProjectDialog = createProjectDialog();

    Spinner projectChooser = (Spinner) findViewById(R.id.current_project);
    projectChooser.setAdapter(mProjectAdapter);
    projectChooser.setSelection(mProjectAdapter.getCount());
    projectChooser.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      // We're going to set selection after this, no problem
      private int lastSelection = 0;

      @Override
      public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (position == parent.getCount() - 1) {
          // + Project
          Log.d(DEBUG_TAG, "Adding project");
          parent.setSelection(lastSelection);
          mAddProjectDialog.show();
        }
        else {
          lastSelection = position;
          if (position != parent.getCount()) {
            // User Project
            String projectName = ((TextView) view).getText().toString();
            PomodoroApi.getInstance().setCurrentProject(projectName);
            Log.d(DEBUG_TAG, String.format("Setting to current project to %s", projectName));
          }
        }
      }

      @Override
      public void onNothingSelected(AdapterView<?> parent) {
        Log.d(DEBUG_TAG, String.format("count: %d", parent.getCount()));
      }
    });

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

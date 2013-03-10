package info.ripz.carpc;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.app.ActionBar.LayoutParams;
import android.app.Activity;
import android.graphics.Color;
/*
public class Settings extends Activity {

	final static String LOG_TAG = "CarPC_Settings";
	protected CarPCPreferences CarPCPrefs;
	private SeekBar VolumeSeekBar = null;
	private SeekBar BalanceSeekBar = null;
	private SeekBar FadSeekBar = null;
	private SeekBar BassSeekBar = null;
	private SeekBar TrebleSeekBar = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.settingsactivity);
		View mlayout = findViewById(R.id.SettingsMainLayout);
		LayoutParams lp;
		lp = (LayoutParams) mlayout.getLayoutParams();
	    lp.width = 700;
	    lp.height = 400;
		mlayout.setLayoutParams(lp);

		CarPCPrefs = new CarPCPreferences(getApplicationContext());
		String Volume = CarPCPreferences.getPreference("Volume");
		String Balance = CarPCPreferences.getPreference("Balance");
		String Fad = CarPCPreferences.getPreference("Fad");
		String Bass = CarPCPreferences.getPreference("Bass");
		String Treble = CarPCPreferences.getPreference("Treble");

		VolumeSeekBar = (SeekBar) findViewById(R.id.VolumeSeekBar);
		BalanceSeekBar = (SeekBar) findViewById(R.id.BalanceSeekBar);
		FadSeekBar = (SeekBar) findViewById(R.id.FadSeekBar);
		BassSeekBar = (SeekBar) findViewById(R.id.BassSeekBar);
		TrebleSeekBar = (SeekBar) findViewById(R.id.TrebleSeekBar);

		VolumeSeekBar.setMax(31);
		BalanceSeekBar.setMax(31);
		FadSeekBar.setMax(31);
		BassSeekBar.setMax(14);
		TrebleSeekBar.setMax(14);

		Log.i(LOG_TAG, "Preferences Volume = " + Volume);
		Log.i(LOG_TAG, "Preferences Balance = " + Balance);
		Log.i(LOG_TAG, "Preferences Fad = " + Fad);
		Log.i(LOG_TAG, "Preferences Bass = " + Bass);
		Log.i(LOG_TAG, "Preferences Treble = " + Treble);

		if (Volume != null) {
			VolumeSeekBar.setProgress(Integer.valueOf(Volume));
			TextView tv_f = (TextView) findViewById(R.id.SettingsVolume);
			tv_f.setText(Volume);
		}

		if (Balance != null) {
			BalanceSeekBar.setProgress(Integer.valueOf(Balance));
			TextView tv_f = (TextView) findViewById(R.id.SettingsBalance);
			tv_f.setText(Balance);
			CarPCService.STM32_Send("B" + Balance + "*");
		}

		if (Fad != null) {
			FadSeekBar.setProgress(Integer.valueOf(Fad));
			TextView tv_f = (TextView) findViewById(R.id.SettingsFad);
			tv_f.setText(Fad);
			CarPCService.STM32_Send("F" + Fad + "*");
		}

		if (Bass != null) {
			BassSeekBar.setProgress(Integer.valueOf(Bass));
			TextView tv_f = (TextView) findViewById(R.id.SettingsBass);
			tv_f.setText(Bass);
			CarPCService.STM32_Send("L" + Bass + "*");
		}

		if (Treble != null) {
			TrebleSeekBar.setProgress(Integer.valueOf(Treble));
			TextView tv_f = (TextView) findViewById(R.id.SettingsTreble);
			tv_f.setText(Treble);
			CarPCService.STM32_Send("H" + Treble + "*");
		}

		VolumeSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				TextView tv_f = (TextView) findViewById(R.id.SettingsVolume);
				tv_f.setText(String.valueOf(progress));
				CarPCPreferences.savePreference("Volume",
						String.valueOf(progress));
				Log.i(LOG_TAG, "Set Volume = " + String.valueOf(progress));
				CarPCService.STM32_Send("V" + String.valueOf(progress) + "*");
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}
		});

		BalanceSeekBar
				.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
					@Override
					public void onProgressChanged(SeekBar seekBar,
							int progress, boolean fromUser) {
						TextView tv_f = (TextView) findViewById(R.id.SettingsBalance);
						tv_f.setText(String.valueOf(progress));
						CarPCPreferences.savePreference("Balance",
								String.valueOf(progress));
						Log.i(LOG_TAG, "Set Balance = " + String.valueOf(progress));
						CarPCService.STM32_Send("B" + String.valueOf(progress)
								+ "*");
					}

					@Override
					public void onStartTrackingTouch(SeekBar seekBar) {
					}

					@Override
					public void onStopTrackingTouch(SeekBar seekBar) {
					}
				});

		FadSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				TextView tv_f = (TextView) findViewById(R.id.SettingsFad);
				tv_f.setText(String.valueOf(progress));
				CarPCPreferences.savePreference("Fad", String.valueOf(progress));
				Log.i(LOG_TAG, "Set Fad = " + String.valueOf(progress));
				CarPCService.STM32_Send("F" + String.valueOf(progress) + "*");
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}
		});

		BassSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				TextView tv_f = (TextView) findViewById(R.id.SettingsBass);
				tv_f.setText(String.valueOf(progress));
				CarPCPreferences.savePreference("Bass",
						String.valueOf(progress));
				Log.i(LOG_TAG, "Set Bass = " + String.valueOf(progress));
				CarPCService.STM32_Send("L" + String.valueOf(progress) + "*");
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}
		});

		TrebleSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				TextView tv_f = (TextView) findViewById(R.id.SettingsTreble);
				tv_f.setText(String.valueOf(progress));
				CarPCPreferences.savePreference("Treble",
						String.valueOf(progress));
				Log.i(LOG_TAG, "Set Treble = " + String.valueOf(progress));
				CarPCService.STM32_Send("H" + String.valueOf(progress) + "*");
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}
		});

		Button BT_pairing = (Button) findViewById(R.id.BT_Pairing_button);
		BT_pairing.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Log.i(LOG_TAG, "BT Pairing press button");
				CarPCService.STM32_Send("P1*");
			}
		});

		Button BT_onoff = (Button) findViewById(R.id.BT_ONOFF_button);
		BT_onoff.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Log.i(LOG_TAG, "BT On/Off press button");
				// TODO: Send BT on/off to stm32
			}
		});

		Button Camera = (Button) findViewById(R.id.Camera_button);
		Camera.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Log.i(LOG_TAG, "Camera press button");
				// TODO: Camera start test activity
			}
		});

		Button STM32_send = (Button) findViewById(R.id.STM32_send_button);
		STM32_send.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				// TODO: Send data to stm32 from STM32_send_text texedit field
				EditText stm32_command = (EditText) findViewById(R.id.STM32_send_text);
				Log.i(LOG_TAG, "STM32 command = "
						+ stm32_command.getText().toString());
				stm32_command = null;
			}
		});
	} // onCreate
} // Activity
*/
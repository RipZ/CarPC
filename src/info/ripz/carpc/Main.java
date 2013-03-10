package info.ripz.carpc;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class Main extends Activity {

	final static String LOG_TAG = "CarPC_Main";
	private Intent mStatusIntent;
	private Intent mTrackIntent;
	private Intent mAAIntent;
	private Bundle mCurrentTrack;

	private SeekBar VolumeSeekBar;
	private SeekBar BalanceSeekBar;
	private SeekBar FadSeekBar;
	private SeekBar BassSeekBar;
	private SeekBar TrebleSeekBar;

	private LocationManager lm; 
    private LocationListener ll;
    static double latitude, longitude;
    final Context context = this;
    
    BroadcastReceiver br;
    public final static String BROADCAST_ACTION = "info.ripz.carpc.broadcast";
    public final static String PARAM = "param";
    public final static String VALUE = "value";
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(LOG_TAG, "onCreate()");

		setContentView(R.layout.mainactivity);

		startService(new Intent(this, CarPCService.class));

	    br = new BroadcastReceiver() {
	      public void onReceive(Context context, Intent intent) {
	        int param = intent.getIntExtra(PARAM, 0);
	        int value = intent.getIntExtra(VALUE, 0);
	        Log.d(LOG_TAG,"Received intent = " + param + ":" + value);
	        switch (param) {
	        case Parameters.ACTIVE_SOURCE:
	        		DisplayAudioSource();
	        	break;
	        case Parameters.UPDATE_FM:
	        	UpdateFM();
	        	break;
	        default:
	        	break;
	        }
	      }
	    };

	    IntentFilter intFilt = new IntentFilter(BROADCAST_ACTION);
	    registerReceiver(br, intFilt);
		
		TextView myTextView=(TextView)findViewById(R.id.FM_freq);
		Typeface typeFace=Typeface.createFromAsset(getAssets(),"fonts/ds-digital.ttf");
		myTextView.setTypeface(typeFace);

		lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE); 
		ll = new SpeedoActionListener(); 
		lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, ll); 
		
		mStatusIntent = registerReceiver(mStatusReceiver, new IntentFilter(
				PowerAMPiAPI.ACTION_STATUS_CHANGED));
		mTrackIntent = registerReceiver(mTrackReceiver, new IntentFilter(
				PowerAMPiAPI.ACTION_TRACK_CHANGED));
		mAAIntent = registerReceiver(mAAReceiver, new IntentFilter(
				PowerAMPiAPI.ACTION_AA_CHANGED));

		ImageView PowerAMP = (ImageView) findViewById(R.id.album_art);

		UpdateCARParameters();
		
		PowerAMP.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent PowerAmp_intent = new Intent();
				PowerAmp_intent.setComponent(new ComponentName(
						"com.maxmpz.audioplayer",
						"com.maxmpz.audioplayer.PlayerUIActivity"));
				PowerAmp_intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
						| Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				startActivity(PowerAmp_intent);
			}
		});

		ImageView Rewind = (ImageView) findViewById(R.id.buttonRewind);
		Rewind.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (!Parameters.poweramp_mode) {
					CarPCService.PrevRadioStation();
					UpdateFM();
				}
				else {
					startService(new Intent(PowerAMPiAPI.ACTION_API_COMMAND).putExtra(
									PowerAMPiAPI.COMMAND, PowerAMPiAPI.Commands.PREVIOUS_IN_CAT));
				}
			}
		});
		
		ImageView Forward = (ImageView) findViewById(R.id.buttonForward);
		Forward.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (!Parameters.poweramp_mode) {
					CarPCService.NextRadioStation();
					UpdateFM();
				}
				else {
					startService(new Intent(PowerAMPiAPI.ACTION_API_COMMAND).putExtra(
							PowerAMPiAPI.COMMAND, PowerAMPiAPI.Commands.NEXT_IN_CAT));
				}
			}
		});

		ImageView RewindSeek = (ImageView) findViewById(R.id.buttonRewindSeek);
		RewindSeek.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (!Parameters.poweramp_mode) {
					CarPCService.RadioFreqDown();
					UpdateFM();
				}
				else {
					startService(new Intent(PowerAMPiAPI.ACTION_API_COMMAND).putExtra(
							PowerAMPiAPI.COMMAND, PowerAMPiAPI.Commands.PREVIOUS));
				}
			}
		});

		ImageView ForwardSeek = (ImageView) findViewById(R.id.buttonForwardSeek);
		ForwardSeek.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (!Parameters.poweramp_mode) {
					CarPCService.RadioFreqUp();
					UpdateFM();
				}
				else {
					startService(new Intent(PowerAMPiAPI.ACTION_API_COMMAND).putExtra(
							PowerAMPiAPI.COMMAND, PowerAMPiAPI.Commands.NEXT));
				}
			}
		});

		ImageView PlayPause = (ImageView) findViewById(R.id.buttonPlayPause);
		PlayPause.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				startService(new Intent(PowerAMPiAPI.ACTION_API_COMMAND).putExtra(
						PowerAMPiAPI.COMMAND,
						PowerAMPiAPI.Commands.TOGGLE_PLAY_PAUSE));
			}
		});

		ImageView Settings = (ImageView) findViewById(R.id.buttonSettings);
		Settings.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Log.d(LOG_TAG,"Settings pressed");
				final Dialog dialog = new Dialog(context);
				dialog.setTitle("Settings");
				dialog.setContentView(R.layout.settingsactivity);

				VolumeSeekBar = (SeekBar) dialog.findViewById(R.id.VolumeSeekBar);
				BalanceSeekBar = (SeekBar) dialog.findViewById(R.id.BalanceSeekBar);
				FadSeekBar = (SeekBar) dialog.findViewById(R.id.FadSeekBar);
				BassSeekBar = (SeekBar) dialog.findViewById(R.id.BassSeekBar);
				TrebleSeekBar = (SeekBar) dialog.findViewById(R.id.TrebleSeekBar);

				VolumeSeekBar.setMax(31);
				BalanceSeekBar.setMax(31);
				FadSeekBar.setMax(31);
				BassSeekBar.setMax(14);
				TrebleSeekBar.setMax(14);

				if (Parameters.PrefsVolume != null) {
					VolumeSeekBar.setProgress(Integer.valueOf(Parameters.PrefsVolume));
					TextView tv_f = (TextView) dialog.findViewById(R.id.SettingsVolume);
					tv_f.setText(Parameters.PrefsVolume);
				}

				if (Parameters.PrefsBalance != null) {
					BalanceSeekBar.setProgress(Integer.valueOf(Parameters.PrefsBalance));
					TextView tv_f = (TextView) dialog.findViewById(R.id.SettingsBalance);
					tv_f.setText(Parameters.PrefsBalance);
				}

				if (Parameters.PrefsFad != null) {
					FadSeekBar.setProgress(Integer.valueOf(Parameters.PrefsFad));
					TextView tv_f = (TextView) dialog.findViewById(R.id.SettingsFad);
					tv_f.setText(Parameters.PrefsFad);
				}

				if (Parameters.PrefsBass != null) {
					BassSeekBar.setProgress(Integer.valueOf(Parameters.PrefsBass));
					TextView tv_f = (TextView) dialog.findViewById(R.id.SettingsBass);
					tv_f.setText(Parameters.PrefsBass);
				}

				if (Parameters.PrefsTreble != null) {
					TrebleSeekBar.setProgress(Integer.valueOf(Parameters.PrefsTreble));
					TextView tv_f = (TextView) dialog.findViewById(R.id.SettingsTreble);
					tv_f.setText(Parameters.PrefsTreble);
				}
				
				Button dialogButton = (Button) dialog.findViewById(R.id.buttonSave);
				// if button is clicked, close the custom dialog
				dialogButton.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						dialog.dismiss();
					}
				});
				
				VolumeSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
					@Override
					public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
						TextView tv_f = (TextView) dialog.findViewById(R.id.SettingsVolume);
						tv_f.setText(String.valueOf(progress));
						CarPCPreferences.savePreference("Volume", String.valueOf(progress));
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

				BalanceSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
					@Override
					public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
							TextView tv_f = (TextView) dialog.findViewById(R.id.SettingsBalance);
							tv_f.setText(String.valueOf(progress));
							CarPCPreferences.savePreference("Balance",String.valueOf(progress));
							Log.i(LOG_TAG, "Set Balance = " + String.valueOf(progress));
							CarPCService.STM32_Send("B" + String.valueOf(progress) + "*");
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
					public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
						TextView tv_f = (TextView) dialog.findViewById(R.id.SettingsFad);
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
					public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
						TextView tv_f = (TextView) dialog.findViewById(R.id.SettingsBass);
						tv_f.setText(String.valueOf(progress));
						CarPCPreferences.savePreference("Bass", String.valueOf(progress));
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
					public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
						TextView tv_f = (TextView) dialog.findViewById(R.id.SettingsTreble);
						tv_f.setText(String.valueOf(progress));
						CarPCPreferences.savePreference("Treble", String.valueOf(progress));
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
				
				
				dialog.show();
			}
		});

		TextView RadioMenu = (TextView) findViewById(R.id.FM_name);

		RadioMenu.setOnLongClickListener(new OnLongClickListener() {
		public boolean onLongClick(View v) {
			TextView station_name = (TextView) findViewById(R.id.FM_name);

			AlertDialog.Builder alert = new AlertDialog.Builder(Main.this);

			if(station_name.getText() == "") {
				alert.setTitle("Enter new station name (" + Parameters.Current_FM_Freq + ") :");
				final EditText input = new EditText(Main.this);
				alert.setView(input);
				alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						String value = input.getText().toString();
						TextView tv_n = (TextView) findViewById(R.id.FM_name);
						tv_n.setText(value);
						Parameters.Current_FM_Name = value;
						CarPCService.SaveCurrentRadioStation();
					}
				});

				alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						// Canceled.
					}
				});
				alert.show();           
			}
			else {
				alert.setTitle("Remove station \"" + Parameters.Current_FM_Name + "\" ?");
				alert.setPositiveButton("Remove", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						CarPCService.RemoveCurrentRadioStation();
						UpdateFM();
					}
				});

				alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						// Canceled.
					}
				});
				alert.show();           
			}
			return true;
		}
	});
		
}
	
	void DisplayAudioSource () {
	
		TextView PowerAMPSong = (TextView) findViewById(R.id.Title);
		TextView PowerAMPArtist = (TextView) findViewById(R.id.Artist);
		TextView PowerAMPAlbum = (TextView) findViewById(R.id.Album);
		TextView RadioName = (TextView) findViewById(R.id.FM_name);
		TextView RadioFreq = (TextView) findViewById(R.id.FM_freq);
		ImageView TitleImage = (ImageView) findViewById(R.id.TitleImage);
		ImageView ArtistImage = (ImageView) findViewById(R.id.ArtistImage);
		ImageView AlbumImage = (ImageView) findViewById(R.id.AlbumImage);
		ImageView AAImage = (ImageView) findViewById(R.id.album_art);

		if (Parameters.poweramp_mode) {
			PowerAMPSong.setTextColor(Color.WHITE);
			PowerAMPArtist.setTextColor(Color.WHITE);
			PowerAMPAlbum.setTextColor(Color.WHITE);
			TitleImage.setAlpha(255);
			ArtistImage.setAlpha(255);
			AlbumImage.setAlpha(255);
			AAImage.setAlpha(255);
			RadioName.setTextColor(Color.DKGRAY);
			RadioFreq.setTextColor(Color.DKGRAY);
		}
		else {
			PowerAMPSong.setTextColor(Color.DKGRAY);
			PowerAMPArtist.setTextColor(Color.DKGRAY);
			PowerAMPAlbum.setTextColor(Color.DKGRAY);
			TitleImage.setAlpha(40);
			ArtistImage.setAlpha(40);
			AlbumImage.setAlpha(40);
			AAImage.setAlpha(40);
			RadioName.setTextColor(Color.WHITE);
			RadioFreq.setTextColor(Color.WHITE);
		}
	}
	
	private void UpdateFM() {
		TextView station_freq = (TextView) findViewById(R.id.FM_freq);
		station_freq.setText(Parameters.Current_FM_Freq);
		TextView station_name = (TextView) findViewById(R.id.FM_name);
		station_name.setText(Parameters.Current_FM_Name);
	}

	private void UpdateCARParameters() {
		TextView rpm = (TextView) findViewById(R.id.RPM);
		if (Parameters.RPM < 750) {
			rpm.setTextColor(Color.MAGENTA);
		}
		if ((Parameters.RPM > 750) && (Parameters.RPM < 3500)) {
			rpm.setTextColor(Color.GREEN);
		}
		if ((Parameters.RPM > 3500) && (Parameters.RPM < 4000)) {
			rpm.setTextColor(Color.YELLOW);
		}
		if (Parameters.RPM > 4000) {
			rpm.setTextColor(Color.RED);
		}
		rpm.setText(String.valueOf(Parameters.RPM));

		TextView cooler_temp = (TextView) findViewById(R.id.COOLER_TEMP);
		cooler_temp.setText(String.valueOf(Parameters.COOLER_TEMP) + "� C");
		TextView outdoor_temp = (TextView) findViewById(R.id.OUTDOOR_TEMP);
		outdoor_temp.setText(String.valueOf(Parameters.OUTDOOR_TEMP) + "� C");
	}

	private class SpeedoActionListener implements LocationListener 
    { 
		@Override
		public void onLocationChanged(Location location) { 
			if(location!=null) { 
//				 latitude = location.getLatitude();
//				 longitude = location.getLongitude();
//				 Log.d(LOG_TAG,"Latitude = " + String.format("%.5f",location.getLatitude()));
//				 Log.d(LOG_TAG,"Longitude = " + String.format("%.5f",location.getLongitude()));
				 if(location.hasSpeed()){ 
					int speed = (int) location.getSpeed();
					TextView Speed = (TextView) findViewById(R.id.Speed);
					if (speed == 0) Speed.setTextColor(Color.WHITE);
					if ((speed > 0) && (speed < 60)) Speed.setTextColor(Color.GREEN);
					if ((speed > 60) && (speed < 80)) Speed.setTextColor(Color.YELLOW);
					if ((speed > 80) && (speed < 110)) Speed.setTextColor(Color.GREEN);
					if ((speed > 110) && (speed < 130)) Speed.setTextColor(Color.YELLOW);
					if (speed > 130) Speed.setTextColor(Color.RED);
					Speed.setText(String.valueOf(speed));
				} 
			} 
		}

		@Override
		public void onProviderDisabled(String provider) {
		}

		@Override
		public void onProviderEnabled(String provider) {
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
		} 
    } 
	
	private BroadcastReceiver mStatusReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			mStatusIntent = intent;
			Parameters.poweramp_playing = !mStatusIntent.getBooleanExtra(
					PowerAMPiAPI.PAUSED, false);
			ImageButton PlayPause = (ImageButton) findViewById(R.id.buttonPlayPause);
			if(!Parameters.poweramp_playing) {
				PlayPause.setBackgroundResource(R.drawable.hardware_icons_toolbar_play);
			}
			else {
				PlayPause.setBackgroundResource(R.drawable.hardware_icons_toolbar_pause);
			}
		}
	};

	private BroadcastReceiver mTrackReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			mTrackIntent = intent;
			if (mTrackIntent != null) {
				mCurrentTrack = mTrackIntent.getBundleExtra(PowerAMPiAPI.TRACK);
				if (mCurrentTrack != null) {
					Parameters.poweramp_duration = mCurrentTrack
							.getInt(PowerAMPiAPI.Track.DURATION);
					Parameters.poweramp_title = mCurrentTrack
							.getString(PowerAMPiAPI.Track.TITLE);
					Parameters.poweramp_artist = mCurrentTrack
							.getString(PowerAMPiAPI.Track.ARTIST);
					Parameters.poweramp_album = mCurrentTrack
							.getString(PowerAMPiAPI.Track.ALBUM);
					TextView title = (TextView) findViewById(R.id.Title);
					title.setText(Parameters.poweramp_title);
					TextView artist = (TextView) findViewById(R.id.Artist);
					artist.setText(Parameters.poweramp_artist);
					TextView album = (TextView) findViewById(R.id.Album);
					album.setText(Parameters.poweramp_album);
				}
			}
		}
	};

	private BroadcastReceiver mAAReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			mAAIntent = intent;
			updateAlbumArt();
		}
	};

	private void updateAlbumArt() {
		Log.d(LOG_TAG, "updateAlbumArt");
		String directAAPath = mAAIntent
				.getStringExtra(PowerAMPiAPI.ALBUM_ART_PATH);
		if (!TextUtils.isEmpty(directAAPath)) {
			Log.d(LOG_TAG, "has AA, albumArtPath=" + directAAPath);
			((ImageView) findViewById(R.id.album_art)).setImageURI(Uri
					.parse(directAAPath));
		} else if (mAAIntent.hasExtra(PowerAMPiAPI.ALBUM_ART_BITMAP)) {
			Bitmap albumArtBitmap = mAAIntent
					.getParcelableExtra(PowerAMPiAPI.ALBUM_ART_BITMAP);
			if (albumArtBitmap != null) {
				((ImageView) findViewById(R.id.album_art))
						.setImageBitmap(albumArtBitmap);
			}
		} else {
			((ImageView) findViewById(R.id.album_art)).setImageBitmap(null);
		}
	}

	public void onDestroy() {
		super.onDestroy();
	}

	public void onStart() {
		super.onStart();
	}

	public void onResume() {
		super.onResume();
	}
	
	public void onPause() {
		super.onPause();
	}

	public void onStop() {
		super.onStop();
	}

	public void onRestart() {
		super.onRestart();
	}
}

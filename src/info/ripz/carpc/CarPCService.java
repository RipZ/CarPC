package info.ripz.carpc;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashMap;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class CarPCService extends Service {

	final static String LOG_TAG = "CarPC_Service";
	public static String packet = null;
	public static boolean STM32_ACK = false;
	public static boolean STM32_PACKET_OK = false;
	public static boolean STM32_PACKET = false;
	public static Integer TotalStations = 0;
	public static final String RadioStation = "fm_name";
	public static final String RadioFreq = "fm_freq";
	public static ArrayList <HashMap<String, Object>> myStations;

	protected static OutputStream mOutputStream;
	protected CarPCPreferences CarPCPrefs;

	private static InputStream mInputStream;
	private static ReadThread mReadThread;
	private static SerialPort mSerialPort = null;

	public void onCreate() {
		super.onCreate();
		Log.d(LOG_TAG, "CarPCService::onCreate");
	}

	public int onStartCommand(Intent intent, int flags, int startId) {

		Log.d(LOG_TAG, "CarPCService::onStartCommand");
		try {
			mSerialPort = getSerialPort();
			mOutputStream = mSerialPort.getOutputStream();
			mInputStream = mSerialPort.getInputStream();
		} catch (SecurityException e) {
			Log.e(LOG_TAG, "error_security");
		} catch (IOException e) {
			Log.e(LOG_TAG, "error_unknown");
		} catch (InvalidParameterException e) {
			Log.e(LOG_TAG, "error_configuration");
		}

		mReadThread = new ReadThread();
		mReadThread.start();

		myStations = new ArrayList<HashMap<String,Object>>();

		LoadPreferences();
		LoadRadioStations();
		
		Log.d(LOG_TAG, "Sending configuration to STM32");
		STM32_Send("S2*"); // TODO make startup source
		SendBroadcast(Parameters.ACTIVE_SOURCE, 0); // TODO
		STM32_Send("B" + Parameters.PrefsBalance + "*");
		STM32_Send("F" + Parameters.PrefsFad + "*");
		STM32_Send("L" + Parameters.PrefsBass + "*");
		STM32_Send("H" + Parameters.PrefsTreble + "*");
		STM32_Send("V" + Parameters.PrefsVolume + "*");
		Log.d(LOG_TAG, "Sending configuration to STM32 done");

		Log.d(LOG_TAG,"Current FM Radio Station position = " + Parameters.Current_FM_Pos);
		SetRadioStation(Parameters.Current_FM_Pos);
		SendBroadcast(Parameters.UPDATE_FM, 0);
		
		Intent Main_intent = new Intent();
		Main_intent.setComponent(new ComponentName("info.ripz.carpc",
				"info.ripz.carpc.Main"));
		Main_intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(Main_intent);
		
		Toast.makeText(this, "CarPC service starting", Toast.LENGTH_SHORT).show();
		return START_STICKY;
	}

	public IBinder onBind(Intent intent) {
		Log.d(LOG_TAG, "onBind");
		return null;
	}

	public void LoadPreferences() {

		CarPCPrefs = new CarPCPreferences(getApplicationContext());

		Parameters.PrefsVolume = CarPCPreferences.getPreference("Volume");
		Parameters.PrefsBalance = CarPCPreferences.getPreference("Balance");
		Parameters.PrefsFad = CarPCPreferences.getPreference("Fad");
		Parameters.PrefsBass = CarPCPreferences.getPreference("Bass");
		Parameters.PrefsTreble = CarPCPreferences.getPreference("Treble");
		Parameters.volume = Integer.valueOf(Parameters.PrefsVolume);
		if (CarPCPreferences.getPreference("RadioStation") == null) {
			Parameters.Current_FM_Pos = 0;
			Log.d(LOG_TAG,"CurrentRadioStation from pereferences = null, first start");
		}
//		else {
//			Parameters.Current_FM_Pos = Integer.valueOf(CarPCPreferences.getPreference("RadioStation"));
//			Log.d(LOG_TAG,"CurrentRadioStation from pereferences = " + Parameters.Current_FM_Pos);
//		}
	}
	
	
	public void LoadRadioStations() {
		// Loading FM radio stations from preferences 
		for(int i=0; i <=25; i++) {
			HashMap<String,Object> st = new HashMap<String, Object>();
			String station = CarPCPreferences.getPreference("FM" + i + "_name");
			String freq = CarPCPreferences.getPreference("FM" + i + "_freq");
			if (station == null && freq == null) {
				TotalStations = i;
				station = CarPCPreferences.getPreference("Listen_FM_Name");
				freq = CarPCPreferences.getPreference("Listen_FM_Freq");
				break;
			}
			st.put(RadioStation, station);
			st.put(RadioFreq, freq);
			myStations.add(st);
			Log.d(LOG_TAG,"Loaded " + st);
		}
		TotalStations--; // begin from 0 to infinity =)
		Log.i(LOG_TAG, "Loaded total " + TotalStations + " radio stations");
	}
	
	static void SetRadioStation (int station) {
		HashMap<String, Object> Station = myStations.get(station);
		Parameters.Current_FM_Name = Station.get("fm_name").toString();
		Parameters.Current_FM_Freq = Station.get("fm_freq").toString();
		Log.d(LOG_TAG,"Station " + station + " Freq: " + Parameters.Current_FM_Freq + ", Name: " + Parameters.Current_FM_Name);
		Parameters.CurrentFreq = Float.parseFloat(Parameters.Current_FM_Freq);
		Log.d(LOG_TAG,"CurrentFreq = " + Parameters.Current_FM_Freq);
		String stm32_freq = Parameters.Current_FM_Freq.replace(".","");
		Log.d(LOG_TAG,"stm32_freq = " + stm32_freq);
		STM32_Send("R" + stm32_freq + "*");
	}

	public static void PrevRadioStation() {
		Parameters.Current_FM_Pos--;
		if (Parameters.Current_FM_Pos < 0) {
			Parameters.Current_FM_Pos = TotalStations;
		}
		Log.d(LOG_TAG,"FM position = " + Parameters.Current_FM_Pos);
		SetRadioStation(Parameters.Current_FM_Pos);
		CarPCPreferences.savePreference("RadioStation", String.valueOf(Parameters.Current_FM_Pos));
	}

	public static void NextRadioStation() {
		Parameters.Current_FM_Pos++;
		if (Parameters.Current_FM_Pos > TotalStations) {
			Parameters.Current_FM_Pos = 0;
		}
		Log.d(LOG_TAG,"FM position = " + Parameters.Current_FM_Pos);
		SetRadioStation(Parameters.Current_FM_Pos);
		CarPCPreferences.savePreference("RadioStation", String.valueOf(Parameters.Current_FM_Pos));
	}
	
	public static void RadioFreqDown () {
		Parameters.CurrentFreq = Parameters.CurrentFreq - 0.1f; // -100 KHz
		if (Parameters.CurrentFreq < 87.5f) Parameters.CurrentFreq = 108.0f;
		BigDecimal bd = new BigDecimal(Parameters.CurrentFreq);
		bd = bd.setScale(1, BigDecimal.ROUND_HALF_DOWN);
//		Log.d(LOG_TAG, "FREQ = " + Parameters.CurrentFreq + " (" + bd.doubleValue() + ")");
		Parameters.Current_FM_Freq = String.valueOf(bd.doubleValue());
		Parameters.Current_FM_Name = "";
		String stm32_freq = String.valueOf(bd.doubleValue()).replace(".","");
		Log.i(LOG_TAG, "STM32_freq = " + stm32_freq);
		STM32_Send("R" + String.valueOf(stm32_freq) + "*");
	}

	public static void RadioFreqUp () {
		Parameters.CurrentFreq = Parameters.CurrentFreq + 0.1f; // +100 KHz
		if (Parameters.CurrentFreq > 108.0f) Parameters.CurrentFreq = 87.5f;
		BigDecimal bd = new BigDecimal(Parameters.CurrentFreq);
		bd = bd.setScale(1, BigDecimal.ROUND_HALF_DOWN);
		Parameters.Current_FM_Freq = String.valueOf(bd.doubleValue());
		Parameters.Current_FM_Name = "";
		String stm32_freq = String.valueOf(bd.doubleValue()).replace(".","");
		Log.d(LOG_TAG, "STM32_freq = " + stm32_freq);
		STM32_Send("R" + String.valueOf(stm32_freq) + "*");
	}

	public static void SaveCurrentRadioStation(){
		TotalStations++;
		Log.d(LOG_TAG,"########################################");
		Log.d(LOG_TAG,"Current FM station name: " + Parameters.Current_FM_Name);
		Log.d(LOG_TAG,"Current FM station freq: " + Parameters.Current_FM_Freq);
		try {
			HashMap<String,Object> st = new HashMap<String, Object>();
			st.put(RadioStation, Parameters.Current_FM_Name);
			BigDecimal bd = new BigDecimal(Parameters.CurrentFreq);
			bd = bd.setScale(1, BigDecimal.ROUND_HALF_DOWN);
			st.put(RadioFreq, String.valueOf(bd.doubleValue()));
			CarPCService.myStations.add(st);

			for(int i=0; i <=25; i++) {
				CarPCPreferences.removePreference("FM" + i + "_name");
				CarPCPreferences.removePreference("FM" + i + "_freq");
			}
			Log.i(LOG_TAG, "Total Stations -> " + TotalStations);
			for(int i = 0; i  <= TotalStations; i++) {
				HashMap<String, Object> Station = myStations.get(i);
				String Station_Name = Station.get("fm_name").toString();
				String Station_Freq = Station.get("fm_freq").toString();
				Log.d(LOG_TAG,"myStations index: " + i + " [ " + Station_Name + "(" + Station_Freq +") ]");
				CarPCPreferences.savePreference("FM" + i + "_name", Station_Name);
				CarPCPreferences.savePreference("FM" + i + "_freq", Station_Freq);
			}
		}
		catch (NullPointerException e) {
			Log.i(LOG_TAG, "Tried to add null value");
		}
		Parameters.Current_FM_Pos = TotalStations; // go to end of list, to added station
		Log.d(LOG_TAG,"########################################");
	}
	
	public static void RemoveCurrentRadioStation(){
		Log.d(LOG_TAG,"########################################");
		Log.d(LOG_TAG,"Current FM station name: " + Parameters.Current_FM_Name);
		Log.d(LOG_TAG,"Current FM station freq: " + Parameters.Current_FM_Freq);
		myStations.remove(Parameters.Current_FM_Pos);
		Parameters.Current_FM_Pos--;
		TotalStations--;
		Log.d(LOG_TAG,"Position " + Parameters.Current_FM_Pos + "removed");
		for(int i=0; i <=25; i++) {
			CarPCPreferences.removePreference("FM" + i + "_name");
			CarPCPreferences.removePreference("FM" + i + "_freq");
		}
		Log.i(LOG_TAG, "Total Stations -> " + TotalStations);
		for(int i = 0; i  <= TotalStations; i++) {
			HashMap<String, Object> Station = myStations.get(i);
			String Station_Name = Station.get("fm_name").toString();
			String Station_Freq = Station.get("fm_freq").toString();
			Log.d(LOG_TAG,"myStations index: " + i + " [ " + Station_Name + "(" + Station_Freq +") ]");
			CarPCPreferences.savePreference("FM" + i + "_name", Station_Name);
			CarPCPreferences.savePreference("FM" + i + "_freq", Station_Freq);
		}
		SetRadioStation(Parameters.Current_FM_Pos);
		Log.d(LOG_TAG,"########################################");
	}
	
	private void SendBroadcast (int parameter, int value) {
		Intent Broadcast_intent = new Intent(Main.BROADCAST_ACTION);
		Broadcast_intent.putExtra(Main.PARAM, parameter);
        Broadcast_intent.putExtra(Main.VALUE, value);
        sendBroadcast(Broadcast_intent);
	}

	public class ReadThread extends Thread implements Runnable {
		@Override
		public void run() {
			super.run();
			Log.i(LOG_TAG, "ReadThread started");
			int pointer = 0;
			String rx_packet = null;
			byte[] rx_buffer = new byte[1024]; // TODO Strange buffer size on
												// receive
			while (!isInterrupted()) {
				int size;
				try {
					byte[] buffer = new byte[1];
					if (mInputStream == null) {
						Log.d(LOG_TAG, "mInputStream == null");
						return;
					}
					size = mInputStream.read(buffer);
					// Log.d(LOG_TAG, "mInputStream.size = " + size);
					if (size > 0) {
						if (buffer[0] == '*') // terminate packet character
						{
							// rx_buffer[pointer++] = 0x00;
							pointer = 0;
							STM32_PACKET = false;
							// Log.d(LOG_TAG,
							// "Give '*', STM32_PACKET =  false, pointer = " +
							// pointer);
							rx_packet = new String(rx_buffer);
							ParseSTM32Packet(rx_packet);
						}

						// Log.i(LOG_TAG,"Rcv buffer = " + toHex(buffer) +
						// ", size = " + size);
//						char symbol = (char) buffer[0];
						if (STM32_PACKET) {
							rx_buffer[pointer] = buffer[0];
							pointer++;
						}

						if (buffer[0] == '+') {
							STM32_ACK = true;
							// Log.i(LOG_TAG, "STM32 ACK recieved");
							pointer = 0;
						}
						if (buffer[0] == '@') {
							// Log.i(LOG_TAG, "STM32 packet detected!");
							STM32_PACKET = true;
							pointer = 0;
						}
						if (buffer[0] == '$') {
							STM32_PACKET_OK = true;
							// Log.i(LOG_TAG,
							// "STM32 get whole packet, sending done");
							pointer = 0;
						}
						if (buffer[0] != '+') { // not ACK?
							if (buffer[0] != '$') { // not END_OF_PACKET?
								if (STM32_PACKET == false) { // packet not from
																// STM32?
																// Log.d(LOG_TAG,
									// "Getting data from STM32, buffer[0] = "
									// + buffer[0] + " ( "
									// + symbol + " )");
									rx_buffer[pointer] = buffer[0];
									pointer++;
								}
							}
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
					return;
				}
			}
		}

		public void ShowMain() {
			Intent Main_intent = new Intent();
			Main_intent.setComponent(new ComponentName("info.ripz.carpc",
					"info.ripz.carpc.Main"));
			Main_intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(Main_intent);
			Log.d(LOG_TAG, "Started Main activity");
		}
		
		private void ShowNavitel() {
			Intent Navitel_intent = new Intent();
			Navitel_intent.setComponent(new ComponentName("com.navitel",
					"com.navitel.Navitel"));
			Navitel_intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(Navitel_intent);
			Log.d(LOG_TAG, "Started Navitel activity");
		}

		private void ShowYandexMaps() {
			Intent YandexMaps_intent = new Intent();
			YandexMaps_intent.setComponent(new ComponentName(
					"ru.yandex.yandexmaps",
					"ru.yandex.yandexmaps.MapActivity"));
			YandexMaps_intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(YandexMaps_intent);
			Log.d(LOG_TAG, "Started Yandex Navigator activity");
		}
		
		public String toHex(byte[] buffer) {
			return String.format("%01x", new BigInteger(buffer.length, buffer));
		}

		private void ParseSTM32Packet(final String STM32_packet) {

			new Thread(new Runnable() {
				private String packet = STM32_packet;

				public void run() {
					packet = packet.replace("*", "");
					// Log.i(LOG_TAG, "STM32 packet is: " + packet);
					switch (packet.charAt(0)) {
					case 'k':
						// Log.d(LOG_TAG, "Keyboard parameter found");
						switch (packet.charAt(1)) {
						case '0': // MODE
							Parameters.mode++;
							if (Parameters.mode > 2)
								Parameters.mode = 0;
							if (Parameters.cold_start) {
								Parameters.mode = 1;
								Parameters.cold_start = false;
							}
							 Log.d(LOG_TAG, "**************** MODE = "
							 + Parameters.mode);
							switch (Parameters.mode) {
							case 0:
								Log.d(LOG_TAG, "Sony RM-X6S key: MODE");
								if (Parameters.poweramp_mode) {
									Log.i(LOG_TAG, "MODE >>>>> PowerAMP");
									ShowMain();
									STM32_Send("S2*"); // Source = 1 (Tablet)
									STM32_Send("V" + Parameters.volume + "*");
									SendBroadcast(Parameters.ACTIVE_SOURCE, 1);
								} else {
									Log.i(LOG_TAG, "MODE >>>>> FM Radio");
									ShowMain();
									STM32_Send("S3*"); // Source = 2 (FM Radio)
									STM32_Send("V" + Parameters.volume + "*");
									SendBroadcast(Parameters.ACTIVE_SOURCE, 2);
								}
								break;
							case 1:
								Log.i(LOG_TAG, "MODE >>>>> Navitel");
								ShowNavitel();
								Parameters.cold_start = false;
								break;
							case 2:
								Log.i(LOG_TAG, "MODE >>>>> Yandex Maps");
								ShowYandexMaps();
								break;
							default:
								break;
							}
							break;
						case '1': // OFF
							Log.d(LOG_TAG, "Sony RM-X6S key: OFF");
							break;
						case '2': // ATT
							Log.d(LOG_TAG, "Sony RM-X6S key: ATT");
							if (!Parameters.poweramp_mode) // PowerAMP
							{
								Log.d(LOG_TAG,
										"PowerAMP mode: PLAY, FM radio: OFF");
								if (!Parameters.poweramp_playing) { // RESUME
																	// PowerAMP
																	// if in
																	// PAUSE
									startService(new Intent(
											PowerAMPiAPI.ACTION_API_COMMAND)
											.putExtra(
													PowerAMPiAPI.COMMAND,
													PowerAMPiAPI.Commands.RESUME));
								}
								Parameters.poweramp_mode = true;
								STM32_Send("S2*"); // Source = 1 (Tablet)
								STM32_Send("V" + Parameters.volume + "*");
								SendBroadcast(Parameters.ACTIVE_SOURCE, 1);
								// if (cold_start) mode = 1; // Navitel mode, if
								// not in cold start
								//Parameters.mode = 0; // next press of MODE, we
								//						// in Navitel mode
							} else {
								Log.d(LOG_TAG,
										"FM Radio: ON, PowerAMP mode: PAUSE");
								if (Parameters.poweramp_playing) { // PAUSE
																	// PowerAMP
																	// if in
																	// PLAY
									startService(new Intent(
											PowerAMPiAPI.ACTION_API_COMMAND)
											.putExtra(PowerAMPiAPI.COMMAND,
													PowerAMPiAPI.Commands.PAUSE));
								}
								SetRadioStation(Parameters.Current_FM_Pos);
								Parameters.poweramp_mode = false;
								STM32_Send("S3*"); // Source = 2 (FM radio)
								STM32_Send("V" + Parameters.volume + "*");
								SendBroadcast(Parameters.ACTIVE_SOURCE, 2);
								// if (cold_start) mode = 1; // Navitel mode, if
								// not in cold start
								// Parameters.mode = 0; // next press of MODE, we
								//						// in Navitel mode
							}
							break;
						case '3': // PUSH
							Log.d(LOG_TAG, "Sony RM-X6S key: PUSH");
							if (Parameters.poweramp_mode) {
									startService(new Intent(
											PowerAMPiAPI.ACTION_API_COMMAND)
											.putExtra(PowerAMPiAPI.COMMAND,
													PowerAMPiAPI.Commands.TOGGLE_PLAY_PAUSE));
								if (!Parameters.bluetooth_mode) {
									Log.d(LOG_TAG, "*********** 1 ***********");
									STM32_Send("S1*"); // Source = 1 (Bluetooth)
									STM32_Send("V" + Parameters.volume + "*");
									Parameters.bluetooth_mode = true;
								} else {
									Log.d(LOG_TAG, "*********** 2 ***********");
									STM32_Send("S2*"); // Source = 1 (Tablet)
									STM32_Send("V" + Parameters.volume + "*");
									Parameters.bluetooth_mode = false;
								}
							}

							else {
								if (!Parameters.bluetooth_mode) {
									Log.d(LOG_TAG, "*********** 3 ***********");
									STM32_Send("S1*"); // Source = 0 (Bluetooth)
									STM32_Send("V" + Parameters.volume + "*");
									Parameters.bluetooth_mode = true;
								} else {
									Log.d(LOG_TAG, "*********** 4 ***********");
									STM32_Send("S3*"); // Source = 2 (FM Radio)
									STM32_Send("V" + Parameters.volume + "*");
									Parameters.bluetooth_mode = false;
								}
							}
							break;
						case '4': // VOL+
							Parameters.volume++;
							Log.d(LOG_TAG, "Sony RM-X6S key: VOL+");
							if (Parameters.volume > 31) {
								Parameters.volume = 31;
							}
							Log.d(LOG_TAG, "Volume = " + Parameters.volume);
							STM32_Send("V" + Parameters.volume + "*");
							break;
						case '5': // VOL-
							Parameters.volume--;
							Log.d(LOG_TAG, "Sony RM-X6S key: VOL-");
							if (Parameters.volume <= 0) {
								Parameters.volume = 0;
							}
							Log.d(LOG_TAG, "Volume = " + Parameters.volume);
							STM32_Send("V" + Parameters.volume + "*");
							break;
						case '6': // PREV
							if (Parameters.poweramp_mode) {
								Log.d(LOG_TAG,
										"Sony RM-X6S key: PREV ( mode = "
												+ Parameters.mode
												+ ", poweramp_mode = "
												+ Parameters.poweramp_mode
												+ " )");
								startService(new Intent(
										PowerAMPiAPI.ACTION_API_COMMAND)
										.putExtra(PowerAMPiAPI.COMMAND,
												PowerAMPiAPI.Commands.PREVIOUS));
							}
							else {
								PrevRadioStation();
								SendBroadcast(Parameters.UPDATE_FM, 0);
							}
							break;
						case '7': // NEXT
							if (Parameters.poweramp_mode) {
								Log.d(LOG_TAG,
										"Sony RM-X6S key: NEXT ( mode = "
												+ Parameters.mode
												+ ", poweramp_mode = "
												+ Parameters.poweramp_mode
												+ " )");
								startService(new Intent(
										PowerAMPiAPI.ACTION_API_COMMAND)
										.putExtra(PowerAMPiAPI.COMMAND,
												PowerAMPiAPI.Commands.NEXT));
							}
							else {
								NextRadioStation();
								SendBroadcast(Parameters.UPDATE_FM, 0);
							}
							break;
						case '8': // DOWN
							if (Parameters.poweramp_mode)
								startService(new Intent(
										PowerAMPiAPI.ACTION_API_COMMAND)
										.putExtra(
												PowerAMPiAPI.COMMAND,
												PowerAMPiAPI.Commands.NEXT_IN_CAT));
							break;
						case '9': // UP
							if (Parameters.poweramp_mode)
								startService(new Intent(
										PowerAMPiAPI.ACTION_API_COMMAND)
										.putExtra(
												PowerAMPiAPI.COMMAND,
												PowerAMPiAPI.Commands.PREVIOUS_IN_CAT));
							break;
						default:
							break;
						}
						break;
					default:
						break;
					}
					Log.d(LOG_TAG, "Exit from ParseSTM32Packet()");
				}
			}).start();
		} // end of ParseSTM32Packet
/*
		private void SendKey(int i) {
			SerialPort.KeyPress(1, i, 1); // key press
			SerialPort.KeyPress(0, 0, 0); //
			SerialPort.KeyPress(1, i, 0); // key release
			SerialPort.KeyPress(0, 0, 0); //
		}
*/
	} // end of ReadThread

	public void onDestroy() {
		if (mReadThread != null)
			mReadThread.interrupt();
		closeSerialPort();
		mSerialPort = null;
		Log.d(LOG_TAG, "onDestroy");
		super.onDestroy();
	}

	public static void STM32_Send(final String packet) {
		 Log.d(LOG_TAG,
		 "STM32_Send::CarPC (" + packet + "), len = " + packet.length()
		 + " ->> STM32");
		for (int i = 0; i < packet.length(); i++) {
			// if (mSerialPort != null) {
			try {
//				 Log.d(LOG_TAG, "STM32_Send:: " + packet.charAt(i) + "[" + i
//				 + "]");
				mOutputStream.write(packet.charAt(i));
			} catch (IOException e) {
				e.printStackTrace();
			}
//			Log.d(LOG_TAG, "ReadThread state is = " +
//			mReadThread.getState());
			while (!STM32_ACK) { // waiting for ACK
//				Log.i(LOG_TAG,"STM32 waiting for ACK");
			}
//			Log.i(LOG_TAG,"STM32 ->> ACK");
			STM32_ACK = false;
		}
		// }
		while (!STM32_PACKET_OK) { // waiting for STM32 getting full packet
			// Log.i(LOG_TAG,"STM32 waiting for PACKET_OK");
		}
		STM32_PACKET_OK = false;
	}

	public SerialPort getSerialPort() throws SecurityException, IOException,
			InvalidParameterException {
		if (mSerialPort == null) {
			// String path = CarPCPreferences.getPreference("STM32_DEVICE");
			// int baudrate =
			// Integer.decode(CarPCPreferences.getPreference("STM32_BAUDRATE"));
			String path = "/dev/ttyUSB1";
			int baudrate = 9600;

			if ((path.length() == 0) || (baudrate == -1)) {
				throw new InvalidParameterException();
			}
			mSerialPort = new SerialPort(new File(path), baudrate, 0);
		}
		return mSerialPort;
	}

	public void closeSerialPort() {
		if (mSerialPort != null) {
			Log.i(LOG_TAG, "mSerialPort.close()");
			mSerialPort.close();
			mSerialPort = null;
		}
	}
}

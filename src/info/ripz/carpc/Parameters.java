/**
 * 
 */
package info.ripz.carpc;

/**
 * @author RipZ
 *
 */
public final class Parameters {

	static String PrefsVolume;
	static String PrefsBalance;
	static String PrefsFad;
	static String PrefsBass;
	static String PrefsTreble;
	
	static int Current_FM_Pos;
	static String Current_FM_Freq;
	static String Current_FM_Name;
	static Float CurrentFreq = 100.5f;

	static int RPM = 3150;
	static int COOLER_TEMP = 94;
	static int OUTDOOR_TEMP = 5;
	
	static boolean cold_start = true;
	static boolean poweramp_mode = true;
	static boolean bluetooth_mode = false;
	static boolean poweramp_playing = false;
	static String poweramp_title = null;
	static String poweramp_artist = null;
	static String poweramp_album = null;	
	static int poweramp_duration = 0;
	static int mode = 0;
	static int volume;

	final static int ACTIVE_SOURCE = 1;
	final static int UPDATE_FM = 2;
	final static int UPDATE_CAR_PARAMETERS = 2;
	
	}

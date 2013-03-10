package info.ripz.carpc;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class CarPCPreferences {
     private static final String APP_SHARED_PREFS = "settings"; //  Name of the file -.xml
     private static SharedPreferences appSharedPrefs;
     private static Editor prefsEditor;

     public CarPCPreferences(Context context)
     {
         appSharedPrefs = context.getSharedPreferences(APP_SHARED_PREFS, Activity.MODE_PRIVATE);
         prefsEditor = appSharedPrefs.edit();
     }

     public static String getPreference(String Key) {
         return appSharedPrefs.getString(Key, null);
     }

     public static void savePreference(String key, String value) {
         prefsEditor.putString(key, value);
         prefsEditor.commit();
     }

     public static void removePreference(String key) {
         prefsEditor.remove(key);
         prefsEditor.commit();
     }
}
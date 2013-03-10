package info.ripz.carpc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootBroadCastReceiver extends BroadcastReceiver {

	final String LOG_TAG = "CarPC_Service";
	@Override
	public void onReceive(Context context, Intent intent) {
		Log.i(LOG_TAG, "BootBroadCastReceiver::onReceive " + intent.getAction());
	    context.startService(new Intent(context, CarPCService.class));	}
}

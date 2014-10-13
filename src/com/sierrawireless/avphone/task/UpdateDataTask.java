package com.sierrawireless.avphone.task;

import java.io.IOException;

import net.airvantage.model.Application;
import net.airvantage.utils.AirVantageClient;
import android.util.Log;

import com.sierrawireless.avphone.MainActivity;
import com.sierrawireless.avphone.model.CustomData;

public class UpdateDataTask extends RegisterSystemTask {

	public UpdateDataTask(AirVantageClient client, String serialNumber, CustomData customData) {
		super(client, serialNumber, customData);
	}

	@Override
	protected Boolean doInBackground(String... params) {

		try {
			Application application = ensureApplicationExists();

			setApplicationData(application.uid);

			return true;
		} catch (IOException e) {
			Log.e(MainActivity.class.getName(), "Error when trying to updat application data", e);
			return false;
		}

	}

}

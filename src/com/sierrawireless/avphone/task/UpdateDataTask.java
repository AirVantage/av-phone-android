package com.sierrawireless.avphone.task;

import java.io.IOException;

import net.airvantage.model.AirVantageException;
import net.airvantage.model.Application;
import net.airvantage.model.AvError;
import android.os.AsyncTask;
import android.util.Log;

import com.sierrawireless.avphone.MainActivity;
import com.sierrawireless.avphone.model.CustomData;

public class UpdateDataTask extends AsyncTask<Object, Void, AvError> {

	private IApplicationClient appClient;

	public UpdateDataTask(IApplicationClient appClient) {
		this.appClient = appClient;
	}

	@Override
	protected AvError doInBackground(Object... params) {

		try {
			
			String serialNumber = (String) params[0];
			CustomData customData = (CustomData) params[1];
			
			Application application = appClient.ensureApplicationExists(serialNumber);

			appClient.setApplicationData(application.uid, customData);

			return null;
		} catch (AirVantageException e) {
			return e.getError();
		} catch (IOException e) {
			Log.e(MainActivity.class.getName(), "Error when trying to update application data", e);
			return new AvError("unknown.error");
		}

	}

}

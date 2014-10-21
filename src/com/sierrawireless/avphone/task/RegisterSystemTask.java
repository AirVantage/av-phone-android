package com.sierrawireless.avphone.task;

import java.io.IOException;

import net.airvantage.model.AirVantageException;
import net.airvantage.model.Application;
import net.airvantage.model.AvError;
import android.os.AsyncTask;
import android.util.Log;

import com.sierrawireless.avphone.MainActivity;
import com.sierrawireless.avphone.model.CustomData;

public class RegisterSystemTask extends AsyncTask<Object, Integer, AvError> {

	private IApplicationClient applicationClient;

	private ISystemClient systemClient;

	public RegisterSystemTask(IApplicationClient applicationClient, ISystemClient systemClient) {
		this.applicationClient = applicationClient;
		this.systemClient = systemClient;
	}

	@Override
	protected AvError doInBackground(Object... params) {

		try {

			String serialNumber = (String) params[0];
			String mqttPassword = (String) params[1];
			CustomData customData = (CustomData) params[2];
			
			Application application = this.applicationClient.ensureApplicationExists(serialNumber);
			
			net.airvantage.model.System system = this.systemClient.getSystem(serialNumber);
			if (system == null) {
				system = systemClient.createSystem(serialNumber, mqttPassword, application.uid);
			}

			this.applicationClient.setApplicationData(application.uid, customData);

			return null;
		} catch (AirVantageException e) {
			return e.getError();
		} catch (IOException e) {
			Log.e(MainActivity.class.getName(), "Error when trying to get current user", e);
			return new AvError("unkown.error");
		}

	}


}

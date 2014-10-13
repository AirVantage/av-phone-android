package com.sierrawireless.avphone.task;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import net.airvantage.model.Application;
import net.airvantage.model.ApplicationData;
import net.airvantage.model.Protocol;
import net.airvantage.utils.AirVantageClient;
import net.airvantage.utils.Utils;
import android.os.AsyncTask;
import android.util.Log;

import com.sierrawireless.avphone.MainActivity;
import com.sierrawireless.avphone.model.AvPhoneApplication;
import com.sierrawireless.avphone.model.CustomData;

public class RegisterSystemTask extends AsyncTask<String, Void, Boolean> {

	private static final String DEMO_APP_TYPE = "av_phone_demo_app";

	private AirVantageClient client = null;
	private String serialNumber = null;

	private CustomData customData;

	public RegisterSystemTask(AirVantageClient client, String serialNumber, CustomData customData) {
		this.client = client;
		this.serialNumber = serialNumber;
		this.customData = customData;
	}

	@Override
	protected Boolean doInBackground(String... params) {

		try {

			Application application = ensureApplicationExists();
		
			net.airvantage.model.System system = getSystem();
			if (system == null) {
				system = createSystem(application.uid);
			}

			setApplicationData(application.uid);
			
			return true;
		} catch (IOException e) {
			Log.e(MainActivity.class.getName(), "Error when trying to get current user", e);
			return false;
		}

	}

	// TODO(pht) move this somewhere else to avoid inheritance between tasks... ?
	protected Application ensureApplicationExists() throws IOException {
		Application application = getApplication();
		if (application == null) {
			application = createApplication();
			setApplicationCommunication(application.uid);
		}
		return application;
	}

	// TODO(pht) move this somewhere else to avoid inheritance between tasks... ?
	
	protected void setApplicationData (String applicationUid) throws IOException {
		ApplicationData data = AvPhoneApplication.createApplicationData(customData);
		client.setApplicationData(applicationUid, data);
	}
	
	private String applicationName() {
		return "av_phone_" + serialNumber;
	}

	
	private net.airvantage.model.System getSystem() throws IOException {
		List<net.airvantage.model.System> systems = client.getSystems(serialNumber);
		// TODO(pht) technically, we should check the first exact match, and
		// nothing else
		return Utils.first(systems);
	}

	private Application getApplication() throws IOException {
		List<Application> applications = client.getApplications(applicationName(), DEMO_APP_TYPE);
		// TODO(pht) technically, we should check the first exact match, and
		// nothing else
		return Utils.first(applications);
	}

	private Application createApplication() throws IOException {
		Application application = new Application();
		application.name = applicationName();
		application.type = DEMO_APP_TYPE;
		application.revision = "0.0.0";
		return client.createApp(application);
	}

	private net.airvantage.model.System  createSystem(String applicationUid) throws IOException {
		net.airvantage.model.System system = new net.airvantage.model.System();
		net.airvantage.model.System.Gateway gateway = new net.airvantage.model.System.Gateway();
		gateway.serialNumber = serialNumber;
		system.gateway = gateway;
		system.state = "READY";
		Application application = new Application();
		application.uid = applicationUid;
		system.applications = Arrays.asList(application);
		return client.createSystem(system);
	}

	private void setApplicationCommunication(String applicationUid) throws IOException {
		List<Protocol> protocols = AvPhoneApplication.createProtocols();
		client.setApplicationCommunication(applicationUid, protocols);
	}
	
}

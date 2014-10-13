package com.sierrawireless.avphone;

import net.airvantage.utils.AirVantageClient;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.sierrawireless.avphone.model.CustomData;
import com.sierrawireless.avphone.task.RegisterSystemTask;
import com.sierrawireless.avphone.task.UpdateDataTask;

public class ConfigureFragment extends Fragment implements OnSharedPreferenceChangeListener {

	public static final String PHONE_UNIQUE_ID = Build.SERIAL;

	private static final int CONTEXT_REGISTER = 0;
	private static final int CONTEXT_UPDATE_DATA = 1;

	private Button registerBt;

	private SharedPreferences prefs;

	private EditText customData1EditText;
	private EditText customData2EditText;
	private EditText customData3EditText;
	
	private View updateDataBt;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		View view = inflater.inflate(R.layout.fragment_configure, container, false);

		// phone identifier
		((TextView) view.findViewById(R.id.phoneid_value)).setText(PHONE_UNIQUE_ID);

		// Register button
		registerBt = (Button) view.findViewById(R.id.register_bt);
		registerBt.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				onRegisterClicked();
			}
		});

		updateDataBt = (Button) view.findViewById(R.id.update_data_bt);
		updateDataBt.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				onUpdateDataClicked();
			}
		});
		updateDataBt.setEnabled(false);

		
		// Preferences
		PreferenceManager.setDefaultValues(getActivity(), R.xml.preferences, false);
		prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
		prefs.registerOnSharedPreferenceChangeListener(this);

		// Fields for custom data
		customData1EditText = buildCustomLabelEditText(view, 
				R.id.custom1_value, 
				R.string.pref_custom1_label_key, 
				R.string.pref_custom1_label_default );
		customData2EditText = buildCustomLabelEditText(view, 
				R.id.custom2_value, 
				R.string.pref_custom2_label_key, 
				R.string.pref_custom2_label_default );
		customData3EditText = buildCustomLabelEditText(view, 
				R.id.custom3_value, 
				R.string.pref_custom3_label_key, 
				R.string.pref_custom3_label_default );

		
		return view;
	}

	private EditText buildCustomLabelEditText(View view, int id, final int prefKey, int labelDefaultKey) {
		EditText res = (EditText) view.findViewById(id);

		res.setText(prefs.getString(this.getString(prefKey),
				this.getString(labelDefaultKey)));
		
		res.addTextChangedListener(new TextWatcher() {
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}
			
			@Override
			public void afterTextChanged(Editable s) {
				prefs.edit().putString(getString(prefKey), s.toString());
				updateDataBt.setEnabled(true);
			}
		});
		return res;
	}
	
	private String serverHost() {
		return prefs.getString(this.getString(R.string.pref_server_key), null);
	}

	protected void onRegisterClicked() {
		Intent intent = new Intent(getActivity(), AuthorizationActivity.class);
		intent.putExtra(AuthorizationActivity.AUTHORIZATION_CONTEXT, CONTEXT_REGISTER);
		startActivityForResult(intent, AuthorizationActivity.REQUEST_AUTHORIZATION);
	}

	private void onUpdateDataClicked() {
		Intent intent = new Intent(getActivity(), AuthorizationActivity.class);
		intent.putExtra(AuthorizationActivity.AUTHORIZATION_CONTEXT, CONTEXT_UPDATE_DATA);
		startActivityForResult(intent, AuthorizationActivity.REQUEST_AUTHORIZATION);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
		case (AuthorizationActivity.REQUEST_AUTHORIZATION): {
			if (resultCode == Activity.RESULT_OK) {
				String token = data.getStringExtra(AuthorizationActivity.TOKEN);

				int request = data.getExtras().getInt(AuthorizationActivity.AUTHORIZATION_CONTEXT);
				if (request == ConfigureFragment.CONTEXT_REGISTER) {
					registerSystem(token);
				} else if (request == ConfigureFragment.CONTEXT_UPDATE_DATA) {
					updateData(token);
				}

			}
			break;
		}
		}
	}

	private void toast(Exception e, String label, String message) {
		Log.e(MainActivity.class.getName(), label, e);
		Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
	}

	private void registerSystem(String token) {

		AirVantageClient client = new AirVantageClient(serverHost(), token);

		AsyncTask<String, Void, Boolean> registerTask = new RegisterSystemTask(client, PHONE_UNIQUE_ID, getCustomData());

		registerTask.execute(token);
		try {
			if (registerTask.get()) {
				Toast.makeText(getActivity(), "System registered on AirVantage.", Toast.LENGTH_SHORT).show();
			} else {
				Toast.makeText(getActivity(), "An error occured when registering system.", Toast.LENGTH_SHORT).show();
			}
		} catch (Exception e) {
			toast(e, "Error", "An error occured when registering system.");
		}
	}

	private void updateData(String token) {
		AirVantageClient client = new AirVantageClient(serverHost(), token);

		AsyncTask<String, Void, Boolean> updateDataTask = new UpdateDataTask(client, PHONE_UNIQUE_ID,
				getCustomData());

		updateDataTask.execute(token);
		try {
			if (updateDataTask.get()) {
				Toast.makeText(getActivity(), "Data updated on AirVantage.", Toast.LENGTH_SHORT).show();
				updateDataBt.setEnabled(false);
			} else {
				Toast.makeText(getActivity(), "An error occured when updating data.", Toast.LENGTH_SHORT).show();
			}
		} catch (Exception e) {
			toast(e, "Error", "An error occured when updating data.");
		}
	}

	protected CustomData getCustomData() {
		CustomData customData = new CustomData();
		customData.custom1 = customData1EditText.getText().toString();
		customData.custom2 = customData2EditText.getText().toString();
		customData.custom3 = customData3EditText.getText().toString();
		return customData;
	}

	// Preferences

	@Override
	public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
		// TODO
	}

}

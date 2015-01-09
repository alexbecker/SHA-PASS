package com.alexbecker.sha_pass;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class Generator extends ActionBarActivity {

	private static final int minLength = 6;

	private SharedPreferences sharedPref;
	private String masterPasswordHash;
	private ArrayList<Service> services;

	private EditText masterPasswordEditText;
	private EditText serviceEditText;
	private Spinner serviceSpinner;
	private EditText lengthEditText;
	private CheckBox lowercaseCheckbox;
	private CheckBox uppercaseCheckbox;
	private CheckBox numberCheckbox;
	private CheckBox symbolCheckbox;
	private TextView password;

	private boolean usedSpinner;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_generator);

		this.masterPasswordEditText = (EditText) findViewById(R.id.master_password);
		this.serviceEditText = (EditText) findViewById(R.id.service_name_edittext);
		this.serviceSpinner = (Spinner) findViewById(R.id.service_name_spinner);
		this.lengthEditText = (EditText) findViewById(R.id.length);
		this.lowercaseCheckbox = (CheckBox) findViewById(R.id.lowercase_checkbox);
		this.uppercaseCheckbox = (CheckBox) findViewById(R.id.uppercase_checkbox);
		this.numberCheckbox = (CheckBox) findViewById(R.id.number_checkbox);
		this.symbolCheckbox = (CheckBox) findViewById(R.id.symbol_checkbox);
		this.password = (TextView) findViewById(R.id.password);

		this.lengthEditText.setHint(String.format("%d+", minLength));

		this.sharedPref = Generator.this.getPreferences(Context.MODE_PRIVATE);

		this.masterPasswordHash = this.sharedPref.getString("masterPasswordHash", null);

		Set<String> servicesPackedSet = this.sharedPref.getStringSet("services", new LinkedHashSet<String>());
		ArrayList<String> servicesPacked = new ArrayList<String>(servicesPackedSet);
		Collections.sort(servicesPacked);
		this.services = new ArrayList<Service>(servicesPacked.size());
		String[] serviceNames = new String[servicesPacked.size() + 1];
		serviceNames[0] = "";
		int i = 1;
		for (String s : servicesPacked) {
			Service service = new Service(s);
			this.services.add(service);
			serviceNames[i++] = service.name;
		}

		ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item, serviceNames);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		this.serviceSpinner.setAdapter(adapter);

		final Generator g = this;
		this.serviceSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				if (position > 0) {
					Service service = g.services.get(position - 1);

					g.lengthEditText.setText(String.format("%d", service.length));
					g.lowercaseCheckbox.setChecked(service.lowercase);
					g.uppercaseCheckbox.setChecked(service.uppercase);
					g.numberCheckbox.setChecked(service.number);
					g.symbolCheckbox.setChecked(service.symbol);

					g.usedSpinner = true;
					
					// grey out edittext
					g.serviceEditText.setText("");
					g.serviceEditText.setEnabled(false);
				} else {
					g.usedSpinner = false;
					g.serviceEditText.setEnabled(true);
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				// TODO Auto-generated method stub

			}

		});

		this.usedSpinner = false;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.generator, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		
		if (id == R.id.action_reset_master_password) {
			this.masterPasswordHash = null;
			return true;
		}
		
		if (id == R.id.action_clear_selected_service) {
			int position = this.serviceSpinner.getSelectedItemPosition();
			
			// check if a service is selected
			if (position == 0) {
				Toast toast = Toast.makeText(Generator.this, "No service selected.", Toast.LENGTH_SHORT);
				toast.show();
				return true;
			}
				
			this.services.remove(position - 1);

			// save service values
			SharedPreferences.Editor editor = this.sharedPref.edit();
			LinkedHashSet<String> serviceSet = new LinkedHashSet<String>();
			for (Service newService : this.services) {
				serviceSet.add(newService.pack());
			}
			editor.putStringSet("services", serviceSet);
			editor.apply();
			
			reset();
			
			return true;
		}
		
		if (id == R.id.action_clear_all_services) {
			// clear current list of services
			this.services = new ArrayList<Service>();

			// delete saved list
			SharedPreferences.Editor editor = this.sharedPref.edit();
			editor.putStringSet("services", new LinkedHashSet<String>());
			editor.apply();

			reset();
			
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	public void generate(View view) {
		SharedPreferences.Editor editor = this.sharedPref.edit();

		// check master password
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("SHA-256");
			byte[] mdbytes = md.digest(this.masterPasswordEditText.getText().toString().getBytes());
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < mdbytes.length; i++) {
				sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
			}
			String newMasterPasswordHash = sb.toString();

			if (this.masterPasswordHash == null) {
				this.masterPasswordHash = newMasterPasswordHash;
				
				// store the master password hash
				editor.putString("masterPasswordHash", newMasterPasswordHash);
				Log.i("SHA-PASS", "saving master password hash");
			} else if (!this.masterPasswordHash.equals(newMasterPasswordHash)) {
				Toast toast = Toast.makeText(Generator.this, "Wrong master password.\nUse action bar to reset.", Toast.LENGTH_LONG);
				toast.show();
				return;
			}
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// get service values
		final Service service = new Service();
		if (this.usedSpinner) {
			service.name = this.serviceSpinner.getSelectedItem().toString();
		} else {
			service.name = this.serviceEditText.getText().toString();
		}
		try {
		service.length = Integer.parseInt(this.lengthEditText.getText().toString());
		} catch (NumberFormatException e) {
			Toast toast = Toast.makeText(Generator.this, "Enter a length.", Toast.LENGTH_SHORT);
			toast.show();
			return;
		}
		service.lowercase = this.lowercaseCheckbox.isChecked();
		service.uppercase = this.uppercaseCheckbox.isChecked();
		service.number = this.numberCheckbox.isChecked();
		service.symbol = this.symbolCheckbox.isChecked();

		// check length
		if (service.length < minLength) {
			Toast toast = Toast.makeText(Generator.this, String.format("Length must be at least %d.", minLength), Toast.LENGTH_LONG);
			toast.show();
			return;
		} else if (service.length > 64) {
			Toast toast = Toast.makeText(Generator.this, "Length must be at most 64.", Toast.LENGTH_LONG);
			toast.show();
			return;
		}
		
		// add service to list of known
		if (this.services.size() == 0) {
			this.services.add(service);

			// print result
			this.password.setText(service.generate(this.masterPasswordEditText.getText().toString()));
		} else {
			boolean done = false;
			int i = 0;
			for (final Service oldService : this.services) {
				int comparison = service.name.compareTo(oldService.name);
				if (comparison == 0) {
					// check agreement with old service
					if (!service.equals(oldService)) {
						AlertDialog.Builder builder = new AlertDialog.Builder(Generator.this);
						final Generator g = this;
						final int index = i;

						builder.setTitle("Service Mismatch");

						// set dialog message
						builder
						.setMessage("Another service with the same name is known, but with different preferences.")
						.setCancelable(false)
						.setPositiveButton("Overwrite", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								g.services.remove(index);
								g.services.add(index, service);

								// print result
								g.password.setText(service.generate(g.masterPasswordEditText.getText().toString()));
							}
						})
						.setNegativeButton("Use saved", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								service.copyVals(oldService);
								dialog.cancel();

								// print result
								g.password.setText(service.generate(g.masterPasswordEditText.getText().toString()));
							}
						});

						AlertDialog alertDialog = builder.create();
						alertDialog.show();
						done = true;
						break;
					} else if (comparison > 0) {
						int index = this.services.indexOf(oldService);
						this.services.add(index, service);

						// print result
						this.password.setText(service.generate(this.masterPasswordEditText.getText().toString()));
						done = true;
						break;
					}
				}
				i++;
			}

			if (!done) {
				this.services.add(service);

				// print result
				this.password.setText(service.generate(this.masterPasswordEditText.getText().toString()));
			}

		}

		// save service values
		LinkedHashSet<String> serviceSet = new LinkedHashSet<String>();
		for (Service newService : this.services) {
			serviceSet.add(newService.pack());
		}
		editor.putStringSet("services", serviceSet);
		editor.apply();

		// reset
		reset();
	}

	private void reset() {
		this.lengthEditText.setText("");
		this.serviceEditText.setText("");
		this.serviceSpinner.setSelection(0);
		this.lengthEditText.setText("");
		this.lowercaseCheckbox.setChecked(false);
		this.uppercaseCheckbox.setChecked(false);
		this.numberCheckbox.setChecked(false);
		this.symbolCheckbox.setChecked(false);

		this.usedSpinner = false;
		this.serviceEditText.setEnabled(true);

		Set<String> servicesPackedSet = this.sharedPref.getStringSet("services", new LinkedHashSet<String>());
		ArrayList<String> servicesPacked = new ArrayList<String>(servicesPackedSet);
		Collections.sort(servicesPacked);
		this.services = new ArrayList<Service>(servicesPacked.size());
		String[] serviceNames = new String[servicesPacked.size() + 1];
		serviceNames[0] = "";
		int i = 1;
		for (String s : servicesPacked) {
			Service service = new Service(s);
			this.services.add(service);
			serviceNames[i++] = service.name;
		}

		ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item, serviceNames);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		this.serviceSpinner.setAdapter(adapter);
	}
}

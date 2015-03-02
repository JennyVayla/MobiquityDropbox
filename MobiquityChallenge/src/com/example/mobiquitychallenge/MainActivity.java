package com.example.mobiquitychallenge;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.location.Location;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.example.Utils.GlobalApplication;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationServices;

public class MainActivity extends Activity implements OnClickListener,
		OnItemClickListener, ConnectionCallbacks, OnConnectionFailedListener {

	private static final String TAG = "Mobiquity Challenge - MainActivity";
	final static private String APP_KEY = "fms289k63kr5zzg";
	final static private String APP_SECRET = "3mbxkwdbu7dtdky";

	private static final String ACCOUNT_PREFS_NAME = "prefs";
	private static final String ACCESS_KEY_NAME = "ACCESS_KEY";
	private static final String ACCESS_SECRET_NAME = "ACCESS_SECRET";

	// User variables
	private static final int TAKE_PHOTO = 1;
	private final String PHOTO_DIR = "/Photos/";
	DropboxAPI<AndroidAuthSession> mApi;
	Boolean mLoggedIn;
	Context context;
	String mCameraFileName, mCity;
	Double mLatitude, mLongitude;
	ArrayAdapter<String> adapter;
	Location mLastLocation;
	GoogleApiClient mGoogleApiClient;

	// Controls
	Button m_btnAuthenticate, m_btnTakePhoto, m_btnViewPhoto, m_btnRecordSound,
			m_btnCreateText;
	ListView m_listPhotos;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		context = MainActivity.this;

		AndroidAuthSession session = buildSession();
		mApi = new DropboxAPI<AndroidAuthSession>(session);
		GlobalApplication.mApi = mApi;

		buildGoogleApiClient();

		InitalizeControls();
	}

	// create instance of Google Api Client
	protected synchronized void buildGoogleApiClient() {
		mGoogleApiClient = new GoogleApiClient.Builder(this)
				.addConnectionCallbacks(this)
				.addOnConnectionFailedListener(this)
				.addApi(LocationServices.API).build();

		mGoogleApiClient.connect();
	}

	// initialize all the controls
	public void InitalizeControls() {
		m_btnAuthenticate = (Button) findViewById(R.id.btnAuthenticate);
		m_btnAuthenticate.setOnClickListener(this);

		m_btnTakePhoto = (Button) findViewById(R.id.btnTakePhoto);
		m_btnTakePhoto.setOnClickListener(this);

		m_btnViewPhoto = (Button) findViewById(R.id.btnViewPhoto);
		m_btnViewPhoto.setOnClickListener(this);

		m_listPhotos = (ListView) findViewById(R.id.listPhotos);
		m_listPhotos.setOnItemClickListener(this);
		// Utility.setListViewHeightBasedOnChildren(m_listPhotos);

		m_btnRecordSound = (Button) findViewById(R.id.btnRecordSound);
		m_btnRecordSound.setOnClickListener(this);

		m_btnCreateText = (Button) findViewById(R.id.btnCreateText);
		m_btnCreateText.setOnClickListener(this);

	}

	// on click listener
	@Override
	public void onClick(View v) {
		if (v == m_btnAuthenticate) {
			if (mLoggedIn) {
				logOut();
			} else {
				mApi.getSession().startOAuth2Authentication(context);
			}
		} else if (v == m_btnTakePhoto) {
			Intent intent = new Intent();
			intent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);

			Date date = new Date();
			DateFormat df = new SimpleDateFormat("yyyy-MM-dd-kk-mm-ss",
					Locale.US);

			String newPicFile = df.format(date) + "-" + mCity + ".jpg";
			String outPath = new File(
					Environment.getExternalStorageDirectory(), newPicFile)
					.getPath();
			File outFile = new File(outPath);

			mCameraFileName = outFile.toString();
			Uri outuri = Uri.fromFile(outFile);
			intent.putExtra(MediaStore.EXTRA_OUTPUT, outuri);
			try {
				startActivityForResult(intent, TAKE_PHOTO);

			} catch (ActivityNotFoundException e) {
				Toast.makeText(context, "No camera found.", Toast.LENGTH_LONG)
						.show();

			}
		} else if (v == m_btnViewPhoto) {
			new GetListOfPhotos().execute();
		} else if (v == m_btnRecordSound) {
			Intent nextAct = new Intent(context, RecordSound.class);
			startActivity(nextAct);
		} else if (v == m_btnCreateText) {
			Intent nextAct = new Intent(context, CreateTextNotes.class);
			startActivity(nextAct);
		}

	}

	// on item click listener of the listview
	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {

		Intent newAct = new Intent(context, PhotoActivity.class);
		newAct.putExtra("filename", adapter.getItem(arg2));
		startActivity(newAct);

	}

	// to get list of photos from dropbox from directory "Photos"
	public class GetListOfPhotos extends AsyncTask<Void, Void, Boolean> {
		List<Entry> contents;
		private ProgressDialog dialog;

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			dialog = new ProgressDialog(context);
			dialog.setMessage("Please Wait...");
			dialog.setCancelable(false);
			dialog.show();
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			Boolean result = false;
			try {
				Entry dropboxDir = mApi
						.metadata(PHOTO_DIR, 0, null, true, null);
				if (dropboxDir.isDir) {
					contents = dropboxDir.contents;
					if (contents != null) {
						result = true;

					}
				}
			} catch (Exception ex) {
				Log.d("dropbox", "ERROR");
			}
			return result;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);
			if (result) {
				ArrayList<String> list = new ArrayList<String>();
				m_listPhotos.setVisibility(View.VISIBLE);
				for (int i = 0; i < contents.size(); i++) {
					Entry e = contents.get(i);
					String a = e.fileName();
					if (a.contains(".jpg")) {
						Log.d("dropbox", "FileName:" + a);
						list.add(a);
					}
				}

				// populate array adapter with string array
				adapter = new ArrayAdapter<String>(context,
						android.R.layout.simple_list_item_1, list);

				// set adapter to listview
				m_listPhotos.setAdapter(adapter);

				dialog.dismiss();
			}
		}

	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == TAKE_PHOTO) {

			if (resultCode == Activity.RESULT_OK) {
				Uri uri = null;
				if (data != null) {
					uri = data.getData();
				}
				if (uri == null && mCameraFileName != null) {
					uri = Uri.fromFile(new File(mCameraFileName));
				}
				File file = new File(mCameraFileName);
				try {
					writeFile(file, mLatitude, mLongitude);
				} catch (IOException e) {
					e.printStackTrace();
				}
				if (uri != null) {
					UploadFile upload = new UploadFile(this, mApi, PHOTO_DIR,
							file);
					upload.execute();
				}

			} else {
				Log.w(TAG, "Unknown Activity Result from mediaImport: "
						+ resultCode);
			}
		}
	}

	// establish session using app key and app secret to dropbox
	private AndroidAuthSession buildSession() {
		AppKeyPair appKeyPair = new AppKeyPair(APP_KEY, APP_SECRET);

		AndroidAuthSession session = new AndroidAuthSession(appKeyPair);
		loadAuth(session);
		return session;
	}

	// load auth access token if present in shared preferences
	private void loadAuth(AndroidAuthSession session) {
		SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
		String key = prefs.getString(ACCESS_KEY_NAME, null);
		String secret = prefs.getString(ACCESS_SECRET_NAME, null);
		if (key == null || secret == null || key.length() == 0
				|| secret.length() == 0)
			return;

		if (key.equals("oauth2:")) {
			session.setOAuth2AccessToken(secret);
		} else {
			session.setAccessTokenPair(new AccessTokenPair(key, secret));
		}
	}

	// logout of the dropbox session by unlinking the session, clearing shared
	// preferences data and hiding all the buttons
	private void logOut() {
		mApi.getSession().unlink();
		clearKeys();
		setLoggedIn(false);
	}

	// Clear Shared Preferences data
	private void clearKeys() {
		SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
		Editor edit = prefs.edit();
		edit.clear();
		edit.commit();
	}

	// Change UI state to display logged in/out version
	private void setLoggedIn(boolean loggedIn) {
		mLoggedIn = loggedIn;
		if (loggedIn) {
			m_btnAuthenticate.setText("Logout from Dropbox");
			m_btnTakePhoto.setVisibility(View.VISIBLE);
			m_btnViewPhoto.setVisibility(View.VISIBLE);
			m_btnRecordSound.setVisibility(View.VISIBLE);
			m_btnCreateText.setVisibility(View.VISIBLE);
		} else {
			m_btnAuthenticate.setText("Authenticate Dropbox");
			m_btnTakePhoto.setVisibility(View.GONE);
			m_btnViewPhoto.setVisibility(View.GONE);
			m_btnRecordSound.setVisibility(View.GONE);
			m_btnCreateText.setVisibility(View.GONE);
			// mImage.setImageDrawable(null);
		}
	}

	// store the data in the shared preferences
	private void storeAuth(AndroidAuthSession session) {
		// Store the OAuth 2 access token, if there is one.
		String oauth2AccessToken = session.getOAuth2AccessToken();
		if (oauth2AccessToken != null) {
			SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME,
					0);
			Editor edit = prefs.edit();
			edit.putString(ACCESS_KEY_NAME, "oauth2:");
			edit.putString(ACCESS_SECRET_NAME, oauth2AccessToken);
			edit.commit();
			return;
		}
		// Store the OAuth 1 access token, if there is one.
		AccessTokenPair oauth1AccessToken = session.getAccessTokenPair();
		if (oauth1AccessToken != null) {
			SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME,
					0);
			Editor edit = prefs.edit();
			edit.putString(ACCESS_KEY_NAME, oauth1AccessToken.key);
			edit.putString(ACCESS_SECRET_NAME, oauth1AccessToken.secret);
			edit.commit();
			return;
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		AndroidAuthSession session = mApi.getSession();

		if (session.authenticationSuccessful()) {
			try {
				session.finishAuthentication();

				storeAuth(session);
				setLoggedIn(true);
			} catch (IllegalStateException e) {
				Toast.makeText(
						context,
						"Couldn't authenticate with Dropbox:"
								+ e.getLocalizedMessage(), Toast.LENGTH_LONG)
						.show();
				Log.i(TAG, "Error authenticating", e);
			}
		} else
			setLoggedIn(false);
	}

	@Override
	public void onConnected(Bundle arg0) {
		mLastLocation = LocationServices.FusedLocationApi
				.getLastLocation(mGoogleApiClient);
		if (mLastLocation != null) {
			mLatitude = mLastLocation.getLatitude();
			mLongitude = mLastLocation.getLongitude();

			Log.e("lat - long", mLatitude + " - " + mLongitude);

			new MatchingNearByLocationTask().execute();

		}

	}

	@Override
	public void onConnectionSuspended(int arg0) {
		// TODO Auto-generated method stub
		Log.e("on connection suspended", " - ");
	}

	@Override
	public void onConnectionFailed(ConnectionResult arg0) {
		// TODO Auto-generated method stub
		Log.e("on connection failed", " - ");
	}

	// background async task to get address from lat-lng
	private class MatchingNearByLocationTask extends
			AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... arg0) {

			JSONObject jsonStr = getLocationInfo(Double.valueOf(mLatitude),
					Double.valueOf(mLongitude));
			if (jsonStr != null) {
				// Log.e("reverse geocoding", jsonStr + "");

				JSONObject jsonObj;
				try {
					jsonObj = new JSONObject(jsonStr.toString());

					String Status = jsonObj.getString("status");
					if (Status.equalsIgnoreCase("OK")) {
						JSONArray Results = jsonObj.getJSONArray("results");
						JSONObject zero = Results.getJSONObject(0);
						JSONArray address_components = zero
								.getJSONArray("address_components");

						for (int i = 0; i < address_components.length(); i++) {
							JSONObject zero2 = address_components
									.getJSONObject(i);
							String long_name = zero2.getString("long_name");
							JSONArray mtypes = zero2.getJSONArray("types");
							String Type = mtypes.getString(0);
							if (Type.equalsIgnoreCase("locality")) {
								mCity = long_name;
								Log.d(" CityName ", mCity + "");
							}
						}
					}

				}

				catch (JSONException e) {

					e.printStackTrace();
				}

			}

			return null;
		}

	}

	// api call to get address
	private JSONObject getLocationInfo(double lat, double lng) {

		HttpGet httpGet = new HttpGet(
				"http://maps.googleapis.com/maps/api/geocode/json?latlng="
						+ lat + "," + lng + "&sensor=false");
		HttpClient client = new DefaultHttpClient();
		HttpResponse response;
		StringBuilder stringBuilder = new StringBuilder();

		try {
			response = client.execute(httpGet);
			HttpEntity entity = response.getEntity();
			InputStream stream = entity.getContent();
			int b;
			while ((b = stream.read()) != -1) {
				stringBuilder.append((char) b);
			}
		} catch (ClientProtocolException e) {
		} catch (IOException e) {
		}

		JSONObject jsonObject = new JSONObject();
		try {
			jsonObject = new JSONObject(stringBuilder.toString());
		} catch (JSONException e) {
			e.printStackTrace();
		}

		return jsonObject;
	}

	// writing gps sco-ordinates to the image file after capturing from camera
	public static void writeFile(File photo, double latitude, double longitude)
			throws IOException {

		ExifInterface exif = null;

		try {
			exif = new ExifInterface(photo.getCanonicalPath());
			if (exif != null) {
				String stringLati = Location.convert(latitude,
						Location.FORMAT_DEGREES);
				String stringLongi = Location.convert(longitude,
						Location.FORMAT_DEGREES);

				exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, stringLati);
				exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, stringLongi);

				Log.v("latiString", "" + stringLati);
				Log.v("longiString", "" + stringLongi);

				exif.saveAttributes();

				String lati = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE);
				String longi = exif
						.getAttribute(ExifInterface.TAG_GPS_LONGITUDE);

				Log.v("latiResult", "" + lati);
				Log.v("longiResult", "" + longi);

			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

		}

	}

}

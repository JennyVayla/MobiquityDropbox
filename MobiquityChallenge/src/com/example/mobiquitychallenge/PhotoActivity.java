package com.example.mobiquitychallenge;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.Utils.GlobalApplication;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.model.GraphUser;
import com.facebook.widget.LoginButton;
import com.facebook.widget.LoginButton.UserInfoChangedCallback;

public class PhotoActivity extends Activity implements OnClickListener {

	Context context;

	// controls
	ImageView m_imgPhoto;
	Button m_btnShare;
	private LoginButton loginBtn;

	// user variables
	private UiLifecycleHelper uiHelper;
	private final String PHOTO_DIR = "/Photos/";
	String filename;
	private static final List<String> PERMISSIONS = Arrays
			.asList("publish_actions");
	private final static String IMAGE_FILE_NAME = "MobiquityChallenge.png";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		uiHelper = new UiLifecycleHelper(this, statusCallback);
		uiHelper.onCreate(savedInstanceState);

		setContentView(R.layout.activity_photo);

		context = PhotoActivity.this;

		Intent i = getIntent();
		if (i.hasExtra("filename"))
			filename = i.getStringExtra("filename");

		GenerateHashkey();

		InitializeControls();
	}

	// generate hashkey and register it on facebook developers site
	public void GenerateHashkey() {
		try {
			PackageInfo info = getPackageManager().getPackageInfo(
					"com.example.mobiquitychallenge",
					PackageManager.GET_SIGNATURES);
			for (android.content.pm.Signature signature : info.signatures) {
				MessageDigest md = MessageDigest.getInstance("SHA");
				md.update(signature.toByteArray());
				String sign = Base64
						.encodeToString(md.digest(), Base64.DEFAULT);
				Log.e("MY KEY HASH:", sign);
			}
		} catch (NameNotFoundException e) {
		} catch (NoSuchAlgorithmException e) {
		}
	}

	// initialize all the controls
	public void InitializeControls() {
		m_imgPhoto = (ImageView) findViewById(R.id.imgPhoto);

		m_btnShare = (Button) findViewById(R.id.btnShare);
		m_btnShare.setOnClickListener(this);

		DownloadPhoto download = new DownloadPhoto(context,
				GlobalApplication.mApi, PHOTO_DIR, m_imgPhoto, filename);
		download.execute();

		loginBtn = (LoginButton) findViewById(R.id.fb_login_button);
		loginBtn.setUserInfoChangedCallback(new UserInfoChangedCallback() {
			@Override
			public void onUserInfoFetched(GraphUser user) {
				if (user != null) {
					Log.e("user info", user + "");
					// UploadImageToFacebook();

					// userName.setText("Hello, " + user.getName());
				} else {
					// userName.setText("You are not logged");
				}
			}
		});

	}

	// on click listener
	@Override
	public void onClick(View v) {
		if (v == m_btnShare) {
			UploadImageToFacebook();
		}
	}

	// facebook session callback
	private Session.StatusCallback statusCallback = new Session.StatusCallback() {
		@Override
		public void call(Session session, SessionState state,
				Exception exception) {
			if (state.isOpened()) {
				Log.d("FacebookSampleActivity", "Facebook session opened");
			} else if (state.isClosed()) {
				m_btnShare.setVisibility(View.GONE);
				Log.d("FacebookSampleActivity", "Facebook session closed");
			}
		}
	};

	// upload image + title to facebook
	public void UploadImageToFacebook() {
		if (checkPermissions()) {
			String cachePath = getCacheDir().getAbsolutePath() + "/"
					+ IMAGE_FILE_NAME;

			Drawable mDrawable = Drawable.createFromPath(cachePath);

			Bitmap img = drawableToBitmap(mDrawable);
			Request uploadRequest = Request.newUploadPhotoRequest(
					Session.getActiveSession(), img, new Request.Callback() {
						@Override
						public void onCompleted(Response response) {
							Toast.makeText(context,
									"Photo uploaded successfully",
									Toast.LENGTH_LONG).show();
						}
					});

			// get the city name from the filename
			String[] str = filename.split("-");
			if (!str[6].equalsIgnoreCase("")) {

				String[] split_more = str[6].split("\\.");
				if (!split_more[0].equalsIgnoreCase("")) {
					Bundle params = uploadRequest.getParameters();
					params.putString("message", split_more[0]);
					uploadRequest.setParameters(params);
				}
			}

			uploadRequest.executeAsync();
		} else {
			requestPermissions();
		}
	}

	// check if the user has granted permission for publishing photo
	public boolean checkPermissions() {
		Session s = Session.getActiveSession();
		if (s != null) {
			return s.getPermissions().contains("publish_actions");
		} else {
			return false;
		}
	}

	// ask user for the publish photo permission
	public void requestPermissions() {
		Session s = Session.getActiveSession();
		if (s != null)
			s.requestNewPublishPermissions(new Session.NewPermissionsRequest(
					this, PERMISSIONS));
	}

	// get bitmap from drawable
	public static Bitmap drawableToBitmap(Drawable drawable) {
		if (drawable instanceof BitmapDrawable) {
			return ((BitmapDrawable) drawable).getBitmap();
		}

		Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
				drawable.getIntrinsicHeight(), Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
		drawable.draw(canvas);

		return bitmap;
	}

	@Override
	public void onResume() {
		super.onResume();
		uiHelper.onResume();
		if (Session.getActiveSession().isOpened())
			m_btnShare.setVisibility(View.VISIBLE);
	}

	@Override
	public void onPause() {
		super.onPause();
		uiHelper.onPause();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		uiHelper.onDestroy();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		uiHelper.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public void onSaveInstanceState(Bundle savedState) {
		super.onSaveInstanceState(savedState);
		uiHelper.onSaveInstanceState(savedState);
	}
}

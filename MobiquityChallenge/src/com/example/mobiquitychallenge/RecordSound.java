package com.example.mobiquitychallenge;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.app.Activity;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.example.Utils.GlobalApplication;

public class RecordSound extends Activity implements OnClickListener {

	// user variables
	MediaRecorder myAudioRecorder;
	String outputFile = null;
	private final String SOUND_DIR = "/Sound/";

	// controls
	Button m_btnStart, m_btnStop, m_btnPlay, m_btnUpload;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.record_sound);

		InitializeControls();

		Date date = new Date();
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd-kk-mm-ss", Locale.US);

		String newPicFile = df.format(date) + ".3gp";

		outputFile = Environment.getExternalStorageDirectory()
				.getAbsolutePath() + "/" + newPicFile;

		// setting up the media recorder for recording audio
		myAudioRecorder = new MediaRecorder();
		myAudioRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
		myAudioRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
		myAudioRecorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB);
		myAudioRecorder.setOutputFile(outputFile);
	}

	// initialize all the controls
	public void InitializeControls() {
		m_btnStart = (Button) findViewById(R.id.btnStart);
		m_btnStart.setOnClickListener(this);

		m_btnStop = (Button) findViewById(R.id.btnStop);
		m_btnStop.setOnClickListener(this);
		m_btnStop.setEnabled(false);

		m_btnPlay = (Button) findViewById(R.id.btnPlay);
		m_btnPlay.setOnClickListener(this);
		m_btnPlay.setEnabled(false);

		m_btnUpload = (Button) findViewById(R.id.btnUpload);
		m_btnUpload.setOnClickListener(this);
		m_btnUpload.setEnabled(false);
	}

	// on click listener
	@Override
	public void onClick(View v) {
		if (v == m_btnStart) {
			try {
				myAudioRecorder.prepare();
				myAudioRecorder.start();
			} catch (IllegalStateException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			m_btnStart.setEnabled(false);
			m_btnStop.setEnabled(true);
			Toast.makeText(getApplicationContext(), "Recording started",
					Toast.LENGTH_LONG).show();
		} else if (v == m_btnStop) {
			myAudioRecorder.stop();
			myAudioRecorder.reset();
			myAudioRecorder.release();
			myAudioRecorder = null;
			m_btnStop.setEnabled(false);
			m_btnPlay.setEnabled(true);
			m_btnUpload.setEnabled(true);
			Toast.makeText(getApplicationContext(),
					"Audio recorded successfully", Toast.LENGTH_LONG).show();
		} else if (v == m_btnPlay) {

			try {
				MediaPlayer m = new MediaPlayer();

				m.setDataSource(outputFile);
				m.prepare();
				m.start();
				Toast.makeText(getApplicationContext(), "Playing audio",
						Toast.LENGTH_LONG).show();
				m_btnUpload.setEnabled(true);
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalStateException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		} else if (v == m_btnUpload) {

			File file = new File(outputFile);
			UploadFile upload = new UploadFile(this, GlobalApplication.mApi,
					SOUND_DIR, file);
			upload.execute();
		}
	}
}

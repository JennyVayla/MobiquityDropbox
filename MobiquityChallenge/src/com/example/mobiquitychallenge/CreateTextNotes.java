package com.example.mobiquitychallenge;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.commonsware.cwac.richedit.RichEditText;
import com.example.Utils.GlobalApplication;

public class CreateTextNotes extends Activity implements OnClickListener {

	// user variables
	private final String TEXT_DIR = "/Text Notes/";
	Context context;

	// controls
	RichEditText m_txtNotes;
	Button m_btnUpload, m_btnBold, m_btnItalic, m_btnUnderline, m_btnStrike;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.create_text_notes);

		context = this;

		InitializeControls();
	}

	public void InitializeControls() {
		m_txtNotes = (RichEditText) findViewById(R.id.txtNote);

		m_btnUpload = (Button) findViewById(R.id.btnUpload);
		m_btnUpload.setOnClickListener(this);

		m_btnBold = (Button) findViewById(R.id.btnBold);
		m_btnBold.setOnClickListener(this);

		m_btnItalic = (Button) findViewById(R.id.btnItalic);
		m_btnItalic.setOnClickListener(this);

		m_btnUnderline = (Button) findViewById(R.id.btnUnderline);
		m_btnUnderline.setOnClickListener(this);

		m_btnStrike = (Button) findViewById(R.id.btnStrike);
		m_btnStrike.setOnClickListener(this);

		m_btnBold = (Button) findViewById(R.id.btnBold);
		m_btnBold.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		if (v == m_btnUpload) {

			if (m_txtNotes.getText().toString().equalsIgnoreCase("")
					|| m_txtNotes.getText() == null) {
				Toast.makeText(context, "Please write some text",
						Toast.LENGTH_LONG).show();
			} else {
				Date date = new Date();
				DateFormat df = new SimpleDateFormat("yyyy-MM-dd-kk-mm-ss",
						Locale.US);

				String newPicFile = df.format(date) + ".txt";

				String outPath = new File(
						Environment.getExternalStorageDirectory(), newPicFile)
						.getPath();
				File outFile = new File(outPath);

				FileWriter fr;
				try {
					fr = new FileWriter(outFile);
					fr.write(m_txtNotes.getText().toString());

					fr.close();

					UploadFile upload = new UploadFile(this,
							GlobalApplication.mApi, TEXT_DIR, outFile);
					upload.execute();

				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} else if (v == m_btnBold) {
			m_txtNotes.applyEffect(RichEditText.BOLD, true);
		} else if (v == m_btnItalic) {
			m_txtNotes.applyEffect(RichEditText.ITALIC, true);
		} else if (v == m_btnUnderline) {
			m_txtNotes.applyEffect(RichEditText.UNDERLINE, true);
		} else if (v == m_btnStrike) {
			m_txtNotes.applyEffect(RichEditText.STRIKETHROUGH, true);
		}
	}
}

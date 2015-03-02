package com.example.Utils;

import android.app.Application;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;

public class GlobalApplication extends Application {

	public static DropboxAPI<AndroidAuthSession> mApi;

}

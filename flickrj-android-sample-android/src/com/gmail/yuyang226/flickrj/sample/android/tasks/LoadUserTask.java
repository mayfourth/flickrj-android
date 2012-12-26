/**
 * 
 */
package com.gmail.yuyang226.flickrj.sample.android.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.widget.ImageView;
import android.widget.Toast;

import com.gmail.yuyang226.flickrj.sample.android.FlickrHelper;
import com.gmail.yuyang226.flickrj.sample.android.FlickrjAndroidSampleActivity;
import com.gmail.yuyang226.flickrj.sample.android.images.ImageUtils.DownloadedDrawable;
import com.googlecode.flickrjandroid.Flickr;
import com.googlecode.flickrjandroid.oauth.OAuth;
import com.googlecode.flickrjandroid.oauth.OAuthToken;
import com.googlecode.flickrjandroid.people.User;

public class LoadUserTask extends AsyncTask<OAuth, Void, User> {
	/**
	 * 
	 */
	private final FlickrjAndroidSampleActivity flickrjAndroidSampleActivity;
	private ImageView userIconImage;
	private final Logger logger = LoggerFactory.getLogger(LoadUserTask.class);
	
	public LoadUserTask(FlickrjAndroidSampleActivity flickrjAndroidSampleActivity, 
			ImageView userIconImage) {
		this.flickrjAndroidSampleActivity = flickrjAndroidSampleActivity;
		this.userIconImage = userIconImage;
	}
	
	/**
	 * The progress dialog before going to the browser.
	 */
	private ProgressDialog mProgressDialog;
	
	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		mProgressDialog = ProgressDialog.show(flickrjAndroidSampleActivity,
				"", "Loading user information..."); //$NON-NLS-1$ //$NON-NLS-2$
		mProgressDialog.setCanceledOnTouchOutside(true);
		mProgressDialog.setCancelable(true);
		mProgressDialog.setOnCancelListener(new OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dlg) {
				LoadUserTask.this.cancel(true);
			}
		});
	}

	/* (non-Javadoc)
	 * @see android.os.AsyncTask#doInBackground(Params[])
	 * 
	 * @param params is oauth object since new LoadUserTask(this, userIcon).execute(oauth)
	 */
	@Override
	protected User doInBackground(OAuth... params) {
		OAuth oauth = params[0];
		User user = oauth.getUser();  //User [id=21040560@N03, username=kai_xu]
		OAuthToken token = oauth.getToken();  //OAuthToken [oauthToken=72157632328080604-76a4a160ffd56fcd, oauthTokenSecret=e3a30c3df070289d]
		try {
			Flickr f = FlickrHelper.getInstance()
					.getFlickrAuthed(token.getOauthToken(), token.getOauthTokenSecret());
			User u= f.getPeopleInterface().getInfo(user.getId());   //network IO
			return u;
		} catch (Exception e) {
			Toast.makeText(flickrjAndroidSampleActivity, e.toString(), Toast.LENGTH_LONG).show();
			logger.error(e.getLocalizedMessage(), e);
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
	 */
	@Override
	protected void onPostExecute(User user) {
		if (mProgressDialog != null) {
			mProgressDialog.dismiss();
		}
		if (user == null) {
			return;
		}
		flickrjAndroidSampleActivity.setUser(user);
		if (user.getBuddyIconUrl() != null) {
			String buddyIconUrl = user.getBuddyIconUrl();
	        if (userIconImage != null) {
	        	ImageDownloadTask task = new ImageDownloadTask(userIconImage);
	            Drawable drawable = new DownloadedDrawable(task);
	            userIconImage.setImageDrawable(drawable);
	            task.execute(buddyIconUrl);
	        }
		}
	}
	
	
}
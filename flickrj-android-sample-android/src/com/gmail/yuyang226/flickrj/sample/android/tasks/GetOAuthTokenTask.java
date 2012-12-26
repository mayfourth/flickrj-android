/**
 * 
 */
package com.gmail.yuyang226.flickrj.sample.android.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.os.AsyncTask;

import com.gmail.yuyang226.flickrj.sample.android.FlickrHelper;
import com.gmail.yuyang226.flickrj.sample.android.FlickrjAndroidSampleActivity;
import com.googlecode.flickrjandroid.Flickr;
import com.googlecode.flickrjandroid.oauth.OAuth;
import com.googlecode.flickrjandroid.oauth.OAuthInterface;

/**
 * @author Toby Yu(yuyang226@gmail.com)
 *
 */
public class GetOAuthTokenTask extends AsyncTask<String, Integer, OAuth> {
	private static final Logger logger = LoggerFactory.getLogger(GetOAuthTokenTask.class);

	private FlickrjAndroidSampleActivity activity;

	public GetOAuthTokenTask(FlickrjAndroidSampleActivity context) {
		this.activity = context;
	}

	/**
	 * 	GetOAuthTokenTask task = new GetOAuthTokenTask(this);
	 *	task.execute(oauthToken, oauth.getToken().getOauthTokenSecret(), oauthVerifier);
	 */
	@Override
	protected OAuth doInBackground(String... params) {
		String oauthToken = params[0];
		String oauthTokenSecret = params[1];
		String verifier = params[2];

		Flickr f = FlickrHelper.getInstance().getFlickr();
		OAuthInterface oauthApi = f.getOAuthInterface();
		try {
			OAuth accessAuth= oauthApi.getAccessToken(oauthToken, oauthTokenSecret,verifier);
			return accessAuth;
		} catch (Exception e) {
			logger.error(e.getLocalizedMessage(), e);
			return null;
		}

	}

	@Override
	protected void onPostExecute(OAuth result) {
		if (activity != null) {
			activity.onOAuthDone(result);
		}
	}


}

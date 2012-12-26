package com.gmail.yuyang226.flickrj.sample.android;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.gmail.yuyang226.flickrj.sample.android.tasks.GetOAuthTokenTask;
import com.gmail.yuyang226.flickrj.sample.android.tasks.LoadPhotostreamTask;
import com.gmail.yuyang226.flickrj.sample.android.tasks.LoadUserTask;
import com.gmail.yuyang226.flickrj.sample.android.tasks.OAuthTask;
import com.googlecode.flickrjandroid.oauth.OAuth;
import com.googlecode.flickrjandroid.oauth.OAuthToken;
import com.googlecode.flickrjandroid.people.User;

public class FlickrjAndroidSampleActivity extends Activity {
	public static final String CALLBACK_SCHEME = "flickrj-android-sample-oauth"; //$NON-NLS-1$
	public static final String PREFS_NAME = "flickrj-android-sample-pref"; //$NON-NLS-1$
	public static final String KEY_OAUTH_TOKEN = "flickrj-android-oauthToken"; //$NON-NLS-1$
	public static final String KEY_TOKEN_SECRET = "flickrj-android-tokenSecret"; //$NON-NLS-1$
	public static final String KEY_USER_NAME = "flickrj-android-userName"; //$NON-NLS-1$
	public static final String KEY_USER_ID = "flickrj-android-userId"; //$NON-NLS-1$
	
	private static final Logger logger = LoggerFactory.getLogger(FlickrjAndroidSampleActivity.class);
	
	
	private ListView listView;
	private TextView textUserTitle;
	private TextView textUserName;
	private TextView textUserId;
	private ImageView userIcon;
	private ImageButton refreshButton;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		cleanupOAuthToken(); //kxu
		
		this.textUserTitle = (TextView) this.findViewById(R.id.profilePageTitle);
		this.textUserName = (TextView) this.findViewById(R.id.userScreenName);
		this.textUserId = (TextView) this.findViewById(R.id.userId);
		this.userIcon = (ImageView) this.findViewById(R.id.userImage);
		this.listView = (ListView) this.findViewById(R.id.imageList);
		this.refreshButton = (ImageButton) this.findViewById(R.id.btnRefreshUserProfile);
		
		this.refreshButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				load(getOAuthToken());
			}
		});

		OAuth oauth = getOAuthToken();
		if (oauth == null || oauth.getUser() == null) {
			OAuthTask task = new OAuthTask(this);
			task.execute();
		} else {
			load(oauth);
		}
	}
	
	private void load(OAuth oauth) {
		if (oauth != null) {
			new LoadUserTask(this, userIcon).execute(oauth);   //doInBackground(OAuth... params)
			new LoadPhotostreamTask(this, listView).execute(oauth);
		}
	}
    
    @Override
    public void onDestroy() {
    	listView.setAdapter(null);
        super.onDestroy();
        
        //cleanupOAuthToken(); //kxu
    }
    
	/* (non-Javadoc)
	 * @see android.app.Activity#onNewIntent(android.content.Intent)
	 */
	@Override
	protected void onNewIntent(Intent intent) {
		//this is very important, otherwise you would get a null Scheme in the onResume later on.
		setIntent(intent);
	}
	
	public void setUser(User user) {
		textUserTitle.setText(user.getUsername());
		textUserName.setText(user.getRealName());
		textUserId.setText(user.getId());
	}
	
	public ImageView getUserIconImageView() {
		return this.userIcon;
	}

	/**
	 * resume app after invoked activity (oauth browser) is done.
	 */
	@Override
	public void onResume() {
		super.onResume();
		Intent intent = getIntent();
		String scheme = intent.getScheme(); //null,  CALLBACK_SCHEME  flickrj-android-sample-oauth
		OAuth savedToken = getOAuthToken();
		if (CALLBACK_SCHEME.equals(scheme) && (savedToken == null || savedToken.getUser() == null)) {
			Uri uri = intent.getData();
			String query = uri.getQuery(); //uri==>  flickrj-android-sample-oauth://oauth?oauth_token=72157632329868125-5d76f2d98bf2612a&oauth_verifier=f04e197df2dee57d
			logger.debug("Returned Query: {}", query); //$NON-NLS-1$
			String[] data = query.split("&"); //$NON-NLS-1$
			if (data != null && data.length == 2) {
				String oauthToken = data[0].substring(data[0].indexOf("=") + 1); //72157632329868125-5d76f2d98bf2612a
				String oauthVerifier = data[1]
						.substring(data[1].indexOf("=") + 1); //f04e197df2dee57d
				logger.debug("OAuth Token: {}; OAuth Verifier: {}", oauthToken, oauthVerifier); //$NON-NLS-1$

				OAuth oauth = getOAuthToken();
				if (oauth != null && oauth.getToken() != null && oauth.getToken().getOauthTokenSecret() != null) {
					GetOAuthTokenTask task = new GetOAuthTokenTask(this);
					task.execute(oauthToken, oauth.getToken().getOauthTokenSecret(), oauthVerifier);
				}
			}
		}

	}
    
    public void onOAuthDone(OAuth result) {
		if (result == null) {
			Toast.makeText(this,
					"Authorization failed", //$NON-NLS-1$
					Toast.LENGTH_LONG).show();
		} else {
			User user = result.getUser();
			OAuthToken token = result.getToken();
			if (user == null || user.getId() == null || token == null
					|| token.getOauthToken() == null
					|| token.getOauthTokenSecret() == null) {
				Toast.makeText(this,
						"Authorization failed", //$NON-NLS-1$
						Toast.LENGTH_LONG).show();
				return;
			}
			String message = String.format(Locale.US, "Authorization Succeed: user=%s, userId=%s, oauthToken=%s, tokenSecret=%s", //$NON-NLS-1$
					user.getUsername(), user.getId(), token.getOauthToken(), token.getOauthTokenSecret());
			Toast.makeText(this,
					message,
					Toast.LENGTH_LONG).show();
			saveOAuthToken(user.getUsername(), user.getId(), token.getOauthToken(), token.getOauthTokenSecret());
			load(result);
		}
	}
    
    
    public OAuth getOAuthToken() {
    	 //Restore preferences
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String oauthTokenString = settings.getString(KEY_OAUTH_TOKEN, null); //72157632328080604-76a4a160ffd56fcd
        String tokenSecret = settings.getString(KEY_TOKEN_SECRET, null); //e3a30c3df070289d
        if (oauthTokenString == null && tokenSecret == null) {
        	logger.warn("No oauth token retrieved"); //$NON-NLS-1$
        	return null;
        }
        OAuth oauth = new OAuth();
        String userName = settings.getString(KEY_USER_NAME, null);  //kai_xu
        String userId = settings.getString(KEY_USER_ID, null);   //21040560@N03
        if (userId != null) {
        	User user = new User();
        	user.setUsername(userName);
        	user.setId(userId);
        	oauth.setUser(user);
        }
        OAuthToken oauthToken = new OAuthToken();
        oauth.setToken(oauthToken);
        oauthToken.setOauthToken(oauthTokenString);  //72157632328080604-76a4a160ffd56fcd
        oauthToken.setOauthTokenSecret(tokenSecret); //e3a30c3df070289d
        logger.debug("Retrieved token from preference store: oauth token={}, and token secret={}", oauthTokenString, tokenSecret); //$NON-NLS-1$
        return oauth;
    }
    
    public void saveOAuthToken(String userName, String userId, String token, String tokenSecret) {
    	logger.debug("Saving userName=%s, userId=%s, oauth token={}, and token secret={}", new String[]{userName, userId, token, tokenSecret}); //$NON-NLS-1$
    	SharedPreferences sp = getSharedPreferences(PREFS_NAME,
				Context.MODE_PRIVATE);
		Editor editor = sp.edit();
		editor.putString(KEY_OAUTH_TOKEN, token);
		editor.putString(KEY_TOKEN_SECRET, tokenSecret);
		editor.putString(KEY_USER_NAME, userName);
		editor.putString(KEY_USER_ID, userId);
		editor.commit();
    }

    private void cleanupOAuthToken() {
    	//logger.debug("Saving userName=%s, userId=%s, oauth token={}, and token secret={}", new String[]{userName, userId, token, tokenSecret}); //$NON-NLS-1$
    	SharedPreferences sp = getSharedPreferences(PREFS_NAME,Context.MODE_PRIVATE);
		Editor editor = sp.edit();
		editor.remove(KEY_OAUTH_TOKEN);
		editor.remove(KEY_TOKEN_SECRET);
		editor.remove(KEY_USER_NAME);
		editor.remove(KEY_USER_ID);
		editor.commit();
    }
    
}
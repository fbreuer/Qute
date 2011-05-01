/*
*	 Qute Text Editor
*    Copyright (C) 2011 Felix Breuer
*
*    This program is free software: you can redistribute it and/or modify
*    it under the terms of the GNU General Public License as published by
*    the Free Software Foundation, either version 3 of the License, or
*    (at your option) any later version.
*
*    This program is distributed in the hope that it will be useful,
*    but WITHOUT ANY WARRANTY; without even the implied warranty of
*    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*    GNU General Public License for more details.
*
*    You should have received a copy of the GNU General Public License
*    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package net.inkcode.qute;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Date;
import java.text.SimpleDateFormat;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class QuteEditor extends Activity {
	
	private static final int THEME_BLACK = 0;
	private static final int THEME_WOOD = 1;
	private static final int THEME_PAPER = 2;
	private static final int THEME_CUTE = 3;
	
	private static final int FONT_COSMETICA = 0;
	private static final int FONT_JUNICODE = 1;
	private static final int FONT_UBUNTU = 2;
	private static final int FONT_GENTIUM = 3;
	
	private static final int MENU_OPEN = 0;
	private static final int MENU_SAVE = 1;
	private static final int MENU_DISCARD = 2;
	private static final int MENU_FULLSCREEN = 3;
	private static final int MENU_HELP = 4;
	private static final int MENU_PREFERENCES = 5;

	protected static final int REQUEST_PICK_FILE = 0;
	private static final int REQUEST_PREFERENCES = 1;
	
	private static final int EOL_N = 0;
	private static final int EOL_R = 1;
	private static final int EOL_RN = 2;
	
	private static boolean DEBUG = false;
	
	private EditText mText = null;
	private Uri mUri;
	private boolean mFullscreen = false;
	private boolean mShowingLicense = false;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // build UI
        setContentView(R.layout.main);
        
        // get Uri depending on Intent
        Intent intent = getIntent();
        String action = intent.getAction();
        mUri = intent.getData();
        if(DEBUG) Log.d("QuteEditor.onCreate","action: " + action + " uri: " + mUri);
                
        // setup UI theme
        mText = (EditText) findViewById(R.id.editor);
        if(mText == null) {
        	if(DEBUG) Log.e("QuteEditor.onCreate","mText is null");
        }        

        applyPreferences();
        showLicense();
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	if(DEBUG) Log.d("QuteEditor.onResume","");
    	if(!mShowingLicense) {
    		showOpenOrLoad();
    	}
    }
    
    @Override
    public void onPause() {
    	super.onPause();
    	if(DEBUG) Log.d("QuteEditor.onPause","");
    	if(uriIsSane()) {
    		save();
    	}
    }
    
    @Override
    public void finish() {
    	Intent data = new Intent();
    	setResult(RESULT_OK, data);
    	super.finish();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	super.onCreateOptionsMenu(menu);
    	
    	menu.add(0, MENU_OPEN, 0, R.string.menu_open)
    		.setShortcut('0','o')
    		.setIcon(android.R.drawable.ic_menu_edit);
    	menu.add(0, MENU_SAVE, 0, R.string.menu_save)
			.setShortcut('1','s')
			.setIcon(android.R.drawable.ic_menu_save);
    	menu.add(0, MENU_DISCARD, 0, R.string.menu_discard)
			.setShortcut('2','d')
			.setIcon(android.R.drawable.ic_menu_delete);
    	menu.add(0, MENU_FULLSCREEN, 0, R.string.menu_fullscreen)
			.setShortcut('3','f')
			.setIcon(android.R.drawable.ic_menu_view);
    	menu.add(0, MENU_HELP, 0, R.string.menu_help)
    		.setShortcut('4','h')
    		.setIcon(android.R.drawable.ic_menu_help);
    	menu.add(0, MENU_PREFERENCES, 0, R.string.menu_preferences)
			.setShortcut('5','p')
			.setIcon(android.R.drawable.ic_menu_preferences);
    	
    	return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch(item.getItemId()) {
    	case MENU_OPEN:
    		save();
    		showNewOrOpenDialog();
    		break;
    	case MENU_SAVE:
    		save();
    		break;
    	case MENU_DISCARD:
    		AlertDialog.Builder builder = new AlertDialog.Builder(this);
    		builder.setMessage("Do you want to discard the changes to the current file and open a different one?")
    		       .setCancelable(false)
    		       .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
    		           public void onClick(DialogInterface dialog, int id) {
    		                QuteEditor.this.mUri = null;
    		                QuteEditor.this.showNewOrOpenDialog();
    		           }
    		       })
    		       .setNegativeButton("No", new DialogInterface.OnClickListener() {
    		           public void onClick(DialogInterface dialog, int id) {
    		                dialog.cancel();
    		           }
    		       });
    		AlertDialog alert = builder.create();
    		alert.show();
    		break;
    	case MENU_FULLSCREEN:
    		toggleFullscreen();
    		break;
    	case MENU_HELP:
    		Intent browserIntent = new Intent("android.intent.action.VIEW", Uri.parse("http://www.inkcode.net/qute"));
    		startActivity(browserIntent);
    		break;
    	case MENU_PREFERENCES:
    		showPreferencesDialog();
    		break;
    	}
    	return super.onOptionsItemSelected(item);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	super.onActivityResult(requestCode, resultCode, data);
    	
    	switch(requestCode) {
    	case REQUEST_PICK_FILE:
    		if (resultCode == RESULT_OK && data != null) {
    			mUri = data.getData();
    			load();
    		} else {
    			showNewOrOpenDialog();
    		}
    		break;
    	case REQUEST_PREFERENCES:
    		applyPreferences();	
    		break;
    	}
    }
    
    private String defaultSaveDir() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
        	return getExternalFilesDir(null).getAbsolutePath();
        }
        return "/";
    }
    
    private String defaultSaveFilename() {
    	SimpleDateFormat sdf_day = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat sdf_time = new SimpleDateFormat("HH-mm-ss");        
        Date date = new Date();
        return sdf_day.format(date) + "-Note-" + sdf_time.format(date) + ".txt";
    }
    
    private Uri defaultSaveUri() {
    	Uri.Builder builder = new Uri.Builder();
		builder.scheme("file").path(defaultSaveDir()).appendPath(defaultSaveFilename());
		return builder.build();
    }
    
    private void showMsg(String str) {
    	Toast toast = Toast.makeText(this, str, Toast.LENGTH_SHORT);
    	toast.show();
    }
    
    private void showOpenOrLoad() {
    	if(!uriIsSane()) {
    		showNewOrOpenDialog();
    	} else {
			load();
    	}
    }
    
    private void showNewOrOpenDialog() {
        final Dialog dialog = new Dialog(QuteEditor.this);
        dialog.setContentView(R.layout.dialog_open);
        dialog.setTitle("Open File");
        dialog.setCancelable(false);
        
        // initialize directory string
        final EditText dir_entry = (EditText) dialog.findViewById(R.id.entry_directory);
      	dir_entry.setText(defaultSaveDir());
        
        // initialize file string
        final EditText file_entry = (EditText) dialog.findViewById(R.id.entry_file);
        file_entry.setText(defaultSaveFilename());
        
        Button button_pick = (Button) dialog.findViewById(R.id.button_pick_with_fm);
        button_pick.setOnClickListener(new Button.OnClickListener() {
        	@Override
            public void onClick(View v) {
        		Intent intent = new Intent("org.openintents.action.PICK_FILE");
        		intent.setData(defaultSaveUri());
        		intent.putExtra("org.openintents.extra.TITLE", "Qute: Open or Create File");
        		intent.putExtra("org.openintents.extra.BUTTON_TEXT", "Open");        		
        		try {
        			startActivityForResult(intent, REQUEST_PICK_FILE);
        			dialog.dismiss(); // this should be reached only if successful!
        		} catch (ActivityNotFoundException e) {
        			showMsg("There is no filemanager installed!");
        		}
            }
        });
        
        Button button = (Button) dialog.findViewById(R.id.button_open);
        button.setOnClickListener(new Button.OnClickListener() {
        	@Override
            public void onClick(View v) {
        		Uri.Builder builder = new Uri.Builder();
        		builder.scheme("file").path(dir_entry.getText().toString()).appendPath(file_entry.getText().toString());
                QuteEditor.this.mUri = builder.build();
                
                dialog.dismiss();
                
                QuteEditor.this.load();
            }
        });
        
        dialog.show();
    }
    
    private void showPreferencesDialog() {
        Intent preferencesActivity = new Intent(getBaseContext(), QutePreferences.class);
        startActivityForResult(preferencesActivity, REQUEST_PREFERENCES);
    }
    
    private void showLicense() {
    	final SharedPreferences prefs = getSharedPreferences("license", MODE_PRIVATE);
    	if(!prefs.getBoolean("license.accepted", false)) {
    		// read license
    		String content = "";
	    	try {
				InputStreamReader inputreader = new InputStreamReader(getAssets().open("license.txt"));
				BufferedReader bufferedreader = new BufferedReader(inputreader);
				String line;
				while((line = bufferedreader.readLine()) != null) {
					content += line + "\n";
				}
	    		bufferedreader.close();
	    	} catch (Exception e) {
				showMsg("There was an opening the license file.");
	    		e.printStackTrace();
	    		if(DEBUG) Log.e("QuteEditor.load","error while reading license");
	    		finish();
	    	}
    		mShowingLicense = true;
    		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
    		builder.setTitle("License");
    		builder.setCancelable(true);
    		View view = LayoutInflater.from(this).inflate(R.layout.license,null);
    		TextView text = (TextView) view.findViewById(R.id.licenseText);
    		builder.setView(view);
    		builder.setPositiveButton("Accept", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					prefs.edit().putBoolean("license.accepted", true).commit();
					QuteEditor.this.mShowingLicense = false;
					QuteEditor.this.showOpenOrLoad();
				}
			});
    		builder.setNegativeButton("Refuse", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					QuteEditor.this.finish();
				}
			});
    		builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					QuteEditor.this.finish();
				}
			});
    		text.setText(content);
    		builder.create().show();
    	}
    }
    
    private void toggleFullscreen() {
    	if(mFullscreen) {
            getWindow().setFlags(0, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            mFullscreen = false;
    	} else {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            mFullscreen = true;
    	}
    }
    
    private boolean uriIsSane() {
    	return mUri != null && mUri.getScheme().equalsIgnoreCase("file") && !mUri.getPath().equals(""); 
    }
    
    private void load() {
    	String content = "";
    	File file = new File(mUri.getPath());
    	if(file.exists()) {
	    	try {
	    		InputStream istream = new FileInputStream(file);
				InputStreamReader inputreader = new InputStreamReader(istream);
				BufferedReader bufferedreader = new BufferedReader(inputreader);
				String line;
				while((line = bufferedreader.readLine()) != null) {
					content += line + "\n";
				}
	    		istream.close();
	    		showMsg("Loaded file " + file.toString() + ".");
	    	} catch (IOException e) {
				showMsg("There was an error opening file " +  file.toString());
	    		e.printStackTrace();
	    		if(DEBUG) Log.e("QuteEditor.load","error while reading file " + mUri);
	    	}
    	} else {
    		showMsg("New file " + file.toString() + ".");
    	}
    	mText.setTextKeepState(content);
    }
    
    private void save() {
    	String content = mText.getText().toString();
    	String ending = "\\n";
    	// handle end of line characters
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
    	switch(Integer.parseInt(prefs.getString("prefsEOL", "0"))) {
    	case EOL_N:
    		break;
    	case EOL_R:
    		ending = "\\r";
    		content = content.replaceAll("\n", "\r");
    		break;
    	case EOL_RN:
    		ending = "\\r\\n";
    		content = content.replaceAll("\n", "\r\n");
    		break;
    	}
    	// save the string to file
    	if(uriIsSane()) {
    		File file = new File(mUri.getPath());
    		if(!file.exists()) {
    			try {
					file.createNewFile();
				} catch (IOException e) {
					e.printStackTrace();
					if(DEBUG) Log.e("QuteEditor.save","error creating file " + file.toString());
				}
    		}
    		try {
    			OutputStream ostream = new BufferedOutputStream(new FileOutputStream(file));
    			// TODO: add encoding handling
    			OutputStreamWriter outputwriter = new OutputStreamWriter(ostream,"UTF-8");
    			outputwriter.write(content);
    			outputwriter.flush();
    			outputwriter.close();
    			showMsg("Saved file "+ file.toString() + " with line ending " + ending + ".");
    		} catch(IOException e) {
    			showMsg("There was an error writing to file " + file.toString());
    			e.printStackTrace();
    			if(DEBUG) Log.e("QuteEditor","There was an error writing to file " + file.toString());
    		}
    	} else {
    		showMsg("URI " + mUri + " is not a valid file URI.");
    		if(DEBUG) Log.e("QuteEditor.save","URI " + mUri + " is not sane.");
    	}
    }
    
    public void applyPreferences() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        setQuteTheme(Integer.parseInt(prefs.getString("prefsTheme", "0")));
        setQuteFont(Integer.parseInt(prefs.getString("prefsFont", "0")), Integer.parseInt(prefs.getString("prefsFontSize", "16")));
    }
    
    public void setQuteTheme(int id) {
        if(mText == null) {
        	if(DEBUG) Log.e("QuteEditor.setTheme","mText is null");
        	return;
        }
    	mText.setPadding(15, 15, 15, 15);
    	mText.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_OVERLAY);
    	mText.setHorizontalFadingEdgeEnabled(false);
    	mText.setVerticalFadingEdgeEnabled(true);
    	switch(id) {
    	case THEME_BLACK:
    		mText.setBackgroundResource(R.drawable.bgblack);
    		mText.setShadowLayer(4.0f, 2.0f, 2.0f, 0xff000000);
    		mText.setTextColor(0xffffffff);
    		break;
    	case THEME_WOOD:
    		mText.setBackgroundResource(R.drawable.bgwood);
    		mText.setShadowLayer(2.0f, 1.0f, 1.0f, 0xff000000);
    		mText.setTextColor(0xffffffff);
    		break;
    	case THEME_PAPER:
    		mText.setBackgroundResource(R.drawable.bgpaper);
    		mText.setShadowLayer(1.0f, 1.0f, 1.0f, 0xffffffff);
    		mText.setTextColor(0xff000000);
    		break;
    	case THEME_CUTE:
    		mText.setBackgroundResource(R.drawable.bgcute);
    		mText.setShadowLayer(2.0f, 0.0f, 0.0f, 0xffff77ff);
    		mText.setTextColor(0xffffffff);
    		break;
    	}
    }
    
    public void setQuteFont(int id, int size) {
        if(mText == null) {
        	if(DEBUG) Log.e("QuteEditor.setFont","mText is null");
        	return;
        }
        mText.setTextSize(size);
    	Typeface font = null;
    	switch(id) {
    	case FONT_COSMETICA:
            font = Typeface.createFromAsset(getAssets(), "mgopencosmeticaregular.ttf");
            mText.setTypeface(font);
            break;
    	case FONT_JUNICODE:
            font = Typeface.createFromAsset(getAssets(), "junicoderegular.ttf");  
            mText.setTypeface(font);
            break;
    	case FONT_UBUNTU:
            font = Typeface.createFromAsset(getAssets(), "ubunturegular.ttf");  
            mText.setTypeface(font);
            break;
    	case FONT_GENTIUM:
            font = Typeface.createFromAsset(getAssets(), "genbkbasr.ttf");  
            mText.setTypeface(font);
            break;
        }
    }
}
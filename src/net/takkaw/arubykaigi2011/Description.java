package net.takkaw.arubykaigi2011;

import java.io.IOException;
import java.io.InputStream;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.util.Linkify;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class Description extends ListActivity {

	private static int[] TO = { R.id.desc_day, R.id.desc_room, R.id.desc_start,
			R.id.desc_end, R.id.desc_title, R.id.desc_speaker, R.id.desc_desc,
			R.id.desc_lang ,R.id.desc_bio ,R.id.desc_gravatar};

	static DBHelper dbHelper;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// setContentView(R.layout.main);
		
		int id = getIntent().getIntExtra("id", 0);
		dbHelper = new DBHelper(this);
		Cursor cursor = dbHelper.idSearch(id);
		SimpleCursorAdapter adapter = new SimpleCursorAdapter(this,	R.layout.desc, cursor, DBHelper.FROM, TO){
			@Override
			public void setViewText(TextView view, String text) {
		        view.setAutoLinkMask(Linkify.WEB_URLS);
		        view.setText(text);
		    }
			
			@Override
			public void bindView(View v, Context context, Cursor cursor) {
				// TODO Auto-generated method stub
				super.bindView(v, context, cursor);
				LinearLayout linearLayoutFavorite = (LinearLayout)v.findViewById(R.id.item_favoriteLayout);
				
				final ToggleButton toggleButtonFavolite = (ToggleButton)v.findViewById(R.id.item_favorite);
				if (cursor.getInt(cursor.getColumnIndex("favorite")) == 1){
					toggleButtonFavolite.setChecked(true);
				}else{
					toggleButtonFavolite.setChecked(false);
				}
				final int id = cursor.getInt(cursor.getColumnIndex("_id")); 
				linearLayoutFavorite.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						Boolean value = !toggleButtonFavolite.isChecked();
						toggleButtonFavolite.setChecked(value);
						Description.dbHelper.updateFavorite(id, value);
					}
				});
				toggleButtonFavolite.setOnClickListener(new OnClickListener(){
					@Override
					public void onClick(View v) {
						Boolean value = ((ToggleButton) v).isChecked();
						Description.dbHelper.updateFavorite(id, value);
					}
				});
			}

			@Override
			public void setViewImage(ImageView view, String text){
				Log.v("gravatarDebug",text);
				try {
					Bitmap bm;
					if(text.equals("")) {
						bm = null;
					}
					else {
						InputStream i = getResources().getAssets().open(text+".jpeg");
						bm = BitmapFactory.decodeStream(i);
					}
					view.setImageBitmap(bm);					
				} catch (IOException e) {
					Log.v("gravatarError",text+".jpeg");
				}
			}
		};

		setListAdapter(adapter);
		dbHelper.close();

		getListView().setClickable(false);
		getListView().setFocusable(false);
		getListView().setFocusableInTouchMode(false);
		// Toast.makeText(this, Integer.toString(id),
		// Toast.LENGTH_SHORT).show();
	}

	// Menu
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.desc_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_share:
			TextView text = (TextView) findViewById(R.id.desc_title);
			String str = text.getText().toString() + " #rubykaigi";
			Intent intent = new Intent(Intent.ACTION_SEND);
			intent.putExtra(Intent.EXTRA_TEXT, str);
			intent.setType("text/plain");
			try {
				startActivity(intent);
			} catch (android.content.ActivityNotFoundException e) {
				Toast.makeText(this, "Intent failed.", Toast.LENGTH_SHORT)
						.show();
			}
			return true;
		}
		return false;
	}
}

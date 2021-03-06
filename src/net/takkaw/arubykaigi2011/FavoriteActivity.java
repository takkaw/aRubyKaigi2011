package net.takkaw.arubykaigi2011;

import java.util.Locale;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CursorAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ToggleButton;

public class FavoriteActivity extends Activity implements OnItemClickListener {

	private static DBHelper dbHelper;
	private static ListView list_view;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// No Window
		// requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN); 
		
		setContentView(R.layout.favorite);

		dbHelper = new DBHelper(this);

		list_view = (ListView) findViewById(R.id.list);
		update_list();
		list_view.setEmptyView(findViewById(R.id.empty));
		list_view.setOnItemClickListener(this);		
	}

	private void update_list() {
		
		Cursor cursor = dbHelper.getFavoriteCursor();
		startManagingCursor(cursor);
		CustomAdapter adapter = new CustomAdapter(this, cursor);
		list_view.setAdapter(adapter);
	}
	
	public void requery(){
		Cursor cursor = dbHelper.getFavoriteCursor();
		CustomAdapter adapter = (CustomAdapter) list_view.getAdapter();
		adapter.changeCursor(cursor);
		adapter.notifyDataSetChanged();
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
		Intent intent = new Intent(this, Description.class);
		intent.putExtra("id", (int) id);
		startActivity(intent);
	}

	class CustomAdapter extends CursorAdapter{
		public CustomAdapter(Context context, Cursor c) {
			super(context, c);
		}

		@Override
		public void bindView(View v, Context context, Cursor cursor) {
			TextView textViewDay = (TextView)v.findViewById(R.id.item_day);
			TextView textViewRoom = (TextView)v.findViewById(R.id.item_room);
			TextView textViewStart = (TextView)v.findViewById(R.id.item_start);
			TextView textViewEnd = (TextView)v.findViewById(R.id.item_end);
			TextView textViewTitle = (TextView)v.findViewById(R.id.item_title);
			TextView textViewSpeaker = (TextView)v.findViewById(R.id.item_speaker);

			String day = cursor.getString(cursor.getColumnIndex("day"));
			String start = cursor.getString(cursor.getColumnIndex("start"));
			String end = cursor.getString(cursor.getColumnIndex("end"));
			textViewDay.setText(day);
			textViewStart.setText(start);
			textViewEnd.setText(end);
			
			String room = "";
			String desc = "";
			String speaker = "";
			String speakerBio = "";
			if (Locale.getDefault().equals(Locale.JAPANESE) || Locale.getDefault().equals(Locale.JAPAN)){
				room = cursor.getString(cursor.getColumnIndex("room_ja"));
				desc = cursor.getString(cursor.getColumnIndex("desc_ja"));
				speaker = cursor.getString(cursor.getColumnIndex("speaker_ja"));
				speakerBio = cursor.getString(cursor.getColumnIndex("speaker_bio_ja"));
				textViewTitle.setText(cursor.getString(cursor.getColumnIndex("title_ja")));
			} else {
				room = cursor.getString(cursor.getColumnIndex("room_en"));
				desc = cursor.getString(cursor.getColumnIndex("desc_ja"));
				speaker = cursor.getString(cursor.getColumnIndex("speaker_en"));
				speakerBio = cursor.getString(cursor.getColumnIndex("speaker_bio_en"));
				textViewTitle.setText(cursor.getString(cursor.getColumnIndex("title_en")));
			}
			textViewRoom.setText(room);
			textViewSpeaker.setText(speaker);
			LinearLayout subLauout = (LinearLayout)v.findViewById(R.id.item_subLayout);
			if ((room == null || room.length() == 0) && (speaker == null || speaker.length() == 0)){
				subLauout.setVisibility(View.GONE);
			} else {
				subLauout.setVisibility(View.VISIBLE);
			}
			
			
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
					FavoriteActivity.dbHelper.updateFavorite(id, value);
					FavoriteActivity.this.requery();
				}
			});
			toggleButtonFavolite.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View v) {
					Boolean value = ((ToggleButton) v).isChecked();
					FavoriteActivity.dbHelper.updateFavorite(id, value);
					FavoriteActivity.this.requery();
				}
			});

			if ((desc == null || desc.length() == 0) && (speakerBio == null || speakerBio.length() == 0)){
				v.setClickable(true);
				linearLayoutFavorite.setVisibility(View.INVISIBLE);
			} else {
				v.setClickable(false);
				linearLayoutFavorite.setVisibility(View.VISIBLE);
			}
			
			LinearLayout timeLayout = (LinearLayout)v.findViewById(R.id.item_timeLayout);
			if (cursor.moveToPrevious()){
				if (day.equals(cursor.getString(cursor.getColumnIndex("day"))) 
						&& start.equals(cursor.getString(cursor.getColumnIndex("start"))) 
						&& end.equals(cursor.getString(cursor.getColumnIndex("end")))){
					timeLayout.setVisibility(View.GONE);
				}else{
					timeLayout.setVisibility(View.VISIBLE);
				}
			}else {
				timeLayout.setVisibility(View.VISIBLE);
			}
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			View v = FavoriteActivity.this.getLayoutInflater().inflate(R.layout.item, null);
			return v;
		}		
	}
	
}
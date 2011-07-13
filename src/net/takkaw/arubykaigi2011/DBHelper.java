package net.takkaw.arubykaigi2011;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {
    	private final static String DB_NAME = "RubyKaigi2011.db";
    	private final static String DB_TABLE = "RubyKaigi2011";		
    	private final static int DB_VERSION = 1;
    	private final static String DB_PATH = "/data/data/net.takkaw.arubykaigi2011/databases/";
    	
    	private static Context c;
    	private static Resources res;
    	private Cursor cursor = null;
    	private boolean createDatabase = false;

    	private static final String DAY = "day";
    	private static final String ROOM = "room_en";
    	private static final String START = "start";
    	private static final String END = "end";
    	private static final String TITLE = "title_en";
    	private static final String SPEAKER = "speaker_en";
    	private static final String DESC = "desc_en";
    	private static final String LANG = "lang";
    	private static final String BIO = "speaker_bio_en";
		
		public static String[] FROM = { DAY, ROOM, START, END, TITLE, SPEAKER, DESC, LANG, BIO };
		
		public void makeCursorFrom(Resources res){
			FROM[1] = res.getString(R.string.room);
			FROM[4] = res.getString(R.string.title);
			FROM[5] = res.getString(R.string.speaker);
			FROM[6] = res.getString(R.string.desc);
			FROM[8] = res.getString(R.string.bio);
		}

		public DBHelper(Context context) {
			super(context, DB_NAME, null, DB_VERSION);
			c = context;
			res = c.getResources();
			makeCursorFrom(res);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			createDatabase = true;
		}
		
		private SQLiteDatabase createDB(SQLiteDatabase db) {
			db.close();

			try {
				InputStream mInput = c.getAssets().open("RubyKaigi2011.db");
				OutputStream mOutput = new FileOutputStream(DB_PATH + DB_NAME);
				byte[] buffer = new byte[1024];
				int size;
				while ((size = mInput.read(buffer)) > 0){
					mOutput.write(buffer,0,size);
				}
				mOutput.flush();
				mOutput.close();
				mInput.close();
			}
			catch (IOException e){
				/* Nothing to do */
			}
			
			createDatabase = false;
			
			return super.getReadableDatabase();
		}

		@Override
		public synchronized SQLiteDatabase getReadableDatabase(){
			SQLiteDatabase db = super.getReadableDatabase();

			if( createDatabase == true ){
				db = createDB(db);
			}
			
			return db;
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL("drop table if exists "+DB_TABLE);
//			onCreate(db);
		}
    	
		public Cursor formSearch(String day, String room, String lang,String keyword){
			
			if( this.cursor != null ) this.cursor.close();
			
			StringBuffer sql = new StringBuffer();

			if( day != null ){				
				sql.append( ( sql.length() == 0 ) ? " where " : " and " );
				sql.append( String.format("day like '%s'", day));
			}
			if( room != null ){
				sql.append( ( sql.length() == 0 ) ? " where " : " and " ); 
				sql.append( String.format("%s like '%s'", res.getString(R.string.room),room));
			}
			if( lang != null ){
				sql.append( ( sql.length() == 0 ) ? " where " : " and " ); 
				sql.append( String.format("lang like '%%%s%%'", lang));
			}
			if( keyword != null ){
				sql.append( ( sql.length() == 0 ) ? " where (" : " and (" ); 
				sql.append(
					String.format("%s like '%%%s%%' or %s like '%%%s%%' or %s like '%%%s%%' or %s like '%%%s%%')",
						res.getString(R.string.title),keyword,
						res.getString(R.string.speaker),keyword,
						res.getString(R.string.desc),keyword,
						res.getString(R.string.bio),keyword
					)
				);
			}
			sql.append(" order by day,start");
			sql.insert(0, "select * from " + DB_TABLE);
			
			String str_sql = sql.toString();
			
			SQLiteDatabase db = this.getReadableDatabase();
			
			this.cursor = db.rawQuery(str_sql,null);
			return cursor;
		}
		
		public Cursor idSearch( int id ){
			SQLiteDatabase db = this.getReadableDatabase();
			String str_sql = String.format("select * from %s where _id like %s",DB_TABLE,Integer.toString(id));
			Cursor cursor = db.rawQuery(str_sql, null);
			return cursor;
		}
		
    }
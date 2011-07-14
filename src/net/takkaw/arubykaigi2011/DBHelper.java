package net.takkaw.arubykaigi2011;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {
    	private final static String DB_NAME = "RubyKaigi2011.db";
    	public final static String DB_TABLE = "RubyKaigi2011";		
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
    	private static final String GRAVATAR = "gravatar";
    	private static final String FAVORITE = "favorite";
		
		public static String[] FROM = { DAY, ROOM, START, END, TITLE, SPEAKER, DESC, LANG, BIO, GRAVATAR, FAVORITE };

		private static final String CREATE_FAVORITE_TABLE_SQL = 
			"create table favorites "
				+ "( id        integer, "
				+ "  favorite  integer )";	
		
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
//			db.execSQL(CREATE_FAVORITE_TABLE_SQL);
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
			db = super.getWritableDatabase();
			db.execSQL(CREATE_FAVORITE_TABLE_SQL);
			db.close();
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
			if (newVersion == 2) db.execSQL(CREATE_FAVORITE_TABLE_SQL);
//			onCreate(db);
		}
    	
		static String SQL_SELECT = "select * from " + DB_TABLE + " left outer join favorites on favorites.id = " + DB_TABLE + "._id";
		
		public Cursor formSearch(String day, String room, String lang,String keyword){
			
			if( this.cursor != null ) this.cursor.close();
			
			StringBuffer sql = new StringBuffer();

			if( day != null ){				
				sql.append( ( sql.length() == 0 ) ? " where " : " and " );
				sql.append( String.format("%s.day like '%s'", DB_TABLE,day));
			}
			if( room != null ){
				sql.append( ( sql.length() == 0 ) ? " where " : " and " ); 
				sql.append( String.format("%s.%s like '%s'", DB_TABLE,res.getString(R.string.room),room));
			}
			if( lang != null ){
				sql.append( ( sql.length() == 0 ) ? " where " : " and " ); 
				sql.append( String.format("%s.lang like '%%%s%%'", DB_TABLE,lang));
			}
			if( keyword != null ){
				sql.append( ( sql.length() == 0 ) ? " where (" : " and (" ); 
				sql.append(
					String.format("%s.%s like '%%%s%%' or %s.%s like '%%%s%%' or %s.%s like '%%%s%%' or %s.%s like '%%%s%%')",
							DB_TABLE,res.getString(R.string.title),keyword,
							DB_TABLE,res.getString(R.string.speaker),keyword,
							DB_TABLE,res.getString(R.string.desc),keyword,
							DB_TABLE,res.getString(R.string.bio),keyword
					)
				);
			}
			sql.append(String.format(" order by %s.day,%s.start",DB_TABLE,DB_TABLE));
			sql.insert(0, SQL_SELECT);
			
			String str_sql = sql.toString();
			SQLiteDatabase db = this.getReadableDatabase();
			this.cursor = db.rawQuery(str_sql,null);
			return cursor;
		}
		
		public Cursor getFavoriteCursor(){
			SQLiteDatabase db = this.getReadableDatabase();
			StringBuilder sql = new StringBuilder();
			sql.append(SQL_SELECT);
			sql.append(" where favorites.favorite = 1");
			sql.append(String.format(" order by %s.day,%s.start",DB_TABLE,DB_TABLE));
			return db.rawQuery(sql.toString(), null);
		}
		
		public Cursor idSearch( int id ){
			SQLiteDatabase db = this.getReadableDatabase();
			StringBuilder sql = new StringBuilder();
			sql.append(SQL_SELECT);
			sql.append(" where _id = ?");
			sql.append(String.format(" order by %s.day,%s.start",DB_TABLE,DB_TABLE));
			String[] args = new String[] {Integer.toString(id)};
			Cursor cursor = db.rawQuery(sql.toString(), args);
			return cursor;
		}
		
		public void updateFavorite(int id, boolean value){
			ContentValues values = new ContentValues();
			int favorite = 0;
			if(value) favorite = 1;
			values.put("id", id);
			values.put("favorite", favorite);
					
			String whereClause = "id = ?";
			String[] whereArgs = {String.valueOf(id)};
			
			SQLiteDatabase db = this.getWritableDatabase();
			db.delete("favorites", whereClause, whereArgs);
			db.insert("favorites", null, values);
			db.close();
		}
    }
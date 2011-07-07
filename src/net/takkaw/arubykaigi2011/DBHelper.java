package net.takkaw.arubykaigi2011;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {
    	private final static String DB_NAME = "RubyKaigi2011.db";
    	private final static String DB_TABLE = "RubyKaigi2011";
    	private final static int DB_VERSION = 1;
    	private final static String DB_PATH = "/data/data/net.takkaw.arubykaigi2011/databases/";
    	
    	private static Context c;
    	private SQLiteDatabase db = null; 
    	private boolean createDatabase = false;
    	
		public DBHelper(Context context) {
			super(context, DB_NAME, null, DB_VERSION);
			c = context;
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
//			createDB(db);
			createDatabase = true;
		}
		
		private SQLiteDatabase createDB(SQLiteDatabase db) {
			String db_path = DB_PATH + DB_NAME;

			db.close();

			try {
				InputStream mInput = c.getAssets().open("RubyKaigi2011ja.db");
				OutputStream mOutput = new FileOutputStream(db_path);
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

		public void reCreateDB(){
			db = this.getWritableDatabase();
			db.execSQL("drop table if exists "+DB_TABLE);
			createDatabase = true;
			createDB(db);
			db.close();
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
			StringBuffer sql = new StringBuffer();

			if( day != null ){				
				sql.append( ( sql.length() == 0 ) ? " where " : " and " );
				sql.append( String.format("day like '%s'", day));
			}
			if( room != null ){
				sql.append( ( sql.length() == 0 ) ? " where " : " and " ); 
				sql.append( String.format("room like '%s'", room));
			}
			if( lang != null ){
				sql.append( ( sql.length() == 0 ) ? " where " : " and " ); 
				sql.append( String.format("lang like '%s'", lang));
			}
			if( keyword != null ){
				sql.append( ( sql.length() == 0 ) ? " where (" : " and (" ); 
				sql.append(String.format("title like '%%%s%%' or speaker like '%%%s%%' or desc like '%%%s%%' )",keyword,keyword,keyword));
			}
			sql.append(" order by day,start");
			sql.insert(0, "select * from " + DB_TABLE);
			
			String str_sql = sql.toString();
			
			db = this.getReadableDatabase();
			
			Cursor cursor = db.rawQuery(str_sql,null);
			return cursor;
		}
		
		public Cursor idSearch( int id ){
			db = this.getReadableDatabase();
			String str_sql = String.format("select * from %s where _id like %s",DB_TABLE,Integer.toString(id));
			Cursor cursor = db.rawQuery(str_sql, null);
			return cursor;
		}
				
		public void close(){
			if( db != null ){
				db.close();
				db = null;
			}
		}
		
    }
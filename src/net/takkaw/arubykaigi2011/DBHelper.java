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
			sql.append("select * from ");
			sql.append( DB_TABLE );
			boolean first = false;
			if( day != null ){
				if( first == false ) sql.append(" where ");
				else sql.append(" and ");
				sql.append("day like '");
				sql.append(day);
				sql.append("'");
				first = true;
			}
			if( room != null ){
				if( first == false )sql.append(" where ");
				else sql.append(" and ");
				sql.append("room like '");
				sql.append(room);
				sql.append("'");
				first = true;
			}
			if( lang != null ){
				if( first == false )sql.append(" where ");
				else sql.append(" and ");
				sql.append("lang like '%");
				sql.append(lang);
				sql.append("%'");
				first = true;
			}
			if( keyword != null ){
				if( first == false )sql.append(" where (");
				else sql.append(" and (");
				sql.append("title like '%");
				sql.append(keyword);
				sql.append("%' or ");
				sql.append("speaker like '%");
				sql.append(keyword);
				sql.append("%' or ");
				sql.append("desc like '%");
				sql.append(keyword);
				sql.append("%' )");
			}
			sql.append(" order by day,start");
			
			String str_sql = sql.toString();
			
			db = this.getReadableDatabase();
			
			Cursor cursor = db.rawQuery(str_sql,null);
			return cursor;
		}
		
		public Cursor idSearch( int id ){
			StringBuffer sql = new StringBuffer();
			sql.append("select * from ");
			sql.append(DB_TABLE);
			sql.append(" where _id like '");
			sql.append(Integer.toString(id));
			sql.append("'");
			String str_sql = sql.toString();
			db = this.getReadableDatabase();
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
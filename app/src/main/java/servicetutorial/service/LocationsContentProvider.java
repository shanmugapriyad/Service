package servicetutorial.service;


import java.sql.SQLException;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;

/** A custom Content Provider to do the database operations */
public class LocationsContentProvider extends ContentProvider{

    public static final String PROVIDER_NAME = "servicetutorial.service";

    /** A uri to do operations on locations table. A content provider is identified by its uri */
    public static final Uri CONTENT_URI = Uri.parse("content://" + PROVIDER_NAME + "/locations" );

    /** Constant to identify the requested operation */
    private static final int LOCATIONS = 1;

    private static final UriMatcher uriMatcher ;

    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(PROVIDER_NAME, "locations", LOCATIONS);
    }

    /** This content provider does the database operations by this object */
    LocationsDB mLocationsDB;

    /** A callback method which is invoked when the content provider is starting up */
    @Override
    public boolean onCreate() {
        mLocationsDB = new LocationsDB(getContext());
        return true;
    }

    /** A callback method which is invoked when insert operation is requested on this content provider */
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        long rowID = mLocationsDB.insert(values);
        Uri _uri=null;
        if(rowID>0){
            _uri = ContentUris.withAppendedId(CONTENT_URI, rowID);
        }else {
            try {
                throw new SQLException("Failed to insert : " + uri);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return _uri;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
// TODO Auto-generated method stub
        return 0;
    }

    /** A callback method which is invoked when delete operation is requested on this content provider */
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int cnt = 0;
        cnt = mLocationsDB.del();
        return cnt;
    }

    /** A callback method which is invoked by default content uri */
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

        if(uriMatcher.match(uri)==LOCATIONS){
            return mLocationsDB.getAllLocations();
        }
        return null;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    public static class LocationsDB extends SQLiteOpenHelper {

        /** Database name */
        private static String DBNAME = "locationmarkersqlite";

        /** Version number of the database */
        private static int VERSION = 1;

        /** Field 1 of the table locations, which is the primary key */
        public static final String FIELD_ROW_ID = "_id";

        /** Field 2 of the table locations, stores the latitude */
        public static final String FIELD_LAT = "lat";

        /** Field 3 of the table locations, stores the longitude*/
        public static final String FIELD_LNG = "lng";

        /** Field 4 of the table locations, stores the zoom level of map*/
        public static final String FIELD_ZOOM = "zom";

        /** A constant, stores the the table name */
        private static final String DATABASE_TABLE = "locations";

        /** An instance variable for SQLiteDatabase */
        private SQLiteDatabase mDB;

        /** Constructor */
        public LocationsDB(Context context) {
            super(context, DBNAME, null, VERSION);
            this.mDB = getWritableDatabase();
        }

        /** This is a callback method, invoked when the method getReadableDatabase() / getWritableDatabase() is called
         * provided the database does not exists
         * */
        @Override
        public void onCreate(SQLiteDatabase db) {
            String sql = "create table " + DATABASE_TABLE + " ( " +
                    FIELD_ROW_ID + " integer primary key autoincrement , " +
                    FIELD_LNG + " double , " +
                    FIELD_LAT + " double , " +
                    FIELD_ZOOM + " text " +
                    " ) ";

            db.execSQL(sql);
        }

        /** Inserts a new location to the table locations */
        public long insert(ContentValues contentValues){
            long rowID = mDB.insert(DATABASE_TABLE, null, contentValues);
            return rowID;
        }

        /** Deletes all locations from the table */
        public int del(){
            int cnt = mDB.delete(DATABASE_TABLE, null , null);
            return cnt;
        }

        /** Returns all the locations from the table */
        public Cursor getAllLocations(){
            return mDB.query(DATABASE_TABLE, new String[] { FIELD_ROW_ID, FIELD_LAT , FIELD_LNG, FIELD_ZOOM } , null, null, null, null, null);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        }

    }
}

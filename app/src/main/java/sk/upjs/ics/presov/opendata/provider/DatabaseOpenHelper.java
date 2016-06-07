package sk.upjs.ics.presov.opendata.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.Set;

import static sk.upjs.ics.presov.opendata.Defaults.DEFAULT_CURSOR_FACTORY;
import static sk.upjs.ics.presov.opendata.provider.Provider.Columns;
import static sk.upjs.ics.presov.opendata.provider.Provider.Rows;

public class DatabaseOpenHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "opendata";

    public static final int DATABASE_VERSION = 1;

    public DatabaseOpenHelper(Context context) {
        super(context, DATABASE_NAME, DEFAULT_CURSOR_FACTORY, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // do nothing
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // do nothing
    }

    public void dropRowsTable(SQLiteDatabase db, long id) {
        String sqlTemplate = "DROP TABLE IF EXISTS %s";
        String sqlQuery = String.format(sqlTemplate, Rows.rowsTableName(id));
        db.execSQL(sqlQuery);
    }

    public void dropColumnsTable(SQLiteDatabase db, long id) {
        String sqlTemplate = "DROP TABLE IF EXISTS %s";
        String sqlQuery = String.format(sqlTemplate, Columns.columnsTableName(id));
        db.execSQL(sqlQuery);
    }

    public void createRowsTable(SQLiteDatabase db, long id, Set<String> columns) {
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE VIRTUAL TABLE IF NOT EXISTS ")
                .append(Rows.rowsTableName(id))
                .append(" USING fts3( ")
                .append(Rows._ID)
                .append(" INTEGER PRIMARY KEY AUTOINCREMENT");
        for (String column : columns) {
            sb.append(", ")
                    .append(Rows.columnName(column))
                    .append(" TEXT");
        }
        sb.append(" )");
        db.execSQL(sb.toString());
    }

    public void createColumnsTable(SQLiteDatabase db, long id) {
        String sqlTemplate = "CREATE TABLE IF NOT EXISTS %s ( " +
                "%s INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "%s TEXT, " +
                "%s TEXT " +
                ")";
        String sqlQuery = String.format(sqlTemplate, Columns.columnsTableName(id), Columns._ID, Columns.KEY, Columns.VALUE);
        db.execSQL(sqlQuery);
    }
}

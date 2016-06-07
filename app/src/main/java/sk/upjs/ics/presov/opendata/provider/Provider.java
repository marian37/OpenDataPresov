package sk.upjs.ics.presov.opendata.provider;

import android.database.DatabaseUtils;
import android.net.Uri;
import android.provider.BaseColumns;

import java.util.List;

import static sk.upjs.ics.presov.opendata.provider.DatasetContentProvider.COLUMNS;
import static sk.upjs.ics.presov.opendata.provider.DatasetContentProvider.ROWS;

public interface Provider {

    public final class Columns implements BaseColumns {

        public static final String KEY = "key";

        public static final String VALUE = "value";

        public static String columnsTableName(long id) {
            return String.format("dataset%1$dcolumns", id);
        }

        public static String columnsTableName(Uri uri) {
            List<String> pathSegments = uri.getPathSegments();
            if (COLUMNS.equals(pathSegments.get(0))) {
                return columnsTableName(Long.parseLong(pathSegments.get(1)));
            }

            return null;
        }

    }

    public final class Rows implements BaseColumns {

        public static String rowsTableName(long id) {
            return String.format("dataset%1$drows", id);
        }

        public static String rowsTableName(Uri uri) {
            List<String> pathSegments = uri.getPathSegments();
            if (ROWS.equals(pathSegments.get(0))) {
                return rowsTableName(Long.parseLong(pathSegments.get(1)));
            }

            return null;
        }

        public static String columnName(String columnName) {
            return DatabaseUtils.sqlEscapeString(columnName);
        }

        public static String columnName(int number) {
            return String.format("col_%1$d", number);
        }
    }
}

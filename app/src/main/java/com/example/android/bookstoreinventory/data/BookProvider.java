package com.example.android.bookstoreinventory.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import com.example.android.bookstoreinventory.data.BookContract.BookEntry;

/**
 * {@link ContentProvider} for Book Store app.
 */

public class BookProvider extends ContentProvider {

    /** Database helper object */
    private BookDbHelper mDbHelper;

    /** Tag for the log messages */
    public static final String LOG_TAG = BookProvider.class.getSimpleName();

    /** URI matcher code for the content URI for the products table */
    private static final int PRODUCTS = 100;

    /** URI matcher code for the content URI for a single product in the products table */
    private static final int PRODUCT_ID = 101;

    /**
     * UriMatcher object to match a content URI to a corresponding code.
     * The input passed into the constructor represents the code to return for the root URI.
     * It's common to use NO_MATCH as the input for this case.
     */
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    // Static initializer. This is run the first time anything is called from this class.
    static {
        // The calls to addURI() go here, for all of the content URI patterns that the provider
        // should recognize. All paths added to the UriMatcher have a corresponding code to return
        // when a match is found.
        sUriMatcher.addURI(BookContract.CONTENT_AUTHORITY, BookContract.PATH_PRODUCTS, PRODUCTS);
        sUriMatcher.addURI(BookContract.CONTENT_AUTHORITY, BookContract.PATH_PRODUCTS + "/#", PRODUCT_ID);
    }

    /**
    * Initialize the provider and the database helper object.
    */
    @Override
    public boolean onCreate() {
        mDbHelper = new BookDbHelper(getContext());
        return true;
    }

    /**
     * Perform the query for the given URI. Use the given projection, selection, selection arguments,
     * and sort order.
     */
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        // Get readable database
        SQLiteDatabase database = mDbHelper.getReadableDatabase();

        // This cursor will hold the result of the query
        Cursor cursor;

        // Figure out if the URI matcher can match the URI to a specific code
        int match = sUriMatcher.match(uri);
        switch (match) {
            case PRODUCTS:
                cursor = database.query(BookEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            case PRODUCT_ID:
                selection = BookEntry._ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };
                cursor = database.query(BookEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Cannot query unknown URI " + uri);
        }

        // Set notification URI on the Cursor,
        // so we know what content URI the Cursor was created for.
        // If the data at this URI changes, then we know we need to update the Cursor.
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    /**
     * Insert new data into the provider with the given ContentValues.
     */
    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PRODUCTS:
                return insertProduct(uri, contentValues);
            default:
                throw new IllegalArgumentException("Insertion is not supported for " + uri);
        }
    }

    /**
     * Insert a product into the database with the given content values. Return the new content URI
     * for that specific row in the database.
     */
    private Uri insertProduct(Uri uri, ContentValues values) {
        // Check that the product name is not null
        String productName = values.getAsString(BookEntry.COLUMN_PRODUCT_NAME);
        if (productName == null || productName.length() == 0) {
            throw new IllegalArgumentException("Product requires a name");
        }
        // If the price is provided, check that it's greater than or equal to 0
        Integer price = values.getAsInteger(BookEntry.COLUMN_PRICE);
        if (price == null && price < 0) {
            throw new IllegalArgumentException("Product requires valid price");
        }
        // If the quantity is provided, check that it's greater than or equal to 0
        Integer quantity = values.getAsInteger(BookEntry.COLUMN_QUANTITY);
        if (quantity == null && quantity < 0) {
            throw new IllegalArgumentException("Product requires valid quantity");
        }

        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        // Insert the new row, returning the primary key value of the new row
        long id = database.insert(BookEntry.TABLE_NAME, null, values);

        if (id == -1) {
            Log.e(LOG_TAG, "Failed to insert row for " + uri);
            return null;
        }

        // Notify all listeners that the data has changed for the product content URI
        getContext().getContentResolver().notifyChange(uri, null);

        return ContentUris.withAppendedId(uri, id);
    }

        /**
         * Updates the data at the given selection and selection arguments, with the new ContentValues.
         */
        @Override
        public int update(Uri uri, ContentValues contentValues, String selection,
                String[] selectionArgs) {
            final int match = sUriMatcher.match(uri);
            switch (match) {
                case PRODUCTS:
                    return updateProduct(uri, contentValues, selection, selectionArgs);
                case PRODUCT_ID:
                    selection = BookEntry._ID + "=?";
                    selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };
                    return updateProduct(uri, contentValues, selection, selectionArgs);
                default:
                    throw new IllegalArgumentException("Update is not supported for " + uri);
            }
        }

        /**
         * Update products in the database with the given content values. Apply the changes to the rows
         * specified in the selection and selection arguments (which could be 0 or 1 or more products).
         * Return the number of rows that were successfully updated.
         */
        private int updateProduct(Uri uri, ContentValues values, String selection, String[] selectionArgs) {

            String name = values.getAsString(BookEntry.COLUMN_PRODUCT_NAME);
            Integer price = values.getAsInteger(BookEntry.COLUMN_PRICE);
            Integer quantity = values.getAsInteger(BookEntry.COLUMN_QUANTITY);

            /** Sanity check */
            if (values.containsKey(BookContract.BookEntry.COLUMN_PRODUCT_NAME)) {
                if (name == null || name.length() == 0) {
                    throw new IllegalArgumentException("Product requires a name");
                }
            }
            if (values.containsKey(BookEntry.COLUMN_PRICE)) {
                if (price != null && price < 0) {
                    throw new IllegalArgumentException("Product requires valid price");
                }
            }
            if (values.containsKey(BookEntry.COLUMN_QUANTITY)) {
                if (quantity != null && quantity < 0) {
                    throw new IllegalArgumentException("Product requires valid quantity");
                }
            }

            if (values.size() == 0) {
                return 0;
            }

            SQLiteDatabase database = mDbHelper.getWritableDatabase();

            // Perform the update on the database and get the number of rows affected
            int rowsUpdated = database.update(BookEntry.TABLE_NAME, values, selection, selectionArgs);

            // If 1 or more rows were updated, then notify all listeners that the data at the
            // given URI has changed
            if (rowsUpdated != 0) {
                getContext().getContentResolver().notifyChange(uri, null);
            }

            // Return the number of rows updated
            return rowsUpdated;
        }

        /**
        * Delete the data at the given selection and selection arguments.
        */
        @Override
        public int delete(Uri uri, String selection, String[] selectionArgs) {
            // Get readable database
            SQLiteDatabase database = mDbHelper.getWritableDatabase();

            // Track the number of rows that were deleted
            int rowsDeleted;

            final int match = sUriMatcher.match(uri);
            switch (match) {
                case PRODUCTS:
                    // Delete all rows that match the selection and selection args
                    rowsDeleted = database.delete(BookEntry.TABLE_NAME, selection, selectionArgs);
                    break;
                case PRODUCT_ID:
                    // Delete a single row given by the ID in the URI
                    selection = BookEntry._ID + "=?";
                    selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                    rowsDeleted = database.delete(BookEntry.TABLE_NAME, selection, selectionArgs);
                    break;
                default:
                    throw new IllegalArgumentException("Deletion is not supported for " + uri);
            }

            // If 1 or more rows were deleted, then notify all listeners that the data at the
            // given URI has changed
            if (rowsDeleted != 0) {
                getContext().getContentResolver().notifyChange(uri, null);
            }

            // Return the number of rows deleted
            return rowsDeleted;
        }

    /**
     * Returns the MIME type of data for the content URI.
     */
    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PRODUCTS:
                return BookEntry.CONTENT_LIST_TYPE;
            case PRODUCT_ID:
                return BookEntry.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalStateException("Unknown URI " + uri + " with match " + match);
        }
    }
}
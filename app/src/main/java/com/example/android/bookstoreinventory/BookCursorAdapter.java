package com.example.android.bookstoreinventory;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import com.example.android.bookstoreinventory.data.BookContract;
import com.example.android.bookstoreinventory.data.BookDbHelper;

/**
 * {@link BookCursorAdapter} is an adapter for a list or grid view
 * that uses a {@link Cursor} of books data as its data source. Adapter knows
 * how to create list items for each row of book data in the {@link Cursor}.
 */
public class BookCursorAdapter extends CursorAdapter {

    /**
     * Constructs a new {@link BookCursorAdapter}.
     *
     * @param context The context
     * @param c       The cursor from which to get the data.
     */
    public BookCursorAdapter(Context context, Cursor c) {
        super(context, c, 0 /* flags */);
    }

    /**
     * Makes a new blank list item view. No data is set (or bound) to the views yet.
     *
     * @param context app context
     * @param cursor  The cursor from which to get the data. The cursor is already
     *                moved to the correct position.
     * @param parent  The parent to which the new view is attached to
     * @return the newly created list item view.
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
    }

    /**
     * This method binds the book data (in the current row pointed to by cursor) to the given
     * list item layout. For example, the name for the current book can be set on the name TextView
     * in the list item layout.
     *
     * @param view    Existing view, returned earlier by newView() method
     * @param context app context
     * @param cursor  The cursor from which to get the data. The cursor is already moved to the
     *                correct row.
     */
    @Override
    public void bindView(View view, final Context context, Cursor cursor) {
        // Find fields to populate in inflated template
        TextView productNameTextView = (TextView) view.findViewById(R.id.product_name);
        TextView priceTextView = (TextView) view.findViewById(R.id.price);
        final TextView quantityTextView = (TextView) view.findViewById(R.id.quantity);
        Button saleButton = (Button) view.findViewById(R.id.sale_button);

        // Extract properties from cursor
        int productNameIndex = cursor.getColumnIndex(BookContract.BookEntry.COLUMN_PRODUCT_NAME);
        int priceIndex = cursor.getColumnIndex(BookContract.BookEntry.COLUMN_PRICE);
        int quantityIndex = cursor.getColumnIndex(BookContract.BookEntry.COLUMN_QUANTITY);

        String productNameString = cursor.getString(productNameIndex);
        int priceInt = cursor.getInt(priceIndex);
        final int quantityInt = cursor.getInt(quantityIndex);

        productNameTextView.setText(productNameString);
        priceTextView.setText(" " + priceInt);
        quantityTextView.setText(" " + quantityInt);

        final Uri uri = ContentUris.withAppendedId(BookContract.BookEntry.CONTENT_URI,
                cursor.getInt(cursor.getColumnIndexOrThrow(BookContract.BookEntry._ID)));

        saleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (quantityInt > 0) {
                    Integer afterSale = quantityInt - 1;

                    ContentValues values = new ContentValues();
                    values.put(BookContract.BookEntry.COLUMN_QUANTITY, afterSale);
                    context.getContentResolver().update(uri, values, null, null);

                    quantityTextView.setText(afterSale.toString());
                }
            }
        });
    }
}


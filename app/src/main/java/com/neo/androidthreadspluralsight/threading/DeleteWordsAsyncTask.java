package com.neo.androidthreadspluralsight.threading;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.neo.androidthreadspluralsight.models.Word;
import com.neo.androidthreadspluralsight.persistence.AppDatabase;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * class for just deleting words on background thread
 */
public class DeleteWordsAsyncTask extends AsyncTask<Word, Void, Integer> {
    private static final String TAG = "RetrieveWordsAsyncTask";

    private AppDatabase mDb;

    public DeleteWordsAsyncTask(Context context) {
        mDb = AppDatabase.getDatabase(context);
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected Integer doInBackground(Word... words) {   // on Background thread
        return deleteWordAsync(words[0]);

    }

    @Override
    protected void onProgressUpdate(Void... values) {
        super.onProgressUpdate(values);
    }

    @Override
    protected void onPostExecute(Integer value) {
        super.onPostExecute(value);
    }

    /**
     * Delete word from the db
     * @param word
     * @return
     */
    private Integer deleteWordAsync(Word word){
        Log.d(TAG, "deleteWordAsync: retrieving words, from Thread: " + Thread.currentThread().getName());

        return mDb.wordDataDao().delete(word);              // returns pos of word deleted from the eb
    }
}

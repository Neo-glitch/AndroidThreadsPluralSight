package com.neo.androidthreadspluralsight.threading;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;


import com.neo.androidthreadspluralsight.models.Word;
import com.neo.androidthreadspluralsight.persistence.AppDatabase;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * class for just querying words on background thread
 */
public class RetrieveWordsAsyncTask extends AsyncTask<String, Void, ArrayList<Word>> {
    private static final String TAG = "RetrieveWordsAsyncTask";

    //vars
    private AppDatabase mDb;
    private WeakReference<TaskDelegate> mDelegate;              // to avoid mem leak,since interface will be implemented in an activity

    public RetrieveWordsAsyncTask(Context context, TaskDelegate delegate) {
        mDb = AppDatabase.getDatabase(context);
        mDelegate = new WeakReference<>(delegate);
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected ArrayList<Word> doInBackground(String... strings) {   // on Background thread
        return retrieveWordAsync(strings[0]);
    }

    @Override
    protected void onProgressUpdate(Void... values) {
        super.onProgressUpdate(values);
    }

    @Override
    protected void onPostExecute(ArrayList<Word> words) {
        super.onPostExecute(words);
        mDelegate.get().onWordsRetrieved(words);
    }

    /**
     * method for query the App's Database
     * @param query
     * @return
     */
    private ArrayList<Word> retrieveWordAsync(String query){
        Log.d(TAG, "retrieveWordAsync: retrieving words, from Thread: " + Thread.currentThread().getName());

        return new ArrayList<>(mDb.wordDataDao().getWords(query));
    }
}

package com.neo.androidthreadspluralsight.threading;


import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.neo.androidthreadspluralsight.models.Word;
import com.neo.androidthreadspluralsight.persistence.AppDatabase;
import com.neo.androidthreadspluralsight.util.Constants;

import java.lang.ref.WeakReference;
import java.util.ArrayList;




/**
 * custom runnable for retrieving words from the db using the handlerThread
 */
public class RetrieveWordsRunnable implements Runnable{
    private static final String TAG = "RetrieveWordsRunnable";

    // var
//    private Handler mMainThreadHandler;             // passes msg to main thread when job complete
    private WeakReference<Handler> mMainThreadHandler;              // done this way to avoid mem leaks
    private AppDatabase mDb;
    private String mQuery;                                          // query String, for words to be searched for

    public RetrieveWordsRunnable(Context context, Handler mainThreadHandler, String query) {
        mMainThreadHandler = new WeakReference<>(mainThreadHandler);
        mQuery = query;
        mDb = AppDatabase.getDatabase(context);
    }

    @Override
    public void run() {
        // job or work to be done is added here
        Log.d(TAG, "run: retrieving words. This is from thread: " + Thread.currentThread().getName());
        ArrayList<Word> words = new ArrayList<>(mDb.wordDataDao().getWords(mQuery));              // retrieves a list of words from the db associated with title
        Message message = null;
        if (words.size() > 0) {
            message = Message.obtain(null, Constants.WORDS_RETRIEVE_SUCCESS);
            Bundle bundle = new Bundle();
            bundle.putParcelableArrayList("words_retrieve", words);
            message.setData(bundle);
        } else {
            message = Message.obtain(null, Constants.WORDS_RETRIEVE_FAIL);
        }

        mMainThreadHandler.get().sendMessage(message);

    }
}

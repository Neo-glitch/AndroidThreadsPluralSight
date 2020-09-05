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
 * runnable for deleting words from the db
 */
public class DeleteWordsRunnable implements Runnable{
    private static final String TAG = "DeleteWordsRunnable";

    // var
//    private Handler mMainThreadHandler;             // passes msg to main thread when job complete
    private WeakReference<Handler> mMainThreadHandler;
    private AppDatabase mDb;
    private Word mWord;                          // query String, for words to be searched for

    public DeleteWordsRunnable(Context context, Handler mainThreadHandler, Word word) {
        mMainThreadHandler = new WeakReference<>(mainThreadHandler);
        mWord = word;
        mDb = AppDatabase.getDatabase(context);
    }

    @Override
    public void run() {
        // job or work to be done is added here
        Log.d(TAG, "run: deleting words. This is from thread: " + Thread.currentThread().getName());
        ArrayList<Word> words = new ArrayList<>(mDb.wordDataDao().delete(mWord));              // retrieves a list of words from the db associated with title
        Message message = null;
        if (words.size() > 0) {
            message = Message.obtain(null, Constants.WORD_DELETE_SUCCESS);
        } else {
            message = Message.obtain(null, Constants.WORD_DELETE_FAIL);
        }

        mMainThreadHandler.get().sendMessage(message);

    }
}

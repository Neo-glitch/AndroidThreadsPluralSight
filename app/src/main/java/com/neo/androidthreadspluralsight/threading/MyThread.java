package com.neo.androidthreadspluralsight.threading;


import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.neo.androidthreadspluralsight.models.Word;
import com.neo.androidthreadspluralsight.persistence.AppDatabase;
import com.neo.androidthreadspluralsight.util.Constants;
import com.neo.androidthreadspluralsight.util.FakeData;
import com.neo.androidthreadspluralsight.util.Utility;

import java.util.ArrayList;
import java.util.List;


/**
 * class for implementing thread/looper/handler
 */
public class MyThread extends Thread {

    private static final String TAG = "MyThread";

    private MyThreadHandler mMyThreadHandler = null;                            // Handler for receiving msg from mainThread
    private Handler mMainThreadHandler = null;                                  // handler for sending message back to mainThread
    private boolean isRunning = false;                                          // true when thread is running
    private AppDatabase mDb;

    public MyThread(Context context, Handler mMainThreadHandler) {
        this.mMainThreadHandler = mMainThreadHandler;
        isRunning = true;
        mDb = AppDatabase.getDatabase(context);
    }

    @Override
    public void run() {
        if(isRunning){
            Looper.prepare();
            mMyThreadHandler = new MyThreadHandler(Looper.myLooper());
            Looper.loop();
        }
    }

    /**
     * clears ref to activities mainThread inorder to avoid memLeaks and called in activity's onStop()
     */
    public void quitThread(){
        isRunning = false;
        mMainThreadHandler = null;
    }

    /**
     * called using the the MyThread(worker) to call the threads handler.sendMessage() method for operations
     * @param message
     */
    public void sendMessageToBackgroundThread(Message message){
        while(true){            // logic to avoid sending msg to threadHandler whe thread not started
            try{
                mMyThreadHandler.sendMessage(message);
                break;
            }catch (NullPointerException e){
                Log.e(TAG, "sendMessageToBackgroundThread: null pointer: " + e.getMessage() );
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    private long[] saveNewWord(Word word){
        long[] returnValue = mDb.wordDataDao().insertWords(word);
        if(returnValue.length > 0){
            Log.d(TAG, "saveNewWord: return value: " + returnValue.toString());
        }
        return returnValue;
    }

    /**
     * gets words similar to title args
     * @param title
     * @return
     */
    private List<Word> retrieveWords(String title){
        return mDb.wordDataDao().getWords(title);
    }

    private int updateWord(Word word){
        return mDb.wordDataDao().updateWord(word.getTitle(), word.getContent(), Utility.getCurrentTimeStamp(), word.getUid());
    }

    private int deleteWord(Word word){
        return mDb.wordDataDao().delete(word);
    }

    private void insertTestWords(){

        for(Word word: FakeData.words){
            saveNewWord(word);
        }

    }


    // custom handler class to receive message from mainThread unto this thread
    class MyThreadHandler extends Handler {

        public MyThreadHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {                // called when sendMessage on the handler obj is called

            switch (msg.what) {
                case Constants.WORD_INSERT_NEW: {
                    Log.d(TAG, "handleMessage: saving word on thread: " + Thread.currentThread().getName());
                    Word word = msg.getData().getParcelable("word_new");
                    Message message = null;
                    if (saveNewWord(word).length > 0) {             // true if insert was successful
                        message = Message.obtain(null, Constants.WORD_INSERT_SUCCESS);
                    } else {
                        message = Message.obtain(null, Constants.WORD_INSERT_FAIL);
                    }
                    mMainThreadHandler.sendMessage(message);

                    break;
                }
                case Constants.WORD_UPDATE: {
                    Log.d(TAG, "handleMessage: updating word on thread: " + Thread.currentThread().getName());
                    Word word = msg.getData().getParcelable("word_update");
                    Message message = null;
                    int updateInt = updateWord(word);
                    if (updateInt > 0) {
                        message = Message.obtain(null, Constants.WORD_UPDATE_SUCCESS);
                    } else {
                        message = Message.obtain(null, Constants.WORD_UPDATE_FAIL);
                    }
                    mMainThreadHandler.sendMessage(message);
                    break;
                }
                case Constants.WORDS_RETRIEVE: {
                    Log.d(TAG, "handleMessage: retrieving words on thread: " + Thread.currentThread().getName());
                    String title = msg.getData().getString("title");
                    ArrayList<Word> words = new ArrayList<>(retrieveWords(title));              // retrieves a list of words from the db associated with title
                    Message message = null;
                    if (words.size() > 0) {
                        message = Message.obtain(null, Constants.WORDS_RETRIEVE_SUCCESS);
                        Bundle bundle = new Bundle();
                        bundle.putParcelableArrayList("words_retrieve", words);
                        message.setData(bundle);
                    } else {
                        message = Message.obtain(null, Constants.WORDS_RETRIEVE_FAIL);
                    }

                    mMainThreadHandler.sendMessage(message);

                    break;
                }
                case Constants.WORD_DELETE: {
                    Log.d(TAG, "handleMessage: deleting word on thread: " + Thread.currentThread().getName());
                    Word word = msg.getData().getParcelable("word_delete");
                    Message message = null;
                    if (deleteWord(word) > 0) {
                        message = Message.obtain(null, Constants.WORD_DELETE_SUCCESS);
                    } else {
                        message = Message.obtain(null, Constants.WORD_DELETE_FAIL);
                    }

                    mMainThreadHandler.sendMessage(message);

                    break;
                }
            }
        }
    }
}

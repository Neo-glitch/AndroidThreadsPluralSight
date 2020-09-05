package com.neo.androidthreadspluralsight.threading;

import com.neo.androidthreadspluralsight.models.Word;

import java.util.ArrayList;

/**
 * returns result from the background thread(AsyncTask class operations)
 */
public interface TaskDelegate {


    /**
     * method returns the retrieved words from the AsyncTask class back to the MainUI thread
     * @param words
     * @return
     */
    void onWordsRetrieved(ArrayList<Word> words);
}

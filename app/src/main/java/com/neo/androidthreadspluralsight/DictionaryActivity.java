package com.neo.androidthreadspluralsight;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.ItemTouchHelper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;


import com.neo.androidthreadspluralsight.R;
import com.neo.androidthreadspluralsight.adapters.WordsRecyclerAdapter;
import com.neo.androidthreadspluralsight.models.Word;

import com.neo.androidthreadspluralsight.threading.DeleteWordsRunnable;
import com.neo.androidthreadspluralsight.threading.MyThread;
import com.neo.androidthreadspluralsight.threading.RetrieveRowsAsyncTask;
import com.neo.androidthreadspluralsight.threading.RetrieveWordsRunnable;

import com.neo.androidthreadspluralsight.threading.DeleteWordsAsyncTask;
import com.neo.androidthreadspluralsight.threading.MyThread;
import com.neo.androidthreadspluralsight.threading.RetrieveWordsAsyncTask;
import com.neo.androidthreadspluralsight.threading.TaskDelegate;
import com.neo.androidthreadspluralsight.threading.ThreadPoolRunnable;
import com.neo.androidthreadspluralsight.util.Constants;
import com.neo.androidthreadspluralsight.util.VerticalSpacingItemDecorator;

import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;


/**
 * activity displays list of words on homeScreen
 */
public class DictionaryActivity extends AppCompatActivity implements
        WordsRecyclerAdapter.OnWordListener,
        View.OnClickListener,
        SwipeRefreshLayout.OnRefreshListener,
        Handler.Callback,                                          // interface to handle message sent to this activity's thread handler
        TaskDelegate
{

    private static final String TAG = "DictionaryActivity";

    //ui components
    private RecyclerView mRecyclerView;
    private SwipeRefreshLayout mSwipeRefresh;

    //vars
    private ArrayList<Word> mWords = new ArrayList<>();
    private WordsRecyclerAdapter mWordRecyclerAdapter;
    private FloatingActionButton mFab;
    private String mSearchQuery = "";

    private HandlerThread mHandlerThread;               // handlerThread obj the exec runnable obj on another thread
    private Handler mMainThreadHandler;                 // handler for receiving msg from the runnable or any other background handler
    private MyThread mMyThread;
    private RetrieveWordsAsyncTask mRetrieveWordsAsyncTask;
    private DeleteWordsAsyncTask mDeleteWordsAsyncTask;

    // for implementing threadPool
    private ExecutorService mExecutorService;
    private int mNumRows = 0;                                   // to store numRows in database
    private RetrieveRowsAsyncTask mRetrieveRowsAsyncTask;
    private ThreadPoolExecutor mThreadPoolExecutor;             // alt to using executor service to implement threadPool and provides more options

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        restoreInstanceState(savedInstanceState);

        setContentView(R.layout.activity_dictionary);
        mRecyclerView = findViewById(R.id.recyclerView);
        mFab = findViewById(R.id.fab);
        mSwipeRefresh = findViewById(R.id.swipe_refresh);

        mFab.setOnClickListener(this);
        mSwipeRefresh.setOnRefreshListener(this);

        mMainThreadHandler = new Handler(this);

        setupRecyclerView();

        initExecutorThreadPool();
    }

    private void initExecutorThreadPool(){
        // gets num of cores on device
        int numCores = Runtime.getRuntime().availableProcessors();
        Log.d(TAG, "initExecutorThreadPool: cores: " + numCores);

        mExecutorService = Executors.newFixedThreadPool(numCores);
    }


    private void restoreInstanceState(Bundle savedInstanceState){
        if(savedInstanceState != null){
            mWords = savedInstanceState.getParcelableArrayList("words");
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelableArrayList("words", mWords);
        super.onSaveInstanceState(outState);
    }


    @Override
    protected void onStart() {
        Log.d(TAG, "onStart: called.");
        super.onStart();

        mHandlerThread = new HandlerThread("DictionaryActivity HandlerThread");
        mHandlerThread.start();
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop: called.");
        super.onStop();
        mHandlerThread.quitSafely();                                        // stops the handlerThread, to avoid MemLeaks

        if(mRetrieveWordsAsyncTask != null){
            mRetrieveWordsAsyncTask.cancel(true);        // forceClose the asyncTask if activity is stopped(user changing activity)
        }
        if(mDeleteWordsAsyncTask != null){
            mDeleteWordsAsyncTask.cancel(true);
        }

        // stop service immediately even if the tasks assigned to it not completed
        mExecutorService.shutdownNow();
    }


    @Override
    protected void onResume() {
        super.onResume();
        if(mSearchQuery.length() > 2){
            onRefresh();
        }
    }

    private void retrieveWords(String title) {
        Log.d(TAG, "retrieveWords: called.");
        // n.b: this code works separately from AsyncTask code
        Handler backgroundHandler = new Handler(mHandlerThread.getLooper());                        // init a handler without init a looper and asso with handlerThread obj
        backgroundHandler.post(new RetrieveWordsRunnable(this, mMainThreadHandler, title)); // runs the runnable on the handlerThread passed


        if(mRetrieveWordsAsyncTask != null){                // logic for avoid stacking of asyncTasks
            mRetrieveWordsAsyncTask.cancel(true);
        }
        mRetrieveWordsAsyncTask = new RetrieveWordsAsyncTask(this, this);
        mRetrieveWordsAsyncTask.execute(title);

        // for implementing threadPool
        if(mRetrieveRowsAsyncTask != null){
            mRetrieveRowsAsyncTask.cancel(true);
        }
        mRetrieveRowsAsyncTask = new RetrieveRowsAsyncTask(this, this);
        mRetrieveRowsAsyncTask.execute();

    }


    public void deleteWord(Word word) {
        Log.d(TAG, "deleteWord: called.");
        mWords.remove(word);
        mWordRecyclerAdapter.getFilteredWords().remove(word);
        mWordRecyclerAdapter.notifyDataSetChanged();

        Handler backgroundHandler = new Handler(mHandlerThread.getLooper());                        // init a handler without init a looper and asso with handlerThread obj
        backgroundHandler.post(new DeleteWordsRunnable(this, mMainThreadHandler, word));    // runs the runnable on the handlerThread passed


        if(mDeleteWordsAsyncTask != null){
            mDeleteWordsAsyncTask.cancel(true);
        }
        mDeleteWordsAsyncTask = new DeleteWordsAsyncTask(this);
        mDeleteWordsAsyncTask.execute(word);

    }



    private void setupRecyclerView(){
        Log.d(TAG, "setupRecyclerView: called.");
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(linearLayoutManager);
        VerticalSpacingItemDecorator itemDecorator = new VerticalSpacingItemDecorator(10);
        mRecyclerView.addItemDecoration(itemDecorator);
        new ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(mRecyclerView);
        mWordRecyclerAdapter = new WordsRecyclerAdapter(mWords, this);
        mRecyclerView.setAdapter(mWordRecyclerAdapter);
    }

    @Override
    public void onWordClick(int position) {
        Intent intent = new Intent(this, EditWordActivity.class);
        intent.putExtra("selected_word", mWords.get(position));
        startActivity(intent);
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()){

            case R.id.fab:{
                Intent intent = new Intent(this, EditWordActivity.class);
                startActivity(intent);
                break;
            }

        }
    }


    ItemTouchHelper.SimpleCallback itemTouchHelperCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
            deleteWord(mWords.get(mWords.indexOf(mWordRecyclerAdapter.getFilteredWords().get(viewHolder.getAdapterPosition()))));
        }
    };


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.dictionary_activity_actions, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView =
                (SearchView) searchItem.getActionView();

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);

        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setMaxWidth(Integer.MAX_VALUE);

        // listening to search query text change
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // filter recycler view when query submitted
                if(query.length() > 2){
                    mSearchQuery = query;
                    retrieveWords(mSearchQuery);
                }
                else{
                    clearWords();
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String query) {
                // filter recycler view when text is changed
                if(query.length() > 2){
                    mSearchQuery = query;
                    retrieveWords(mSearchQuery);
                }
                else{
                    clearWords();
                }
                return false;
            }
        });

        return super.onCreateOptionsMenu(menu);
    }

    private void clearWords(){
        if(mWords != null){
            if(mWords.size() > 0){
                mWords.clear();
            }
        }
        mWordRecyclerAdapter.getFilter().filter(mSearchQuery);
    }

    @Override
    public void onRefresh() {
        retrieveWords(mSearchQuery);
        mSwipeRefresh.setRefreshing(false);
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what){

            case Constants.WORDS_RETRIEVE_SUCCESS:{
                Log.d(TAG, "handleMessage: successfully retrieved words. This is from thread: " + Thread.currentThread().getName());

                clearWords();

                ArrayList<Word> words = new ArrayList<>(msg.getData().<Word>getParcelableArrayList("words_retrieve"));
                mWords.addAll(words);
                mWordRecyclerAdapter.getFilter().filter(mSearchQuery);
                break;
            }

            case Constants.WORDS_RETRIEVE_FAIL:{
                Log.d(TAG, "handleMessage: unable to retrieve words. This is from thread: " + Thread.currentThread().getName());

                clearWords();
                break;
            }

            case Constants.WORD_INSERT_SUCCESS:{
                Log.d(TAG, "handleMessage: successfully inserted new word. This is from thread: " + Thread.currentThread().getName());

                break;
            }

            case Constants.WORD_INSERT_FAIL:{
                Log.d(TAG, "handleMessage: unable to insert new word. This is from thread: " + Thread.currentThread().getName());

                break;
            }

            case Constants.WORD_DELETE_SUCCESS:{
                Log.d(TAG, "handleMessage: successfully deleted a word. This is from thread: " + Thread.currentThread().getName());

                break;
            }

            case Constants.WORD_DELETE_FAIL:{
                Log.d(TAG, "handleMessage: unable to delete word. This is from thread: " + Thread.currentThread().getName());

                break;
            }

            case Constants.MSG_THREAD_POOL_TASK_COMPLETE:{
                ArrayList<Word> words = msg.getData().getParcelableArrayList("word_data_from_thread_pool");
                mWords.addAll(words);
                mWordRecyclerAdapter.getFilter().filter(mSearchQuery);

                Log.d(TAG, "handleMessage: recieved some words: " + words.size());
                Log.d(TAG, "handleMessage: total words: " + mWords.size());
                break;
            }

        }
        return true;
    }

    @Override
    public void onWordsRetrieved(ArrayList<Word> words) {
        clearWords();           // clears recent retrieved words
        mWords.addAll(words);     // adds newly retrieved words
        mWordRecyclerAdapter.notifyDataSetChanged();
    }

    @Override
    public void onRowsRetrieved(int numRows) {
        Log.d(TAG, "onRowsRetrieved: num rows: " + numRows);
        mNumRows = numRows;
        executeThreadPool();
    }


    /**
     * fun makes sure each thread work is evenly shared
     */
    private void executeThreadPool(){
        clearWords();

        int numTasks = Runtime.getRuntime().availableProcessors();

        // checks if NumRows/numTasks has a remainder, if yes round up else round down
        int chunkSize = (mNumRows % numTasks) != 0 ?
                (int) Math.ceil((double)mNumRows / (double)numTasks) : (int) Math.floor((double)mNumRows / (double)numTasks);
        Log.d(TAG, "executeThreadPool: chunksize: " + chunkSize);

        for(int i = 0; i < numTasks; i++){
            // iters through each task or core num and run our custom runnable
            // each time .submit  is called a runnable is ran on another thread if first one is still in use unless all the threads are in use and it waits
            Log.d(TAG, "executeThreadPool: starting query at: row#" + (chunkSize * i));


            ThreadPoolRunnable runnable = new ThreadPoolRunnable(
                    this,
                    mMainThreadHandler,
                    chunkSize * i,
                    chunkSize
            );
            mExecutorService.submit(runnable);
        }
    }

}



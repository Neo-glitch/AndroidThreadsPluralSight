package com.neo.androidthreadspluralsight.persistence;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;


import com.neo.androidthreadspluralsight.models.Word;

import java.util.List;

@Dao
public interface WordDao {

    @Query("SELECT * FROM Word WHERE title LIKE :title || '%'")
    List<Word> getWords(String title);

    @Query("SELECT * FROM Word")
    List<Word> getAllWords();

    @Insert
    long[] insertWords(Word... words);

    @Delete
    int delete(Word note);

    @Query("UPDATE Word SET title = :title, content = :content, timestamp = :timestamp WHERE uid = :uid")
    int updateWord(String title, String content, String timestamp, int uid);


    // queries db for rows; int row is row index and numRows is num of Rows to index starting from that row passed
    @Query("SELECT * FROM Word LIMIT :row, :numRows ")
    public List<Word> getSomeWords(int row, int numRows);

    @Query("SELECT COUNT(*) FROM Word")
    public Integer getNumRows();
}

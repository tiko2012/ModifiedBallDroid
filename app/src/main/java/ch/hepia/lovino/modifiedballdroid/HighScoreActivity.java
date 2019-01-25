package ch.hepia.lovino.modifiedballdroid;

import android.app.Activity;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;

import ch.hepia.lovino.modifiedballdroid.models.DifficultyLevel;
import ch.hepia.lovino.modifiedballdroid.models.HighScore;
import ch.hepia.lovino.modifiedballdroid.models.db.DBHelper;

import static ch.hepia.lovino.modifiedballdroid.models.db.DBContract.*;

public class HighScoreActivity extends Activity {
    private DBHelper dbHelper;
    private SQLiteDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_high_score);
        dbHelper = new DBHelper(this);
        db = dbHelper.getReadableDatabase();
        ArrayList<HighScore> scores = selectHighScores();
        ArrayAdapter<HighScore> arrayAdapter = new ArrayAdapter<HighScore>(this, R.layout.score_list_item, R.id.score_list_item_text, scores);
        ListView list = findViewById(R.id.scores_list);
        list.setAdapter(arrayAdapter);
        if (scores.isEmpty()) {
            Toast.makeText(this, R.string.no_highscores, Toast.LENGTH_LONG).show();
        }
    }

    private ArrayList<HighScore> selectHighScores() {
        ArrayList<HighScore> scores = new ArrayList<>();
        Cursor cursor = db.query(ScoreEntry.TABLE_NAME, null, null, null, null, null, ScoreEntry.COLUMN_SCORE + " DESC");
        if (cursor.moveToFirst()) {
            do {
                int score = cursor.getInt(cursor.getColumnIndex(ScoreEntry.COLUMN_SCORE));
                DifficultyLevel difficulty = DifficultyLevel.values()[cursor.getInt(cursor.getColumnIndex(ScoreEntry.COLUMN_DIFFICULTY))];
                scores.add(new HighScore(score, difficulty));
            } while (cursor.moveToNext());
        }
        return scores;
    }
}

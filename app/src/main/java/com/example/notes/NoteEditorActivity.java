package com.example.notes;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class NoteEditorActivity extends AppCompatActivity {
    private EditText editTextNote;
    private Button saveButton;
    private int notePosition = -1;
    private final String LOG_TAG = "NOTE_ACTIVITY";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.note_activity);
        editTextNote = findViewById(R.id.edit_text_note);
        saveButton = findViewById(R.id.save_button);

        Intent intent = getIntent();
        if (intent.hasExtra("NOTE_TEXT")) {
            String existingText = intent.getStringExtra("NOTE_TEXT");
            notePosition = intent.getIntExtra("NOTE_POSITION", -1);
            if (editTextNote != null) {
                editTextNote.setText(existingText);
            }
        }

        saveButton.setOnClickListener(v -> {
            String newText = editTextNote.getText().toString();

            Intent resultIntent = new Intent();
            resultIntent.putExtra("NOTE_TEXT", newText);
            resultIntent.putExtra("NOTE_POSITION", notePosition);

            setResult(Activity.RESULT_OK, resultIntent);

            finish();
        });

        Log.d(LOG_TAG, "NoteActivity on Create");
    }
}

package com.example.notes;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;


public class MainActivity extends AppCompatActivity {

    private ArrayList<String> notesList = new ArrayList<>();
    private NotesAdapter adapter;
    private ActivityResultLauncher<Intent> noteEditorLauncher;
    private RecyclerView recyclerView;
    private Button button;

    @Override
    protected void onCreate(Bundle saveInstanceState) {
        super.onCreate(saveInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recyclerViewNotes);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new NotesAdapter(notesList, this::onNoteClick);
        recyclerView.setAdapter(adapter);

        noteEditorLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == MainActivity.RESULT_OK) {

                        Intent data = result.getData();

                        if (data != null) {
                            String noteText = data.getStringExtra("NOTE_TEXT");

                            int position = data.getIntExtra("NOTE_POSITION", -1);

                            if (position == -1) {
                                notesList.add(noteText);
                                adapter.notifyItemInserted(notesList.size() -1);
                            } else {
                                notesList.set(position, noteText);
                                adapter.notifyItemInserted(position);
                            }
                        }
                    }
                });

        button = findViewById(R.id.buttonio);

        button.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, NoteEditorActivity.class);
            noteEditorLauncher.launch(intent);
        });
    }

    public void onNoteClick(int position) {
        String existingNotes = notesList.get(position);

        Intent intent = new Intent(MainActivity.this, NoteEditorActivity.class);

        intent.putExtra("NOTE_TEXT",existingNotes);
        intent.putExtra("NOTE_POSITION",position);

        noteEditorLauncher.launch(intent);
    }
}

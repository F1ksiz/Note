package com.example.notes;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.NoteViewHolder> {
    public interface OnNoteClickListener {
        void onNoteClick(int position);
    }

    public interface OnDeleteClickListener {
        void onDeleteClick(int position);
    }

    private ArrayList<String> noteList;
    private OnNoteClickListener clickListener;
    private OnDeleteClickListener deleteClickListener;

    public NotesAdapter(ArrayList<String> noteList, OnNoteClickListener clickListener, OnDeleteClickListener deleteClickListener) {
        this.noteList = noteList;
        this.clickListener = clickListener;
        this.deleteClickListener = deleteClickListener;
    }

    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_note, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        String currentNote = noteList.get(position);
        holder.noteTextView.setText(currentNote);
    }

    @Override
    public int getItemCount() {
        return (noteList != null) ? noteList.size() : 0;
    }

    public class NoteViewHolder extends RecyclerView.ViewHolder {
        public TextView noteTextView;
        public Button deleteButton;

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            noteTextView = itemView.findViewById(R.id.input_text);
            deleteButton = itemView.findViewById(R.id.delete_button);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (clickListener != null && position != RecyclerView.NO_POSITION) {
                    clickListener.onNoteClick(position);
                }
            });

            deleteButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (deleteClickListener != null && position != RecyclerView.NO_POSITION) {
                    deleteClickListener.onDeleteClick(position);
                }
            });
        }
    }
}

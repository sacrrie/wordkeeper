/*
 * Copyright 2016 Alidibir Akhbulatov <alidibir.akhbulatov@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.akhbulatov.wordkeeper.ui;

import android.app.Activity;
import android.app.Dialog;
import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.akhbulatov.wordkeeper.R;
import com.akhbulatov.wordkeeper.adapter.WordAdapter;
import com.akhbulatov.wordkeeper.database.DatabaseCategoryAdapter;
import com.akhbulatov.wordkeeper.database.DatabaseContract.WordEntry;
import com.akhbulatov.wordkeeper.database.DatabaseWordAdapter;
import com.akhbulatov.wordkeeper.ui.listener.FabAddWordListener;
import com.akhbulatov.wordkeeper.ui.widget.DividerItemDecoration;

import java.util.List;

/**
 * Shows a list of words from the database.
 * Loader uses a custom class for working with the database,
 * NOT the ContentProvider (temporary solution)
 */
public class WordListFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>,
        WordAdapter.WordViewHolder.WordAdapterListener, WordSortDialogFragment.WordSortDialogListener,
        CategoryListDialogFragment.CategoryListDialogListener {

    private static final int LOADER_ID = 1;

    private static final int WORD_SORT_DIALOG_REQUEST = 1;
    private static final int CATEGORY_LIST_DIALOG_REQUEST = 2;

    private static final String WORD_SORT_DIALOG_ID = WordSortDialogFragment.class.getName();
    private static final String CATEGORY_LIST_DIALOG_ID = CategoryListDialogFragment.class.getName();

    private static int sSortMode;

    // Contains the ID of the current selected item (word)
    private long mSelectedItemId;

    private RecyclerView mWordList;
    private WordAdapter mWordAdapter;
    private DatabaseWordAdapter mDbWordAdapter;
    private TextView mTextEmptyWordList;

    private ActionModeCallback mActionModeCallback;
    private ActionMode mActionMode;

    private FabAddWordListener mListener;

    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (FabAddWordListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement "
                    + FabAddWordListener.class.getName());
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        mDbWordAdapter = new DatabaseWordAdapter(getActivity());
        mDbWordAdapter.open();

        SharedPreferences mPrefs = getActivity()
                .getSharedPreferences(WordSortDialogFragment.PREF_NAME, Context.MODE_PRIVATE);
        sSortMode = mPrefs.getInt(WordSortDialogFragment.PREF_SORT_MODE,
                WordSortDialogFragment.DEFAULT_SORT_MODE);

        mActionModeCallback = new ActionModeCallback();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_word_list, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mWordList = (RecyclerView) view.findViewById(R.id.recycler_word_list);
        mWordList.setHasFixedSize(true);
        mWordList.addItemDecoration(new DividerItemDecoration(getActivity(),
                DividerItemDecoration.VERTICAL_LIST));
        mWordList.setLayoutManager(new LinearLayoutManager(getActivity()));

        mTextEmptyWordList = (TextView) view.findViewById(R.id.text_empty_word_list);

        FloatingActionButton fabAddWord = (FloatingActionButton) view.findViewById(R.id.fab_add_word);
        fabAddWord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mListener.onFabAddWordClick(R.string.title_new_word,
                        R.string.word_editor_action_add,
                        R.string.word_editor_action_cancel);
            }
        });
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(LOADER_ID, null, this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mDbWordAdapter.close();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_word_list, menu);

        MenuItem searchItem = menu.findItem(R.id.menu_search_word);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);

        SearchManager searchManager = (SearchManager)
                getActivity().getSystemService(Context.SEARCH_SERVICE);
        // Create intent for launching search results screen
        searchView.setSearchableInfo(searchManager
                .getSearchableInfo(new ComponentName(getActivity(), SearchActivity.class)));
        searchView.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_sort_word:
                showWordSortDialog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // Returns the cursor with all records from the database.
        // Uses own class instead of a ContentProvider
        return new SimpleCursorLoader(getActivity(), mDbWordAdapter);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (mWordAdapter == null) {
            // The adapter is created only the first time retrieving data from the database
            mWordAdapter = new WordAdapter(getActivity(), data, this);
            mWordAdapter.setHasStableIds(true);
            mWordList.setAdapter(mWordAdapter);
        } else {
            mWordAdapter.swapCursor(data);
        }

        if (mWordAdapter.getItemCount() == 0) {
            mTextEmptyWordList.setVisibility(View.VISIBLE);
        } else {
            mTextEmptyWordList.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (mWordAdapter != null) {
            mWordAdapter.swapCursor(null);
        }
    }

    @Override
    public void onWordItemClick(int position) {
        if (mActionMode != null) {
            toggleSelection(position);
        }
    }

    @Override
    public boolean onWordItemLongClick(int position) {
        if (mActionMode == null) {
            mActionMode = ((AppCompatActivity)
                    getActivity()).startSupportActionMode(mActionModeCallback);
        }
        toggleSelection(position);
        return true;
    }

    // Updates the word list with the new sort mode
    @Override
    public void onFinishWordSortDialog(int sortMode) {
        // Saves to pass to the inner class SimpleCursorLoader
        sSortMode = sortMode;
        getLoaderManager().restartLoader(LOADER_ID, null, this);
    }

    // Moves the marked words in the selected category
    @Override
    public void onFinishCategoryListDialog(String category) {
        Cursor cursor = null;

        for (Integer i : mWordAdapter.getSelectedWords()) {
            long id = mWordAdapter.getItemId(i);
            cursor = mDbWordAdapter.fetchRecord(id);

            String name = cursor.getString(cursor.getColumnIndex(WordEntry.COLUMN_NAME));
            String translation = cursor.getString(cursor.getColumnIndex(WordEntry.COLUMN_TRANSLATION));

            mDbWordAdapter.updateRecord(id, name, translation, category);
        }

        mActionMode.finish();

        if (cursor != null) {
            cursor.close();
            Toast.makeText(getActivity(), R.string.success_move_word, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getActivity(), R.string.error_move_word, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Receives the entered data (word) and saves in the database
     *
     * @param dialog The dialog from where take the data (word) to save
     */
    public void addWord(DialogFragment dialog) {
        Dialog dialogView = dialog.getDialog();

        EditText editName = (EditText) dialogView.findViewById(R.id.edit_word_name);
        EditText editTranslation = (EditText) dialogView.findViewById(R.id.edit_word_translation);
        Spinner spinnerCategory = (Spinner) dialogView.findViewById(R.id.spinner_categories);

        String name = editName.getText().toString();
        String translation = editTranslation.getText().toString();
        String category = spinnerCategory.getSelectedItem().toString();

        if ((TextUtils.isEmpty(name) & TextUtils.isEmpty(translation))
                | (TextUtils.isEmpty(name) | TextUtils.isEmpty(translation))) {
            Toast.makeText(getActivity(),
                    R.string.error_word_editor_empty_fields,
                    Toast.LENGTH_SHORT)
                    .show();
        } else {
            mDbWordAdapter.addRecord(name, translation, category);
            // Checked for null in case this method is called from the screen "Categories"
            if (mWordList != null) {
                mWordList.scrollToPosition(0);
            }
            getLoaderManager().restartLoader(LOADER_ID, null, this);
        }
    }

    public void editWord(String name, String translation, String category) {
        if ((TextUtils.isEmpty(name) & TextUtils.isEmpty(translation))
                | (TextUtils.isEmpty(name) | TextUtils.isEmpty(translation))) {
            Toast.makeText(getActivity(),
                    R.string.error_word_editor_empty_fields,
                    Toast.LENGTH_SHORT)
                    .show();
        } else {
            mDbWordAdapter.updateRecord(mSelectedItemId, name, translation, category);
            getLoaderManager().restartLoader(LOADER_ID, null, this);
        }
    }

    public String getName() {
        Cursor cursor = mDbWordAdapter.fetchRecord(mSelectedItemId);
        if (cursor.getCount() > 0) {
            return cursor.getString(cursor.getColumnIndex(WordEntry.COLUMN_NAME));
        }
        return null;
    }

    public String getTranslation() {
        Cursor cursor = mDbWordAdapter.fetchRecord(mSelectedItemId);
        if (cursor.getCount() > 0) {
            return cursor.getString(cursor.getColumnIndex(WordEntry.COLUMN_TRANSLATION));
        }
        return null;
    }

    public String getCategory() {
        Cursor cursor = mDbWordAdapter.fetchRecord(mSelectedItemId);
        if (cursor.getCount() > 0) {
            return cursor.getString(cursor.getColumnIndex(WordEntry.COLUMN_CATEGORY));
        }
        return null;
    }

    public String[] getCategories() {
        DatabaseCategoryAdapter mDbCategoryAdapter = new DatabaseCategoryAdapter(getActivity());
        mDbCategoryAdapter.open();

        Cursor cursor = mDbCategoryAdapter.fetchAllRecords();
        String[] categories = new String[cursor.getCount()];

        for (int i = 0; i < cursor.getCount(); i++) {
            categories[i] = cursor.getString(cursor.getColumnIndex(WordEntry.COLUMN_NAME));
            cursor.moveToNext();
        }

        mDbCategoryAdapter.close();
        return categories;
    }

    private void toggleSelection(int position) {
        mWordAdapter.toggleSelection(position);
        int count = mWordAdapter.getSelectedWordCount();

        if (count == 0) {
            mActionMode.finish();
        } else {
            mActionMode.setTitle(String.valueOf(count));
            mActionMode.invalidate();
        }
    }

    private void deleteWords(List<Integer> words) {
        for (Integer i : words) {
            mDbWordAdapter.deleteRecord(mWordAdapter.getItemId(i));
        }
        getLoaderManager().restartLoader(LOADER_ID, null, this);
    }

    /**
     * Gets the single item (word) ID from the list of words,
     * despite the collection that is passed in the parameter
     *
     * @param words The list of selected words
     * @return Returns the item (word) ID
     */
    private long getWordItemId(List<Integer> words) {
        long id = 0;
        for (Integer i : words) {
            id = mWordAdapter.getItemId(i);
        }
        return id;
    }

    private void showWordSortDialog() {
        DialogFragment dialog = new WordSortDialogFragment();
        dialog.setTargetFragment(WordListFragment.this, WORD_SORT_DIALOG_REQUEST);
        dialog.show(getActivity().getSupportFragmentManager(), WORD_SORT_DIALOG_ID);
    }

    private void showCategoryListDialog() {
        DialogFragment dialog = new CategoryListDialogFragment();
        dialog.setTargetFragment(WordListFragment.this, CATEGORY_LIST_DIALOG_REQUEST);
        dialog.show(getActivity().getSupportFragmentManager(), CATEGORY_LIST_DIALOG_ID);
    }

    /**
     * Used to work with a Loader instead of a ContentProvider
     */
    private static class SimpleCursorLoader extends CursorLoader {

        private DatabaseWordAdapter mDbWordAdapter;

        public SimpleCursorLoader(Context context, DatabaseWordAdapter dbWordAdapter) {
            super(context);
            mDbWordAdapter = dbWordAdapter;
        }

        @Override
        public Cursor loadInBackground() {
            return mDbWordAdapter.fetchAllRecords(sSortMode);
        }
    }

    /**
     * Provides support for CAB
     */
    private class ActionModeCallback implements ActionMode.Callback {

        private MenuItem mItemEditWord;

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.selected_word, menu);
            mItemEditWord = menu.findItem(R.id.menu_edit_word);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            // Editing is available only for one selected word
            if (mWordAdapter.getSelectedWordCount() == 1) {
                mItemEditWord.setVisible(true);
            } else {
                mItemEditWord.setVisible(false);
            }
            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.menu_move_word:
                    showCategoryListDialog();
                    return true;
                case R.id.menu_edit_word:
                    // Saves the id to use to retrieve the selected row
                    // and paste the edited string into the database.
                    // Called for only one selected word
                    mSelectedItemId = getWordItemId(mWordAdapter.getSelectedWords());

                    mListener.onFabAddWordClick(R.string.title_edit_word,
                            R.string.word_editor_action_edit,
                            R.string.word_editor_action_cancel);
                    mode.finish();
                    return true;
                case R.id.menu_delete_word:
                    deleteWords(mWordAdapter.getSelectedWords());
                    mode.finish();
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mWordAdapter.clearSelection();
            mActionMode = null;
        }
    }
}

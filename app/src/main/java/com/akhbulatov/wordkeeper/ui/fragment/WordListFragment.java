/*
 * Copyright 2019 Alidibir Akhbulatov
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

package com.akhbulatov.wordkeeper.ui.fragment;

import android.app.Dialog;
import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Html;
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

import com.akhbulatov.wordkeeper.R;
import com.akhbulatov.wordkeeper.adapter.WordAdapter;
import com.akhbulatov.wordkeeper.database.CategoryDatabaseAdapter;
import com.akhbulatov.wordkeeper.database.DatabaseContract.WordEntry;
import com.akhbulatov.wordkeeper.database.WordDatabaseAdapter;
import com.akhbulatov.wordkeeper.model.Category;
import com.akhbulatov.wordkeeper.model.Word;
import com.akhbulatov.wordkeeper.ui.activity.MainActivity;
import com.akhbulatov.wordkeeper.ui.dialog.CategoryListDialog;
import com.akhbulatov.wordkeeper.ui.dialog.WordSortDialog;
import com.akhbulatov.wordkeeper.ui.listener.FabAddWordListener;
import com.akhbulatov.wordkeeper.util.CommonUtils;
import com.akhbulatov.wordkeeper.util.FilterCursorWrapper;
import com.akhbulatov.wordkeeper.util.SharedPreferencesManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.DialogFragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Shows a list of words from the database.
 * Loader uses a custom class for working with the database,
 * NOT the ContentProvider (temporary solution)
 */
public class WordListFragment extends BaseFragment implements LoaderManager.LoaderCallbacks<Cursor>,
        WordAdapter.WordItemClickListener,
        WordSortDialog.WordSortDialogListener,
        CategoryListDialog.CategoryListDialogListener {

    private static final int LOADER_ID = 1;

    private static final int WORD_SORT_DIALOG_REQUEST = 1;
    private static final int CATEGORY_LIST_DIALOG_REQUEST = 2;

    private static int sSortMode;

    // Contains the ID of the current selected item (word)
    private long mSelectedItemId;

    private RecyclerView mWordList;
    private TextView mTextEmptyWordList;
    private TextView mTextNoResultsWord;

    private LoaderManager loaderManager;

    private WordAdapter mWordAdapter;
    private WordDatabaseAdapter mWordDbAdapter;

    private ActionModeCallback mActionModeCallback;
    private ActionMode mActionMode;

    private FabAddWordListener mListener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            mListener = (FabAddWordListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement "
                    + FabAddWordListener.class.getName());
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        loaderManager = getLoaderManager();

        mWordDbAdapter = new WordDatabaseAdapter(getActivity());
        mWordDbAdapter.open();

        sSortMode = SharedPreferencesManager.getSortMode(getActivity());

        mActionModeCallback = new ActionModeCallback();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_word_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mWordList = view.findViewById(R.id.recycler_word_list);
        mWordList.setHasFixedSize(true);
        mWordList.addItemDecoration(new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));

        mTextEmptyWordList = view.findViewById(R.id.text_empty_word_list);
        mTextEmptyWordList.setVisibility(View.GONE);

        mTextNoResultsWord = view.findViewById(R.id.text_no_results_word);
        mTextNoResultsWord.setVisibility(View.GONE);

        FloatingActionButton fabAddWord = view.findViewById(R.id.fab_add_word);
        fabAddWord.setOnClickListener(v ->
                mListener.onFabAddWordClick(R.string.title_new_word,
                        R.string.word_editor_action_add,
                        android.R.string.cancel));

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        loaderManager.initLoader(LOADER_ID, null, this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mWordDbAdapter.close();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_word, menu);

        MenuItem searchItem = menu.findItem(R.id.menu_search_word);
        SearchView searchView = (SearchView) searchItem.getActionView();

        SearchManager searchManager = (SearchManager) requireActivity().getSystemService(Context.SEARCH_SERVICE);
        if (searchManager != null) {
            searchView.setSearchableInfo(searchManager
                    .getSearchableInfo(new ComponentName(requireActivity(), MainActivity.class)));
        }
        searchView.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                final Cursor cursor = mWordDbAdapter.getAll(sSortMode);
                final int column = cursor.getColumnIndex(WordEntry.COLUMN_NAME);
                if (newText.length() > 0) {
                    mWordAdapter.swapCursor(new FilterCursorWrapper(cursor, newText, column));

                    mTextEmptyWordList.setVisibility(View.GONE);
                    if (mWordAdapter.getItemCount() == 0) {
                        String escapedNewText = TextUtils.htmlEncode(newText);
                        String formattedNoResults = String.format(
                                getString(R.string.no_results_word), escapedNewText);
                        CharSequence styledNoResults = Html.fromHtml(formattedNoResults);

                        mTextNoResultsWord.setText(styledNoResults);
                        mTextNoResultsWord.setVisibility(View.VISIBLE);
                    } else {
                        mTextNoResultsWord.setVisibility(View.GONE);
                    }
                } else {
                    mWordAdapter.swapCursor(cursor);

                    mTextNoResultsWord.setVisibility(View.GONE);
                    if (mWordAdapter.getItemCount() == 0) {
                        mTextEmptyWordList.setVisibility(View.VISIBLE);
                    }
                }
                return true;
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_sort_word:
                showWordSortDialog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // Returns the cursor with all records from the database.
        // Uses own class instead of a ContentProvider
        return new SimpleCursorLoader(getActivity(), mWordDbAdapter);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
        if (mWordAdapter == null) {
            // The adapter is created only the first time retrieving data from the database
            mWordAdapter = new WordAdapter(data);
            mWordAdapter.setHasStableIds(true);
            mWordAdapter.setOnItemClickListener(this);
            mWordList.setAdapter(mWordAdapter);
        } else {
            mWordAdapter.swapCursor(data);
        }

        if (mWordAdapter.getItemCount() == 0) {
            mTextEmptyWordList.setVisibility(View.VISIBLE);
        } else {
            mTextEmptyWordList.setVisibility(View.GONE);
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
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
            mActionMode = ((AppCompatActivity) requireActivity()).startSupportActionMode(mActionModeCallback);
        }
        toggleSelection(position);
        return true;
    }

    @Override
    public void onFinishWordSortDialog(int sortMode) {
        // Saves to pass to the inner class SimpleCursorLoader
        sSortMode = sortMode;
        loaderManager.restartLoader(LOADER_ID, null, this);
    }

    // Updates the word list with the new sort mode
    @Override
    public void onFinishCategoryListDialog(String category) {
        Word word = null;
        for (Integer i : mWordAdapter.getSelectedWords()) {
            word = mWordDbAdapter.get(mWordAdapter.getItemId(i));
            mWordDbAdapter.update(new Word(
                    word.getId(),
                    word.getName(),
                    word.getTranslation(),
                    category));
        }

        mActionMode.finish();

        if (word != null) {
            CommonUtils.showToast(getActivity(), R.string.success_move_word);
        } else {
            CommonUtils.showToast(getActivity(), R.string.error_move_word);
        }
    }

    /**
     * Receives the entered data (word) and saves in the database
     *
     * @param dialog The dialog from where take the data (word) to save
     */
    public void addWord(DialogFragment dialog) {
        Dialog dialogView = dialog.getDialog();

        EditText editName = dialogView.findViewById(R.id.edit_word_name);
        EditText editTranslation = dialogView.findViewById(R.id.edit_word_translation);
        Spinner spinnerCategory = dialogView.findViewById(R.id.spinner_categories);

        String name = editName.getText().toString();
        String translation = editTranslation.getText().toString();
        String category = spinnerCategory.getSelectedItem().toString();

        if ((TextUtils.isEmpty(name) & TextUtils.isEmpty(translation))
                | (TextUtils.isEmpty(name) | TextUtils.isEmpty(translation))) {
            CommonUtils.showToast(getActivity(), R.string.error_word_editor_empty_fields);
        } else {
            mWordDbAdapter.insert(new Word(name, translation, category));
            // Checked for null in case this method is called from the screen "Categories"
            if (mWordList != null) {
                mWordList.scrollToPosition(0);
            }
            loaderManager.restartLoader(LOADER_ID, null, this);
        }
    }

    public void editWord(String name, String translation, String category) {
        if ((TextUtils.isEmpty(name) & TextUtils.isEmpty(translation))
                | (TextUtils.isEmpty(name) | TextUtils.isEmpty(translation))) {
            CommonUtils.showToast(getActivity(), R.string.error_word_editor_empty_fields);
        } else {
            mWordDbAdapter.update(new Word(mSelectedItemId, name, translation, category));
            loaderManager.restartLoader(LOADER_ID, null, this);
        }
    }

    public String getName() {
        return mWordDbAdapter.get(mSelectedItemId).getName();
    }

    public String getTranslation() {
        return mWordDbAdapter.get(mSelectedItemId).getTranslation();
    }

    public String getCategory() {
        return mWordDbAdapter.get(mSelectedItemId).getCategory();
    }

    public String[] getCategories() {
        CategoryDatabaseAdapter categoryDbAdapter = new CategoryDatabaseAdapter(getActivity());
        categoryDbAdapter.open();

        Cursor cursor = categoryDbAdapter.getAll();
        List<Category> categoryList = Category.getCategories(cursor);
        String[] categories = new String[categoryList.size()];
        for (int i = 0; i < categoryList.size(); i++) {
            categories[i] = categoryList.get(i).getName();
        }

        cursor.close();
        categoryDbAdapter.close();
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
            mWordDbAdapter.delete(new Word(mWordAdapter.getItemId(i)));
        }
        loaderManager.restartLoader(LOADER_ID, null, this);
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
        DialogFragment dialog = new WordSortDialog();
        dialog.setTargetFragment(WordListFragment.this, WORD_SORT_DIALOG_REQUEST);
        dialog.show(requireActivity().getSupportFragmentManager(), null);
    }

    private void showCategoryListDialog() {
        DialogFragment dialog = new CategoryListDialog();
        dialog.setTargetFragment(WordListFragment.this, CATEGORY_LIST_DIALOG_REQUEST);
        dialog.show(requireActivity().getSupportFragmentManager(), null);
    }

    /**
     * Used to work with a Loader instead of a ContentProvider
     */
    private static class SimpleCursorLoader extends CursorLoader {

        private WordDatabaseAdapter mWordDbAdapter;

        SimpleCursorLoader(Context context, WordDatabaseAdapter wordDbAdapter) {
            super(context);
            mWordDbAdapter = wordDbAdapter;
        }

        @Override
        public Cursor loadInBackground() {
            return mWordDbAdapter.getAll(sSortMode);
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
                            android.R.string.cancel);
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

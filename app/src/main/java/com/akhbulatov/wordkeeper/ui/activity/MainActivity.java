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

package com.akhbulatov.wordkeeper.ui.activity;

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import com.akhbulatov.wordkeeper.R;
import com.akhbulatov.wordkeeper.ui.dialog.WordEditorDialog;
import com.akhbulatov.wordkeeper.ui.fragment.CategoryListFragment;
import com.akhbulatov.wordkeeper.ui.fragment.WordListFragment;
import com.akhbulatov.wordkeeper.ui.listener.FabAddWordListener;
import com.akhbulatov.wordkeeper.util.CommonUtils;
import com.google.android.material.navigation.NavigationView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

/**
 * Provides navigation drawer to switch between screens
 */
public class MainActivity extends AppCompatActivity implements FabAddWordListener,
        WordEditorDialog.WordEditorDialogListener {

    private static final String BUNDLE_SCREEN_TITLE = "BUNDLE_SCREEN_TITLE";

    private static final String WORD_LIST_FRAGMENT_TAG = WordListFragment.class.getName();
    private static final String CATEGORY_LIST_FRAGMENT_TAG = CategoryListFragment.class.getName();

    private DrawerLayout mDrawerLayout;
    private NavigationView mNavigationView;
    private ActionBarDrawerToggle mDrawerToggle;

    private WordListFragment mWordListFragment;
    private CategoryListFragment mCategoryListFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mDrawerLayout = findViewById(R.id.drawer_layout);
        // Also sets Toolbar's navigation click listener to toggle the drawer when it is clicked
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, toolbar,
                R.string.drawer_open, R.string.drawer_close);

        // Creates an animation of the hamburger icon
        // for opening and closing the drawer
        mDrawerLayout.addDrawerListener(mDrawerToggle);

        mNavigationView = findViewById(R.id.nav_view);
        mNavigationView.setNavigationItemSelectedListener(item -> {
            selectDrawerItem(item);
            return true;
        });

        if (savedInstanceState != null) {
            mWordListFragment = (WordListFragment)
                    getSupportFragmentManager().findFragmentByTag(WORD_LIST_FRAGMENT_TAG);
            mCategoryListFragment = (CategoryListFragment)
                    getSupportFragmentManager().findFragmentByTag(CATEGORY_LIST_FRAGMENT_TAG);

            setTitle(savedInstanceState.getString(BUNDLE_SCREEN_TITLE));
        } else {
            mWordListFragment = new WordListFragment();
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.layout_root_container, mWordListFragment, WORD_LIST_FRAGMENT_TAG)
                    .commit();

            setTitle(R.string.title_all_words);
        }
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Syncs the toggle state whenever the screen is restored
        // or there is a configuration change (i.e screen rotation)
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Passes any configuration change to the drawer toggles
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(BUNDLE_SCREEN_TITLE, getTitle().toString());
    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
        } else if (getSupportFragmentManager().getBackStackEntryCount() != 0) {
            getSupportFragmentManager().popBackStack();
            // Returns to the main fragment and shows it
            mWordListFragment = new WordListFragment();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.layout_root_container, mWordListFragment, WORD_LIST_FRAGMENT_TAG)
                    .commit();

            mNavigationView.setCheckedItem(R.id.menu_drawer_all_words);
            setTitle(R.string.title_all_words);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onFabAddWordClick(int titleId, int positiveTextId, int negativeTextId) {
        showWordEditorDialog(titleId, positiveTextId, negativeTextId);
    }

    // Passes the ID of the text on the positive button
    // to determine which the dialog (word) button was pressed: add or edit
    @Override
    public void onFinishWordEditorDialog(DialogFragment dialog, int positiveTextId) {
        // Add the word
        if (positiveTextId == R.string.word_editor_action_add) {
            mWordListFragment.addWord(dialog);

            // Updates the category list only from the screen "Categories"
            if (mCategoryListFragment != null && mCategoryListFragment.isVisible()) {
                mCategoryListFragment.updateCategoryList();
            } else {
                // Edit the word
                Dialog dialogView = dialog.getDialog();

                EditText editName = dialogView.findViewById(R.id.edit_word_name);
                EditText editTranslation = dialogView.findViewById(R.id.edit_word_translation);
                Spinner spinnerCategories = dialogView.findViewById(R.id.spinner_categories);

                String name = editName.getText().toString();
                String translation = editTranslation.getText().toString();
                String category = spinnerCategories.getSelectedItem().toString();

                mWordListFragment.editWord(name, translation, category);
            }
        }
    }

    private void selectDrawerItem(MenuItem item) {
        Class fragmentClass = null;
        Fragment fragment = null;

        switch (item.getItemId()) {
            case R.id.menu_drawer_all_words:
                fragmentClass = WordListFragment.class;
                break;
            case R.id.menu_drawer_categories:
                fragmentClass = CategoryListFragment.class;
                break;
            case R.id.menu_drawer_rate_app:
                showRateApp();
                break;
            case R.id.menu_drawer_about:
                showAbout();
                break;
            default:
                fragmentClass = WordListFragment.class;
        }

        // Block is executed only if the selected item is a fragment,
        // otherwise was the selected activity and the replacement fragment is not executed
        if (fragmentClass != null) {
            try {
                fragment = (Fragment) fragmentClass.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
            }

            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            if (fragmentClass == WordListFragment.class) {
                getSupportFragmentManager().popBackStack();
                transaction.replace(R.id.layout_root_container, fragment, WORD_LIST_FRAGMENT_TAG);
                mWordListFragment = (WordListFragment) fragment;
            } else {
                transaction.replace(R.id.layout_root_container, fragment, CATEGORY_LIST_FRAGMENT_TAG);
                transaction.addToBackStack(null);
                mCategoryListFragment = (CategoryListFragment) fragment;
            }
            transaction.commit();

            // Not required for activity. Only for a fragment
            item.setChecked(true);
            setTitle(item.getTitle());
        }

        mDrawerLayout.closeDrawers();
    }

    private void showWordEditorDialog(@StringRes int titleId,
                                      @StringRes int positiveTextId,
                                      @StringRes int negativeTextId) {
        DialogFragment dialog = WordEditorDialog.newInstance(titleId, positiveTextId, negativeTextId);
        dialog.show(getSupportFragmentManager(), null);
        // NOTE! If the method is not called, the app crashes
        getSupportFragmentManager().executePendingTransactions();

        Dialog dialogView = dialog.getDialog();
        Spinner spinnerCategories = dialogView.findViewById(R.id.spinner_categories);

        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                mWordListFragment.getCategories());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategories.setAdapter(adapter);

        // Receives and shows data of the selected word to edit in the dialog
        // Data is the name, translation and category
        if (positiveTextId == R.string.word_editor_action_edit) {
            EditText editName = dialogView.findViewById(R.id.edit_word_name);
            EditText editTranslation = dialogView.findViewById(R.id.edit_word_translation);

            editName.setText(mWordListFragment.getName());
            editTranslation.setText(mWordListFragment.getTranslation());
            spinnerCategories.setSelection(adapter.getPosition(mWordListFragment.getCategory()));
        }
    }

    private void showRateApp() {
        Uri uri = Uri.parse("market://details?id=" + getPackageName());
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (ActivityNotFoundException e) {
            CommonUtils.showToast(this, R.string.error_rate_app);
        }
    }

    private void showAbout() {
        startActivity(new Intent(this, AboutActivity.class));
    }
}

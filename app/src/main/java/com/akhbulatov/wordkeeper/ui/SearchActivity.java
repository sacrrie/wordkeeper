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

import android.app.SearchManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;

import com.akhbulatov.wordkeeper.R;
import com.akhbulatov.wordkeeper.adapter.WordAdapter;
import com.akhbulatov.wordkeeper.database.WordDatabaseAdapter;
import com.akhbulatov.wordkeeper.ui.widget.DividerItemDecoration;

/**
 * Shows search results words
 */
public class SearchActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_actionbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Receives a query from ACTION_SEARCH
        String query = getIntent().getStringExtra(SearchManager.QUERY);

        RecyclerView wordList = (RecyclerView) findViewById(R.id.recycler_word_list);
        wordList.setHasFixedSize(true);
        wordList.addItemDecoration(new DividerItemDecoration(this,
                DividerItemDecoration.VERTICAL_LIST));
        wordList.setLayoutManager(new LinearLayoutManager(this));

        WordDatabaseAdapter wordDbAdapter = new WordDatabaseAdapter(this);
        wordDbAdapter.open();

        WordAdapter wordAdapter = new WordAdapter(this, wordDbAdapter.fetchRecordsByName(query));
        wordAdapter.setHasStableIds(true);
        wordList.setAdapter(wordAdapter);

        TextView textNoSearchResults = (TextView) findViewById(R.id.text_no_search_results);

        if (wordAdapter.getItemCount() == 0) {
            String noSearchResults = String.format(getString(R.string.no_search_results), query);

            textNoSearchResults.setText(noSearchResults);
            textNoSearchResults.setVisibility(View.VISIBLE);
        } else {
            textNoSearchResults.setVisibility(View.INVISIBLE);
        }

        // Closes the database already here, as it is no longer needed
        wordDbAdapter.close();
    }
}

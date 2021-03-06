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

package com.akhbulatov.wordkeeper.database.dao;

import android.database.Cursor;

import com.akhbulatov.wordkeeper.model.Category;

/**
 * @author Alidibir Akhbulatov
 * @since 26.11.2016
 */
public interface CategoryDao {
    long insert(Category category);

    int update(Category category);

    int delete(Category category);

    Cursor getAll();

    Category get(long id);
}

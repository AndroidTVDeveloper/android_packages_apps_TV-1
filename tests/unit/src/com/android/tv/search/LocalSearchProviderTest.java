/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.tv.search;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;

import android.app.SearchManager;
import android.database.Cursor;
import android.net.Uri;
import android.support.test.filters.SmallTest;
import android.test.ProviderTestCase2;
import com.android.tv.TvApplication;
import com.android.tv.common.BaseSingletons;
import com.android.tv.common.CommonConstants;
import com.android.tv.perf.PerformanceMonitor;
import com.android.tv.util.MockTvSingletons;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/** Unit test for {@link LocalSearchProvider}. */
@SmallTest
@SuppressWarnings("TryWithResources") // TODO(b/62143348): remove when error prone check fixed
public class LocalSearchProviderTest extends ProviderTestCase2<LocalSearchProvider> {
    private static final String AUTHORITY = CommonConstants.BASE_PACKAGE + ".search";
    private static final String KEYWORD = "keyword";
    private static final Uri BASE_SEARCH_URI =
            Uri.parse(
                    "content://"
                            + AUTHORITY
                            + "/"
                            + SearchManager.SUGGEST_URI_PATH_QUERY
                            + "/"
                            + KEYWORD);
    private static final Uri WRONG_SEARCH_URI =
            Uri.parse("content://" + AUTHORITY + "/wrong_path/" + KEYWORD);

    private BaseSingletons mOldTvSingletons;
    MockTvSingletons mMockTvSingletons;
    @Mock PerformanceMonitor mMockPerformanceMointor;
    @Mock SearchInterface mMockSearchInterface;

    public LocalSearchProviderTest() {
        super(LocalSearchProvider.class, AUTHORITY);
    }

    @Before
    @Override
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        setContext(getTargetContext());
        mOldTvSingletons = TvApplication.sSingletons;
        mMockTvSingletons = new MockTvSingletons(getTargetContext());
        mMockTvSingletons.setPerformanceMonitor(mMockPerformanceMointor);
        TvApplication.sSingletons = mMockTvSingletons;
        super.setUp();
        getProvider().setSearchInterface(mMockSearchInterface);
    }

    @After
    @Override
    public void tearDown() throws Exception {
        TvApplication.sSingletons = mOldTvSingletons;
        super.tearDown();
    }

    @Test
    public void testQuery_normalUri() {
        verifyQueryWithArguments(null, null);
        verifyQueryWithArguments(1, null);
        verifyQueryWithArguments(null, 1);
        verifyQueryWithArguments(1, 1);
    }

    @Test
    public void testQuery_invalidUri() {
        try (Cursor c = getProvider().query(WRONG_SEARCH_URI, null, null, null, null)) {
            fail("Query with invalid URI should fail.");
        } catch (IllegalArgumentException e) {
            // Success.
        }
    }

    @Test
    public void testQuery_invalidLimit() {
        verifyQueryWithArguments(-1, null);
    }

    @Test
    public void testQuery_invalidAction() {
        verifyQueryWithArguments(null, SearchInterface.ACTION_TYPE_START - 1);
        verifyQueryWithArguments(null, SearchInterface.ACTION_TYPE_END + 1);
    }

    private void verifyQueryWithArguments(Integer limit, Integer action) {
        Uri uri = BASE_SEARCH_URI;
        if (limit != null || action != null) {
            Uri.Builder builder = uri.buildUpon();
            if (limit != null) {
                builder.appendQueryParameter(
                        SearchManager.SUGGEST_PARAMETER_LIMIT, limit.toString());
            }
            if (action != null) {
                builder.appendQueryParameter(
                        LocalSearchProvider.SUGGEST_PARAMETER_ACTION, action.toString());
            }
            uri = builder.build();
        }
        try (Cursor c = getProvider().query(uri, null, null, null, null)) {
            // Do nothing.
        }
        int expectedLimit =
                limit == null || limit <= 0 ? LocalSearchProvider.DEFAULT_SEARCH_LIMIT : limit;
        int expectedAction =
                (action == null
                                || action < SearchInterface.ACTION_TYPE_START
                                || action > SearchInterface.ACTION_TYPE_END)
                        ? LocalSearchProvider.DEFAULT_SEARCH_ACTION
                        : action;
        verify(mMockSearchInterface).search(KEYWORD, expectedLimit, expectedAction);
        clearInvocations(mMockSearchInterface);
    }
}

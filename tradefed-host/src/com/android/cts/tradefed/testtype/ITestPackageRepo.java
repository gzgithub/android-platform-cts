/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.cts.tradefed.testtype;

import com.android.cts.util.AbiUtils;

import java.util.Collection;
import java.util.Set;

/**
 * Interface for accessing tests from the CTS repository.
 */
public interface ITestPackageRepo {

    /**
     * Get a {@link TestPackageDef} given an id.
     *
     * @param id the unique identifier of this test package, generated by
     * {@link AbiUtils#createId(String, String)}.
     * @return a {@link TestPackageDef}
     */
    public ITestPackageDef getTestPackage(String id);

    /**
     * Get a set of {@link TestPackageDef} given a name
     *
     * @param name the string package name
     * @return a {@link Set} of {@link TestPackageDef}
     */
    public Set<ITestPackageDef> getTestPackages(String name);

    /**
     * Attempt to find the package ids for a given test class name
     *
     * @param testClassName the test class name
     * @return a {@link Set} of package ids.
     */
    public Set<String> findPackageIdsForTest(String testClassName);

    /**
     * return a sorted {@link Collection} of all package ids found in repo.
     */
    public Collection<String> getPackageIds();

    /**
     * return a sorted {@link Collection} of all package names found in repo.
     */
    public Collection<String> getPackageNames();

}

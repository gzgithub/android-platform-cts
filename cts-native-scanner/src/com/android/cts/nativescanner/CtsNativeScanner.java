/*
 * Copyright (C) 2011 The Android Open Source Project
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
package com.android.cts.nativescanner;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * Class that searches a source directory for native gTests and outputs a
 * list of test classes and methods.
 */
public class CtsNativeScanner {

    private static void usage(String[] args) {
        System.err.println("Arguments: " + Arrays.asList(args));
        System.err.println("Usage: cts-native-scanner -s SOURCE_DIR");
        System.exit(1);
    }

    public static void main(String[] args) throws Exception {
        File sourceDir = null;

        for (int i = 0; i < args.length; i++) {
            if ("-s".equals(args[i])) {
                if (i + 1 < args.length) {
                    sourceDir = new File(args[++i]);
                } else {
                    System.err.println("Missing value for source directory");
                    usage(args);
                }
            } else {
                System.err.println("Unsupported flag: " + args[i]);
                usage(args);
            }
        }

        if (sourceDir == null) {
            System.out.println("Source directory is required");
            usage(args);
        }

        TestScanner scanner = new TestScanner(sourceDir);
        List<String> testNames = scanner.getTestNames();
        for (String name : testNames) {
            System.out.println(name);
        }
    }
}

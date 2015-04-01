/*
 * Copyright (C) 2008 The Android Open Source Project
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

package util.build;

import com.android.jack.Jack;
import com.android.jack.Main;
import com.android.jack.Options;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class JackDexBuildStep extends BuildStep {

    private final boolean deleteInputFileAfterBuild;

    JackDexBuildStep(BuildFile inputFile, BuildFile outputFile,
            boolean deleteInputFileAfterBuild) {
        super(inputFile, outputFile);
        this.deleteInputFileAfterBuild = deleteInputFileAfterBuild;
    }

    @Override
    boolean build() {

        if (super.build()) {
            String outputFilePath = outputFile.fileName.getAbsolutePath();
            if (outputFilePath.endsWith(".dex")) {
              throw new AssertionError(
                  "DexBuildStep does not support dex output outside of an archive");
            }

            File outDir = outputFile.fileName.getParentFile();
            if (!outDir.exists() && !outDir.mkdirs()) {
                System.err.println("failed to create output dir: "
                        + outDir.getAbsolutePath());
                return false;
            }

            List<String> commandLine = new ArrayList<String>(4);
            commandLine.add("--verbose");
            commandLine.add("error");
            commandLine.add("--output-dex-zip");
            commandLine.add(outputFilePath);
            commandLine.add("--import");
            commandLine.add(inputFile.fileName.getAbsolutePath());

            try {
               Options options = Main.parseCommandLine(commandLine);
               Jack.checkAndRun(options);
                if (deleteInputFileAfterBuild) {
                    inputFile.fileName.delete();
                }
                return true;
            } catch (Throwable ex) {
                System.err.println("exception while dexing "
                        + inputFile.fileName.getAbsolutePath() + " to "
                        + outputFile.fileName.getAbsolutePath());
                ex.printStackTrace();
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return inputFile.hashCode() ^ outputFile.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (super.equals(obj)) {
            JackDexBuildStep other = (JackDexBuildStep) obj;

            return inputFile.equals(other.inputFile)
                    && outputFile.equals(other.outputFile);
        }
        return false;
    }


}

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

package com.android.cts.tradefed.result;

import com.android.cts.tradefed.build.CtsBuildHelper;
import com.android.cts.tradefed.device.DeviceInfoCollector;
import com.android.cts.tradefed.testtype.CtsTest;
import com.android.ddmlib.Log;
import com.android.ddmlib.Log.LogLevel;
import com.android.ddmlib.testrunner.TestIdentifier;
import com.android.tradefed.build.IBuildInfo;
import com.android.tradefed.build.IFolderBuildInfo;
import com.android.tradefed.config.Option;
import com.android.tradefed.log.LogUtil.CLog;
import com.android.tradefed.result.CollectingTestListener;
import com.android.tradefed.result.InputStreamSource;
import com.android.tradefed.result.LogDataType;
import com.android.tradefed.result.TestResult;
import com.android.tradefed.result.TestRunResult;
import com.android.tradefed.util.FileUtil;
import com.android.tradefed.util.StreamUtil;

import org.kxml2.io.KXmlSerializer;

import android.tests.getinfo.DeviceInfoConstants;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

/**
 * Writes results to an XML files in the CTS format.
 * <p/>
 * Collects all test info in memory, then dumps to file when invocation is complete.
 * <p/>
 * Outputs xml in format governed by the cts_result.xsd
 */
public class CtsXmlResultReporter extends CollectingTestListener {

    private static final String LOG_TAG = "CtsXmlResultReporter";

    private static final String TEST_RESULT_FILE_NAME = "testResult.xml";
    private static final String CTS_RESULT_FILE_VERSION = "1.11";
    private static final String CTS_VERSION = "ICS_tradefed";

    private static final String[] CTS_RESULT_RESOURCES = {"cts_result.xsl", "cts_result.css",
        "logo.gif", "newrule-green.png"};

    /** the XML namespace */
    static final String ns = null;

    private static final String REPORT_DIR_NAME = "output-file-path";
    @Option(name=REPORT_DIR_NAME, description="root file system path to directory to store xml " +
            "test results and associated logs. If not specified, results will be stored at " +
            "<cts root>/repository/results")
    protected File mReportDir = null;

    // listen in on the plan option provided to CtsTest
    @Option(name = CtsTest.PLAN_OPTION, description = "the test plan to run.")
    private String mPlanName = "NA";

    protected IBuildInfo mBuildInfo;

    private String mStartTime;

    private String mReportPath = "";

    public void setReportDir(File reportDir) {
        mReportDir = reportDir;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void invocationStarted(IBuildInfo buildInfo) {
        super.invocationStarted(buildInfo);
        if (mReportDir == null) {
            if (!(buildInfo instanceof IFolderBuildInfo)) {
                throw new IllegalArgumentException("build info is not a IFolderBuildInfo");
            }

            IFolderBuildInfo ctsBuild = (IFolderBuildInfo)buildInfo;
            try {
                CtsBuildHelper buildHelper = new CtsBuildHelper(ctsBuild.getRootDir());
                buildHelper.validateStructure();
                mReportDir = buildHelper.getResultsDir();
            } catch (FileNotFoundException e) {
                throw new IllegalArgumentException("Invalid CTS build", e);
            }
        }
        // create a unique directory for saving results, using old cts host convention
        // TODO: in future, consider using LogFileSaver to create build-specific directories
        mReportDir = new File(mReportDir, TimeUtil.getResultTimestamp());
        mReportDir.mkdirs();
        mStartTime = getTimestamp();
        Log.logAndDisplay(LogLevel.INFO, LOG_TAG, String.format("Using ctsbuild %s",
                mReportDir.getAbsolutePath()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void testLog(String dataName, LogDataType dataType, InputStreamSource dataStream) {
        // save as zip file in report dir
        // TODO: ensure uniqueness of file name
        // TODO: use dataType.getFileExt() when its made public
        String fileName = String.format("%s.%s", dataName, dataType.name().toLowerCase());
        // TODO: consider compressing large files
        File logFile = new File(mReportDir, fileName);
        try {
            FileUtil.writeToFile(dataStream.createInputStream(), logFile);
        } catch (IOException e) {
            Log.e(LOG_TAG, String.format("Failed to write log %s", logFile.getAbsolutePath()));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void testFailed(TestFailure status, TestIdentifier test, String trace) {
        super.testFailed(status, test, trace);
        Log.i(LOG_TAG, String.format("Test %s#%s: %s\n%s", test.getClassName(), test.getTestName(),
                status.toString(), trace));
    }

    @Override
    public void testEnded(TestIdentifier test, Map<String, String> testMetrics) {
        super.testEnded(test, testMetrics);
        TestRunResult results = getCurrentRunResults();
        TestResult result = results.getTestResults().get(test);
        Log.logAndDisplay(LogLevel.INFO, LOG_TAG,
                String.format("%s#%s %s", test.getClassName(), test.getTestName(),
                        result.getStatus()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void testRunEnded(long elapsedTime, Map<String, String> runMetrics) {
        super.testRunEnded(elapsedTime, runMetrics);
        CLog.i("%s complete: Passed %d, Failed %d, Not Executed %d",
                getCurrentRunResults().getName(), getCurrentRunResults().getNumPassedTests(),
                getCurrentRunResults().getNumFailedTests() +
                getCurrentRunResults().getNumErrorTests(),
                getCurrentRunResults().getNumIncompleteTests());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void invocationEnded(long elapsedTime) {
        super.invocationEnded(elapsedTime);
        createXmlResult(mReportDir, mStartTime, elapsedTime);
        copyFormattingFiles(mReportDir);
        zipResults(mReportDir);
    }

    /**
     * Creates a report file and populates it with the report data from the completed tests.
     */
    private void createXmlResult(File reportDir, String startTimestamp, long elapsedTime) {
        String endTime = getTimestamp();

        OutputStream stream = null;
        try {
            stream = createOutputResultStream(reportDir);
            KXmlSerializer serializer = new KXmlSerializer();
            serializer.setOutput(stream, "UTF-8");
            serializer.startDocument("UTF-8", false);
            serializer.setFeature(
                    "http://xmlpull.org/v1/doc/features.html#indent-output", true);
            serializer.processingInstruction("xml-stylesheet type=\"text/xsl\"  " +
                    "href=\"cts_result.xsl\"");
            serializeResultsDoc(serializer, startTimestamp, endTime);
            serializer.endDocument();
            // TODO: output not executed timeout omitted counts
            String msg = String.format("XML test result file generated at %s. Passed %d, " +
                    "Failed %d, Not Executed %d", getReportPath(), getNumPassedTests(),
                    getNumFailedTests() + getNumErrorTests(), getNumIncompleteTests());
            Log.logAndDisplay(LogLevel.INFO, LOG_TAG, msg);
            Log.logAndDisplay(LogLevel.INFO, LOG_TAG, String.format("Time: %s",
                    TimeUtil.formatElapsedTime(elapsedTime)));
        } catch (IOException e) {
            Log.e(LOG_TAG, "Failed to generate report data");
        } finally {
            StreamUtil.closeStream(stream);
        }
    }

    private String getReportPath() {
        return mReportPath;
    }

    /**
     * Output the results XML.
     *
     * @param serializer the {@link KXmlSerializer} to use
     * @param startTime the user-friendly starting time of the test invocation
     * @param endTime the user-friendly ending time of the test invocation
     * @throws IOException
     */
    private void serializeResultsDoc(KXmlSerializer serializer, String startTime, String endTime)
            throws IOException {
        serializer.startTag(ns, "TestResult");
        serializer.attribute(ns, "testPlan", mPlanName);
        serializer.attribute(ns, "starttime", startTime);
        serializer.attribute(ns, "endtime", endTime);
        serializer.attribute(ns, "version", CTS_RESULT_FILE_VERSION);

        serializeDeviceInfo(serializer);
        serializeHostInfo(serializer);
        serializeTestSummary(serializer);
        serializeTestResults(serializer);
    }

    /**
     * Output the device info XML.
     *
     * @param serializer
     */
    private void serializeDeviceInfo(KXmlSerializer serializer) throws IOException {
        serializer.startTag(ns, "DeviceInfo");

        TestRunResult deviceInfoResult = findRunResult(DeviceInfoCollector.APP_PACKAGE_NAME);
        if (deviceInfoResult == null) {
            Log.w(LOG_TAG, String.format("Could not find device info run %s",
                    DeviceInfoCollector.APP_PACKAGE_NAME));
            return;
        }
        // Extract metrics that need extra handling, and then dump the remainder into BuildInfo
        Map<String, String> metricsCopy = new HashMap<String, String>(
                deviceInfoResult.getRunMetrics());
        serializer.startTag(ns, "Screen");
        String screenWidth = metricsCopy.remove(DeviceInfoConstants.SCREEN_WIDTH);
        String screenHeight = metricsCopy.remove(DeviceInfoConstants.SCREEN_HEIGHT);
        serializer.attribute(ns, "resolution", String.format("%sx%s", screenWidth, screenHeight));
        serializer.attribute(ns, DeviceInfoConstants.SCREEN_DENSITY,
                metricsCopy.remove(DeviceInfoConstants.SCREEN_DENSITY));
        serializer.attribute(ns, DeviceInfoConstants.SCREEN_DENSITY_BUCKET,
                metricsCopy.remove(DeviceInfoConstants.SCREEN_DENSITY_BUCKET));
        serializer.attribute(ns, DeviceInfoConstants.SCREEN_SIZE,
                metricsCopy.remove(DeviceInfoConstants.SCREEN_SIZE));
        serializer.endTag(ns, "Screen");

        serializer.startTag(ns, "PhoneSubInfo");
        serializer.attribute(ns, "subscriberId", metricsCopy.remove(
                DeviceInfoConstants.PHONE_NUMBER));
        serializer.endTag(ns, "PhoneSubInfo");

        String featureData = metricsCopy.remove(DeviceInfoConstants.FEATURES);
        String processData = metricsCopy.remove(DeviceInfoConstants.PROCESSES);

        // dump the remaining metrics without translation
        serializer.startTag(ns, "BuildInfo");
        for (Map.Entry<String, String> metricEntry : metricsCopy.entrySet()) {
            serializer.attribute(ns, metricEntry.getKey(), metricEntry.getValue());
        }
        serializer.attribute(ns, "deviceID", getBuildInfo().getDeviceSerial());
        serializer.endTag(ns, "BuildInfo");

        serializeFeatureInfo(serializer, featureData);
        serializeProcessInfo(serializer, processData);

        serializer.endTag(ns, "DeviceInfo");
    }

    /**
     * Prints XML indicating what features are supported by the device. It parses a string from the
     * featureData argument that is in the form of "feature1:true;feature2:false;featuer3;true;"
     * with a trailing semi-colon.
     *
     * <pre>
     *  <FeatureInfo>
     *     <Feature name="android.name.of.feature" available="true" />
     *     ...
     *   </FeatureInfo>
     * </pre>
     *
     * @param serializer used to create XML
     * @param featureData raw unparsed feature data
     */
    private void serializeFeatureInfo(KXmlSerializer serializer, String featureData) throws IOException {
        serializer.startTag(ns, "FeatureInfo");

        if (featureData == null) {
            featureData = "";
        }

        String[] featurePairs = featureData.split(";");
        for (String featurePair : featurePairs) {
            String[] nameTypeAvailability = featurePair.split(":");
            if (nameTypeAvailability.length >= 3) {
                serializer.startTag(ns, "Feature");
                serializer.attribute(ns, "name", nameTypeAvailability[0]);
                serializer.attribute(ns, "type", nameTypeAvailability[1]);
                serializer.attribute(ns, "available", nameTypeAvailability[2]);
                serializer.endTag(ns, "Feature");
            }
        }
        serializer.endTag(ns, "FeatureInfo");
    }

    /**
     * Prints XML data indicating what particular processes of interest were running on the device.
     * It parses a string from the rootProcesses argument that is in the form of
     * "processName1;processName2;..." with a trailing semi-colon.
     *
     * <pre>
     *   <ProcessInfo>
     *     <Process name="long_cat_viewer" uid="0" />
     *     ...
     *   </ProcessInfo>
     * </pre>
     *
     * @param document
     * @param parentNode
     * @param deviceInfo
     */
    private void serializeProcessInfo(KXmlSerializer serializer, String rootProcesses)
            throws IOException {
        serializer.startTag(ns, "ProcessInfo");

        if (rootProcesses == null) {
            rootProcesses = "";
        }

        String[] processNames = rootProcesses.split(";");
        for (String processName : processNames) {
            processName = processName.trim();
            if (processName.length() > 0) {
                serializer.startTag(ns, "Process");
                serializer.attribute(ns, "name", processName);
                serializer.attribute(ns, "uid", "0");
                serializer.endTag(ns, "Process");
            }
        }
        serializer.endTag(ns, "ProcessInfo");
    }

    /**
     * Finds the {@link TestRunResult} with the given name.
     *
     * @param runName
     * @return the {@link TestRunResult}
     */
    private TestRunResult findRunResult(String runName) {
        for (TestRunResult runResult : getRunResults()) {
            if (runResult.getName().equals(runName)) {
                return runResult;
            }
        }
        return null;
    }

    /**
     * Output the host info XML.
     *
     * @param serializer
     */
    private void serializeHostInfo(KXmlSerializer serializer) throws IOException {
        serializer.startTag(ns, "HostInfo");

        String hostName = "";
        try {
            hostName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException ignored) {}
        serializer.attribute(ns, "name", hostName);

        serializer.startTag(ns, "Os");
        serializer.attribute(ns, "name", System.getProperty("os.name"));
        serializer.attribute(ns, "version", System.getProperty("os.version"));
        serializer.attribute(ns, "arch", System.getProperty("os.arch"));
        serializer.endTag(ns, "Os");

        serializer.startTag(ns, "Java");
        serializer.attribute(ns, "name", System.getProperty("java.vendor"));
        serializer.attribute(ns, "version", System.getProperty("java.version"));
        serializer.endTag(ns, "Java");

        serializer.startTag(ns, "Cts");
        serializer.attribute(ns, "version", CTS_VERSION);
        // TODO: consider outputting tradefed options here
        serializer.endTag(ns, "Cts");

        serializer.endTag(ns, "HostInfo");
    }

    /**
     * Output the test summary XML containing summary totals for all tests.
     *
     * @param serializer
     * @throws IOException
     */
    private void serializeTestSummary(KXmlSerializer serializer) throws IOException {
        serializer.startTag(ns, "Summary");
        serializer.attribute(ns, "failed", Integer.toString(getNumErrorTests() +
                getNumFailedTests()));
        // TODO: output notExecuted, timeout count
        serializer.attribute(ns, "notExecuted",  Integer.toString(getNumIncompleteTests()));
        // ignore timeouts - these are reported as errors
        serializer.attribute(ns, "timeout", "0");
        serializer.attribute(ns, "pass", Integer.toString(getNumPassedTests()));
        serializer.endTag(ns, "Summary");
    }

    /**
     * Output the detailed test results XML.
     *
     * @param serializer
     * @throws IOException
     */
    private void serializeTestResults(KXmlSerializer serializer) throws IOException {
        for (TestRunResult runResult : getRunResults()) {
            serializeTestRunResult(serializer, runResult);
        }
    }

    /**
     * Output the XML for one test run aka test package.
     *
     * @param serializer
     * @param runResult the {@link TestRunResult}
     * @throws IOException
     */
    private void serializeTestRunResult(KXmlSerializer serializer, TestRunResult runResult)
            throws IOException {
        if (runResult.getName().equals(DeviceInfoCollector.APP_PACKAGE_NAME)) {
            // ignore run results for the info collecting packages
            return;
        }
        serializer.startTag(ns, "TestPackage");
        serializer.attribute(ns, "name", getMetric(runResult, CtsTest.PACKAGE_NAME_METRIC));
        serializer.attribute(ns, "appPackageName", runResult.getName());
        serializer.attribute(ns, "digest", getMetric(runResult, CtsTest.PACKAGE_DIGEST_METRIC));

        // Dump the results.

        // organize the tests into data structures that mirror the expected xml output.
        TestSuiteRoot suiteRoot = new TestSuiteRoot();
        for (Map.Entry<TestIdentifier, TestResult> testEntry : runResult.getTestResults()
                .entrySet()) {
            suiteRoot.insertTest(testEntry.getKey(), testEntry.getValue());
        }
        suiteRoot.serialize(serializer);
        serializer.endTag(ns, "TestPackage");
    }

    /**
     * Helper method to retrieve the metric value with given name, or blank if not found
     *
     * @param runResult
     * @param string
     * @return
     */
    private String getMetric(TestRunResult runResult, String keyName) {
        String value = runResult.getRunMetrics().get(keyName);
        if (value == null) {
            return "";
        }
        return value;
    }

    /**
     * Creates the output stream to use for test results. Exposed for mocking.
     * @param mReportPath
     */
    OutputStream createOutputResultStream(File reportDir) throws IOException {
        File reportFile = new File(reportDir, TEST_RESULT_FILE_NAME);
        Log.i(LOG_TAG, String.format("Created xml report file at %s",
                reportFile.getAbsolutePath()));
        // TODO: convert to path relative to cts root
        mReportPath = reportFile.getAbsolutePath();
        return new FileOutputStream(reportFile);
    }

    /**
     * Copy the xml formatting files stored in this jar to the results directory
     *
     * @param resultsDir
     */
    private void copyFormattingFiles(File resultsDir) {
        for (String resultFileName : CTS_RESULT_RESOURCES) {
            InputStream configStream = getClass().getResourceAsStream(String.format("/%s",
                    resultFileName));
            if (configStream != null) {
                File resultFile = new File(resultsDir, resultFileName);
                try {
                    FileUtil.writeToFile(configStream, resultFile);
                } catch (IOException e) {
                    Log.w(LOG_TAG, String.format("Failed to write %s to file", resultFileName));
                }
            } else {
                Log.w(LOG_TAG, String.format("Failed to load %s from jar", resultFileName));
            }
        }
    }

    /**
     * Zip the contents of the given results directory.
     *
     * @param resultsDir
     */
    private void zipResults(File resultsDir) {
        try {
            // create a file in parent directory, with same name as resultsDir
            File zipResultFile = new File(resultsDir.getParent(), String.format("%s.zip",
                    resultsDir.getName()));
            FileUtil.createZip(resultsDir, zipResultFile);
        } catch (IOException e) {
            Log.w(LOG_TAG, String.format("Failed to create zip for %s", resultsDir.getName()));
        }
    }

    /**
     * Get a String version of the current time.
     * <p/>
     * Exposed so unit tests can mock.
     */
    String getTimestamp() {
        return TimeUtil.getTimestamp();
    }
}

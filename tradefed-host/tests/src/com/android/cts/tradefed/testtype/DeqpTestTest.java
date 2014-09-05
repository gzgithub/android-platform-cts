/*
 * Copyright (C) 2014 The Android Open Source Project
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

import com.android.ddmlib.IShellOutputReceiver;
import com.android.ddmlib.testrunner.ITestRunListener;
import com.android.ddmlib.testrunner.TestIdentifier;
import com.android.tradefed.device.ITestDevice;
import com.android.tradefed.result.ITestInvocationListener;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.easymock.IAnswer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * Unit tests for {@link DeqpTest}.
 */
public class DeqpTestTest extends TestCase {
    private static final String URI = "dEQP-GLES3";
    private static final String CASE_LIST_FILE_NAME = "/sdcard/dEQP-TestCaseList.txt";
    private static final String LOG_FILE_NAME = "/sdcard/TestLog.qpa";
    private static final String INSTRUMENTATION_NAME =
                "com.drawelements.deqp/com.drawelements.deqp.testercore.DeqpInstrumentation";

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    /**
     * Test that result code produces correctly pass or fail.
     */
    private void testResultCode(final String resultCode, boolean pass) throws Exception {
        final TestIdentifier testId = new TestIdentifier("dEQP-GLES3.info", "version");
        final String testPath = "dEQP-GLES3.info.version";
        final String testTrie = "{dEQP-GLES3{info{version}}}";

        /* MultiLineReceiver expects "\r\n" line ending. */
        final String output = "INSTRUMENTATION_STATUS: dEQP-SessionInfo-Name=releaseName\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=SessionInfo\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-SessionInfo-Value=2014.x\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-SessionInfo-Name=releaseId\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=SessionInfo\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-SessionInfo-Value=0xcafebabe\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-SessionInfo-Name=targetName\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=SessionInfo\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-SessionInfo-Value=android\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=BeginSession\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=BeginTestCase\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-BeginTestCase-TestCasePath=" + testPath + "\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-TestCaseResult-Code=" + resultCode + "\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-TestCaseResult-Details=Detail" + resultCode + "\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=TestCaseResult\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=EndTestCase\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=EndSession\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_CODE: 0\r\n";

        ITestDevice mockDevice = EasyMock.createMock(ITestDevice.class);
        ITestInvocationListener mockListener
                = EasyMock.createStrictMock(ITestInvocationListener.class);
        Collection<TestIdentifier> tests = new ArrayList<TestIdentifier>();

        tests.add(testId);

        DeqpTest deqpTest = new DeqpTest(URI, tests);

        EasyMock.expect(mockDevice.executeShellCommand(EasyMock.eq("rm " + CASE_LIST_FILE_NAME)))
                .andReturn("").once();

        EasyMock.expect(mockDevice.executeShellCommand(EasyMock.eq("rm " + LOG_FILE_NAME)))
                .andReturn("").once();

        EasyMock.expect(mockDevice.pushString(testTrie + "\n", CASE_LIST_FILE_NAME)).andReturn(true)
                .once();

        String command = "am instrument -w -e deqpLogFileName \"" + LOG_FILE_NAME
                + "\" -e deqpCmdLine \"--deqp-caselist-file=" + CASE_LIST_FILE_NAME + "\" "
                + INSTRUMENTATION_NAME;

        mockDevice.executeShellCommand(EasyMock.eq(command),
                EasyMock.<IShellOutputReceiver>notNull());

        EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
            @Override
            public Object answer() {
                IShellOutputReceiver receiver
                        = (IShellOutputReceiver)EasyMock.getCurrentArguments()[1];

                receiver.addOutput(output.getBytes(), 0, output.length());
                receiver.flush();

                return null;
            }
        });

        mockListener.testRunStarted(URI, 1);
        EasyMock.expectLastCall().once();

        mockListener.testStarted(EasyMock.eq(testId));
        EasyMock.expectLastCall().once();

        if (!pass) {
            mockListener.testFailed(ITestRunListener.TestFailure.ERROR, testId,
                    resultCode + ":Detail" + resultCode);

            EasyMock.expectLastCall().once();
        }

        mockListener.testEnded(EasyMock.eq(testId), EasyMock.<Map<String, String>>notNull());
        EasyMock.expectLastCall().once();

        mockListener.testRunEnded(EasyMock.anyLong(), EasyMock.<Map<String, String>>notNull());
        EasyMock.expectLastCall().once();

        EasyMock.replay(mockDevice);
        EasyMock.replay(mockListener);

        deqpTest.setDevice(mockDevice);
        deqpTest.run(mockListener);

        EasyMock.verify(mockListener);
        EasyMock.verify(mockDevice);
    }

    /**
     * Test running multiple test cases.
     */
    public void testRun_multipleTets() throws Exception {
        /* MultiLineReceiver expects "\r\n" line ending. */
        final String output = "INSTRUMENTATION_STATUS: dEQP-SessionInfo-Name=releaseName\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=SessionInfo\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-SessionInfo-Value=2014.x\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-SessionInfo-Name=releaseId\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=SessionInfo\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-SessionInfo-Value=0xcafebabe\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-SessionInfo-Name=targetName\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=SessionInfo\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-SessionInfo-Value=android\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=BeginSession\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=BeginTestCase\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-BeginTestCase-TestCasePath=dEQP-GLES3.info.vendor\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-TestCaseResult-Code=Pass\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-TestCaseResult-Details=Pass\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=TestCaseResult\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=EndTestCase\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=BeginTestCase\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-BeginTestCase-TestCasePath=dEQP-GLES3.info.renderer\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-TestCaseResult-Code=Pass\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-TestCaseResult-Details=Pass\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=TestCaseResult\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=EndTestCase\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=BeginTestCase\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-BeginTestCase-TestCasePath=dEQP-GLES3.info.version\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-TestCaseResult-Code=Pass\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-TestCaseResult-Details=Pass\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=TestCaseResult\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=EndTestCase\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=BeginTestCase\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-BeginTestCase-TestCasePath=dEQP-GLES3.info.shading_language_version\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-TestCaseResult-Code=Pass\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-TestCaseResult-Details=Pass\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=TestCaseResult\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=EndTestCase\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=BeginTestCase\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-BeginTestCase-TestCasePath=dEQP-GLES3.info.extensions\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-TestCaseResult-Code=Pass\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-TestCaseResult-Details=Pass\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=TestCaseResult\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=EndTestCase\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=BeginTestCase\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-BeginTestCase-TestCasePath=dEQP-GLES3.info.render_target\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-TestCaseResult-Code=Pass\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-TestCaseResult-Details=Pass\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=TestCaseResult\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=EndTestCase\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=EndSession\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_CODE: 0\r\n";

        final TestIdentifier[] testIds = {
                new TestIdentifier("dEQP-GLES3.info", "vendor"),
                new TestIdentifier("dEQP-GLES3.info", "renderer"),
                new TestIdentifier("dEQP-GLES3.info", "version"),
                new TestIdentifier("dEQP-GLES3.info", "shading_language_version"),
                new TestIdentifier("dEQP-GLES3.info", "extensions"),
                new TestIdentifier("dEQP-GLES3.info", "render_target")
        };

        final String[] testPaths = {
                "dEQP-GLES3.info.vendor",
                "dEQP-GLES3.info.renderer",
                "dEQP-GLES3.info.version",
                "dEQP-GLES3.info.shading_language_version",
                "dEQP-GLES3.info.extensions",
                "dEQP-GLES3.info.render_target"
        };

        final String testTrie
                = "{dEQP-GLES3{info{vendor,renderer,version,shading_language_version,extensions,render_target}}}";

        ITestDevice mockDevice = EasyMock.createMock(ITestDevice.class);
        ITestInvocationListener mockListener = EasyMock.createStrictMock(ITestInvocationListener.class);
        Collection<TestIdentifier> tests = new ArrayList<TestIdentifier>();

        for (TestIdentifier id : testIds) {
            tests.add(id);
        }

        DeqpTest deqpTest = new DeqpTest(URI, tests);

        EasyMock.expect(mockDevice.executeShellCommand(EasyMock.eq("rm " + CASE_LIST_FILE_NAME)))
                .andReturn("").once();

        EasyMock.expect(mockDevice.executeShellCommand(EasyMock.eq("rm " + LOG_FILE_NAME)))
                .andReturn("").once();

        EasyMock.expect(mockDevice.pushString(testTrie + "\n", CASE_LIST_FILE_NAME))
                .andReturn(true).once();

        String command = "am instrument -w -e deqpLogFileName \"" + LOG_FILE_NAME
                + "\" -e deqpCmdLine \"--deqp-caselist-file=" + CASE_LIST_FILE_NAME + "\" "
                + INSTRUMENTATION_NAME;

        mockDevice.executeShellCommand(EasyMock.eq(command),
                EasyMock.<IShellOutputReceiver>notNull());

        EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
            @Override
            public Object answer() {
                IShellOutputReceiver receiver
                        = (IShellOutputReceiver)EasyMock.getCurrentArguments()[1];

                receiver.addOutput(output.getBytes(), 0, output.length());
                receiver.flush();

                return null;
            }
        });

        mockListener.testRunStarted(URI, testPaths.length);
        EasyMock.expectLastCall().once();

        for (int i = 0; i < testPaths.length; i++) {
            mockListener.testStarted(EasyMock.eq(testIds[i]));
            EasyMock.expectLastCall().once();

            mockListener.testEnded(EasyMock.eq(testIds[i]),
                    EasyMock.<Map<String, String>>notNull());

            EasyMock.expectLastCall().once();
        }

        mockListener.testRunEnded(EasyMock.anyLong(), EasyMock.<Map<String, String>>notNull());
        EasyMock.expectLastCall().once();

        EasyMock.replay(mockDevice);
        EasyMock.replay(mockListener);

        deqpTest.setDevice(mockDevice);
        deqpTest.run(mockListener);

        EasyMock.verify(mockListener);
        EasyMock.verify(mockDevice);
    }

    /**
     * Test dEQP Pass result code.
     */
    public void testRun_resultPass() throws Exception {
        testResultCode("Pass", true);
    }

    /**
     * Test dEQP Fail result code.
     */
    public void testRun_resultFail() throws Exception {
        testResultCode("Fail", false);
    }

    /**
     * Test dEQP NotSupported result code.
     */
    public void testRun_resultNotSupported() throws Exception {
        testResultCode("NotSupported", true);
    }

    /**
     * Test dEQP QualityWarning result code.
     */
    public void testRun_resultQualityWarning() throws Exception {
        testResultCode("QualityWarning", true);
    }

    /**
     * Test dEQP CompatibilityWarning result code.
     */
    public void testRun_resultCompatibilityWarning() throws Exception {
        testResultCode("CompatibilityWarning", true);
    }

    /**
     * Test dEQP ResourceError result code.
     */
    public void testRun_resultResourceError() throws Exception {
        testResultCode("ResourceError", false);
    }

    /**
     * Test dEQP InternalError result code.
     */
    public void testRun_resultInternalError() throws Exception {
        testResultCode("InternalError", false);
    }

    /**
     * Test dEQP Crash result code.
     */
    public void testRun_resultCrash() throws Exception {
        testResultCode("Crash", false);
    }

    /**
     * Test dEQP Timeout result code.
     */
    public void testRun_resultTimeout() throws Exception {
        testResultCode("Timeout", false);
    }
}

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
package com.android.cts.tradefed.result;

import com.android.tradefed.result.TestResult;

import org.kxml2.io.KXmlSerializer;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

/**
 * Data structure that represents a "Test" result XML element.
 */
class Test extends AbstractXmlPullParser {

    static final String TAG = "Test";
    private static final String NAME_ATTR = "name";
    private static final String MESSAGE_ATTR = "message";
    private static final String ENDTIME_ATTR = "endtime";
    private static final String STARTTIME_ATTR = "starttime";
    private static final String RESULT_ATTR = "result";
    private static final String SCENE_TAG = "FailedScene";
    private static final String STACK_TAG = "StackTrace";

    private String mName;
    private CtsTestStatus mResult;
    private String mStartTime;
    private String mEndTime;
    private String mMessage;
    private String mStackTrace;

    /**
     * Create an empty {@link Test}
     */
    public Test() {
    }

    /**
     * Create a {@link Test} from a {@link TestResult}.
     *
     * @param name
     */
    public Test(String name) {
        mName = name;
        mResult = CtsTestStatus.NOT_EXECUTED;
        mStartTime = TimeUtil.getTimestamp();
        updateEndTime();
    }

    /**
     * Set the name of this {@link Test}
     */
    public void setName(String name) {
        mName = name;
    }

    /**
     * Get the name of this {@link Test}
     */
    public String getName() {
        return mName;
    }

    public CtsTestStatus getResult() {
        return mResult;
    }

    public String getMessage() {
        return mMessage;
    }

    public String getStartTime() {
        return mStartTime;
    }

    public String getEndTime() {
        return mEndTime;
    }

    public String getStackTrace() {
        return mStackTrace;
    }

    public void setStackTrace(String stackTrace) {

        mStackTrace = sanitizeStackTrace(stackTrace);
        mMessage = getFailureMessageFromStackTrace(mStackTrace);
    }

    public void updateEndTime() {
        mEndTime = TimeUtil.getTimestamp();
    }

    public void setResultStatus(CtsTestStatus status) {
        mResult = status;
    }

    /**
     * Serialize this object and all its contents to XML.
     *
     * @param serializer
     * @throws IOException
     */
    public void serialize(KXmlSerializer serializer)
            throws IOException {
        serializer.startTag(CtsXmlResultReporter.ns, TAG);
        serializer.attribute(CtsXmlResultReporter.ns, NAME_ATTR, getName());
        serializer.attribute(CtsXmlResultReporter.ns, RESULT_ATTR, mResult.getValue());
        serializer.attribute(CtsXmlResultReporter.ns, STARTTIME_ATTR, mStartTime);
        serializer.attribute(CtsXmlResultReporter.ns, ENDTIME_ATTR, mEndTime);

        if (mMessage != null) {
            serializer.startTag(CtsXmlResultReporter.ns, SCENE_TAG);
            serializer.attribute(CtsXmlResultReporter.ns, MESSAGE_ATTR, mMessage);
            if (mStackTrace != null) {
                serializer.startTag(CtsXmlResultReporter.ns, STACK_TAG);
                serializer.text(mStackTrace);
                serializer.endTag(CtsXmlResultReporter.ns, STACK_TAG);
            }
            serializer.endTag(CtsXmlResultReporter.ns, SCENE_TAG);
        }
        serializer.endTag(CtsXmlResultReporter.ns, TAG);
    }

    /**
     * Strip out any invalid XML characters that might cause the report to be unviewable.
     * http://www.w3.org/TR/REC-xml/#dt-character
     */
    private static String sanitizeStackTrace(String trace) {
        if (trace != null) {
            return trace.replaceAll("[^\\u0009\\u000A\\u000D\\u0020-\\uD7FF\\uE000-\\uFFFD]", "");
        } else {
            return null;
        }
    }

    private static String getFailureMessageFromStackTrace(String stack) {
        // This is probably too simplistic to work in all cases, but for now, just return first
        // line of stack as failure message
        int firstNewLine = stack.indexOf('\n');
        if (firstNewLine != -1) {
            return stack.substring(0, firstNewLine);
        }
        return stack;
    }

    /**
     * Populates this class with test result data parsed from XML.
     *
     * @param parser the {@link XmlPullParser}. Expected to be pointing at start
     *            of a Test tag
     */
    @Override
    void parse(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (!parser.getName().equals(TAG)) {
            throw new XmlPullParserException(String.format(
                    "invalid XML: Expected %s tag but received %s", TAG, parser.getName()));
        }
        setName(getAttribute(parser, NAME_ATTR));
        mResult = CtsTestStatus.getStatus(getAttribute(parser, RESULT_ATTR));
        mStartTime = getAttribute(parser, STARTTIME_ATTR);
        mEndTime = getAttribute(parser, ENDTIME_ATTR);

        int eventType = parser.next();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && parser.getName().equals(SCENE_TAG)) {
                mMessage = getAttribute(parser, MESSAGE_ATTR);
            } else if (eventType == XmlPullParser.START_TAG && parser.getName().equals(STACK_TAG)) {
                mStackTrace = parser.nextText();
            } else if (eventType == XmlPullParser.END_TAG && parser.getName().equals(TAG)) {
                return;
            }
            eventType = parser.next();
        }
    }
}

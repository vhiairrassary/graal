/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 * 
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 * 
 * (a) the Software, and
 * 
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 * 
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 * 
 * This license is subject to the following condition:
 * 
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oracle.truffle.nfi.test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.nfi.test.interop.BoxedPrimitive;
import com.oracle.truffle.nfi.test.interop.TestCallback;
import com.oracle.truffle.tck.TruffleRunner;
import com.oracle.truffle.tck.TruffleRunner.Inject;

@RunWith(TruffleRunner.class)
public class StringNFITest extends NFITest {

    public static class StringArgNode extends SendExecuteNode {

        public StringArgNode() {
            super("string_arg", "(string):sint32");
        }
    }

    @Test
    public void testJavaStringArg(@Inject(StringArgNode.class) CallTarget callTarget) {
        Object ret = callTarget.call("42");
        Assert.assertThat("return value", ret, is(instanceOf(Integer.class)));
        Assert.assertEquals("return value", 42, (int) (Integer) ret);
    }

    @Test
    public void testBoxedStringArg(@Inject(StringArgNode.class) CallTarget callTarget) {
        Object ret = callTarget.call(new BoxedPrimitive("42"));
        Assert.assertThat("return value", ret, is(instanceOf(Integer.class)));
        Assert.assertEquals("return value", 42, (int) (Integer) ret);
    }

    public static class NativeStringArgNode extends NFITestRootNode {

        final TruffleObject function = lookupAndBind("string_arg", "(string):sint32");
        final TruffleObject strdup = lookupAndBind(defaultLibrary, "strdup", "(string):string");
        final TruffleObject free = lookupAndBind(defaultLibrary, "free", "(pointer):void");

        @Child Node executeFunction = Message.EXECUTE.createNode();
        @Child Node executeStrdup = Message.EXECUTE.createNode();
        @Child Node executeFree = Message.EXECUTE.createNode();

        @Override
        public Object executeTest(VirtualFrame frame) throws InteropException {
            Object nativeString = ForeignAccess.sendExecute(executeStrdup, strdup, frame.getArguments()[0]);
            Object ret = ForeignAccess.sendExecute(executeFunction, function, nativeString);
            ForeignAccess.sendExecute(executeFree, free, nativeString);
            return ret;
        }
    }

    @Test
    public void testNativeStringArg(@Inject(NativeStringArgNode.class) CallTarget callTarget) {
        Object retObj = callTarget.call("8472");
        Assert.assertThat("return value", retObj, is(instanceOf(Integer.class)));
        Assert.assertEquals("return value", 8472, (int) (Integer) retObj);
    }

    public static class StringRetConstNode extends SendExecuteNode {

        public StringRetConstNode() {
            super("string_ret_const", "():string");
        }
    }

    @Test
    public void testStringRetConst(@Inject(StringRetConstNode.class) CallTarget callTarget) {
        Object ret = callTarget.call();

        Assert.assertThat("return value", ret, is(instanceOf(TruffleObject.class)));
        TruffleObject obj = (TruffleObject) ret;

        Assert.assertTrue("isBoxed", isBoxed(obj));
        Assert.assertEquals("return value", "Hello, World!", unbox(obj));
    }

    public static class StringRetDynamicNode extends NFITestRootNode {

        final TruffleObject function = lookupAndBind("string_ret_dynamic", "(sint32):string");
        final TruffleObject free = lookupAndBind("free_dynamic_string", "(pointer):sint32");

        @Child Node executeFunction = Message.EXECUTE.createNode();
        @Child Node executeFree = Message.EXECUTE.createNode();

        @Override
        public Object executeTest(VirtualFrame frame) throws InteropException {
            Object ret = ForeignAccess.sendExecute(executeFunction, function, frame.getArguments()[0]);

            checkRet(ret);

            /*
             * Normally here we'd just call "free" from libc. We're using a wrapper to be able to
             * reliably test whether it was called with the correct argument.
             */
            Object magic = ForeignAccess.sendExecute(executeFree, free, ret);
            assertEquals(42, magic);

            return null;
        }

        @TruffleBoundary
        private static void checkRet(Object ret) {
            Assert.assertThat("return value", ret, is(instanceOf(TruffleObject.class)));
            TruffleObject obj = (TruffleObject) ret;

            Assert.assertTrue("isBoxed", isBoxed(obj));
            Assert.assertEquals("return value", "42", unbox(obj));
        }

    }

    @Test
    public void testStringRetDynamic(@Inject(StringRetDynamicNode.class) CallTarget target) {
        target.call(42);
    }

    public static class StringCallbackNode extends SendExecuteNode {

        public StringCallbackNode() {
            super("string_callback", "( (string):sint32, ():string ) : sint32");
        }
    }

    private static void testStringCallback(CallTarget target, Object callbackRet) {
        TruffleObject strArgCallback = new TestCallback(1, (args) -> {
            Assert.assertEquals("string argument", "Hello, Truffle!", args[0]);
            return 42;
        });
        TruffleObject strRetCallback = new TestCallback(0, (args) -> {
            return callbackRet;
        });

        Object ret = target.call(strArgCallback, strRetCallback);

        Assert.assertThat("return value", ret, is(instanceOf(Integer.class)));
        Assert.assertEquals("return value", 42, (int) (Integer) ret);
    }

    @Test
    public void testStringCallback(@Inject(StringCallbackNode.class) CallTarget target) {
        testStringCallback(target, "Hello, Native!");
    }

    @Test
    public void testBoxedStringCallback(@Inject(StringCallbackNode.class) CallTarget target) {
        testStringCallback(target, new BoxedPrimitive("Hello, Native!"));
    }

    public static class NativeStringCallbackNode extends NFITestRootNode {

        final TruffleObject stringRetConst = lookupAndBind("string_ret_const", "():string");
        final TruffleObject nativeStringCallback = lookupAndBind("native_string_callback", "(():string) : string");

        @Child Node executeStringRetConst = Message.EXECUTE.createNode();
        @Child Node executeNativeStringCallback = Message.EXECUTE.createNode();

        @Override
        public Object executeTest(VirtualFrame frame) throws InteropException {
            Object string = ForeignAccess.sendExecute(executeStringRetConst, stringRetConst);
            TruffleObject callback = createCallback(string);
            return ForeignAccess.sendExecute(executeNativeStringCallback, nativeStringCallback, callback);
        }

        @TruffleBoundary
        private static TruffleObject createCallback(Object obj) {
            return new TestCallback(0, (args) -> {
                return obj;
            });
        }
    }

    @Test
    public void testNativeStringCallback(@Inject(NativeStringCallbackNode.class) CallTarget target) {
        Object ret = target.call();

        Assert.assertThat("return value", ret, is(instanceOf(TruffleObject.class)));
        TruffleObject obj = (TruffleObject) ret;

        Assert.assertTrue("isBoxed", isBoxed(obj));
        Assert.assertEquals("return value", "same", unbox(obj));
    }
}

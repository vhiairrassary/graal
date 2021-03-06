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
package com.oracle.truffle.api.test.host;

import static org.junit.Assert.assertEquals;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Assert;
import org.junit.Test;

import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;

public class VarArgsTest extends ProxyLanguageEnvTest {

    @Test
    public void testStringJoin1() throws InteropException {
        TruffleObject strClass = asTruffleHostSymbol(String.class);

        TruffleObject join = (TruffleObject) ForeignAccess.sendRead(Message.READ.createNode(), strClass, "join");
        TruffleObject delimiter = asTruffleObject(" ");
        TruffleObject elements = asTruffleObject(new String[]{"Hello", "World"});
        Object result = ForeignAccess.sendExecute(Message.EXECUTE.createNode(), join, new Object[]{delimiter, elements});
        Assert.assertEquals("Hello World", result);
    }

    @Test
    public void testStringJoin2() throws InteropException {
        TruffleObject strClass = asTruffleHostSymbol(String.class);

        TruffleObject join = (TruffleObject) ForeignAccess.sendRead(Message.READ.createNode(), strClass, "join");
        TruffleObject delimiter = asTruffleObject(" ");
        TruffleObject element1 = asTruffleObject("Hello");
        TruffleObject element2 = asTruffleObject("World");
        Object result = ForeignAccess.sendExecute(Message.EXECUTE.createNode(), join, new Object[]{delimiter, element1, element2});
        Assert.assertEquals("Hello World", result);
    }

    @Test
    public void testStringEllipsis() throws InteropException {
        TruffleObject mainClass = asTruffleHostSymbol(Join.class);

        TruffleObject ellipsis = (TruffleObject) ForeignAccess.sendRead(Message.READ.createNode(), mainClass, "stringEllipsis");
        TruffleObject element1 = asTruffleObject("Hello");
        TruffleObject element2 = asTruffleObject("World");
        Object result = ForeignAccess.sendExecute(Message.EXECUTE.createNode(), ellipsis, new Object[]{element1, element2});
        Assert.assertEquals("Hello World", result);

        TruffleObject elements = asTruffleObject(new String[]{"Hello", "World"});
        result = ForeignAccess.sendExecute(Message.EXECUTE.createNode(), ellipsis, elements);
        Assert.assertEquals("Hello World", result);
    }

    @Test
    public void testCharSequenceEllipsis() throws InteropException {
        TruffleObject mainClass = asTruffleHostSymbol(Join.class);

        TruffleObject ellipsis = (TruffleObject) ForeignAccess.sendRead(Message.READ.createNode(), mainClass, "charSequenceEllipsis");
        TruffleObject element1 = asTruffleObject("Hello");
        TruffleObject element2 = asTruffleObject("World");
        Object result = ForeignAccess.sendExecute(Message.EXECUTE.createNode(), ellipsis, new Object[]{element1, element2});
        Assert.assertEquals("Hello World", result);

        TruffleObject elements = asTruffleObject(new String[]{"Hello", "World"});
        result = ForeignAccess.sendExecute(Message.EXECUTE.createNode(), ellipsis, elements);
        Assert.assertEquals("Hello World", result);
    }

    @Test
    public void testPathsGet() throws InteropException {
        Node n = Message.INVOKE.createNode();
        TruffleObject paths = asTruffleHostSymbol(Paths.class);
        TruffleObject result;
        result = (TruffleObject) ForeignAccess.sendInvoke(n, paths, "get", "dir");
        assertEquals("dir", asJavaObject(Path.class, result).toString());
        result = (TruffleObject) ForeignAccess.sendInvoke(n, paths, "get", "dir1", "dir2");
        assertEquals("dir1/dir2", asJavaObject(Path.class, result).toString());
        result = (TruffleObject) ForeignAccess.sendInvoke(n, paths, "get", "dir1", "dir2", "dir3");
        assertEquals("dir1/dir2/dir3", asJavaObject(Path.class, result).toString());
        result = (TruffleObject) ForeignAccess.sendInvoke(n, paths, "get", "dir1", asTruffleObject(new String[]{"dir2", "dir3"}));
        assertEquals("dir1/dir2/dir3", asJavaObject(Path.class, result).toString());
    }

    @Test
    public void testOverloadedVarArgsPrimitive() throws InteropException {
        Node n = Message.INVOKE.createNode();
        TruffleObject paths = asTruffleHostSymbol(Sum.class);
        Object result;
        result = ForeignAccess.sendInvoke(n, paths, "sum", 10);
        assertEquals("I", result);
        result = ForeignAccess.sendInvoke(n, paths, "sum", 10, 20);
        assertEquals("DD", result);
        result = ForeignAccess.sendInvoke(n, paths, "sum", 10, 20, 30);
        assertEquals("I[I", result);
    }

    public static class Join {
        public static String stringEllipsis(String... args) {
            return String.join(" ", args);
        }

        public static String charSequenceEllipsis(CharSequence... args) {
            return String.join(" ", args);
        }
    }

    @SuppressWarnings("unused")
    public static class Sum {
        public static String sum(int first) {
            return "I";
        }

        public static String sum(int first, int... more) {
            return "I[I";
        }

        public static String sum(double first) {
            return "D";
        }

        public static String sum(double first, double second) {
            return "DD";
        }

        public static String sum(double first, double... more) {
            return "D[D";
        }
    }
}

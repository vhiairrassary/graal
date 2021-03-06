/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.polyglot;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.MessageResolution;
import com.oracle.truffle.api.interop.Resolve;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;

final class HostFunction implements TruffleObject {

    final HostMethodDesc method;
    final Object obj;
    final PolyglotLanguageContext languageContext;

    HostFunction(HostMethodDesc method, Object obj, PolyglotLanguageContext languageContext) {
        this.method = method;
        this.obj = obj;
        this.languageContext = languageContext;
    }

    public static boolean isInstance(TruffleObject obj) {
        return obj instanceof HostFunction;
    }

    public static boolean isInstance(Object obj) {
        return obj instanceof HostFunction;
    }

    @Override
    public ForeignAccess getForeignAccess() {
        return HostFunctionMRForeign.ACCESS;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof HostFunction) {
            HostFunction other = (HostFunction) o;
            return this.method == other.method && this.obj == other.obj && this.languageContext == other.languageContext;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return method.hashCode();
    }

    String getDescription() {
        if (obj == null) {
            return "null";
        }
        String typeName = obj.getClass().getTypeName();
        return typeName + "." + method.getName();
    }

}

@MessageResolution(receiverType = HostFunction.class)
class HostFunctionMR {

    @Resolve(message = "EXECUTE")
    abstract static class ExecuteNode extends Node {

        @Child private HostExecuteNode doExecute;

        public Object access(HostFunction function, Object[] args) {
            if (doExecute == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                doExecute = insert(HostExecuteNode.create());
            }
            return doExecute.execute(function.method, function.obj, args, function.languageContext);
        }
    }

    @Resolve(message = "IS_EXECUTABLE")
    abstract static class IsExecutableNode extends Node {

        public Object access(@SuppressWarnings("unused") HostFunction receiver) {
            return Boolean.TRUE;
        }

    }

}

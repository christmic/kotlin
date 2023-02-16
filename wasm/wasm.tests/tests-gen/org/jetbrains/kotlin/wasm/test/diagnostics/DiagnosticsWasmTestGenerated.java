/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.diagnostics;

import com.intellij.testFramework.TestDataPath;
import org.jetbrains.kotlin.test.util.KtTestUtil;
import org.jetbrains.kotlin.test.TestMetadata;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.regex.Pattern;

/** This class is generated by {@link org.jetbrains.kotlin.generators.tests.GenerateWasmTestsKt}. DO NOT MODIFY MANUALLY */
@SuppressWarnings("all")
@TestMetadata("compiler/testData/diagnostics/wasmTests")
@TestDataPath("$PROJECT_ROOT")
public class DiagnosticsWasmTestGenerated extends AbstractDiagnosticsWasmTest {
    @Test
    public void testAllFilesPresentInWasmTests() throws Exception {
        KtTestUtil.assertAllTestsPresentByMetadataWithExcluded(this.getClass(), new File("compiler/testData/diagnostics/wasmTests"), Pattern.compile("^(.+)\\.kt$"), null, true);
    }

    @Nested
    @TestMetadata("compiler/testData/diagnostics/wasmTests/jsInterop")
    @TestDataPath("$PROJECT_ROOT")
    public class JsInterop {
        @Test
        public void testAllFilesPresentInJsInterop() throws Exception {
            KtTestUtil.assertAllTestsPresentByMetadataWithExcluded(this.getClass(), new File("compiler/testData/diagnostics/wasmTests/jsInterop"), Pattern.compile("^(.+)\\.kt$"), null, true);
        }

        @Test
        @TestMetadata("dynamicUnsupported.kt")
        public void testDynamicUnsupported() throws Exception {
            runTest("compiler/testData/diagnostics/wasmTests/jsInterop/dynamicUnsupported.kt");
        }

        @Test
        @TestMetadata("inheritance.kt")
        public void testInheritance() throws Exception {
            runTest("compiler/testData/diagnostics/wasmTests/jsInterop/inheritance.kt");
        }
    }
}

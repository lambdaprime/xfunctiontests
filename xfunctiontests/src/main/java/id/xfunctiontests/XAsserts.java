/*
 * Copyright 2022 lambdaprime
 * 
 * Website: https://github.com/lambdaprime/xfunctiontests
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package id.xfunctiontests;

import id.xfunction.ResourceUtils;
import id.xfunction.text.WildcardMatcher;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.opentest4j.AssertionFailedError;

/**
 * @author lambdaprime intid@protonmail.com
 */
public class XAsserts {

    private static final ResourceUtils resourceUtils = new ResourceUtils();

    public static void assertMatchesAll(Class<?> clazz, String resourceWithTemplates, String str)
            throws AssertionError {
        List<String> templates = resourceUtils.readResourceAsList(clazz, resourceWithTemplates);
        for (var template : templates) {
            assertMatches(template, str);
        }
    }

    public static void assertMatches(Class<?> clazz, String resourceTemplate, String str)
            throws AssertionError {
        assertMatches(resourceUtils.readResource(clazz, resourceTemplate), str);
    }

    public static void assertMatches(String template, String str) throws AssertionError {
        // change line endings to UNIX
        str = str.replace(System.lineSeparator(), "\n");
        if (!new WildcardMatcher(template).matches(str))
            throw new AssertionFailedError(
                    String.format("expected template <%s>, actual text <%s>", template, str),
                    template,
                    str);
    }

    public static void assertEquals(
            Class<?> clazz, String resourceWithExpectedString, String actualString) {
        var expectedString = resourceUtils.readResource(clazz, resourceWithExpectedString);
        if (!Objects.equals(expectedString, actualString))
            throw new AssertionFailedError(
                    String.format("expected <%s>, actual text <%s>", expectedString, actualString),
                    expectedString,
                    actualString);
    }

    /**
     * Assert that two files or two folders are equal. Folders are compared recursively.
     *
     * @throws IOException
     */
    public static void assertContentEquals(Path expectedSource, Path actualSource)
            throws IOException {
        var expectedFile = expectedSource.toFile();
        var actualFile = actualSource.toFile();
        if (expectedFile.isFile() && actualFile.isFile()) {
            assertFileContentEquals(expectedSource, actualSource);
        } else if (expectedFile.isDirectory() && actualFile.isDirectory()) {
            List<Path> diff = difference(expectedSource, actualSource).collect(Collectors.toList());
            for (var pathA : diff) {
                var relativePath = expectedSource.relativize(pathA);
                var fileA = pathA.toFile();
                var pathB = actualSource.resolve(relativePath);
                var fileB = pathB.toFile();
                var c = (fileA.isDirectory() ? 1 : 0) + (fileB.isDirectory() ? 1 : 0);
                if (c == 2) continue;
                if (c == 1)
                    throw new AssertionFailedError(
                            String.format("Folder missmatch: %s != %s", pathA, pathB));
                assertFileContentEquals(pathA, pathB);
            }
        } else if (expectedFile.isFile()) {
            throw new AssertionFailedError(
                    "Expected path is a file but actual path is not " + actualSource);
        } else if (expectedFile.isDirectory()) {
            throw new AssertionFailedError(
                    "Expected path is a folder but actual path is not " + actualSource);
        } else {
            throw new AssertionFailedError(
                    String.format("Not equals: %s != %s", expectedSource, actualSource),
                    expectedSource,
                    actualSource);
        }
    }

    /**
     * Assert that similarity between two values is less than delta. Similarity computed as
     * abs(expected - actual).
     */
    public static void assertSimilar(double expected, double actual, double delta)
            throws AssertionError {
        var diff = Math.abs(expected - actual);
        if (diff > delta)
            throw new AssertionFailedError(
                    String.format(
                            "delta %s exceeds allowed %s (expected value <%s>, actual <%s>)",
                            diff, delta, expected, actual),
                    expected,
                    actual);
    }

    /**
     * Assert that similarity between each pair of values between two arrays is less than delta.
     * Similarity computed as abs(expected[i] - actual[i]).
     */
    public static void assertSimilar(double[] expected, double[] actual, double delta) {
        if (expected.length != actual.length)
            throw new AssertionFailedError(
                    String.format(
                            "Array length is different: expected <%s>, actual <%s>",
                            expected.length, actual.length),
                    expected.length,
                    actual.length);
        for (int i = 0; i < actual.length; i++) {
            var diff = Math.abs(expected[i] - actual[i]);
            if (diff > delta)
                throw new AssertionFailedError(
                        String.format(
                                "delta %s exceeds allowed %s for item %s (expected value <%s>,"
                                        + " actual <%s>)",
                                diff, delta, i, expected, actual),
                        expected,
                        actual);
        }
    }

    private static void assertFileContentEquals(Path expectedSource, Path actualSource)
            throws IOException {
        var expected = Files.readString(expectedSource);
        var actual = Files.readString(actualSource);
        if (!expected.equals(actual))
            throw new AssertionFailedError(
                    String.format("File content differs <%s>, <%s>", expectedSource, actualSource),
                    expected,
                    actual);
    }

    /**
     * Calculates difference between content of folder A and folder B.
     *
     * @param a folder A
     * @param b folder B
     * @return difference between A - B
     */
    private static Stream<Path> difference(Path a, Path b) throws IOException {
        return Files.list(a)
                .filter(
                        pathA -> {
                            var pathB = b.resolve(pathA.getFileName());
                            if (!pathB.toFile().exists()) return true;
                            if (pathA.toFile().isDirectory()) {
                                return !pathB.toFile().isDirectory();
                            }
                            try {
                                String fileA = Files.lines(pathA).collect(Collectors.joining("\n"));
                                String fileB = Files.lines(pathB).collect(Collectors.joining("\n"));
                                return !fileA.equals(fileB);
                            } catch (IOException e) {
                                e.printStackTrace();
                                return false;
                            }
                        });
    }
}

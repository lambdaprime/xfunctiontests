/*
 * Copyright 2019 lambdaprime
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

import static java.util.stream.Collectors.joining;

import id.xfunction.Preconditions;
import id.xfunction.ResourceUtils;
import id.xfunction.lang.XExec;
import id.xfunction.lang.XProcess;
import id.xfunction.text.WildcardMatcher;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Assert execution of an external command.
 *
 * @author lambdaprime intid@protonmail.com
 */
public class AssertRunCommand {

    private static final ResourceUtils RESOURCE_UTILS = new ResourceUtils();

    private XExec exec;
    private Optional<String> expectedOutput = Optional.empty();
    private Optional<Integer> expectedCode = Optional.empty();
    private Optional<Consumer<String>> consumer = Optional.empty();

    private boolean isWildcardMatching;
    private boolean isPrintCommandCall = true;

    public AssertRunCommand(String... cmd) {
        this.exec = new XExec(cmd);
    }

    /** Assert that command output (stdout and stderr) should be equal to expected output */
    public AssertRunCommand assertOutput(String expectedOutput) {
        this.expectedOutput = Optional.of(expectedOutput);
        return this;
    }

    /**
     * Assert that command output (stdout and stderr) should be equal to to expected output stored
     * inside the given resource file
     */
    public AssertRunCommand assertOutputFromResource(String absoluteResourcePath) {
        this.expectedOutput = Optional.of(RESOURCE_UTILS.readResource(absoluteResourcePath));
        return this;
    }

    /** Assert that command after execution should return give code */
    public AssertRunCommand assertReturnCode(int code) {
        this.expectedCode = Optional.of(code);
        return this;
    }

    /** Allow to consume the command output (stdout and stderr) */
    public AssertRunCommand withOutputConsumer(Consumer<String> out) {
        this.consumer = Optional.of(out);
        return this;
    }

    /** Send input for the command through its stdin */
    public AssertRunCommand withInput(Stream<String> input) {
        exec.withInput(input);
        return this;
    }

    /** Enables support of wildcards in the expected output */
    public AssertRunCommand withWildcardMatching() {
        isWildcardMatching = true;
        return this;
    }

    /** Adds following variables into environment */
    public AssertRunCommand withEnvironmentVariables(Map<String, String> vars) {
        exec.withEnvironmentVariables(vars);
        return this;
    }

    /**
     * By default the command call which will be executed is printed to the System.out. It is done
     * to help debug problems if any. This method disables such behavior.
     */
    public AssertRunCommand withPrintCommandCall(boolean isEnabled) {
        isPrintCommandCall = isEnabled;
        return this;
    }

    public void run() {
        run(proc -> {});
    }

    /** Run the command and perform the test */
    public void run(Consumer<XProcess> procConsumer) {
        XProcess proc = exec.start();
        if (isPrintCommandCall) {
            System.out.println(
                    "Executing command with following parameters: "
                            + Arrays.stream(exec.getCommand()).collect(joining(" ")));
        }
        proc.outputAsync(false);
        procConsumer.accept(proc);
        int actualCode = proc.await();
        var actualOutput =
                Stream.of(proc.stdout(), proc.stderr())
                        .filter(s -> s != null && !s.isBlank())
                        .collect(Collectors.joining("\n"));
        consumer.ifPresent(c -> c.accept(actualOutput));
        expectedCode.ifPresent(
                expectedCode -> {
                    Preconditions.equals(
                            expectedCode.intValue(), actualCode, "Unexpected return code");
                });
        expectedOutput.ifPresent(
                expectedOutput -> {
                    if (!isWildcardMatching) Preconditions.equals(expectedOutput, actualOutput);
                    else
                        Preconditions.isTrue(
                                new WildcardMatcher(expectedOutput).matches(actualOutput),
                                "Actual output <" + actualOutput + "> does not match expected");
                });
    }
}

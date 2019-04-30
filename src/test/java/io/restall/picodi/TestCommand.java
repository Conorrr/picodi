package io.restall.picodi;

import picocli.CommandLine;

@CommandLine.Command(name = "test-command")
public class TestCommand {

    private final TestClasses.A a;

    public TestCommand(TestClasses.A a) {
        this.a = a;
    }

    public TestClasses.A getA() {
        return a;
    }
}

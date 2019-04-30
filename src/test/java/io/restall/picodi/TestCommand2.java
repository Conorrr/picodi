package io.restall.picodi;

import picocli.CommandLine;

@CommandLine.Command(name = "test-command")
public class TestCommand2 {

    @CommandLine.ArgGroup(exclusive = true, multiplicity = "1")
    private Exclusive exclusive;

    public static class Exclusive {
        @CommandLine.Option(names = "-a", required = true) int x;
        @CommandLine.Option(names = "-b", required = true) int y;
        @CommandLine.Option(names = "-c", required = true) int z;

        private final TestClasses.A a;

        Exclusive(TestClasses.A a) {
            this.a = a;
        }
    }

    private final TestClasses.A a;

    public TestCommand2(TestClasses.A a) {
        this.a = a;
    }

    public TestClasses.A getA() {
        return a;
    }

    public Exclusive getExclusive() {
        return exclusive;
    }
}

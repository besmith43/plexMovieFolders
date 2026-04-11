package com.example.jpi;

public final class TestMain {
    private TestMain() {
    }

    public static void main(String[] args) throws Exception {
        PathBuilderTest.runAll();
        LibraryScannerTest.runAll();
        ImportExecutorTest.runAll();
        System.out.println("All tests passed.");
    }
}

package freshmart;

import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

/**
 * JUnit 5 entry point for the automated regression suite.
 *
 * <p>A {@link TestFactory} turns every case defined in {@link EUK00361749_Tests} into its own JUnit
 * test, named with the same ID as the report table, e.g. "TC75 [Boundary] Discount threshold exactly
 * LKR 10,000".
 * JUnit reports each case separately, so a failure points straight at the row in the table.
 */
@DisplayName("FreshMart regression suite")
class EUK00361749_JUnitTests {

    @TestFactory
    @DisplayName("Test-case table (hand-written cases)")
    Stream<DynamicTest> regressionCases() {
        return EUK00361749_Tests.allCases().stream()
                .map(testCase -> DynamicTest.dynamicTest(
                        testCase.id() + " [" + testCase.type() + "] " + testCase.scenario(),
                        () -> testCase.body().run()));
    }
}

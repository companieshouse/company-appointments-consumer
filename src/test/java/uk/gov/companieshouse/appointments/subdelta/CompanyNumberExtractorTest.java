package uk.gov.companieshouse.appointments.subdelta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class CompanyNumberExtractorTest {

    private final CompanyNumberExtractor extractor = new CompanyNumberExtractor();

    @Test
    @DisplayName("The extractor should get the correct company number back")
    void process() {
        // given
        // when
        String actual = extractor.extractFromUri(
                "company/OC305127/appointments/-0YatipCW4ZL295N9UVFo1TGyW8");

        // then
        assertEquals("OC305127", actual);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("extractorFixtures")
    void processPatternDoesNotMatch(String displayName, String uri, String expected) {
        // given

        // when
        Executable executable = () -> extractor.extractFromUri(uri);

        // then
        Exception exception = assertThrows(NonRetryableException.class, executable);
        assertEquals(expected, exception.getMessage());
    }

    private static Stream<Arguments> extractorFixtures() {
        return Stream.of(
                arguments(
                        "The extractor should throw a non retryable exception when it cannot extract a company number",
                        "company-appointments",
                        "Could not extract company number from resource URI: company-appointments"),
                arguments(
                        "The extractor should throw a non retryable exception when it cannot extract an empty company number",
                        "company//charges",
                        "Could not extract company number from resource URI: company//charges"),
                arguments(
                        "The extractor should throw a non retryable exception when it cannot extract a company number from an empty uri",
                        "",
                        "Could not extract company number from empty or null resource uri"),
                arguments(
                        "The extractor should throw a non retryable exception when it cannot extract a company number from a null uri",
                        null,
                        "Could not extract company number from empty or null resource uri"));
    }
}

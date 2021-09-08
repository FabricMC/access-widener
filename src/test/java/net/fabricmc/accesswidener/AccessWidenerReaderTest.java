/*
 * Copyright (c) 2020 FabricMC
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

package net.fabricmc.accesswidener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

public class AccessWidenerReaderTest {
	AccessWidener visitor = new AccessWidener();

	AccessWidenerReader reader = new AccessWidenerReader(visitor);

	@Nested
	class Header {
		@Test
		public void throwsOnInvalidFileHeader() {
			assertFormatError(
					"Invalid access widener file header. Expected: 'accessWidener <version> <namespace>'",
					() -> parse("accessWidenerX junk junk\nxxx")
			);
		}

		@Test
		public void throwsOnUnsupportedVersion() {
			assertFormatError(
					"Unsupported access widener format (v99)",
					() -> parse("accessWidener v99 junk\nxxx")
			);
		}

		@Test
		public void throwsOnUnsupportedNamespaceIfNamespaceSet() {
			AccessWidenerFormatException e = assertThrows(
					AccessWidenerFormatException.class,
					() -> reader.read(new BufferedReader(new StringReader("accessWidener v1 junk\nxxx")), "expectedNamespace")
			);
			assertThat(e).hasMessageContaining("Namespace (junk) does not match current runtime namespace (expectedNamespace)");
		}

		@Test
		public void acceptsMatchingNamespaceIfNamespaceSet() throws IOException {
			reader.read(new BufferedReader(new StringReader("accessWidener v1 expectedNamespace")), "expectedNamespace");
			assertEquals("expectedNamespace", visitor.getNamespace());
			assertEquals(Collections.emptySet(), visitor.classes);
		}

		@Test
		public void acceptsAnyNamespaceIfNoNamespaceSet() throws IOException {
			parse("accessWidener v1 anyWeirdNamespace");
			assertEquals("anyWeirdNamespace", visitor.getNamespace());
			assertEquals(Collections.emptySet(), visitor.classes);
		}
	}

	@Nested
	class Classes {
		@Test
		void testThrowsOnMissingTokensInLine() {
			assertFormatError(
					"Expected (<access> class <className>) got (accessible class)",
					() -> parseLines("accessible class")
			);
		}

		@Test
		void testThrowsOnExtraTokensInLine() {
			assertFormatError(
					"Expected (<access> class <className>) got (accessible class Class extra)",
					() -> parseLines("accessible class Class extra")
			);
		}

		@Test
		void testThrowsOnMutableClass() {
			assertFormatError(
					"java.lang.UnsupportedOperationException: Classes cannot be made mutable",
					() -> parseLines("mutable class Class")
			);
		}

		@Test
		public void testParseAccessible() throws IOException {
			testParseClassAccess(AccessWidener.ClassAccess.ACCESSIBLE, "accessible");
		}

		@Test
		public void testParseExtendable() throws IOException {
			testParseClassAccess(AccessWidener.ClassAccess.EXTENDABLE, "extendable");
		}

		@Test
		public void testParseAccessibleAndExtendable() throws IOException {
			// Test that they're merged the same way, independent of order
			testParseClassAccess(AccessWidener.ClassAccess.ACCESSIBLE_EXTENDABLE, "accessible", "extendable");
			testParseClassAccess(AccessWidener.ClassAccess.ACCESSIBLE_EXTENDABLE, "extendable", "accessible");
		}

		private void testParseClassAccess(AccessWidener.Access expectedAccess, String... keyword) throws IOException {
			String lines = Arrays.stream(keyword)
					.map(kw -> kw + " class some/test/Class")
					.collect(Collectors.joining("\n"));
			parseLines(lines);

			assertThat(visitor.classes).containsOnly("some.test.Class");
			assertThat(visitor.classAccess).containsOnly(entry("some/test/Class", expectedAccess));
			assertThat(visitor.fieldAccess).isEmpty();
			assertThat(visitor.methodAccess).isEmpty();
		}
	}

	@Nested
	class Fields {
		@Test
		void testThrowsOnMissingTokensInLine() {
			assertFormatError(
					"Expected (<access> field <className> <fieldName> <fieldDesc>) got (accessible field)",
					() -> parseLines("accessible field")
			);
			assertFormatError(
					"Expected (<access> field <className> <fieldName> <fieldDesc>) got (accessible field Class)",
					() -> parseLines("accessible field Class")
			);
			assertFormatError(
					"Expected (<access> field <className> <fieldName> <fieldDesc>) got (accessible field Class Field)",
					() -> parseLines("accessible field Class Field")
			);
		}

		@Test
		void testThrowsOnExtraTokensInLine() {
			assertFormatError(
					"Expected (<access> field <className> <fieldName> <fieldDesc>) got (accessible field Class field I extra)",
					() -> parseLines("accessible field Class field I extra")
			);
		}

		@Test
		void testThrowsOnExtendableField() {
			assertFormatError(
					"java.lang.UnsupportedOperationException: Fields cannot be made extendable",
					() -> parseLines("extendable field Class field I")
			);
		}

		@Test
		public void testParseAccessible() throws IOException {
			testParseFieldAccess(
					AccessWidener.ClassAccess.ACCESSIBLE,
					AccessWidener.FieldAccess.ACCESSIBLE,
					"accessible"
			);
		}

		@Test
		public void testParseMutable() throws IOException {
			testParseFieldAccess(
					null,
					AccessWidener.FieldAccess.MUTABLE,
					"mutable"
			);
		}

		@Test
		public void testParseAccessibleAndMutable() throws IOException {
			// Test that they're merged the same way, independent of order
			testParseFieldAccess(
					AccessWidener.ClassAccess.ACCESSIBLE,
					AccessWidener.FieldAccess.ACCESSIBLE_MUTABLE,
					"accessible", "mutable"
			);
			testParseFieldAccess(
					AccessWidener.ClassAccess.ACCESSIBLE,
					AccessWidener.FieldAccess.ACCESSIBLE_MUTABLE,
					"mutable", "accessible"
			);
		}

		private void testParseFieldAccess(
				AccessWidener.Access expectedClassAccess,
				AccessWidener.Access expectedFieldAccess,
				String... keyword
		) throws IOException {
			String lines = Arrays.stream(keyword)
					.map(kw -> kw + " field some/test/Class someField I")
					.collect(Collectors.joining("\n"));
			parseLines(lines);

			assertThat(visitor.classes).containsOnly("some.test.Class");

			if (expectedClassAccess != null) {
				assertThat(visitor.classAccess).containsOnly(
						entry("some/test/Class", expectedClassAccess)
				);
			} else {
				assertThat(visitor.classAccess).isEmpty();
			}

			assertThat(visitor.fieldAccess).containsOnly(
					entry(new EntryTriple("some/test/Class", "someField", "I"), expectedFieldAccess)
			);
			assertThat(visitor.methodAccess).isEmpty();
		}
	}

	@Nested
	class Methods {
		@Test
		void testThrowsOnMissingTokensInLine() {
			assertFormatError(
					"Expected (<access> method <className> <methodName> <methodDesc>) got (accessible method)",
					() -> parseLines("accessible method")
			);
			assertFormatError(
					"Expected (<access> method <className> <methodName> <methodDesc>) got (accessible method Method)",
					() -> parseLines("accessible method Method")
			);
			assertFormatError(
					"Expected (<access> method <className> <methodName> <methodDesc>) got (accessible method Class Method)",
					() -> parseLines("accessible method Class Method")
			);
		}

		@Test
		void testThrowsOnExtraTokensInLine() {
			assertFormatError(
					"Expected (<access> method <className> <methodName> <methodDesc>) got (accessible method Method method ()V extra)",
					() -> parseLines("accessible method Method method ()V extra")
			);
		}

		@Test
		void testThrowsOnMutableMethod() {
			assertFormatError(
					"java.lang.UnsupportedOperationException: Methods cannot be made mutable",
					() -> parseLines("mutable method Class method ()V")
			);
		}

		@Test
		public void testParseAccessible() throws IOException {
			testParseMethodAccess(
					AccessWidener.ClassAccess.ACCESSIBLE,
					AccessWidener.MethodAccess.ACCESSIBLE,
					"accessible"
			);
		}

		@Test
		public void testParseExtendable() throws IOException {
			// Note how the class is also made implicitly extendable
			testParseMethodAccess(
					AccessWidener.ClassAccess.EXTENDABLE,
					AccessWidener.MethodAccess.EXTENDABLE,
					"extendable"
			);
		}

		@Test
		public void testParseAccessibleAndExtendable() throws IOException {
			// Test that they're merged the same way, independent of order
			testParseMethodAccess(
					AccessWidener.ClassAccess.ACCESSIBLE_EXTENDABLE,
					AccessWidener.MethodAccess.ACCESSIBLE_EXTENDABLE,
					"accessible", "extendable"
			);
			testParseMethodAccess(
					AccessWidener.ClassAccess.ACCESSIBLE_EXTENDABLE,
					AccessWidener.MethodAccess.ACCESSIBLE_EXTENDABLE,
					"extendable", "accessible"
			);
		}

		private void testParseMethodAccess(
				AccessWidener.Access expectedClassAccess,
				AccessWidener.Access expectedMethodAccess,
				String... keyword
		) throws IOException {
			String lines = Arrays.stream(keyword)
					.map(kw -> kw + " method some/test/Class someMethod ()V")
					.collect(Collectors.joining("\n"));
			parseLines(lines);

			assertThat(visitor.classes).containsOnly("some.test.Class");

			if (expectedClassAccess != null) {
				assertThat(visitor.classAccess).containsOnly(
						entry("some/test/Class", expectedClassAccess)
				);
			} else {
				assertThat(visitor.classAccess).isEmpty();
			}

			assertThat(visitor.methodAccess).containsOnly(
					entry(new EntryTriple("some/test/Class", "someMethod", "()V"), expectedMethodAccess)
			);
			assertThat(visitor.fieldAccess).isEmpty();
		}
	}

	@Nested
	class GeneralParsing {
		@Test
		public void testCorrectLineNumbersInPresenceOfComments() {
			int lineNumber = assertThrows(AccessWidenerFormatException.class,
					() -> reader.read(new BufferedReader(new StringReader("accessWidener v1 namespace\n\n# comment\n\nERROR")))
			).getLineNumber();
			assertEquals(5, lineNumber);
		}

		@Test
		public void throwsOnUnknownAccessType() {
			assertFormatError(
					"Unknown access type: somecommand",
					() -> parseLines("somecommand")
			);
		}

		@Test
		public void throwsOnMissingTypeAfterAccessible() {
			assertFormatError(
					"Expected <class|field|method> following accessible",
					() -> parseLines("accessible")
			);
		}

		@Test
		public void throwsOnInvalidTypeAfterAccessible() {
			assertFormatError(
					"Unsupported type: 'blergh'",
					() -> parseLines("accessible blergh")
			);
		}

		@Test
		public void throwsWithLeadingWhitespace() {
			assertFormatError(
					"Leading whitespace is not allowed",
					() -> parseLines("   accessible class SomeClass")
			);
		}

		// This is a quirk in access-widener v1
		@Test
		public void testLeadingWhitespaceWithLineComment() throws IOException {
			parseLines("   accessible class SomeClass #linecomment");
			assertThat(visitor.classes).containsOnly("SomeClass");
		}

		@Test
		public void testTrailingWhitespace() throws IOException {
			parseLines("accessible class SomeClass    ");
			assertThat(visitor.classes).containsOnly("SomeClass");
		}

		@Test
		public void testCanParseWithTabSeparators() throws IOException {
			parseLines("accessible\tclass\tSomeName");
			assertThat(visitor.classes).containsOnly("SomeName");
		}

		@Test
		public void testCanParseWithMultipleSeparators() throws IOException {
			parseLines("accessible \tclass\t\t SomeName");
			assertThat(visitor.classes).containsOnly("SomeName");
		}

		@Test
		void allowsMalformedClassNameInNonStrictMode() throws IOException {
			parseLines("accessible class some.Class");
			assertThat(visitor.classes).containsOnly("some.Class");
		}
	}

	@Nested
	class StrictModeParsing {
		@Test
		void testClassName() {
			reader.setStrictMode(true);
			assertFormatError(
					"Class-names must be specified as a/b/C, not a.b.C, but found: some.Class",
					() -> parseLines("accessible class some.Class")
			);
		}

		@Test
		void testClassNameInMethodWidener() {
			reader.setStrictMode(true);
			assertFormatError(
					"Class-names must be specified as a/b/C, not a.b.C, but found: some.Class",
					() -> parseLines("accessible method some.Class method ()V")
			);
		}

		@Test
		void testClassNameInFieldWidener() {
			reader.setStrictMode(true);
			assertFormatError(
					"Class-names must be specified as a/b/C, not a.b.C, but found: some.Class",
					() -> parseLines("accessible field some.Class field I")
			);
		}
	}

	/**
	 * Tests parsing features introduced in the V2 format.
	 */
	@Nested
	class V2Parsing {
		@Test
		void transitiveKeywordIsIgnoredWhenNoFilterIsSet() throws Exception {
			String testInput = readTestInput("AccessWidenerReaderTest_transitive.txt");
			parse(testInput);

			assertWidenerContains("local");
			assertWidenerContains("transitive");
			assertThat(visitor.classAccess).hasSize(6);
			assertThat(visitor.methodAccess).hasSize(6);
			assertThat(visitor.fieldAccess).hasSize(4);
		}

		@Test
		void nonTransitiveEntriesAreIgnoredByNonTransitiveFilter() throws Exception {
			String testInput = readTestInput("AccessWidenerReaderTest_transitive.txt");
			reader = new AccessWidenerReader(new TransitiveOnlyFilter(visitor));
			parse(testInput);

			assertWidenerContains("transitive");
			assertThat(visitor.classAccess).hasSize(3);
			assertThat(visitor.methodAccess).hasSize(3);
			assertThat(visitor.fieldAccess).hasSize(2);
		}

		private void assertWidenerContains(String prefix) {
			assertThat(visitor.classAccess).contains(
					entry(prefix + "/AccessibleClass", AccessWidener.ClassAccess.ACCESSIBLE),
					entry(prefix + "/ExtendableClass", AccessWidener.ClassAccess.EXTENDABLE),
					entry(prefix + "/AccessibleExtendableClass", AccessWidener.ClassAccess.ACCESSIBLE_EXTENDABLE)
			);
			assertThat(visitor.methodAccess).contains(
					entry(new EntryTriple(prefix + "/AccessibleClass", "method", "()V"), AccessWidener.MethodAccess.ACCESSIBLE),
					entry(new EntryTriple(prefix + "/ExtendableClass", "method", "()V"), AccessWidener.MethodAccess.EXTENDABLE),
					entry(new EntryTriple(prefix + "/AccessibleExtendableClass", "method", "()V"), AccessWidener.MethodAccess.ACCESSIBLE_EXTENDABLE)
			);
			assertThat(visitor.fieldAccess).contains(
					entry(new EntryTriple(prefix + "/AccessibleClass", "finalField", "I"), AccessWidener.FieldAccess.MUTABLE),
					entry(new EntryTriple(prefix + "/AccessibleClass", "field", "I"), AccessWidener.FieldAccess.ACCESSIBLE)
			);
		}
	}

	private void parse(String content) throws IOException {
		reader.read(new BufferedReader(new StringReader(content)));
	}

	private void parseLines(String line) throws IOException {
		parse("accessWidener v1 namespace\n" + line);
	}

	private void assertFormatError(String expectedError, Executable executable) {
		AccessWidenerFormatException e = assertThrows(
				AccessWidenerFormatException.class,
				executable
		);
		assertEquals(expectedError, e.getMessage());
	}

	private String readTestInput(String name) throws Exception {
		URL resource = Objects.requireNonNull(getClass().getResource(name));
		return new String(Files.readAllBytes(
				Paths.get(resource.toURI())
		));
	}
}

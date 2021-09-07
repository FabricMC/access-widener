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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

import org.junit.jupiter.api.Test;

class AccessWidenerWriterTest {
	@Test
	void testWriteWidenerV1() throws Exception {
		String expectedContent = readReferenceContent("AccessWidenerWriterTest_v1.txt");

		AccessWidenerWriter writer = new AccessWidenerWriter();
		accept(writer, false);

		assertEquals(expectedContent, writer.writeString());
	}

	@Test
	void testWriteWidenerV2() throws Exception {
		String expectedContent = readReferenceContent("AccessWidenerWriterTest_v2.txt");

		AccessWidenerWriter writer = new AccessWidenerWriter();
		accept(writer, true);

		assertEquals(expectedContent, writer.writeString());
	}

	@Test
	void testCanMergeMultipleRunsIntoOneFile() {
		AccessWidenerWriter writer = new AccessWidenerWriter();
		writer.visitHeader("ns1");
		writer.visitClass("SomeClass", AccessWidenerReader.AccessType.ACCESSIBLE, false);
		writer.visitHeader("ns1");
		writer.visitClass("SomeClass", AccessWidenerReader.AccessType.EXTENDABLE, false);
		assertEquals("accessWidener\tv1\tns1\n"
				+ "accessible\tclass\tSomeClass\n"
				+ "extendable\tclass\tSomeClass\n", writer.writeString());
	}

	@Test
	void testDoesNotAllowDifferentNamespacesWhenMerging() {
		AccessWidenerWriter writer = new AccessWidenerWriter();
		writer.visitHeader("ns1");
		assertThrows(Exception.class, () -> writer.visitHeader("ns2"));
	}

	private String readReferenceContent(String name) throws IOException, URISyntaxException {
		URL resource = Objects.requireNonNull(getClass().getResource(name));
		String expectedContent = new String(Files.readAllBytes(
				Paths.get(resource.toURI())
		));
		return expectedContent.replace("\r\n", "\n"); // Normalize line endings
	}

	private void accept(AccessWidenerReader.Visitor visitor, boolean includeV2Content) {
		visitor.visitHeader("somenamespace");

		visitor.visitClass("pkg/AccessibleClass", AccessWidenerReader.AccessType.ACCESSIBLE, false);
		visitor.visitClass("pkg/ExtendableClass", AccessWidenerReader.AccessType.EXTENDABLE, false);
		visitor.visitClass("pkg/AccessibleExtendableClass", AccessWidenerReader.AccessType.ACCESSIBLE, false);
		visitor.visitClass("pkg/AccessibleExtendableClass", AccessWidenerReader.AccessType.EXTENDABLE, false);

		visitor.visitMethod("pkg/AccessibleClass", "method", "()V", AccessWidenerReader.AccessType.ACCESSIBLE, false);
		visitor.visitMethod("pkg/ExtendableClass", "method", "()V", AccessWidenerReader.AccessType.EXTENDABLE, false);
		visitor.visitMethod("pkg/AccessibleExtendableClass", "method", "()V", AccessWidenerReader.AccessType.ACCESSIBLE, false);
		visitor.visitMethod("pkg/AccessibleExtendableClass", "method", "()V", AccessWidenerReader.AccessType.EXTENDABLE, false);

		visitor.visitField("pkg/AccessibleClass", "field", "I", AccessWidenerReader.AccessType.ACCESSIBLE, false);
		visitor.visitField("pkg/AccessibleClass", "finalField", "I", AccessWidenerReader.AccessType.MUTABLE, false);

		if (includeV2Content) {
			visitor.visitClass("pkg/GlobalAccessibleClass", AccessWidenerReader.AccessType.ACCESSIBLE, true);
			visitor.visitMethod("pkg/GlobalAccessibleClass", "method", "()V", AccessWidenerReader.AccessType.ACCESSIBLE, true);
			visitor.visitField("pkg/GlobalAccessibleClass", "field", "I", AccessWidenerReader.AccessType.ACCESSIBLE, true);
			visitor.visitAddInterface("pkg/IfaceClass", "Interface", false);
			visitor.visitAddInterface("pkg/GlobalIfaceClass", "Interface", true);
		}
	}
}

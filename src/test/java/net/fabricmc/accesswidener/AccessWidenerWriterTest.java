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

import java.io.IOException;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AccessWidenerWriterTest {
	AccessWidener widener = new AccessWidener();

	@BeforeEach
	void setup() {
		widener.visitHeader("somenamespace");

		widener.visitClass("pkg/AccessibleClass", AccessWidenerReader.AccessType.ACCESSIBLE);
		widener.visitClass("pkg/ExtendableClass", AccessWidenerReader.AccessType.EXTENDABLE);
		widener.visitClass("pkg/AccessibleExtendableClass", AccessWidenerReader.AccessType.ACCESSIBLE);
		widener.visitClass("pkg/AccessibleExtendableClass", AccessWidenerReader.AccessType.EXTENDABLE);

		widener.visitMethod("pkg/AccessibleClass", "method", "()V", AccessWidenerReader.AccessType.ACCESSIBLE);
		widener.visitMethod("pkg/ExtendableClass", "method", "()V", AccessWidenerReader.AccessType.EXTENDABLE);
		widener.visitMethod("pkg/AccessibleExtendableClass", "method", "()V", AccessWidenerReader.AccessType.ACCESSIBLE);
		widener.visitMethod("pkg/AccessibleExtendableClass", "method", "()V", AccessWidenerReader.AccessType.EXTENDABLE);

		widener.visitField("pkg/AccessibleClass", "field", "I", AccessWidenerReader.AccessType.ACCESSIBLE);
		widener.visitField("pkg/AccessibleClass", "finalField", "I", AccessWidenerReader.AccessType.MUTABLE);
	}

	@Test
	void testWriteWidenerV1() throws Exception {
		String expectedContent = readReferenceContent("AccessWidenerWriterTest_v1.txt");

		StringWriter writer = new StringWriter();
		new AccessWidenerWriter(widener).write(writer);

		assertEquals(expectedContent, writer.toString());
	}

	private String readReferenceContent(String name) throws IOException, URISyntaxException {
		URL resource = Objects.requireNonNull(getClass().getResource(name));
		String expectedContent = new String(Files.readAllBytes(
				Paths.get(resource.toURI())
		));
		return expectedContent.replace("\r\n", "\n"); // Normalize line endings
	}
}

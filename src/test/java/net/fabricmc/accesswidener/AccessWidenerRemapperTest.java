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
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.commons.SimpleRemapper;

class AccessWidenerRemapperTest {
	SimpleRemapper remapper;

	@BeforeEach
	void setUp() {
		Map<String, String> mappings = new HashMap<>();
		mappings.put("a/Class", "newa/NewClass");
		mappings.put("g/Class", "newg/NewClass");
		mappings.put("x/Class", "newx/NewClass");
		mappings.put("a/Class.someMethod()I", "otherMethod");
		mappings.put("g/Class.someField", "otherField");
		mappings.put("z/Interface", "newz/Interface");
		remapper = new SimpleRemapper(mappings);
	}

	Remapper getRemapper(String from, String to) {
		assertEquals("original_namespace", from);
		return remapper;
	}

	@Test
	void testRemappingForDifferentNamespace() throws Exception {
		AccessWidenerWriter writer = new AccessWidenerWriter();
		accept(new AccessWidenerRemapper(writer, this::getRemapper, "different_namespace"));
		assertEquals(readReferenceContent("Remapped.txt"), writer.writeString());
	}

	@Test
	void testNoRemappingForSameNamespace() {
		AccessWidenerWriter remappedWriter = new AccessWidenerWriter();
		accept(new AccessWidenerRemapper(remappedWriter, this::getRemapper, "original_namespace"));

		// Write out the same stream without a remapper and check it's the same
		AccessWidenerWriter writer = new AccessWidenerWriter();
		accept(writer);

		assertEquals(writer.writeString(), remappedWriter.writeString());
	}

	void accept(AccessWidenerVisitor visitor) {
		visitor.visitHeader("original_namespace");
		visitor.visitClass("a/Class", AccessWidenerReader.AccessType.ACCESSIBLE, false);
		visitor.visitClass("x/Class", AccessWidenerReader.AccessType.EXTENDABLE, false);
		visitor.visitMethod("a/Class", "someMethod", "()I", AccessWidenerReader.AccessType.ACCESSIBLE, false);
		visitor.visitField("g/Class", "someField", "I", AccessWidenerReader.AccessType.MUTABLE, false);
	}

	private String readReferenceContent(String name) throws IOException, URISyntaxException {
		URL resource = Objects.requireNonNull(getClass().getResource(name));
		String expectedContent = new String(Files.readAllBytes(
				Paths.get(resource.toURI())
		));
		return expectedContent.replace("\r\n", "\n"); // Normalize line endings
	}
}

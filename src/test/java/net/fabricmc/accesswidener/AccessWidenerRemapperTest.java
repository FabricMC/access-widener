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
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.commons.SimpleRemapper;

class AccessWidenerRemapperTest {
	SimpleRemapper remapper;
	AccessWidener widener;

	@BeforeEach
	void setUp() {
		Map<String, String> mappings = new HashMap<>();
		mappings.put("a/Class", "newa/NewClass");
		mappings.put("g/Class", "newg/NewClass");
		mappings.put("x/Class", "newx/NewClass");
		mappings.put("a/Class.someMethod()I", "otherMethod");
		mappings.put("g/Class.someField", "otherField");
		remapper = new SimpleRemapper(mappings);

		widener = new AccessWidener();
		widener.visitHeader("original_namespace");
		widener.visitClass("a/Class", AccessWidenerReader.AccessType.ACCESSIBLE);
		widener.visitClass("x/Class", AccessWidenerReader.AccessType.EXTENDABLE);
		widener.visitMethod("a/Class", "someMethod", "()I", AccessWidenerReader.AccessType.ACCESSIBLE);
		widener.visitField("g/Class", "someField", "I", AccessWidenerReader.AccessType.MUTABLE);
	}

	@Nested
	class Remapping {
		AccessWidener remapped;

		@BeforeEach
		void remap() {
			remapped = new AccessWidenerRemapper(widener, remapper, "new_namespace").remap();
		}

		@Test
		void testReturnsNewWidener() {
			assertNotSame(widener, remapped);
		}

		@Test
		void testRemappingClassAccess() {
			assertThat(remapped.classAccess).containsOnly(
					entry("newa/NewClass", AccessWidener.ClassAccess.ACCESSIBLE),
					entry("newx/NewClass", AccessWidener.ClassAccess.EXTENDABLE)
			);
		}

		@Test
		void testRemappingMethodAccess() {
			assertThat(remapped.methodAccess).containsOnly(
					entry(new EntryTriple("newa/NewClass", "otherMethod", "()I"), AccessWidener.MethodAccess.ACCESSIBLE)
			);
		}

		@Test
		void testRemappingFieldAccess() {
			assertThat(remapped.fieldAccess).containsOnly(
					entry(new EntryTriple("newg/NewClass", "otherField", "I"), AccessWidener.FieldAccess.MUTABLE)
			);
		}

		@Test
		@Disabled("This functionality is currently not implemented")
		void testRemappingClassTargets() {
			assertThat(remapped.getTargets()).containsOnly(
					"newa/NewClass", "newx/NewClass"
			);
		}
	}

	@Test
	void noRemappingIfNamespaceMatches() {
		assertSame(widener, new AccessWidenerRemapper(widener, remapper, widener.getNamespace()).remap());
	}
}

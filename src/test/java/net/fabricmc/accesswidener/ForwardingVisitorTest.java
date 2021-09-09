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

import org.junit.jupiter.api.Test;

class ForwardingVisitorTest {
	AccessWidenerWriter writer1 = new AccessWidenerWriter();
	AccessWidenerWriter writer2 = new AccessWidenerWriter();
	ForwardingVisitor visitor = new ForwardingVisitor(writer1, writer2);

	@Test
	void visitHeader() {
		visitor.visitHeader(2, "special-namespace");
		assertEquals("accessWidener\tv2\tspecial-namespace\n", writer1.writeString());
		assertEquals(writer1.writeString(), writer2.writeString());
	}

	@Test
	void visitClass() {
		visitor.visitHeader(2, "special-namespace");
		visitor.visitClass("class-name", AccessWidenerReader.AccessType.ACCESSIBLE, true);
		assertEquals("accessWidener\tv2\tspecial-namespace\n"
				+ "transitive-accessible\tclass\tclass-name\n", writer1.writeString());
		assertEquals(writer1.writeString(), writer2.writeString());
	}

	@Test
	void visitMethod() {
		visitor.visitHeader(2, "special-namespace");
		visitor.visitMethod("class-name", "method-name", "method-desc", AccessWidenerReader.AccessType.ACCESSIBLE, true);
		assertEquals("accessWidener\tv2\tspecial-namespace\n"
				+ "transitive-accessible\tmethod\tclass-name\tmethod-name\tmethod-desc\n", writer1.writeString());
		assertEquals(writer1.writeString(), writer2.writeString());
	}

	@Test
	void visitField() {
		visitor.visitHeader(2, "special-namespace");
		visitor.visitField("field-name", "field-name", "field-desc", AccessWidenerReader.AccessType.ACCESSIBLE, true);
		assertEquals("accessWidener\tv2\tspecial-namespace\n"
				+ "transitive-accessible\tfield\tfield-name\tfield-name\tfield-desc\n", writer1.writeString());
		assertEquals(writer1.writeString(), writer2.writeString());
	}
}

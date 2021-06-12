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

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Locale;

public final class AccessWidenerReader {
	private final Visitor visitor;

	public AccessWidenerReader(AccessWidener accessWidener) {
		this.visitor = accessWidener;
	}

	public AccessWidenerReader(Visitor visitor) {
		this.visitor = visitor;
	}

	public void read(BufferedReader reader) throws IOException {
		this.read(reader, null);
	}

	public void read(BufferedReader reader, String currentNamespace) throws IOException {
		String[] header = reader.readLine().split("\\s+");

		if (header.length != 3 || !header[0].equals("accessWidener")) {
			throw new UnsupportedOperationException("Invalid access access widener file");
		}

		if (!header[1].equals("v1")) {
			throw new RuntimeException(String.format("Unsupported access widener format (%s)", header[1]));
		}

		if (currentNamespace != null && !header[2].equals(currentNamespace)) {
			throw new RuntimeException(String.format("Namespace (%s) does not match current runtime namespace (%s)", header[2], currentNamespace));
		}

		visitor.visitHeader(header[2]);

		String line;

		while ((line = reader.readLine()) != null) {
			//Comment handling
			int commentPos = line.indexOf('#');

			if (commentPos >= 0) {
				line = line.substring(0, commentPos).trim();
			}

			if (line.isEmpty()) continue;

			String[] split = line.split("\\s+");

			if (split.length != 3 && split.length != 5) {
				throw new RuntimeException(String.format("Invalid line (%s)", line));
			}

			AccessType access = readAccessType(split[0]);

			switch (split[1]) {
			case "class":
				if (split.length != 3) {
					throw new RuntimeException(String.format("Expected (<access>\tclass\t<className>) got (%s)", line));
				}

				visitor.visitClass(split[2], access);
				break;
			case "field":
				if (split.length != 5) {
					throw new RuntimeException(String.format("Expected (<access>\tfield\t<className>\t<fieldName>\t<fieldDesc>) got (%s)", line));
				}

				visitor.visitField(split[2], split[3], split[4], access);
				break;
			case "method":
				if (split.length != 5) {
					throw new RuntimeException(String.format("Expected (<access>\tmethod\t<className>\t<methodName>\t<methodDesc>) got (%s)", line));
				}

				visitor.visitMethod(split[2], split[3], split[4], access);
				break;
			default:
				throw new UnsupportedOperationException("Unsupported type " + split[1]);
			}
		}
	}

	private static AccessType readAccessType(String access) {
		switch (access.toLowerCase(Locale.ROOT)) {
		case "accessible":
			return AccessType.ACCESSIBLE;
		case "extendable":
			return AccessType.EXTENDABLE;
		case "mutable":
			return AccessType.MUTABLE;
		default:
			throw new IllegalArgumentException("Unknown access type: " + access);
		}
	}

	public enum AccessType {
		ACCESSIBLE("accessible"),
		EXTENDABLE("extendable"),
		MUTABLE("mutable");

		private final String id;

		AccessType(String id) {
			this.id = id;
		}

		@Override
		public String toString() {
			return id;
		}
	}

	public interface Visitor {
		/**
		 * Visits the header data.
		 *
		 * @param namespace the access widener's mapping namespace
		 */
		default void visitHeader(String namespace) {
		}

		/**
		 * Visits a widened class.
		 *
		 * @param name   the name of the class
		 * @param access the access type ({@link AccessType#ACCESSIBLE} or {@link AccessType#EXTENDABLE})
		 */
		default void visitClass(String name, AccessType access) {
		}

		/**
		 * Visits a widened method.
		 *
		 * @param owner      the name of the containing class
		 * @param name       the name of the method
		 * @param descriptor the method descriptor
		 * @param access     the access type ({@link AccessType#ACCESSIBLE} or {@link AccessType#EXTENDABLE})
		 */
		default void visitMethod(String owner, String name, String descriptor, AccessType access) {
		}

		/**
		 * Visits a widened field.
		 *
		 * @param owner      the name of the containing class
		 * @param name       the name of the field
		 * @param descriptor the type of the field as a type descriptor
		 * @param access     the access type ({@link AccessType#ACCESSIBLE} or {@link AccessType#MUTABLE})
		 */
		default void visitField(String owner, String name, String descriptor, AccessType access) {
		}
	}
}

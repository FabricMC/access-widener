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

	private int lineNumber;

	public AccessWidenerReader(Visitor visitor) {
		this.visitor = visitor;
	}

	public void read(BufferedReader reader) throws IOException {
		this.read(reader, null);
	}

	public void read(BufferedReader reader, String currentNamespace) throws IOException {
		String[] header = reader.readLine().split("\\s+");
		lineNumber = 1;

		if (header.length != 3 || !header[0].equals("accessWidener")) {
			throw error("Invalid access widener file header. Expected: 'accessWidener <version> <namespace>'");
		}

		if (!header[1].equals("v1")) {
			throw error("Unsupported access widener format (%s)", header[1]);
		}

		if (currentNamespace != null && !header[2].equals(currentNamespace)) {
			throw error("Namespace (%s) does not match current runtime namespace (%s)", header[2], currentNamespace);
		}

		visitor.visitHeader(header[2]);

		String line;

		while ((line = reader.readLine()) != null) {
			lineNumber++;

			//Comment handling
			int commentPos = line.indexOf('#');

			if (commentPos >= 0) {
				line = line.substring(0, commentPos).trim();
			}

			if (line.isEmpty()) continue;

			if (Character.isWhitespace(line.codePointAt(0))) {
				throw error("Leading whitespace is not allowed");
			}

			String[] split = line.split("\\s+");

			AccessType access = readAccessType(split[0]);

			if (split.length < 2) {
				throw error("Expected <class|field|method> following " + split[0]);
			}

			switch (split[1]) {
			case "class":
				if (split.length != 3) {
					throw error("Expected (<access> class <className>) got (%s)", line);
				}

				try {
					visitor.visitClass(split[2], access);
				} catch (Exception e) {
					throw error(e.toString());
				}

				break;
			case "field":
				if (split.length != 5) {
					throw error("Expected (<access> field <className> <fieldName> <fieldDesc>) got (%s)", line);
				}

				try {
					visitor.visitField(split[2], split[3], split[4], access);
				} catch (Exception e) {
					throw error(e.toString());
				}

				break;
			case "method":
				if (split.length != 5) {
					throw error("Expected (<access> method <className> <methodName> <methodDesc>) got (%s)", line);
				}

				try {
					visitor.visitMethod(split[2], split[3], split[4], access);
				} catch (Exception e) {
					throw error(e.toString());
				}

				break;
			default:
				throw error("Unsupported type " + split[1]);
			}
		}
	}

	private AccessType readAccessType(String access) {
		switch (access.toLowerCase(Locale.ROOT)) {
		case "accessible":
			return AccessType.ACCESSIBLE;
		case "extendable":
			return AccessType.EXTENDABLE;
		case "mutable":
			return AccessType.MUTABLE;
		default:
			throw error("Unknown access type: " + access);
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

	private AccessWidenerFormatException error(String format, Object... args) {
		// Note that getLineNumber is actually 1 line after the current line position,
		// because it is 0-based. But since our reporting here is 1-based, it works out.
		// If this class ever starts reading lines incrementally however, it'd need to be changed.
		String message = String.format(Locale.ROOT, format, args);
		return new AccessWidenerFormatException(lineNumber, message);
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

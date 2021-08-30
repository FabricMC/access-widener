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
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class AccessWidenerReader {
	// Also includes some weirdness such as vertical tabs
	private static final Pattern V1_DELIMITER = Pattern.compile("\\s+");
	// Only spaces or tabs
	private static final Pattern V2_DELIMITER = Pattern.compile("[ \\t]+");

	// Access widener format versions
	private static final int V1 = 1;
	private static final int V2 = 1;

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

		int version = parseVersion(header[1]);

		if (currentNamespace != null && !header[2].equals(currentNamespace)) {
			throw error("Namespace (%s) does not match current runtime namespace (%s)", header[2], currentNamespace);
		}

		visitor.visitHeader(header[2]);

		String line;

		Pattern delimiter = version < V2 ? V1_DELIMITER : V2_DELIMITER;

		while ((line = reader.readLine()) != null) {
			lineNumber++;

			line = handleComment(version, line);

			if (line.isEmpty()) {
				continue;
			}

			if (Character.isWhitespace(line.codePointAt(0))) {
				throw error("Leading whitespace is not allowed");
			}

			// Note that this trims trailing spaces. See the docs of split for details.
			List<String> tokens = Arrays.asList(delimiter.split(line));

			boolean global = false;

			if (version >= V2) {
				// Global access widener flag
				if (!tokens.isEmpty() && tokens.get(0).equals("global")) {
					tokens = tokens.subList(1, tokens.size());
					global = true;
				}

				// Interface injection
				if (!tokens.isEmpty() && tokens.get(0).equals("add-interface")) {
					handleInterfaceInjection(tokens, global);
					continue;
				}
			}

			if (tokens.isEmpty()) {
				throw error("Expected <accessible|extendable|mutable>");
			}

			AccessType access = readAccessType(tokens.get(0));

			if (tokens.size() < 2) {
				throw error("Expected <class|field|method> following " + tokens.get(0));
			}

			switch (tokens.get(1)) {
			case "class":
				if (tokens.size() != 3) {
					throw error("Expected (<access> class <className>) got (%s)", line);
				}

				try {
					visitor.visitClass(tokens.get(2), access, global);
				} catch (Exception e) {
					throw error(e.toString());
				}

				break;
			case "field":
				if (tokens.size() != 5) {
					throw error("Expected (<access> field <className> <fieldName> <fieldDesc>) got (%s)", line);
				}

				try {
					visitor.visitField(tokens.get(2), tokens.get(3), tokens.get(4), access, global);
				} catch (Exception e) {
					throw error(e.toString());
				}

				break;
			case "method":
				if (tokens.size() != 5) {
					throw error("Expected (<access> method <className> <methodName> <methodDesc>) got (%s)", line);
				}

				try {
					visitor.visitMethod(tokens.get(2), tokens.get(3), tokens.get(4), access, global);
				} catch (Exception e) {
					throw error(e.toString());
				}

				break;
			default:
				throw error("Unsupported type: '" + tokens.get(1) + "'");
			}
		}
	}

	private void handleInterfaceInjection(List<String> tokens, boolean global) {
		if (tokens.size() < 2) {
			throw error("Expected class name following add-interface keyword");
		}

		String className = tokens.get(1);

		if (tokens.size() < 3) {
			throw error("Expected interface name following class-name");
		}

		String interfaceName = tokens.get(2);

		if (tokens.size() > 3) {
			throw error("Expected no extra text following interface-name");
		}

		visitor.visitAddInterface(className, interfaceName, global);
	}

	private String handleComment(int version, String line) {
		//Comment handling
		int commentPos = line.indexOf('#');

		if (commentPos >= 0) {
			line = line.substring(0, commentPos);

			// In V1, trimming led to leading whitespace being tolerated
			// The tailing whitespace is already stripped by the split below
			if (version <= V1) {
				line = line.trim();
			}
		}

		return line;
	}

	private int parseVersion(String versionString) {
		switch (versionString) {
		case "v1":
			return V1;
		case "v2":
			return V2;
		default:
			throw error("Unsupported access widener format (%s)", versionString);
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
		 * @param global whether this widener should be applied across mod boundaries
		 */
		default void visitClass(String name, AccessType access, boolean global) {
		}

		/**
		 * Visits a widened method.
		 *
		 * @param owner      the name of the containing class
		 * @param name       the name of the method
		 * @param descriptor the method descriptor
		 * @param access     the access type ({@link AccessType#ACCESSIBLE} or {@link AccessType#EXTENDABLE})
		 * @param global     whether this widener should be applied across mod boundaries
		 */
		default void visitMethod(String owner, String name, String descriptor, AccessType access, boolean global) {
		}

		/**
		 * Visits a widened field.
		 *
		 * @param owner      the name of the containing class
		 * @param name       the name of the field
		 * @param descriptor the type of the field as a type descriptor
		 * @param access     the access type ({@link AccessType#ACCESSIBLE} or {@link AccessType#MUTABLE})
		 * @param global     whether this widener should be applied across mod boundaries
		 */
		default void visitField(String owner, String name, String descriptor, AccessType access, boolean global) {
		}

		/**
		 * Visits an injected interface.
		 *
		 * @param name   the name of the class to add an interface to
		 * @param iface  the name of the interface to add
		 * @param global whether this widener should be applied across mod boundaries
		 */
		default void visitAddInterface(String name, String iface, boolean global) {
		}
	}
}

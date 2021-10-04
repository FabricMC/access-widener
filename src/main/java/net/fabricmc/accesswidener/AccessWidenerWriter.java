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

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.io.Writer;

public final class AccessWidenerWriter implements AccessWidenerVisitor {
	private final Writer writer;
	private int version = -1;
	private String namespace;

	/**
	 * Constructs a writer that writes an access widener in the given version.
	 * If features not supported by the version are used, an exception is thrown.
	 */
	public AccessWidenerWriter(int version) {
		this.version = version;
		this.writer = new StringWriter();
	}

	/**
	 * Constructs a writer that writes using the latest version.
	 */
	public AccessWidenerWriter() {
		this(2); // Latest version
	}

	/**
	 * Constructs a writer that writes to a writer instead of a string/byte array in the given version.
	 * @param writer
	 */
	public AccessWidenerWriter(Writer writer, int version) {
		this.writer = writer;
		this.version = version;
	}

	/**
	 * Constructs a writer that writes to a writer instead of a string/byte array.
	 * Uses the version from visitVersion.
	 * @param writer
	 */
	public AccessWidenerWriter(Writer writer) {
		this.writer = writer;
	}

	@Override
	public void visitVersion(int version) {
		if (this.version == -1) this.version = version;
	}

	@Override
	public void visitHeader(String namespace) {
		if (version < 0 || version > 2) throw new IllegalArgumentException("Invalid version " + version);

		if (this.namespace == null) {
			try {
				writer.append("accessWidener\tv")
						.append(String.valueOf(version))
						.append('\t')
						.append(namespace)
						.append('\n');
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		} else if (!this.namespace.equals(namespace)) {
			throw new IllegalArgumentException("Cannot write different namespaces to the same file ("
					+ this.namespace + " != " + namespace + ")");
		}

		this.namespace = namespace;
	}

	@Override
	public void visitClass(String name, AccessWidenerReader.AccessType access, boolean transitive) {
		writeAccess(access, transitive);

		try {
			writer.append("\tclass\t").append(name).append('\n');
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public void visitMethod(String owner, String name, String descriptor, AccessWidenerReader.AccessType access, boolean transitive) {
		writeAccess(access, transitive);

		try {
			writer.append("\tmethod\t").append(owner).append('\t').append(name)
					.append('\t').append(descriptor).append('\n');
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public void visitField(String owner, String name, String descriptor, AccessWidenerReader.AccessType access, boolean transitive) {
		writeAccess(access, transitive);

		try {
			writer.append("\tfield\t").append(owner).append('\t').append(name)
					.append('\t').append(descriptor).append('\n');
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public byte[] write() {
		return writeString().getBytes(AccessWidenerReader.ENCODING);
	}

	public String writeString() {
		if (namespace == null) {
			throw new IllegalStateException("No namespace set. visitHeader wasn't called.");
		}

		if (!(writer instanceof StringWriter)) {
			throw new IllegalStateException("Writer constructor used");
		}

		return writer.toString();
	}

	private void writeAccess(AccessWidenerReader.AccessType access, boolean transitive) {
		try {
			if (transitive) {
				if (version < 2) {
					throw new IllegalStateException("Cannot write transitive rule in version " + version);
				}

				writer.append("transitive-");
			}

			writer.append(access.toString());
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}

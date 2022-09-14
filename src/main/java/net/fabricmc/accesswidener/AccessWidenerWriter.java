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

import java.util.Set;

public final class AccessWidenerWriter implements AccessWidenerVisitor {
	private final StringBuilder builder = new StringBuilder();
	private final int version;
	private String namespace;

	/**
	 * Constructs a writer that writes an access widener in the given version.
	 * If features not supported by the version are used, an exception is thrown.
	 */
	public AccessWidenerWriter(int version) {
		this.version = version;
	}

	/**
	 * Constructs a writer that writes using the latest version.
	 */
	public AccessWidenerWriter() {
		this(2); // Latest version
	}

	@Override
	public void visitHeader(String namespace) {
		if (this.namespace == null) {
			builder.append("accessWidener\tv")
					.append(version)
					.append('\t')
					.append(namespace)
					.append('\n');
		} else if (!this.namespace.equals(namespace)) {
			throw new IllegalArgumentException("Cannot write different namespaces to the same file ("
					+ this.namespace + " != " + namespace + ")");
		}

		this.namespace = namespace;
	}

	@Override
	public void visitClass(String name, Set<AccessWidenerReader.AccessType> access, boolean transitive) {
		writeAccess(access, transitive);
		builder.append("\tclass\t").append(name).append('\n');
	}

	@Override
	public void visitMethod(String owner, String name, String descriptor, Set<AccessWidenerReader.AccessType> access, boolean transitive) {
		writeAccess(access, transitive);
		builder.append("\tmethod\t").append(owner).append('\t').append(name)
				.append('\t').append(descriptor).append('\n');
	}

	@Override
	public void visitField(String owner, String name, String descriptor, Set<AccessWidenerReader.AccessType> access, boolean transitive) {
		writeAccess(access, transitive);
		builder.append("\tfield\t").append(owner).append('\t').append(name)
				.append('\t').append(descriptor).append('\n');
	}

	public byte[] write() {
		return writeString().getBytes(AccessWidenerReader.ENCODING);
	}

	public String writeString() {
		if (namespace == null) {
			throw new IllegalStateException("No namespace set. visitHeader wasn't called.");
		}

		return builder.toString();
	}

	private void writeAccess(Set<AccessWidenerReader.AccessType> access, boolean transitive) {
		if (transitive) {
			if (version < 2) {
				throw new IllegalStateException("Cannot write transitive rule in version " + version);
			}

			builder.append("transitive-");
		}

		builder.append(access);
	}
}

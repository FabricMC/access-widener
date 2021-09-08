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

import java.util.ArrayList;
import java.util.List;

public final class AccessWidenerWriter implements AccessWidenerReader.Visitor {
	private String namespace;

	private final List<ClassAccessor> classAccessors = new ArrayList<>();
	private final List<MethodAccessor> methodAccessors = new ArrayList<>();
	private final List<FieldAccessor> fieldAccessors = new ArrayList<>();

	@Override
	public void visitHeader(String namespace) {
		if (this.namespace != null && !this.namespace.equals(namespace)) {
			throw new IllegalArgumentException("Cannot write two different namespaces to the same file ("
					+ this.namespace + " != " + namespace + ")");
		}

		this.namespace = namespace;
	}

	@Override
	public void visitClass(String name, AccessWidenerReader.AccessType access, boolean transitive) {
		classAccessors.add(new ClassAccessor(name, access, transitive));
	}

	@Override
	public void visitMethod(String owner, String name, String descriptor, AccessWidenerReader.AccessType access, boolean transitive) {
		methodAccessors.add(new MethodAccessor(owner, name, descriptor, access, transitive));
	}

	@Override
	public void visitField(String owner, String name, String descriptor, AccessWidenerReader.AccessType access, boolean transitive) {
		fieldAccessors.add(new FieldAccessor(owner, name, descriptor, access, transitive));
	}

	public byte[] write() {
		return writeString().getBytes(AccessWidenerReader.ENCODING);
	}

	public String writeString() {
		int version = determineVersion();

		StringBuilder builder = new StringBuilder();
		builder.append("accessWidener\tv")
				.append(version)
				.append('\t')
				.append(namespace != null ? namespace : "unknown")
				.append("\n");

		for (ClassAccessor classAccessor : classAccessors) {
			classAccessor.write(builder, version);
		}

		for (MethodAccessor methodAccessor : methodAccessors) {
			methodAccessor.write(builder, version);
		}

		for (FieldAccessor fieldAccessor : fieldAccessors) {
			fieldAccessor.write(builder, version);
		}

		return builder.toString();
	}

	/**
	 * Checks which version has to be used based on the features that were used.
	 */
	private int determineVersion() {
		boolean hasTransitive = classAccessors.stream().anyMatch(c -> c.transitive)
				|| methodAccessors.stream().anyMatch(m -> m.transitive)
				|| fieldAccessors.stream().anyMatch(f -> f.transitive);

		if (hasTransitive) {
			return 2;
		} else {
			return 1;
		}
	}

	private static class ClassAccessor {
		final String name;
		final AccessWidenerReader.AccessType access;
		final boolean transitive;

		ClassAccessor(String name, AccessWidenerReader.AccessType access, boolean transitive) {
			this.name = name;
			this.access = access;
			this.transitive = transitive;
		}

		void write(StringBuilder builder, int version) {
			if (version >= 2 && transitive) {
				builder.append("transitive-");
			}

			builder.append(access).append("\tclass\t").append(name).append('\n');
		}
	}

	private static class MethodAccessor {
		final String owner;
		final String name;
		final String descriptor;
		final AccessWidenerReader.AccessType access;
		final boolean transitive;

		MethodAccessor(String owner, String name, String descriptor, AccessWidenerReader.AccessType access, boolean transitive) {
			this.owner = owner;
			this.name = name;
			this.descriptor = descriptor;
			this.access = access;
			this.transitive = transitive;
		}

		void write(StringBuilder builder, int version) {
			if (version >= 2 && transitive) {
				builder.append("transitive-");
			}

			builder.append(access).append("\tmethod\t").append(owner).append('\t').append(name)
					.append('\t').append(descriptor).append('\n');
		}
	}

	private static class FieldAccessor {
		final String owner;
		final String name;
		final String descriptor;
		final AccessWidenerReader.AccessType access;
		final boolean transitive;

		FieldAccessor(String owner, String name, String descriptor, AccessWidenerReader.AccessType access, boolean transitive) {
			this.owner = owner;
			this.name = name;
			this.descriptor = descriptor;
			this.access = access;
			this.transitive = transitive;
		}

		void write(StringBuilder builder, int version) {
			if (version >= 2 && transitive) {
				builder.append("transitive-");
			}

			builder.append(access).append("\tfield\t").append(owner).append('\t').append(name)
					.append('\t').append(descriptor).append('\n');
		}
	}
}

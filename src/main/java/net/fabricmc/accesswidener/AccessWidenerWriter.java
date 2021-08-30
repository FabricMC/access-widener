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
	private final List<AddedInterface> addedInterfaces = new ArrayList<>();

	@Override
	public void visitHeader(String namespace) {
		if (this.namespace != null && !this.namespace.equals(namespace)) {
			throw new IllegalArgumentException("Cannot write two different namespaces to the same file ("
					+ this.namespace + " != " + namespace + ")");
		}

		this.namespace = namespace;
	}

	@Override
	public void visitClass(String name, AccessWidenerReader.AccessType access, boolean global) {
		classAccessors.add(new ClassAccessor(name, access, global));
	}

	@Override
	public void visitMethod(String owner, String name, String descriptor, AccessWidenerReader.AccessType access, boolean global) {
		methodAccessors.add(new MethodAccessor(owner, name, descriptor, access, global));
	}

	@Override
	public void visitField(String owner, String name, String descriptor, AccessWidenerReader.AccessType access, boolean global) {
		fieldAccessors.add(new FieldAccessor(owner, name, descriptor, access, global));
	}

	@Override
	public void visitAddInterface(String name, String iface, boolean global) {
		addedInterfaces.add(new AddedInterface(name, iface, global));
	}

	public String write() {
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

		for (AddedInterface addedInterface : addedInterfaces) {
			addedInterface.write(builder, version);
		}

		return builder.toString();
	}

	/**
	 * Checks which version has to be used based on the features that were used.
	 */
	private int determineVersion() {
		boolean hasGlobal = classAccessors.stream().anyMatch(c -> c.global)
				|| methodAccessors.stream().anyMatch(m -> m.global)
				|| fieldAccessors.stream().anyMatch(f -> f.global)
				|| addedInterfaces.stream().anyMatch(i -> i.global);
		boolean hasAddedInterfaces = !addedInterfaces.isEmpty();

		if (hasGlobal || hasAddedInterfaces) {
			return 2;
		} else {
			return 1;
		}
	}

	private static class ClassAccessor {
		final String name;
		final AccessWidenerReader.AccessType access;
		final boolean global;

		ClassAccessor(String name, AccessWidenerReader.AccessType access, boolean global) {
			this.name = name;
			this.access = access;
			this.global = global;
		}

		void write(StringBuilder builder, int version) {
			if (version >= 2 && global) {
				builder.append("global\t");
			}

			builder.append(access).append("\tclass\t").append(name).append('\n');
		}
	}

	private static class MethodAccessor {
		final String owner;
		final String name;
		final String descriptor;
		final AccessWidenerReader.AccessType access;
		final boolean global;

		MethodAccessor(String owner, String name, String descriptor, AccessWidenerReader.AccessType access, boolean global) {
			this.owner = owner;
			this.name = name;
			this.descriptor = descriptor;
			this.access = access;
			this.global = global;
		}

		void write(StringBuilder builder, int version) {
			if (version >= 2 && global) {
				builder.append("global\t");
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
		final boolean global;

		FieldAccessor(String owner, String name, String descriptor, AccessWidenerReader.AccessType access, boolean global) {
			this.owner = owner;
			this.name = name;
			this.descriptor = descriptor;
			this.access = access;
			this.global = global;
		}

		void write(StringBuilder builder, int version) {
			if (version >= 2 && global) {
				builder.append("global\t");
			}

			builder.append(access).append("\tfield\t").append(owner).append('\t').append(name)
					.append('\t').append(descriptor).append('\n');
		}
	}

	private static class AddedInterface {
		final String name;
		final String iface;
		final boolean global;

		AddedInterface(String name, String iface, boolean global) {
			this.name = name;
			this.iface = iface;
			this.global = global;
		}

		void write(StringBuilder builder, int version) {
			if (version >= 2 && global) {
				builder.append("global\t");
			}

			builder.append("add-interface\t").append(name).append('\t').append(iface).append('\n');
		}
	}
}

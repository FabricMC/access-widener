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

public final class AccessWidenerWriter implements AccessWidenerVisitor {
	private String namespace;

	private int version;

	private final List<Rule> rules = new ArrayList<>();

	@Override
	public void visitHeader(int version, String namespace) {
		if (this.namespace != null && !this.namespace.equals(namespace)) {
			throw new IllegalArgumentException("Cannot write different namespaces to the same file ("
					+ this.namespace + " != " + namespace + ")");
		}

		this.namespace = namespace;
		// Supports merging different AWs by using the highest version
		this.version = Math.max(this.version, version);
	}

	@Override
	public void visitClass(String name, AccessWidenerReader.AccessType access, boolean transitive) {
		rules.add(new ClassRule(name, access, transitive));
	}

	@Override
	public void visitMethod(String owner, String name, String descriptor, AccessWidenerReader.AccessType access, boolean transitive) {
		rules.add(new MethodRule(owner, name, descriptor, access, transitive));
	}

	@Override
	public void visitField(String owner, String name, String descriptor, AccessWidenerReader.AccessType access, boolean transitive) {
		rules.add(new FieldRule(owner, name, descriptor, access, transitive));
	}

	public byte[] write() {
		return writeString().getBytes(AccessWidenerReader.ENCODING);
	}

	public String writeString() {
		if (namespace == null) {
			throw new IllegalStateException("No namespace set. visitHeader wasn't called.");
		}

		StringBuilder builder = new StringBuilder();
		builder.append("accessWidener\tv")
				.append(version)
				.append('\t')
				.append(namespace)
				.append('\n');

		for (Rule rule : rules) {
			rule.write(builder, version);
		}

		return builder.toString();
	}

	private abstract static class Rule {
		final boolean transitive;
		final AccessWidenerReader.AccessType access;

		Rule(boolean transitive, AccessWidenerReader.AccessType access) {
			this.transitive = transitive;
			this.access = access;
		}

		void write(StringBuilder builder, int version) {
			if (transitive) {
				if (version < 2) {
					throw new IllegalStateException("Cannot write transitive rule in version " + version);
				}

				builder.append("transitive-");
			}

			builder.append(access);
		}
	}

	private static class ClassRule extends Rule {
		final String name;

		ClassRule(String name, AccessWidenerReader.AccessType access, boolean transitive) {
			super(transitive, access);
			this.name = name;
		}

		@Override
		void write(StringBuilder builder, int version) {
			super.write(builder, version);
			builder.append("\tclass\t").append(name).append('\n');
		}
	}

	private static class MethodRule extends Rule {
		final String owner;
		final String name;
		final String descriptor;

		MethodRule(String owner, String name, String descriptor, AccessWidenerReader.AccessType access, boolean transitive) {
			super(transitive, access);
			this.owner = owner;
			this.name = name;
			this.descriptor = descriptor;
		}

		@Override
		void write(StringBuilder builder, int version) {
			super.write(builder, version);
			builder.append("\tmethod\t").append(owner).append('\t').append(name)
					.append('\t').append(descriptor).append('\n');
		}
	}

	private static class FieldRule extends Rule {
		final String owner;
		final String name;
		final String descriptor;

		FieldRule(String owner, String name, String descriptor, AccessWidenerReader.AccessType access, boolean transitive) {
			super(transitive, access);
			this.owner = owner;
			this.name = name;
			this.descriptor = descriptor;
		}

		@Override
		void write(StringBuilder builder, int version) {
			super.write(builder, version);
			builder.append("\tfield\t").append(owner).append('\t').append(name)
					.append('\t').append(descriptor).append('\n');
		}
	}
}

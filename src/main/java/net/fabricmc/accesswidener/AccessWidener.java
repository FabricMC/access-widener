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

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.Opcodes;

public final class AccessWidener implements AccessWidenerReader.Visitor {
	String namespace;
	// Contains the actual transforms. Class names are as class-file internal binary names (forward slash is used
	// instead of period as the package separator).
	final Map<String, Access> classAccess = new HashMap<>();
	final Map<EntryTriple, Access> methodAccess = new HashMap<>();
	final Map<EntryTriple, Access> fieldAccess = new HashMap<>();
	// Contains the class-names that are affected by loaded wideners.
	// Names are period-separated binary names (i.e. a.b.C).
	final Set<String> classes = new LinkedHashSet<>();

	@Override
	public void visitHeader(String namespace) {
		if (this.namespace != null && !this.namespace.equals(namespace)) {
			throw new RuntimeException(String.format("Namespace mismatch, expected %s got %s", this.namespace, namespace));
		}

		this.namespace = namespace;
	}

	@Override
	public void visitClass(String name, AccessWidenerReader.AccessType access, boolean transitive) {
		classAccess.put(name, applyAccess(access, classAccess.getOrDefault(name, ClassAccess.DEFAULT), null));
		addTargets(name);
	}

	@Override
	public void visitMethod(String owner, String name, String descriptor, AccessWidenerReader.AccessType access, boolean transitive) {
		addOrMerge(methodAccess, new EntryTriple(owner, name, descriptor), access, MethodAccess.DEFAULT);
		addTargets(owner);
	}

	@Override
	public void visitField(String owner, String name, String descriptor, AccessWidenerReader.AccessType access, boolean transitive) {
		addOrMerge(fieldAccess, new EntryTriple(owner, name, descriptor), access, FieldAccess.DEFAULT);
		addTargets(owner);
	}

	private void addTargets(String clazz) {
		clazz = clazz.replace('/', '.');
		classes.add(clazz);

		//Also transform all parent classes
		while (clazz.contains("$")) {
			clazz = clazz.substring(0, clazz.lastIndexOf("$"));
			classes.add(clazz);
		}
	}

	void addOrMerge(Map<EntryTriple, Access> map, EntryTriple entry, AccessWidenerReader.AccessType access, Access defaultAccess) {
		if (entry == null || access == null) {
			throw new RuntimeException("Input entry or access is null");
		}

		map.put(entry, applyAccess(access, map.getOrDefault(entry, defaultAccess), entry));
	}

	Access applyAccess(AccessWidenerReader.AccessType input, Access access, EntryTriple entryTriple) {
		switch (input) {
		case ACCESSIBLE:
			makeClassAccessible(entryTriple);
			return access.makeAccessible();
		case EXTENDABLE:
			makeClassExtendable(entryTriple);
			return access.makeExtendable();
		case MUTABLE:
			return access.makeMutable();
		default:
			throw new UnsupportedOperationException("Unknown access type:" + input);
		}
	}

	private void makeClassAccessible(EntryTriple entryTriple) {
		if (entryTriple == null) return;
		classAccess.put(entryTriple.getOwner(), applyAccess(AccessWidenerReader.AccessType.ACCESSIBLE, classAccess.getOrDefault(entryTriple.getOwner(), ClassAccess.DEFAULT), null));
	}

	private void makeClassExtendable(EntryTriple entryTriple) {
		if (entryTriple == null) return;
		classAccess.put(entryTriple.getOwner(), applyAccess(AccessWidenerReader.AccessType.EXTENDABLE, classAccess.getOrDefault(entryTriple.getOwner(), ClassAccess.DEFAULT), null));
	}

	Access getClassAccess(String className) {
		return classAccess.getOrDefault(className, ClassAccess.DEFAULT);
	}

	Access getFieldAccess(EntryTriple entryTriple) {
		return fieldAccess.getOrDefault(entryTriple, FieldAccess.DEFAULT);
	}

	Access getMethodAccess(EntryTriple entryTriple) {
		return methodAccess.getOrDefault(entryTriple, MethodAccess.DEFAULT);
	}

	public Set<String> getTargets() {
		return classes;
	}

	public String getNamespace() {
		return namespace;
	}

	private static int makePublic(int i) {
		return (i & ~(Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED)) | Opcodes.ACC_PUBLIC;
	}

	private static int makeProtected(int i) {
		if ((i & Opcodes.ACC_PUBLIC) != 0) {
			//Return i if public
			return i;
		}

		return (i & ~(Opcodes.ACC_PRIVATE)) | Opcodes.ACC_PROTECTED;
	}

	private static int makeFinalIfPrivate(int access, String name, int ownerAccess) {
		// Dont make constructors final
		if (name.equals("<init>")) {
			return access;
		}

		// Skip interface and static methods
		if ((ownerAccess & Opcodes.ACC_INTERFACE) != 0 || (access & Opcodes.ACC_STATIC) != 0) {
			return access;
		}

		if ((access & Opcodes.ACC_PRIVATE) != 0) {
			return access | Opcodes.ACC_FINAL;
		}

		return access;
	}

	private static int removeFinal(int i) {
		return i & ~Opcodes.ACC_FINAL;
	}

	interface Access extends AccessOperator {
		Access makeAccessible();

		Access makeExtendable();

		Access makeMutable();
	}

	enum ClassAccess implements Access {
		DEFAULT((access, name, ownerAccess) -> access),
		ACCESSIBLE((access, name, ownerAccess) -> makePublic(access)),
		EXTENDABLE((access, name, ownerAccess) -> makePublic(removeFinal(access))),
		ACCESSIBLE_EXTENDABLE((access, name, ownerAccess) -> makePublic(removeFinal(access)));

		private final AccessOperator operator;

		ClassAccess(AccessOperator operator) {
			this.operator = operator;
		}

		@Override
		public Access makeAccessible() {
			if (this == EXTENDABLE || this == ACCESSIBLE_EXTENDABLE) {
				return ACCESSIBLE_EXTENDABLE;
			}

			return ACCESSIBLE;
		}

		@Override
		public Access makeExtendable() {
			if (this == ACCESSIBLE || this == ACCESSIBLE_EXTENDABLE) {
				return ACCESSIBLE_EXTENDABLE;
			}

			return EXTENDABLE;
		}

		@Override
		public Access makeMutable() {
			throw new UnsupportedOperationException("Classes cannot be made mutable");
		}

		@Override
		public int apply(int access, String targetName, int ownerAccess) {
			return operator.apply(access, targetName, ownerAccess);
		}
	}

	enum MethodAccess implements Access {
		DEFAULT((access, name, ownerAccess) -> access),
		ACCESSIBLE((access, name, ownerAccess) -> makePublic(makeFinalIfPrivate(access, name, ownerAccess))),
		EXTENDABLE((access, name, ownerAccess) -> makeProtected(removeFinal(access))),
		ACCESSIBLE_EXTENDABLE((access, name, owner) -> makePublic(removeFinal(access)));

		private final AccessOperator operator;

		MethodAccess(AccessOperator operator) {
			this.operator = operator;
		}

		@Override
		public Access makeAccessible() {
			if (this == EXTENDABLE || this == ACCESSIBLE_EXTENDABLE) {
				return ACCESSIBLE_EXTENDABLE;
			}

			return ACCESSIBLE;
		}

		@Override
		public Access makeExtendable() {
			if (this == ACCESSIBLE || this == ACCESSIBLE_EXTENDABLE) {
				return ACCESSIBLE_EXTENDABLE;
			}

			return EXTENDABLE;
		}

		@Override
		public Access makeMutable() {
			throw new UnsupportedOperationException("Methods cannot be made mutable");
		}

		@Override
		public int apply(int access, String targetName, int ownerAccess) {
			return operator.apply(access, targetName, ownerAccess);
		}
	}

	enum FieldAccess implements Access {
		DEFAULT((access, name, ownerAccess) -> access),
		ACCESSIBLE((access, name, ownerAccess) -> makePublic(access)),
		MUTABLE((access, name, ownerAccess) -> removeFinal(access)),
		ACCESSIBLE_MUTABLE((access, name, ownerAccess) -> makePublic(removeFinal(access)));

		private final AccessOperator operator;

		FieldAccess(AccessOperator operator) {
			this.operator = operator;
		}

		@Override
		public Access makeAccessible() {
			if (this == MUTABLE || this == ACCESSIBLE_MUTABLE) {
				return ACCESSIBLE_MUTABLE;
			}

			return ACCESSIBLE;
		}

		@Override
		public Access makeExtendable() {
			throw new UnsupportedOperationException("Fields cannot be made extendable");
		}

		@Override
		public Access makeMutable() {
			if (this == ACCESSIBLE || this == ACCESSIBLE_MUTABLE) {
				return ACCESSIBLE_MUTABLE;
			}

			return MUTABLE;
		}

		@Override
		public int apply(int access, String targetName, int ownerAccess) {
			return operator.apply(access, targetName, ownerAccess);
		}
	}

	@FunctionalInterface
	interface AccessOperator {
		int apply(int access, String targetName, int ownerAccess);
	}
}

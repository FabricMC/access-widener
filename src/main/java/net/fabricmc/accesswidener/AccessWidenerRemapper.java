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

import org.objectweb.asm.commons.Remapper;

/**
 * Decorates a {@link AccessWidenerVisitor} with a {@link Remapper}
 * to remap names passing through the visitor if they come from a different namespace.
 */
public final class AccessWidenerRemapper implements AccessWidenerVisitor {
	private final Provider remapperProvider;
	private final String targetNamespace;
	private final AccessWidenerVisitor delegate;
	// is null while no header was read or when no remapping should occur
	private Remapper remapper;

	/**
	 * @param delegate        The visitor to forward the remapped information to.
	 * @param remapper        Will be used to remap names found in the access widener.
	 * @param sourceNamespace The expected namespace of the access widener being remapped. Remapping fails if the
	 *                        actual namespace is different.
	 * @param targetNamespace The namespace that the access widener will be remapped to.
	 */
	public AccessWidenerRemapper(
			AccessWidenerVisitor delegate,
			Remapper remapper,
			String sourceNamespace,
			String targetNamespace
	) {
		this.delegate = delegate;
		this.targetNamespace = targetNamespace;
		this.remapperProvider = (from, to) -> {
			if (!sourceNamespace.equals(from)) {
				throw new IllegalArgumentException("Cannot remap access widener from namespace '" + from + "'."
						+ " Expected " + sourceNamespace);
			}

			return remapper;
		};
	}

	/**
	 * @param delegate         The visitor to forward the remapped information to.
	 * @param remapperProvider Will be used to acquire a remapper based on the namespace found in the access widener.
	 *                         Can return null to indicate no remapping should occur for this namespace.
	 * @param targetNamespace  The namespace that the access widener will be remapped to.
	 */
	public AccessWidenerRemapper(
			AccessWidenerVisitor delegate,
			Provider remapperProvider,
			String targetNamespace
	) {
		this.delegate = delegate;
		this.targetNamespace = targetNamespace;
		this.remapperProvider = remapperProvider;
	}

	@Override
	public void visitHeader(String namespace) {
		if (!namespace.equals(targetNamespace)) {
			remapper = remapperProvider.getRemapper(namespace, targetNamespace);
		} else {
			remapper = null;
		}

		delegate.visitHeader(targetNamespace);
	}

	@Override
	public void visitClass(String name, AccessWidenerReader.AccessType access, boolean transitive) {
		if (remapper != null) {
			name = remapper.map(name);
		}

		delegate.visitClass(name, access, transitive);
	}

	@Override
	public void visitMethod(String owner, String name, String descriptor, AccessWidenerReader.AccessType access, boolean transitive) {
		if (remapper != null) {
			name = remapper.mapMethodName(owner, name, descriptor);
			descriptor = remapper.mapDesc(descriptor);
			owner = remapper.map(owner);
		}

		delegate.visitMethod(owner, name, descriptor, access, transitive);
	}

	@Override
	public void visitField(String owner, String name, String descriptor, AccessWidenerReader.AccessType access, boolean transitive) {
		if (remapper != null) {
			name = remapper.mapFieldName(owner, name, descriptor);
			descriptor = remapper.mapDesc(descriptor);
			owner = remapper.map(owner);
		}

		delegate.visitField(owner, name, descriptor, access, transitive);
	}

	@FunctionalInterface
	public interface Provider {
		Remapper getRemapper(String from, String to);
	}
}

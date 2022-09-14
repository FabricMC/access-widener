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

import java.util.Set;

/**
 * Decorates a {@link AccessWidenerVisitor} with a {@link Remapper}
 * to remap names passing through the visitor if they come from a different namespace.
 */
public final class AccessWidenerRemapper implements AccessWidenerVisitor {
	private final AccessWidenerVisitor delegate;
	private final String fromNamespace;
	private final String toNamespace;
	private final Remapper remapper;

	/**
	 * @param delegate      The visitor to forward the remapped information to.
	 * @param remapper      Will be used to remap names found in the access widener.
	 * @param fromNamespace The expected namespace of the access widener being remapped. Remapping fails if the
	 *                      actual namespace is different.
	 * @param toNamespace   The namespace that the access widener will be remapped to.
	 */
	public AccessWidenerRemapper(
			AccessWidenerVisitor delegate,
			Remapper remapper,
			String fromNamespace,
			String toNamespace
	) {
		this.delegate = delegate;
		this.fromNamespace = fromNamespace;
		this.toNamespace = toNamespace;
		this.remapper = remapper;
	}

	@Override
	public void visitHeader(String namespace) {
		if (!this.fromNamespace.equals(namespace)) {
			throw new IllegalArgumentException("Cannot remap access widener from namespace '" + namespace + "'."
					+ " Expected: '" + this.fromNamespace + "'");
		}

		delegate.visitHeader(toNamespace);
	}

	@Override
	public void visitClass(String name, Set<AccessWidenerReader.AccessType> access, boolean transitive) {
		delegate.visitClass(remapper.map(name), access, transitive);
	}

	@Override
	public void visitMethod(String owner, String name, String descriptor, Set<AccessWidenerReader.AccessType> access, boolean transitive) {
		delegate.visitMethod(
				remapper.map(owner),
				remapper.mapMethodName(owner, name, descriptor),
				remapper.mapDesc(descriptor),
				access,
				transitive
		);
	}

	@Override
	public void visitField(String owner, String name, String descriptor, Set<AccessWidenerReader.AccessType> access, boolean transitive) {
		delegate.visitField(
				remapper.map(owner),
				remapper.mapFieldName(owner, name, descriptor),
				remapper.mapDesc(descriptor),
				access,
				transitive
		);
	}
}

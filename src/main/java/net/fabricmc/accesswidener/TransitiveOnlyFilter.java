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

/**
 * Decorates a visitor to only receive elements that are marked as transitive.
 */
public final class TransitiveOnlyFilter implements AccessWidenerReader.Visitor {
	private final AccessWidenerReader.Visitor delegate;

	public TransitiveOnlyFilter(AccessWidenerReader.Visitor delegate) {
		this.delegate = delegate;
	}

	@Override
	public void visitHeader(String namespace) {
		delegate.visitHeader(namespace);
	}

	@Override
	public void visitClass(String name, AccessWidenerReader.AccessType access, boolean transitive) {
		if (transitive) {
			delegate.visitClass(name, access, transitive);
		}
	}

	@Override
	public void visitMethod(String owner, String name, String descriptor, AccessWidenerReader.AccessType access, boolean transitive) {
		if (transitive) {
			delegate.visitMethod(owner, name, descriptor, access, transitive);
		}
	}

	@Override
	public void visitField(String owner, String name, String descriptor, AccessWidenerReader.AccessType access, boolean transitive) {
		if (transitive) {
			delegate.visitField(owner, name, descriptor, access, transitive);
		}
	}
}

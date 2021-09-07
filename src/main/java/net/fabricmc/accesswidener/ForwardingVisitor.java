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
 * Forwards visitor events to multiple other visitors.
 */
public class ForwardingVisitor implements AccessWidenerReader.Visitor {
	private final AccessWidenerReader.Visitor[] visitors;

	public ForwardingVisitor(AccessWidenerReader.Visitor... visitors) {
		this.visitors = visitors.clone();
	}

	@Override
	public void visitHeader(String namespace) {
		for (AccessWidenerReader.Visitor visitor : visitors) {
			visitor.visitHeader(namespace);
		}
	}

	@Override
	public void visitClass(String name, AccessWidenerReader.AccessType access, boolean global) {
		for (AccessWidenerReader.Visitor visitor : visitors) {
			visitor.visitClass(name, access, global);
		}
	}

	@Override
	public void visitMethod(String owner, String name, String descriptor, AccessWidenerReader.AccessType access, boolean global) {
		for (AccessWidenerReader.Visitor visitor : visitors) {
			visitor.visitMethod(owner, name, descriptor, access, global);
		}
	}

	@Override
	public void visitField(String owner, String name, String descriptor, AccessWidenerReader.AccessType access, boolean global) {
		for (AccessWidenerReader.Visitor visitor : visitors) {
			visitor.visitField(owner, name, descriptor, access, global);
		}
	}

	@Override
	public void visitAddInterface(String name, String iface, boolean global) {
		for (AccessWidenerReader.Visitor visitor : visitors) {
			visitor.visitAddInterface(name, iface, global);
		}
	}
}

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

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.RecordComponentVisitor;

/**
 * Applies rules from an {@link AccessWidener} by transforming Java classes using an ASM {@link ClassVisitor}.
 */
public final class AccessWidenerClassVisitor extends ClassVisitor {
	private final AccessWidener accessWidener;
	private final boolean strictRecords;
	private String className;
	private int classAccess;

	AccessWidenerClassVisitor(int api, ClassVisitor classVisitor, AccessWidener accessWidener) {
		this(api, classVisitor, accessWidener, true);
	}

	AccessWidenerClassVisitor(int api, ClassVisitor classVisitor, AccessWidener accessWidener, boolean strictRecords) {
		super(api, classVisitor);
		this.accessWidener = accessWidener;
		this.strictRecords = strictRecords;
	}

	public static ClassVisitor createClassVisitor(int api, ClassVisitor visitor, AccessWidener accessWidener) {
		return new AccessWidenerClassVisitor(api, visitor, accessWidener);
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		className = name;
		classAccess = access;

		AccessWidener.Access classAccess = accessWidener.getClassAccess(name);

		if (strictRecords && "java/lang/Record".equals(superName) && AccessWidener.ClassAccess.isExtendable(classAccess)) {
			throw new UnsupportedOperationException(String.format("Cannot modify record (%s) access to be extendable", name));
		}

		super.visit(
				version,
				classAccess.apply(access, name, this.classAccess),
				name,
				signature,
				superName,
				interfaces
		);
	}

	@Override
	public void visitPermittedSubclass(String permittedSubclass) {
		if (AccessWidener.ClassAccess.isExtendable(accessWidener.getClassAccess(className))) {
			return;
		}

		super.visitPermittedSubclass(permittedSubclass);
	}

	@Override
	public RecordComponentVisitor visitRecordComponent(String name, String descriptor, String signature) {
		if (strictRecords) {
			AccessWidener.Access fieldAccess = accessWidener.getFieldAccess(new EntryTriple(className, name, descriptor));

			if (AccessWidener.FieldAccess.isMutable(fieldAccess)) {
				throw new UnsupportedOperationException(String.format("Cannot modify record (%s) field (%s) access to be mutable", className, name + descriptor));
			}
		}

		return super.visitRecordComponent(name, descriptor, signature);
	}

	@Override
	public void visitInnerClass(String name, String outerName, String innerName, int access) {
		super.visitInnerClass(
				name,
				outerName,
				innerName,
				accessWidener.getClassAccess(name).apply(access, name, classAccess)
		);
	}

	@Override
	public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
		return super.visitField(
				accessWidener.getFieldAccess(new EntryTriple(className, name, descriptor)).apply(access, name, classAccess),
				name,
				descriptor,
				signature,
				value
		);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		return new AccessWidenerMethodVisitor(super.visitMethod(
				accessWidener.getMethodAccess(new EntryTriple(className, name, descriptor)).apply(access, name, classAccess),
				name,
				descriptor,
				signature,
				exceptions
		));
	}

	private class AccessWidenerMethodVisitor extends MethodVisitor {
		AccessWidenerMethodVisitor(MethodVisitor methodVisitor) {
			super(AccessWidenerClassVisitor.this.api, methodVisitor);
		}

		@Override
		public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
			if (opcode == Opcodes.INVOKESPECIAL && owner.equals(className) && !name.equals("<init>")) {
				AccessWidener.Access methodAccess = accessWidener.getMethodAccess(new EntryTriple(owner, name, descriptor));

				if (methodAccess != AccessWidener.MethodAccess.DEFAULT) {
					opcode = Opcodes.INVOKEVIRTUAL;
				}
			}

			super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
		}
	}
}

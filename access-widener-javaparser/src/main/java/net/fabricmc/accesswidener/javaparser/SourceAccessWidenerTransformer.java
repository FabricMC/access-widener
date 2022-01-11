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

package net.fabricmc.accesswidener.javaparser;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.nodeTypes.NodeWithName;
import com.github.javaparser.resolution.UnsolvedSymbolException;

import net.fabricmc.accesswidener.AccessWidener;
import net.fabricmc.accesswidener.EntryTriple;

public class SourceAccessWidenerTransformer {
	protected final AccessWidener widener;
	protected final PrintStream warn;

	public SourceAccessWidenerTransformer(AccessWidener widener, PrintStream warn) {
		this.widener = widener;
		this.warn = warn;

		if(!widener.isSourceCompatible()) {
			throw new UnsupportedOperationException("AccessWidener must have source compatible entries!");
		}
	}

	public SourceAccessWidenerTransformer(AccessWidener widener) {
		this(widener, System.err);
	}

	/**
	 * A heuristic on whether the java file may be transformed.
	 */
	public boolean mayTransform(String javaFilePath) {
		String pkg = AccessWidener.getPackage(javaFilePath);
		return this.widener.getPackages().contains(pkg);
	}

	public boolean transform(CompilationUnit unit) {
		boolean transformed = false;

		for(TypeDeclaration<?> type : unit.findAll(TypeDeclaration.class)) {
			String qualifiedName = getFullyQualifiedName(type).orElse(null);

			if(qualifiedName == null) {
				System.err.println("Unable to find internal name of local class in " + unit
						.getPrimaryType()
						.flatMap(SourceAccessWidenerTransformer::getFullyQualifiedName) + ".java");
				return false;
			}

			if(this.widener.getTargets().contains(qualifiedName)) {
				return false;
			}

			boolean isInterface = type.isClassOrInterfaceDeclaration() && type
					.asClassOrInterfaceDeclaration()
					.isInterface() || type.isAnnotationDeclaration();

			String internalName = qualifiedName.replace('.', '/');
			NodeList<Modifier> classModifiers = type.getModifiers();
			int classFlags = SourceUtil.toClassFlags(classModifiers, type);
			int newFlags = this.widener.getClassAccess(internalName).apply(classFlags, internalName, classFlags);
			SourceUtil.fromFlags(classModifiers, newFlags, false);

			// todo add unsealing support when javaparser supports it

			UnsolvedSymbolException warn = null;
			for(MethodDeclaration declaration : type.getMethods()) {
				try {
					String desc = declaration.toDescriptor();
					EntryTriple triple = EntryTriple.create(internalName, declaration.getNameAsString(), desc, true);
					this.apply(declaration.getModifiers(), triple, AccessWidener::getMethodAccess, classFlags, isInterface);
				} catch(UnsolvedSymbolException ignore) {
					warn = ignore;
				}
			}

			for(ConstructorDeclaration constructor : type.getConstructors()) {
				try {
					String desc = toDescriptor(constructor);
					EntryTriple triple = EntryTriple.create(internalName, "<init>", desc, true);
					this.apply(constructor.getModifiers(), triple, AccessWidener::getMethodAccess, classFlags, isInterface);
				} catch(UnsolvedSymbolException ignore) {
					warn = ignore;
				}
			}

			List<FieldDeclaration> fields = type.getFields();

			for(int i = fields.size() - 1; i >= 0; i--) {
				FieldDeclaration field = fields.get(i);
				Map<AccessWidener.Access, FieldDeclaration> in = new HashMap<>();

				for(VariableDeclarator variable : field.getVariables()) {
					try {
						String desc = variable.getType().toDescriptor();
						EntryTriple triple = EntryTriple.create(internalName, variable.getNameAsString(), desc, true);
						AccessWidener.Access access = this.widener.getFieldAccess(triple);
						in.computeIfAbsent(access, $ -> {
							FieldDeclaration declaration = field.clone();
							this.apply(declaration.getModifiers(), triple, AccessWidener::getFieldAccess, classFlags, isInterface);
							return declaration;
						}).getVariables().add(variable);
					} catch(UnsolvedSymbolException ignore) {
						warn = ignore;
					}
				}

				if(in.size() == 1) {
					FieldDeclaration declaration = in.values().iterator().next();
					field.setModifiers(declaration.getModifiers());
				} else {
					type.getMembers().remove(field);
					type.getMembers().addAll(in.values());
				}
			}

			if(warn != null) {
				this.warn.println("[AccessWidener] May cause incorrectly applied AWs: Unable to resolve identifiers in " + qualifiedName + " due to: ");
				warn.printStackTrace(this.warn);
			}

			transformed = true;
		}

		return transformed;
	}

	static String toDescriptor(ConstructorDeclaration declaration) {
		StringBuilder sb = new StringBuilder();
		sb.append("(");

		for(int i = 0; i < declaration.getParameters().size(); i++) {
			sb.append(declaration.getParameter(i).getType().toDescriptor());
		}

		sb.append(")V");
		return sb.toString();
	}

	static Optional<String> getFullyQualifiedName(TypeDeclaration<?> declaration) {
		String name = declaration.getNameAsString();

		if(declaration.isTopLevelType()) {
			return declaration
					.findCompilationUnit()
					.map(cu -> cu.getPackageDeclaration().map(NodeWithName::getNameAsString).map(pkg -> pkg + "." + name).orElse(name));
		}

		return declaration
				.findAncestor(TypeDeclaration.class)
				.map(td -> (TypeDeclaration<?>) td)
				.flatMap(SourceAccessWidenerTransformer::getFullyQualifiedName)
				.map(fqn -> fqn + "$" + name);
	}

	void apply(
			NodeList<Modifier> modifiers,
			EntryTriple triple,
			BiFunction<AccessWidener, EntryTriple, AccessWidener.Access> access,
			int classFlags,
			boolean isInterfaceMethod
	) {
		int flags = SourceUtil.toFlags(modifiers, isInterfaceMethod);
		int newFlags = access.apply(this.widener, triple).apply(flags, triple.getName(), classFlags);
		SourceUtil.fromFlags(modifiers, newFlags, isInterfaceMethod);
	}
}

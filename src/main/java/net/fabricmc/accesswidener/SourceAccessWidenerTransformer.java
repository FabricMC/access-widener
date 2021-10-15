package net.fabricmc.accesswidener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.nodeTypes.NodeWithName;

public class SourceAccessWidenerTransformer {
	protected final AccessWidener widener;

	public SourceAccessWidenerTransformer(AccessWidener widener) {
		this.widener = widener;
		if(!widener.requiresSourceCompatibility) {
			throw new UnsupportedOperationException("AccessWidener must have source compatible entries!");
		}
	}

	/**
	 * A heuristic on whether the java file may be transformed
	 */
	public boolean mayTransform(String javaFilePath) {
		String pkg = AccessWidener.getPackage(javaFilePath);
		return this.widener.javaPackages.contains(pkg);
	}

	public boolean transform(CompilationUnit unit) {
		for(TypeDeclaration<?> type : unit.getTypes()) {
			String internalName = getFullyQualifiedName(type).map(s -> s.replace('.', '/')).orElse(null);
			if(internalName == null) {
				System.err.println("Unable to find internal name of local class in " + unit.getPrimaryType()
						.flatMap(SourceAccessWidenerTransformer::getFullyQualifiedName) + ".java");
			}

			if(this.widener.getTargets().contains(internalName)) {
				this.widener.getClassAccess(internalName).apply(type.getModifiers());

				for(MethodDeclaration declaration : type.getMethods()) {
					String desc = declaration.toDescriptor();
					EntryTriple triple = EntryTriple.create(internalName, declaration.getNameAsString(), desc, true);
					this.widener.getMethodAccess(triple).apply(declaration.getModifiers());
				}

				for(ConstructorDeclaration constructor : type.getConstructors()) {
					String desc = toDescriptor(constructor);
					EntryTriple triple = EntryTriple.create(internalName, "<init>", desc, true);
					this.widener.getMethodAccess(triple).apply(constructor.getModifiers());
				}

				List<FieldDeclaration> fields = type.getFields();
				for(int i = fields.size() - 1; i >= 0; i--) {
					FieldDeclaration field = fields.get(i);
					Map<AccessWidener.Access, FieldDeclaration> in = new HashMap<>();
					for(VariableDeclarator variable : field.getVariables()) {
						String desc = variable.getType().toDescriptor();
						EntryTriple triple = EntryTriple.create(internalName, variable.getNameAsString(), desc, true);
						AccessWidener.Access access = this.widener.getFieldAccess(triple);
						in.computeIfAbsent(access, $ -> {
							FieldDeclaration declaration = field.clone();
							access.apply(declaration.getModifiers());
							return declaration;
						}).getVariables().add(variable);
					}
					if(in.size() == 1) {
						FieldDeclaration declaration = in.values().iterator().next();
						field.setModifiers(declaration.getModifiers());
					} else {
						field.remove(field);
						fields.addAll(in.values());
					}
				}
				return true;
			}
		}

		return false;
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
			return declaration.findCompilationUnit()
					.map(cu -> cu.getPackageDeclaration().map(NodeWithName::getNameAsString).map(pkg -> pkg + "." + name).orElse(name));
		}
		return declaration.findAncestor(TypeDeclaration.class)
				.map(td -> (TypeDeclaration<?>) td)
				.flatMap(SourceAccessWidenerTransformer::getFullyQualifiedName)
				.map(fqn -> fqn + "$" + name);
	}
}
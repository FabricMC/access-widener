package net.fabricmc.accesswidener;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ClassLoaderTypeSolver;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import net.fabricmc.accesswidener.javaparser.SourceAccessWidenerTransformer;

class AccessWidenerSourceTransformerTest {
	AccessWidener widener = new AccessWidener(true);

	private CompilationUnit applyTransformer(String rss) throws IOException {
		JavaParser parser = new JavaParser();
		JavaSymbolSolver symbolSolver = new JavaSymbolSolver(new ClassLoaderTypeSolver(AccessWidenerSourceTransformerTest.class.getClassLoader()));
		parser.getParserConfiguration().setSymbolResolver(symbolSolver);
		CompilationUnit unit = parser.parse(new File("src/test/java/" + rss.replace('.', '/') + ".java")).getResult().get();
		SourceAccessWidenerTransformer transformer = new SourceAccessWidenerTransformer(this.widener);
		transformer.transform(unit);
		return unit;
	}

	@Nested
	class Classes {
		@Test
		public void testMakeAccessible() throws IOException {
			widener.visitClass("test/PackagePrivateClass", AccessWidenerReader.AccessType.ACCESSIBLE, false);
			CompilationUnit unit = applyTransformer("test.PackagePrivateClass");
			TypeDeclaration<?> declaration = unit.getPrimaryType().orElseThrow();
			assertThat(declaration.getModifiers()).contains(Modifier.publicModifier());
		}

		@Test
		void testMakeExtendable() throws Exception {
			widener.visitClass("test/PackagePrivateClass", AccessWidenerReader.AccessType.EXTENDABLE, false);
			CompilationUnit testClass = applyTransformer("test.PackagePrivateClass");
			TypeDeclaration<?> declaration = testClass.getPrimaryType().orElseThrow();
			assertThat(declaration.getModifiers()).contains(Modifier.publicModifier()).doesNotContain(Modifier.finalModifier());
		}

		@Test
		void testMakeAccessibleAndExtendable() throws Exception {
			widener.visitClass("test/FinalPackagePrivateClass", AccessWidenerReader.AccessType.ACCESSIBLE, false);
			widener.visitClass("test/FinalPackagePrivateClass", AccessWidenerReader.AccessType.EXTENDABLE, false);
			CompilationUnit testClass = applyTransformer("test.FinalPackagePrivateClass");
			TypeDeclaration<?> declaration = testClass.getPrimaryType().orElseThrow();
			var modifiers = declaration.getModifiers();
			assertThat(modifiers).contains(Modifier.publicModifier()).doesNotContain(Modifier.finalModifier());
		}

		@Test
		void testMakeInnerClassAccessible() throws Exception {
			widener.visitClass("test/PrivateInnerClass$Inner", AccessWidenerReader.AccessType.ACCESSIBLE, false);
			CompilationUnit testClass = applyTransformer("test.PrivateInnerClass");
			TypeDeclaration<?> inner = testClass.getPrimaryType()
					.map(t -> t.findAll(TypeDeclaration.class, p -> p != t))
					.map(List::stream)
					.flatMap(Stream::findAny)
					.orElseThrow();
			assertThat(inner.getModifiers()).contains(Modifier.publicModifier());
		}
	}

	@Nested
	class Fields {
		@Test
		void testMakeAccessible() throws Exception {
			widener.visitField("test/FieldTests", "privateFinalIntField", "I", AccessWidenerReader.AccessType.ACCESSIBLE, false);
			CompilationUnit unit = applyTransformer("test.FieldTests");
			TypeDeclaration<?> testClass = unit.getPrimaryType().orElseThrow();

			assertThat(List.of(Modifier.publicModifier(), Modifier.finalModifier()))
					.isEqualTo(testClass.getModifiers())
					.isEqualTo(testClass.getFieldByName("privateFinalIntField").orElseThrow().getModifiers());
		}

		@Test
		void testMakeMutable() throws Exception {
			widener.visitField("test/FieldTests", "privateFinalIntField", "I", AccessWidenerReader.AccessType.MUTABLE, false);
			CompilationUnit unit = applyTransformer("test.FieldTests");
			TypeDeclaration<?> testClass = unit.getPrimaryType().orElseThrow();

			// making the field mutable does not affect the containing class
			assertThat(List.of(Modifier.finalModifier())).isEqualTo(testClass.getModifiers());
			assertThat(List.of(Modifier.privateModifier())).isEqualTo(testClass.getFieldByName("privateFinalIntField").orElseThrow().getModifiers());
		}

		@Test
		void testMakeMutableAndAccessible() throws Exception {
			widener.visitField("test/FieldTests", "privateFinalIntField", "I", AccessWidenerReader.AccessType.MUTABLE, false);
			widener.visitField("test/FieldTests", "privateFinalIntField", "I", AccessWidenerReader.AccessType.ACCESSIBLE, false);
			CompilationUnit unit = applyTransformer("test.FieldTests");
			TypeDeclaration<?> testClass = unit.getPrimaryType().orElseThrow();

			// Making the field accessible and mutable affects the class visibility
			assertThat(List.of(Modifier.publicModifier(), Modifier.finalModifier()))
					.isEqualTo(testClass.getModifiers());
			assertThat(List.of(Modifier.publicModifier())).isEqualTo(testClass.getFieldByName("privateFinalIntField").orElseThrow().getModifiers());
		}

		@Test
		void testDontMakeInterfaceMutable() throws Exception {
			widener.visitField("test/InterfaceTests", "staticFinalIntField", "I", AccessWidenerReader.AccessType.MUTABLE, false);
			CompilationUnit unit = applyTransformer("test.InterfaceTests");
			TypeDeclaration<?> testClass = unit.getPrimaryType().orElseThrow();

			assertThat(List.of())
					.isEqualTo((testClass.getFieldByName("staticFinalIntField").orElseThrow().getModifiers()));
		}
	}

	@Nested
	class Methods {
		@Test
		void testMakeAccessible() throws Exception {
			widener.visitMethod("test/MethodTests", "privateMethod", "()V", AccessWidenerReader.AccessType.ACCESSIBLE, false);
			CompilationUnit unit = applyTransformer("test.MethodTests");
			TypeDeclaration<?> testClass = unit.getPrimaryType().orElseThrow();

			assertThat(List.of(Modifier.publicModifier(), Modifier.finalModifier()))
					.isEqualTo(testClass.getModifiers())
					.isEqualTo(testClass.getMethodsByName("privateMethod").get(0).getModifiers());
		}

		@Test
		void testMakeConstructorAccessible() throws Exception {
			widener.visitMethod("test/MethodTests", "<init>", "()V", AccessWidenerReader.AccessType.ACCESSIBLE, false);
			CompilationUnit unit = applyTransformer("test.MethodTests");
			TypeDeclaration<?> testClass = unit.getPrimaryType().orElseThrow();

			assertThat(List.of(Modifier.publicModifier(), Modifier.finalModifier()))
					.isEqualTo(testClass.getModifiers());
			assertThat(List.of(Modifier.publicModifier()))
					.isEqualTo(testClass.getConstructors().get(0).getModifiers());
		}

		@Test
		void testMakeStaticMethodAccessible() throws Exception {
			widener.visitMethod("test/MethodTests", "staticMethod", "()V", AccessWidenerReader.AccessType.ACCESSIBLE, false);
			CompilationUnit unit = applyTransformer("test.MethodTests");
			TypeDeclaration<?> testClass = unit.getPrimaryType().orElseThrow();

			assertThat(List.of(Modifier.publicModifier(), Modifier.finalModifier()))
					.isEqualTo(testClass.getModifiers());
			assertThat(List.of(Modifier.publicModifier(), Modifier.staticModifier()))
					.isEqualTo(testClass.getMethodsByName("staticMethod").get(0).getModifiers());
		}

		@Test
		void testMakeExtendable() throws Exception {
			widener.visitMethod("test/MethodTests", "privateMethod", "()V", AccessWidenerReader.AccessType.EXTENDABLE, false);
			CompilationUnit unit = applyTransformer("test.MethodTests");
			TypeDeclaration<?> testClass = unit.getPrimaryType().orElseThrow();

			assertThat(List.of(Modifier.publicModifier()))
					.isEqualTo(testClass.getModifiers());

			assertThat(List.of(Modifier.protectedModifier()))
					.isEqualTo(testClass.getMethodsByName("privateMethod").get(0).getModifiers());
		}

		@Test
		void testMakeAccessibleAndExtendable() throws Exception {
			widener.visitMethod("test/MethodTests", "privateMethod", "()V", AccessWidenerReader.AccessType.ACCESSIBLE, false);
			widener.visitMethod("test/MethodTests", "privateMethod", "()V", AccessWidenerReader.AccessType.EXTENDABLE, false);
			CompilationUnit unit = applyTransformer("test.MethodTests");
			TypeDeclaration<?> testClass = unit.getPrimaryType().orElseThrow();

			assertThat(List.of(Modifier.publicModifier()))
					.isEqualTo(testClass.getModifiers())
					.isEqualTo(testClass.getMethodsByName("privateMethod").get(0).getModifiers());
		}
	}
}

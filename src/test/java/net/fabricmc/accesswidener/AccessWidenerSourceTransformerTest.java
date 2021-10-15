package net.fabricmc.accesswidener;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.function.Consumer;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ClassLoaderTypeSolver;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import test.PrivateInnerClass;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class AccessWidenerSourceTransformerTest {
	AccessWidener widener = new AccessWidener();
	@Nested
	class Classes {
		@Test
		public void testMakeAccessible() throws IOException {
			widener.visitClass("test/PackagePrivateClass", AccessWidenerReader.AccessType.ACCESSIBLE, false);
			CompilationUnit unit = applyTransformer("test.PackagePrivateClass");
			TypeDeclaration<?> declaration = unit.getPrimaryType().orElseThrow();
			assertEquals(NodeList.nodeList(Modifier.publicModifier()), declaration.getModifiers());
		}

		@Test
		void testMakeExtendable() throws Exception {
			widener.visitClass("test/PackagePrivateClass", AccessWidenerReader.AccessType.EXTENDABLE, false);
			CompilationUnit testClass = applyTransformer("test.PackagePrivateClass");
			TypeDeclaration<?> declaration = testClass.getPrimaryType().orElseThrow();
			var modifiers = declaration.getModifiers();
			assertTrue(modifiers.contains(Modifier.publicModifier()));
			assertFalse(modifiers.contains(Modifier.finalModifier()));
		}

		@Test
		void testMakeAccessibleAndExtendable() throws Exception {
			widener.visitClass("test/FinalPackagePrivateClass", AccessWidenerReader.AccessType.ACCESSIBLE, false);
			widener.visitClass("test/FinalPackagePrivateClass", AccessWidenerReader.AccessType.EXTENDABLE, false);
			CompilationUnit testClass = applyTransformer("test.FinalPackagePrivateClass");
			TypeDeclaration<?> declaration = testClass.getPrimaryType().orElseThrow();
			var modifiers = declaration.getModifiers();
			assertTrue(modifiers.contains(Modifier.publicModifier()));
			assertFalse(modifiers.contains(Modifier.finalModifier()));
		}

	}


	private CompilationUnit applyTransformer(String rss) throws IOException {
		JavaParser parser = new JavaParser();
		JavaSymbolSolver symbolSolver = new JavaSymbolSolver(new ClassLoaderTypeSolver(AccessWidenerSourceTransformerTest.class.getClassLoader()));
		parser.getParserConfiguration().setSymbolResolver(symbolSolver);
		CompilationUnit unit = parser.parse(new File("src/test/java/" + rss.replace('.', '/') + ".java")).getResult().get();
		SourceAccessWidenerTransformer transformer = new SourceAccessWidenerTransformer(this.widener);
		transformer.transform(unit);
		return unit;
	}
}

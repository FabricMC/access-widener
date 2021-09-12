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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import test.PrivateInnerClass;

class AccessWidenerClassVisitorTest {
	AccessWidener widener = new AccessWidener();

	@Nested
	class Classes {
		@Test
		void testMakeAccessible() throws Exception {
			widener.visitClass("test/PackagePrivateClass", AccessWidenerReader.AccessType.ACCESSIBLE, false);
			Class<?> testClass = applyTransformer("test.PackagePrivateClass");
			assertThat(testClass).isPublic();
		}

		@Test
		void testMakeExtendable() throws Exception {
			widener.visitClass("test/PackagePrivateClass", AccessWidenerReader.AccessType.EXTENDABLE, false);
			Class<?> testClass = applyTransformer("test.PackagePrivateClass");
			assertThat(testClass).isPublic().isNotFinal();
		}

		@Test
		void testMakeAccessibleAndExtendable() throws Exception {
			widener.visitClass("test/FinalPackagePrivateClass", AccessWidenerReader.AccessType.ACCESSIBLE, false);
			widener.visitClass("test/FinalPackagePrivateClass", AccessWidenerReader.AccessType.EXTENDABLE, false);
			Class<?> testClass = applyTransformer("test.FinalPackagePrivateClass");
			assertThat(testClass).isPublic().isNotFinal();
		}

		@Test
		void testMakeInnerClassAccessible() throws Exception {
			widener.visitClass("test/PrivateInnerClass$Inner", AccessWidenerReader.AccessType.ACCESSIBLE, false);
			Map<String, Class<?>> classes = applyTransformer();
			assertThat(classes).containsOnlyKeys("test.PrivateInnerClass$Inner", "test.PrivateInnerClass");

			Class<?> outerClass = classes.get("test.PrivateInnerClass");
			Class<?> innerClass = classes.get("test.PrivateInnerClass$Inner");
			assertThat(outerClass.getClasses()).containsOnly(innerClass);
			// For comparison purposes, the untransformed outer class has no public inner-classes
			assertThat(PrivateInnerClass.class.getClasses()).isEmpty();

			assertThat(innerClass).isPublic();
		}

		@Test
		void testMakeInnerClassExtendable() throws Exception {
			widener.visitClass("test/FinalPrivateInnerClass$Inner", AccessWidenerReader.AccessType.EXTENDABLE, false);
			Map<String, Class<?>> classes = applyTransformer();
			assertThat(classes).containsOnlyKeys("test.FinalPrivateInnerClass$Inner", "test.FinalPrivateInnerClass");

			Class<?> outerClass = classes.get("test.FinalPrivateInnerClass");
			Class<?> innerClass = classes.get("test.FinalPrivateInnerClass$Inner");
			assertThat(outerClass.getClasses()).containsOnly(innerClass);
			assertThat(innerClass).isPublic().isNotFinal();
		}
	}

	@Nested
	class Fields {
		@Test
		void testMakeAccessible() throws Exception {
			widener.visitField("test/FieldTests", "privateFinalIntField", "I", AccessWidenerReader.AccessType.ACCESSIBLE, false);
			Class<?> testClass = applyTransformer("test.FieldTests");

			assertEquals("public final", Modifier.toString(testClass.getModifiers()));
			assertEquals("public final", Modifier.toString(testClass.getDeclaredField("privateFinalIntField").getModifiers()));
		}

		@Test
		void testMakeMutable() throws Exception {
			widener.visitField("test/FieldTests", "privateFinalIntField", "I", AccessWidenerReader.AccessType.MUTABLE, false);
			Class<?> testClass = applyTransformer("test.FieldTests");

			// making the field mutable does not affect the containing class
			assertEquals("final", Modifier.toString(testClass.getModifiers()));
			assertEquals("private", Modifier.toString(testClass.getDeclaredField("privateFinalIntField").getModifiers()));
		}

		@Test
		void testMakeMutableAndAccessible() throws Exception {
			widener.visitField("test/FieldTests", "privateFinalIntField", "I", AccessWidenerReader.AccessType.MUTABLE, false);
			widener.visitField("test/FieldTests", "privateFinalIntField", "I", AccessWidenerReader.AccessType.ACCESSIBLE, false);
			Class<?> testClass = applyTransformer("test.FieldTests");

			// Making the field accessible and mutable affects the class visibility
			assertEquals("public final", Modifier.toString(testClass.getModifiers()));
			assertEquals("public", Modifier.toString(testClass.getDeclaredField("privateFinalIntField").getModifiers()));
		}
	}

	@Nested
	class Methods {
		@Test
		void testMakeAccessible() throws Exception {
			widener.visitMethod("test/MethodTests", "privateMethod", "()V", AccessWidenerReader.AccessType.ACCESSIBLE, false);
			Class<?> testClass = applyTransformer("test.MethodTests");

			assertEquals("public final", Modifier.toString(testClass.getModifiers()));
			// Note that this also made the method final
			assertEquals("public final", Modifier.toString(testClass.getDeclaredMethod("privateMethod").getModifiers()));
		}

		@Test
		void testMakeConstructorAccessible() throws Exception {
			widener.visitMethod("test/MethodTests", "<init>", "()V", AccessWidenerReader.AccessType.ACCESSIBLE, false);
			Class<?> testClass = applyTransformer("test.MethodTests");

			assertEquals("public final", Modifier.toString(testClass.getModifiers()));
			// Note that this did _not_ make the ctor final since constructors cannot be overridden anyway.
			assertEquals("public", Modifier.toString(testClass.getDeclaredConstructor().getModifiers()));
		}

		@Test
		void testMakeStaticMethodAccessible() throws Exception {
			widener.visitMethod("test/MethodTests", "staticMethod", "()V", AccessWidenerReader.AccessType.ACCESSIBLE, false);
			Class<?> testClass = applyTransformer("test.MethodTests");

			assertEquals("public final", Modifier.toString(testClass.getModifiers()));
			// Note that this did _not_ make the method final since static methods cannot be overridden anyway.
			assertEquals("public static", Modifier.toString(testClass.getDeclaredMethod("staticMethod").getModifiers()));
		}

		@Test
		void testMakeExtendable() throws Exception {
			widener.visitMethod("test/MethodTests", "privateMethod", "()V", AccessWidenerReader.AccessType.EXTENDABLE, false);
			Class<?> testClass = applyTransformer("test.MethodTests");

			assertEquals("public", Modifier.toString(testClass.getModifiers()));
			assertEquals("protected", Modifier.toString(testClass.getDeclaredMethod("privateMethod").getModifiers()));
		}

		@Test
		void testMakeAccessibleAndExtendable() throws Exception {
			widener.visitMethod("test/MethodTests", "privateMethod", "()V", AccessWidenerReader.AccessType.ACCESSIBLE, false);
			widener.visitMethod("test/MethodTests", "privateMethod", "()V", AccessWidenerReader.AccessType.EXTENDABLE, false);
			Class<?> testClass = applyTransformer("test.MethodTests");

			assertEquals("public", Modifier.toString(testClass.getModifiers()));
			assertEquals("public", Modifier.toString(testClass.getDeclaredMethod("privateMethod").getModifiers()));
		}

		@Test
		void testPrivateMethodCallsAreRewrittenToInvokeVirtual() throws Exception {
			widener.visitMethod("test/PrivateMethodSubclassTest", "test", "()I", AccessWidenerReader.AccessType.EXTENDABLE, false);
			// We need to ensure that the subclass goes through our hacky class-loader as well
			widener.getTargets().add("test.PrivateMethodSubclassTest$Subclass");
			Class<?> testClass = applyTransformer().get("test.PrivateMethodSubclassTest");
			int result = (int) testClass.getMethod("callMethodOnSubclass").invoke(null);
			// this signifies that the INVOKESPECIAL instruction got rewritten to INVOKEVIRTUAL and the
			// method of the same name in the subclass was invoked.
			assertThat(result).isEqualTo(456);
		}
	}

	/**
	 * Applies a given access transformer but also ensures that the given class is the only class that is affected.
	 */
	private Class<?> applyTransformer(String className) throws Exception {
		Map<String, Class<?>> classes = applyTransformer();
		assertThat(classes).containsOnlyKeys(className);
		return classes.get(className);
	}

	/**
	 * Loads all classes from the "test" package with the given access widener applied. Only returns the classes
	 * that have actually been affected by the widener.
	 */
	private Map<String, Class<?>> applyTransformer() throws Exception {
		TransformingClassLoader classLoader = new TransformingClassLoader(getClass().getClassLoader(), widener);

		// This assumes that tests are being run from an exploded classpath and not a jar file, which is the case
		// for both IDE and Gradle runs.
		Path classFile = Paths.get(getClass().getResource("/test/PackagePrivateClass.class").toURI());
		Path classFolder = classFile.getParent();
		return Files.walk(classFolder)
				// Map /test/X.class -> X.class
				.map(p -> p.getFileName().toString())
				.filter(p -> p.endsWith(".class"))
				.map(p -> "test." + p.substring(0, p.length() - ".class".length()))
				.filter(widener.getTargets()::contains)
				.collect(Collectors.toMap(
						p -> p,
						p -> {
							try {
								return Class.forName(p, false, classLoader);
							} catch (ClassNotFoundException e) {
								return fail(e);
							}
						}
				));
	}

	private static class TransformingClassLoader extends ClassLoader {
		private final AccessWidener widener;

		TransformingClassLoader(ClassLoader parent, AccessWidener widener) {
			super(parent);
			this.widener = widener;
		}

		@Override
		protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
			if (name.startsWith("test.")) {
				InputStream classData = getParent().getResourceAsStream(name.replace('.', '/') + ".class");

				if (classData != null) {
					if (widener.getTargets().contains(name)) {
						try {
							ClassReader classReader = new ClassReader(classData);
							ClassWriter classWriter = new ClassWriter(0);
							ClassVisitor visitor = classWriter;
							visitor = AccessWidenerClassVisitor.createClassVisitor(Opcodes.ASM9, visitor, widener);
							classReader.accept(visitor, 0);
							byte[] bytes = classWriter.toByteArray();
							return defineClass(name, bytes, 0, bytes.length);
						} catch (IOException e) {
							throw new ClassNotFoundException();
						}
					}
				}
			}

			return super.loadClass(name, resolve);
		}
	}
}

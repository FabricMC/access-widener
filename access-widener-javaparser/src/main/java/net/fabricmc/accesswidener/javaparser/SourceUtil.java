package net.fabricmc.accesswidener.javaparser;

import java.util.Iterator;
import java.util.List;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.TypeDeclaration;
import org.objectweb.asm.Opcodes;

class SourceUtil implements Opcodes {

	static void fromFlags(List<Modifier> modifiers, int access, boolean abstract_) {
		for(int i = 0; i < 32; i++) {
			int flag = 1 << i;
			switch(flag & access) {
			case ACC_ABSTRACT:
				if(!abstract_) {
					modifiers.add(Modifier.abstractModifier());
				}
				break;
			case ACC_STATIC:
				modifiers.add(Modifier.staticModifier());
				break;
			case ACC_PRIVATE:
				modifiers.add(Modifier.privateModifier());
				break;
			case ACC_PROTECTED:
				modifiers.add(Modifier.protectedModifier());
				break;
			case ACC_PUBLIC:
				modifiers.add(Modifier.publicModifier());
				break;
			case ACC_FINAL:
				modifiers.add(Modifier.finalModifier());
				break;
			}
		}
	}

	static int toClassFlags(List<Modifier> modifiers, TypeDeclaration<?> declaration) {
		int flags = toFlags(modifiers, false);
		if(declaration.isEnumDeclaration()) {
			flags |= ACC_ENUM;
		} else if(declaration.isAnnotationDeclaration()) {
			flags |= ACC_ANNOTATION;
		} else if(declaration.isClassOrInterfaceDeclaration() && declaration.asClassOrInterfaceDeclaration().isInterface()) {
			flags |= ACC_INTERFACE;
		} else if(declaration.isRecordDeclaration()) {
			flags |= ACC_RECORD;
		}
		return flags;
	}

	/**
	 * ATM this only implements the flags access widener actually changes, it can be expanded further as needed
	 *
	 * @param abstract_ true if class is interface
	 */
	static int toFlags(List<Modifier> modifiers, boolean abstract_) {
		int flag = 0;
		Iterator<Modifier> iterator = modifiers.iterator();
		while(iterator.hasNext()) {
			Modifier modifier = iterator.next();
			Modifier.Keyword keyword = modifier.getKeyword();
			boolean remove = true;
			switch(keyword) {
			case DEFAULT:
				abstract_ = false;
				break;
			case FINAL:
				flag |= ACC_FINAL;
				break;
			case PUBLIC:
				flag |= ACC_PUBLIC;
				break;
			case PROTECTED:
				flag |= ACC_PROTECTED;
				break;
			case PRIVATE:
				flag |= ACC_PRIVATE;
				break;
			case STATIC:
				flag |= ACC_STATIC;
				break;
			default:
				remove = false;
			}
			if(remove) {
				iterator.remove();
			}
		}
		if(abstract_) {
			flag |= ACC_ABSTRACT;
		}
		return flag;
	}
}

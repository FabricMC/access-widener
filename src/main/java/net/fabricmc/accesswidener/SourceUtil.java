package net.fabricmc.accesswidener;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import com.github.javaparser.ast.Modifier;

class SourceUtil {
	private static List<Modifier> apply(List<Modifier> modifiers, Predicate<Modifier.Keyword> shouldAdd, Modifier add, Modifier.Keyword... remove) {
		List<Modifier.Keyword> keywordList = Arrays.asList(remove);
		modifiers.removeIf(m -> keywordList.contains(m.getKeyword()));
		if(modifiers.stream().map(Modifier::getKeyword).allMatch(shouldAdd)) {
			modifiers.add(add);
		}
		return modifiers;
	}

	static List<Modifier> makePublic(List<Modifier> modifiers) {
		return apply(modifiers, m -> m != Modifier.Keyword.PUBLIC, Modifier.publicModifier(), Modifier.Keyword.PRIVATE, Modifier.Keyword.PROTECTED);
	}

	static List<Modifier> removeFinal(List<Modifier> modifiers) {
		return apply(modifiers, m -> false, null, Modifier.Keyword.FINAL);
	}

	static List<Modifier> makeExtendable(List<Modifier> modifiers) {
		return apply(modifiers, m -> m != Modifier.Keyword.PUBLIC && m != Modifier.Keyword.PROTECTED, Modifier.protectedModifier(), Modifier.Keyword.FINAL, Modifier.Keyword.PRIVATE);
	}
}

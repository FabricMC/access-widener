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

import java.util.Map;

import org.objectweb.asm.commons.Remapper;

public final class AccessWidenerRemapper {
	private final AccessWidener input;
	private final String to;
	private final Remapper remapper;

	/**
	 * @param input The access widener to remap. It will not be modified.
	 * @param to    The namespace that the access widener will be remapped to by the given remapper.
	 */
	public AccessWidenerRemapper(AccessWidener input, Remapper remapper, String to) {
		this.input = input;
		this.to = to;
		this.remapper = remapper;
	}

	/**
	 * @return Either the original access widener if no remapping is necessary, or a new access widener that contains
	 * remapped names.
	 */
	public AccessWidener remap() {
		// Dont remap if we dont need to
		if (input.namespace.equals(to)) {
			return input;
		}

		AccessWidener remapped = new AccessWidener();
		remapped.namespace = to;

		for (Map.Entry<String, AccessWidener.Access> entry : input.classAccess.entrySet()) {
			remapped.classAccess.put(remapper.map(entry.getKey()), entry.getValue());
		}

		for (Map.Entry<EntryTriple, AccessWidener.Access> entry : input.methodAccess.entrySet()) {
			remapped.addOrMerge(remapped.methodAccess, remapMethod(entry.getKey()), entry.getValue());
		}

		for (Map.Entry<EntryTriple, AccessWidener.Access> entry : input.fieldAccess.entrySet()) {
			remapped.addOrMerge(remapped.fieldAccess, remapField(entry.getKey()), entry.getValue());
		}

		return remapped;
	}

	private EntryTriple remapMethod(EntryTriple entryTriple) {
		return new EntryTriple(
				remapper.map(entryTriple.getOwner()),
				remapper.mapMethodName(entryTriple.getOwner(), entryTriple.getName(), entryTriple.getDesc()),
				remapper.mapDesc(entryTriple.getDesc())
		);
	}

	private EntryTriple remapField(EntryTriple entryTriple) {
		return new EntryTriple(
				remapper.map(entryTriple.getOwner()),
				remapper.mapFieldName(entryTriple.getOwner(), entryTriple.getName(), entryTriple.getDesc()),
				remapper.mapDesc(entryTriple.getDesc())
		);
	}
}

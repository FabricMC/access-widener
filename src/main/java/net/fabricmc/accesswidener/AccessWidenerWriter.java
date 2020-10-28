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

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class AccessWidenerWriter {
	private final AccessWidener accessWidener;

	public AccessWidenerWriter(AccessWidener accessWidener) {
		this.accessWidener = accessWidener;
	}

	public void write(StringWriter writer) {
		writer.write("accessWidener\tv1\t");
		writer.write(accessWidener.namespace);
		writer.write("\n");

		for (Map.Entry<String, AccessWidener.Access> entry : accessWidener.classAccess.entrySet()) {
			for (String s : getAccesses(entry.getValue())) {
				writer.write(s);
				writer.write("\tclass\t");
				writer.write(entry.getKey());
				writer.write("\n");
			}
		}

		for (Map.Entry<EntryTriple, AccessWidener.Access> entry : accessWidener.methodAccess.entrySet()) {
			writeEntry(writer, "method", entry.getKey(), entry.getValue());
		}

		for (Map.Entry<EntryTriple, AccessWidener.Access> entry : accessWidener.fieldAccess.entrySet()) {
			writeEntry(writer, "field", entry.getKey(), entry.getValue());
		}
	}

	private void writeEntry(StringWriter writer, String type, EntryTriple entryTriple, AccessWidener.Access access) {
		for (String s : getAccesses(access)) {
			writer.write(s);
			writer.write("\t");
			writer.write(type);
			writer.write("\t");
			writer.write(entryTriple.getOwner());
			writer.write("\t");
			writer.write(entryTriple.getName());
			writer.write("\t");
			writer.write(entryTriple.getDesc());
			writer.write("\n");
		}
	}

	private List<String> getAccesses(AccessWidener.Access access) {
		List<String> accesses = new ArrayList<>();

		if (access == AccessWidener.ClassAccess.ACCESSIBLE || access == AccessWidener.MethodAccess.ACCESSIBLE || access == AccessWidener.FieldAccess.ACCESSIBLE || access == AccessWidener.MethodAccess.ACCESSIBLE_EXTENDABLE || access == AccessWidener.ClassAccess.ACCESSIBLE_EXTENDABLE || access == AccessWidener.FieldAccess.ACCESSIBLE_MUTABLE) {
			accesses.add("accessible");
		}

		if (access == AccessWidener.ClassAccess.EXTENDABLE || access == AccessWidener.MethodAccess.EXTENDABLE || access == AccessWidener.MethodAccess.ACCESSIBLE_EXTENDABLE || access == AccessWidener.ClassAccess.ACCESSIBLE_EXTENDABLE) {
			accesses.add("extendable");
		}

		if (access == AccessWidener.FieldAccess.MUTABLE || access == AccessWidener.FieldAccess.ACCESSIBLE_MUTABLE) {
			accesses.add("mutable");
		}

		return accesses;
	}
}

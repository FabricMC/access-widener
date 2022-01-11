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

public final class EntryTriple {
	final String owner;
	final String name;
	final String desc;
	final String fuzzy;
	final boolean requiresSourceCompatibility;

	public static EntryTriple create(String owner, String name, String desc, boolean requiresSourceCompatibility) {
		return new EntryTriple(owner, name, desc, requiresSourceCompatibility);
	}

	private EntryTriple(String owner, String name, String desc, boolean requiresSourceCompatibility) {
		this.owner = owner;
		this.name = name;
		this.desc = desc;
		this.fuzzy = requiresSourceCompatibility ? desc.replace('.', '/') : null;
		this.requiresSourceCompatibility = requiresSourceCompatibility;
	}

	public String getOwner() {
		return this.owner;
	}

	public String getName() {
		return this.name;
	}

	public String getDesc() {
		return this.desc;
	}

	@Override
	public String toString() {
		return "EntryTriple{owner=" + this.owner + ",name=" + this.name + ",desc=" + this.desc + "}";
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof EntryTriple)) {
			return false;
		} else if (o == this) {
			return true;
		} else {
			EntryTriple other = (EntryTriple) o;
			if(other.owner.equals(this.owner) && other.name.equals(this.name)) {
				return this.requiresSourceCompatibility ? other.fuzzy.equals(this.fuzzy) : other.desc.equals(this.desc);
			}
			return false;
		}
	}

	@Override
	public int hashCode() {
		return this.owner.hashCode() * 37 + this.name.hashCode() * 19 + (requiresSourceCompatibility ? this.fuzzy : this.desc).hashCode();
	}
}

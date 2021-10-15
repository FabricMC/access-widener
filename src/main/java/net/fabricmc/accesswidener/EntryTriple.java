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

class EntryTriple {
	final String owner;
	final String name;
	final String desc;

	static EntryTriple create(String owner, String name, String desc, boolean requiresSourceCompatibility) {
		return requiresSourceCompatibility ? new Fuzzy(owner, name, desc) : new EntryTriple(owner, name, desc);
	}

	EntryTriple(String owner, String name, String desc) {
		this.owner = owner;
		this.name = name;
		this.desc = desc;
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
			return other.owner.equals(this.owner) && other.name.equals(this.name) && other.desc.equals(this.desc);
		}
	}

	@Override
	public int hashCode() {
		return this.owner.hashCode() * 37 + this.name.hashCode() * 19 + this.desc.hashCode();
	}

	static class Fuzzy extends EntryTriple {
		final String replaced;
		Fuzzy(String owner, String name, String desc) {
			super(owner, name, desc);
			// javaparser does not correctly resolve descriptors correctly
			this.replaced = desc.replace('$', '/');
		}

		@Override
		public boolean equals(Object o) {
			EntryTriple triple;
			if(o instanceof EntryTriple) {
				triple = (EntryTriple) o;
				if(!(triple.owner.equals(this.owner) && triple.name.equals(this.name))) {
					return false;
				}
			} else {
				return false;
			}

			if(o instanceof Fuzzy) {
				Fuzzy fuzzy = (Fuzzy) o;
				return fuzzy.replaced.equals(this.replaced);
			} else {
				return triple.desc.replace('$', '/').equals(this.replaced);
			}
		}

		@Override
		public int hashCode() {
			return this.owner.hashCode() * 37 + this.name.hashCode() * 19 + this.replaced.hashCode();
		}
	}
}

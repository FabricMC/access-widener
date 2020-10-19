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

final class EntryTriple {
	private final String owner;
	private final String name;
	private final String desc;

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

	public String toString() {
		return "EntryTriple{owner=" + this.owner + ",name=" + this.name + ",desc=" + this.desc + "}";
	}

	protected Object clone() {
		return new EntryTriple(this.owner, this.name, this.desc);
	}

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

	public int hashCode() {
		return this.owner.hashCode() * 37 + this.name.hashCode() * 19 + this.desc.hashCode();
	}
}

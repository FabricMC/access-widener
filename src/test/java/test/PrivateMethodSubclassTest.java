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

package test;

import java.util.function.Supplier;

public class PrivateMethodSubclassTest {
	private int test() {
		return 123;
	}

	private int callPrivateMethod() {
		// This should be INVOKESPECIAL because test() is private in this context
		return test();
	}

	private int callPrivateMethodWithLambda() {
		// Private method handles tag in bootstrap method arguments should be H_INVOKESPECIAL
		Supplier<Integer> supplier = this::test;
		return supplier.get();
	}

	public static int callMethodOnSubclass() {
		// Without making test() extendable or accessible, this will call the private method, even on the subclass
		// otherwise it'll call the subclasses method. This is detectable from the return value.
		PrivateMethodSubclassTest r = new Subclass();
		return r.callPrivateMethod();
	}

	public static int callMethodWithLambdaOnSubclass() {
		PrivateMethodSubclassTest r = new Subclass();
		return r.callPrivateMethodWithLambda();
	}

	public static class Subclass extends PrivateMethodSubclassTest {
		public int test() {
			return 456;
		}
	}
}

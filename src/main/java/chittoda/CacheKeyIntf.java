/*
 * Copyright (C) 2011-2012 Jitendra Chittoda
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership. Jitendra Chittoda licenses
 * this file to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package chittoda;

/**
 * Object that will act as a Key while assigning the 
 * Task to ThreadPool, should implement this.
 * 
 * Key defines how your Tasks would be sequentialise. Sequence
 * of the Tasks would be maintained based on their Key.
 * @author Jitendra Chittoda
 *
 * @param <T>
 */
public interface CacheKeyIntf<T> {

	/**
	 * Key of type T, which would be tell FSMThreadPool to sequentialise the
	 * Tasks those are having the same Key. 
	 * @return
	 */
	public T getKey();
}

/*
 * Copyright (C) 2017 Sirius, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package renovate;



class DefaultCallAdapter<T,E> implements CallAdapter<E,Call<T>> {
    static final DefaultCallAdapter INSTANCE = new DefaultCallAdapter();

    //传入call转为rxjava实现等
    @Override
    public Call<T> adapt(Call<E> call, AdapterParam param) {
        return (Call<T>) call;
    }
}

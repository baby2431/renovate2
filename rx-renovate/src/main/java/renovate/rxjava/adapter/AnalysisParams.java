/*
 * Copyright 2016 jeasonlzy(廖子尧)
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
package renovate.rxjava.adapter;

import renovate.AdapterParam;
import renovate.Call;
import renovate.Response;
import renovate.rxjava.subscribe.CallEnqueueOnSubscribe;
import renovate.rxjava.subscribe.CallExecuteOnSubscribe;
import rx.Observable;

/**
 * ================================================
 * 作    者：jeasonlzy（廖子尧）Github地址：https://github.com/jeasonlzy
 * 版    本：1.0
 * 创建日期：2017/5/27
 * 描    述：
 * 修订历史：
 * ================================================
 */
class AnalysisParams {

    static <T> Observable.OnSubscribe<Response<T>> analysis(Call<T> call, AdapterParam param) {
        Observable.OnSubscribe<Response<T>> onSubscribe;
        if (param == null) param = new AdapterParam();
        if (param.isAsync) {
            onSubscribe = new CallEnqueueOnSubscribe<>(call);
        } else {
            onSubscribe = new CallExecuteOnSubscribe<>(call);
        }
        return onSubscribe;
    }
}

package com.test.rxjava;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.test.bean.BaseBean;
import com.test.bean.LoginBean;
import com.test.bean.RegisterBean;
import com.test.net.Api;
import com.test.net.RetrofitManager;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

public class TransformActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "TransformActivity";
    private EditText mEtUserName;
    private EditText mEtPwd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transform);

        findViewById(R.id.tv_tran_map).setOnClickListener(this);
        findViewById(R.id.tv_tran_flatmap).setOnClickListener(this);
        findViewById(R.id.tv_do_next).setOnClickListener(this);
        findViewById(R.id.tv_tran_zip).setOnClickListener(this);
        findViewById(R.id.tv_tran_test).setOnClickListener(this);

        mEtUserName = findViewById(R.id.et_user_name);
        mEtPwd = findViewById(R.id.et_pwd);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_tran_map:
                /*
                 * 通常情况下，上游放射什么数据类型的事件，下游就接收什么类型的事件。
                 * 通过Map转换操作符，可使下游接收事件数据类型跟上游不一样。
                 * 通过Map, 可以将上游发来的事件转换为任意的类型, 可以是一个Object, 也可以是一个集合。
                 * */
                Observable.create(new ObservableOnSubscribe<Integer>() {
                    @Override
                    public void subscribe(ObservableEmitter<Integer> emitter) throws Exception {
                        //上游发射事件为 Integer 类型
                        emitter.onNext(1);
                    }
                }).map(new Function<Integer, String>() {
                    @Override
                    public String apply(Integer integer) throws Exception {
                        //通过map转换，接收上游 Integer 类型，返回 String 到下游
                        return "map transform -- " + integer;
                    }
                }).subscribe(new Consumer<String>() {
                    @Override
                    public void accept(String s) throws Exception {
                        //下游接收到的事件为String类型
                        Log.d(TAG, "observer : " + s);
                    }
                });
                break;
            case R.id.tv_tran_flatmap:
                /*
                 * FlatMap() 将一个发送事件的上游Observable变换为多个发送事件的Observables，然后将它们发射的事件合并后放进一个单独的Observable里。
                 *
                 * flatMap() 其实与 map() 类似，但是 flatMap() 返回的是一个 Observable.Map()返回的是转换后的数据
                 *
                 * 经过 flatMap() 转换后的事件是无序的，若有序需要使用 concatMap() 操作符
                 * */
                Observable.create(new ObservableOnSubscribe<Integer>() {
                    @Override
                    public void subscribe(ObservableEmitter<Integer> emitter) throws Exception {
                        emitter.onNext(133);
                        emitter.onNext(888);
                        emitter.onNext(55);
                        emitter.onNext(456);
                        emitter.onNext(133);
                        emitter.onNext(888);
                        emitter.onNext(55);
                        emitter.onNext(456);
                        emitter.onNext(133);
                        emitter.onNext(888);
                        emitter.onNext(55);
                        emitter.onNext(456);
                        emitter.onNext(133);
                        emitter.onNext(888);
                        emitter.onNext(55);
                        emitter.onNext(456);
                    }
                }).doOnNext(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer integer) throws Exception {
                        Log.d(TAG, "accept: " + integer);
                    }
                }).flatMap(new Function<Integer, ObservableSource<String>>() {
                    @Override
                    public ObservableSource<String> apply(final Integer integer) throws Exception {
                        // just 操作符，创建一个被观察者并发射事件
                        return Observable.just("observe_2nd : " + integer).delay(10, TimeUnit.MILLISECONDS);
                    }
                }).subscribe(new Consumer<String>() {
                    @Override
                    public void accept(String s) throws Exception {
                        Log.d(TAG, s);
                    }
                });
                break;
            case R.id.tv_do_next:
                /*
                 * doNext能响应上游发送的事件
                 * 只有创建的第一个Observable的subscribe方法为上游线程，后面无论是flatmap方法中返回的Observable的subscribe方法都是下游线程
                 * 下游线程可以重复多次切换
                 * */
                Observable.create(new ObservableOnSubscribe<Integer>() {
                    @Override
                    public void subscribe(ObservableEmitter<Integer> emitter) throws Exception {
                        Log.d(TAG, "Observable thread: " + Thread.currentThread().getName());
                        emitter.onNext(133);
                    }
                }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                        .doOnNext(new Consumer<Integer>() {
                            @Override
                            public void accept(Integer integer) throws Exception {
                                //只有在订阅关系 subscribe 建立后，doNext 才会接收到上游事件（在下游onNext前调用，相当于一个下游，必须建立订阅关系后才会调用）
                                Log.d(TAG, "doOnNext thread: " + Thread.currentThread().getName());
                            }
                        }).observeOn(Schedulers.io()).concatMap(new Function<Integer, ObservableSource<String>>() {
                    @Override
                    public ObservableSource<String> apply(Integer integer) throws Exception {
                        Log.d(TAG, "concatMap thread: " + Thread.currentThread().getName());
                        return Observable.create(new ObservableOnSubscribe<String>() {
                            @Override
                            public void subscribe(ObservableEmitter<String> emitter) throws Exception {
                                // concamap 中返回的Observable仍然在下游线程中，受observeOn来控制线程
                                Log.d(TAG, "concatMap--oba thread: " + Thread.currentThread().getName());
                                emitter.onNext("ababab");
                            }
                        });
                    }
                }).subscribe(new Consumer<String>() {
                    @Override
                    public void accept(String s) throws Exception {
                        Log.d(TAG, "accept thread: " + Thread.currentThread().getName());
                    }
                });
                break;
            case R.id.tv_tran_zip:
                /*
                 * zip操作符
                 * zip通过一个函数将多个Observable发送的事件结合到一起，然后发送这些组合到一起的事件. 它按照严格的顺序应用这个函数。它只发射与发射数据项最少的那个Observable一样多的数据。
                 * 上游有两根水管，经过zip，将两根水管的事件组合到一起，向下游发射事件。
                 * 组合的过程是分别从 两根水管里各取出一个事件 来进行组合, 并且一个事件只能被使用一次, 组合的顺序是严格按照事件发送的顺利 来进行的。
                 * 最终下游收到的事件数量 是和上游中发送事件最少的那一根水管的事件数量 相同。
                 * */

                //上游水管1
                Observable<Integer> observable1 = Observable.create(new ObservableOnSubscribe<Integer>() {
                    @Override
                    public void subscribe(ObservableEmitter<Integer> emitter) throws Exception {
                        Log.d(TAG, "subscribe_1: " + 1);
                        emitter.onNext(1);

                        Log.d(TAG, "subscribe_1: " + 2);
                        emitter.onNext(2);

                        Log.d(TAG, "subscribe_1: " + 3);
                        emitter.onNext(3);

                        Log.d(TAG, "subscribe_1: " + 4);
                        emitter.onNext(4);
                    }
                }).subscribeOn(Schedulers.io()); 

                //上游水管2
                Observable<String> observable2 = Observable.create(new ObservableOnSubscribe<String>() {
                    @Override
                    public void subscribe(ObservableEmitter<String> emitter) throws Exception {
                        Log.d(TAG, "subscribe_2: " + "A");
                        emitter.onNext("A");

                        Log.d(TAG, "subscribe_2: " + "B");
                        emitter.onNext("B");

                        Log.d(TAG, "subscribe_2: " + "C");
                        emitter.onNext("C");

                        Log.d(TAG, "subscribe_2: " + "D");
                        emitter.onNext("D");
                    }
                }).subscribeOn(Schedulers.io());

                //zip操作
                Observable.zip(observable1, observable2, new BiFunction<Integer, String, String>() {
                    @Override
                    public String apply(Integer i, String s) throws Exception {
                        String t = "--- " + s + i + " ---";
//                        Log.d(TAG, "apply: " + t);
                        return t;
                    }
                }).subscribe(new Observer<String>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(String s) {
                        Log.d(TAG, "onNext: " + s);
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });


                break;
            case R.id.tv_tran_test:
                /*
                 * request test
                 * 先注册账号，获取registerbean，通过flatmap获取registerbean中数据构建loginobservable进行login请求
                 * */
                String userName = mEtUserName.getText().toString();
                String pwd = mEtPwd.getText().toString();

                final Api api = RetrofitManager.getInstance().create(Api.class);
                api.register(userName, pwd, pwd)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnNext(new Consumer<BaseBean<RegisterBean>>() {
                            @Override
                            public void accept(BaseBean<RegisterBean> registerBeanBaseBean) throws Exception {
                                //注册成功
                                Log.d(TAG, "accept: register success");
                            }
                        }).observeOn(Schedulers.io()) // 登录请求放在io线程
                        .flatMap(new Function<BaseBean<RegisterBean>, ObservableSource<BaseBean<LoginBean>>>() {
                            @Override
                            public ObservableSource<BaseBean<LoginBean>> apply(BaseBean<RegisterBean> registerBeanBaseBean) throws Exception {
                                Log.d(TAG, "flatMap_username: " + registerBeanBaseBean.getData().getUsername());
                                Log.d(TAG, "flatMap_pwd: " + registerBeanBaseBean.getData().getPassword());
                                return api.login(registerBeanBaseBean.getData().getUsername(), registerBeanBaseBean.getData().getPassword());
                            }
                        }).observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Observer<BaseBean<LoginBean>>() {
                            @Override
                            public void onSubscribe(Disposable d) {

                            }

                            @Override
                            public void onNext(BaseBean<LoginBean> loginBeanBaseBean) {
                                Log.d(TAG, "onNext: login success");
                            }

                            @Override
                            public void onError(Throwable e) {
                                Log.d(TAG, "onError: " + e);
                            }

                            @Override
                            public void onComplete() {

                            }
                        });
                break;
            default:
                break;
        }

    }
}

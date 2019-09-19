package com.test.rxjava;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

public class CreateActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "CreateActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create);

        initView();
    }

    private void initView() {
        findViewById(R.id.btn_create_ob).setOnClickListener(this);
        findViewById(R.id.btn_create_ob2).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_create_ob:
                //创建被观察者observable （上游）
                Observable<String> observable = Observable.create(new ObservableOnSubscribe<String>() {
                    @Override
                    public void subscribe(ObservableEmitter<String> emitter) throws Exception {
                        Log.d(TAG, "observable_subscribe");
                        /*使用emitter发射器向下游发送事件,emitter能发出next事件、complete事件和error事件。*/
                        /*规则：
                              上游可以发送无限个onNext, 下游也可以接收无限个onNext.
                              当上游发送了一个onComplete后, 上游onComplete之后的事件将会继续发送, 而下游收到onComplete事件之后将不再继续接收事件.
                              当上游发送了一个onError后, 上游onError之后的事件将继续发送, 而下游收到onError事件之后将不再继续接收事件.
                              上游可以不发送onComplete或onError.
                              最为关键的是onComplete和onError必须唯一并且互斥, 即不能发多个onComplete, 也不能发多个onError, 也不能先发一个onComplete, 然后再发一个onError, 反之亦然
                        * */
                        emitter.onNext("observable ---- 1");
                        emitter.onNext("observable ---- 2");
                        emitter.onNext("observable ---- 3");
                        emitter.onComplete();
                    }
                });
                //创建观察者observe （下游）
                Observer<String> observer = new Observer<String>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        //subscribe方法调用后回调到这里
                        Log.d(TAG, "onSubscribe");
                    }

                    @Override
                    public void onNext(String s) {
                        //接收上游发送的事件
                        Log.d(TAG, "observer ---  onNext" + " : " + s);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.d(TAG, "observer --- onError");
                    }

                    @Override
                    public void onComplete() {
                        Log.d(TAG, "observer --- onComplete");
                    }
                };
                //创建订阅关系（连接上下游）,点阅后，上游才能向下游发送事件
                observable.subscribe(observer);
                break;
            case R.id.btn_create_ob2:
                //链式调用
                Observable.create(new ObservableOnSubscribe<Integer>() {
                    @Override
                    public void subscribe(ObservableEmitter<Integer> emitter) throws Exception {
                        emitter.onNext(1);
                        emitter.onNext(2);
                        emitter.onNext(3);
                        emitter.onNext(4);
                        emitter.onNext(5);
//                        emitter.onComplete();
                    }
                }).subscribe(new Observer<Integer>() {
                    private Disposable mDisposable;
                    @Override
                    public void onSubscribe(Disposable d) {
                        //Disposable 相当于上下游之间的一个开关
                        Log.d(TAG, "observer --- onSubscribe");
                        mDisposable = d;
                    }

                    @Override
                    public void onNext(Integer s) {
                        /*
                            调用 Disposable 的 dispose()方法切断上下游，导致下游不再接收上游的事件，直接回调到onComplete()
                            dispose()方法不会影响上游事件，几时调用这个方法，上游还会继续发送事件
                        * */
                        if (s == 3) {
                            mDisposable.dispose();
                        }
                        Log.d(TAG, "observer ---  onNext" + " : " + s);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.d(TAG, "observer --- onError");
                    }

                    @Override
                    public void onComplete() {
                        Log.d(TAG, "observer --- onComplete");
                    }
                });

                break;
            default:
                break;
        }

    }
}

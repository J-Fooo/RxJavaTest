package com.test.rxjava;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class BackPressureActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "BackPressureActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_back_pressure);

        findViewById(R.id.tv_event_cache_size).setOnClickListener(this);
        findViewById(R.id.tv_sync_subscribe).setOnClickListener(this);
        findViewById(R.id.tv_async_subscribe).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_event_cache_size:
                /*
                * 当通过zip将上游两条管道发出的事件进行压缩时，zip会给每根管道提供一个（水缸）缓冲区! 它将每根水管发出的事件保存起来,
                * 等两个水缸都有事件了之后就分别从水缸中取出一个事件来组合, 当其中一个水缸是空的时候就处于等待的状态。水缸存储是按顺序保存的, 先进来的事件先取出来。
                * 如果一直往水缸存入事件，会导致oom
                * */
                Observable<Integer> observable1 = Observable.create(new ObservableOnSubscribe<Integer>() {
                    @Override
                    public void subscribe(ObservableEmitter<Integer> emitter) throws Exception {
                        for (int i = 0; ; i++) {   //无限循环发事件
                            emitter.onNext(i);
                        }
                    }
                }).subscribeOn(Schedulers.io());

                Observable<String> observable2 = Observable.create(new ObservableOnSubscribe<String>() {
                    @Override
                    public void subscribe(ObservableEmitter<String> emitter) throws Exception {
                        emitter.onNext("A");// 由于水管2只发送一个事件，水管1中的事件会一直发送到水缸中，等待水管2其他事件

//                        emitter.onComplete();  若水管2发送了onComplete事件，水管一将不会继续发送事件到水缸等待
                    }
                }).subscribeOn(Schedulers.io());

                Observable.zip(observable1, observable2, new BiFunction<Integer, String, String>() {
                    @Override
                    public String apply(Integer integer, String s) throws Exception {
                        return integer + s;
                    }
                }).observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer<String>() {
                    @Override
                    public void accept(String s) throws Exception {
                        Log.d(TAG, s);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        Log.d(TAG, throwable.getMessage());
                    }
                });
                break;
            case R.id.tv_sync_subscribe:
                /*
                * 同步订阅
                *   上下游在同一线程的时候，代码同步调用。上游每次调用emitter.onNext(i)其实就相当于直接调用了Consumer中的accept()。
                *   当上下游工作在同一个线程中时, 这时候是一个同步的订阅关系, 也就是说上游每发送一个事件必须等到下游接收处理完了以后才能接着发送下一个事件，所以上游发射的事件不会保存到水缸中。
                * */
                Observable.create(new ObservableOnSubscribe<Integer>() {
                    @Override
                    public void subscribe(ObservableEmitter<Integer> emitter) throws Exception {
                        for (int i = 0; ; i++) {   //无限循环发事件
                            emitter.onNext(i);
                        }
                    }
                }).subscribe(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer integer) throws Exception {
                        Thread.sleep(2000);
                        Log.d(TAG, "" + integer);
                    }
                });
                break;
            case R.id.tv_async_subscribe:
                /*
                * 异步订阅
                *   上下游工作在不同的线程中时，是一个异步的订阅关系, 这个时候上游发送数据不需要等待下游接收,两个线程并不能直接进行通信, 因此上游发送的事件并不能直接到下游里去。
                *   上游把事件发送到水缸里去, 下游从水缸里取出事件来处理, 因此, 当上游发事件的速度太快, 下游取事件的速度太慢, 水缸就会迅速装满, 然后溢出来, 最后就OOM了。
                * */
                Observable.create(new ObservableOnSubscribe<Integer>() {
                    @Override
                    public void subscribe(ObservableEmitter<Integer> emitter) throws Exception {
                        for (int i = 0; ; i++) {    //无限循环发事件
                            emitter.onNext(i);
                        }
                    }
                }).subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Consumer<Integer>() {
                            @Override
                            public void accept(Integer integer) throws Exception {
                                Thread.sleep(2000);
                                Log.d(TAG, "" + integer);
                            }
                        });
                break;
            default:
                break;
        }

    }
}

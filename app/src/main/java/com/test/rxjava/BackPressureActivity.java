package com.test.rxjava;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

public class BackPressureActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "BackPressureActivity";
    private Subscription mSubscription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_back_pressure);

        findViewById(R.id.tv_event_cache_size).setOnClickListener(this);
        findViewById(R.id.tv_sync_subscribe).setOnClickListener(this);
        findViewById(R.id.tv_async_subscribe).setOnClickListener(this);
        findViewById(R.id.tv_avoid_oom).setOnClickListener(this);
        findViewById(R.id.tv_request).setOnClickListener(this);
        findViewById(R.id.tv_ERROR).setOnClickListener(this);
        findViewById(R.id.tv_BUFFER).setOnClickListener(this);
        findViewById(R.id.tv_DROP).setOnClickListener(this);
        findViewById(R.id.tv_LATEST).setOnClickListener(this);
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
            case R.id.tv_avoid_oom:
                /*
                 *异步订阅防止内存溢出
                 * 一 是从数量上进行治理, 减少发送进水缸里的事件：
                 *   1、使用filter操作符，将发送的事件进行过滤，能减缓放进水缸的事件数目。
                 *   2、使用sample操作符，定时取样，规定事件间隔将事件放进水缸中。但这样会丢失大量的事件。
                 * 二 是从速度上进行治理, 减缓事件发送进水缸的速度：
                 *   1、减缓上游放射事件速度，让下游有充分时间将事件冲水缸中去事件处理。
                 * 三 使用Flowable上游和Subscriber下游
                 * */

                //上游变成Flowable
                //创建 Flowable 上游时，增加了一个背压 BackPressure 参数，这个参数是用来选出现上下游流速不均衡的时候应该怎么处理的办法。
                //          使用BackpressureStrategy.ERROR这种方式, 这种方式会在出现上下游流速不均衡的时候直接抛出一个异常,这个异常就是著名的MissingBackpressureException。
                //          使用策略是BackpressureStrategy.BUFFER，可以增大上游缓冲水缸存放事件数。
                //          BackpressureStrategy.DROP：把存不下的事件丢弃。
                //          BackpressureStrategy.LATEST：只保留最新的事件。
                Flowable<Integer> integerFlowable = Flowable.create(new FlowableOnSubscribe<Integer>() {
                    @Override
                    public void subscribe(FlowableEmitter<Integer> emitter) throws Exception {
                        Log.d(TAG, "emit 1");
                        emitter.onNext(1);

                        Log.d(TAG, "emit 2");
                        emitter.onNext(2);

                        Log.d(TAG, "emit 3");
                        emitter.onNext(3);

                        Log.d(TAG, "emit complete");
                        emitter.onComplete();
                    }
                }, BackpressureStrategy.ERROR);

                //下游变成Subscriber
                Subscriber<Integer> subscriber = new Subscriber<Integer>() {
                    @Override
                    public void onSubscribe(Subscription s) {
                        //onSubscribe方法中传给我们的是Subscription, 它是上下游中间的一个开关, 调用Subscription.cancel()可以切断水管
                        //同时Subscription增加了一个void request(long n)方法，用于告诉上游处理能力
                        Log.d(TAG, "onSubscribe");

                        /*
                         * request方法用于告诉上游下游的处理能力。
                         *   在同步订阅中，不声明request，或者声明处理能力的数量少于上游发送事件的数量是，会抛出MissingBackpressureException异常
                         *   在异步订阅中，不声明request数量，上游会继续发送事件到水缸中（存放128个事件）,等待下游取出事件处理；声明了request，下游会取出对应数量的事件进行处理
                         * */
                        //                        s.request(2); //上游发射3个事件，这里声明处理能力为2个，多出来一个事件就会抛出MissingBackpressureException异常
                        //                        s.request(Long.MAX_VALUE);
                        mSubscription = s;
                    }

                    @Override
                    public void onNext(Integer integer) {
                        Log.d(TAG, "onNext: " + integer);

                    }

                    @Override
                    public void onError(Throwable t) {
                        Log.w(TAG, "onError: ", t);
                    }

                    @Override
                    public void onComplete() {
                        Log.d(TAG, "onComplete");
                    }
                };

                integerFlowable
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(subscriber);
                break;
            case R.id.tv_request:
                //保存起来的 Subscription，每点击一次，去水缸一个事件进行处理
                if (mSubscription != null) {
                    mSubscription.request(1);
                }
                break;
            case R.id.tv_ERROR:
                //BackpressureStrategy.ERROR
                //上下游不均衡时直接抛出异常
                Flowable<Integer> integerError = Flowable.create(new FlowableOnSubscribe<Integer>() {
                    @Override
                    public void subscribe(FlowableEmitter<Integer> emitter) throws Exception {

                    }
                }, BackpressureStrategy.ERROR);
                Subscriber<String> subscriberError = new Subscriber<String>() {
                    @Override
                    public void onSubscribe(Subscription s) {

                    }

                    @Override
                    public void onNext(String s) {

                    }

                    @Override
                    public void onError(Throwable t) {

                    }

                    @Override
                    public void onComplete() {

                    }
                };
                integerError
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .map(new Function<Integer, String>() {
                    @Override
                    public String apply(Integer integer) throws Exception {
                        return integer + "";
                    }
                }).subscribe(subscriberError);
                break;
            case R.id.tv_BUFFER:
                //BackpressureStrategy.BUFFER
                //增大上游缓冲水缸存放事件数。
                break;
            case R.id.tv_DROP:
                //BackpressureStrategy.DROP
                // 把存不下的事件丢弃。
                break;
            case R.id.tv_LATEST:
                //BackpressureStrategy.LATEST：
                //只保留最新的事件。
                break;
            default:
                break;
        }

    }
}

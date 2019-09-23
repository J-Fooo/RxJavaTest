package com.test.rxjava;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;

public class TransformActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "TransformActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transform);

        findViewById(R.id.tv_tran_map).setOnClickListener(this);
        findViewById(R.id.tv_tran_flatmap).setOnClickListener(this);
        findViewById(R.id.tv_tran_test).setOnClickListener(this);
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
                        Log.d(TAG,"observer : "+s);
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
                }).flatMap(new Function<Integer, ObservableSource<String>>() {
                    @Override
                    public ObservableSource<String> apply(final Integer integer) throws Exception {
                       // just 操作符，创建一个被观察者并发射事件
                       return Observable.just("observe_2nd : "+integer).delay(10, TimeUnit.MILLISECONDS);
                    }
                }).subscribe(new Consumer<String>() {
                    @Override
                    public void accept(String s) throws Exception {
                        Log.d(TAG,s);
                    }
                });
                break;
            case R.id.tv_tran_test:
                break;
            default:
                break;
        }

    }
}

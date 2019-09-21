package com.test.rxjava;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.test.bean.BaseBean;
import com.test.bean.HomeArticleList;
import com.test.net.Api;
import com.test.net.RetrofitManager;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Retrofit;

public class SchedulerActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "SchedulerActivity";
    private Disposable RequestDisposable;
    private CompositeDisposable mCompositeDisposable;
    private TextView mTvNum;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scheduler);

        mCompositeDisposable = new CompositeDisposable();

        findViewById(R.id.btn_same_thread).setOnClickListener(this);
        findViewById(R.id.btn_change_thread).setOnClickListener(this);
        findViewById(R.id.btn_request_test).setOnClickListener(this);
        mTvNum = findViewById(R.id.tv_num);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_same_thread:
                /*
                 *通常情况下，上下游是工作在同一个线程中（上游在哪个线程发射事件，下游就在哪个线程接收事件）
                 * */
                Observable.create(new ObservableOnSubscribe<Integer>() {
                    @Override
                    public void subscribe(ObservableEmitter<Integer> emitter) throws Exception {
                        Log.d(TAG, "Observable thread: " + Thread.currentThread().getName());
                        emitter.onNext(11);
                    }
                }).subscribe(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer i) throws Exception {
                        Log.d(TAG, i + "");
                        Log.d(TAG, "Consumer thread: " + Thread.currentThread().getName());
                    }
                });
                break;
            case R.id.btn_change_thread:
                /*
                 * 通过线程调度器对上下游线程进行切换
                 * */
                Observable<Integer> observable = Observable.create(new ObservableOnSubscribe<Integer>() {
                    @Override
                    public void subscribe(ObservableEmitter<Integer> emitter) throws Exception {
                        Log.d(TAG, "Observable thread: " + Thread.currentThread().getName());
                        emitter.onNext(13);
                    }
                });

                Consumer<Integer> consumer = new Consumer<Integer>() {
                    @Override
                    public void accept(Integer i) throws Exception {
                        Log.d(TAG, i + "");
                        Log.d(TAG, "Consumer thread: " + Thread.currentThread().getName());
                        mTvNum.setText(String.valueOf(i));
                    }
                };

                // subscribeOn() 指定上游放射事件的线程   observeOn() 指定下游接收事件的线程

                // Schedulers.newThread() 开启一个常规的新线程   AndroidSchedulers.mainThread() 线程为安卓主线程   Schedulers.io() io操作的线程, 通常用于网络,读写文件等io密集型的操作

                Disposable changeSubscribe = observable.subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(consumer);
                mCompositeDisposable.add(changeSubscribe);
                break;
            case R.id.btn_request_test:
                // retrofit 网络请求测试
                Retrofit retrofit = RetrofitManager.getInstance();
                Api api = retrofit.create(Api.class);
                api.getHomeArticleList(0)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Observer<BaseBean<HomeArticleList>>() {
                            @Override
                            public void onSubscribe(Disposable d) {
                                RequestDisposable = d;
                                mCompositeDisposable.add(RequestDisposable);
                            }

                            @Override
                            public void onNext(BaseBean<HomeArticleList> homeArticleListBaseBean) {
                                Log.d(TAG,homeArticleListBaseBean.getData().getSize()+"");
                                mTvNum.setText(String.valueOf(homeArticleListBaseBean.getData().getSize()+""));
                            }

                            @Override
                            public void onError(Throwable e) {
                                Log.d(TAG,e.getMessage());
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        /*
        * 如果在请求的过程中Activity已经退出了, 这个时候如果回到主线程去更新UI, 那么APP肯定就崩溃了
        * 可以使用Disposable开关，在Activity中将这个Disposable 保存起来, 当Activity退出时, 切断它即可
        *
        * 如果有多个Disposable，RxJava中已经内置了一个容器CompositeDisposable,
        * 每当我们得到一个Disposable时就调用CompositeDisposable.add()将它添加到容器中,
        * 在退出的时候, 调用CompositeDisposable.clear() 即可切断所有的水管
        * */
        mCompositeDisposable.clear();
    }
}

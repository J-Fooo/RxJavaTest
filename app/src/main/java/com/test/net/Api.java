package com.test.net;

import com.test.bean.BaseBean;
import com.test.bean.HomeArticleList;

import io.reactivex.Observable;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface Api {
    @GET("article/list/{page}/json")
    Observable<BaseBean<HomeArticleList>> getHomeArticleList(@Path("page") int page);

//    @GET
//    Observable<RegisterResponse> register(@Body RegisterRequest request);



}
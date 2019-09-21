package com.test.net;

import com.test.bean.BaseBean;
import com.test.bean.HomeArticleList;
import com.test.bean.LoginBean;

import io.reactivex.Observable;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface Api {
    @GET("article/list/{page}/json")
    Observable<BaseBean<HomeArticleList>> getHomeArticleList(@Path("page") int page);

    /**
     * 账号注册
     *
     * @param username
     * @param password
     * @param repassword
     * @return
     */
    @POST("user/register")
    @FormUrlEncoded
    Observable<BaseBean<LoginBean>> register(@Field("username") String username, @Field("password") String password, @Field("repassword") String repassword);


    /**
     * 登陆接口
     *
     * @param username 账号
     * @param password 密码
     * @return
     */
    @POST("user/login")
    @FormUrlEncoded
    Observable<BaseBean<LoginBean>> login(@Field("username") String username, @Field("password") String password);
}
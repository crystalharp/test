/*
 * Copyright 2011 Sina.
 *
 * Licensed under the Apache License and Weibo License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.open.weibo.com
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tigerknows.net;


/**
 * Encapsulation a Weibo error, when weibo request can not be implemented successful.
 *
 * @author  ZhangJie (zhangjie2@staff.sina.com.cn)
 */
public class TKException extends Exception {

	private static final long serialVersionUID = 475022994858770424L;
	
	
	/*
	200  成功
    201 操作成功，但是相关POI已失效
    202 服务器成功响应，但数据库插入点评操作失败
    203 服务器成功响应，但数据库更新点评操作失败
    300 session失效
    301 用户已在别处登录
    400 注册/修改手机号时，手机号已被注册
    401 注册/修改昵称时，昵称已被使用
    402 注册/修改手机/重置密码时，验证码错误
    403 手机号不存在
    404 密码错误,或者密码无效（比如空密码）
    405 修改密码时，原始密码错误
    406 无效的手机号（一般是格式错误）
    407 参数非法或缺少参数
    408 缺少参数
    500 服务器错误
    503 服务器短信发送失败
    504 短信长度超过限制
    601 该用户已点评过该POI
    602 该点评不存在
    603 该poi不存在或已失效
    604 匿名用户不能更新点评
    700 本城市没有所请求的发现类型（如小城市没有任何团购信息，ps：客户端应通过发现首页避免该类请求的产生）
    701 发现数据已更新（在列表页请求具有一定偏移量的结果时，服务器切换了数据库，客户端需给出提示“需重新发请求”） 
    */
	private int statusCode = -1;
	
	
	
    public TKException(String msg) {
        super(msg);
    }

    public TKException(Exception cause) {
        super(cause);
    }

    public TKException(String msg, int statusCode) {
        super(msg);
        this.statusCode = statusCode;
    }

    public TKException(String msg, Exception cause) {
        super(msg, cause);
    }

    public TKException(String msg, Exception cause, int statusCode) {
        super(msg, cause);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return this.statusCode;
    }
    
    
	public TKException() {
		super(); 
	}

	public TKException(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
	}

	public TKException(Throwable throwable) {
		super(throwable);
	}

	public TKException(int statusCode) {
		super();
		this.statusCode = statusCode;
	}

	public void setStatusCode(int statusCode) {
		this.statusCode = statusCode;
	}
	
}

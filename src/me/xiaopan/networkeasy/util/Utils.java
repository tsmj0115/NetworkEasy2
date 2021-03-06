package me.xiaopan.networkeasy.util;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import me.xiaopan.networkeasy.Host;
import me.xiaopan.networkeasy.HttpHeaderUtils;
import me.xiaopan.networkeasy.Path;
import me.xiaopan.networkeasy.RequestParams;
import me.xiaopan.networkeasy.headers.ContentType;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpRequestBase;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Utils {
	public static final String DEFAULT_CHARSET = "UTF-8";
	
	/**
	 * 把给定的字符串用给定的字符分割
	 * @param string 给定的字符串
	 * @param ch 给定的字符
	 * @return 分割后的字符串数组
	 */
	public static String[] partition(String string, char ch) {
		ArrayList<String> stringList = new ArrayList<String>();
		char chars[] = string.toCharArray();
		int nextStart = 0;
		for (int w = 0; w < chars.length; w++){
			if (ch == chars[w]) {
				if(w != nextStart){					
					stringList.add(new String(chars,nextStart, w-nextStart));
					nextStart = w+1;
				}else{
					nextStart++;
				}
			}
		}
		if(nextStart < chars.length-1){
			stringList.add(new String(chars,nextStart, chars.length-1-nextStart+1));
		}
		return stringList.toArray(new String[stringList.size()]);
	}
	
	/**
	 * 判断给定的字符串是否不为null且不为空
	 * @param string 给定的字符串
	 * @return 
	 */
	public static boolean isNotNullAndEmpty(String string){
		return string != null && !"".equals(string.trim());
	}
	
	/**
	 * 判断给定的字符串数组中是否全部都不为null且不为空
	 * @param strings 给定的字符串数组
	 * @return 是否全部都不为null且不为空
	 */
	public static boolean isNotNullAndEmpty(String... strings){
		boolean result = true;
		for(String string : strings){
			if(string == null || "".equals(string.trim())){
				result = false;
				break;
			}
		}
		return result;
	}
	
	/**
	 * 获取响应编码，首先会尝试从响应体的Content-Type中获取，如果获取不到的话就返回默认的UTF-8
	 * @param httpResponse
	 * @return
	 */
	public static final String getResponseCharset(HttpResponse httpResponse){
		ContentType contentType = HttpHeaderUtils.getContentType(httpResponse);
		if(contentType != null){
			return contentType.getCharset(DEFAULT_CHARSET);
		}else{
			return DEFAULT_CHARSET;
		}
	}
	
	/**
	 * 获取给定的类所有的父类
	 * @param clas 给定的类
	 * @param isAddCurrentClass 是否将当年类放在最终返回的父类列表的首位
	 * @return 给定的类所有的父类
	 */
	public static List<Class<?>> getSuperClasss(Class<?> sourceClass, boolean isAddCurrentClass){
		List<Class<?>> classList = new ArrayList<Class<?>>();
		Class<?> classs;
		if(isAddCurrentClass){
			classs = sourceClass;
		}else{
			classs = sourceClass.getSuperclass();
		}
		while(classs != null){
			classList.add(classs);
			classs = classs.getSuperclass();
		}
		return classList;
	}
	
	/**
	 * 获取给定类的所有字段
	 * @param sourceClass 给定的类
	 * @param isGetDeclaredField 是否需要获取Declared字段
	 * @param isFromSuperClassGet 是否需要把其父类中的字段也取出
	 * @param isDESCGet 在最终获取的列表里，父类的字段是否需要排在子类的前面。只有需要把其父类中的字段也取出时此参数才有效
	 * @return 给定类的所有字段
	 */
	public static List<Field> getFileds(Class<?> sourceClass, boolean isGetDeclaredField, boolean isFromSuperClassGet, boolean isDESCGet){
		List<Field> fieldList = new ArrayList<Field>();
		//如果需要从父类中获取
		if(isFromSuperClassGet){
			//获取当前类的所有父类
			List<Class<?>> classList = getSuperClasss(sourceClass, true);
			
			//如果是降序获取
			if(isDESCGet){
				for(int w = classList.size()-1; w > -1; w--){
					for(Field field : isGetDeclaredField ? classList.get(w).getDeclaredFields() : classList.get(w).getFields()){
						fieldList.add(field);
					}
				}
			}else{
				for(int w = 0; w < classList.size(); w++){
					for(Field field : isGetDeclaredField ? classList.get(w).getDeclaredFields() : classList.get(w).getFields()){
						fieldList.add(field);
					}
				}
			}
		}else{
			for(Field field : isGetDeclaredField ? sourceClass.getDeclaredFields() : sourceClass.getFields()){
				fieldList.add(field);
			}
		}
		return fieldList;
	}
	
	/**
	 * 将一个请求对象转换为RequestParams对象
	 * @param requestObject 请求对象
	 * @return 
	 */
	public static RequestParams toRequestParams(Object requestObject){
		if(requestObject != null){
			RequestParams requestParams = new RequestParams();
			
			//循环处理所有字段
			String paramValue;
			String paramName;
			Object valueObject;
			Class<?> requestClass = requestObject.getClass();
			for(Field field : getFileds(requestClass, true, true, true)){
				try {
					/* 如果当前字段被标记为需要序列化 */
					if(field.getAnnotation(Expose.class) != null){
						//初始化参数值
						field.setAccessible(true);
						valueObject = field.get(requestObject);
						if(valueObject instanceof Map){
							@SuppressWarnings("unchecked")
							Map<Object, Object> map = (Map<Object, Object>)valueObject;
							for(java.util.Map.Entry<Object, Object> entry : map.entrySet()){
								if(entry.getKey() != null && entry.getValue() != null && isNotNullAndEmpty(entry.getKey().toString(), entry.getValue().toString())){
									//添加请求参数
									requestParams.put(entry.getKey().toString(), entry.getValue().toString());
								}
							}
						}else{
							paramValue = valueObject != null?valueObject.toString():null;
							if(isNotNullAndEmpty(paramValue)){
								//初始化参数名
								SerializedName serializedName = field.getAnnotation(SerializedName.class);
								if(serializedName != null && isNotNullAndEmpty(serializedName.value())){
									paramName = serializedName.value();
								}else{
									paramName = field.getName();
								}
								
								//添加请求参数
								requestParams.put(paramName, paramValue);
							}
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			return requestParams;
		}else{
			return null;
		}
	}
	
	/**
	 * 判断给定的字符串是否以一个特定的字符串开头，忽略大小写
	 * @param sourceString 给定的字符串
	 * @param newString 一个特定的字符串
	 * @return 
	 */
	public static boolean startsWithIgnoreCase(String sourceString, String newString){
		int newLength = newString.length();
		int sourceLength = sourceString.length();
		if(newLength == sourceLength){
			return newString.equalsIgnoreCase(sourceString);
		}else if(newLength < sourceLength){
			char[] newChars = new char[newLength];
			sourceString.getChars(0, newLength, newChars, 0);
			return newString.equalsIgnoreCase(String.valueOf(newChars));
		}else{
			return false;
		}
	}
	
	public static final String getUrlFromRequestObject(String defaultUrl, Object requestObject) throws Exception{
		if(isNotNullAndEmpty(defaultUrl)){
			return defaultUrl;
		}else{
			Class<?> requestClass = requestObject.getClass();
			Host host = requestClass.getAnnotation(Host.class);
			if(host == null || !isNotNullAndEmpty(host.value())){
				throw new Exception(requestClass.getName()+"上没有Host注解");
			}
			//尝试从请求对象中获取路径地址
			Path path = requestClass.getAnnotation(Path.class);
			if(path == null || !isNotNullAndEmpty(path.value())){
				throw new Exception(requestClass.getName()+"上没有Path注解");
			}
			
			return host.value() + (startsWithIgnoreCase(host.value(), "/")?"":"/") + path.value();
		}
	}
	
	public static final String getUrlWithQueryString(String url, RequestParams params) {
        if(params != null) {
            String paramString = params.getParamString();
            if (url.indexOf("?") == -1) {
                url += "?" + paramString;
            } else {
                url += "&" + paramString;
            }
        }
        return url;
    }
    
	public static final HttpEntity paramsToEntity(RequestParams params) {
        return params != null?params.getEntity():null;
    }

	public static final HttpEntityEnclosingRequestBase setEntity(HttpEntityEnclosingRequestBase requestBase, HttpEntity entity, Header[] headers) {
        if(entity != null){
            requestBase.setEntity(entity);
        }
        if(headers != null){
        	requestBase.setHeaders(headers);
        }
        return requestBase;
    }

	public static final HttpEntityEnclosingRequestBase setEntity(HttpEntityEnclosingRequestBase requestBase, HttpEntity entity) {
        if(entity != null){
            requestBase.setEntity(entity);
        }
        return requestBase;
    }
	
	public static final HttpRequestBase setHeaders(HttpRequestBase httpRequest, Header[] headers){
		if(headers != null){
			httpRequest.setHeaders(headers);
		}
		return httpRequest;
	}
}
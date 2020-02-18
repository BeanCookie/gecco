package com.geccocrawler.gecco.spider.render.html;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.net.URLEncoder;
import java.util.*;

import com.geccocrawler.gecco.annotation.ImageGroup;
import com.geccocrawler.gecco.annotation.Video;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.reflections.ReflectionUtils;

import com.geccocrawler.gecco.annotation.Image;
import com.geccocrawler.gecco.downloader.DownloaderContext;
import com.geccocrawler.gecco.request.HttpRequest;
import com.geccocrawler.gecco.response.HttpResponse;
import com.geccocrawler.gecco.spider.SpiderBean;
import com.geccocrawler.gecco.spider.render.FieldRender;
import com.geccocrawler.gecco.spider.render.FieldRenderException;
import com.geccocrawler.gecco.utils.DownloadUrlSourceUtils;

import net.sf.cglib.beans.BeanMap;

/**
 * 渲染@Image属性
 * 
 * @author huchengyi
 *
 */
public class UrlSourceFieldRender implements FieldRender {

	@Override
	@SuppressWarnings("unchecked")
	public void render(HttpRequest request, HttpResponse response, BeanMap beanMap, SpiderBean bean) {
		Map<String, Object> fieldMap = new HashMap<String, Object>();
		Set<Field> imageFields = ReflectionUtils.getAllFields(bean.getClass(), ReflectionUtils.withAnnotation(Image.class));
		Set<Field> videoFields = ReflectionUtils.getAllFields(bean.getClass(), ReflectionUtils.withAnnotation(Video.class));
		Set<Field> urlSourceFields = Sets.union(imageFields, videoFields);
		for (Field urlSourceField : urlSourceFields) {
			Object value = injectUrlSourceField(request, beanMap, bean, urlSourceField);
			if(value != null) {
				fieldMap.put(urlSourceField.getName(), value);
			}
		}
		beanMap.putAll(fieldMap);
	}

	@SuppressWarnings("unchecked")
	private Object injectUrlSourceField(HttpRequest request, BeanMap beanMap, SpiderBean bean, Field field) {
		Object value = beanMap.get(field.getName());
		if(value == null) {
			return null;
		}
		if(value instanceof Collection) {
			Collection<Object> collection = (Collection<Object>)value;
			for(Object item : collection) {
				String url = downloadUrlSource(request, bean, field, item.toString());
				item = url;
			}
			return collection;
		} else {
			return downloadUrlSource(request, bean, field, value.toString());
		}
	}

	private String downloadUrlSource(HttpRequest request, SpiderBean bean, Field field, String imgUrl) {
		if(StringUtils.isEmpty(imgUrl)) {
			return imgUrl;
		}
		String parentPath = null;

		Annotation[] annotations = field.getDeclaredAnnotations();
		for (Annotation annotation : annotations) {
			if (annotation instanceof Image) {
				Image image = field.getAnnotation(Image.class);
				Set<Field> imageGroupFields = ReflectionUtils.getAllFields(bean.getClass(), ReflectionUtils.withAnnotation(ImageGroup.class));
				if (imageGroupFields.size() > 0) {
					Field imageGroupField = imageGroupFields.stream().findFirst().get();
					imageGroupField.setAccessible(true);
					try {
						Object imageGroup = imageGroupField.get(bean);
						if (imageGroup instanceof String) {
							String imageGroupValue = (String) imageGroup;
							if (StringUtils.isNotEmpty(imageGroupValue)) {
								if (imageGroupValue.startsWith("/")) {
									parentPath = image.download() + imageGroupValue;
								} else {
									parentPath = image.download() + "/" + imageGroupValue;
								}
							}
						}
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					}

				} else {
					parentPath = image.download();
				}
			} else if (annotation instanceof Video) {
				Video video = field.getAnnotation(Video.class);
				parentPath = video.download();
			}

		}

		if(StringUtils.isEmpty(parentPath)) {
			return imgUrl;
		}
		HttpResponse subReponse = null;
		try {
			String before =  StringUtils.substringBefore(imgUrl, "?");
			String last =  StringUtils.substringAfter(imgUrl, "?");
			String fileName = StringUtils.substringAfterLast(before, "/");
			if(StringUtils.isNotEmpty(last)) {
				last = URLEncoder.encode(last, "UTF-8");
				imgUrl = before + "?" + last;
			}
			HttpRequest subRequest = request.subRequest(imgUrl);
			subReponse = DownloaderContext.defaultDownload(subRequest);
			return DownloadUrlSourceUtils.download(parentPath, fileName, subReponse.getRaw());
		} catch (Exception ex) {
			//throw new FieldRenderException(field, ex.getMessage(), ex);
			FieldRenderException.log(field, "download image error : " + imgUrl, ex);
			return imgUrl;
		} finally {
			if(subReponse != null) {
				subReponse.close();
			}
		}
	}
}

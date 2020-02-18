package com.geccocrawler.gecco.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.base.Strings;
import com.google.common.io.ByteStreams;
import com.google.common.io.Closer;
import com.google.common.io.Files;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;

import javax.net.ssl.SSLContext;

/**
 * 下载图片到指定目录
 * 
 * @author huchengyi
 *
 */
public class DownloadUrlSourceUtils {
	
	private static Log log = LogFactory.getLog(DownloadUrlSourceUtils.class);

	private static final byte[] MP4_CHECK = new byte[] {0, 0, 0, 1, 0, 0, 0, 98, 117, 100, 116, 97, 0, 0, 0, 90, 109, 101, 116, 97, 0, 0, 0, 0, 0, 0, 0, 33, 104, 100, 108, 114, 0, 0, 0, 0, 0, 0, 0, 0, 109, 100, 105, 114, 97, 112, 112, 108, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 45, 105, 108, 115, 116, 0, 0, 0, 37, -87, 116, 111, 111, 0, 0, 0, 29, 100, 97, 116, 97, 0, 0, 0, 1, 0, 0, 0, 0, 76, 97, 118, 102, 53, 56, 46, 49, 50, 46, 49, 48, 48};

	public static boolean videoIsFull(File file) {
		if (Objects.isNull(file) || !file.exists()) {
			return false;
		}
		try (RandomAccessFile accessFile = new RandomAccessFile(file, "r")) {
			long readIndex = accessFile.length() - 102;
			accessFile.seek(readIndex);
			byte[] last = new byte[102];
			accessFile.read(last);
			return Arrays.equals(MP4_CHECK, last);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return false;
	}

	private static CloseableHttpClient httpClient;

	static {
		Registry<ConnectionSocketFactory> socketFactoryRegistry = null;
		try {
			//构造一个信任所有ssl证书的httpclient
			SSLContext sslContext = SSLContexts.custom().loadTrustMaterial(null, new TrustStrategy() {
				@Override
				public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
					return true;
				}
			}).build();
			SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext);
			socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
					.register("http", PlainConnectionSocketFactory.getSocketFactory())
					.register("https", sslsf)
					.build();
		} catch(Exception ex) {
			socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
					.register("http", PlainConnectionSocketFactory.getSocketFactory())
					.register("https", SSLConnectionSocketFactory.getSocketFactory())
					.build();
		}

		RequestConfig clientConfig = RequestConfig.custom().setRedirectsEnabled(false).build();
		PoolingHttpClientConnectionManager syncConnectionManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
		syncConnectionManager.setMaxTotal(100);
		syncConnectionManager.setDefaultMaxPerRoute(5);
		httpClient = HttpClientBuilder.create()
				.setDefaultRequestConfig(clientConfig)
				.setConnectionManager(syncConnectionManager)
				.build();
	}
	
	/**
	 * 下载图片到指定目录
	 * 
	 * @param parentPath 指定目录
	 * @param url 图片地址
	 * @return 下载文件地址
	 */
	public static String download(String parentPath, String url) {
		if(Strings.isNullOrEmpty(url) || Strings.isNullOrEmpty(parentPath)) {
			return null;
		}
		if(url.length() > 500) {
			return null;
		}
		Closer closer = Closer.create();
		try {
			File sourceDir = new File(parentPath);
			if(!sourceDir.exists()) {
				sourceDir.mkdirs();
			}
			String fileName =  StringUtils.substringBefore(url, "?");
			fileName = StringUtils.substringAfterLast(fileName, "/");
			String fileType = StringUtils.substringAfter(fileName, ".");

			File sourceFile = new File(sourceDir, fileName);
			if (fileType.equals("mp4")) {
				if (!videoIsFull(sourceFile)) {
					sourceFile.deleteOnExit();
				}
			}
			HttpGet request = new HttpGet(url);
			HttpResponse response = httpClient.execute(request);
			InputStream in = closer.register(response.getEntity().getContent());
			Files.write(ByteStreams.toByteArray(in), sourceFile);
			return sourceFile.getAbsolutePath();
		} catch(Exception ex) {
			ex.printStackTrace();
			log.error("image download error :"+url);
			return null;
		} finally {
			try {
				closer.close();
			} catch (IOException e) {
				closer = null;
			}
		}
	}
	
	public static String download(String parentPath, String fileName, InputStream in) {
		Closer closer = Closer.create();
		try {
			File sourceDir = new File(parentPath);
			if(!sourceDir.exists()) {
				sourceDir.mkdirs();
			}
			File sourceFile = new File(sourceDir, fileName);
			Files.write(ByteStreams.toByteArray(in), sourceFile);
			return sourceFile.getAbsolutePath();
		} catch(Exception ex) {
			ex.printStackTrace();
			return null;
		} finally {
			try {
				closer.close();
			} catch (IOException e) {
				closer = null;
			}
		}
	}
}

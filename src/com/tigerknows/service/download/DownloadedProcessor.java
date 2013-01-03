/**
 * 
 */
package com.tigerknows.service.download;

import java.io.File;

import android.content.Context;

/**
 * @author chenming
 *
 */
public interface DownloadedProcessor {
	// 处理用DownloadService下载的文件
	public void process(File file, Context context);
	
}

/**
 * 
 */
package com.tigerknows.service.download;

import java.io.File;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.decarta.android.util.LogWrapper;

/**
 * @author chenming
 *
 */
public class SoftwareDownloadedProcessor implements DownloadedProcessor {

	@Override
	public void process(File file, Context context) {
		LogWrapper.d("chen", "to install");
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setAction(android.content.Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
        context.startActivity(intent);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {

	}

}

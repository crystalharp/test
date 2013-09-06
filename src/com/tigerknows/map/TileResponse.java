/**
 * 
 */
package com.tigerknows.map;

import java.util.List;

import android.graphics.Bitmap;
import com.tigerknows.map.label.*;

/**
 * @author chenming
 *
 */
public class TileResponse {
	public Label[] labels;
	public Bitmap bitmap = null;
	public TileDownload[] lostTileInfos = null;

	public TileResponse(Bitmap bitmap, Label[] labels) {
		this.bitmap = bitmap;
		this.labels = labels;
	}

	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (object == null)
			return false;
		if (object instanceof TileResponse) {
			TileResponse other = (TileResponse) object;
			if (other.lostTileInfos != null && lostTileInfos != null) {
				if (other.lostTileInfos.length == lostTileInfos.length) {
					int i = 0;
					for (TileDownload tileDownload : lostTileInfos) {
						if (!tileDownload.equals(other.lostTileInfos[i++])) {
							return false;
						}
					}
					return true;
				}
			} else if (other.lostTileInfos == null && lostTileInfos == null) {
				return true;
			}
		}
		return false;
	}
}

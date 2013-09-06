/**
 * 
 */
package com.tigerknows.map;
import java.util.*;
import com.decarta.android.util.*;

/**
 * @author chenming
 *
 */
public class Grid {
	public int width;
	public int height;
	public int w;
	public int h;
	public int length;
	public int gridBit;
    public List<List<RectInteger>> gridList;
    
    public Grid(int width, int height, int gridBit) {
    	this.width = width;
    	this.height = height;
    	this.gridBit = gridBit;
    	w = width >> gridBit;
    	if ((width & ((1 << gridBit) - 1)) != 0) {
    		w += 1;
    	}
    	h = height >> gridBit;
    	if ((height & ((1 << gridBit) - 1)) != 0) {
    		h += 1;
    	}
    	length = w * h;
    	gridList = new ArrayList<List<RectInteger>>(length);
    	for (int i = 0; i < length; ++ i) {
    		List<RectInteger> row = new ArrayList<RectInteger>();
    		gridList.add(row);
    	}
    }

    public void clean() {
    	List<RectInteger> grid;
    	for (int i = 0; i < length; ++i) {
    		grid = gridList.get(i);
    		grid.clear();
    	}
    }
    
    public void addRect(RectInteger rect) {
    	int leftGrid = rect.left > 0 ? (rect.left >> gridBit) : 0;
    	int right = rect.right >> gridBit;
    	int rightGrid = right < w ? right : (w - 1);
    	int topGrid = rect.top > 0 ? (rect.top >> gridBit) : 0;
    	int bottom = rect.bottom >> gridBit;
    	int bottomGrid = bottom < h ? bottom : (h - 1);
    	int rowStartIdx, idx, i, j;
    	List<RectInteger> grid;
    	for (i = topGrid; i <= bottomGrid; ++i) {
    		rowStartIdx = i * w;
    		for (j = leftGrid; j <= rightGrid; ++j) {
    			idx = rowStartIdx + j;
    			grid = gridList.get(idx);
    			grid.add(rect);
    		}
    	}
    	
    }
    
    public RectInteger gridIntersect(RectInteger rect) {
    	int leftGrid = rect.left > 0 ? (rect.left >> gridBit) : 0;
    	int right = rect.right >> gridBit;
    	int rightGrid = right < w ? right : (w - 1);
    	int topGrid = rect.top > 0 ? (rect.top >> gridBit) : 0;
    	int bottom = rect.bottom >> gridBit;
    	int bottomGrid = bottom < h ? bottom : (h - 1);
    	int rowStartIdx, idx, i, j, k;
    	List<RectInteger> grid;
    	RectInteger addedRect;
    	for (i = topGrid; i <= bottomGrid; ++i) {
    		rowStartIdx = i * w;
    		for (j = leftGrid; j <= rightGrid; ++j) {
    			idx = rowStartIdx + j;
    			grid = gridList.get(idx);
    			int len = grid.size();
    			for (k = 0; k < len; ++k) {
    				addedRect = grid.get(k);
    				if (!(rect.left > addedRect.right || rect.right < addedRect.left ||
                          rect.top > addedRect.bottom || rect.bottom < addedRect.top)) {
    					return addedRect;
    				}
    			}
    		}
    	}
    	return null;
    }

    public boolean isInterSectRect(RectInteger rect) {
    	int leftGrid = rect.left > 0 ? (rect.left >> gridBit) : 0;
    	int right = rect.right >> gridBit;
    	int rightGrid = right < w ? right : (w - 1);
    	int topGrid = rect.top > 0 ? (rect.top >> gridBit) : 0;
    	int bottom = rect.bottom >> gridBit;
    	int bottomGrid = bottom < h ? bottom : (h - 1);
    	int rowStartIdx, idx, i, j, k;
    	RectInteger addedRect;
    	List<RectInteger> grid;
    	for (i = topGrid; i <= bottomGrid; ++i) {
    		rowStartIdx = i * w;
    		for (j = leftGrid; j <= rightGrid; ++j) {
    			idx = rowStartIdx + j;
    			grid = gridList.get(idx);
    			int len = grid.size();
    			for (k = 0; k < len; ++k) {
    				addedRect = grid.get(k);
    				if (!(rect.left > addedRect.right || rect.right < addedRect.left ||
                          rect.top > addedRect.bottom || rect.bottom < addedRect.top)) {
    					return true;
    				}
    			}
    		}
    	}
    	return false;
    }
    
    public boolean isContainRect(RectInteger rect) {
    	if (rect.left < 0 || rect.right >= width || rect.top < 0 || rect.bottom >= height) {
    		return false;
    	}
    	return true;
    }
    
    public boolean isContainPoint(XYFloat point) {
    	if (point.x < 0 || point.x >= width || point.y < 0 || point.y >= height) {
    		return false;
    	}
    	return true;
    }

    public boolean isContainPoint(float x, float y) {
    	if (x < 0 || x >= width || y < 0 || y >= height) {
    		return false;
    	}
    	return true;
    }

    public boolean isRectOutBound(RectInteger rect) {
    	return rect.right < 0 || rect.left >= width || rect.bottom < 0 || rect.top >= height;
    }
}

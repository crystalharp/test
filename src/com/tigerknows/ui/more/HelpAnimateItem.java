package com.tigerknows.ui.more;

public class HelpAnimateItem {
	
	String tag;
	int x;
	int y;
	float startX;
	float startY;
	float endX;
	float endY;
	float scrollStartX;
	float scrollEndX;
	int resId;
	boolean visible;
	
	int width = 0;
	int height = 0;
	
	public HelpAnimateItem(String tag, float startX, float startY, float endX, float endY,
			float scrollStartX, float scrollEndX, int resId) {
		super();
		this.tag = tag;
		this.x = 0;
		this.y = 0;
		this.startX = startX;
		this.startY = startY;
		this.endX = endX;
		this.endY = endY;
		this.scrollStartX = scrollStartX;
		this.scrollEndX = scrollEndX;
		this.resId = resId;
	}
	
	

}

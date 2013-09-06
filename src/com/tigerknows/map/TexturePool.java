/**
 * 
 */
package com.tigerknows.map;

import static android.opengl.GLES10.glDeleteTextures;

import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Iterator;

import com.decarta.android.util.LogWrapper;

/**
 * @author chenming
 *
 */
public class TexturePool {
	private static class Node {
		int textureRef = 0;
		Node pre = null;
		Node next = null;
		
		public Node(int textureRef) {
			this.textureRef = textureRef;
		}
	}
	private int capacity;
	private int size;
	private Node header;
	private Node unusedHeader;
	private HashMap<Integer, TexturePool.Node> nodeMap;

	public TexturePool(int capacity) {
		this.capacity = capacity;
		size = 0;
		nodeMap = new HashMap<Integer, TexturePool.Node>();
		header = new Node(0);
		header.pre = header;
		header.next = header;
		unusedHeader = new Node(0);
		unusedHeader.pre = unusedHeader;
		unusedHeader.next = unusedHeader;
	}
	
	public int getAvailabelTexture() {
	    if (unusedHeader.next != unusedHeader) {
	        //从unused表取出
	        Node cur = unusedHeader.next;
	        unusedHeader.next = cur.next;
	        cur.next.pre = unusedHeader;
	        //插入inuse表
	        header.next.pre = cur;
	        cur.next = header.next;
	        header.next = cur;
	        cur.pre = header;
	        return cur.textureRef;
	    }
	    else {
	        return 0;
	    }
	}
	
	public boolean putTexture(int textureRef) {
	    if (size >= capacity) {
	        return false;
	    }
	    else {
	        Node cur = new Node(textureRef);
	        nodeMap.put(textureRef, cur);
	        header.next.pre = cur;
	        cur.next = header.next;
	        header.next = cur;
	        cur.pre = header;
	        ++size;
	        return true;
	    }
	}
	
	public boolean returnTexture(int textureRef) {
	    Node cur = nodeMap.get(textureRef);
	    if (cur != null) {
	    	LogWrapper.i("TexturePool","return texture:"+textureRef);
	        cur.pre.next = cur.next;
	        cur.next.pre = cur.pre;
	        unusedHeader.next.pre = cur;
	        cur.next = unusedHeader.next;
	        unusedHeader.next = cur;
	        cur.pre = unusedHeader;
	        return true;
	    }
	    else {
	    	return false;
	    }
	}
	
	public boolean removeTexture(int textureRef) {
		Node cur = nodeMap.get(textureRef);
		if(cur != null) {
			LogWrapper.i("TexturePool","removeTexture texture:"+textureRef);
			cur.pre.next = cur.next;
			cur.next.pre = cur.pre;
			nodeMap.remove(textureRef);
			IntBuffer textureRefBuf=IntBuffer.allocate(1);
			textureRefBuf.clear();
			textureRefBuf.put(0, textureRef);
			textureRefBuf.position(0);
			glDeleteTextures(1, textureRefBuf);
			return true;
		}
		else {
			return false;
		}
	}
	
	public void clean() {
		LogWrapper.i("TexturePool","clean TexturePool");
		header.next = header;
		header.pre = header;
		unusedHeader.next = unusedHeader;
		unusedHeader.pre = unusedHeader;
		size = 0;
        Iterator<Integer> iterator = nodeMap.keySet().iterator();
		IntBuffer textureRefBuf=IntBuffer.allocate(1);
        while(iterator.hasNext()){
            int textureRef = iterator.next();
            if(textureRef != 0) {
    			textureRefBuf.clear();
    			textureRefBuf.put(0, textureRef);
    			textureRefBuf.position(0);
    			glDeleteTextures(1, textureRefBuf);
            }
        }
	}
	
}

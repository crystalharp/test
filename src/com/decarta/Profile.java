package com.decarta;


/**
 * Global static Profile class 
 * This class is mainly for performance debugging.
 */
public class Profile {
	public final static int SIZE=100;
	
	public static int tiles_network_index=-1;
	public static long[] tiles_network=new long[SIZE];
	public static int tiles_network_size=0;
	
	public static int get_tile_buffer_index=-1;
	public static long[] get_tile_buffer_db=new long[SIZE];
	public static int get_tile_buffer_size=0;
	
	public static int decode_byte_array_index=-1;
	public static long[] decode_byte_array=new long[SIZE];
	public static int decode_byte_array_size=0;
	
	public static int draw_index=-1;
	public static long[] draw=new long[SIZE];
	public static int draw_size=0;
	
	public static int rotate_index=-1;
	public static long[] rotate=new long[SIZE];
	public static int rotate_size=0;
	
	public static void tilesNetworkInc(long time){
		tiles_network_index=(tiles_network_index+1)%SIZE;
		tiles_network[tiles_network_index]=time;
		tiles_network_size++;
		if(tiles_network_size>SIZE) tiles_network_size=SIZE;
	}
	
	public static void getTileBufferInc(long time){
		get_tile_buffer_index=(get_tile_buffer_index+1)%SIZE;
		get_tile_buffer_db[get_tile_buffer_index]=time;
		get_tile_buffer_size++;
		if(get_tile_buffer_size>SIZE) get_tile_buffer_size=SIZE;
	}
		
	public static void decodeByteArrayInc(long time){
		decode_byte_array_index=(decode_byte_array_index+1)%SIZE;
		decode_byte_array[decode_byte_array_index]=time;
		decode_byte_array_size++;
		if(decode_byte_array_size>SIZE) decode_byte_array_size=SIZE;
	}
		
	public static void drawInc(long time){
		draw_index=(draw_index+1)%SIZE;
		draw[draw_index]=time;
		draw_size++;
		if(draw_size>SIZE) draw_size=SIZE;
	}
		
	public static void rotateInc(long time){
		rotate_index=(rotate_index+1)%SIZE;
		rotate[rotate_index]=time;
		rotate_size++;
		if(rotate_size>SIZE) rotate_size=SIZE;
	}
	
	public static long avg(long[] array, int index, int size){
		if(size==0) return 0;
		long sum=0;
		for(int i=0;i<size;i++){
			sum+=(array[(index-i+SIZE)%SIZE]);
		}
		return sum/size;
	}
	
	public static long avgTilesNetwork(){
		return avg(tiles_network,tiles_network_index,tiles_network_size);
	}
	
	public static long avgGetTileBuffer(){
		return avg(get_tile_buffer_db,get_tile_buffer_index,get_tile_buffer_size);
	}
	
	public static long avgDecodeByteArray(){
		return avg(decode_byte_array, decode_byte_array_index,decode_byte_array_size);
	}
	
	public static long avgDraw(){
		return avg(draw,draw_index,draw_size);
	}
	
	public static long avgRotate(){
		return avg(rotate,rotate_index,rotate_size);
	}
		
	public static void reset(){
		tiles_network_index=-1;
		tiles_network_size=0;
		
		get_tile_buffer_index=-1;
		get_tile_buffer_size=0;
		
		decode_byte_array_index=-1;
		decode_byte_array_size=0;
		
		draw_index=-1;
		draw_size=0;
		
		rotate_index=-1;
		rotate_size=0;
	}
}

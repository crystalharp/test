package com.tigerknows.model;

public class CategoryProperty {
	private int ID;
	private String name;
	private int linearLayoutVisibility;
	private int[] operationType;
	private String[] buttonText;
	
	public static final int NUM_OF_SUBBUTTONS = 9;
	public static final int[] LINEAR_ARRAY = {4,2,1};
	
	public static final int OP_INVISIBLE = -2;
	public static final int OP_HIDE = 0;
	public static final int OP_SEARCH = 1;
	public static final int OP_MORE = 23;
	public static final int OP_TUANGOU = 2;
	public static final int OP_HOTEL = 65537;
	public static final int OP_ZHANLAN = 14;
	public static final int OP_YANCHU = 13;
	public static final int OP_DIANYING = 4;
	public static final int OP_SUBWAY = 5;
	public static final int OP_DISH = 24;
	public CategoryProperty(int id, int llyVisibility){
		this.ID = id;
		this.linearLayoutVisibility = llyVisibility;
		this.operationType = new int[NUM_OF_SUBBUTTONS];
		for(int i=0; i<LINEAR_ARRAY.length; i++){
			if((LINEAR_ARRAY[i] & llyVisibility) == 0){
				for(int j=0; j<LINEAR_ARRAY.length; j++){
					this.operationType[i*LINEAR_ARRAY.length + j] = OP_HIDE;
				}
			}else{
				for(int j=0; j<LINEAR_ARRAY.length; j++){
					this.operationType[i*LINEAR_ARRAY.length + j] = OP_SEARCH;
				}
			}
			this.operationType[8] = OP_MORE;
		}
	}
	
	public int getID(){
		return ID;
	}
	
	public String getName(){
		return name;
	}
	
	public void setName(String name){
		this.name = name;
	}
	
	public int getLlyVisibility(){
		return linearLayoutVisibility;
	}
	
	public int getOperationType(int index){
		if(index >= 0 && index < NUM_OF_SUBBUTTONS){
			return operationType[index];
		}else{
			return -1;
		}
	}
	
	public String getButtonText(int index){
		if(index >= 0 && index < NUM_OF_SUBBUTTONS){
			return buttonText[index];
		}else{
			return "";
		}
	}
	
	public void setOperationType(int index, int value){
		if(index >= 0 && index < 9){
			operationType[index] = value;
		}
	}
	
	public void setButtonText(String[] source){
		this.buttonText = new String[NUM_OF_SUBBUTTONS];
		for(int i=0,j=0; j < source.length && i < NUM_OF_SUBBUTTONS; i++){
			if(operationType[i] > OP_HIDE){
				this.buttonText[i] = source[j];
				j++;
			}else{
				this.buttonText[i] = "";
			}
		}
	}
}
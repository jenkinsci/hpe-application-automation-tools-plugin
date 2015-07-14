package com.ge.applications.automation.testStepRetriever;


public class TestSetFolder {
	private int id;
	private int parentId;
	private String name;
	
	public TestSetFolder(String name, int id, int parentId) {
		this.name = name;
		this.id = id;
		this.parentId = parentId;
	}
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public int getParentId() {
		return parentId;
	}
	public void setParentId(int parentId) {
		this.parentId = parentId;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	
}

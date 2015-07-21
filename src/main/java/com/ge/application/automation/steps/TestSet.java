package com.ge.application.automation.steps;

/**
 * Representation of Test Set or Test Set Folder.
 * ParentId is ALM's id for the parent folder.
 * 
 * @author Tyler Hoffman
 */
public class TestSet {
	private int id;
	private int parentId;
	private String name;
	
	public TestSet(String name, int id) {
		this(name, id, -1);
	}
	
	public TestSet(String name, int id, int parentId) {
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

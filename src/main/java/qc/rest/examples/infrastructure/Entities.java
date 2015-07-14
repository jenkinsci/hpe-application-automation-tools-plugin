package qc.rest.examples.infrastructure;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = { "entities"})
@XmlRootElement(name = "Entities")
public class Entities {
	@XmlElement(name = "Entity", required = true)
	protected List<Entity> entities;
	
	public Entities() {
	}
	
	public Entities(Entities original) {
		entities = new ArrayList<Entity>(original.entities);
	}
	
	public List<Entity> getEntities() {
		if (entities == null) {
			entities = new ArrayList<Entity>();
		}
		return entities;
	}
	
	public void setEntities(List<Entity> entities) {
		this.entities = entities;
	}
	
}

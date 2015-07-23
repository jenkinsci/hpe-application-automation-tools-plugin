package com.ge.application.automation.steps;

import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

public class MarshallingUtility {
	
	/**
	 * Marshal an object to a String
	 * @param objectClass
	 * @param object Object to marshal
	 * @return Xml representation of object
	 * @throws JAXBException
	 */
	public static <T> String marshal(T object) throws JAXBException {
		StringWriter stringWriter = new StringWriter();
		JAXBContext context = JAXBContext.newInstance(object.getClass());
		Marshaller marshaller = context.createMarshaller();
		marshaller.marshal(object, stringWriter);
		return stringWriter.toString();
	}
	
	/**
	 * Unmarshal xml to an object
	 * @param objectClass Object type
	 * @param xml Xml to unmarshal
	 * @return Returns the object the xml represents
	 * @throws JAXBException
	 */
	public static <T> T unmarshal(Class<T> objectClass, String xml) throws JAXBException {
		JAXBContext context = JAXBContext.newInstance(objectClass);
		Unmarshaller unmarshaller = context.createUnmarshaller();
		return (T)unmarshaller.unmarshal(new StreamSource(new StringReader(xml)));
	}
	
}

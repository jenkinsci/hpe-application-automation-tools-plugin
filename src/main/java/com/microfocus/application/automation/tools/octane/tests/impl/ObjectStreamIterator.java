/*
 *
 *  Certain versions of software and/or documents (“Material”) accessible here may contain branding from
 *  Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.  As of September 1, 2017,
 *  the Material is now offered by Micro Focus, a separately owned and operated company.  Any reference to the HP
 *  and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE
 *  marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * © Copyright 2012-2018 Micro Focus or one of its affiliates.
 *
 * The only warranties for products and services of Micro Focus and its affiliates
 * and licensors (“Micro Focus”) are set forth in the express warranty statements
 * accompanying such products and services. Nothing herein should be construed as
 * constituting an additional warranty. Micro Focus shall not be liable for technical
 * or editorial errors or omissions contained herein.
 * The information contained herein is subject to change without notice.
 * ___________________________________________________________________
 *
 */

package com.microfocus.application.automation.tools.octane.tests.impl;

import hudson.FilePath;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class ObjectStreamIterator<E> implements Iterator<E> {
	private static Logger logger = LogManager.getLogger(ObjectStreamIterator.class);

	private MyObjectInputStream ois;
	private E next;

	public ObjectStreamIterator(FilePath filePath) throws IOException, InterruptedException {
		ois = new MyObjectInputStream(new BufferedInputStream(filePath.read()));
	}

	@Override
	public boolean hasNext() {
		if (next != null) {
			return true;
		}
		try {
			next = (E) ois.readObject();
			return true;
		} catch (Exception e) {
			ois.close();
			return false;
		}
	}

	@Override
	public E next() {
		if (hasNext()) {
			E value = next;
			next = null;
			return value;
		} else {
			throw new NoSuchElementException();
		}
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

	private class MyObjectInputStream extends ObjectInputStream {

		private MyObjectInputStream(InputStream in) throws IOException {
			super(in);
		}

		@Override
		public void close() {
			try {
				super.close();
			} catch (IOException ioe) {
				logger.error("Failed to close the stream", ioe); // NON-NLS
			}
		}
	}
}

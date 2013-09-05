package com._37coins.test;

import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;

import junit.framework.Assert;

import org.junit.Before;
import org.mockito.MockitoAnnotations;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SnsTestHelper {
	
	private final SnsTestHelper self;
	
	public SnsTestHelper(){
		self = this;
	}

	@Before
	public void initMocks() {
		MockitoAnnotations.initMocks(this);
	}


	public Map<String, Object> objectToMap(Object object)
			throws IOException {
		final ObjectMapper om = new ObjectMapper();
		String objectString = om.writeValueAsString(object);
		Map<String, Object> actual = om.readValue(objectString,
				new TypeReference<HashMap<String, Object>>() {
				});
		return actual;
	}

	public void validate(String responseString, Object object) throws Exception {
		final ObjectMapper om = new ObjectMapper();
		final Map<String, Object> expected = om.readValue(responseString,
				new TypeReference<HashMap<String, Object>>() {
				});
		Map<String, Object> actual = objectToMap(object);
		int rv = -1;
		rv = compare(actual, expected);
		if (rv != 0) {
			try {
				System.out.println("expectation failed, received: "
						+ om.writeValueAsString(object));
			} catch (JsonProcessingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		Assert.assertEquals(rv,0);
	}

	@SuppressWarnings("rawtypes")
	private final Comparator stringFallbackComparator = new Comparator() {
		@SuppressWarnings("unchecked")
		public int compare(Object o1, Object o2) {
			if (!(o1 instanceof Comparable))
				o1 = o1.toString();
			if (!(o2 instanceof Comparable))
				o2 = o2.toString();
			return ((Comparable) o1).compareTo(o2);
		}
	};

	@SuppressWarnings("rawtypes")
	private final Comparator mapFallbackComparator = new Comparator() {
		@SuppressWarnings("unchecked")
		public int compare(Object o1, Object o2) {
			Map<String, Object> m1 = null;
			Map<String, Object> m2 = null;
			int rv = 0;
			if (!(o1 instanceof Comparable) || !(o2 instanceof Comparable)) {
				try {
					m1 = objectToMap(o1);
					m2 = objectToMap(o2);
					rv = self.compare(m1, m2);
				} catch (IOException e) {
					e.printStackTrace();
				}
				return rv;
			} else {
				return ((Comparable) o1).compareTo(o2);
			}
		}
	};

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public int compare(Map m1, Map m2) throws JsonProcessingException {
		ObjectMapper om = new ObjectMapper();
		TreeSet s1 = new TreeSet(stringFallbackComparator);
		s1.addAll(m1.keySet());
		TreeSet s2 = new TreeSet(stringFallbackComparator);
		s2.addAll(m2.keySet());
		Iterator i1 = s1.iterator();
		Iterator i2 = s2.iterator();
		int i;
		while (i1.hasNext() && i2.hasNext()) {
			Object k1 = i1.next();
			Object k2 = i2.next();
			if (0 != (i = stringFallbackComparator.compare(k1, k2))) {
				return i;
			}
			if (0 != (i = mapFallbackComparator.compare(m1.get(k1), m2.get(k2)))) {
				System.out.println("COMPARISSON ERROR: "
						+ om.writeValueAsString(m1.get(k1))
						+ " different from "
						+ om.writeValueAsString(m2.get(k2)));
				return i;
			}
		}
		if (i1.hasNext())
			return 1;
		if (i2.hasNext())
			return -1;
		return 0;
	}

}

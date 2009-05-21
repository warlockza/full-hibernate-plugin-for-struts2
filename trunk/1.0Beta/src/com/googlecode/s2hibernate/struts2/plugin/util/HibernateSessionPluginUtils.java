package com.googlecode.s2hibernate.struts2.plugin.util;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;

public class HibernateSessionPluginUtils {

	public static List<Field> getFieldsFromAction(Object action) {
		List<Field> fields = new ArrayList<Field>();
		Class clazz = action.getClass();
		do {
			Field[] fieldsArray = clazz.getDeclaredFields();
			CollectionUtils.addAll(fields, fieldsArray);
			clazz=clazz.getSuperclass();
		} while (!clazz.equals(Object.class));
		return fields;
	}
	
	public static String wildcardToRegex(String wildcard){
        StringBuffer s = new StringBuffer(wildcard.length());
        s.append('^');
        for (int i = 0, is = wildcard.length(); i < is; i++) {
            char c = wildcard.charAt(i);
            switch(c) {
                case '*':
                    s.append(".*");
                    break;
                case '?':
                    s.append(".");
                    break;
                    // escape special regexp-characters
                case '(': case ')': case '[': case ']': case '$':
                case '^': case '.': case '{': case '}': case '|':
                case '\\':
                    s.append("\\");
                    s.append(c);
                    break;
                default:
                    s.append(c);
                    break;
            }
        }
        s.append('$');
        return(s.toString());
    }

}
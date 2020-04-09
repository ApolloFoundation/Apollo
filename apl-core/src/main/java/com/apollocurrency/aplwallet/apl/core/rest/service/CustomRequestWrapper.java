package com.apollocurrency.aplwallet.apl.core.rest.service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomRequestWrapper extends HttpServletRequestWrapper {

    private HttpServletRequest wrapped;

    private Map<String, String[]> parameterMap;

    public CustomRequestWrapper(HttpServletRequest wrapped) {
        super(wrapped);
        this.wrapped = wrapped;
    }

    public void addParameter(String name, String value) {
        if (parameterMap == null) {
            parameterMap = new HashMap<>();
            parameterMap.putAll(wrapped.getParameterMap());
        }
        String[] values = parameterMap.get(name);
        if (values == null) {
            values = new String[0];
        }
        List<String> list = new ArrayList<>(values.length + 1);
        list.addAll(Arrays.asList(values));
        list.add(value);
        parameterMap.put(name, list.toArray(new String[0]));
    }

    @Override
    public String getParameter(String name) {
        if (parameterMap == null) {
            return wrapped.getParameter(name);
        }

        String[] strings = parameterMap.get(name);
        if (strings != null) {
            return strings[0];
        }
        return null;
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        if (parameterMap == null) {
            return wrapped.getParameterMap();
        }

        return Collections.unmodifiableMap(parameterMap);
    }

    @Override
    public Enumeration<String> getParameterNames() {
        if (parameterMap == null) {
            return wrapped.getParameterNames();
        }

        return Collections.enumeration(parameterMap.keySet());
    }

    @Override
    public String[] getParameterValues(String name) {
        if (parameterMap == null) {
            return wrapped.getParameterValues(name);
        }
        return parameterMap.get(name);
    }

}
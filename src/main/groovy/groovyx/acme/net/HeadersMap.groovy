/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package groovyx.acme.net;

import java.util.Map;

/** The map that keeps the order and keys of the original map but on get ignores case
*/

@groovy.transform.CompileStatic
public class HeadersMap implements Map<String,Object> {
	Map<String,String> keyMap;
	@Delegate LinkedHashMap<String,Object> delegate;

	private HeadersMap(Map<String,Object> m){
		int msize = m==null?10:m.size()+5;
		this.keyMap = new HashMap(msize);
		this.delegate = new LinkedHashMap(msize);
		if(m!=null)this.putAll(m);
	}

	public static HeadersMap cast(Map<String,Object> m){
		if(m instanceof HeadersMap)return (HeadersMap)m;
		return new HeadersMap(m);
	}

	public String first(Object key){
		def v = delegate.get(realKey(key,0));
		if(v instanceof List)return v?v.get(0) as String:null;
		return v as String;
	}

	public Object each(Closure c){
        for (Map.Entry<String, Object> entry : delegate.entrySet()) {
        	if(entry.getValue() instanceof List){
        		for(Object value: (List)entry.getValue()){
		           	c.call(entry.getKey(),value as String);
        		}
        	}else{
	           	c.call(entry.getKey(),entry.getValue() as String);
        	}
        }
	}

	@Override
	public void clear(){
		delegate.clear();
		keyMap.clear();
	}

	@Override
	public boolean containsKey(Object key){
		return delegate.containsKey(realKey(key,0));
	}

	@Override
	public Object get(Object key){
		return delegate.get(realKey(key,0));
	}

	@Override
	public Object put(String key, Object value){
		return delegate.put(realKey(key,1), value);
	}

	@Override
	public void putAll(Map<String,Object> m){
		m.each{ k,v-> put(k,v) }
	}

	@Override
	public Object remove(Object key){
		return delegate.remove(realKey(key,-1));
	}

	private String realKey(Object _key, int action){
		//action: -1:remove, 0:nothing, 1:add
		if(_key==null)return null;
		String key = _key as String;
		String lkey = key.toLowerCase();
		String okey = keyMap.get(lkey);
		if(okey==null){
			okey = key;
			if(action>0)keyMap.put(lkey,okey);
		}else{
			if(action<0)keyMap.remove(lkey);
		}
		return okey;
	}

}

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

import groovy.json.JsonOutput;
import groovy.transform.Memoized;

import java.security.KeyStore;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.KeyManager;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLContext;
import java.security.SecureRandom;
import javax.net.ssl.HttpsURLConnection;
import java.util.regex.Matcher;
/**
 * simple http client for groovy
 * by dlukyanov@ukr.net 
 */
@groovy.transform.CompileStatic
public class AcmeHTTP{
    //default response handler
    /** The default receiver that detects and applies *_RECEIVER by content-type response header. 
     * For content type '* /json' uses JSON_RECEIVER, for '* /xml' uses XML_RECEIVER, otherwise TEXT_RECEIVER
     */
    public static Closure DEFAULT_RECEIVER = {InputStream instr,Map ctx->
        Map response       = (Map)ctx.response;
        String contentType = response?.contentType;
        Closure receiver   = TEXT_RECEIVER; //default receiver
        if(contentType){
            if( contentType.indexOf("/json")>0 ){ //TODO
                receiver = JSON_RECEIVER;
            }else if( contentType.indexOf('/xml')>0 ){
                receiver = XML_RECEIVER;
            }
        }
        ctx.receiver = receiver; //to be able to check what receiver was used
        return receiver(instr,ctx);
    }

    /** Receiver to get response as a text with encoding defined in ctx.
     * Stores parsed text (String) as `response.body`
     */
    public static Closure TEXT_RECEIVER = {InputStream instr,Map ctx->
        return instr.getText( (String)((Map)ctx.response).encoding );
    }
    
    /** Receiver to get response as json. groovy.json.JsonSlurper() used to parse incoming stream. 
     * Encoding could be defined through ctx.encoding.
     * Stores parsed json object as `response.body`
     */
    public static Closure JSON_RECEIVER = {BufferedInputStream instr, Map ctx-> 
    	instr.mark(1);
    	if(instr.read()==-1)return null; //no content
    	instr.reset();
        return new groovy.json.JsonSlurper().parse(instr,(String)((Map)ctx.response).encoding);
    }
    
    /** Receiver to get response as xml. groovy.util.XmlParser() used to parse incoming stream. 
     * Stores parsed xml (groovy.util.Node) object as `response.body`
     */
    public static Closure XML_RECEIVER = {InputStream instr, Map ctx-> 
    	instr.mark(1);
    	if(instr.read()==-1)return null; //no content
    	instr.reset();
        return new groovy.util.XmlParser().parse(instr);
    }
    
    /** Creates receiver that transfers incoming stream into the file. Stores created `java.io.File` object as `response.body` */
    public static Closure FILE_RECEIVER(File f){
        return { InputStream instr, Map ctx-> 
            f<<instr;
            return f;
        }
    }
    public static class Builder{
    	Map<String,Object> base;
    	Builder(Map<String,Object> base){
    		this.base = ( base==null ? new LinkedHashMap() : new LinkedHashMap(base) );
    	}
        /** sets/replaces current value in multivalue map. */
    	private void multimapSet(String mapname, String key, Object value){
    		Map<String,Object> map = (Map)base.get(mapname);
    		if(map==null)map = new LinkedHashMap();
   			map.put(key, value);
    		base.put(mapname, map);
    	}
        /** adds another http header into headers map. note that there could be several values for one header */
    	private void multimapAdd(String mapname, String key, Object value){
    		Map<String,Object> map = (Map)base.get(mapname);
    		if(map==null)map = new LinkedHashMap();
    		Object oldValue = map.get(key);
    		if(oldValue instanceof List){
    			map.put(key, ((List)oldValue)+value); //groovy merges two lists into a new list, or if value is not a list it will be added into a list
    		}else if(oldValue==null){
    			map.put(key, value);
    		}else{
    			map.put(key, [oldValue,value]);
    		}
    		base.put(mapname, map);
    	}

    	/** string where to send request. could be a base url because it extended with path & query before call. */
    	public Builder setUrl(String v){
    		base.put("url", v);
    		return this;
    	}
        /** additional url part that added to url before sending request.*/
    	public Builder setPath(String v){
    		base.put("path", v);
    		return this;
    	}
        /** query Map - parameters to append to url like: <code>?key1=value1&key2=...</code>. the value of map could be list to pass several values for one parameter */
    	public Builder setQuery(Map<String,Object> v){
    		base.put("query", v);
    		return this;
    	}
    	public Map<String,Object> getQuery(){
    		return (Map<String,Object>) base.get("query");
    	}
        /** replaces parameter value in the query map */
    	public Builder setQuery(String key, Object value){
    		multimapSet("query",key,value);
    		return this;
    	}
        /** add another one parameter into the query map. value could be a list or exact value to add to query map.*/
    	public Builder addQuery(String key, Object value){
    		multimapAdd("query",key,value);
    		return this;
    	}
        /** fully replaces current headers map with new value */
    	public Builder setHeaders(Map<String,Object> v){
    		base.put("headers", v);
    		return this;
    	}
        /** fully replaces current headers map with new value */
    	public Map<String,Object> getHeaders(){
    		return (Map<String,Object>) base.get("headers");
    	}
        /** sets/replaces current value in http headers map. note that there could be several values for one header. value could be exact value or a list of values. */
    	public Builder setHeader(String key, Object value){
    		multimapSet("headers",key,value);
    		return this;
    	}
        /** adds another http header into headers map. note that there could be several values for one header */
    	public Builder addHeader(String key, Object value){
    		multimapAdd("headers",key,value);
    		return this;
    	}
        /** define body as input stream. will be closed at the end. */
    	public Builder setBody(InputStream v){
    		base.put("body", v);
    		return this;
    	}
        /** define body as char sequence (string) */
    	public Builder setBody(CharSequence v){
    		base.put("body", v);
    		return this;
    	}
        /** define body as writable (for example result of Template.make()) */
    	public Builder setBody(Writable v){
    		base.put("body", v);
    		return this;
    	}
        /** define body as map. could be used for `json` and `x-www-form-urlencoded` context types */
    	public Builder setBody(Map v){
    		base.put("body", v);
    		return this;
    	}
        /** define body as groovy Closure(OutputStream,Map context). the closure should write data into output stream when called. the `context` parameter is a reference to all request parameters. */
    	public Builder setBody(Closure v){
    		base.put("body", v);
    		return this;
    	}
        /** define default encoding for request/response */
    	public Builder setEncoding(String v){
    		base.put("encoding", v);
    		return this;
    	}
        /** a Closure(URLConnection con,Map context) that wil be called just after connection established to initialize it in an own way. 
         * `con` is a connection that you could initialize. `context` - all request parameters 
         */
    	public Builder setConnector(Closure v){
    		base.put("connector", v);
    		return this;
    	}
        /** a Closure(InputStream inStream,Map context) that wil be called to receive/transform the response stream. 
         * `inStream` is a response stream to read. `context` - all request/response parameters. 
         * Response http headers are already parsed when closure called and are available in `context.response.headers` map.
         * There are different pre-defined receivers. Default: {@link #DEFAULT_RECEIVER}. Available: {@link #JSON_RECEIVER}, {@link #XML_RECEIVER}, {@link #TEXT_RECEIVER}, {@link #FILE_RECEIVER(java.io.File)}
         */
    	public Builder setReceiver(Closure v){
    		base.put("receiver", v);
    		return this;
    	}
        /**  whether HTTP redirects (requests with response code 3xx) should be automatically followed by this request */
    	public Builder setFollowRedirects(boolean v){
    		base.put("followRedirects", v as Boolean);
    		return this;
    	}
        /**  javax.net.ssl.SSLContext to establish https connection. if not defined standard java ssl-context used. */
    	public Builder setSsl(javax.net.ssl.SSLContext v){
    		base.put("ssl", v);
    		return this;
    	}
        /**  set groovy expression that should return javax.net.ssl.SSLContext. for some specific cases when we need to define it in some config files. */
    	public Builder setSsl(String groovyExpression){
    		base.put("ssl", groovyExpression);
    		return this;
    	}
        /**  the Closure(Map context) that should return ssl context based on request parameters */
    	public Builder setSsl(Closure v){
    		base.put("ssl", v);
    		return this;
    	}
    	/** makes a clone of builder, so it cold be used to append new context parameters */
    	public Builder clone(){
    		Map map;
    		Builder clone = new Builder(this.base);
    		map = (Map)clone.base.get("headers");
    		if(map) clone.base.put("headers", new LinkedHashMap(map));
    		map = (Map)clone.base.get("query");
    		if(map) clone.base.put("query", new LinkedHashMap(map));
    		return clone;
    	}

    	/*=========================================== SEND methods ====================================*/

    	/* main send method. merges parameters defined in this builder with new onces from closure. context parameters rewrites from current builder */
        public Map<String,Object> send(String method, Closure ctx=null)throws IOException{
        	Builder clone = this.clone();
			if(method)clone.base.put("method", method);
			if(ctx) {
	        	ctx.setDelegate(clone);
		        ctx.setResolveStrategy(Closure.DELEGATE_FIRST);
    	    	ctx.call(clone);
    	    }
			return AcmeHTTP.send(clone.base);
        }

        public Map<String,Object> get    (Closure ctx=null)   throws IOException { return send("GET",ctx); }
        public Map<String,Object> put    (Closure ctx=null)   throws IOException { return send("PUT",ctx); }
        public Map<String,Object> post   (Closure ctx=null)   throws IOException { return send("POST",ctx); }
        public Map<String,Object> delete (Closure ctx=null)   throws IOException { return send("DELETE",ctx); }
        public Map<String,Object> head   (Closure ctx=null)   throws IOException { return send("HEAD",ctx); }

    }

    public static Builder builder(Map<String,Object> base=null){
    	return new Builder(base);
    }

    public static Builder builder(Builder base){
    	return base.clone();
    }

    public static Builder builder(Closure base){
    	Builder b = new Builder(null);
		if(base) {
        	base.setDelegate(b);
	        base.setResolveStrategy(Closure.DELEGATE_FIRST);
	    	base.call(b);
	    }
		return b;
    }

    /** Sends request using http method 'GET'. See {@link #send(Map&lt;String,Object&gt;)} for parameter details. */
    public static Map<String,Object> get(Map<String,Object> ctx)throws IOException{
        ctx.put('method','GET');
        return send(ctx);
    }
    
    /** Sends request using http method 'HEAD'. See {@link #send(Map&lt;String,Object&gt;)} for parameter details. */
    public static Map<String,Object> head(Map<String,Object> ctx)throws IOException{
        ctx.put('method','HEAD');
        return send(ctx);
    }
    
    /** Sends request using http method 'POST'. See {@link #send(Map&lt;String,Object&gt;)} for parameter details. */
    public static Map<String,Object> post(Map<String,Object> ctx)throws IOException{
        ctx.put('method','POST');
        return send(ctx);
    }
    
    /** Sends request using http method 'PUT'. See {@link #send(Map&lt;String,Object&gt;)} for parameter details. */
    public static Map<String,Object> put(Map<String,Object> ctx)throws IOException{
        ctx.put('method','PUT');
        return send(ctx);
    }
    
    /** Sends request using http method 'DELETE'. See {@link #send(Map&lt;String,Object&gt;)} for parameter details. */
    public static Map<String,Object> delete(Map<String,Object> ctx)throws IOException{
        ctx.put('method','DELETE');
        return send(ctx);
    }

    /**
     * @param url string where to send request. hould be a base url.
     * @param path additional url part that added to url before sending request.
     * @param query Map<String,String> parameters to append to url
     * @param method http method to be used in request. standard methods: GET, POST, PUT, DELETE, HEAD
     * @param headers key-value Map<String,String> with headers that should be sent with request
     * @param body request body/data to send to url (InputStream, CharSequence, groovy.lang.Writable, Closure{outStream,ctx->...}, or Map for json and x-www-form-urlencoded context types)
     * @param encoding encoding name to use to send/receive data - default UTF-8
     * @param connector Closure{connection, ctx->...} that will be called to init connection after header, method, ssl were set but before connection established.
     * @param receiver Closure{inStream, ctx->...} that will be called to receive data from server. Default: {@link #DEFAULT_RECEIVER}. Available: {@link #JSON_RECEIVER}, {@link #XML_RECEIVER}, {@link #TEXT_RECEIVER}, {@link #FILE_RECEIVER(java.io.File)}.
     * @param followRedirects Boolean - whether HTTP redirects (requests with response code 3xx) should be automatically followed by this request.
     * @param ssl {@link javax.net.ssl.SSLContext} or String that evaluates the {@link javax.net.ssl.SSLContext}. example: send( url:..., ssl: "HTTP.getKeystoreSSLContext('./keystore.jks', 'testpass')" )
     * @return the modified ctx Map with new property `response`:
     * <table> 
     * <tr><td>response.code</td><td>http response code. for example '200' as Integer</td><tr> 
     * <tr><td>response.message</td><td>http response message. for example for code '404' it will be 'Not Found'</td><tr> 
     * <tr><td>response.contentType</td><td>http `content-type` header. returned by URLConnection.getContentType()</td><tr> 
     * <tr><td>response.headers</td><td>http response headers Map<String,List<String>> returned by URLConnection.getHeaderFields()</td><tr> 
     * <tr><td>response.body</td><td>response body returned by a *_RECEIVER. For example {@link #TEXT_RECEIVER} returns body as text, and {@link #FILE_RECEIVER(java.io.File)} returns body as java.io.File object</td><tr> 
     * </table> 
     */
    public static Map<String,Object> send(Map<String,Object> ctx)throws IOException{
        String             url      = ctx.url;
        String             path     = ctx.path;
        Map<String,Object> headers  = HeadersMap.cast((Map<String,Object>)ctx.headers);
        String             method   = ctx.method;
        Object             body     = ctx.body;
        String             encoding = ctx.encoding?:"UTF-8";
        Closure            connector= (Closure)ctx.connector;
        Closure            receiver = (Closure)ctx.receiver?:DEFAULT_RECEIVER;
        Map<String,Object> query    = (Map<String,Object>)ctx.query;
        Object             sslCtxObj= ctx.ssl;
        Boolean            followRedirects= ctx.followRedirects as Boolean;
        
        //copy context and set default values
        ctx = [:] + ctx;
        ctx.encoding = encoding;
        ctx.headers = headers;
        String contentType="";
        
        if(path){
            url+=path;
        }
        if(query){
        	def e2s = {String k,Object v-> k + "=" + URLEncoder.encode(v as String ?: "", encoding) }
            url+=(url.indexOf('?')>0 ? "&" : "?")+query.collectMany{k,v-> v instanceof List ? v.collect{ e2s(k,it) } : [ e2s(k,v) ] }.join('&');
        }
        
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        if(sslCtxObj!=null && connection instanceof HttpsURLConnection){
            SSLContext         sslCtx   = null;
            if(sslCtxObj instanceof SSLContext){
                sslCtx = (SSLContext)sslCtxObj;
            }else if(sslCtxObj instanceof CharSequence){
                //assume this is a groovy code to get ssl context
                sslCtx = evaluateSSLContext((CharSequence)sslCtxObj);
            }else if(sslCtxObj instanceof Closure){
                sslCtx = ((Closure)sslCtxObj).call(ctx);
            }else{
                throw new IllegalArgumentException("Unsupported ssl parameter ${sslCtxObj.getClass()}")
            }
            if(sslCtx)((HttpsURLConnection)connection).setSSLSocketFactory(sslCtx.getSocketFactory());
        }
        
        connection.setDoOutput(true);
        connection.setRequestMethod(method);
        if ( headers ) {
            //add headers
            headers.each{String k, String v-> 
                connection.addRequestProperty(k, v);
                if("content-type".equals(k.toLowerCase()))contentType=v;
            }
        }
        if( followRedirects!=null ){
        	connection.setFollowRedirects(followRedirects);
        }
        if( connector!=null )connector.call(connection,ctx);
        
        if(body!=null){
            //write body
            OutputStream out = connection.getOutputStream();
            if( body instanceof Closure ){
                ((Closure)body).call(out, ctx);
            }else if(body instanceof InputStream){
                out << (InputStream)body;
            }else if(body instanceof Writable){
                out.withWriter((String)ctx.encoding){
                    ((Writable)body).writeTo(it);
                }
            }else if(body instanceof Map){
                if( contentType =~ "(?i)[^/]+/json" ) {
                    out.withWriter((String)ctx.encoding){
                        it.append( JsonOutput.toJson((Map)body) );
                    }
                } else if( contentType =~ "(?i)[^/]+/x-www-form-urlencoded" ) {
                    out.withWriter((String)ctx.encoding) {
                        it.append( ((Map)body).collect{k,v-> ""+k+"="+URLEncoder.encode((String)v,encoding) }.join('&') )
                    }
                } else {
                    throw new IOException("Map body type supported only for */json of */x-www-form-urlencoded content-type");
                }
            }else if(body instanceof CharSequence){
                out.withWriter((String)ctx.encoding){
                    it.append((CharSequence)body);
                }
            }else{
                throw new IOException("Unsupported body type: "+body.getClass());
            }
            out.flush();
            out.close();
            out=null;
        }
        
        Map response     = [:];
        ctx.response     = response;
        response.code    = connection.getResponseCode();
        response.message = connection.getResponseMessage();
        response.contentType = connection.getContentType();
        response.headers = HeadersMap.cast((Map<String,Object>)connection.getHeaderFields());
        //eval encoding 
        Matcher em       = response.contentType =~ /(?i) charset=([\d\w\-]*)/ ;
        response.encoding=  em.find() ? em.group(1) : encoding;
        
        InputStream instr = null;
        
        if( ((int)response.code)>=400 ){
            try{
                instr = connection.getErrorStream();
            }catch(Exception ei){}
        }else{
            try{
                instr = connection.getInputStream();
            }catch(java.io.IOException ei){
                throw new IOException("fail to open InputStream for http code "+response.code+":"+ei);
            }
        }
        
        if(instr!=null) {
            instr = new BufferedInputStream(instr);
            response.body = receiver(instr,ctx);
            instr.close();
            instr=null;
        }
        return ctx;
    }
    /**
     * Creates keystore ssl context based on private key.
     * @param protocol used for SSLContext creation. Valid parameters: "TLS", "TLSv1", "TLSv1.1", "TLSv1.2", "SSL", "SSLv2", "SSLv3". 
     * @param keystorePath path to keystore ( usually keystore.jks file )
     * @param keystorePass password to keystore
     * @param keystoreType by default "JKS". Used for java.security.KeyStore.getInstance(java.lang.String)
     * @param keyPass password for the private key - by default and if null then equals to `keystorePass`
     */
    @Memoized
    public static SSLContext getKeystoreSSLContext(String protocol, String keystorePath, String keystorePass, String keystoreType="JKS", String keyPass = null){
        if(keyPass == null) keyPass=keystorePass;
        KeyStore clientStore = KeyStore.getInstance(keystoreType);
        clientStore.load(new File( keystorePath ).newInputStream(), keystorePass.toCharArray());
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(clientStore, keyPass.toCharArray());
        KeyManager[] kms = kmf.getKeyManagers();
        //init TrustCerts
        TrustManager[] trustCerts = new TrustManager[1];
        trustCerts[0] = new X509TrustManager() {
            public void checkClientTrusted( final X509Certificate[] chain, final String authType ) { }
            public void checkServerTrusted( final X509Certificate[] chain, final String authType ) { }
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        }
        SSLContext sslContext = SSLContext.getInstance(protocol);
        sslContext.init(kms, trustCerts, new SecureRandom());
        return sslContext;
    }
    
    /**
     * Creates naive ssl context that trusts to all. Prints to System.err the warning if used...
     * @param protocol used for SSLContext creation. 
     *   Valid parameters: "SSL", "SSLv2", "SSLv3", "TLS", "TLSv1", "TLSv1.1", "TLSv1.2". 
     *   For more information see {@link javax.net.ssl.SSLContext#getInstance(java.lang.String)}
     */
    @Memoized
    public static SSLContext getNaiveSSLContext(String protocol="TLS"){
        System.err.println("HTTP.getNaiveSSLContext() used. Must be disabled on prod!");
        KeyManager[] kms = new KeyManager[0];
        TrustManager[] trustCerts = new TrustManager[1];                
        trustCerts[0] = new X509TrustManager() {
            public void checkClientTrusted( final X509Certificate[] chain, final String authType ) { }
            public void checkServerTrusted( final X509Certificate[] chain, final String authType ) { }
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }    
        }
        SSLContext sslContext = SSLContext.getInstance(protocol);
        sslContext.init(null, trustCerts, new SecureRandom());
        return sslContext;
    }
    
    /**
     * Creates default ssl context but with forced protocol.
     * @param protocol used for SSLContext creation. 
     *   Valid parameters: "SSL", "SSLv2", "SSLv3", "TLS", "TLSv1", "TLSv1.1", "TLSv1.2". 
     *   For more information see {@link javax.net.ssl.SSLContext#getInstance(java.lang.String)}
     */
    public static SSLContext getSSLContext(String protocol="TLS"){
        SSLContext sslContext = SSLContext.getInstance(protocol);
        sslContext.init(null, null, null);
        return sslContext;
    }
    
    /**
     * evaluates code that should return SSLContext.
     */
    @Memoized
    public static SSLContext evaluateSSLContext(CharSequence code) {
        Object ssl = new GroovyShell( AcmeHTTP.class.getClassLoader() ).evaluate( code as String );
        return (SSLContext) ssl;
    }
}

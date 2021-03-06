# AcmeHTTP Examples




### simple http get 	
```groovy
import groovyx.acme.net.AcmeHTTP

def t = AcmeHTTP.get(url:"http://httpbin.org/get")
assert t.response.code==200
assert t.response.contentType =~ "/json"
assert t.response.headers.first('content-type') =~ "/json"
assert t.response.body instanceof Map
```

### use response validator-assertion
```groovy
import groovyx.acme.net.AcmeHTTP

try{
    def t = AcmeHTTP.get(
        url:"http://httpbin.org/get",
        assertTrue: {ctx-> ctx.response.code==201} //actual result 200
    )
}catch(e){
    println e
    assert ( e instanceof AcmeHTTP.AcmeHTTPError )
}

```

### simple https get and query parameters
```groovy
import groovyx.acme.net.AcmeHTTP

//this will build url: https://httpbin.org/get?a=1&a=2&b=3
def t = AcmeHTTP.get(
    url:"https://httpbin.org/get",
    query: [a: ['1','2'], b:'3'],
    headers: [h1:[1,2],h2:3],
    assertTrue: {ctx-> ctx.response.code==200}
)
//assert t.response.code==200 //moved to `assertTrue` parameter
assert t.response.contentType =~ "/json"
assert t.response.body.args.a == ["1", "2"]
assert t.response.body.args.b == "3"
```

### simple https get and query parameters using bulder
```groovy
import groovyx.acme.net.AcmeHTTP

def base = AcmeHTTP.builder().setUrl('https://httpbin.org').addHeader('h1',1).addHeader('h1',2)
def t = base.get{
	path = '/get'
	headers.h2 = 3
	query = [a: ['1','2'], b:'3']
} 
assert t.response.code==200
assert t.response.contentType =~ "/json"
assert t.response.body.args.a == ["1", "2"]
assert t.response.body.args.b == "3"
assert t.response.body.headers.H1 == "1,2"
assert t.response.body.headers.H2 == "3"
```

### xml post
```groovy
import groovyx.acme.net.AcmeHTTP

def xml = new groovy.util.NodeBuilder().
    aaa(a0:'a00'){
        bbb(b0:'b00', "some text")
    }
assert xml instanceof groovy.util.Node
//send post request
def t = AcmeHTTP.post(
    url:   "https://httpbin.org/post",
    query: [p1: "11&22", p2:"33 44"],
    //define body as closure, so it will be called to serialize data to output stream 
    body: {outStream,ctx->
        groovy.xml.XmlUtil.serialize(xml,outStream)
    },
    //define content type xml
    headers:[
        "content-type":"application/xml"
    ],
    //connector - a closure called just before connection. here you can manipulate with connection
    connector:{connection, ctx->
    	connection.setReadTimeout(10000) //just for example set read timeout to 10 seconds
    }
)
assert t.response.code==200
//the https://httpbin.org/post service always returns json object 
//with `args` attribute evaluated from url query and `data` attribute with body as string 
assert t.response.contentType =~ "/json"
assert t.response.body.args.p1 =="11&22"
assert t.response.body.args.p2 =="33 44"
//in the response `data` tag there should be originally posted xml
assert t.response.body.data == groovy.xml.XmlUtil.serialize(xml)
```

### json post
```groovy
import groovyx.acme.net.AcmeHTTP

def t = AcmeHTTP.post(
    url:   "https://httpbin.org/post",
    //define payload as maps/arrays
    body: [
      arr_int: [1,2,3,4,5,9],
      str: "hello",
    ],
    //let's specify content-type = json, so JsonOutput.toJson() will be applied
    headers:[
        "content-type":"application/json"
    ],
    encoding: "UTF-8",  //force to use utf-8 encoding for sending/receiving data
)
assert t.response.code==200
assert t.response.contentType =~ "/json"
assert t.response.body instanceof Map
//the https://httpbin.org/post service returns json object with json key that contains posted body
//so let's take it and validate
def data = t.response.body.json
assert data.arr_int==[1,2,3,4,5,9]
assert data.str=="hello"
```


### get with body
not normal but possible
```groovy
import groovyx.acme.net.AcmeHTTP

def t = AcmeHTTP.get(
    url:   "https://httpbin.org/post",
    //define payload as maps/arrays
    body: [
      arr_int: [1,2,3,4,5,9],
      str: "hello",
    ],
    //let's specify content-type = json, so JsonOutput.toJson() will be applied
    headers:[
        "content-type":"application/json"
    ],
    encoding: "UTF-8",  //force to use utf-8 encoding for sending/receiving data
)
assert t.response.code==200
assert t.response.contentType =~ "/json"
assert t.response.body instanceof Map
println t.response
//the https://httpbin.org/post service returns json object with json key that contains posted body
//so let's take it and validate
def data = t.response.body.json
assert data.arr_int==[1,2,3,4,5,9]
assert data.str=="hello"
```

### headers and trust all https (naive ssl context)
```groovy
import groovyx.acme.net.AcmeHTTP

def t = AcmeHTTP.get(
    url: "https://httpbin.org/headers",
    ssl: AcmeHTTP.getNaiveSSLContext(),
    "headers": ["Header1":"value1", "Header2":"value2"]
)
assert t.response.code==200
assert t.response.contentType =~ "/json"
assert t.response.body instanceof Map // expecting response like:  { "headers": { "Header1":"value1", ... } }
assert t.response.body.headers.findAll{it.key =~ /^Header\d+/ }.sort() == ["Header1":"value1", "Header2":"value2"]
```

### post a stream and receive into file
```groovy
import groovyx.acme.net.AcmeHTTP

def tmpFile = new File("./build/Copy_of_README.md")
def t = AcmeHTTP.post(
    url: "https://httpbin.org/post",
    body: new File("./README.md").newInputStream(),
    receiver: AcmeHTTP.FILE_RECEIVER( tmpFile )
)
assert t.response.code==200
```

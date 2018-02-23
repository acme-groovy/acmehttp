# AcmeHTTP Examples



### simple http get 	
```groovy
import groovyx.acme.net.AcmeHTTP

def t = AcmeHTTP.get(url:"http://time.jsontest.com/")
assert t.response.code==200
assert t.response.contentType =~ "/json"
assert t.response.body instanceof Map // expecting response like:  {"time": "01:07:59 PM", "date": "05-31-2017"}
assert t.response.body.time
assert t.response.body.date
```

### simple https get
```groovy
import groovyx.acme.net.AcmeHTTP

def t = AcmeHTTP.get(
    url:"https://api.github.com/"
)
assert t.response.code==200
assert t.response.contentType =~ "/json"
assert t.response.body instanceof Map // expecting response like:  { "current_user_url": "https://api.github.com/user", ... }
assert t.response.body.current_user_url == "https://api.github.com/user"
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
    ]
)
assert t.response.code==200
//the https://httpbin.org/post service always returns json object 
//with `args` attribute evaluated from url query and `data` attribute with body as string 
assert t.response.contentType =~ "/json"
assert t.response.body.args.p1 =="11&22"
assert t.response.body.args.p2 =="33 44"
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

### trust all https (naive ssl context)
```groovy
import groovyx.acme.net.AcmeHTTP

def t = AcmeHTTP.get(
    url: "https://api.github.com/",
    ssl: AcmeHTTP.getNaiveSSLContext()
)
assert t.response.code==200
assert t.response.contentType =~ "/json"
assert t.response.body instanceof Map // expecting response like:  { "current_user_url": "https://api.github.com/user", ... }
assert t.response.body.current_user_url == "https://api.github.com/user"
```

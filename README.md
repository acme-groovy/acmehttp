# AcmeHTTP
simplified plain http client optimized for groovy language

## grab it

```groovy
@Grab(group='acme.groovy', module='acmehttp', version='20180301', transitive=false)
import groovyx.acme.net.AcmeHTTP
```

## examples

[EXAMPLES.md](EXAMPLES.md)

## AcmeHTTP class

### methods: get, post, put, delete, head, send

All methods has the same parameter map:

| key | description |
|-----------------|-------------------------------------------|
| url | string where to send request |
| query | `Map` parameters to append to url |
| method | http method to be used in request. all methods except: `send` substitutes this key with corresponting one |
| headers | key-value `Map` with headers that should be sent with request. `value` could be a `List` of strings for multiple headers with the same name. |
| body | request body/data to send to url. could be: `InputStream`, `CharSequence`, `groovy.lang.Writable`, `Closure{outStream,ctx->...}`, or `Map` for `json` and `x-www-form-urlencoded` context types |
| encoding | encoding name to use to send/receive data - default UTF-8 |
| connector | `Closure{connection,ctx->...}` that will be called just before connection established. |
| receiver | `Closure{inputStream, ctx->...}` that will be called to receive data from server. default: `AcmeHTTP.DEFAULT_RECEIVER`. available: `AcmeHTTP.JSON_RECEIVER`, `AcmeHTTP.XML_RECEIVER`, `AcmeHTTP.TEXT_RECEIVER`, `AcmeHTTP.FILE_RECEIVER(java.io.File)`, or custom closure. |
| followRedirects | Boolean - whether we should follow redirects (requests with response code 3xx) |
| ssl | `javax.net.ssl.SSLContext` to be used to establish ssl/tls connection. available: `AcmeHTTP.getNaiveSSLContext()` |

**result**

As result every method returns a modified input parameter map with additional key `response`:

| key | desctiption |
|----------------------|----------------------------|
| `response.code` | http response code. for example '200' as Integer |
| `response.message` | http response message. for example for code `'404'` it will be `'Not Found'` |
| `response.contentType` | http `content-type` header. returned by `URLConnection.getContentType()` |
| `response.headers` | http response headers Map returned by `URLConnection.getHeaderFields()` |
| `response.body` | response body returned by a `*_RECEIVER`. For example `TEXT_RECEIVER` returns body as text, and `FILE_RECEIVER(java.io.File)` returns body as `java.io.File` object |

### receivers

| name | description |
|------|-------------|
| `DEFAULT_RECEIVER` | The default receiver that detects and applies `*_RECEIVER` by content-type response header. For content type `*/json` uses `JSON_RECEIVER`, for `*/xml` uses `XML_RECEIVER`, otherwise `TEXT_RECEIVER` |
| `JSON_RECEIVER` | Receiver to get response as json. `groovy.json.JsonSlurper()` used to parse incoming stream. Encoding could be defined through `encoding` parameter. Stores parsed json object as `response.body` |
| `TEXT_RECEIVER` | Receiver to get response as a text with encoding defined in parameters. Stores parsed text (`String`) as `response.body` |
| `XML_RECEIVER` | Receiver to get response as xml. `groovy.util.XmlParser()` used to parse incoming stream. Stores parsed xml (`groovy.util.Node`) object as `response.body` |
| `FILE_RECEIVER(java.io.File f)` | Creates receiver that transfers incoming stream into the file. Stores created `java.io.File` object as `response.body` |

**example for FILE_RECEIVER**

```groovy
def m = AcmeHTTP.get(
    url: "https://httpbin.org/get",
    receiver: AcmeHTTP.FILE_RECEIVER( new File("out.json") )
  )
assert m.response.code==200
//the `m.response.body` now contains File("out.json") that contains received response
//let's print out the content of the file
println m.response.body.getText("UTF-8")
```

### ssl context

helpers to get ssl context

* `getKeystoreSSLContext(String sslProtocol, String keystorePath, String keystorePass, String keystoreType="JKS", String keyPass = null)`
  
  Creates keystore ssl context based on private key
  
  Parameters:
  
  * `sslProtocol` valid values: "SSL", "SSLv2", "SSLv3", "TLS", "TLSv1", "TLSv1.1", "TLSv1.2". see more information: [SSLContext.html.getInstance()](https://docs.oracle.com/javase/8/docs/api/javax/net/ssl/SSLContext.html#getInstance-java.lang.String-)
  * `keystorePath` path to keystore ( usually keystore.jks file )
  * `keystorePass` password to keystore
  * `keystoreType` by default `"JKS"`. Used for `java.security.KeyStore.getInstance(java.lang.String)`
  * `keyPass` password for the private key - by default and if null then equals to `keystorePass`
* `getNaiveSSLContext(String sslProtocol="TLS")`
  
  returns naive (trust all) ssl context. don't use it on prod.




check this page for more [examples](EXAMPLES.md)

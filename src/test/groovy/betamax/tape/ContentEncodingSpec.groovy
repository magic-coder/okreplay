package betamax.tape

import betamax.tape.yaml.YamlTape

import org.apache.http.entity.ByteArrayEntity
import org.apache.http.message.BasicHttpResponse
import betamax.encoding.*
import static java.net.HttpURLConnection.HTTP_OK
import static org.apache.http.HttpHeaders.*
import static org.apache.http.HttpVersion.HTTP_1_1
import spock.lang.*
import betamax.util.message.BasicRequest
import betamax.proxy.httpcore.HttpCoreResponseImpl

@Issue("https://github.com/robfletcher/betamax/issues/3")
class ContentEncodingSpec extends Specification {

	@Unroll("a #encoding encoded response body is stored as plain text in a tape file")
	def "an encoded response body is stored as plain text in a tape file"() {
		given:
		def request = new BasicRequest("GET", "http://robfletcher.github.com/betamax")
		request.addHeader(ACCEPT_ENCODING, encoding)

		def entity = new ByteArrayEntity(encoder.encode("O HAI!"))
		entity.setContentType("text/plain")
		entity.setContentEncoding(encoding)

		def response = new BasicHttpResponse(HTTP_1_1, HTTP_OK, "OK")
		response.addHeader(CONTENT_TYPE, "text/plain")
		response.addHeader(CONTENT_ENCODING, encoding)
		response.entity = entity

		and:
		def tape = new YamlTape(name: "encoded response tape")
		tape.record(request, new HttpCoreResponseImpl(response))

		when:
		def writer = new StringWriter()
		tape.writeTo(writer)

		then:
		def yaml = writer.toString()
		yaml.contains("body: O HAI!")

		where:
		encoding  | encoder
		"gzip"    | new GzipEncoder()
		"deflate" | new DeflateEncoder()
	}

	@Unroll("response body is encoded when played from tape and a #encoding content-encoding header is present")
	def "response body is encoded when played from tape and a content-encoding header is present"() {
		given:
		def yaml = """\
!tape
name: encoded response tape
interactions:
- recorded: 2011-08-24T20:38:40.000Z
  request:
    protocol: HTTP/1.1
    method: GET
    uri: http://robfletcher.github.com/betamax
    headers: {Accept-Encoding: $encoding}
  response:
    protocol: HTTP/1.1
    status: 200
    headers: {Content-Type: text/plain, Content-Language: en-GB, Content-Encoding: $encoding}
    body: O HAI!
"""
		def tape = YamlTape.readFrom(new StringReader(yaml))

		and:
		def response = new BasicHttpResponse(HTTP_1_1, 200, "OK")

		when:
		tape.seek(new BasicRequest("GET", "http://robfletcher.github.com/betamax"))
		tape.play(new HttpCoreResponseImpl(response))

		then:
		response.getFirstHeader(CONTENT_ENCODING).value == encoding
		encoder.decode(response.entity.content) == "O HAI!"

		where:
		encoding  | encoder
		"gzip"    | new GzipEncoder()
		"deflate" | new DeflateEncoder()
	}

}
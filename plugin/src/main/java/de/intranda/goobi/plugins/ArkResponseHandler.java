package de.intranda.goobi.plugins;

import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.util.EntityUtils;

public class ArkResponseHandler implements ResponseHandler<String> {


	@Override
	public String handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
		int status = response.getStatusLine().getStatusCode();
		HttpEntity entity = response.getEntity();
		if (status >= 200 && status < 300) {

			if (entity == null) {
				throw new ClientProtocolException(status + ": reason-> " + "no response provided");
			} else {
				return EntityUtils.toString(entity, Charset.forName("utf-8"));
			}
		} else {
			if (status == 400) {
				entity = response.getEntity();
				throw new ClientProtocolException(status + ": reason-> " + (entity == null ? "no response body received"
						: EntityUtils.toString(entity, Charset.forName("utf-8"))));
			} else
				throw new ClientProtocolException(status + ": reason-> " + " unhandeld error");
		}
	}
}

package net.yupol.transmissionremote.app.transport.request;

import android.util.Log;

import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpContent;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.octo.android.robospice.request.googlehttpclient.GoogleHttpClientSpiceRequest;

import net.yupol.transmissionremote.app.server.Server;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.StringReader;
import java.util.Locale;

import javax.annotation.Nonnull;

public abstract class Request<RESULT> extends GoogleHttpClientSpiceRequest<RESULT> {

    private static final String TAG = Request.class.getSimpleName();

    private static final String HEADER_SESSION_ID = "X-Transmission-Session-Id";

    private static final JsonObjectParser JSON_PARSER = new JsonObjectParser.Builder(new JacksonFactory()).build();

    private Server server;
    private String responseSessionId;

    private int statusCode = -1;

    public Request(Class<RESULT> resultClass) {
        super(resultClass);
    }

    public void setServer(@Nonnull Server server) {
        this.server = server;
    }

    public Server getServer() {
        return server;
    }

    public int getResponseStatusCode() {
        return statusCode;
    }

    public String getResponseSessionId() {
        return responseSessionId;
    }

    @Override
    public RESULT loadDataFromNetwork() throws Exception {
        if (server == null) {
            throw new IllegalStateException("Server must be set before executing");
        }

        String url = String.format(Locale.ROOT, "%s://%s:%d/%s",
                server.useHttps() ? "https" : "http",
                server.getHost(),
                server.getPort(),
                server.getRpcUrl());

        HttpRequestFactory requestFactory = getHttpRequestFactory();

        String body = Optional.fromNullable(createBody()).or("");
        HttpContent content = new ByteArrayContent("application/json", body.getBytes());

        HttpRequest request = requestFactory.buildPostRequest(new GenericUrl(url), content);
        request.setThrowExceptionOnExecuteError(false);
        request.setNumberOfRetries(0);

        HttpHeaders headers = new HttpHeaders()
                .setContentType("json")
                .set(HEADER_SESSION_ID, Strings.emptyToNull(server.getLastSessionId()));
        if (server.isAuthenticationEnabled()) {
            headers.setBasicAuthentication(server.getUserName(), server.getPassword());
        }
        request.setHeaders(headers);
        request.setParser(JSON_PARSER);

        HttpResponse response = request.execute();

        statusCode = response.getStatusCode();
        responseSessionId = response.getHeaders().getFirstHeaderStringValue(HEADER_SESSION_ID);

        RESULT result;
        try {
            //result = response.parseAs(getResultType());
            String responseBody = response.parseAsString();

            JSONObject responseBodyJson = new JSONObject(responseBody);

            String resultStatus = responseBodyJson.getString("result");
            if (!"success".equalsIgnoreCase(resultStatus)) {
                throw new ResponseFailureException(resultStatus);
            }

            result = request.getParser().parseAndClose(
                    new StringReader(responseBodyJson.getString("arguments")),
                    getResultType());
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse response. SC: " + statusCode, e);
            throw e;
        } finally {
            response.disconnect();
        }

        return result;
    }

    private String createBody() {
        JSONObject bodyObj = new JSONObject();
        try {
            bodyObj.put("method", getMethod());
            bodyObj.putOpt("arguments", getArguments());
        } catch (JSONException e) {
            Log.e(TAG, "Error while creating json body", e);
        }
        return bodyObj.toString();
    }

    protected abstract String getMethod();

    protected abstract JSONObject getArguments();
}
